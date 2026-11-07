package backgammon.app;

import backgammon.model.Board;
import backgammon.model.DiceDraft;
import backgammon.model.DiceRoll;
import backgammon.model.Game;
import backgammon.model.Move;
import backgammon.model.MovementTurn;
import backgammon.model.PlayerColor;
import backgammon.trivia.ContestResult;
import backgammon.trivia.TriviaContest;
import backgammon.trivia.TriviaQuestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Runs the game: walks each round through its phases and decides which input
 * counts at each moment.
 *
 * <p>A round goes:</p>
 * <ol>
 *   <li>{@link TurnPhase#TRIVIA_CONTEST TRIVIA_CONTEST}: four dice are rolled
 *       and a question goes up. Both players may answer; the first answer
 *       decides the contest. If nobody answers within
 *       {@value #CONTEST_MILLIS} ms the winner is drawn at random. The correct
 *       answer is then shown for {@value #REVEAL_MILLIS} ms.</li>
 *   <li>{@link TurnPhase#DICE_DRAFT DICE_DRAFT}: the winner picks two dice.</li>
 *   <li>{@link TurnPhase#WINNER_MOVEMENT WINNER_MOVEMENT}: the winner moves.</li>
 *   <li>{@link TurnPhase#LOSER_MOVEMENT LOSER_MOVEMENT}: the loser moves with
 *       the two dice left over. Then the next round begins.</li>
 * </ol>
 * <p>The game ends in {@link TurnPhase#GAME_OVER GAME_OVER} as soon as a side
 * bears off its last checker, even mid-round.</p>
 *
 * <p>The views call the input methods ({@link #answer}, {@link #pickDie},
 * {@link #clickLocation}, {@link #newGame}) and redraw from the getters when a
 * {@link GameListener} fires. Input that does not fit the current phase is
 * ignored rather than rejected, because stray clicks and key presses are
 * normal in a two-player game.</p>
 *
 * <p>Not thread safe: every method, and every task given to the
 * {@link Scheduler}, must run on the same thread (Swing's event dispatch
 * thread in the running game).</p>
 */
public final class GameController {

    /** How long players have to answer a question, in milliseconds. */
    public static final int CONTEST_MILLIS = 15_000;

    /** How often the countdown updates, in milliseconds. */
    public static final int TICK_MILLIS = 100;

    /** How long the correct answer stays on screen before the draft, in milliseconds. */
    public static final int REVEAL_MILLIS = 2_000;

    /** How long a "no legal moves" message shows before play moves on, in milliseconds. */
    public static final int BLOCKED_PAUSE_MILLIS = 1_800;

    /** Where questions come from; never returns {@code null}, never blocks. */
    private final Supplier<TriviaQuestion> questions;

    /** Runs the timed events. */
    private final Scheduler scheduler;

    /** Randomness for dice and for drawing a winner when nobody answers. */
    private final Random random;

    /** Everyone to tell when the state changes. */
    private final List<GameListener> listeners;

    /** The winner's dice picks so far during the draft, as roll positions. */
    private final List<Integer> draftPicks;

    /** The game in progress. */
    private Game game;

    /** Where in the round we are. */
    private TurnPhase phase;

    /** Which round this is, counting from 1. */
    private int roundNumber;

    /** This round's four dice. */
    private DiceRoll roll;

    /** This round's trivia contest. */
    private TriviaContest contest;

    /** The side whose answer decided the contest, or {@code null} if none did. */
    private PlayerColor answeredBy;

    /** The answer that decided the contest, or -1 if none did. */
    private int answeredIndex;

    /** Milliseconds left on the trivia countdown. */
    private int remainingMillis;

    /** How the four dice were split, once the winner has picked. */
    private DiceDraft draft;

    /** The movement turn in progress, or {@code null} outside movement phases. */
    private MovementTurn turn;

    /** The location the mover has selected a checker from, or {@code null}. */
    private Integer selectedSource;

    /** A one-line description of the last notable event, or {@code null}. */
    private String notice;

    /**
     * Bumped whenever a new game or round starts. Each scheduled task remembers
     * the value it was created under and does nothing if it has changed, which
     * is how stale timers are made harmless without being cancelled.
     */
    private int epoch;

    /**
     * Creates a controller. Call {@link #newGame()} to start playing.
     *
     * @param questions where to get each round's question; must return
     *                  immediately and never {@code null}
     * @param scheduler runs the countdown and pauses
     * @param random    randomness for dice and tie-break draws
     */
    public GameController(final Supplier<TriviaQuestion> questions,
                          final Scheduler scheduler,
                          final Random random) {
        this.questions = questions;
        this.scheduler = scheduler;
        this.random = random;
        this.listeners = new ArrayList<>();
        this.draftPicks = new ArrayList<>();
        this.answeredIndex = -1;
    }

    /**
     * Returns the name players are shown for a side.
     *
     * @param color the side
     * @return "White" for {@code WHITE}, "Black" for {@code BLACK}
     */
    public static String nameOf(final PlayerColor color) {
        return color == PlayerColor.WHITE ? "White" : "Black";
    }

    /**
     * Registers a listener to be told about every state change.
     *
     * @param listener the listener to add
     */
    public void addListener(final GameListener listener) {
        listeners.add(listener);
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    /** Throws away any game in progress and starts a fresh one. */
    public void newGame() {
        game = new Game(random);
        roundNumber = 0;
        startRound();
    }

    /**
     * Stops the game for good: every pending timer is disarmed and no further
     * timed event will fire. Call when players leave the game.
     */
    public void abandon() {
        // Scheduled tasks only run if the epoch is unchanged, so bumping it
        // disarms every one of them.
        epoch++;
    }

    /**
     * Records a player's answer to the current question.
     *
     * <p>Ignored unless a question is open. The first answer accepted decides
     * the contest: right wins it, wrong hands it to the other player.</p>
     *
     * @param player      the side answering
     * @param answerIndex which answer they chose, from 0
     */
    public void answer(final PlayerColor player, final int answerIndex) {
        if (phase != TurnPhase.TRIVIA_CONTEST || !contest.isAcceptingAnswers()) {
            return;
        }
        if (answerIndex < 0 || answerIndex >= contest.getQuestion().getAnswerCount()) {
            return;
        }
        answeredBy = player;
        answeredIndex = answerIndex;
        resolveContest(contest.submitAnswer(player, answerIndex));
    }

    /**
     * Picks or un-picks one of the four dice for the contest winner.
     *
     * <p>Ignored outside the draft. Picking a second die completes the draft and
     * starts the winner's movement.</p>
     *
     * @param position which die, from 0 to 3
     */
    public void pickDie(final int position) {
        if (phase != TurnPhase.DICE_DRAFT || position < 0 || position >= DiceRoll.DICE_COUNT) {
            return;
        }
        // Clicking a picked die again puts it back, so a misclick can be undone.
        if (draftPicks.contains(position)) {
            draftPicks.remove(Integer.valueOf(position));
            fireChanged();
            return;
        }
        draftPicks.add(position);
        if (draftPicks.size() == 2) {
            draft = roll.draft(draftPicks.get(0), draftPicks.get(1));
            startMovement(TurnPhase.WINNER_MOVEMENT);
        }
        fireChanged();
    }

    /**
     * Handles a click on the board during movement.
     *
     * <p>The first click selects a checker that can move; the second click, on
     * a highlighted destination, moves it. Clicking the selected checker again
     * deselects it, and clicking a different movable checker switches to it.
     * Ignored outside the movement phases.</p>
     *
     * @param location a point from 1 to 24, {@link Move#BAR}, or {@link Move#OFF}
     */
    public void clickLocation(final int location) {
        if (!isMovementPhase() || turn.isOver()) {
            return;
        }
        if (selectedSource != null) {
            if (location == selectedSource) {
                selectedSource = null;
                fireChanged();
                return;
            }
            // Smallest die first: when bearing off, both a 5 and a 6 might work,
            // and spending the smaller keeps more options open.
            for (final Move move : turn.legalMovesFrom(selectedSource)) {
                if (move.getTo() == location) {
                    playMove(move);
                    return;
                }
            }
        }
        selectedSource = turn.legalSources().contains(location) ? location : null;
        fireChanged();
    }

    // ------------------------------------------------------------------
    // State for the views
    // ------------------------------------------------------------------

    /**
     * Returns where in the round the game is.
     *
     * @return the current phase
     */
    public TurnPhase getPhase() {
        return phase;
    }

    /**
     * Returns which round this is.
     *
     * @return the round number, from 1
     */
    public int getRoundNumber() {
        return roundNumber;
    }

    /**
     * Returns the live board, for drawing.
     *
     * @return the board; do not change it
     */
    public Board getBoard() {
        return game.getBoard();
    }

    /**
     * Returns this round's four dice.
     *
     * @return the roll
     */
    public DiceRoll getRoll() {
        return roll;
    }

    /**
     * Returns the dice picked so far during the draft.
     *
     * @return roll positions, in the order picked
     */
    public List<Integer> getDraftPicks() {
        return Collections.unmodifiableList(draftPicks);
    }

    /**
     * Returns how the dice were split, once the draft is complete.
     *
     * @return the split, or empty before it is made
     */
    public Optional<DiceDraft> getDraft() {
        return Optional.ofNullable(draft);
    }

    /**
     * Returns this round's question.
     *
     * @return the question being, or last, contested
     */
    public TriviaQuestion getQuestion() {
        return contest.getQuestion();
    }

    /**
     * Returns whether the question is still open for answers.
     *
     * @return {@code true} while players may still answer
     */
    public boolean isQuestionOpen() {
        return phase == TurnPhase.TRIVIA_CONTEST && contest.isAcceptingAnswers();
    }

    /**
     * Returns how the contest ended.
     *
     * @return the result, or empty while the question is open
     */
    public Optional<ContestResult> getContestResult() {
        return contest.getResult();
    }

    /**
     * Returns which side's answer decided the contest.
     *
     * @return the side, or empty if the contest is open or timed out
     */
    public Optional<PlayerColor> getAnsweredBy() {
        return Optional.ofNullable(answeredBy);
    }

    /**
     * Returns the answer that decided the contest.
     *
     * @return its index, or -1 if the contest is open or timed out
     */
    public int getAnsweredIndex() {
        return answeredIndex;
    }

    /**
     * Returns how much of the answering time is left.
     *
     * @return a fraction from 1 (just started) to 0 (time up)
     */
    public double getTimeRemainingFraction() {
        return remainingMillis / (double) CONTEST_MILLIS;
    }

    /**
     * Returns the side currently in control of the game.
     *
     * @return the side that may act, or empty during the open contest (when
     *         both may) and once the game is over (when neither may)
     */
    public Optional<PlayerColor> getActivePlayer() {
        if (phase == TurnPhase.TRIVIA_CONTEST || phase == TurnPhase.GAME_OVER) {
            return Optional.empty();
        }
        if (phase == TurnPhase.LOSER_MOVEMENT) {
            return Optional.of(contestWinner().opponent());
        }
        return Optional.of(contestWinner());
    }

    /**
     * Returns the die values the mover still has.
     *
     * @return the values, largest first, or an empty list outside movement
     */
    public List<Integer> getRemainingDice() {
        return turn == null ? Collections.emptyList() : turn.getRemainingDice();
    }

    /**
     * Returns the locations the mover could pick a checker up from.
     *
     * @return points or {@link Move#BAR}; empty outside movement
     */
    public Set<Integer> getLegalSources() {
        if (!isMovementPhase()) {
            return Collections.emptySet();
        }
        return turn.legalSources();
    }

    /**
     * Returns the selected checker's location.
     *
     * @return a point or {@link Move#BAR}, or empty if nothing is selected
     */
    public OptionalInt getSelectedSource() {
        return selectedSource == null ? OptionalInt.empty() : OptionalInt.of(selectedSource);
    }

    /**
     * Returns where the selected checker could move to.
     *
     * @return points or {@link Move#OFF}; empty if nothing is selected
     */
    public Set<Integer> getLegalTargets() {
        final Set<Integer> targets = new TreeSet<>();
        if (selectedSource != null && isMovementPhase()) {
            for (final Move move : turn.legalMovesFrom(selectedSource)) {
                targets.add(move.getTo());
            }
        }
        return targets;
    }

    /**
     * Returns a description of the most recent notable event.
     *
     * @return a sentence such as "White answered correctly.", or empty
     */
    public Optional<String> getNotice() {
        return Optional.ofNullable(notice);
    }

    /**
     * Returns the winner of the game.
     *
     * @return the side that has borne off every checker, or empty while play goes on
     */
    public Optional<PlayerColor> getWinner() {
        return game.getWinner();
    }

    // ------------------------------------------------------------------
    // Phase transitions
    // ------------------------------------------------------------------

    /** Rolls the dice, puts up a question, and starts the countdown. */
    private void startRound() {
        epoch++;
        roundNumber++;
        phase = TurnPhase.TRIVIA_CONTEST;
        roll = game.rollDice();
        contest = new TriviaContest(questions.get());
        answeredBy = null;
        answeredIndex = -1;
        remainingMillis = CONTEST_MILLIS;
        draftPicks.clear();
        draft = null;
        turn = null;
        selectedSource = null;
        notice = null;
        scheduleTick();
        fireChanged();
    }

    /** Arranges for the countdown to advance one tick. */
    private void scheduleTick() {
        later(TICK_MILLIS, () -> {
            if (!isQuestionOpen()) {
                // Someone answered; the countdown simply stops.
                return;
            }
            remainingMillis = Math.max(0, remainingMillis - TICK_MILLIS);
            if (remainingMillis == 0) {
                resolveContest(contest.resolveByTimeout(random));
            } else {
                scheduleTick();
                fireChanged();
            }
        });
    }

    /**
     * Shows the contest outcome, then moves on to the draft after a pause.
     *
     * @param result how the contest ended
     */
    private void resolveContest(final ContestResult result) {
        final String winner = nameOf(result.getWinner());
        switch (result.getReason()) {
            case ANSWERED_CORRECTLY:
                notice = winner + " answered correctly!";
                break;
            case OPPONENT_ANSWERED_INCORRECTLY:
                notice = nameOf(result.getLoser()) + " answered wrong, so " + winner + " goes first.";
                break;
            default:
                notice = "Time's up! " + winner + " was drawn to go first.";
                break;
        }
        later(REVEAL_MILLIS, this::startDraft);
        fireChanged();
    }

    /** Hands control to the contest winner to pick two dice. */
    private void startDraft() {
        phase = TurnPhase.DICE_DRAFT;
        fireChanged();
    }

    /**
     * Starts the winner's or the loser's movement.
     *
     * @param movementPhase {@link TurnPhase#WINNER_MOVEMENT} or
     *                      {@link TurnPhase#LOSER_MOVEMENT}
     */
    private void startMovement(final TurnPhase movementPhase) {
        phase = movementPhase;
        final boolean winnerMoves = movementPhase == TurnPhase.WINNER_MOVEMENT;
        final PlayerColor mover = winnerMoves ? contestWinner() : contestWinner().opponent();
        turn = game.startTurn(mover, winnerMoves ? draft.getWinnerDice() : draft.getLoserDice());
        selectedSource = null;
        if (turn.isOver()) {
            finishTurn();
        } else {
            autoSelectBar();
        }
        fireChanged();
    }

    /**
     * Makes a move and deals with whatever follows from it.
     *
     * @param move a legal move for the current turn
     */
    private void playMove(final Move move) {
        final boolean hit = turn.play(move);
        selectedSource = null;
        notice = hit ? nameOf(turn.getPlayer()) + " hit a checker!" : null;
        if (turn.isOver()) {
            finishTurn();
        } else {
            autoSelectBar();
        }
        fireChanged();
    }

    /**
     * Wraps up a movement turn that has just ended: declares a winner, or
     * moves play on, pausing first if the player was left with no legal move.
     */
    private void finishTurn() {
        final PlayerColor mover = turn.getPlayer();
        if (game.isOver()) {
            phase = TurnPhase.GAME_OVER;
            notice = nameOf(mover) + " bore off every checker!";
            return;
        }
        if (turn.isBlocked()) {
            // Leave the board up for a moment so players can see why.
            notice = nameOf(mover) + " has no legal moves.";
            later(BLOCKED_PAUSE_MILLIS, this::advanceAfterMovement);
        } else {
            advanceAfterMovement();
        }
    }

    /** Moves from the winner's movement to the loser's, or from the loser's to a new round. */
    private void advanceAfterMovement() {
        if (phase == TurnPhase.WINNER_MOVEMENT) {
            startMovement(TurnPhase.LOSER_MOVEMENT);
        } else {
            startRound();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Selects the bar automatically when entering from it is the only thing the
     * mover can do, saving a pointless click.
     */
    private void autoSelectBar() {
        final Set<Integer> sources = turn.legalSources();
        if (sources.size() == 1 && sources.contains(Move.BAR)) {
            selectedSource = Move.BAR;
        }
    }

    /**
     * Returns whether a movement turn is underway.
     *
     * @return {@code true} in either movement phase
     */
    private boolean isMovementPhase() {
        return phase == TurnPhase.WINNER_MOVEMENT || phase == TurnPhase.LOSER_MOVEMENT;
    }

    /**
     * Returns the side that won this round's contest.
     *
     * @return the winner
     * @throws IllegalStateException if the contest is still open
     */
    private PlayerColor contestWinner() {
        return contest.getResult()
                .orElseThrow(() -> new IllegalStateException("Contest still open"))
                .getWinner();
    }

    /**
     * Schedules a task that only runs if no new round or game has started in
     * the meantime.
     *
     * @param delayMillis how long to wait
     * @param task        what to run
     */
    private void later(final int delayMillis, final Runnable task) {
        final int scheduledEpoch = epoch;
        scheduler.schedule(delayMillis, () -> {
            if (scheduledEpoch == epoch) {
                task.run();
            }
        });
    }

    /** Tells every listener the state has changed. */
    private void fireChanged() {
        for (final GameListener listener : listeners) {
            listener.gameChanged();
        }
    }
}
