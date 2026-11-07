package backgammon.ui;

import backgammon.model.Board;
import backgammon.model.Move;
import backgammon.model.PlayerColor;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Where everything on the board sits on screen, for one panel size.
 *
 * <p>Both painting and click handling ask this class where things are, so the
 * picture and the click targets can never drift apart. It is recomputed
 * whenever the panel is resized, which keeps the board scaling cleanly.</p>
 *
 * <p>Layout, left to right: frame, the outer board (points 13&ndash;18 on top,
 * 12&ndash;7 below), the bar, the home boards (19&ndash;24 on top,
 * 6&ndash;1 below), frame, the bear-off trays, frame. {@code WHITE} travels
 * anticlockwise along the bottom toward point 1 at the bottom right and bears
 * off into the lower tray; {@code BLACK} mirrors that along the top.</p>
 *
 * <pre>
 *  13 14 15 16 17 18 | bar | 19 20 21 22 23 24 | BLACK tray
 *  12 11 10  9  8  7 | bar |  6  5  4  3  2  1 | WHITE tray
 * </pre>
 *
 * <p>Immutable.</p>
 */
public final class BoardGeometry {

    /** Returned by {@link #locationAt} for a spot that is no board location. */
    public static final int NOWHERE = Integer.MIN_VALUE;

    /** Most checkers drawn in one column; taller stacks show a count instead. */
    public static final int MAX_VISIBLE_STACK = 5;

    /** Points per row of the board. */
    private static final int POINTS_PER_ROW = 12;

    /** Points on each side of the bar within a row. */
    private static final int POINTS_PER_QUARTER = 6;

    /** Tray width as a fraction of the point width. */
    private static final double TRAY_WIDTH_IN_POINTS = 1.3;

    /** Point height as a fraction of the playing area's height. */
    private static final double POINT_HEIGHT_FRACTION = 0.42;

    /** Thickness of the frame around and between the areas. */
    private final double frame;

    /** Width of one point, which is also the width of the bar. */
    private final double pointWidth;

    /** Height of one point's triangle. */
    private final double pointHeight;

    /** Width of each bear-off tray. */
    private final double trayWidth;

    /** Top edge of the playing area. */
    private final double top;

    /** Height of the playing area. */
    private final double innerHeight;

    /** Diameter of one checker. */
    private final double checkerDiameter;

    /**
     * Lays the board out within a panel.
     *
     * @param width  the panel's width in pixels
     * @param height the panel's height in pixels
     */
    public BoardGeometry(final int width, final int height) {
        // The frame grows a little with the window, within sensible limits.
        this.frame = Math.max(20.0, Math.min(width, height) * 0.035);
        // Width budget: 3 frame strips, 12 points, the bar (one point wide), the tray.
        final double usable = width - 3 * frame;
        this.pointWidth = usable / (POINTS_PER_ROW + 1 + TRAY_WIDTH_IN_POINTS);
        this.trayWidth = pointWidth * TRAY_WIDTH_IN_POINTS;
        this.top = frame;
        this.innerHeight = height - 2 * frame;
        this.pointHeight = innerHeight * POINT_HEIGHT_FRACTION;
        // Checkers fit the point's width, and five of them fit its height.
        this.checkerDiameter = Math.min(pointWidth * 0.92, pointHeight / MAX_VISIBLE_STACK);
    }

    /**
     * Returns the diameter every checker is drawn at.
     *
     * @return the diameter in pixels
     */
    public double getCheckerDiameter() {
        return checkerDiameter;
    }

    /**
     * Returns the rectangle the whole wooden board occupies.
     *
     * @return the board's outer bounds
     */
    public Rectangle2D getBoardBounds() {
        return new Rectangle2D.Double(0, 0, playingRight() + 2 * frame + trayWidth, innerHeight + 2 * frame);
    }

    /**
     * Returns the playing surface on one side of the bar.
     *
     * @param leftHalf {@code true} for the outer boards, {@code false} for the home boards
     * @return that half's bounds
     */
    public Rectangle2D getHalfBounds(final boolean leftHalf) {
        final double x = leftHalf ? frame : columnX(POINTS_PER_QUARTER);
        return new Rectangle2D.Double(x, top, POINTS_PER_QUARTER * pointWidth, innerHeight);
    }

    /**
     * Returns the strip a point's triangle is drawn in.
     *
     * @param point a point from 1 to 24
     * @return the triangle's bounding box; the triangle's base is on the board edge
     */
    public Rectangle2D getPointBounds(final int point) {
        final double y = isTopRow(point) ? top : top + innerHeight - pointHeight;
        return new Rectangle2D.Double(columnX(columnOf(point)), y, pointWidth, pointHeight);
    }

    /**
     * Returns whether a point is in the top row, where its triangle hangs down.
     *
     * @param point a point from 1 to 24
     * @return {@code true} for points 13&ndash;24
     */
    public boolean isTopRow(final int point) {
        return point > POINTS_PER_ROW;
    }

