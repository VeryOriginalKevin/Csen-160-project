package backgammon.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the board, dice, and movement rules.
 *
 * <p>Sits in {@code backgammon.model} so it can build test positions with the
 * board's package-private setters.</p>
 */
public class ModelTest {

    /** The opening position has fifteen checkers a side, mirrored. */
    @Test
    void openingPosition() {
        final Board board = Board.standard();
        for (final PlayerColor color : PlayerColor.values()) {
            int total = 0;
            for (int point = 1; point <= Board.POINT_COUNT; point++) {
                total += board.countAt(point, color);
            }
            assertEquals(Board.CHECKERS_PER_SIDE, total, color + " starts with 15 checkers");
            assertEquals(0, board.barCount(color), color + " starts with none on the bar");
            assertEquals(0, board.offCount(color), color + " starts with none off");
        }
        assertEquals(2, board.countAt(24, PlayerColor.WHITE), "WHITE has 2 on point 24");
        assertEquals(2, board.countAt(1, PlayerColor.BLACK), "BLACK has 2 on point 1");
        assertEquals(5, board.countAt(6, PlayerColor.WHITE), "WHITE has 5 on point 6");
        assertEquals(5, board.countAt(19, PlayerColor.BLACK), "BLACK has 5 on point 19");
        assertFalse(board.isAllHome(PlayerColor.WHITE), "nobody starts all home");

        // A copy must not share state with the original.
        final Board copy = board.copy();
        copy.apply(new Move(6, 5, 1), PlayerColor.WHITE);
        assertEquals(5, board.countAt(6, PlayerColor.WHITE), "copy is independent");
    }

    /** Four dice are rolled, and a draft splits them two and two. */
    @Test
    void diceAndDraft() {
        final DiceRoll roll = DiceRoll.roll(new Random(7));
        for (int i = 0; i < DiceRoll.DICE_COUNT; i++) {
            final int face = roll.get(i).getValue();
            assertTrue(face >= Die.MIN_VALUE && face <= Die.MAX_VALUE, "die " + i + " shows a face from 1 to 6");
        }

        final DiceRoll known = rollOf(1, 2, 3, 4);
        final DiceDraft draft = known.draft(3, 1);
        assertEquals(Arrays.asList(2, 4), values(draft.getWinnerDice()),
                "winner gets the picked dice, in roll order");
        assertEquals(Arrays.asList(1, 3), values(draft.getLoserDice()), "loser gets the rest");
        assertTrue(draft.isWinnerDie(1) && draft.isWinnerDie(3) && !draft.isWinnerDie(0),
                "draft remembers positions");
        assertThrows(IllegalArgumentException.class, () -> known.draft(2, 2),
                "picking one die twice is rejected");
        assertThrows(IllegalArgumentException.class, () -> known.draft(0, 4),
                "picking off the end is rejected");
    }

    /** A pool spends one value per move, and doubles give four moves. */
    @Test
    void dicePool() {
        final DicePool mixed = new DicePool(Arrays.asList(new Die(3), new Die(5)));
        assertEquals(Arrays.asList(5, 3), mixed.getRemaining(), "two different dice give two moves");
        mixed.spend(3);
        assertEquals(Arrays.asList(5), mixed.getRemaining(), "spending removes one value");
        assertThrows(IllegalArgumentException.class, () -> mixed.spend(3), "spending a missing value is rejected");

        final DicePool doubles = new DicePool(Arrays.asList(new Die(4), new Die(4)));
        assertEquals(DicePool.MOVES_FOR_DOUBLES, doubles.getRemaining().size(), "doubles give four moves");
        assertEquals(1, doubles.getDistinctValues().size(), "doubles have one distinct value");
    }

    /** Checkers on the bar must enter first, and only onto open points. */
    @Test
    void barEntry() {
        final RuleBook rules = new RuleBook();
        final Board board = Board.empty();
        board.setBar(PlayerColor.WHITE, 1);
        board.setPoint(13, PlayerColor.WHITE, 14);
        board.setPoint(22, PlayerColor.BLACK, 2);

        final DicePool pool = new DicePool(Arrays.asList(new Die(3), new Die(5)));
        final List<Move> moves = rules.legalMoves(board, PlayerColor.WHITE, pool);
        assertTrue(moves.contains(new Move(Move.BAR, 20, 5)), "WHITE enters with a 5 on point 20");
        assertFalse(moves.contains(new Move(Move.BAR, 22, 3)), "WHITE cannot enter on blocked point 22");
        assertEquals(1, moves.size(), "only entries are legal while on the bar");

        final Board black = Board.empty();
        black.setBar(PlayerColor.BLACK, 1);
        black.setPoint(12, PlayerColor.BLACK, 14);
        final List<Move> blackMoves = rules.legalMoves(black, PlayerColor.BLACK,
                new DicePool(Arrays.asList(new Die(2), new Die(2))));
        assertEquals(Arrays.asList(new Move(Move.BAR, 2, 2)), blackMoves, "BLACK enters with a 2 on point 2");

        // Fully blocked entry: no legal move at all.
        board.setPoint(20, PlayerColor.BLACK, 2);
        assertTrue(rules.legalMoves(board, PlayerColor.WHITE, pool).isEmpty(), "a closed entry leaves no moves");
    }

