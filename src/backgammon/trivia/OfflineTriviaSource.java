package backgammon.trivia;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A fixed set of questions bundled with the application.
 *
 * <p>This is the safety net. It lets the game be played with no network at all,
 * covers the moment before the first batch has arrived from
 * {@link OpenTriviaDatabase} (so it supplies the first question of every
 * game), and gives tests a source that needs no network.
 * {@link #fetchQuestions(int)} never fails.</p>
 *
 * <p>No bundled question repeats until every eligible one has been shown. That
 * memory is shared by every instance and lasts for the whole run, so starting a
 * new game does not bring back the questions from the last one. The pool is
 * small, so a long offline session will eventually cycle round again; that is
 * a fair trade for a fallback, since {@link QuestionDeck} prefers live
 * questions whenever they are available.</p>
 *
 * <p>Bundled questions honour the players' topic and difficulty choice where
 * any match it; if none do, the whole pool is used, since some question is
 * better than none.</p>
 */
public final class OfflineTriviaSource implements TriviaSource {

    /** One question before its answers have been shuffled. */
    private static final class Entry {

        /** Subject area. */
        private final TriviaCategory category;

        /** How hard the question is. */
        private final Difficulty difficulty;

        /** The question text. */
        private final String prompt;

        /** The one right answer. */
        private final String correctAnswer;

        /** The decoys. */
        private final List<String> incorrectAnswers;

        /**
         * Records one question.
         *
         * @param category         the subject area
         * @param difficulty       how hard the question is
         * @param prompt           the question text
         * @param correctAnswer    the one right answer
         * @param incorrectAnswers the decoys
         */
        private Entry(final TriviaCategory category,
                      final Difficulty difficulty,
                      final String prompt,
                      final String correctAnswer,
                      final List<String> incorrectAnswers) {
            this.category = category;
            this.difficulty = difficulty;
            this.prompt = prompt;
            this.correctAnswer = correctAnswer;
            this.incorrectAnswers = incorrectAnswers;
        }
    }

    /** Every bundled question. Four answers each, to match the live API. */
    private static final List<Entry> POOL = List.of(
            new Entry(TriviaCategory.GEOGRAPHY, Difficulty.EASY,
                    "What is the capital of Australia?",
                    "Canberra", List.of("Sydney", "Melbourne", "Perth")),
            new Entry(TriviaCategory.SCIENCE_NATURE, Difficulty.EASY,
                    "What is the chemical symbol for gold?",
                    "Au", List.of("Ag", "Gd", "Go")),
            new Entry(TriviaCategory.SCIENCE_NATURE, Difficulty.MEDIUM,
                    "How many bones are in the adult human body?",
                    "206", List.of("201", "212", "196")),
            new Entry(TriviaCategory.HISTORY, Difficulty.MEDIUM,
                    "In which year did the Berlin Wall fall?",
                    "1989", List.of("1987", "1991", "1985")),
            new Entry(TriviaCategory.GEOGRAPHY, Difficulty.MEDIUM,
                    "Which is the longest river in South America?",
                    "The Amazon", List.of("The Parana", "The Orinoco", "The Madeira")),
            new Entry(TriviaCategory.SCIENCE_NATURE, Difficulty.EASY,
                    "Which planet is known as the Red Planet?",
                    "Mars", List.of("Venus", "Jupiter", "Mercury")),
            new Entry(TriviaCategory.MATHEMATICS, Difficulty.EASY,
                    "What is the only even prime number?",
                    "2", List.of("1", "4", "0")),
            new Entry(TriviaCategory.BOOKS, Difficulty.MEDIUM,
                    "Who wrote the novel Pride and Prejudice?",
                    "Jane Austen",
                    List.of("Charlotte Bronte", "Emily Dickinson", "Mary Shelley")),
            new Entry(TriviaCategory.COMPUTERS, Difficulty.EASY,
                    "What does CPU stand for?",
                    "Central Processing Unit",
                    List.of("Computer Personal Unit", "Central Process Utility",
                            "Core Programming Unit")),
            new Entry(TriviaCategory.SCIENCE_NATURE, Difficulty.EASY,
                    "Which gas do plants absorb from the atmosphere?",
                    "Carbon dioxide", List.of("Oxygen", "Nitrogen", "Hydrogen")),
            new Entry(TriviaCategory.GEOGRAPHY, Difficulty.EASY,
                    "What is the largest ocean on Earth?",
                    "The Pacific", List.of("The Atlantic", "The Indian", "The Arctic")),
            new Entry(TriviaCategory.SPORTS, Difficulty.EASY,
                    "How many players from one team are on the field in soccer?",
                    "11", List.of("9", "10", "12")),
            new Entry(TriviaCategory.ART, Difficulty.MEDIUM,
                    "Who painted the ceiling of the Sistine Chapel?",
                    "Michelangelo",
                    List.of("Raphael", "Leonardo da Vinci", "Donatello")),
            new Entry(TriviaCategory.COMPUTERS, Difficulty.HARD,
                    "In Java, which keyword prevents a class from being subclassed?",
                    "final", List.of("static", "private", "abstract")),
            new Entry(TriviaCategory.SCIENCE_NATURE, Difficulty.MEDIUM,
                    "What is the hardest naturally occurring substance?",
                    "Diamond", List.of("Quartz", "Topaz", "Corundum")),
            new Entry(TriviaCategory.BOARD_GAMES, Difficulty.MEDIUM,
                    "In backgammon, how many checkers does each player start with?",
                    "15", List.of("12", "16", "20")));

    /**
     * Bundled questions already shown this run, shared by every instance so a
     * new game (which gets a new source) remembers what earlier games showed.
     * Guarded by its own lock, as a fetch may run on the deck's background thread.
     */
    private static final Set<Entry> SHOWN = new HashSet<>();

    /** Randomness for picking questions and shuffling each answer list. */
    private final Random shuffleSource;

    /** The bundled questions this source serves: those matching the settings. */
    private final List<Entry> eligible;

    /**
     * Creates a source over every bundled question.
     *
     * @param shuffleSource randomness for the shuffles; pass a seeded
     *                      {@code Random} to make a test deterministic
     */
    public OfflineTriviaSource(final Random shuffleSource) {
        this(shuffleSource, TriviaSettings.ANY);
    }

    /**
     * Creates a source over the bundled questions matching a choice of topics
     * and difficulties, or over all of them if none match.
     *
     * @param shuffleSource randomness for the shuffles
     * @param settings      which topics and difficulties the players chose
     */
    public OfflineTriviaSource(final Random shuffleSource, final TriviaSettings settings) {
        this.shuffleSource = shuffleSource;
        final List<Entry> matching = new ArrayList<>();
        for (final Entry entry : POOL) {
            if (settings.allows(entry.category, entry.difficulty)) {
                matching.add(entry);
            }
        }
        this.eligible = matching.isEmpty() ? POOL : matching;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Never throws. Asking for more questions than the pool holds is allowed
     * and will repeat questions, each time with its answers reshuffled.</p>
     */
    @Override
    public List<TriviaQuestion> fetchQuestions(final int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
        final List<TriviaQuestion> questions = new ArrayList<>(amount);
        synchronized (SHOWN) {
            for (int served = 0; served < amount; served++) {
                questions.add(build(takeUnshown()));
            }
        }
        return questions;
    }

    /**
     * Forgets which bundled questions have been shown, so tests can start from
     * a known state.
     */
    static void forgetShown() {
        synchronized (SHOWN) {
            SHOWN.clear();
        }
    }

    /**
     * Picks a random eligible question that has not been shown yet, and marks
     * it shown. Once every eligible question has been shown, they all become
     * available again. Must be called holding the {@link #SHOWN} lock.
     *
     * @return the chosen entry
     */
    private Entry takeUnshown() {
        List<Entry> candidates = unshown();
        if (candidates.isEmpty()) {
            // Every eligible question has been seen: start a fresh cycle.
            SHOWN.removeAll(eligible);
            candidates = unshown();
        }
        final Entry chosen = candidates.get(shuffleSource.nextInt(candidates.size()));
        SHOWN.add(chosen);
        return chosen;
    }

    /**
     * Lists the eligible questions not yet shown.
     *
     * @return the candidates, in pool order
     */
    private List<Entry> unshown() {
        final List<Entry> candidates = new ArrayList<>();
        for (final Entry entry : eligible) {
            if (!SHOWN.contains(entry)) {
                candidates.add(entry);
            }
        }
        return candidates;
    }

    /**
     * Shuffles one entry's answers into a question.
     *
     * @param entry the entry to build from
     * @return the assembled question
     */
    private TriviaQuestion build(final Entry entry) {
        return TriviaQuestion.of(entry.category.getDisplayName(), entry.difficulty, entry.prompt,
                entry.correctAnswer, entry.incorrectAnswers, shuffleSource);
    }
}
