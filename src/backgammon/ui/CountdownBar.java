package backgammon.ui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A thin bar that shrinks as the time to answer runs out.
 *
 * <p>Drawn in the neutral text colour until the last stretch, then in red, so
 * players notice time running out without having to read a number.</p>
 */
public final class CountdownBar extends JComponent {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Height of the bar in pixels. */
    private static final int HEIGHT = 6;

    /** Fraction of time left below which the bar turns red. */
    private static final double URGENT_FRACTION = 0.25;

    /** How much of the time is left, from 0 to 1. */
    private double fraction;

    /** Creates a full bar. */
    public CountdownBar() {
        this.fraction = 1.0;
        setPreferredSize(new Dimension(100, HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
        setOpaque(false);
    }

    /**
     * Sets how much time is left and redraws.
     *
     * @param fraction from 0 (time up) to 1 (full time)
     */
    public void setFraction(final double fraction) {
        this.fraction = Math.max(0.0, Math.min(1.0, fraction));
        repaint();
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            final double width = getWidth();
            final double height = getHeight();
            g.setColor(Theme.COUNTDOWN_TRACK);
            g.fill(new RoundRectangle2D.Double(0, 0, width, height, height, height));
            final Color fill = fraction < URGENT_FRACTION ? Theme.COUNTDOWN_URGENT : Theme.TEXT_SECONDARY;
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Double(0, 0, width * fraction, height, height, height));
        } finally {
            g.dispose();
        }
    }
}
