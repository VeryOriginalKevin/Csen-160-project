package backgammon.model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Checks for the board, dice, and movement rules. From the project root:
 * <pre>
 * javac -d out -sourcepath src test/backgammon/model/*.java
 * java -cp out backgammon.model.ModelChecks
 * </pre>
 *
 * <p>Plain {@code main}-method checks, like {@code TriviaChecks}: each check
 * prints only when it fails, and the run exits non-zero if any did. The class
 * sits in {@code backgammon.model} so it can build test positions with the
 * board's package-private setters.</p>
 */
public final class ModelChecks {

    /** How many checks have run. */
    private int checksRun;

    /** How many checks have failed. */
    private int checksFailed;

    /** Not instantiated from outside; {@link #main} makes the one instance. */
    private ModelChecks() {
        this.checksRun = 0;
        this.checksFailed = 0;
    }

    /**
     * Runs every check and reports the tally.
     *
     * @param args ignored
     */
    public static void main(final String[] args) {
        final ModelChecks checks = new ModelChecks();
        checks.checkOpeningPosition();
        checks.checkDiceAndDraft();
        checks.checkDicePool();
        checks.checkBarEntry();
        checks.checkBlockingAndHitting();
        checks.checkBearingOff();
        checks.checkMovementTurn();
        checks.checkWinner();
        checks.report();
    }

    /** The opening position has fifteen checkers a side, mirrored. */
    private void checkOpeningPosition() {
        final Board board = Board.standard();
        for (final PlayerColor color : PlayerColor.values()) {
            int total = 0;
            for (int point = 1; point <= Board.POINT_COUNT; point++) {
                total += board.countAt(point, color);
            }
            check(color + " starts with 15 checkers", total == Board.CHECKERS_PER_SIDE);
            check(color + " starts with none on the bar or off",
                    board.barCount(color) == 0 && board.offCount(color) == 0);
        }
        check("WHITE has 2 on point 24", board.countAt(24, PlayerColor.WHITE) == 2);
        check("BLACK has 2 on point 1", board.countAt(1, PlayerColor.BLACK) == 2);
        check("WHITE has 5 on point 6", board.countAt(6, PlayerColor.WHITE) == 5);
        check("BLACK has 5 on point 19", board.countAt(19, PlayerColor.BLACK) == 5);
        check("nobody starts all home", !board.isAllHome(PlayerColor.WHITE));

        // A copy must not share state with the original.
        final Board copy = board.copy();
        copy.apply(new Move(6, 5, 1), PlayerColor.WHITE);
        check("copy is independent", board.countAt(6, PlayerColor.WHITE) == 5);
    }

    /** Four dice are rolled, and a draft splits them two and two. */
    private void checkDiceAndDraft() {
        final DiceRoll roll = DiceRoll.roll(new Random(7));
        for (int i = 0; i < DiceRoll.DICE_COUNT; i++) {
            final int face = roll.get(i).getValue();
            check("die " + i + " shows a face from 1 to 6", face >= Die.MIN_VALUE && face <= Die.MAX_VALUE);
        }

        final DiceRoll known = rollOf(1, 2, 3, 4);
        final DiceDraft draft = known.draft(3, 1);
        check("winner gets the picked dice, in roll order",
                values(draft.getWinnerDice()).equals(Arrays.asList(2, 4)));
        check("loser gets the rest", values(draft.getLoserDice()).equals(Arrays.asList(1, 3)));
        check("draft remembers positions", draft.isWinnerDie(1) && draft.isWinnerDie(3) && !draft.isWinnerDie(0));
        check("picking one die twice is rejected", throwsIllegalArgument(() -> known.draft(2, 2)));
        check("picking off the end is rejected", throwsIllegalArgument(() -> known.draft(0, 4)));
    }

