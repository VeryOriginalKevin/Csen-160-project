package backgammon.ui;

import backgammon.app.GameController;
import backgammon.model.PlayerColor;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.util.Optional;

/**
 * The headline at the top of the side panel: who should do what right now.
 *
 * <p>The large prompt is coloured with the accent of the player in control, so
 * a glance tells both players whose turn it is. Beneath it, a smaller line
 * explains how to act, or reports the last notable event (a correct answer, a
 * hit, a blocked turn).</p>
 */
public final class StatusPanel extends JPanel {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Width the detail line wraps at. */
    private static final int DETAIL_WIDTH = 330;

    /** The game whose state is described. */
    private final GameController controller;

    /** The large instruction. */
    private final JLabel prompt;

    /** The smaller explanation or event report. */
    private final JLabel detail;

    /**
     * Builds the panel.
     *
     * @param controller the game to describe
     */
    public StatusPanel(final GameController controller) {
        this.controller = controller;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        prompt = new JLabel(" ");
        prompt.setFont(Theme.FONT_PROMPT);
        prompt.setAlignmentX(LEFT_ALIGNMENT);
        add(prompt);
        add(Box.createVerticalStrut(4));

        detail = new JLabel(" ");
        detail.setFont(Theme.FONT_STATUS);
        detail.setForeground(Theme.TEXT_SECONDARY);
        detail.setAlignmentX(LEFT_ALIGNMENT);
        add(detail);
    }

    /** Redraws the panel from the controller's current state. */
    public void refresh() {
        final Optional<PlayerColor> active = controller.getActivePlayer();
        final String mover = active.map(GameController::nameOf).orElse("");
        final Optional<String> notice = controller.getNotice();
        String headline;
        String explanation;
        Color colour = active.map(Theme::accentFor).orElse(Theme.TEXT_PRIMARY);

        switch (controller.getPhase()) {
            case TRIVIA_CONTEST:
                if (controller.isQuestionOpen()) {
                    headline = "Round " + controller.getRoundNumber() + ": answer first!";
                    explanation = "White: keys " + AnswerKeys.rangeLabel(PlayerColor.WHITE)
                            + "   \u00B7   Black: keys " + AnswerKeys.rangeLabel(PlayerColor.BLACK);
                } else {
                    // Revealing the answer: the notice says who won and how.
                    final PlayerColor winner = controller.getContestResult().get().getWinner();
                    headline = GameController.nameOf(winner) + " goes first";
                    colour = Theme.accentFor(winner);
                    explanation = notice.orElse("");
                }
                break;
            case DICE_DRAFT:
                headline = mover + ": pick two dice";
                explanation = "Click two of the four dice below. The other two go to your opponent.";
                break;
            case WINNER_MOVEMENT:
            case LOSER_MOVEMENT:
                headline = mover + " to move";
                explanation = notice.orElse(
                        "Click a ringed checker, then a highlighted point.");
                break;
            case GAME_OVER:
            default:
                headline = controller.getWinner().map(GameController::nameOf).orElse("Nobody") + " wins!";
                colour = controller.getWinner().map(Theme::accentFor).orElse(Theme.TEXT_PRIMARY);
                explanation = notice.orElse("") + " Press New game to play again.";
                break;
        }
        prompt.setText(headline);
        prompt.setForeground(colour);
        detail.setText(Html.wrapped(explanation, DETAIL_WIDTH));
    }
}
