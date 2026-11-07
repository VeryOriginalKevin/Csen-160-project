package backgammon.ui;

import backgammon.app.GameController;
import backgammon.app.TurnPhase;
import backgammon.model.Board;
import backgammon.model.Move;
import backgammon.model.PlayerColor;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Draws the backgammon board and turns clicks on it into moves.
 *
 * <p>Everything is painted with Java2D from {@link BoardGeometry} and
 * {@link Theme}; there are no images. During movement the panel also shows
 * which stacks can move (a faint ring), the selected checker (a bold ring), and
 * where it may go (tinted points or tray).</p>
 *
 * <p>The panel holds no game state of its own: each repaint reads the
 * controller afresh.</p>
 */
public final class BoardPanel extends JComponent {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Size the panel asks for when the window is first laid out. */
    private static final Dimension PREFERRED_SIZE = new Dimension(860, 700);

    /** The game being shown. */
    private final GameController controller;

    /**
     * Creates the panel and starts listening for clicks.
     *
     * @param controller the game to draw and to send clicks to
     */
    public BoardPanel(final GameController controller) {
        this.controller = controller;
        setPreferredSize(PREFERRED_SIZE);
        addMouseListener(new MouseAdapter() {
            /** Forwards a press on a board location to the controller. */
            @Override
            public void mousePressed(final MouseEvent event) {
                final int location = currentGeometry().locationAt(event.getX(), event.getY());
                if (location != BoardGeometry.NOWHERE) {
                    controller.clickLocation(location);
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    protected void paintComponent(final Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            final BoardGeometry geometry = currentGeometry();

            // Back to front: background, wood, felt, points, highlights, checkers, overlays.
            g.setColor(Theme.WINDOW_BACKGROUND);
            g.fillRect(0, 0, getWidth(), getHeight());
            paintFrameAndSurface(g, geometry);
            paintPoints(g, geometry);
            paintTargets(g, geometry);
            paintCheckers(g, geometry);
            paintSelection(g, geometry);
            if (controller.getPhase() == TurnPhase.GAME_OVER) {
                paintGameOver(g);
            }
        } finally {
            g.dispose();
        }
    }

    /**
     * Returns the layout for the panel's current size.
     *
     * @return a geometry matching the panel's width and height
     */
    private BoardGeometry currentGeometry() {
        return new BoardGeometry(getWidth(), getHeight());
    }

    /**
     * Paints the wooden frame, the two felt halves, and the trays.
     *
     * @param g        the graphics to paint with
     * @param geometry the layout
     */
    private void paintFrameAndSurface(final Graphics2D g, final BoardGeometry geometry) {
        final Rectangle2D board = geometry.getBoardBounds();
        g.setColor(Theme.BOARD_FRAME);
        g.fill(new RoundRectangle2D.Double(board.getX(), board.getY(),
                board.getWidth(), board.getHeight(), 14, 14));

        // The bar is simply the frame showing through between the two halves.
        g.setColor(Theme.BOARD_SURFACE);
        g.fill(geometry.getHalfBounds(true));
        g.fill(geometry.getHalfBounds(false));

        g.setColor(Theme.TRAY_SURFACE);
        for (final PlayerColor color : PlayerColor.values()) {
            final Rectangle2D tray = geometry.getTrayBounds(color);
            g.fill(new RoundRectangle2D.Double(tray.getX(), tray.getY(),
                    tray.getWidth(), tray.getHeight(), 8, 8));
        }
    }

    /**
     * Paints the 24 triangles and their numbers on the frame.
     *
     * @param g        the graphics to paint with
     * @param geometry the layout
     */
    private void paintPoints(final Graphics2D g, final BoardGeometry geometry) {
        g.setFont(Theme.FONT_POINT_NUMBER);
        final FontMetrics metrics = g.getFontMetrics();
        for (int point = 1; point <= Board.POINT_COUNT; point++) {
            g.setColor(point % 2 == 1 ? Theme.POINT_LIGHT : Theme.POINT_DARK);
            g.fill(triangle(geometry, point));

            // Number centred in the frame above or below the point.
            final String label = Integer.toString(point);
            final Point2D centre = geometry.getPointNumberCenter(point);
            g.setColor(Theme.POINT_NUMBER);
            g.drawString(label,
                    (float) (centre.getX() - metrics.stringWidth(label) / 2.0),
                    (float) (centre.getY() + metrics.getAscent() / 2.0 - 1));
        }
    }

    /**
     * Tints every place the selected checker may move to.
     *
     * @param g        the graphics to paint with
     * @param geometry the layout
     */
    private void paintTargets(final Graphics2D g, final BoardGeometry geometry) {
        g.setColor(Theme.MOVE_TARGET);
        for (final int target : controller.getLegalTargets()) {
            if (target == Move.OFF) {
                g.fill(geometry.getTrayBounds(controller.getActivePlayer().orElseThrow()));
            } else {
                g.fill(triangle(geometry, target));
            }
        }
    }

    /**
     * Paints every checker: on the points, on the bar, and in the trays.
     *
     * @param g        the graphics to paint with
     * @param geometry the layout
     */
    private void paintCheckers(final Graphics2D g, final BoardGeometry geometry) {
        final Board board = controller.getBoard();
        final Set<Integer> movable = controller.getLegalSources();

        // Points.
        for (int point = 1; point <= Board.POINT_COUNT; point++) {
            final PlayerColor owner = board.ownerOf(point);
            if (owner == null) {
                continue;
            }
            final int count = board.countAt(point, owner);
            final int visible = Math.min(count, BoardGeometry.MAX_VISIBLE_STACK);
            for (int i = 0; i < visible; i++) {
                paintChecker(g, geometry, geometry.getCheckerCenter(point, i), owner);
            }
            final Point2D topChecker = geometry.getCheckerCenter(point, visible - 1);
            if (count > visible) {
                paintCount(g, geometry, topChecker, owner, count);
            }
            if (movable.contains(point)) {
                paintRing(g, geometry, topChecker, Theme.MOVABLE_RING, 2f);
            }
        }

        // Bar.
        for (final PlayerColor color : PlayerColor.values()) {
            final int count = board.barCount(color);
            final int visible = Math.min(count, BoardGeometry.MAX_VISIBLE_STACK);
            for (int i = 0; i < visible; i++) {
                paintChecker(g, geometry, geometry.getBarCheckerCenter(color, i), color);
            }
            if (count > visible) {
                paintCount(g, geometry, geometry.getBarCheckerCenter(color, visible - 1), color, count);
            }
        }
        if (movable.contains(Move.BAR)) {
            final PlayerColor mover = controller.getActivePlayer().orElseThrow();
            final int top = Math.min(board.barCount(mover), BoardGeometry.MAX_VISIBLE_STACK) - 1;
            paintRing(g, geometry, geometry.getBarCheckerCenter(mover, top), Theme.MOVABLE_RING, 2f);
        }

        // Trays: borne-off checkers lie flat, so they are drawn as slabs.
        for (final PlayerColor color : PlayerColor.values()) {
            for (int i = 0; i < board.offCount(color); i++) {
                final Rectangle2D slab = geometry.getTraySlab(color, i);
                final RoundRectangle2D shape = new RoundRectangle2D.Double(slab.getX(), slab.getY() + 1,
                        slab.getWidth(), Math.max(1, slab.getHeight() - 2), 4, 4);
                g.setColor(Theme.checkerFor(color));
                g.fill(shape);
                g.setColor(Theme.CHECKER_OUTLINE);
                g.draw(shape);
            }
        }
    }

    /**
     * Draws a bold ring around the selected checker.
     *
     * @param g        the graphics to paint with
     * @param geometry the layout
     */
    private void paintSelection(final Graphics2D g, final BoardGeometry geometry) {
        final OptionalInt selected = controller.getSelectedSource();
        if (selected.isEmpty()) {
            return;
        }
        final Board board = controller.getBoard();
        final PlayerColor mover = controller.getActivePlayer().orElseThrow();
        final Point2D centre;
        if (selected.getAsInt() == Move.BAR) {
            final int top = Math.min(board.barCount(mover), BoardGeometry.MAX_VISIBLE_STACK) - 1;
            centre = geometry.getBarCheckerCenter(mover, top);
        } else {
            final int point = selected.getAsInt();
            final int top = Math.min(board.countAt(point, mover), BoardGeometry.MAX_VISIBLE_STACK) - 1;
            centre = geometry.getCheckerCenter(point, top);
        }
        paintRing(g, geometry, centre, Theme.DIE_SELECTED, 3.5f);
    }

    /**
     * Shades the board and announces the winner.
     *
     * @param g the graphics to paint with
     */
    private void paintGameOver(final Graphics2D g) {
        g.setColor(Theme.BANNER_SHADE);
        g.fillRect(0, 0, getWidth(), getHeight());
        final String text = controller.getWinner()
                .map(winner -> GameController.nameOf(winner) + " wins!")
                .orElse("Game over");
        g.setFont(Theme.FONT_BANNER);
        final FontMetrics metrics = g.getFontMetrics();
        g.setColor(controller.getWinner().map(Theme::accentFor).orElse(Theme.TEXT_PRIMARY));
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2,
                (getHeight() + metrics.getAscent()) / 2);
    }

    /**
     * Paints one checker as a disc with an outline and a faint inner ring.
     *
     * @param g        the graphics to paint with
     * @param geometry the layout, for the checker size
     * @param centre   where the checker's centre is
     * @param color    whose checker it is
     */
    private void paintChecker(final Graphics2D g, final BoardGeometry geometry,
                              final Point2D centre, final PlayerColor color) {
        final double d = geometry.getCheckerDiameter();
        final Ellipse2D disc = circle(centre, d);
        g.setColor(Theme.checkerFor(color));
        g.fill(disc);
        // The inner ring gives the flat disc a hint of a raised rim.
        g.setColor(color == PlayerColor.WHITE ? new Color(0, 0, 0, 0x22) : new Color(255, 255, 255, 0x22));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(circle(centre, d * 0.62));
        g.setColor(Theme.CHECKER_OUTLINE);
        g.setStroke(new BasicStroke(1.2f));
        g.draw(disc);
    }

    /**
     * Writes a stack's true size on its top checker, for stacks too tall to draw.
     *
     * @param g        the graphics to paint with
     * @param geometry the layout
     * @param centre   the top checker's centre
     * @param color    whose stack it is, which decides the text colour
     * @param count    how many checkers are really there
     */
    private void paintCount(final Graphics2D g, final BoardGeometry geometry,
                            final Point2D centre, final PlayerColor color, final int count) {
        g.setFont(Theme.FONT_STACK_COUNT);
        final FontMetrics metrics = g.getFontMetrics();
        final String text = Integer.toString(count);
        g.setColor(color == PlayerColor.WHITE ? Theme.CHECKER_BLACK : Theme.CHECKER_WHITE);
        g.drawString(text,
                (float) (centre.getX() - metrics.stringWidth(text) / 2.0),
                (float) (centre.getY() + metrics.getAscent() / 2.0 - 1));
    }

    /**
     * Draws a ring around a checker.
     *
     * @param g        the graphics to paint with
     * @param geometry the layout, for the checker size
     * @param centre   the checker's centre
     * @param color    the ring's colour
     * @param width    the ring's stroke width
     */
    private void paintRing(final Graphics2D g, final BoardGeometry geometry,
                           final Point2D centre, final Color color, final float width) {
        g.setColor(color);
        g.setStroke(new BasicStroke(width));
        g.draw(circle(centre, geometry.getCheckerDiameter() + width));
    }

    /**
     * Builds a point's triangle, with its base on the board edge.
     *
     * @param geometry the layout
     * @param point    a point from 1 to 24
     * @return the triangle
     */
    private static Path2D triangle(final BoardGeometry geometry, final int point) {
        final Rectangle2D bounds = geometry.getPointBounds(point);
        final double baseY = geometry.isTopRow(point) ? bounds.getMinY() : bounds.getMaxY();
        final double tipY = geometry.isTopRow(point) ? bounds.getMaxY() : bounds.getMinY();
        final Path2D path = new Path2D.Double();
        path.moveTo(bounds.getMinX(), baseY);
        path.lineTo(bounds.getMaxX(), baseY);
        path.lineTo(bounds.getCenterX(), tipY);
        path.closePath();
        return path;
    }

    /**
     * Builds a circle around a centre.
     *
     * @param centre   the centre
     * @param diameter the diameter
     * @return the circle
     */
    private static Ellipse2D circle(final Point2D centre, final double diameter) {
        return new Ellipse2D.Double(centre.getX() - diameter / 2, centre.getY() - diameter / 2,
                diameter, diameter);
    }
}
