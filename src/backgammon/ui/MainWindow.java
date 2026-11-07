package backgammon.ui;

import backgammon.app.GameSession;
import backgammon.trivia.TriviaSettings;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The application's one window. It shows the {@link TitleScreen} first, then a
 * {@link GameScreen} once players press Start, and returns to the title screen
 * when they leave a game.
 *
 * <p>Each game gets a fresh {@link GameSession} and a fresh game screen, and
 * both are discarded on leaving, so nothing from an old game (its timers, its
 * background question fetching) lingers into the next.</p>
 */
public final class MainWindow extends JFrame {

    /** Serialisation version; Swing components are serialisable. */
    private static final long serialVersionUID = 1L;

    /** Smallest size at which the board and side panel both fit. */
    private static final Dimension MINIMUM_SIZE = new Dimension(1100, 720);

    /** The title screen, kept so players' last choices are still set when they return. */
    private final TitleScreen titleScreen;

    /** The game being played, or {@code null} while the title screen shows. */
    private GameSession session;

    /** Builds the window on the title screen. Call {@link #setVisible(boolean)} to show it. */
    public MainWindow() {
        super("Triviagammon");
        this.titleScreen = new TitleScreen(this::startGame);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            /** Stops any game's background work once the window has gone. */
            @Override
            public void windowClosed(final WindowEvent event) {
                endSession();
            }
        });

        setContentPane(titleScreen);
        setMinimumSize(MINIMUM_SIZE);
        setPreferredSize(MINIMUM_SIZE);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Starts a game with the players' trivia choice: shows a short loading
     * screen while the first questions download, then the game.
     *
     * @param settings the topics and difficulties chosen on the title screen
     */
    private void startGame(final TriviaSettings settings) {
        endSession();
        showScreen(loadingScreen());
        session = GameSession.start(settings,
                () -> showScreen(new GameScreen(session.getController(), this::returnToTitle)));
    }

    /**
     * Builds the screen shown while the first questions download.
     *
     * @return a plain screen with a centred message
     */
    private static JPanel loadingScreen() {
        // GridBagLayout with no constraints centres its one child.
        final JPanel screen = new JPanel(new GridBagLayout());
        screen.setBackground(Theme.WINDOW_BACKGROUND);
        final JLabel message = new JLabel("Fetching questions\u2026");
        message.setFont(Theme.FONT_PROMPT);
        message.setForeground(Theme.TEXT_SECONDARY);
        screen.add(message);
        return screen;
    }

    /** Ends the current game and shows the title screen again. */
    private void returnToTitle() {
        endSession();
        showScreen(titleScreen);
    }

    /**
     * Swaps the window's content for another screen.
     *
     * @param screen the screen to show
     */
    private void showScreen(final Container screen) {
        setContentPane(screen);
        revalidate();
        repaint();
    }

    /** Ends the current game, if there is one. */
    private void endSession() {
        if (session != null) {
            session.end();
            session = null;
        }
    }
}
