package backgammon.app;

/**
 * The phases a single turn passes through, in order.
 *
 * <p>The controller advances through these phases exactly once per turn. The
 * current phase decides which inputs the user interface accepts, which is what
 * keeps the two players from acting at the same time outside the contest.</p>
 */
public enum TurnPhase {

    /**
     * Four dice have been rolled and a question is on screen. Both players'
     * answer buttons are live; the only phase in which that is true.
     */
    TRIVIA_CONTEST,

    /** The contest winner picks two of the four dice. Only the winner has input. */
    DICE_DRAFT,

    /** The contest winner moves with the two dice it drafted. */
    WINNER_MOVEMENT,

    /** The contest loser moves with the two dice left behind. */
    LOSER_MOVEMENT,

    /** One side has borne off all fifteen checkers; no further input is accepted. */
    GAME_OVER
}
