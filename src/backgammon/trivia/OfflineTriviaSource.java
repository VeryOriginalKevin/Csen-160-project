package backgammon.trivia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * A fixed set of questions bundled with the application.
 *
 * <p>This is the safety net. It lets the game be played with no network at all,
 * covers the moment before the first batch has arrived from
 * {@link OpenTriviaDatabase}, and gives tests a source that behaves identically
 * every run. {@link #fetchQuestions(int)} never fails.</p>
 *
 * <p>The pool is small, so a long game will repeat questions. That is a fair
 * trade for a fallback; {@link QuestionDeck} prefers live questions whenever
 * they are available.</p>
 */
public final class OfflineTriviaSource implements TriviaSource {

    /** One question before its answers have been shuffled. */
    private static final class Entry {

        /** Subject area. */
        private final String category;

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
        private Entry(final String category,
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
            new Entry("Geography", Difficulty.EASY,
                    "What is the capital of Australia?",
                    "Canberra", List.of("Sydney", "Melbourne", "Perth")),
            new Entry("Science: Chemistry", Difficulty.EASY,
                    "What is the chemical symbol for gold?",
                    "Au", List.of("Ag", "Gd", "Go")),
            new Entry("Science: Biology", Difficulty.MEDIUM,
                    "How many bones are in the adult human body?",
                    "206", List.of("201", "212", "196")),
            new Entry("History", Difficulty.MEDIUM,
                    "In which year did the Berlin Wall fall?",
                    "1989", List.of("1987", "1991", "1985")),
            new Entry("Geography", Difficulty.MEDIUM,
                    "Which is the longest river in South America?",
                    "The Amazon", List.of("The Parana", "The Orinoco", "The Madeira")),
            new Entry("Science: Astronomy", Difficulty.EASY,
                    "Which planet is known as the Red Planet?",
                    "Mars", List.of("Venus", "Jupiter", "Mercury")),
            new Entry("Mathematics", Difficulty.EASY,
                    "What is the only even prime number?",
                    "2", List.of("1", "4", "0")),
            new Entry("Literature", Difficulty.MEDIUM,
                    "Who wrote the novel Pride and Prejudice?",
                    "Jane Austen",
                    List.of("Charlotte Bronte", "Emily Dickinson", "Mary Shelley")),
            new Entry("Science: Computers", Difficulty.EASY,
                    "What does CPU stand for?",
                    "Central Processing Unit",
                    List.of("Computer Personal Unit", "Central Process Utility",
                            "Core Programming Unit")),
            new Entry("Science: Biology", Difficulty.EASY,
                    "Which gas do plants absorb from the atmosphere?",
                    "Carbon dioxide", List.of("Oxygen", "Nitrogen", "Hydrogen")),
            new Entry("Geography", Difficulty.EASY,
                    "What is the largest ocean on Earth?",
                    "The Pacific", List.of("The Atlantic", "The Indian", "The Arctic")),
            new Entry("Sports", Difficulty.EASY,
                    "How many players from one team are on the field in soccer?",
                    "11", List.of("9", "10", "12")),
            new Entry("Art", Difficulty.MEDIUM,
                    "Who painted the ceiling of the Sistine Chapel?",
                    "Michelangelo",
                    List.of("Raphael", "Leonardo da Vinci", "Donatello")),
            new Entry("Science: Computers", Difficulty.HARD,
                    "In Java, which keyword prevents a class from being subclassed?",
                    "final", List.of("static", "private", "abstract")),
            new Entry("Science: Chemistry", Difficulty.MEDIUM,
                    "What is the hardest naturally occurring substance?",
                    "Diamond", List.of("Quartz", "Topaz", "Corundum")),
            new Entry("Board Games", Difficulty.MEDIUM,
                    "In backgammon, how many checkers does each player start with?",
                    "15", List.of("12", "16", "20")));

    /** Randomness for shuffling both the pool order and each answer list. */
    private final Random shuffleSource;

    /** Creates a source with unseeded randomness. */
    public OfflineTriviaSource() {
        this(new Random());
    }

    /**
     * Creates a source with a specific source of randomness.
     *
     * @param shuffleSource randomness for the shuffles; pass a seeded
     *                      {@code Random} to make a test deterministic
     */
    public OfflineTriviaSource(final Random shuffleSource) {
        this.shuffleSource = shuffleSource;
    }

    /**
     * Returns how many distinct questions are bundled.
     *
     * @return the size of the pool
     */
    public static int getPoolSize() {
        return POOL.size();
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
        final List<Entry> order = new ArrayList<>(POOL);
        Collections.shuffle(order, shuffleSource);
        final List<TriviaQuestion> questions = new ArrayList<>(amount);
        for (int served = 0; served < amount; served++) {
            // Walk the shuffled order, wrapping around if more were asked for
            // than the pool holds.
            questions.add(build(order.get(served % order.size())));
        }
        return questions;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "Offline questions";
    }

    /**
     * Shuffles one entry's answers into a question.
     *
     * @param entry the entry to build from
     * @return the assembled question
     */
    private TriviaQuestion build(final Entry entry) {
        return TriviaQuestion.of(entry.category, entry.difficulty, entry.prompt,
                entry.correctAnswer, entry.incorrectAnswers, shuffleSource);
    }
}
