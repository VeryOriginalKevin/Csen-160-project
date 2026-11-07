package backgammon.model;

/**
 * One checker moving from one place to another using one die: immutable.
 *
 * <p>Places are point numbers 1&ndash;24, plus two special locations that are
 * not points: {@link #BAR}, where hit checkers wait to re-enter, and
 * {@link #OFF}, the tray a checker leaves the board for when borne off. A move
 * never says which side is moving; that is always clear from whose turn it is,
 * and leaving it out keeps a move a plain value.</p>
 */
public final class Move {

    /** Location code for the bar. Only ever a move's source. */
    public static final int BAR = -1;

    /** Location code for the bear-off tray. Only ever a move's destination. */
    public static final int OFF = -2;

    /** Where the checker starts: a point, or {@link #BAR}. */
    private final int from;

    /** Where the checker ends: a point, or {@link #OFF}. */
    private final int to;

    /** The face of the die this move uses up. */
    private final int dieValue;

    /**
     * Creates a move.
     *
     * @param from     a point from 1 to 24, or {@link #BAR}
     * @param to       a point from 1 to 24, or {@link #OFF}
     * @param dieValue the die this move spends, from 1 to 6
     * @throws IllegalArgumentException if any argument is out of range
     */
    public Move(final int from, final int to, final int dieValue) {
        if (from != BAR && !Board.isPoint(from)) {
            throw new IllegalArgumentException("Bad source: " + from);
        }
        if (to != OFF && !Board.isPoint(to)) {
            throw new IllegalArgumentException("Bad destination: " + to);
        }
        if (dieValue < Die.MIN_VALUE || dieValue > Die.MAX_VALUE) {
            throw new IllegalArgumentException("Bad die value: " + dieValue);
        }
        this.from = from;
        this.to = to;
        this.dieValue = dieValue;
    }

    /**
     * Returns where the checker starts.
     *
     * @return a point from 1 to 24, or {@link #BAR}
     */
    public int getFrom() {
        return from;
    }

    /**
     * Returns where the checker ends.
     *
     * @return a point from 1 to 24, or {@link #OFF}
     */
    public int getTo() {
        return to;
    }

    /**
     * Returns the die this move spends.
     *
     * @return a value from 1 to 6
     */
    public int getDieValue() {
        return dieValue;
    }

    /**
     * Returns whether this move brings a checker in from the bar.
     *
     * @return {@code true} if the source is {@link #BAR}
     */
    public boolean isEntry() {
        return from == BAR;
    }

    /**
     * Returns whether this move bears a checker off.
     *
     * @return {@code true} if the destination is {@link #OFF}
     */
    public boolean isBearOff() {
        return to == OFF;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        // Moves are values: equal when every field matches.
        if (this == other) {
            return true;
        }
        if (!(other instanceof Move)) {
            return false;
        }
        final Move move = (Move) other;
        return from == move.from && to == move.to && dieValue == move.dieValue;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (from * 31 + to) * 31 + dieValue;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String source = from == BAR ? "bar" : Integer.toString(from);
        final String target = to == OFF ? "off" : Integer.toString(to);
        return "Move(" + source + "->" + target + " using " + dieValue + ")";
    }
}
