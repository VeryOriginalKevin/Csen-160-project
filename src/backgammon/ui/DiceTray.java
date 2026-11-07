package backgammon.ui;

import backgammon.app.GameController;
import backgammon.app.TurnPhase;
import backgammon.model.DiceDraft;
import backgammon.model.DiceRoll;
import backgammon.model.PlayerColor;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Optional;

/**
 * Shows the round's four dice, and lets the contest winner draft two of them.
 *
 * <p>Ring colours tell the story of the round: no ring while the question is
 * open; gold on the dice picked so far during the draft; and afterwards, each
 * die ringed in the accent of the player who owns it. Below the four dice, a
 * row of small dice shows the moves the current mover has left, which matters
 * most for doubles (two dice, four moves).</p>
 */
public final class DiceTray extends JComponent {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Edge length of each of the four main dice. */
    private static final int DIE_SIZE = 54;

    /** Gap between the main dice. */
    private static final int DIE_GAP = 16;

    /** Edge length of the small dice in the moves-left row. */
    private static final int SMALL_DIE_SIZE = 24;

    /** Gap between small dice. */
    private static final int SMALL_DIE_GAP = 8;

    /** Space above the main dice, leaving room for rings. */
    private static final int TOP_MARGIN = 8;

    /** Vertical gap between the main dice and the moves-left row. */
    private static final int ROW_GAP = 16;

    /** Width of the ring drawn around a die. */
    private static final float RING_WIDTH = 3.5f;

    /**
     * Pip positions on a unit square for each face, as {x, y} pairs from 0 to 1.
     * Index 0 is unused so a face value can index the table directly.
     */
    private static final double[][][] PIPS = {
        {},
        {{0.5, 0.5}},
        {{0.27, 0.27}, {0.73, 0.73}},
        {{0.27, 0.27}, {0.5, 0.5}, {0.73, 0.73}},
        {{0.27, 0.27}, {0.73, 0.27}, {0.27, 0.73}, {0.73, 0.73}},
        {{0.27, 0.27}, {0.73, 0.27}, {0.5, 0.5}, {0.27, 0.73}, {0.73, 0.73}},
        {{0.27, 0.25}, {0.73, 0.25}, {0.27, 0.5}, {0.73, 0.5}, {0.27, 0.75}, {0.73, 0.75}},
    };

    /** The game whose dice are shown. */
    private final GameController controller;

    /**
     * Creates the tray and starts listening for draft clicks.
     *
     * @param controller the game to show and to send picks to
     */
    public DiceTray(final GameController controller) {
        this.controller = controller;
        setPreferredSize(new Dimension(4 * DIE_SIZE + 3 * DIE_GAP,
                TOP_MARGIN + DIE_SIZE + ROW_GAP + SMALL_DIE_SIZE + TOP_MARGIN));
        setOpaque(false);
        addMouseListener(new MouseAdapter() {
            /** Sends a press on one of the four dice to the controller as a pick. */
            @Override
            public void mousePressed(final MouseEvent event) {
                for (int position = 0; position < DiceRoll.DICE_COUNT; position++) {
                    if (dieBounds(position).contains(event.getPoint())) {
                        controller.pickDie(position);
                        return;
                    }
                }
            }
        });
    }