    /** Two or more opponents block a point; a lone one is hit. */
    @Test
    void blockingAndHitting() {
        final RuleBook rules = new RuleBook();
        final Board board = Board.empty();
        board.setPoint(10, PlayerColor.WHITE, 1);
        board.setPoint(8, PlayerColor.BLACK, 2);
        board.setPoint(7, PlayerColor.BLACK, 1);

        final DicePool pool = new DicePool(Arrays.asList(new Die(2), new Die(3)));
        final List<Move> moves = rules.legalMoves(board, PlayerColor.WHITE, pool);
        assertFalse(moves.contains(new Move(10, 8, 2)), "blocked point 8 is not a target");
        assertTrue(moves.contains(new Move(10, 7, 3)), "lone checker on 7 is a target");

        final boolean hit = board.apply(new Move(10, 7, 3), PlayerColor.WHITE);
        assertTrue(hit, "landing on a lone checker hits it");
        assertEquals(1, board.barCount(PlayerColor.BLACK), "hit checker goes to the bar");
        assertEquals(PlayerColor.WHITE, board.ownerOf(7), "hitter now owns the point");
    }

    /** Bearing off needs everyone home, an exact die, or the rearmost checker. */
    @Test
    void bearingOff() {
        final RuleBook rules = new RuleBook();
        final Board board = Board.empty();
        board.setPoint(5, PlayerColor.WHITE, 1);
        board.setPoint(3, PlayerColor.WHITE, 1);
        board.setOff(PlayerColor.WHITE, 13);

        final List<Move> exact = rules.legalMoves(board, PlayerColor.WHITE,
                new DicePool(Arrays.asList(new Die(3), new Die(1))));
        assertTrue(exact.contains(new Move(3, Move.OFF, 3)), "exact die bears off from 3");

        final List<Move> high = rules.legalMoves(board, PlayerColor.WHITE,
                new DicePool(Arrays.asList(new Die(6), new Die(6))));
        assertTrue(high.contains(new Move(5, Move.OFF, 6)), "a 6 bears off the rearmost checker on 5");
        assertFalse(high.contains(new Move(3, Move.OFF, 6)), "a 6 may not bear off from 3 while 5 is occupied");

        // One checker outside home stops all bearing off.
        board.setPoint(9, PlayerColor.WHITE, 1);
        board.setOff(PlayerColor.WHITE, 12);
        final List<Move> notHome = rules.legalMoves(board, PlayerColor.WHITE,
                new DicePool(Arrays.asList(new Die(3), new Die(5))));
        for (final Move move : notHome) {
            assertFalse(move.isBearOff(), "no bearing off with a checker on 9: " + move);
        }

        // BLACK mirrors it: point 22 is three pips from BLACK's edge.
        final Board black = Board.empty();
        black.setPoint(22, PlayerColor.BLACK, 1);
        black.setOff(PlayerColor.BLACK, 14);
        assertTrue(rules.legalMoves(black, PlayerColor.BLACK,
                        new DicePool(Arrays.asList(new Die(3), new Die(1)))).contains(new Move(22, Move.OFF, 3)),
                "BLACK bears off from 22 with a 3");
    }

    /** A turn spends its dice, refuses illegal moves, and notices being blocked. */
    @Test
    void movementTurn() {
        final Board board = Board.standard();
        final MovementTurn turn = new MovementTurn(board, PlayerColor.WHITE,
                Arrays.asList(new Die(2), new Die(2)), new RuleBook());
        assertEquals(4, turn.getRemainingDice().size(), "doubles allow four moves");
        assertThrows(IllegalArgumentException.class, () -> turn.play(new Move(6, 1, 2)),
                "illegal move is rejected");
        // Point 6 holds five checkers, so all four twos can come from there.
        for (int i = 0; i < 4; i++) {
            turn.play(new Move(6, 4, 2));
        }
        assertTrue(turn.isOver() && !turn.isBlocked(), "turn is over once the dice are spent");
        assertEquals(1, board.countAt(6, PlayerColor.WHITE), "four checkers left point 6");

        // A checker on the bar facing a closed board: blocked immediately.
        final Board closed = Board.empty();
        closed.setBar(PlayerColor.WHITE, 1);
        closed.setPoint(1, PlayerColor.WHITE, 14);
        for (int point = 19; point <= 24; point++) {
            closed.setPoint(point, PlayerColor.BLACK, 2);
        }
        final MovementTurn stuck = new MovementTurn(closed, PlayerColor.WHITE,
                Arrays.asList(new Die(6), new Die(1)), new RuleBook());
        assertTrue(stuck.isOver() && stuck.isBlocked(), "closed board blocks the turn");
        assertTrue(stuck.legalSources().isEmpty(), "blocked turn offers no sources");
    }

    /** A side wins by bearing off all fifteen. */
    @Test
    void winner() {
        final Board board = Board.empty();
        board.setPoint(1, PlayerColor.WHITE, 1);
        board.setOff(PlayerColor.WHITE, 14);
        board.setPoint(12, PlayerColor.BLACK, 15);
        final Game game = new Game(board, new Random(1));
        assertFalse(game.isOver(), "no winner yet");
        final MovementTurn turn = game.startTurn(PlayerColor.WHITE, Arrays.asList(new Die(1), new Die(4)));
        turn.play(new Move(1, Move.OFF, 1));
        assertEquals(PlayerColor.WHITE, game.getWinner().orElse(null), "bearing off the last checker wins");
        assertTrue(turn.isOver() && !turn.isBlocked(), "turn ends when the game is won");
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
}
