package backgammon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * One side moving its checkers with the two dice it was given this round.
 *
 * <p>A round contains two of these: the trivia winner's, then the loser's. Each
 * spends the player's {@link DicePool} one move at a time, and is over when the
 * pool is empty, when no legal move remains, or when the player has borne off
 * every checker.</p>
 */
public final class MovementTurn {

    /** The board this turn moves checkers on; shared with the game. */
    private final Board board;

    /** The side moving. */
    private final PlayerColor player;

    /** The die values still to be used. */
    private final DicePool pool;

    /** The rules deciding which moves are legal. */
    private final RuleBook rules;

    /**
     * Starts a turn.
     *
     * @param board  the board to move on; changed in place as moves are made
     * @param player the side moving
     * @param dice   that side's two dice
     * @param rules  the rules to check moves against
     */
    public MovementTurn(final Board board, final PlayerColor player,
                        final List<Die> dice, final RuleBook rules) {
        this.board = board;
        this.player = player;
        this.pool = new DicePool(dice);
        this.rules = rules;
    }

    /**
     * Returns the side moving.
     *
     * @return the player whose turn this is
     */
    public PlayerColor getPlayer() {
        return player;
    }

    /**
     * Returns the die values still to be used.
     *
     * @return the values, largest first, with repeats for doubles
     */
    public List<Integer> getRemainingDice() {
        return pool.getRemaining();
    }

    /**
     * Lists every move the player may make next.
     *
     * @return the legal moves; empty once the turn is over
     */
    public List<Move> legalMoves() {
        if (pool.isEmpty() || board.hasBorneOffAll(player)) {
            return Collections.emptyList();
        }
        return rules.legalMoves(board, player, pool);
    }

    /**
     * Returns every location the player may move a checker from next.
     *
     * @return points 1&ndash;24, or {@link Move#BAR}, in ascending order
     */
    public Set<Integer> legalSources() {
        final Set<Integer> sources = new TreeSet<>();
        for (final Move move : legalMoves()) {
            sources.add(move.getFrom());
        }
        return sources;
    }

    /**
     * Lists the legal moves that start from one location.
     *
     * @param from a point, or {@link Move#BAR}
     * @return the moves from there, smallest die first
     */
    public List<Move> legalMovesFrom(final int from) {
        final List<Move> moves = new ArrayList<>();
        for (final Move move : legalMoves()) {
            if (move.getFrom() == from) {
                moves.add(move);
            }
        }
        moves.sort((a, b) -> Integer.compare(a.getDieValue(), b.getDieValue()));
        return moves;
    }

    /**
     * Makes a move, spending its die.
     *
     * @param move the move to make; it must be one of {@link #legalMoves()}
     * @return {@code true} if the move hit an opposing checker
     * @throws IllegalArgumentException if the move is not currently legal
     */
    public boolean play(final Move move) {
        if (!legalMoves().contains(move)) {
            throw new IllegalArgumentException("Not a legal move now: " + move);
        }
        final boolean hit = board.apply(move, player);
        pool.spend(move.getDieValue());
        return hit;
    }

    /**
     * Returns whether this turn is over.
     *
     * @return {@code true} if the dice are spent, nothing legal remains, or the
     *         player has won
     */
    public boolean isOver() {
        return legalMoves().isEmpty();
    }

    /**
     * Returns whether the turn ended with dice still unused because no legal
     * move was left for them.
     *
     * @return {@code true} if the player was blocked rather than finishing normally
     */
    public boolean isBlocked() {
        return isOver() && !pool.isEmpty() && !board.hasBorneOffAll(player);
    }
}