    /**
     * Updates the cursor to show whether the dice can be clicked. Called by the
     * window whenever the game changes.
     */
    public void refresh() {
        final boolean drafting = controller.getPhase() == TurnPhase.DICE_DRAFT;
        setCursor(Cursor.getPredefinedCursor(drafting ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        repaint();
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintMainDice(g);
            paintRemainingMoves(g);
        } finally {
            g.dispose();
        }
    }

    /**
     * Paints the four dice with whatever ring the phase calls for.
     *
     * @param g the graphics to paint with
     */
    private void paintMainDice(final Graphics2D g) {
        final DiceRoll roll = controller.getRoll();
        final Optional<DiceDraft> draft = controller.getDraft();
        final List<Integer> picks = controller.getDraftPicks();
        final Optional<PlayerColor> active = controller.getActivePlayer();
        for (int position = 0; position < DiceRoll.DICE_COUNT; position++) {
            Color ring = null;
            boolean dimmed = false;
            if (draft.isPresent() && controller.getContestResult().isPresent()) {
                // After the draft: ring each die in its owner's colour, and dim
                // the dice of the player who is not moving right now.
                final PlayerColor winner = controller.getContestResult().get().getWinner();
                final PlayerColor owner = draft.get().isWinnerDie(position) ? winner : winner.opponent();
                ring = Theme.accentFor(owner);
                dimmed = active.isPresent() && active.get() != owner;
            } else if (picks.contains(position)) {
                ring = Theme.DIE_SELECTED;
            }
            final Rectangle2D bounds = dieBounds(position);
            paintDie(g, bounds, roll.get(position).getValue(), ring, dimmed);
        }
    }

    /**
     * Paints the small dice showing the moves the current mover has left.
     *
     * @param g the graphics to paint with
     */
    private void paintRemainingMoves(final Graphics2D g) {
        final List<Integer> remaining = controller.getRemainingDice();
        if (remaining.isEmpty()) {
            return;
        }
        final int rowWidth = remaining.size() * SMALL_DIE_SIZE + (remaining.size() - 1) * SMALL_DIE_GAP;
        double x = (getWidth() - rowWidth) / 2.0;
        final double y = TOP_MARGIN + DIE_SIZE + ROW_GAP;
        for (final int value : remaining) {
            paintDie(g, new Rectangle2D.Double(x, y, SMALL_DIE_SIZE, SMALL_DIE_SIZE), value, null, false);
            x += SMALL_DIE_SIZE + SMALL_DIE_GAP;
        }
    }

    /**
     * Returns where one of the four main dice is drawn, centred in the tray.
     *
     * @param position which die, from 0 to 3
     * @return its bounds
     */
    private Rectangle2D dieBounds(final int position) {
        final int rowWidth = DiceRoll.DICE_COUNT * DIE_SIZE + (DiceRoll.DICE_COUNT - 1) * DIE_GAP;
        final double left = (getWidth() - rowWidth) / 2.0;
        return new Rectangle2D.Double(left + position * (DIE_SIZE + DIE_GAP), TOP_MARGIN, DIE_SIZE, DIE_SIZE);
    }

    /**
     * Paints one die: rounded face, pips, and an optional ring.
     *
     * @param g      the graphics to paint with
     * @param bounds where the die goes
     * @param value  the face showing, from 1 to 6
     * @param ring   the ring colour, or {@code null} for none
     * @param dimmed whether to fade the die, for dice not in play right now
     */
    private static void paintDie(final Graphics2D g, final Rectangle2D bounds, final int value,
                                 final Color ring, final boolean dimmed) {
        final double size = bounds.getWidth();
        final double arc = size * 0.25;
        final RoundRectangle2D face = new RoundRectangle2D.Double(bounds.getX(), bounds.getY(), size, size, arc, arc);
        g.setColor(dimmed ? Theme.DIE_FACE.darker().darker() : Theme.DIE_FACE);
        g.fill(face);

        // Pips scale with the die so the small dice read as well as the big ones.
        final double pip = size * 0.17;
        g.setColor(Theme.DIE_PIP);
        for (final double[] spot : PIPS[value]) {
            g.fill(new Ellipse2D.Double(bounds.getX() + spot[0] * size - pip / 2,
                    bounds.getY() + spot[1] * size - pip / 2, pip, pip));
        }

        if (ring != null) {
            g.setColor(ring);
            g.setStroke(new BasicStroke(RING_WIDTH));
            final double grow = RING_WIDTH;
            g.draw(new RoundRectangle2D.Double(bounds.getX() - grow, bounds.getY() - grow,
                    size + 2 * grow, size + 2 * grow, arc + grow, arc + grow));
        }
    }
}
