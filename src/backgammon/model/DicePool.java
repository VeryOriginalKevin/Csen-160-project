package backgammon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The die values a player still has to move with during their turn.
 *
 * <p>Built from the player's two dice. As in ordinary backgammon, a matching
 * pair counts double: two fives give four moves of five. Each move spends one
 * value from the pool.</p>
 */
public final class DicePool {

    /** How many moves a matching pair gives. */
    public static final int MOVES_FOR_DOUBLES = 4;

    /** Values not yet spent, in no particular order. */
    private final List<Integer> remaining;

    /**
     * Creates a pool from a player's two dice.
     *
     * @param dice exactly two dice
     * @throws IllegalArgumentException if there are not exactly two
     */
    public DicePool(final List<Die> dice) {
        if (dice.size() != 2) {
            throw new IllegalArgumentException("A pool is built from two dice, got " + dice.size());
        }
        this.remaining = new ArrayList<>();
        final int first = dice.get(0).getValue();
        final int second = dice.get(1).getValue();
        if (first == second) {
            // Doubles: the one value may be moved four times.
            for (int i = 0; i < MOVES_FOR_DOUBLES; i++) {
                remaining.add(first);
            }
        } else {
            remaining.add(first);
            remaining.add(second);
        }
    }

    /**
     * Returns the values still to be moved, largest first.
     *
     * @return an unmodifiable snapshot; repeats appear once per remaining move
     */
    public List<Integer> getRemaining() {
        final List<Integer> sorted = new ArrayList<>(remaining);
        sorted.sort(Collections.reverseOrder());
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Returns each value still available, once.
     *
     * @return the distinct remaining values, smallest first
     */
    public SortedSet<Integer> getDistinctValues() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(remaining));
    }

    /**
     * Returns whether a value is still available.
     *
     * @param value a die face
     * @return {@code true} if at least one move with that value remains
     */
    public boolean contains(final int value) {
        return remaining.contains(value);
    }

    /**
     * Returns whether every move has been spent.
     *
     * @return {@code true} once the pool is empty
     */
    public boolean isEmpty() {
        return remaining.isEmpty();
    }

    /**
     * Spends one move of a value.
     *
     * @param value the die face being used
     * @throws IllegalArgumentException if no move of that value remains
     */
    public void spend(final int value) {
        // Integer.valueOf selects remove(Object) rather than remove(int index).
        if (!remaining.remove(Integer.valueOf(value))) {
            throw new IllegalArgumentException("No " + value + " left in " + remaining);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DicePool" + getRemaining();
    }
}
