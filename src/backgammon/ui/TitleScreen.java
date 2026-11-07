package backgammon.ui;

import backgammon.model.PlayerColor;
import backgammon.trivia.Difficulty;
import backgammon.trivia.TriviaCategory;
import backgammon.trivia.TriviaSettings;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * The first thing players see: the game's name, the trivia options, and a
 * Start button.
 *
 * <p>Players choose which trivia topics and difficulties to play with. Each
 * group starts on "Random", meaning any; choosing specific options narrows the
 * questions to those. Pressing Start hands the choice to whoever is listening,
 * which starts the game.</p>
 */
public final class TitleScreen extends JPanel {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Width of the centred column holding everything. */
    private static final int COLUMN_WIDTH = 760;

    /** Font for the game's name. */
    private static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 46);

    /** Font for section headings. */
    private static final Font FONT_HEADING = new Font(Font.SANS_SERIF, Font.BOLD, 16);

    /** Topic chips per row. */
    private static final int TOPIC_COLUMNS = 5;

    /** Topic choices. */
    private final ChoiceGroup<TriviaCategory> topics;

    /** Difficulty choices. */
    private final ChoiceGroup<Difficulty> difficulties;

    /**
     * Builds the screen.
     *
     * @param onStart told the players' choice when they press Start
     */
    public TitleScreen(final Consumer<TriviaSettings> onStart) {
        // GridBagLayout with no constraints centres the column in the window.
        super(new GridBagLayout());
        setBackground(Theme.WINDOW_BACKGROUND);

        topics = new ChoiceGroup<>(Arrays.asList(TriviaCategory.values()),
                TriviaCategory::getDisplayName, TOPIC_COLUMNS);
        difficulties = new ChoiceGroup<>(Arrays.asList(Difficulty.values()),
                Difficulty::getDisplayLabel, Difficulty.values().length + 1);

        final JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);

        addLeft(column, label("Triviagammon", FONT_TITLE, Theme.TEXT_PRIMARY));
        column.add(Box.createVerticalStrut(6));
        addLeft(column, label("Backgammon where a trivia race decides who moves first and picks the dice.",
                Theme.FONT_ANSWER, Theme.TEXT_SECONDARY));
        column.add(Box.createVerticalStrut(34));

        addLeft(column, label("Topics", FONT_HEADING, Theme.TEXT_PRIMARY));
        column.add(Box.createVerticalStrut(10));
        addLeft(column, topics);
        column.add(Box.createVerticalStrut(28));

        addLeft(column, label("Difficulty", FONT_HEADING, Theme.TEXT_PRIMARY));
        column.add(Box.createVerticalStrut(10));
        addLeft(column, difficulties);
        column.add(Box.createVerticalStrut(34));

        addLeft(column, startButton(onStart));
        column.add(Box.createVerticalStrut(16));
        addLeft(column, label("White answers with keys " + AnswerKeys.rangeLabel(PlayerColor.WHITE)
                + ", Black with " + AnswerKeys.rangeLabel(PlayerColor.BLACK)
                + ". Pick several topics or difficulties to mix them.",
                Theme.FONT_STATUS, Theme.TEXT_SECONDARY));

        // Fix the column's width so the chip grids lay out evenly.
        column.setPreferredSize(new Dimension(COLUMN_WIDTH, column.getPreferredSize().height));
        add(column);
    }

    /**
     * Builds the Start button.
     *
     * @param onStart told the players' choice when the button is pressed
     * @return the button
     */
    private JButton startButton(final Consumer<TriviaSettings> onStart) {
        final JButton start = new JButton("Start game");
        start.setFont(Theme.FONT_PROMPT);
        start.setForeground(Theme.DIE_PIP);
        start.setBackground(Theme.DIE_SELECTED);
        start.setFocusPainted(false);
        start.setBorder(BorderFactory.createEmptyBorder(12, 34, 12, 34));
        start.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        start.addActionListener(event ->
                onStart.accept(new TriviaSettings(topics.getSelected(), difficulties.getSelected())));
        return start;
    }

    /**
     * Builds a plain label.
     *
     * @param text   the text
     * @param font   its font
     * @param colour its colour
     * @return the label
     */
    private static JLabel label(final String text, final Font font, final Color colour) {
        final JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(colour);
        return label;
    }

    /**
     * Adds a child to a vertical box, aligned to the left edge.
     *
     * @param box       the box
     * @param component the child
     */
    private static void addLeft(final JPanel box, final JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        box.add(component);
    }
}
