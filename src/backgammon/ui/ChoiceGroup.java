package backgammon.ui;

import javax.swing.JPanel;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * A grid of chips where players choose any number of options, or "Random".
 *
 * <p>The first chip is always "Random", meaning no restriction. Choosing a
 * specific option turns Random off; choosing Random clears every specific
 * option; and un-choosing the last specific option turns Random back on, so
 * the group can never be left with nothing chosen. Used on the title screen
 * for both topics and difficulties.</p>
 *
 * @param <T> the kind of option offered
 */
final class ChoiceGroup<T> extends JPanel {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Space between chips, in pixels. */
    private static final int GAP = 8;

    /** The "Random" chip, first in the grid. */
    private final ChoiceChip randomChip;

    /** The options, in display order, matching {@link #optionChips}. */
    private final List<T> options;

    /** One chip per option. */
    private final List<ChoiceChip> optionChips;

    /**
     * Builds the group with Random chosen.
     *
     * @param options the options to offer, in display order
     * @param labeler turns an option into the text on its chip
     * @param columns how many chips per row
     */
    ChoiceGroup(final List<T> options, final Function<T, String> labeler, final int columns) {
        super(new GridLayout(0, columns, GAP, GAP));
        setOpaque(false);
        this.options = new ArrayList<>(options);
        this.optionChips = new ArrayList<>();
        this.randomChip = new ChoiceChip("Random", this::chooseRandom);
        add(randomChip);
        for (final T option : options) {
            // The chip's index in optionChips is the option's index in options.
            final int index = optionChips.size();
            final ChoiceChip chip = new ChoiceChip(labeler.apply(option), () -> toggle(index));
            optionChips.add(chip);
            add(chip);
        }
        chooseRandom();
    }

    /**
     * Returns the options chosen.
     *
     * @return the chosen options in display order, or an empty set if Random is chosen
     */
    Set<T> getSelected() {
        final Set<T> chosen = new LinkedHashSet<>();
        for (int i = 0; i < options.size(); i++) {
            if (optionChips.get(i).isSelected()) {
                chosen.add(options.get(i));
            }
        }
        return chosen;
    }

    /** Chooses Random, clearing every specific option. */
    private void chooseRandom() {
        for (final ChoiceChip chip : optionChips) {
            chip.setSelected(false);
        }
        randomChip.setSelected(true);
    }

    /**
     * Flips one specific option, keeping Random consistent with the rest.
     *
     * @param index which option, as an index into {@link #options}
     */
    private void toggle(final int index) {
        final ChoiceChip chip = optionChips.get(index);
        chip.setSelected(!chip.isSelected());
        if (getSelected().isEmpty()) {
            // Nothing specific left: fall back to Random rather than nothing.
            chooseRandom();
        } else {
            randomChip.setSelected(false);
        }
    }
}
