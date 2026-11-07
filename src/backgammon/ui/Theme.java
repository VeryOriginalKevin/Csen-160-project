package backgammon.ui;

import backgammon.model.PlayerColor;

import java.awt.Color;
import java.awt.Font;

/**
 * The single source of truth for the application's colours and fonts.
 *
 * <p>Every graphic in the game &mdash; board, points, checkers, dice, trivia
 * panel &mdash; is drawn with Java2D from the values below rather than loaded
 * from an image file. Centralising them here means the whole look can be retuned
 * in one place, and the painting code stays free of literal colours.</p>
 *
 * <p>This class is a constant holder and cannot be instantiated.</p>
 */
public final class Theme {

    /** Background behind the board frame. */
    public static final Color WINDOW_BACKGROUND = new Color(0x1E, 0x22, 0x26);

    /** The playing surface between the two halves of the board. */
    public static final Color BOARD_SURFACE = new Color(0x2B, 0x60, 0x4A);

    /** The wooden frame and the bar down the middle. */
    public static final Color BOARD_FRAME = new Color(0x5A, 0x3E, 0x2B);

    /** Fill of the odd-numbered points. */
    public static final Color POINT_LIGHT = new Color(0xE8, 0xDA, 0xBE);

    /** Fill of the even-numbered points. */
    public static final Color POINT_DARK = new Color(0x8C, 0x4A, 0x3A);

    /** Body of a {@code WHITE} checker. */
    public static final Color CHECKER_WHITE = new Color(0xF2, 0xEE, 0xE4);

    /** Body of a {@code BLACK} checker. */
    public static final Color CHECKER_BLACK = new Color(0x24, 0x24, 0x28);

    /** Outline around every checker, for contrast on both point colours. */
    public static final Color CHECKER_OUTLINE = new Color(0x00, 0x00, 0x00, 0x80);

    /** Face of a die. */
    public static final Color DIE_FACE = new Color(0xFA, 0xFA, 0xF7);

    /** Pips on a die face. */
    public static final Color DIE_PIP = new Color(0x1A, 0x1A, 0x1A);

    /** Ring around a die the contest winner has drafted. */
    public static final Color DIE_SELECTED = new Color(0xF2, 0xC2, 0x4B);

    /** Highlight over a point the selected checker may legally reach. */
    public static final Color MOVE_TARGET = new Color(0xF2, 0xC2, 0x4B, 0x66);

    /** Panel background behind the trivia question and answer buttons. */
    public static final Color PANEL_BACKGROUND = new Color(0x26, 0x2B, 0x30);

    /** Primary text colour on dark panels. */
    public static final Color TEXT_PRIMARY = new Color(0xF4, 0xF4, 0xF2);

    /** Secondary text colour, for prompts and status lines. */
    public static final Color TEXT_SECONDARY = new Color(0xA8, 0xAE, 0xB4);

    /**
     * Accent marking White throughout the interface: headlines, key badges,
     * dice rings. Ivory, like White's checkers.
     */
    public static final Color WHITE_ACCENT = new Color(0xEE, 0xE8, 0xDA);

    /**
     * Accent marking Black. True black would vanish against the dark panels,
     * so Black is shown in slate grey, clearly darker than White's ivory.
     */
    public static final Color BLACK_ACCENT = new Color(0x6E, 0x78, 0x84);

    /** Fill of the bear-off trays, a shade darker than the playing surface. */
    public static final Color TRAY_SURFACE = new Color(0x22, 0x4D, 0x3B);

    /** Point numbers printed on the frame. */
    public static final Color POINT_NUMBER = new Color(0xFF, 0xFF, 0xFF, 0x80);

    /** Ring around the top checker of every stack the mover may pick up. */
    public static final Color MOVABLE_RING = new Color(0xF2, 0xC2, 0x4B, 0x90);

    /** Background of an answer once revealed as correct. */
    public static final Color ANSWER_CORRECT = new Color(0x2F, 0x7D, 0x4F);

    /** Background of an answer a player chose that was wrong. */
    public static final Color ANSWER_WRONG = new Color(0x9A, 0x36, 0x36);

    /** Background of an answer row in its normal state. */
    public static final Color ANSWER_IDLE = new Color(0x33, 0x39, 0x40);

    /** Track behind the countdown bar. */
    public static final Color COUNTDOWN_TRACK = new Color(0x3A, 0x41, 0x48);

    /** Countdown bar fill once time is nearly up. */
    public static final Color COUNTDOWN_URGENT = new Color(0xE0, 0x55, 0x4C);

    /** Translucent wash behind the game-over banner. */
    public static final Color BANNER_SHADE = new Color(0x00, 0x00, 0x00, 0xA0);

    /** Font for the large prompt at the top of the side panel. */
    public static final Font FONT_PROMPT = new Font(Font.SANS_SERIF, Font.BOLD, 22);

    /** Font for the game-over banner. */
    public static final Font FONT_BANNER = new Font(Font.SANS_SERIF, Font.BOLD, 40);

    /** Font for the key labels beside each answer. */
    public static final Font FONT_KEY = new Font(Font.MONOSPACED, Font.BOLD, 15);

    /** Font for the point numbers on the frame. */
    public static final Font FONT_POINT_NUMBER = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    /** Font for the trivia question. */
    public static final Font FONT_QUESTION = new Font(Font.SANS_SERIF, Font.BOLD, 20);

    /** Font for answer buttons. */
    public static final Font FONT_ANSWER = new Font(Font.SANS_SERIF, Font.PLAIN, 16);

    /** Font for status and prompt text. */
    public static final Font FONT_STATUS = new Font(Font.SANS_SERIF, Font.PLAIN, 14);

    /** Font for the count badge drawn on a tall checker stack. */
    public static final Font FONT_STACK_COUNT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    /** Not instantiable. */
    private Theme() {
        throw new AssertionError("Theme is a constant holder and must not be instantiated.");
    }

    /**
     * Returns the accent colour that marks a player throughout the interface.
     *
     * @param color the player's side
     * @return {@link #WHITE_ACCENT} for {@code WHITE},
     *         {@link #BLACK_ACCENT} for {@code BLACK}
     */
    public static Color accentFor(final PlayerColor color) {
        return color == PlayerColor.WHITE ? WHITE_ACCENT : BLACK_ACCENT;
    }

    /**
     * Returns the text colour that reads well on a player's accent colour,
     * for labels drawn on top of it such as key badges.
     *
     * @param color the player's side
     * @return dark text on White's ivory, light text on Black's slate
     */
    public static Color textOnAccentFor(final PlayerColor color) {
        return color == PlayerColor.WHITE ? DIE_PIP : TEXT_PRIMARY;
    }

    /**
     * Returns the body colour of a side's checkers.
     *
     * @param color the side
     * @return {@link #CHECKER_WHITE} or {@link #CHECKER_BLACK}
     */
    public static Color checkerFor(final PlayerColor color) {
        return color == PlayerColor.WHITE ? CHECKER_WHITE : CHECKER_BLACK;
    }
}
