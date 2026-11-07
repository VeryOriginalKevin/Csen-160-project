package backgammon.ui;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A rounded, clickable option on the title screen that is either on or off.
 *
 * <p>Drawn by hand rather than as a styled toggle button so it looks the same
 * on every platform's look and feel. Clicking it does not change it directly:
 * it tells its owner, which decides what should be selected (for example,
 * choosing "Random" clears every other choice) and sets each chip to match.</p>
 */
final class ChoiceChip extends JComponent {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Height of every chip. */
    private static final int HEIGHT = 34;

    /** Horizontal padding around the label. */
    private static final int PADDING = 14;

    /** The text on the chip. */
    private final String label;

    /** Whether the chip is currently chosen. */
    private boolean selected;

    /**
     * Creates a chip, initially not selected.
     *
     * @param label   the text to show
     * @param onClick run when the chip is clicked; the owner should then
     *                update every chip's selection
     */
    ChoiceChip(final String label, final Runnable onClick) {
        this.label = label;
        setFont(Theme.FONT_STATUS);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final FontMetrics metrics = getFontMetrics(getFont());
        setPreferredSize(new Dimension(metrics.stringWidth(label) + 2 * PADDING, HEIGHT));
        addMouseListener(new MouseAdapter() {
            /** Reports the click to the owner. */
            @Override
            public void mousePressed(final MouseEvent event) {
                onClick.run();
            }
        });
    }

    /**
     * Returns whether the chip is chosen.
     *
     * @return {@code true} if selected
     */
    boolean isSelected() {
        return selected;
    }

    /**
     * Turns the chip on or off and redraws it.
     *
     * @param selected whether the chip is chosen
     */
    void setSelected(final boolean selected) {
        this.selected = selected;
        repaint();
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            final RoundRectangle2D shape = new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2,
                    getHeight() - 2, getHeight() - 2);

            // Selected chips are filled gold; others are outlined.
            g.setColor(selected ? Theme.DIE_SELECTED : Theme.ANSWER_IDLE);
            g.fill(shape);
            if (!selected) {
                g.setColor(Theme.COUNTDOWN_TRACK);
                g.setStroke(new BasicStroke(1.2f));
                g.draw(shape);
            }

            g.setFont(getFont());
            final FontMetrics metrics = g.getFontMetrics();
            g.setColor(selected ? Theme.DIE_PIP : Theme.TEXT_PRIMARY);
            g.drawString(label, (getWidth() - metrics.stringWidth(label)) / 2,
                    (getHeight() + metrics.getAscent() - metrics.getDescent()) / 2);
        } finally {
            g.dispose();
        }
    }
}
