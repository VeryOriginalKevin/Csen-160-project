package backgammon.ui;

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

    /** Body of a {@code LIGHT} checker. */
    public static final Color CHECKER_LIGHT = new Color(0xF2, 0xEE, 0xE4);

    /** Body of a {@code DARK} checker. */
    public static final Color CHECKER_DARK = new Color(0x24, 0x24, 0x28);

    /** Outline around every checker, for contrast on both point colours. */
    public static final Color CHECKER_OUTLINE = new Color(0x00, 0x00, 0x00, 0x80);

    /** Face of a die. */
    public static final Color DIE_FACE = new Color(0xFA, 0xFA, 0xF7);

    /** Pips on a die face. */
    public static final Color DIE_PIP = new Color(0x1A, 0x1A, 0x1A);

    /** Ring around a die the contest winner has drafted. */
    public static final Color DIE_SELECTED = new Color(0xF2, 0xC2, 0x4B);

    /** Ring around a die left behind for the losing player. */
    public static final Color DIE_LEFTOVER = new Color(0x7A, 0x7A, 0x7A);

    /** Highlight over a point the selected checker may legally reach. */
    public static final Color MOVE_TARGET = new Color(0xF2, 0xC2, 0x4B, 0x66);

    /** Panel background behind the trivia question and answer buttons. */
    public static final Color PANEL_BACKGROUND = new Color(0x26, 0x2B, 0x30);

    /** Primary text colour on dark panels. */
    public static final Color TEXT_PRIMARY = new Color(0xF4, 0xF4, 0xF2);

    /** Secondary text colour, for prompts and status lines. */
    public static final Color TEXT_SECONDARY = new Color(0xA8, 0xAE, 0xB4);

    /** Accent for the player-one side of the trivia panel. */
    public static final Color PLAYER_ONE_ACCENT = new Color(0x4C, 0x9A, 0xE0);

    /** Accent for the player-two side of the trivia panel. */
    public static final Color PLAYER_TWO_ACCENT = new Color(0xE0, 0x7A, 0x4C);

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
}
