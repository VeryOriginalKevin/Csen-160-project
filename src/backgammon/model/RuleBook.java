package backgammon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Decides which single moves are legal for a side, given the board and the
 * die values it has left.
 *
 * <p>The rules this game uses:</p>
 * <ul>
 *   <li>A side with checkers on the bar must enter them before moving anything
 *       else. A checker enters on the point a die's distance into the
 *       opponent's home board.</li>
 *   <li>A checker may land on any point not held by two or more opposing
 *       checkers. Landing on a lone opposing checker hits it to the bar.</li>
 *   <li>Once every checker of a side is in its home board, it may bear off: a
 *       die that exactly reaches the edge bears off from that point, and a
 *       larger die bears off the checker furthest from home if no checker
 *       stands further back.</li>
 * </ul>
 *
 * <p>Deliberately relaxed compared with tournament play: any legal move may be
 * made in any order, and there is no obligation to choose moves that use both
 * dice. A turn ends when its dice are spent or nothing legal remains.</p>
 *
 * <p>Stateless, so one instance can be shared freely.</p>
 */
public final class RuleBook {

    /** Creates the rule book. */
    public RuleBook() {
        // Nothing to set up: every method works from its arguments alone.
    }

    /**
     * Lists every legal single move.
     *
     * @param board the current position
     * @param color the side to move
     * @param pool  the die values that side has left
     * @return every legal move, in no guaranteed order; empty if the side
     *         cannot move at all
     */
    public List<Move> legalMoves(final Board board, final PlayerColor color, final DicePool pool) {
        final List<Move> moves = new ArrayList<>();

        // Checkers on the bar come first: while any wait, only entries are legal.
        if (board.barCount(color) > 0) {
            for (final int die : pool.getDistinctValues()) {
                final int target = color.getBarPoint() + die * color.getDirection();
                if (!board.isBlockedFor(target, color)) {
                    moves.add(new Move(Move.BAR, target, die));
                }
            }
            return Collections.unmodifiableList(moves);
        }

        // Otherwise, try every distinct die from every point this side occupies.
        final boolean bearingOff = board.isAllHome(color);
        for (int point = 1; point <= Board.POINT_COUNT; point++) {
            if (board.countAt(point, color) == 0) {
                continue;
            }
            for (final int die : pool.getDistinctValues()) {
                final int target = point + die * color.getDirection();
                if (Board.isPoint(target)) {
                    if (!board.isBlockedFor(target, color)) {
                        moves.add(new Move(point, target, die));
                    }
                } else if (bearingOff && canBearOff(board, color, point, die)) {
                    moves.add(new Move(point, Move.OFF, die));
                }
            }
        }
        return Collections.unmodifiableList(moves);
    }

    /**
     * Returns whether a checker may bear off from a point with a die, assuming
     * the side already has every checker home.
     *
     * @param board the current position
     * @param color the side bearing off
     * @param point the point the checker stands on
     * @param die   the die value to use
     * @return {@code true} if the die exactly reaches the edge, or overshoots it
     *         with no checker of this side further from home
     */
    private boolean canBearOff(final Board board, final PlayerColor color,
                               final int point, final int die) {
        final int distance = color.pipsToBearOff(point);
        if (die == distance) {
            return true;
        }
        if (die < distance) {
            return false;
        }
        // Overshooting is only allowed from the rearmost occupied point.
        for (int other = 1; other <= Board.POINT_COUNT; other++) {
            if (board.countAt(other, color) > 0 && color.pipsToBearOff(other) > distance) {
                return false;
            }
        }
        return true;
    }
}
