package backgammon.model;

/**
 * The two sides of the board, and the direction each one travels.
 *
 * <p>Points are numbered 1&ndash;24 once, for the whole board. {@code WHITE}
 * moves from high-numbered points toward point 1 and bears off past point 0;
 * {@code BLACK} moves from low-numbered points toward point 24 and bears off past
 * point 25. Expressing the difference as a signed step lets the move rules be
 * written once instead of twice.</p>
 *
 * <p>Two imaginary points complete the picture. A checker on the bar behaves as
 * if it sat just before the first point of its journey (25 for {@code WHITE},
 * 0 for {@code BLACK}), so entering is an ordinary move from there. And a
 * checker is borne off once it travels past the far end (0 for {@code WHITE},
 * 25 for {@code BLACK}).</p>
 */
public enum PlayerColor {

    /** Moves 24 &rarr; 1; home board is points 1&ndash;6. */
    WHITE(-1, 25, 0),

    /** Moves 1 &rarr; 24; home board is points 19&ndash;24. */
    BLACK(1, 0, 25);

    /** How many points make up a home board. */
    public static final int HOME_BOARD_SIZE = 6;

    /** Signed step added to a point number when this side moves one pip. */
    private final int direction;

    /** Imaginary point a checker on the bar is treated as standing on. */
    private final int barPoint;

    /** Imaginary point just past the end of the journey, where checkers bear off. */
    private final int offPoint;

    /**
     * Creates a colour with its direction of travel.
     *
     * @param direction      {@code -1} to move toward point 1, {@code +1} toward point 24
     * @param barPoint       imaginary point the bar stands for
     * @param offPoint       imaginary point past which checkers are borne off
     */
    PlayerColor(final int direction, final int barPoint, final int offPoint) {
        this.direction = direction;
        this.barPoint = barPoint;
        this.offPoint = offPoint;
    }

    /**
     * Returns the signed step this side adds to a point number per pip moved.
     *
     * @return {@code -1} for {@code WHITE}, {@code +1} for {@code BLACK}
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Returns the imaginary point a checker on the bar enters from.
     *
     * <p>A checker entering with a die of {@code d} lands on
     * {@code getBarPoint() + d * getDirection()}.</p>
     *
     * @return {@code 25} for {@code WHITE}, {@code 0} for {@code BLACK}
     */
    public int getBarPoint() {
        return barPoint;
    }

    /**
     * Returns how many pips a checker on a point still has to travel to be
     * borne off.
     *
     * @param point a point from 1 to 24
     * @return the distance to the bear-off edge, from 1 to 24; a value of
     *         {@value #HOME_BOARD_SIZE} or less means the point is in this
     *         side's home board
     */
    public int pipsToBearOff(final int point) {
        return Math.abs(point - offPoint);
    }

    /**
     * Returns whether a point lies in this side's home board.
     *
     * @param point a point from 1 to 24
     * @return {@code true} if a checker there may take part in bearing off
     */
    public boolean isHomePoint(final int point) {
        return pipsToBearOff(point) <= HOME_BOARD_SIZE;
    }

    /**
     * Returns the other side.
     *
     * @return the opposing colour
     */
    public PlayerColor opponent() {
        return this == WHITE ? BLACK : WHITE;
    }
}