    /** A pool spends one value per move, and doubles give four moves. */
    private void checkDicePool() {
        final DicePool mixed = new DicePool(Arrays.asList(new Die(3), new Die(5)));
        check("two different dice give two moves", mixed.getRemaining().equals(Arrays.asList(5, 3)));
        mixed.spend(3);
        check("spending removes one value", mixed.getRemaining().equals(Arrays.asList(5)));
        check("spending a missing value is rejected", throwsIllegalArgument(() -> mixed.spend(3)));

        final DicePool doubles = new DicePool(Arrays.asList(new Die(4), new Die(4)));
        check("doubles give four moves", doubles.getRemaining().size() == DicePool.MOVES_FOR_DOUBLES);
        check("doubles have one distinct value", doubles.getDistinctValues().size() == 1);
    }

    /** Checkers on the bar must enter first, and only onto open points. */
    private void checkBarEntry() {
        final RuleBook rules = new RuleBook();
        final Board board = Board.empty();
        board.setBar(PlayerColor.WHITE, 1);
        board.setPoint(13, PlayerColor.WHITE, 14);
        board.setPoint(22, PlayerColor.BLACK, 2);

        final DicePool pool = new DicePool(Arrays.asList(new Die(3), new Die(5)));
        final List<Move> moves = rules.legalMoves(board, PlayerColor.WHITE, pool);
        check("WHITE enters with a 5 on point 20", moves.contains(new Move(Move.BAR, 20, 5)));
        check("WHITE cannot enter on blocked point 22", !moves.contains(new Move(Move.BAR, 22, 3)));
        check("only entries are legal while on the bar", moves.size() == 1);

        final Board black = Board.empty();
        black.setBar(PlayerColor.BLACK, 1);
        black.setPoint(12, PlayerColor.BLACK, 14);
        final List<Move> blackMoves = rules.legalMoves(black, PlayerColor.BLACK,
                new DicePool(Arrays.asList(new Die(2), new Die(2))));
        check("BLACK enters with a 2 on point 2", blackMoves.equals(Arrays.asList(new Move(Move.BAR, 2, 2))));

        // Fully blocked entry: no legal move at all.
        board.setPoint(20, PlayerColor.BLACK, 2);
        check("a closed entry leaves no moves", rules.legalMoves(board, PlayerColor.WHITE, pool).isEmpty());
    }

    /** Two or more opponents block a point; a lone one is hit. */
    private void checkBlockingAndHitting() {
        final RuleBook rules = new RuleBook();
        final Board board = Board.empty();
        board.setPoint(10, PlayerColor.WHITE, 1);
        board.setPoint(8, PlayerColor.BLACK, 2);
        board.setPoint(7, PlayerColor.BLACK, 1);

        final DicePool pool = new DicePool(Arrays.asList(new Die(2), new Die(3)));
        final List<Move> moves = rules.legalMoves(board, PlayerColor.WHITE, pool);
        check("blocked point 8 is not a target", !moves.contains(new Move(10, 8, 2)));
        check("lone checker on 7 is a target", moves.contains(new Move(10, 7, 3)));

        final boolean hit = board.apply(new Move(10, 7, 3), PlayerColor.WHITE);
        check("landing on a lone checker hits it", hit);
        check("hit checker goes to the bar", board.barCount(PlayerColor.BLACK) == 1);
        check("hitter now owns the point", board.ownerOf(7) == PlayerColor.WHITE);
    }

    /** Bearing off needs everyone home, an exact die, or the rearmost checker. */
    private void checkBearingOff() {
        final RuleBook rules = new RuleBook();
        final Board board = Board.empty();
        board.setPoint(5, PlayerColor.WHITE, 1);
        board.setPoint(3, PlayerColor.WHITE, 1);
        board.setOff(PlayerColor.WHITE, 13);

        final List<Move> exact = rules.legalMoves(board, PlayerColor.WHITE,
                new DicePool(Arrays.asList(new Die(3), new Die(1))));
        check("exact die bears off from 3", exact.contains(new Move(3, Move.OFF, 3)));

        final List<Move> high = rules.legalMoves(board, PlayerColor.WHITE,
                new DicePool(Arrays.asList(new Die(6), new Die(6))));
        check("a 6 bears off the rearmost checker on 5", high.contains(new Move(5, Move.OFF, 6)));
        check("a 6 may not bear off from 3 while 5 is occupied", !high.contains(new Move(3, Move.OFF, 6)));

        // One checker outside home stops all bearing off.
        board.setPoint(9, PlayerColor.WHITE, 1);
        board.setOff(PlayerColor.WHITE, 12);
        final List<Move> notHome = rules.legalMoves(board, PlayerColor.WHITE,
                new DicePool(Arrays.asList(new Die(3), new Die(5))));
        for (final Move move : notHome) {
            check("no bearing off with a checker on 9: " + move, !move.isBearOff());
        }

        // BLACK mirrors it: point 22 is three pips from BLACK's edge.
        final Board black = Board.empty();
        black.setPoint(22, PlayerColor.BLACK, 1);
        black.setOff(PlayerColor.BLACK, 14);
        check("BLACK bears off from 22 with a 3", rules.legalMoves(black, PlayerColor.BLACK,
                new DicePool(Arrays.asList(new Die(3), new Die(1)))).contains(new Move(22, Move.OFF, 3)));
    }

