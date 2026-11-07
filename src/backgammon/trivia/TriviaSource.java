package backgammon.trivia;

import java.util.List;

/**
 * Somewhere questions can be fetched from.
 *
 * <p>The interface exists so the game does not depend on the network being
 * reachable: {@link OpenTriviaDatabase} calls opentdb.com, while
 * {@link OfflineTriviaSource} serves a bundled set. Tests use the latter, or a
 * stub of their own, and never touch the network.</p>
 *
 * <p>Implementations may block, so {@link #fetchQuestions(int)} must never be
 * called on Swing's event dispatch thread. {@link QuestionDeck} takes care of
 * that by fetching on a background thread.</p>
 */
public interface TriviaSource {

    /**
     * Fetches a batch of questions.
     *
     * @param amount how many questions are wanted; must be positive
     * @return the questions fetched, never empty, though possibly fewer than
     *         {@code amount}
     * @throws TriviaException          if the questions cannot be fetched
     * @throws IllegalArgumentException if {@code amount} is not positive
     */
    List<TriviaQuestion> fetchQuestions(int amount) throws TriviaException;

}
