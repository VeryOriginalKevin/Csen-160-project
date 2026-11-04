package backgammon.trivia;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps questions ready so a turn never waits on the network.
 *
 * <p>A turn begins with a question, and the game must not freeze while one is
 * fetched. So the deck buffers a batch from a live source on a background
 * thread, hands them out one at a time, and refills when the buffer runs low.
 * {@link #next()} returns immediately, always: if the buffer has run dry it
 * serves a bundled question from the fallback source instead.</p>
 *
 * <p>Fetching in batches also keeps the game inside the API's rate limit of one
 * request every five seconds &mdash; one request covers twenty turns.</p>
 *
 * <p>Thread safe. {@link #next()} is meant to be called from Swing's event
 * dispatch thread; refills never run there.</p>
 */
public final class QuestionDeck {

    /** How many questions to fetch per request. */
    private static final int BATCH_SIZE = 20;

    /** Buffer size at or below which a refill is started. */
    private static final int REFILL_THRESHOLD = 5;

    /** The live source, tried first. */
    private final TriviaSource primary;

    /** The bundled source, used whenever the buffer is empty. */
    private final TriviaSource fallback;

    /** Background thread on which refills run. */
    private final ExecutorService fetcher;

    /** Questions fetched but not yet handed out. */
    private final Queue<TriviaQuestion> buffered = new ConcurrentLinkedQueue<>();

    /** Whether a refill is already running, so only one runs at a time. */
    private final AtomicBoolean refilling = new AtomicBoolean(false);

    /** Whether the most recent refill attempt succeeded. */
    private volatile boolean primaryReachable = true;

    /** Message from the most recent failed refill, or {@code null} if none has failed. */
    private volatile String lastFailure;

    /**
     * Creates a deck over a live source and a fallback.
     *
     * @param primary  the live source, preferred for every question
     * @param fallback the bundled source, used when the buffer is empty; it must
     *                 not fail or block
     */
    public QuestionDeck(final TriviaSource primary, final TriviaSource fallback) {
        this.primary = primary;
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
     * Creates a deck over the real API with the bundled questions as fallback.
     *
     * @return a deck ready for {@link #start()}
     */
    public static QuestionDeck createDefault() {
        return new QuestionDeck(new OpenTriviaDatabase(), new OfflineTriviaSource());
    }

    /**
     * Starts the first refill.
     *
     * <p>Call once, at startup, so questions are already buffered by the time
     * the first turn begins. Returns straight away; the fetch runs in the
     * background.</p>
     */
    public void start() {
        requestRefill();
    }

    /**
     * Returns the next question, without ever blocking.
     *
     * @return a question from the buffer, or a bundled one if the buffer is
     *         empty; never {@code null}
     */
    public TriviaQuestion next() {
        final TriviaQuestion ready = buffered.poll();
        if (buffered.size() <= REFILL_THRESHOLD) {
            requestRefill();
        }
        if (ready != null) {
            return ready;
        }
        return fetchFallbackQuestion();
    }

    /**
     * Returns how many questions are buffered and ready.
     *
     * @return the buffer's current size
     */
    public int getBufferedCount() {
        return buffered.size();
    }

    /**
     * Returns whether the most recent refill reached the live source.
     *
     * @return {@code true} if live questions are being served
     */
    public boolean isPrimaryReachable() {
        return primaryReachable;
    }

    /**
     * Returns why the most recent refill failed.
     *
     * @return the failure message, or {@code null} if no refill has failed
     */
    public String getLastFailure() {
        return lastFailure;
    }

    /**
     * Stops the background thread. The deck must not be used afterwards.
     *
     * <p>Call when the application closes. Buffered questions are discarded.</p>
     */
    public void shutdown() {
        fetcher.shutdownNow();
    }

    /**
     * Starts a refill unless one is already running.
     *
     * <p>The guard matters because {@link #next()} asks for a refill on every
     * call once the buffer is low, and a request takes seconds to come back.</p>
     */
    private void requestRefill() {
        if (!refilling.compareAndSet(false, true)) {
            return;
        }
        try {
            fetcher.execute(this::refill);
        } catch (final RuntimeException rejected) {
            // The executor is shut down, or could not accept the task; clear
            // the guard so a later call can try again.
            refilling.set(false);
        }
    }

    /**
     * Fetches a batch and adds it to the buffer. Runs on the background thread.
     *
     * <p>A failure here is not fatal and is not rethrown: it is recorded for the
     * status line, and {@link #next()} falls back to bundled questions.</p>
     */
    private void refill() {
        try {
            final List<TriviaQuestion> batch = primary.fetchQuestions(BATCH_SIZE);
            buffered.addAll(batch);
            primaryReachable = true;
        } catch (final TriviaException failure) {
            primaryReachable = false;
            lastFailure = failure.getMessage();
        } catch (final RuntimeException failure) {
            // A malformed response can surface as an unchecked exception. The
            // game carries on with bundled questions either way.
            primaryReachable = false;
            lastFailure = failure.toString();
        } finally {
            refilling.set(false);
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
