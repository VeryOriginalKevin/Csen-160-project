package backgammon.trivia;

import backgammon.model.PlayerColor;

/**
 * How a trivia contest ended: who won it, and why.
 *
 * <p>Winning the contest decides two things for the turn &mdash; the winner
 * moves first, and the winner drafts two of the four dice. Immutable.</p>
 */
public final class ContestResult {

    /** The ways a contest can be decided. */
    public enum Reason {

        /** The winner answered correctly before the other player answered. */
        ANSWERED_CORRECTLY,

        /** The loser answered incorrectly, handing the contest over. */
        OPPONENT_ANSWERED_INCORRECTLY,

        /** Neither player answered in time, so the winner was drawn at random. */
        NO_ANSWER_IN_TIME
    }

    /** The side that won the contest, and so moves first and drafts dice. */
    private final PlayerColor winner;

    /** How the contest was decided. */
    private final Reason reason;

    /**
     * Records an outcome.
     *
     * @param winner the side that won the contest
     * @param reason how the contest was decided
     */
    public ContestResult(final PlayerColor winner, final Reason reason) {
        this.winner = winner;
        this.reason = reason;
    }

    /**
     * Returns the side that won: it moves first and drafts two dice.
     *
     * @return the winning side
     */
    public PlayerColor getWinner() {
        return winner;
    }

    /**
     * Returns the side that lost: it moves second with the two dice left over.
     *
     * @return the losing side
     */
    public PlayerColor getLoser() {
        return winner.opponent();
    }

    /**
     * Returns how the contest was decided.
     *
     * @return the reason
     */
    public Reason getReason() {
        return reason;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ContestResult(winner=" + winner + ", reason=" + reason + ")";
    }
}
