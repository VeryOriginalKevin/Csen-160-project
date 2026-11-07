package backgammon.model;

/**
 * Where every checker is: the 24 points, the bar, and the two bear-off trays.
 *
 * <p>The board only stores positions and carries out moves it is told to make;
 * deciding whether a move is legal is {@link RuleBook}'s job. Keeping the two
 * apart means the board stays a simple container, and the rules can be read in
 * one place.</p>
 *
 * <p>Invariant: a point never holds checkers of both colours at once, and each
 * side always has exactly {@value #CHECKERS_PER_SIDE} checkers spread across
 * the points, its bar, and its tray.</p>
 *
 * <p>Mutable, so a game can progress, but {@link #copy()} gives an independent
 * snapshot whenever one is needed.</p>
 */
public final class Board {

    /** Number of points on the board. */
    public static final int POINT_COUNT = 24;

    /** Checkers each side starts with, and must bear off to win. */
    public static final int CHECKERS_PER_SIDE = 15;

    /**
     * Checkers per point in the standard opening position, for {@code WHITE}.
     * {@code BLACK} mirrors this. Pairs of {point, count}.
     */
    private static final int[][] OPENING_WHITE = {
        {24, 2}, {13, 5}, {8, 3}, {6, 5}
    };

    /**
     * Checkers per point, indexed {@code [colour ordinal][point]}. Index 0 is
     * unused so that point numbers can be used directly.
     */
    private final int[][] pointCounts;

    /** Checkers waiting on the bar, indexed by colour ordinal. */
    private final int[] barCounts;

    /** Checkers already borne off, indexed by colour ordinal. */
    private final int[] offCounts;

    /** Creates a board with no checkers on it. Use the factories instead. */
    private Board() {
        this.pointCounts = new int[PlayerColor.values().length][POINT_COUNT + 1];
        this.barCounts = new int[PlayerColor.values().length];
        this.offCounts = new int[PlayerColor.values().length];
    }

    /**
     * Creates a board in the standard opening position.
     *
     * @return a new board ready for the first turn
     */
    public static Board standard() {
        final Board board = new Board();
        for (final int[] entry : OPENING_WHITE) {
            // BLACK's layout is WHITE's reflected across the middle of the board.
            board.pointCounts[PlayerColor.WHITE.ordinal()][entry[0]] = entry[1];
            board.pointCounts[PlayerColor.BLACK.ordinal()][POINT_COUNT + 1 - entry[0]] = entry[1];
        }
        return board;
    }

    /**
     * Creates a board with no checkers anywhere, which breaks the fifteen-a-side
     * invariant until a test places them. For setting up test positions only.
     *
     * @return an empty board
     */
    static Board empty() {
        return new Board();
    }

    /**
     * Returns whether a number names a real point.
     *
     * @param location the number to test
     * @return {@code true} if it is from 1 to {@value #POINT_COUNT}
     */
    public static boolean isPoint(final int location) {
        return location >= 1 && location <= POINT_COUNT;
    }

    /**
     * Returns an independent copy of this board.
     *
     * @return a board with the same checkers, unaffected by later changes to this one
     */
    public Board copy() {
        final Board copy = new Board();
        for (int side = 0; side < pointCounts.length; side++) {
            System.arraycopy(pointCounts[side], 0, copy.pointCounts[side], 0, pointCounts[side].length);
        }
        System.arraycopy(barCounts, 0, copy.barCounts, 0, barCounts.length);
        System.arraycopy(offCounts, 0, copy.offCounts, 0, offCounts.length);
        return copy;
    }

    /**
     * Returns how many of a side's checkers stand on a point.
     *
     * @param point a point from 1 to 24
     * @param color the side to count
     * @return the number of that side's checkers there
     */
    public int countAt(final int point, final PlayerColor color) {
        requirePoint(point);
        return pointCounts[color.ordinal()][point];
    }

    /**
     * Returns which side occupies a point.
     *
     * @param point a point from 1 to 24
     * @return the occupying side, or {@code null} if the point is empty
     */
    public PlayerColor ownerOf(final int point) {
        requirePoint(point);
        for (final PlayerColor color : PlayerColor.values()) {
            if (pointCounts[color.ordinal()][point] > 0) {
                return color;
            }
        }
        return null;
    }

    /**
     * Returns how many of a side's checkers are waiting on the bar.
     *
     * @param color the side to count
     * @return checkers on that side's bar
     */
    public int barCount(final PlayerColor color) {
        return barCounts[color.ordinal()];
    }

