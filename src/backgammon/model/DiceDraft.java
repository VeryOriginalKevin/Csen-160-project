package backgammon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result of splitting a {@link DiceRoll}: the two dice the trivia winner
 * picked, and the two left for the loser. Immutable.
 *
 * <p>The positions of the winner's picks are kept, not just their values, so
 * the display can show exactly which of the four dice went to whom even when
 * two dice show the same face.</p>
 */
public final class DiceDraft {

    /** Positions, within the roll, of the dice the winner picked. */
    private final List<Integer> winnerPositions;

    /** The winner's two dice, in roll order. */
    private final List<Die> winnerDice;

    /** The loser's two dice, in roll order. */
    private final List<Die> loserDice;

    /**
     * Splits a roll. Use {@link DiceRoll#draft(int, int)} rather than calling
     * this directly.
     *
     * @param roll   the four dice
     * @param first  position of the winner's first pick
     * @param second position of the winner's second pick
     * @throws IllegalArgumentException if a position is out of range or both are the same
     */
    DiceDraft(final DiceRoll roll, final int first, final int second) {
        if (first == second) {
            throw new IllegalArgumentException("Pick two different dice");
        }
        if (!isPosition(first) || !isPosition(second)) {
            throw new IllegalArgumentException("No die at " + first + " or " + second);
        }
        final List<Integer> picked = new ArrayList<>();
        final List<Die> winner = new ArrayList<>();
        final List<Die> loser = new ArrayList<>();
        // One pass in roll order puts every die in exactly one hand.
        for (int position = 0; position < DiceRoll.DICE_COUNT; position++) {
            if (position == first || position == second) {
                picked.add(position);
                winner.add(roll.get(position));
            } else {
                loser.add(roll.get(position));
            }
        }
        this.winnerPositions = Collections.unmodifiableList(picked);
        this.winnerDice = Collections.unmodifiableList(winner);
        this.loserDice = Collections.unmodifiableList(loser);
    }

    /**
     * Returns the dice the winner picked.
     *
     * @return two dice, in roll order
     */
    public List<Die> getWinnerDice() {
        return winnerDice;
    }

    /**
     * Returns the dice left for the loser.
     *
     * @return two dice, in roll order
     */
    public List<Die> getLoserDice() {
        return loserDice;
    }

    /**
     * Returns whether the die at a position of the roll went to the winner.
     *
     * @param position position from 0 to 3
     * @return {@code true} if the winner picked it, {@code false} if the loser has it
     */
    public boolean isWinnerDie(final int position) {
        return winnerPositions.contains(position);
    }

    /**
     * Returns whether a number is a valid position in a roll.
     *
     * @param position the number to test
     * @return {@code true} if it is from 0 to 3
     */
    private static boolean isPosition(final int position) {
        return position >= 0 && position < DiceRoll.DICE_COUNT;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "DiceDraft(winner=" + winnerDice + ", loser=" + loserDice + ")";
    }
}
