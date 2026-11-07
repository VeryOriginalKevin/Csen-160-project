package backgammon.app;

import backgammon.trivia.QuestionDeck;
import backgammon.trivia.TriviaSettings;

import java.util.Random;

/**
 * One game from the title screen's Start button until players leave it: the
 * question deck it draws from and the controller that runs it.
 *
 * <p>The game does not begin the instant Start is pressed. Bundled offline
 * questions cover only a few topics, so if the first question came from them
 * it could ignore the players' choice. Instead the session waits briefly for
 * the first live questions to arrive, and only gives up and starts anyway
 * (with bundled questions) after {@value #MAX_WAIT_MILLIS} ms, which in
 * practice means there is no internet connection.</p>
 *
 * <p>Owning both pieces in one object means leaving a game is a single call,
 * {@link #end()}, which also stops the deck's background fetching so an
 * abandoned game leaves nothing running.</p>
 */
public final class GameSession {

    /** Longest to wait for live questions before starting with bundled ones. */
    public static final int MAX_WAIT_MILLIS = 10_000;

    /** How often to check whether live questions have arrived. */
    private static final int POLL_MILLIS = 100;

    /** Supplies the questions, fetched in the background. */
    private final QuestionDeck deck;

    /** Runs the game. */
    private final GameController controller;

    /** Runs the polling while waiting for questions. */
    private final Scheduler scheduler;

    /** Whether the session has been ended, which also cancels any waiting. */
    private boolean ended;

    /**
     * Assembles a session. Use {@link #start(TriviaSettings, Runnable)}.
     *
     * @param deck       the question deck, already fetching
     * @param controller the controller, not yet started
     * @param scheduler  runs the polling while waiting for questions
     */
    private GameSession(final QuestionDeck deck, final GameController controller, final Scheduler scheduler) {
        this.deck = deck;
        this.controller = controller;
        this.scheduler = scheduler;
    }

    /**
     * Starts fetching questions for the players' choice, and begins the game
     * once the first live ones arrive. Must be called on Swing's event
     * dispatch thread.
     *
     * @param settings which topics and difficulties the players chose
     * @param onReady  run, on the event dispatch thread, once the game has
     *                 begun and its controller can be shown; not run if the
     *                 session is ended first
     * @return the session, still waiting for questions
     */
    public static GameSession start(final TriviaSettings settings, final Runnable onReady) {
        final QuestionDeck deck = QuestionDeck.createDefault(settings);
        deck.start();
        final Scheduler scheduler = new SwingScheduler();
        final GameController controller = new GameController(deck::next, scheduler, new Random());
        final GameSession session = new GameSession(deck, controller, scheduler);
        // The first check is scheduled rather than made now, so onReady never
        // runs before the caller has the session in hand.
        scheduler.schedule(POLL_MILLIS, () -> session.waitForQuestions(POLL_MILLIS, onReady));
        return session;
    }

    /**
     * Returns the controller running this game.
     *
     * @return the controller; its game has begun once {@code onReady} has run
     */
    public GameController getController() {
        return controller;
    }

    /** Ends the session: cancels any waiting, disarms the game's timers, and stops fetching. */
    public void end() {
        ended = true;
        controller.abandon();
        deck.shutdown();
    }

    /**
     * Checks whether live questions have arrived, beginning the game if so (or
     * if the wait has gone on too long), and otherwise checks again shortly.
     *
     * @param waitedMillis how long the session has waited so far
     * @param onReady      run once the game has begun
     */
    private void waitForQuestions(final int waitedMillis, final Runnable onReady) {
        if (ended) {
            return;
        }
        if (deck.getBufferedCount() > 0 || waitedMillis >= MAX_WAIT_MILLIS) {
            controller.newGame();
            onReady.run();
            return;
        }
        scheduler.schedule(POLL_MILLIS, () -> waitForQuestions(waitedMillis + POLL_MILLIS, onReady));
    }
}
