package backgammon.trivia;

import backgammon.model.PlayerColor;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * The race that opens a turn: both players answer one question at once.
 *
 * <p>This is the only moment in the game when both players have input. The
 * contest ends on the very first answer submitted:</p>
 * <ul>
 *   <li>a correct answer wins the contest for the player who gave it;</li>
 *   <li>an incorrect answer loses it, handing the turn's first move to the
 *       other player.</li>
 * </ul>
 *
 * <p>Either way, exactly one answer decides the contest, and every answer after
 * it is ignored &mdash; which is what makes the winner "immediately the only one
 * in control". {@link #isAcceptingAnswers()} tells the UI when to disable both
 * sets of buttons.</p>
 *
 * <p>No tie is possible. Swing delivers button presses one at a time on the
 * event dispatch thread, so two presses are never simultaneous however close
 * together they land; one is always first. This class is therefore not thread
 * safe by design, and must only be used from the event dispatch thread.</p>
 */
public final class TriviaContest {

    /** The question both players are racing to answer. */
    private final TriviaQuestion question;

    /** The outcome, or {@code null} while the contest is still open. */
    private ContestResult result;

    /**
     * Opens a contest over a question.
     *
     * @param question the question to put to both players
     */
    public TriviaContest(final TriviaQuestion question) {
        this.question = Objects.requireNonNull(question, "question");
        this.result = null;
    }

    /**
     * Returns the question being contested.
     *
     * @return the question
     */
    public TriviaQuestion getQuestion() {
        return question;
    }

    /**
     * Returns whether answers are still being accepted.
     *
     * @return {@code true} while the contest is open
     */
    public boolean isAcceptingAnswers() {
        return result == null;
    }

    /**
     * Submits one player's answer.
     *
     * <p>The first call decides the contest. Later calls change nothing and
     * return the outcome already reached, so a press that arrives a moment too
     * late is harmless.</p>
     *
     * @param player      the side pressing the button
     * @param answerIndex which answer they chose, as an index into
     *                    {@link TriviaQuestion#getAnswers()}
     * @return the outcome, which is now final
     * @throws IndexOutOfBoundsException if {@code answerIndex} names no answer
     */
    public ContestResult submitAnswer(final PlayerColor player, final int answerIndex) {
        // Checked before the resolved test so a bad index is reported even on a
        // late press, rather than being quietly swallowed.
        final boolean correct = question.isCorrect(answerIndex);
        if (result != null) {
            return result;
        }
        if (correct) {
            result = new ContestResult(player, ContestResult.Reason.ANSWERED_CORRECTLY);
        } else {
            result = new ContestResult(player.opponent(),
                    ContestResult.Reason.OPPONENT_ANSWERED_INCORRECTLY);
        }
        return result;
    }

    /**
     * Decides the contest by random draw because neither player answered.
     *
     * <p>The rules say nothing about a question nobody answers, and the turn has
     * to start somehow. A draw keeps it even-handed. If the contest has already
     * been decided, this changes nothing.</p>
     *
     * @param drawSource randomness for the draw; pass a seeded {@code Random} to
     *                   make a test deterministic
     * @return the outcome, which is now final
     */
    public ContestResult resolveByTimeout(final Random drawSource) {
        if (result != null) {
            return result;
        }
        final PlayerColor drawn = drawSource.nextBoolean()
                ? PlayerColor.WHITE
                : PlayerColor.BLACK;
        result = new ContestResult(drawn, ContestResult.Reason.NO_ANSWER_IN_TIME);
        return result;
    }

    /**
     * Returns the outcome if there is one.
     *
     * @return the outcome, or an empty optional while the contest is open
     */
    public Optional<ContestResult> getResult() {
        return Optional.ofNullable(result);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "TriviaContest(" + (result == null ? "open" : result.toString()) + ")";
    }
}
