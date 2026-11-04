package backgammon.model;

/**
 * The two sides of the board, and the direction each one travels.
 *
 * <p>Points are numbered 1&ndash;24 once, for the whole board. {@code LIGHT}
 * moves from high-numbered points toward point 1 and bears off past point 0;
 * {@code DARK} moves from low-numbered points toward point 24 and bears off past
 * point 25. Expressing the difference as a signed step lets the move rules be
 * written once instead of twice.</p>
 */
public enum PlayerColor {

    /** Moves 24 &rarr; 1; home board is points 1&ndash;6. */
    LIGHT(-1, 6),

    /** Moves 1 &rarr; 24; home board is points 19&ndash;24. */
    DARK(1, 19);

    /** Signed step added to a point number when this side moves one pip. */
    private final int direction;

    /** Point number at which this side's six-point home board begins. */
    private final int homeBoardStart;

    /**
     * Creates a colour with its direction of travel.
     *
     * @param direction      {@code -1} to move toward point 1, {@code +1} toward point 24
     * @param homeBoardStart first point of this side's home board
     */
    PlayerColor(final int direction, final int homeBoardStart) {
        this.direction = direction;
        this.homeBoardStart = homeBoardStart;
    }

    /**
     * Returns the signed step this side adds to a point number per pip moved.
     *
     * @return {@code -1} for {@code LIGHT}, {@code +1} for {@code DARK}
     */
    public int getDirection() {
        return direction;
    }

    /**
     * Returns the first point of this side's home board.
     *
     * @return {@code 6} for {@code LIGHT}, {@code 19} for {@code DARK}
     */
    public int getHomeBoardStart() {
        return homeBoardStart;
    }

    /**
     * Returns the other side.
     *
     * @return the opposing colour
     */
    public PlayerColor opponent() {
        return this == LIGHT ? DARK : LIGHT;
    }
}
