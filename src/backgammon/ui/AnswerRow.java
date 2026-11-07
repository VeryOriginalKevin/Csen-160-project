package backgammon.ui;

import backgammon.model.PlayerColor;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

/**
 * One answer to the trivia question, with each player's key beside it.
 *
 * <p>White's key sits on the left in their accent colour, Black's on the
 * right in theirs, mirroring where each player's hand is on the keyboard. The
 * row is display only &mdash; answers come from the keyboard, never the mouse
 * &mdash; and its background changes to reveal the outcome.</p>
 */
final class AnswerRow extends JPanel {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** How an answer row looks at a given moment. */
    enum Look {
        /** Question open: neutral background. */
        IDLE,
        /** Revealed as the correct answer. */
        CORRECT,
        /** Chosen by a player, and wrong. */
        WRONG,
        /** Revealed, and neither correct nor chosen: faded into the background. */
        FADED
    }

    /** Width of the answer text before it wraps. */
    private static final int TEXT_WIDTH = 250;

    /** Size of each key badge. */
    private static final Dimension BADGE_SIZE = new Dimension(30, 30);

    /** The answer's text. */
    private final JLabel text;

    /** White's key for this answer. */
    private final JLabel leftKey;

    /** Black's key for this answer. */
    private final JLabel rightKey;

    /**
     * Creates a row for one answer position.
     *
     * @param answerIndex which answer this row shows, from 0; decides the keys
     */
    AnswerRow(final int answerIndex) {
        super(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        this.leftKey = badge(PlayerColor.WHITE, answerIndex);
        this.rightKey = badge(PlayerColor.BLACK, answerIndex);
        this.text = new JLabel();
        text.setFont(Theme.FONT_ANSWER);
        text.setForeground(Theme.TEXT_PRIMARY);
        add(leftKey, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        add(rightKey, BorderLayout.EAST);
        show("", Look.IDLE, null);
    }

    /**
     * Shows an answer in a given look.
     *
     * @param answer    the answer's text
     * @param look      how to present it
     * @param pressedBy the player whose key press chose this answer, whose
     *                  badge stays lit after the reveal; {@code null} if nobody
     *                  chose it
     */
    void show(final String answer, final Look look, final PlayerColor pressedBy) {
        text.setText(Html.wrapped(answer, TEXT_WIDTH));
        final Color background;
        switch (look) {
            case CORRECT:
                background = Theme.ANSWER_CORRECT;
                break;
            case WRONG:
                background = Theme.ANSWER_WRONG;
                break;
            default:
                background = Theme.ANSWER_IDLE;
                break;
        }
        setBackground(background);
        // Once the outcome is shown the keys no longer do anything, so they
        // grey out, except the one that was pressed, which shows who answered.
        final boolean open = look == Look.IDLE;
        text.setForeground(look == Look.FADED ? Theme.TEXT_SECONDARY : Theme.TEXT_PRIMARY);
        final boolean leftLit = open || pressedBy == PlayerColor.WHITE;
        final boolean rightLit = open || pressedBy == PlayerColor.BLACK;
        paintBadge(leftKey, PlayerColor.WHITE, leftLit);
        paintBadge(rightKey, PlayerColor.BLACK, rightLit);
    }

    /**
     * Builds a key badge.
     *
     * @param color       whose key it is, which decides the colour
     * @param answerIndex which answer it selects
     * @return the badge
     */
    private static JLabel badge(final PlayerColor color, final int answerIndex) {
        final JLabel badge = new JLabel(String.valueOf(AnswerKeys.keyFor(color, answerIndex)),
                SwingConstants.CENTER);
        badge.setFont(Theme.FONT_KEY);
        badge.setOpaque(true);
        badge.setPreferredSize(BADGE_SIZE);
        paintBadge(badge, color, true);
        return badge;
    }

    /**
     * Colours a key badge as lit (in its player's accent) or greyed out.
     *
     * @param badge the badge
     * @param color whose key it is
     * @param lit   whether it should stand out
     */
    private static void paintBadge(final JLabel badge, final PlayerColor color, final boolean lit) {
        badge.setBackground(lit ? Theme.accentFor(color) : Theme.COUNTDOWN_TRACK);
        badge.setForeground(lit ? Theme.textOnAccentFor(color) : Theme.TEXT_SECONDARY);
    }
}