    /**
     * Returns how many of a side's checkers have been borne off.
     *
     * @param color the side to count
     * @return checkers in that side's tray
     */
    public int offCount(final PlayerColor color) {
        return offCounts[color.ordinal()];
    }

    /**
     * Returns whether a side has borne off every checker, and so won.
     *
     * @param color the side to test
     * @return {@code true} once all fifteen are in the tray
     */
    public boolean hasBorneOffAll(final PlayerColor color) {
        return offCount(color) == CHECKERS_PER_SIDE;
    }

    /**
     * Returns whether a point is closed to a side, meaning two or more opposing
     * checkers stand there.
     *
     * @param point a point from 1 to 24
     * @param color the side that wants to land there
     * @return {@code true} if that side may not land on the point
     */
    public boolean isBlockedFor(final int point, final PlayerColor color) {
        return countAt(point, color.opponent()) >= 2;
    }

    /**
     * Returns whether every checker a side still has in play is in its home
     * board, which is the condition for bearing off.
     *
     * @param color the side to test
     * @return {@code true} if nothing of that side is on the bar or outside home
     */
    public boolean isAllHome(final PlayerColor color) {
        if (barCount(color) > 0) {
            return false;
        }
        for (int point = 1; point <= POINT_COUNT; point++) {
            if (!color.isHomePoint(point) && countAt(point, color) > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Carries out a move for a side, hitting a lone opposing checker if one
     * stands on the destination.
     *
     * <p>This method checks only that there is a checker to move and that the
     * destination is not held by two or more opponents; whether the move is
     * otherwise legal is the caller's concern.</p>
     *
     * @param move  the move to make
     * @param color the side making it
     * @return {@code true} if an opposing checker was hit and sent to the bar
     * @throws IllegalStateException if there is no checker at the source, or the
     *                               destination is blocked
     */
    public boolean apply(final Move move, final PlayerColor color) {
        final int side = color.ordinal();
        final int enemy = color.opponent().ordinal();

        // Lift the checker from where it stands.
        if (move.isEntry()) {
            if (barCounts[side] == 0) {
                throw new IllegalStateException("No checker on the bar for " + color);
            }
            barCounts[side]--;
        } else {
            if (pointCounts[side][move.getFrom()] == 0) {
                throw new IllegalStateException("No " + color + " checker on " + move.getFrom());
            }
            pointCounts[side][move.getFrom()]--;
        }

        // Bearing off: the checker simply leaves the board.
        if (move.isBearOff()) {
            offCounts[side]++;
            return false;
        }

        // Landing on a point, hitting a lone opposing checker if there is one.
        final int target = move.getTo();
        if (pointCounts[enemy][target] >= 2) {
            throw new IllegalStateException("Point " + target + " is blocked for " + color);
        }
        final boolean hit = pointCounts[enemy][target] == 1;
        if (hit) {
            pointCounts[enemy][target] = 0;
            barCounts[enemy]++;
        }
        pointCounts[side][target]++;
        return hit;
    }

    /**
     * Puts checkers on a point, replacing whatever was there. For building test
     * positions only; it does not maintain the fifteen-a-side invariant.
     *
     * @param point a point from 1 to 24
     * @param color the side the checkers belong to
     * @param count how many to place
     */
    void setPoint(final int point, final PlayerColor color, final int count) {
        requirePoint(point);
        pointCounts[color.ordinal()][point] = count;
        pointCounts[color.opponent().ordinal()][point] = 0;
    }

    /**
     * Sets how many of a side's checkers wait on the bar. For tests only.
     *
     * @param color the side
     * @param count how many checkers are on its bar
     */
    void setBar(final PlayerColor color, final int count) {
        barCounts[color.ordinal()] = count;
    }

    /**
     * Sets how many of a side's checkers have been borne off. For tests only.
     *
     * @param color the side
     * @param count how many checkers are in its tray
     */
    void setOff(final PlayerColor color, final int count) {
        offCounts[color.ordinal()] = count;
    }

    /**
     * Rejects a number that is not a point.
     *
     * @param point the number to check
     * @throws IllegalArgumentException if it is not from 1 to 24
     */
    private static void requirePoint(final int point) {
        if (!isPoint(point)) {
            throw new IllegalArgumentException("Not a point: " + point);
        }
    }
}
