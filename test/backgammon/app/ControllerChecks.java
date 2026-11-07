package backgammon.app;

import backgammon.model.PlayerColor;
import backgammon.trivia.ContestResult;
import backgammon.trivia.Difficulty;
import backgammon.trivia.TriviaQuestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Checks for the controller's phase sequence and input handling. From the
 * project root:
 * <pre>
 * javac -d out -sourcepath src test/backgammon/app/*.java
 * java -cp out backgammon.app.ControllerChecks
 * </pre>
 *
 * <p>Uses a {@link ManualScheduler}, so timers fire only when a check advances
 * time, and a fixed question whose first answer is always correct. The final
 * check plays whole games with random answers and moves, to show the controller
 * reaches the end of a game without getting stuck or throwing.</p>
 */
public final class ControllerChecks {

    /** Index of the right answer in {@link #QUESTION}. */
    private static final int RIGHT = 0;

    /** Index of a wrong answer in {@link #QUESTION}. */
    private static final int WRONG = 1;

    /** The question every round asks. */
    private static final TriviaQuestion QUESTION = new TriviaQuestion("Test", Difficulty.EASY,
            "Which is right?", Arrays.asList("right", "wrong", "wrong too", "also wrong"), RIGHT);

    /** How many checks have run. */
    private int checksRun;

    /** How many checks have failed. */
    private int checksFailed;

    /** Not instantiated from outside; {@link #main} makes the one instance. */
    private ControllerChecks() {
        this.checksRun = 0;
        this.checksFailed = 0;
    }

    /**
     * Runs every check and reports the tally.
     *
     * @param args ignored
     */
    public static void main(final String[] args) {
        final ControllerChecks checks = new ControllerChecks();
        checks.checkCorrectAnswerWins();
        checks.checkWrongAnswerLoses();
        checks.checkTimeout();
        checks.checkDraft();
        checks.checkMovementClicks();
        checks.checkNewGameDiscardsTimers();
        checks.checkWholeGames();
        checks.report();
    }