    /**
     * Returns where the n-th checker of a point's stack is drawn.
     *
     * @param point the point, from 1 to 24
     * @param index position in the stack from the board edge, from 0; indices at
     *              or past {@value #MAX_VISIBLE_STACK} share the last slot
     * @return the checker's centre
     */
    public Point2D getCheckerCenter(final int point, final int index) {
        final int slot = Math.min(index, MAX_VISIBLE_STACK - 1);
        final double x = columnX(columnOf(point)) + pointWidth / 2;
        final double offset = checkerDiameter / 2 + slot * checkerDiameter;
        final double y = isTopRow(point) ? top + offset : top + innerHeight - offset;
        return new Point2D.Double(x, y);
    }

    /**
     * Returns the bar's rectangle.
     *
     * @return the bar's bounds
     */
    public Rectangle2D getBarBounds() {
        return new Rectangle2D.Double(columnX(POINTS_PER_QUARTER) - pointWidth, top, pointWidth, innerHeight);
    }

    /**
     * Returns where the n-th checker on a side's bar is drawn. {@code WHITE}
     * waits in the top half, beside the quarter it enters into, and
     * {@code BLACK} in the bottom half.
     *
     * @param color the side
     * @param index position in that side's bar stack, from 0
     * @return the checker's centre
     */
    public Point2D getBarCheckerCenter(final PlayerColor color, final int index) {
        final int slot = Math.min(index, MAX_VISIBLE_STACK - 1);
        final double x = getBarBounds().getCenterX();
        final double middle = top + innerHeight / 2;
        final double offset = checkerDiameter * 0.75 + slot * checkerDiameter;
        final double y = color == PlayerColor.WHITE ? middle - offset : middle + offset;
        return new Point2D.Double(x, y);
    }

    /**
     * Returns a side's bear-off tray. {@code BLACK}'s is the upper one,
     * {@code WHITE}'s the lower, each beside its home board.
     *
     * @param color the side
     * @return the tray's bounds
     */
    public Rectangle2D getTrayBounds(final PlayerColor color) {
        final double x = playingRight() + frame;
        final double gap = frame / 2;
        final double height = innerHeight / 2 - gap;
        final double y = color == PlayerColor.BLACK ? top : top + innerHeight - height;
        return new Rectangle2D.Double(x, y, trayWidth, height);
    }

    /**
     * Returns the slab the n-th borne-off checker is drawn as.
     *
     * @param color the side
     * @param index which borne-off checker, from 0
     * @return the slab's bounds, stacked from the outer edge of the tray inward
     */
    public Rectangle2D getTraySlab(final PlayerColor color, final int index) {
        final Rectangle2D tray = getTrayBounds(color);
        final double inset = tray.getWidth() * 0.12;
        final double slabHeight = (tray.getHeight() - 2 * inset) / Board.CHECKERS_PER_SIDE;
        final double y = color == PlayerColor.BLACK
                ? tray.getY() + inset + index * slabHeight
                : tray.getMaxY() - inset - (index + 1) * slabHeight;
        return new Rectangle2D.Double(tray.getX() + inset, y, tray.getWidth() - 2 * inset, slabHeight);
    }

    /**
     * Returns where a point's number is printed on the frame.
     *
     * @param point a point from 1 to 24
     * @return the centre of the number's position
     */
    public Point2D getPointNumberCenter(final int point) {
        final double x = columnX(columnOf(point)) + pointWidth / 2;
        final double y = isTopRow(point) ? frame / 2 : top + innerHeight + frame / 2;
        return new Point2D.Double(x, y);
    }

    /**
     * Works out which board location a click landed on.
     *
     * <p>Each point owns its whole half-column, not just its triangle, so a
     * click near a checker stack's tip still counts.</p>
     *
     * @param x the click's x coordinate
     * @param y the click's y coordinate
     * @return a point from 1 to 24, {@link Move#BAR}, {@link Move#OFF}, or
     *         {@link #NOWHERE}
     */
    public int locationAt(final double x, final double y) {
        if (y < top || y > top + innerHeight) {
            return NOWHERE;
        }
        if (getTrayBounds(PlayerColor.BLACK).getMinX() <= x
                && x <= getTrayBounds(PlayerColor.BLACK).getMaxX()) {
            return Move.OFF;
        }
        if (getBarBounds().contains(x, y)) {
            return Move.BAR;
        }
        final boolean topRow = y < top + innerHeight / 2;
        for (int column = 0; column < POINTS_PER_ROW; column++) {
            final double left = columnX(column);
            if (x >= left && x < left + pointWidth) {
                return topRow ? POINTS_PER_ROW + 1 + column : POINTS_PER_ROW - column;
            }
        }
        return NOWHERE;
    }

    /**
     * Returns the screen column, from 0 at the left, that a point is drawn in.
     *
     * @param point a point from 1 to 24
     * @return the column, from 0 to 11
     */
    private int columnOf(final int point) {
        return isTopRow(point) ? point - (POINTS_PER_ROW + 1) : POINTS_PER_ROW - point;
    }

    /**
     * Returns the left edge of a column, skipping over the bar for the right half.
     *
     * @param column a column from 0 to 12 (12 being the right edge of the board)
     * @return the x coordinate of the column's left edge
     */
    private double columnX(final int column) {
        final double barOffset = column >= POINTS_PER_QUARTER ? pointWidth : 0;
        return frame + column * pointWidth + barOffset;
    }

    /**
     * Returns the right edge of the playing surface.
     *
     * @return the x coordinate where the right half ends
     */
    private double playingRight() {
        return columnX(POINTS_PER_ROW);
    }
}
