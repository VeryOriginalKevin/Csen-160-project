package backgammon.ui;

import backgammon.app.GameController;
import backgammon.app.TurnPhase;
import backgammon.model.PlayerColor;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/**
 * The game itself: the board on the left, and on the right a side panel with
 * the status headline, the trivia question, the dice, and a button back to the
 * title screen.
 *
 * <p>The screen wires input to the controller &mdash; the answer keys for both
 * players, and through its child panels the mouse &mdash; and refreshes every
 * panel whenever the controller reports a change. It holds no game state
 * itself.</p>
 */
public final class GameScreen extends JPanel {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Width of the side panel. */
    private static final int SIDE_PANEL_WIDTH = 410;

    /** Padding inside the side panel. */
    private static final int SIDE_PADDING = 20;

    /** The game this screen shows. */
    private final GameController controller;

    /** The headline. */
    private final StatusPanel status;

    /** The question and answers. */
    private final TriviaPanel trivia;

    /** The four dice. */
    private final DiceTray dice;

    /** The board. */
    private final BoardPanel board;

    /**
     * Builds the screen.
     *
     * @param controller the game to show; its first game should already be started
     * @param onLeave    run when the players choose to leave for the title screen
     */
    public GameScreen(final GameController controller, final Runnable onLeave) {
        super(new BorderLayout());
        this.controller = controller;
        this.status = new StatusPanel(controller);
        this.trivia = new TriviaPanel(controller);
        this.dice = new DiceTray(controller);
        this.board = new BoardPanel(controller);

        setBackground(Theme.WINDOW_BACKGROUND);
        add(board, BorderLayout.CENTER);
        add(buildSidePanel(onLeave), BorderLayout.EAST);

        bindAnswerKeys();
        controller.addListener(this::refreshAll);
        refreshAll();
    }

    /**
     * Lays out the side panel.
     *
     * @param onLeave run when the players leave for the title screen
     * @return the panel
     */
    private JPanel buildSidePanel(final Runnable onLeave) {
        final JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.PANEL_BACKGROUND);
        side.setBorder(BorderFactory.createEmptyBorder(SIDE_PADDING, SIDE_PADDING, SIDE_PADDING, SIDE_PADDING));
        side.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, 0));

        addLeft(side, status);
        side.add(Box.createVerticalStrut(22));
        addLeft(side, trivia);
        side.add(Box.createVerticalStrut(16));
        addLeft(side, dice);
        side.add(Box.createVerticalGlue());
        addLeft(side, legendLine(PlayerColor.WHITE));
        side.add(Box.createVerticalStrut(4));
        addLeft(side, legendLine(PlayerColor.BLACK));
        side.add(Box.createVerticalStrut(14));
        addLeft(side, newGameButton(onLeave));
        return side;
    }

    /**
     * Builds one line of the player legend, such as "White &middot; keys 1&ndash;4".
     *
     * @param color the player's side
     * @return the label, in the player's accent colour
     */
    private static JLabel legendLine(final PlayerColor color) {
        final JLabel line = new JLabel("\u25CF  " + GameController.nameOf(color)
                + "  \u00B7  keys " + AnswerKeys.rangeLabel(color));
        line.setFont(Theme.FONT_STATUS);
        line.setForeground(Theme.accentFor(color));
        return line;
    }

    /**
     * Builds the new-game button, which returns to the title screen so players
     * can change the trivia options. It asks first if a game is in progress.
     *
     * @param onLeave run when the players confirm
     * @return the button
     */
    private JButton newGameButton(final Runnable onLeave) {
        final JButton button = new JButton("New game");
        button.setFont(Theme.FONT_STATUS);
        button.setForeground(Theme.TEXT_PRIMARY);
        button.setBackground(Theme.ANSWER_IDLE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Not focusable, so it can never swallow the players' answer keys.
        button.setFocusable(false);
        button.addActionListener(event -> {
            final boolean inProgress = controller.getPhase() != TurnPhase.GAME_OVER;
            if (!inProgress || JOptionPane.showConfirmDialog(this,
                    "Abandon this game and return to the title screen?", "New game",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                onLeave.run();
            }
        });
        return button;
    }

    /**
     * Binds both players' answer keys for the whole window, so they work
     * whichever component has focus. The controller ignores them outside the
     * contest.
     */
    private void bindAnswerKeys() {
        final InputMap keys = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        final ActionMap actions = getActionMap();
        for (final PlayerColor color : PlayerColor.values()) {
            for (int index = 0; index < AnswerKeys.count(); index++) {
                final String name = "answer-" + color + "-" + index;
                final int answerIndex = index;
                keys.put(KeyStroke.getKeyStroke(AnswerKeys.keyCodeFor(color, index), 0), name);
                actions.put(name, new AbstractAction() {
                    /** Serialisation version; Swing actions are serialisable. */
                    private static final long serialVersionUID = 1L;

                    /** Submits this key's answer for this key's player. */
                    @Override
                    public void actionPerformed(final ActionEvent event) {
                        controller.answer(color, answerIndex);
                    }
                });
            }
        }
    }

    /** Redraws every panel from the controller's current state. */
    private void refreshAll() {
        status.refresh();
        trivia.refresh();
        dice.refresh();
        board.repaint();
    }

    /**
     * Adds a child to a vertical box, aligned to the left edge.
     *
     * @param box       the box
     * @param component the child
     */
    private static void addLeft(final JPanel box, final JComponent component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        box.add(component);
    }
}
