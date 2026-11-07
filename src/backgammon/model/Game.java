package backgammon.model;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * One game of backgammon: the board, the dice, and who has won.
 *
 * <p>The game knows nothing about trivia or turn order; it simply rolls four
 * dice when asked and starts movement turns for whichever side the caller
 * names. Sequencing those steps is the job of the application layer.</p>
 */
public final class Game {

    /** Where every checker is. */
    private final Board board;

    /** The rules every movement turn is checked against. */
    private final RuleBook rules;

    /** Randomness for rolling dice. */
    private final Random random;

    /**
     * Starts a game from the standard opening position.
     *
     * @param random randomness for the dice; pass a seeded {@code Random} to make
     *               a test deterministic
     */
    public Game(final Random random) {
        this(Board.standard(), random);
    }

    /**
     * Starts a game from a given position. Mainly for tests.
     *
     * @param board  the position to start from; the game takes ownership of it
     * @param random randomness for the dice
     */
    public Game(final Board board, final Random random) {
        this.board = board;
        this.rules = new RuleBook();
        this.random = random;
    }

    /**
     * Returns the board.
     *
     * <p>The board itself is returned, not a copy, so callers can read the live
     * position. It should only be changed through a {@link MovementTurn}.</p>
     *
     * @return the live board
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Rolls the four dice for a new round.
     *
     * @return the roll
     */
    public DiceRoll rollDice() {
        return DiceRoll.roll(random);
    }

    /**
     * Starts a movement turn on this game's board.
     *
     * @param player the side to move
     * @param dice   that side's two dice
     * @return the turn, ready for moves
     */
    public MovementTurn startTurn(final PlayerColor player, final List<Die> dice) {
        return new MovementTurn(board, player, dice, rules);
    }

    /**
     * Returns the winner, if there is one.
     *
     * @return the side that has borne off all its checkers, or empty while the
     *         game is still on
     */
    public Optional<PlayerColor> getWinner() {
        for (final PlayerColor color : PlayerColor.values()) {
            if (board.hasBorneOffAll(color)) {
                return Optional.of(color);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns whether the game has finished.
     *
     * @return {@code true} once one side has borne off every checker
     */
    public boolean isOver() {
        return getWinner().isPresent();
    }
}
