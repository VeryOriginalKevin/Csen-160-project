package backgammon.model;

/**
 * One rolled die: an immutable value from one to six.
 *
 * <p>Dice are values rather than mutable objects, so a drafted die can be handed
 * to a player with no risk of the board or the UI changing it afterwards.</p>
 */
public final class Die {

    /** Lowest face a die can show. */
    public static final int MIN_VALUE = 1;

    /** Highest face a die can show. */
    public static final int MAX_VALUE = 6;

    /** The face showing, between {@link #MIN_VALUE} and {@link #MAX_VALUE}. */
    private final int value;

    /**
     * Creates a die showing the given face.
     *
     * @param value the face showing, from {@value #MIN_VALUE} to {@value #MAX_VALUE}
     * @throws IllegalArgumentException if {@code value} is outside that range
     */
    public Die(final int value) {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException("Die value out of range: " + value);
        }
        this.value = value;
    }

    /**
     * Returns the face showing.
     *
     * @return the die's value
     */
    public int getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object other) {
        // Two dice are interchangeable when they show the same face.
        if (this == other) {
            return true;
        }
        if (!(other instanceof Die)) {
            return false;
        }
        return value == ((Die) other).value;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "Die(" + value + ")";
    }
}
