package backgammon.trivia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * One multiple-choice question, with its answers already shuffled.
 *
 * <p>Immutable. The shuffle happens once, in {@link #of}, so that the answer
 * order is fixed for the life of the question: both players see the same
 * ordering, and the index a player clicks keeps meaning the same answer.</p>
 *
 * <p>Holding the answers in one list, with the correct one's index alongside,
 * means the UI can lay out buttons without knowing which is correct, and no
 * caller can accidentally reveal the answer through the order it receives.</p>
 */
public final class TriviaQuestion {

    /** Subject area, such as {@code "Entertainment: Film"}. */
    private final String category;

    /** How hard the API rated this question. */
    private final Difficulty difficulty;

    /** The question text, shown above the answer buttons. */
    private final String prompt;

    /** Every answer, correct and incorrect, in their shuffled order. */
    private final List<String> answers;

    /** Position of the correct answer within {@link #answers}. */
    private final int correctIndex;

    /**
     * Creates a question whose answers are already in their final order.
     *
     * @param category     the subject area
     * @param difficulty   how hard the question is
     * @param prompt       the question text
     * @param answers      every answer, in the order players will see them
     * @param correctIndex position of the correct answer in {@code answers}
     * @throws IllegalArgumentException if fewer than two answers are given, or
     *                                  {@code correctIndex} falls outside them
     */
    public TriviaQuestion(final String category,
                          final Difficulty difficulty,
                          final String prompt,
                          final List<String> answers,
                          final int correctIndex) {
        if (answers.size() < 2) {
            throw new IllegalArgumentException("A question needs at least two answers");
        }
        if (correctIndex < 0 || correctIndex >= answers.size()) {
            throw new IllegalArgumentException("Correct index out of range: " + correctIndex);
        }
        this.category = Objects.requireNonNull(category, "category");
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        this.prompt = Objects.requireNonNull(prompt, "prompt");
        this.answers = List.copyOf(answers);
        this.correctIndex = correctIndex;
    }

    /**
     * Creates a question from a correct answer and its decoys, shuffling them
     * together.
     *
     * @param category         the subject area
     * @param difficulty       how hard the question is
     * @param prompt           the question text
     * @param correctAnswer    the one right answer
     * @param incorrectAnswers the decoys
     * @param shuffleSource    randomness for the shuffle; pass a seeded
     *                         {@code Random} to make a test deterministic
     * @return the assembled question
     * @throws IllegalArgumentException if no decoys are given
     */
    public static TriviaQuestion of(final String category,
                                    final Difficulty difficulty,
                                    final String prompt,
                                    final String correctAnswer,
                                    final List<String> incorrectAnswers,
                                    final Random shuffleSource) {
        final List<String> shuffled = new ArrayList<>(incorrectAnswers);
        shuffled.add(correctAnswer);
        Collections.shuffle(shuffled, shuffleSource);
        // indexOf is safe here: identical answer strings would be
        // indistinguishable to a player anyway, so either index is correct.
        return new TriviaQuestion(category, difficulty, prompt,
                shuffled, shuffled.indexOf(correctAnswer));
    }

    /**
     * Returns the subject area.
     *
     * @return the category, as the API spells it
     */
    public String getCategory() {
        return category;
    }

    /**
     * Returns how hard this question is.
     *
     * @return the difficulty tier
     */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /**
     * Returns the question text.
     *
     * @return the prompt shown to both players
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Returns every answer, in the order players see them.
     *
     * @return an unmodifiable list of answers, at least two long
     */
    public List<String> getAnswers() {
        return answers;
    }

    /**
     * Returns how many answers this question offers, and so how many buttons
     * each player's panel needs.
     *
     * @return the number of answers
     */
    public int getAnswerCount() {
        return answers.size();
    }

    /**
     * Returns whether the answer at a position is the correct one.
     *
     * @param answerIndex position of the answer a player chose
     * @return {@code true} if that answer is correct
     * @throws IndexOutOfBoundsException if {@code answerIndex} names no answer
     */
    public boolean isCorrect(final int answerIndex) {
        if (answerIndex < 0 || answerIndex >= answers.size()) {
            throw new IndexOutOfBoundsException("No answer at index " + answerIndex);
        }
        return answerIndex == correctIndex;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // The correct answer is left out so a stray log line cannot leak it.
        return "TriviaQuestion(" + category + ", " + difficulty + ", " + prompt + ")";
    }
}