    /** A correct answer wins the contest, and later answers change nothing. */
    private void checkCorrectAnswerWins() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 1);
        check("a game opens on a question", game.getPhase() == TurnPhase.TRIVIA_CONTEST && game.isQuestionOpen());
        check("nobody is in control during the question", game.getActivePlayer().isEmpty());

        game.answer(PlayerColor.BLACK, RIGHT);
        check("question closes on the first answer", !game.isQuestionOpen());
        check("right answer wins", winnerOf(game) == PlayerColor.BLACK);

        game.answer(PlayerColor.WHITE, RIGHT);
        check("a late answer changes nothing", winnerOf(game) == PlayerColor.BLACK);
        check("draft waits for the reveal", game.getPhase() == TurnPhase.TRIVIA_CONTEST);

        clock.advance(GameController.REVEAL_MILLIS);
        check("draft follows the reveal", game.getPhase() == TurnPhase.DICE_DRAFT);
        check("winner is in control of the draft", game.getActivePlayer().orElse(null) == PlayerColor.BLACK);
    }

    /** A wrong answer hands the contest to the other player. */
    private void checkWrongAnswerLoses() {
        final GameController game = newController(new ManualScheduler(), 2);
        game.answer(PlayerColor.WHITE, WRONG);
        check("wrong answer loses", winnerOf(game) == PlayerColor.BLACK);
        check("the reason is recorded", game.getContestResult().get().getReason()
                == ContestResult.Reason.OPPONENT_ANSWERED_INCORRECTLY);
        check("the answer is remembered for the reveal",
                game.getAnsweredIndex() == WRONG && game.getAnsweredBy().orElse(null) == PlayerColor.WHITE);
    }

    /** With no answer, time runs out and a winner is drawn. */
    private void checkTimeout() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 3);
        clock.advance(GameController.CONTEST_MILLIS / 2);
        check("countdown is half way", Math.abs(game.getTimeRemainingFraction() - 0.5) < 0.01);
        check("question still open half way", game.isQuestionOpen());
        clock.advance(GameController.CONTEST_MILLIS / 2);
        check("question closes when time is up", !game.isQuestionOpen());
        check("timeout decides by draw", game.getContestResult().get().getReason()
                == ContestResult.Reason.NO_ANSWER_IN_TIME);
    }

    /** Picking toggles, and a second pick completes the draft. */
    private void checkDraft() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 4);
        game.pickDie(0);
        check("picks are ignored during the question", game.getDraftPicks().isEmpty());

        game.answer(PlayerColor.WHITE, RIGHT);
        clock.advance(GameController.REVEAL_MILLIS);
        game.pickDie(2);
        game.pickDie(2);
        check("picking a die again puts it back", game.getDraftPicks().isEmpty());
        game.pickDie(2);
        game.pickDie(0);
        check("draft is complete after two picks", game.getDraft().isPresent());
        check("draft gave the winner dice 0 and 2", game.getDraft().get().isWinnerDie(0)
                && game.getDraft().get().isWinnerDie(2));
        // The winner may have been blocked outright, in which case play moved on.
        final TurnPhase phase = game.getPhase();
        check("winner moves next", phase == TurnPhase.WINNER_MOVEMENT || phase == TurnPhase.LOSER_MOVEMENT);
    }

    /** Clicks select a checker, then move it; the round then passes on. */
    private void checkMovementClicks() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 5);
        game.answer(PlayerColor.WHITE, RIGHT);
        clock.advance(GameController.REVEAL_MILLIS);
        game.pickDie(0);
        game.pickDie(1);
        check("WHITE moves first", game.getActivePlayer().orElse(null) == PlayerColor.WHITE);

        // From the opening position WHITE can always move.
        final int source = game.getLegalSources().iterator().next();
        game.clickLocation(source);
        check("click selects a movable checker", game.getSelectedSource().orElse(-99) == source);
        check("a selection has targets", !game.getLegalTargets().isEmpty());
        game.clickLocation(source);
        check("clicking it again deselects it", game.getSelectedSource().isEmpty());

        final int before = game.getRemainingDice().size();
        game.clickLocation(source);
        game.clickLocation(game.getLegalTargets().iterator().next());
        check("a move spends a die", game.getRemainingDice().size() == before - 1
                || game.getPhase() != TurnPhase.WINNER_MOVEMENT);

        // Finish both movement turns; the next round's question should follow.
        playUntilPhaseIs(game, clock, TurnPhase.TRIVIA_CONTEST, new Random(5));
        check("next round opens on a question", game.getPhase() == TurnPhase.TRIVIA_CONTEST
                || game.getPhase() == TurnPhase.GAME_OVER);
        check("round counter advanced", game.getRoundNumber() == 2 || game.getPhase() == TurnPhase.GAME_OVER);
    }

    /** Timers left over from an abandoned game do not touch the new one. */
    private void checkNewGameDiscardsTimers() {
        final ManualScheduler clock = new ManualScheduler();
        final GameController game = newController(clock, 6);
        game.answer(PlayerColor.WHITE, RIGHT);
        game.newGame();
        clock.advance(GameController.REVEAL_MILLIS);
        check("old reveal timer does not start a draft", game.getPhase() == TurnPhase.TRIVIA_CONTEST);
        check("new game's question is open", game.isQuestionOpen());
    }

    /** Whole games with random play always finish. */
    private void checkWholeGames() {
        for (int seed = 10; seed < 30; seed++) {
            final ManualScheduler clock = new ManualScheduler();
            final GameController game = newController(clock, seed);
            final Random player = new Random(seed);
            try {
                playUntilPhaseIs(game, clock, TurnPhase.GAME_OVER, player);
                check("game with seed " + seed + " ends", game.getPhase() == TurnPhase.GAME_OVER);
                check("game with seed " + seed + " has a winner", game.getWinner().isPresent());
            } catch (final RuntimeException failure) {
                check("game with seed " + seed + " threw " + failure, false);
            }
        }
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

    /**
     * Records the outcome of one check, printing it only on failure.
     *
     * @param description what was being checked
     * @param satisfied   whether the check passed
     */
    private void check(final String description, final boolean satisfied) {
        checksRun++;
        if (!satisfied) {
            checksFailed++;
            System.out.println("FAIL: " + description);
        }
    }

    /** Prints the tally and exits non-zero if any check failed. */
    private void report() {
        System.out.println(checksRun + " checks, " + checksFailed + " failures");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }
}
