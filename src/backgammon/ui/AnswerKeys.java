package backgammon.ui;

import backgammon.model.PlayerColor;

import java.awt.event.KeyEvent;

/**
 * Which keyboard keys each player answers trivia with.
 *
 * <p>Both players share one keyboard: White ({@code WHITE}) uses
 * <kbd>1</kbd>&ndash;<kbd>4</kbd> with the left hand, Black
 * ({@code BLACK}) uses <kbd>7</kbd>&ndash;<kbd>0</kbd> with the right. Keeping
 * both on the keyboard makes the race fair; a mouse would be slower. The
 * bindings and the labels on screen both come from here, so they cannot
 * disagree.</p>
 *
 * <p>A constant holder; not instantiable.</p>
 */
public final class AnswerKeys {

    /** White's keys, one per answer, in answer order. */
    private static final char[] PLAYER_ONE_KEYS = {'1', '2', '3', '4'};

    /** Black's keys, one per answer, in answer order. */
    private static final char[] PLAYER_TWO_KEYS = {'7', '8', '9', '0'};

    /** Not instantiable. */
    private AnswerKeys() {
        throw new AssertionError("AnswerKeys is a constant holder and must not be instantiated.");
    }

    /**
     * Returns how many answers each player has keys for.
     *
     * @return the number of keys per player
     */
    public static int count() {
        return PLAYER_ONE_KEYS.length;
    }

    /**
     * Returns the key a player presses for an answer.
     *
     * @param color       the player's side
     * @param answerIndex the answer, from 0
     * @return the key's character, as printed on the keyboard
     */
    public static char keyFor(final PlayerColor color, final int answerIndex) {
        return (color == PlayerColor.WHITE ? PLAYER_ONE_KEYS : PLAYER_TWO_KEYS)[answerIndex];
    }

    /**
     * Returns the key code a player presses for an answer, for key bindings.
     *
     * @param color       the player's side
     * @param answerIndex the answer, from 0
     * @return the {@link KeyEvent} virtual key code
     */
    public static int keyCodeFor(final PlayerColor color, final int answerIndex) {
        // For digits the virtual key code is the character itself (VK_0..VK_9).
        return KeyEvent.getExtendedKeyCodeForChar(keyFor(color, answerIndex));
    }

    /**
     * Describes a player's keys for on-screen hints.
     *
     * @param color the player's side
     * @return text such as "1&ndash;4" or "7&ndash;0"
     */
    public static String rangeLabel(final PlayerColor color) {
        return keyFor(color, 0) + "\u2013" + keyFor(color, count() - 1);
    }
}
