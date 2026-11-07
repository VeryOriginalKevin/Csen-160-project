package backgammon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The four dice rolled at the start of every round: immutable.
 *
 * <p>In this variant a round begins with four dice instead of two. The trivia
 * winner {@linkplain #draft(int, int) drafts} two of them and the loser keeps
 * the other two.</p>
 */
public final class DiceRoll {

    /** How many dice are rolled each round. */
    public static final int DICE_COUNT = 4;

    /** The four dice, in the order they are displayed. */
    private final List<Die> dice;

    /**
     * Creates a roll from four known dice.
     *
     * @param dice exactly four dice, in display order
     * @throws IllegalArgumentException if there are not exactly four
     */
    public DiceRoll(final List<Die> dice) {
        if (dice.size() != DICE_COUNT) {
            throw new IllegalArgumentException("Need " + DICE_COUNT + " dice, got " + dice.size());
        }
        this.dice = Collections.unmodifiableList(new ArrayList<>(dice));
    }

    /**
     * Rolls four fresh dice.
     *
     * @param random randomness for the roll; pass a seeded {@code Random} to make
     *               a test deterministic
     * @return the new roll
     */
    public static DiceRoll roll(final Random random) {
        final List<Die> rolled = new ArrayList<>(DICE_COUNT);
        for (int i = 0; i < DICE_COUNT; i++) {
            rolled.add(new Die(Die.MIN_VALUE + random.nextInt(Die.MAX_VALUE)));
        }
        return new DiceRoll(rolled);
    }

    /**
     * Returns one die.
     *
     * @param index position from 0 to 3
     * @return the die at that position
     */
    public Die get(final int index) {
        return dice.get(index);
    }

    /**
     * Splits the roll: the winner takes the two dice named, the loser the rest.
     *
     * @param first  position of the winner's first pick, from 0 to 3
     * @param second position of the winner's second pick, different from {@code first}
     * @return the split
     * @throws IllegalArgumentException if a position is out of range or both are the same
     */
    public DiceDraft draft(final int first, final int second) {
        return new DiceDraft(this, first, second);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DiceRoll" + dice;
    }
}