    /** A turn spends its dice, refuses illegal moves, and notices being blocked. */
    private void checkMovementTurn() {
        final Board board = Board.standard();
        final MovementTurn turn = new MovementTurn(board, PlayerColor.WHITE,
                Arrays.asList(new Die(2), new Die(2)), new RuleBook());
        check("doubles allow four moves", turn.getRemainingDice().size() == 4);
        check("illegal move is rejected", throwsIllegalArgument(() -> turn.play(new Move(6, 1, 2))));
        // Point 6 holds five checkers, so all four twos can come from there.
        for (int i = 0; i < 4; i++) {
            turn.play(new Move(6, 4, 2));
        }
        check("turn is over once the dice are spent", turn.isOver() && !turn.isBlocked());
        check("four checkers left point 6", board.countAt(6, PlayerColor.WHITE) == 1);

        // A checker on the bar facing a closed board: blocked immediately.
        final Board closed = Board.empty();
        closed.setBar(PlayerColor.WHITE, 1);
        closed.setPoint(1, PlayerColor.WHITE, 14);
        for (int point = 19; point <= 24; point++) {
            closed.setPoint(point, PlayerColor.BLACK, 2);
        }
        final MovementTurn stuck = new MovementTurn(closed, PlayerColor.WHITE,
                Arrays.asList(new Die(6), new Die(1)), new RuleBook());
        check("closed board blocks the turn", stuck.isOver() && stuck.isBlocked());
        check("blocked turn offers no sources", stuck.legalSources().isEmpty());
    }

    /** A side wins by bearing off all fifteen. */
    private void checkWinner() {
        final Board board = Board.empty();
        board.setPoint(1, PlayerColor.WHITE, 1);
        board.setOff(PlayerColor.WHITE, 14);
        board.setPoint(12, PlayerColor.BLACK, 15);
        final Game game = new Game(board, new Random(1));
        check("no winner yet", !game.isOver());
        final MovementTurn turn = game.startTurn(PlayerColor.WHITE, Arrays.asList(new Die(1), new Die(4)));
        turn.play(new Move(1, Move.OFF, 1));
        check("bearing off the last checker wins", game.getWinner().orElse(null) == PlayerColor.WHITE);
        check("turn ends when the game is won", turn.isOver() && !turn.isBlocked());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Builds a roll from four known faces.
     *
     * @param faces the four values, in order
     * @return the roll
     */
    private static DiceRoll rollOf(final int... faces) {
        final Die[] dice = new Die[faces.length];
        for (int i = 0; i < faces.length; i++) {
            dice[i] = new Die(faces[i]);
        }
        return new DiceRoll(Arrays.asList(dice));
    }

    /**
     * Converts dice to their face values.
     *
     * @param dice the dice
     * @return their values, in the same order
     */
    private static List<Integer> values(final List<Die> dice) {
        final Integer[] values = new Integer[dice.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = dice.get(i).getValue();
        }
        return Arrays.asList(values);
    }

    /**
     * Returns whether running some code throws {@link IllegalArgumentException}.
     *
     * @param code the code to run
     * @return {@code true} if it threw that exception
     */
    private static boolean throwsIllegalArgument(final Runnable code) {
        try {
            code.run();
            return false;
        } catch (final IllegalArgumentException expected) {
            return true;
        }
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
