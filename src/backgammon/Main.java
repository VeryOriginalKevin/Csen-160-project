package backgammon;

import backgammon.ui.MainWindow;

import javax.swing.SwingUtilities;

/**
 * Entry point for the application.
 *
 * <p>Its only job is to open the window, on Swing's event dispatch thread. The
 * window starts on the title screen, and each game is assembled when players
 * press Start (see {@link backgammon.app.GameSession}).</p>
 */
public final class Main {

    /** Not instantiable. */
    private Main() {
        throw new AssertionError("Main is an entry point and must not be instantiated.");
    }

    /**
     * Launches the game.
     *
     * @param args ignored; the game takes no command-line arguments
     */
    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
