package backgammon;

/**
 * Entry point for the application.
 *
 * <p>Its only job is to open the main window on Swing's event dispatch thread;
 * all wiring belongs in {@link backgammon.app}.</p>
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
        // Once GameApplication exists this becomes:
        //     SwingUtilities.invokeLater(() -> new GameApplication().start());
        System.out.println("Scaffold only: no game wiring yet. See docs/ARCHITECTURE.md.");
    }
}
