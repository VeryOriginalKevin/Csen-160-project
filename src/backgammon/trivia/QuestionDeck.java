package backgammon.trivia;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps questions ready so a turn never waits on the network, and mixes the
 * players' chosen topics question by question.
 *
 * <p>The API returns one topic and one difficulty per request, so a single
 * buffer would serve long runs of the same topic. Instead the deck keeps one
 * <em>supply</em> per topic-and-difficulty combination the players chose, each
 * with its own live source and its own buffer. Every call to {@link #next()}
 * picks a random supply that has questions ready, so each question is
 * independently from any of the chosen topics.</p>
 *
 * <p>A background thread keeps the supplies topped up, always refilling the
 * emptiest one next. The API allows one request every five seconds, so with
 * several topics the supplies fill one after another; until then questions
 * come from whichever supplies are ready. If none are, {@link #next()} serves
 * a bundled question instead, so it never blocks.</p>
 *
 * <p>Thread safe. {@link #next()} is meant to be called from Swing's event
 * dispatch thread; refills never run there.</p>
 */
public final class QuestionDeck {

    /** How many questions to fetch per request. */
    private static final int BATCH_SIZE = 10;

    /** A supply holding this many questions or fewer is due a refill. */
    private static final int LOW_WATER = 2;

    /** Failed requests after which a supply is given up on (say, a topic with no questions). */
    private static final int MAX_FAILURES = 3;

    /** One topic-and-difficulty combination: where its questions come from, and those ready. */
    private static final class Supply {

        /** The live source for this combination. */
        private final TriviaSource source;

        /** Questions fetched but not yet handed out. Guarded by the deck's lock. */
        private final List<TriviaQuestion> ready = new ArrayList<>();

        /** How many requests to the source have failed. Guarded by the deck's lock. */
        private int failures;

        /**
         * Creates an empty supply.
         *
         * @param source where its questions come from
         */
        Supply(final TriviaSource source) {
            this.source = source;
        }

        /**
         * Returns whether this supply has been given up on.
         *
         * @return {@code true} after too many failed requests
         */
        boolean isRetired() {
            return failures >= MAX_FAILURES;
        }
    }

    /** One supply per combination; never empty. */
    private final List<Supply> supplies;

    /** The bundled source, used whenever every supply is empty. */
    private final TriviaSource fallback;

    /** Guards every supply's buffer and failure count. */
    private final Object lock = new Object();

    /** Randomness for choosing which supply serves each question. */
    private final Random chooser = new Random();

    /** Background thread on which refills run. */
    private final ExecutorService fetcher;

    /** Whether a refill is queued or running, so only one runs at a time. */
    private final AtomicBoolean refilling = new AtomicBoolean(false);

    /**
     * Creates a deck over some live sources and a fallback.
     *
     * @param primaries one live source per combination to mix; at least one
     * @param fallback  the bundled source, used when every supply is empty; it
     *                  must not fail or block
     * @throws IllegalArgumentException if no live source is given
     */
    public QuestionDeck(final List<TriviaSource> primaries, final TriviaSource fallback) {
        if (primaries.isEmpty()) {
            throw new IllegalArgumentException("A deck needs at least one live source");
        }
        this.supplies = new ArrayList<>();
        for (final TriviaSource source : primaries) {
            supplies.add(new Supply(source));
        }
        this.fallback = fallback;
        // A daemon thread so a pending fetch cannot keep the JVM alive after
        // the window closes.
        this.fetcher = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "trivia-fetch");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Creates a deck over the real API, with one supply for every combination
     * of the chosen topics and difficulties, and the bundled questions as
     * fallback.
     *
     * @param settings which topics and difficulties to serve
     * @return a deck ready for {@link #start()}
     */
    public static QuestionDeck createDefault(final TriviaSettings settings) {
        // "Any" is a single null entry: the source then leaves that filter off.
        final List<TriviaCategory> topics = new ArrayList<>(settings.getCategories());
        if (topics.isEmpty()) {
            topics.add(null);
        }
        final List<Difficulty> tiers = new ArrayList<>(settings.getDifficulties());
        if (tiers.isEmpty()) {
            tiers.add(null);
        }
        final List<TriviaSource> primaries = new ArrayList<>();
        for (final TriviaCategory topic : topics) {
            for (final Difficulty tier : tiers) {
                primaries.add(new OpenTriviaDatabase(new Random(), topic, tier));
            }
        }
        return new QuestionDeck(primaries, new OfflineTriviaSource(new Random(), settings));
    }

    /**
     * Starts filling the supplies in the background. Returns straight away.
     */
    public void start() {
        requestRefill();
    }

    /**
     * Returns the next question, without ever blocking.
     *
     * @return a question from a randomly chosen supply that has one ready, or a
     *         bundled one if none has; never {@code null}
     */
    public TriviaQuestion next() {
        TriviaQuestion question = null;
        synchronized (lock) {
            final List<Supply> stocked = new ArrayList<>();
            for (final Supply supply : supplies) {
                if (!supply.ready.isEmpty()) {
                    stocked.add(supply);
                }
            }
            if (!stocked.isEmpty()) {
                // Choosing the supply first, rather than a question from all of
                // them, gives every chosen topic an equal chance each turn.
                question = stocked.get(chooser.nextInt(stocked.size())).ready.remove(0);
            }
        }
        requestRefill();
        return question != null ? question : fetchFallbackQuestion();
    }

    /**
     * Returns how many live questions are ready across every supply.
     *
     * @return the total buffered
     */
    public int getBufferedCount() {
        synchronized (lock) {
            int total = 0;
            for (final Supply supply : supplies) {
                total += supply.ready.size();
            }
            return total;
        }
    }

    /**
     * Stops the background thread. The deck must not be used afterwards.
     *
     * <p>Call when the game ends. Buffered questions are discarded.</p>
     */
    public void shutdown() {
        fetcher.shutdownNow();
    }

    /**
     * Queues a refill unless one is already queued or running.
     */
    private void requestRefill() {
        if (!refilling.compareAndSet(false, true)) {
            return;
        }
        try {
            fetcher.execute(this::refill);
        } catch (final RuntimeException rejected) {
            // The executor is shut down; clear the guard and give up quietly.
            refilling.set(false);
        }
    }

    /**
     * Tops up the neediest supply, then queues another refill if any supply is
     * still low. Runs on the background thread.
     *
     * <p>A failure is not fatal and is not rethrown: it counts against that
     * supply, and {@link #next()} serves from the others, or bundled questions,
     * meanwhile.</p>
     */
    private void refill() {
        try {
            final Supply supply = neediestSupply();
            if (supply == null) {
                return;
            }
            try {
                final List<TriviaQuestion> batch = supply.source.fetchQuestions(BATCH_SIZE);
                synchronized (lock) {
                    supply.ready.addAll(batch);
                }
            } catch (final TriviaException | RuntimeException failure) {
                // Unreachable network, an API error, or a malformed response
                // (which can surface unchecked).
                synchronized (lock) {
                    supply.failures++;
                }
            }
        } finally {
            refilling.set(false);
        }
        // Keep going until every supply is stocked. The API's pacing spaces
        // these requests five seconds apart.
        if (neediestSupply() != null) {
            requestRefill();
        }
    }

    /**
     * Finds the supply most in need of questions.
     *
     * @return the emptiest supply at or below {@value #LOW_WATER} questions
     *         that has not been given up on, or {@code null} if none needs a refill
     */
    private Supply neediestSupply() {
        synchronized (lock) {
            Supply neediest = null;
            for (final Supply supply : supplies) {
                if (supply.isRetired() || supply.ready.size() > LOW_WATER) {
                    continue;
                }
                if (neediest == null || supply.ready.size() < neediest.ready.size()) {
                    neediest = supply;
                }
            }
            return neediest;
        }
    }

    /**
     * Takes one question from the fallback source.
     *
     * @return a bundled question
     * @throws IllegalStateException if the fallback source fails, which would
     *                               mean it was not the dependable source this
     *                               class requires
     */
    private TriviaQuestion fetchFallbackQuestion() {
        try {
            return fallback.fetchQuestions(1).get(0);
        } catch (final TriviaException failure) {
            throw new IllegalStateException(
                    "The fallback trivia source must not fail", failure);
        }
    }
}
