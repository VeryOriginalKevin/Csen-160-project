package backgammon.app;

import backgammon.model.PlayerColor;
import backgammon.trivia.ContestResult;
import backgammon.trivia.Difficulty;
import backgammon.trivia.TriviaQuestion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the controller's phase sequence and input handling.
 *
 * <p>Uses a {@link ManualScheduler}, so timers fire only when a test advances
 * time, and a fixed question whose first answer is always correct. The final
 * test plays whole games with random answers and moves, to show the controller
 * reaches the end of a game without getting stuck or throwing.</p>
 */
public class ControllerTest {

    /** Index of the right answer in {@link #QUESTION}. */
    private static final int RIGHT = 0;

    /** Index of a wrong answer in {@link #QUESTION}. */
    private static final int WRONG = 1;

    /** The question every round asks. */
    private static final TriviaQuestion QUESTION = new TriviaQuestion("Test", Difficulty.EASY,
            "Which is right?", Arrays.asList("right", "wrong", "wrong too", "also wrong"), RIGHT);

    /** A correct answer wins the contest, and later answers change nothing. */
    @Test
    void correctAnswerWins() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 1);
        assertTrue(game.getPhase() == TurnPhase.TRIVIA_CONTEST && game.isQuestionOpen(),
                "a game opens on a question");
        assertTrue(game.getActivePlayer().isEmpty(), "nobody is in control during the question");

        game.answer(PlayerColor.BLACK, RIGHT);
        assertFalse(game.isQuestionOpen(), "question closes on the first answer");
        assertEquals(PlayerColor.BLACK, winnerOf(game), "right answer wins");

        game.answer(PlayerColor.WHITE, RIGHT);
        assertEquals(PlayerColor.BLACK, winnerOf(game), "a late answer changes nothing");
        assertEquals(TurnPhase.TRIVIA_CONTEST, game.getPhase(), "draft waits for the reveal");

        clock.advance(GameController.REVEAL_MILLIS);
        assertEquals(TurnPhase.DICE_DRAFT, game.getPhase(), "draft follows the reveal");
        assertEquals(PlayerColor.BLACK, game.getActivePlayer().orElse(null),
                "winner is in control of the draft");
    }

    /** A wrong answer hands the contest to the other player. */
    @Test
    void wrongAnswerLoses() {
        final GameController game = newController(new ManualScheduler(), 2);
        game.answer(PlayerColor.WHITE, WRONG);
        assertEquals(PlayerColor.BLACK, winnerOf(game), "wrong answer loses");
        assertEquals(ContestResult.Reason.OPPONENT_ANSWERED_INCORRECTLY,
                game.getContestResult().get().getReason(), "the reason is recorded");
        assertTrue(game.getAnsweredIndex() == WRONG && game.getAnsweredBy().orElse(null) == PlayerColor.WHITE,
                "the answer is remembered for the reveal");
    }

    /** With no answer, time runs out and a winner is drawn. */
    @Test
    void timeout() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 3);
        clock.advance(GameController.CONTEST_MILLIS / 2);
        assertTrue(Math.abs(game.getTimeRemainingFraction() - 0.5) < 0.01, "countdown is half way");
        assertTrue(game.isQuestionOpen(), "question still open half way");
        clock.advance(GameController.CONTEST_MILLIS / 2);
        assertFalse(game.isQuestionOpen(), "question closes when time is up");
        assertEquals(ContestResult.Reason.NO_ANSWER_IN_TIME, game.getContestResult().get().getReason(),
                "timeout decides by draw");
    }

    /** Picking toggles, and a second pick completes the draft. */
    @Test
    void draft() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 4);
        game.pickDie(0);
        assertTrue(game.getDraftPicks().isEmpty(), "picks are ignored during the question");

        game.answer(PlayerColor.WHITE, RIGHT);
        clock.advance(GameController.REVEAL_MILLIS);
        game.pickDie(2);
        game.pickDie(2);
        assertTrue(game.getDraftPicks().isEmpty(), "picking a die again puts it back");
        game.pickDie(2);
        game.pickDie(0);
        assertTrue(game.getDraft().isPresent(), "draft is complete after two picks");
        assertTrue(game.getDraft().get().isWinnerDie(0) && game.getDraft().get().isWinnerDie(2),
                "draft gave the winner dice 0 and 2");
        // The winner may have been blocked outright, in which case play moved on.
        final TurnPhase phase = game.getPhase();
        assertTrue(phase == TurnPhase.WINNER_MOVEMENT || phase == TurnPhase.LOSER_MOVEMENT, "winner moves next");
    }

    /** Clicks select a checker, then move it; the round then passes on. */
    @Test
    void movementClicks() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 5);
        game.answer(PlayerColor.WHITE, RIGHT);
        clock.advance(GameController.REVEAL_MILLIS);
        game.pickDie(0);
        game.pickDie(1);
        assertEquals(PlayerColor.WHITE, game.getActivePlayer().orElse(null), "WHITE moves first");

        // From the opening position WHITE can always move.
        final int source = game.getLegalSources().iterator().next();
        game.clickLocation(source);
        assertEquals(source, game.getSelectedSource().orElse(-99), "click selects a movable checker");
        assertFalse(game.getLegalTargets().isEmpty(), "a selection has targets");
        game.clickLocation(source);
        assertTrue(game.getSelectedSource().isEmpty(), "clicking it again deselects it");

        final int before = game.getRemainingDice().size();
        game.clickLocation(source);
        game.clickLocation(game.getLegalTargets().iterator().next());
        assertTrue(game.getRemainingDice().size() == before - 1 || game.getPhase() != TurnPhase.WINNER_MOVEMENT,
                "a move spends a die");

        // Finish both movement turns; the next round's question should follow.
        playUntilPhaseIs(game, clock, TurnPhase.TRIVIA_CONTEST, new Random(5));
        assertTrue(game.getPhase() == TurnPhase.TRIVIA_CONTEST || game.getPhase() == TurnPhase.GAME_OVER,
                "next round opens on a question");
        assertTrue(game.getRoundNumber() == 2 || game.getPhase() == TurnPhase.GAME_OVER, "round counter advanced");
    }

    /** Timers left over from an abandoned game do not touch the new one. */
    @Test
    void newGameDiscardsTimers() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 6);
        game.answer(PlayerColor.WHITE, RIGHT);
        game.newGame();
        clock.advance(GameController.REVEAL_MILLIS);
        assertEquals(TurnPhase.TRIVIA_CONTEST, game.getPhase(), "old reveal timer does not start a draft");
        assertTrue(game.isQuestionOpen(), "new game's question is open");
    }

    /** Whole games with random play always finish. Every seed is tried, and all failures reported together. */
    @Test
    void wholeGames() {
        final List<Executable> seeds = new ArrayList<>();
        for (int seed = 10; seed < 30; seed++) {
            final int s = seed;
            seeds.add(() -> {
                final ManualScheduler clock = new ManualScheduler();
                final GameController game = newController(clock, s);
                final Random player = new Random(s);
                playUntilPhaseIs(game, clock, TurnPhase.GAME_OVER, player);
                assertEquals(TurnPhase.GAME_OVER, game.getPhase(), "game with seed " + s + " ends");
                assertTrue(game.getWinner().isPresent(), "game with seed " + s + " has a winner");
            });
        }
        assertAll(seeds);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Creates a controller with the fixed question and starts a game.
     *
     * @param clock the scheduler to drive it with
     * @param seed  seed for its dice
     * @return the controller, on its first question
     */
    private static GameController newController(final ManualScheduler clock, final long seed) {
        final GameController game = new GameController(() -> QUESTION, clock, new Random(seed));
        game.newGame();
        return game;
    }

    /**
     * Returns who won the current contest.
     *
     * @param game the controller
     * @return the winner, or {@code null} if the contest is open
     */
    private static PlayerColor winnerOf(final GameController game) {
        return game.getContestResult().map(ContestResult::getWinner).orElse(null);
    }

    /**
     * Plays randomly &mdash; random answers, random dice, random legal moves
     * &mdash; until the game reaches a phase.
     *
     * @param game   the controller
     * @param clock  its scheduler, advanced past every pause
     * @param target the phase to stop at; play also stops at game over
     * @param random randomness for the choices
     * @throws IllegalStateException if the target is not reached in a sane
     *                               number of steps, which would mean the game is stuck
     */
    private static void playUntilPhaseIs(final GameController game, final ManualScheduler clock,
                                         final TurnPhase target, final Random random) {
        final int maxSteps = 200_000;
        // Leave the starting phase first, so asking for the phase we are in means "the next one".
        final TurnPhase start = game.getPhase();
        boolean left = false;
        for (int step = 0; step < maxSteps; step++) {
            final TurnPhase phase = game.getPhase();
            if (phase != start) {
                left = true;
            }
            if ((left && phase == target) || phase == TurnPhase.GAME_OVER) {
                return;
            }
            takeRandomAction(game, clock, random);
        }
        throw new IllegalStateException("Game stuck in " + game.getPhase());
    }

    /**
     * Does one random thing appropriate to the current phase.
     *
     * @param game   the controller
     * @param clock  its scheduler
     * @param random randomness for the choice
     */
    private static void takeRandomAction(final GameController game, final ManualScheduler clock,
                                         final Random random) {
        switch (game.getPhase()) {
            case TRIVIA_CONTEST:
                if (game.isQuestionOpen()) {
                    final PlayerColor who = random.nextBoolean() ? PlayerColor.WHITE : PlayerColor.BLACK;
                    game.answer(who, random.nextInt(4));
                } else {
                    clock.advance(GameController.REVEAL_MILLIS);
                }
                break;
            case DICE_DRAFT:
                game.pickDie(random.nextInt(4));
                break;
            case WINNER_MOVEMENT:
            case LOSER_MOVEMENT:
                final Set<Integer> sources = game.getLegalSources();
                if (sources.isEmpty()) {
                    // Blocked: wait out the pause.
                    clock.advance(GameController.BLOCKED_PAUSE_MILLIS);
                    break;
                }
                if (game.getSelectedSource().isEmpty()) {
                    game.clickLocation(pick(new ArrayList<>(sources), random));
                }
                final Set<Integer> targets = game.getLegalTargets();
                if (!targets.isEmpty()) {
                    game.clickLocation(pick(new ArrayList<>(targets), random));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Picks a random element.
     *
     * @param options the choices; not empty
     * @param random  randomness for the pick
     * @return one of the options
     */
    private static int pick(final List<Integer> options, final Random random) {
        return options.get(random.nextInt(options.size()));
    }
}
