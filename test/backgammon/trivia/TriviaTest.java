package backgammon.trivia;

import backgammon.model.PlayerColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * JUnit tests for the trivia package.
 *
 * <p>Sits in {@code backgammon.trivia} so it can reach package-private
 * members such as {@link JsonParser} if a test ever needs to.</p>
 *
 * <p>Live-API tests are skipped, not failed, when opentdb.com cannot be
 * reached: the game is designed to work offline, so an unreachable API is
 * not a defect.</p>
 */
public class TriviaTest {

    /** Seed used wherever a test needs reproducible randomness. */
    private static final long SEED = 42L;

    /** How long to wait for a background refill before giving up, in milliseconds. */
    private static final long REFILL_TIMEOUT_MILLIS = 20_000L;

    /** Checks that the parser reads every construct the API uses. */
    @Test
    void jsonParsing() {
        final String document = "{\"response_code\":0,\"results\":[{"
                + "\"text\":\"a\\u0026b \\\"quoted\\\"\","
                + "\"number\":-12.5e1,\"flag\":true,\"nothing\":null,"
                + "\"emptyArray\":[],\"emptyObject\":{}}]}";
        final JsonValue root = JsonValue.parse(document);

        assertEquals(0, root.field("response_code").asInt(), "reads a top-level number");

        final JsonValue entry = root.field("results").elements().get(0);
        assertEquals("a&b \"quoted\"", entry.field("text").asString(), "resolves unicode and quote escapes");
        assertEquals(-125, entry.field("number").asInt(), "reads a negative exponent number");
        // Booleans, null and an empty object must parse even though the game
        // never reads them: a real response could contain any of them.
        assertTrue(entry.field("emptyArray").elements().isEmpty(), "reads an empty array");

        assertThrowsDocumented(() -> root.field("absent"), "rejects a request for an absent field");
        assertThrowsDocumented(() -> root.field("response_code").asString(),
                "rejects an accessor of the wrong kind");
    }

    /** Checks that malformed documents are rejected rather than half-read. */
    @Test
    void jsonRejectsMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> JsonValue.parse("{} stray"), "rejects trailing junk");
        assertThrows(IllegalArgumentException.class, () -> JsonValue.parse("{\"a\":\"b}"),
                "rejects an unterminated string");
        assertThrows(IllegalArgumentException.class, () -> JsonValue.parse("{\"a\":\"\\q\"}"),
                "rejects an unknown escape");
        assertThrows(IllegalArgumentException.class, () -> JsonValue.parse("{\"a\" 1}"), "rejects a missing colon");
        assertThrows(IllegalArgumentException.class, () -> JsonValue.parse("[1 2]"), "rejects a missing comma");
        assertThrows(IllegalArgumentException.class, () -> JsonValue.parse(""), "rejects empty input");
        assertThrows(IllegalArgumentException.class, () -> JsonValue.parse("{\"a\":tru}"),
                "rejects a truncated keyword");
    }

    /** Checks the invariants a question must hold however it was built. */
    @Test
    void questionInvariants() {
        final TriviaQuestion question = TriviaQuestion.of("Category", Difficulty.EASY,
                "Prompt?", "right", List.of("wrong one", "wrong two", "wrong three"), new Random(SEED));

        assertEquals(4, question.getAnswerCount(), "keeps every answer");
        assertEquals(1, countCorrect(question), "marks exactly one answer correct");
        assertEquals("right", question.getAnswers().get(correctIndexOf(question)),
                "the shuffled-in correct answer is the right one");
        assertFalse(question.toString().contains("right"), "does not leak the answer through toString");

        assertThrowsDocumented(() -> question.isCorrect(question.getAnswerCount()),
                "rejects an out-of-range answer index");

        assertThrows(IllegalArgumentException.class,
                () -> new TriviaQuestion("Category", Difficulty.EASY, "Prompt?", List.of("only one"), 0),
                "rejects a question with fewer than two answers");

        assertThrows(IllegalArgumentException.class,
                () -> new TriviaQuestion("Category", Difficulty.EASY, "Prompt?", List.of("a", "b"), 5),
                "rejects a correct index outside the answers");

        assertEquals(Difficulty.MEDIUM, Difficulty.fromApiValue("impossible"),
                "maps an unknown difficulty to a usable default");
        assertEquals(Difficulty.HARD, Difficulty.fromApiValue("HARD"), "maps a known difficulty");
        assertEquals("Hard", Difficulty.HARD.getDisplayLabel(), "labels a difficulty for display");
    }

    /** Checks the bundled source, including its behaviour past the pool size. */
    @Test
    void offlineSource() {
        final OfflineTriviaSource source = new OfflineTriviaSource(new Random(SEED));
        final List<TriviaQuestion> batch = source.fetchQuestions(4);

        assertEquals(4, batch.size(), "returns the number of questions asked for");
        for (final TriviaQuestion question : batch) {
            assertEquals(4, question.getAnswerCount(), "every bundled question offers four answers");
            assertEquals(1, countCorrect(question), "every bundled question has exactly one correct answer");
            assertFalse(question.getPrompt().isBlank(), "every bundled question has a prompt");
        }

        // More than the bundled pool holds, to force repeats.
        final int beyondPool = 100;
        assertEquals(beyondPool, source.fetchQuestions(beyondPool).size(),
                "serves more questions than the pool holds by repeating");

        // A new game gets a new source, even one seeded identically, and must
        // still not reopen with the questions the last game showed.
        OfflineTriviaSource.forgetShown();
        final Set<String> lastGame = new HashSet<>();
        for (final TriviaQuestion question : new OfflineTriviaSource(new Random(7)).fetchQuestions(6)) {
            lastGame.add(question.getPrompt());
        }
        assertEquals(6, lastGame.size(), "no bundled question repeats within a game");
        for (final TriviaQuestion question : new OfflineTriviaSource(new Random(7)).fetchQuestions(6)) {
            assertFalse(lastGame.contains(question.getPrompt()),
                    "a new game does not repeat the last game's questions");
        }

        assertThrowsDocumented(() -> source.fetchQuestions(0), "rejects a non-positive amount");
    }

    /** Checks the contest rules, which decide who moves first and drafts dice. */
    @Test
    void contestRules() {
        final TriviaQuestion question = new OfflineTriviaSource(new Random(1)).fetchQuestions(1).get(0);
        final int correct = correctIndexOf(question);
        final int incorrect = (correct + 1) % question.getAnswerCount();

        final TriviaContest answeredRight = new TriviaContest(question);
        assertTrue(answeredRight.isAcceptingAnswers(), "a new contest accepts answers");
        assertTrue(answeredRight.getResult().isEmpty(), "a new contest has no result");

        final ContestResult rightResult = answeredRight.submitAnswer(PlayerColor.WHITE, correct);
        assertEquals(PlayerColor.WHITE, rightResult.getWinner(), "a correct answer wins the contest");
        assertEquals(PlayerColor.BLACK, rightResult.getLoser(), "the other side loses");
        assertEquals(ContestResult.Reason.ANSWERED_CORRECTLY, rightResult.getReason(),
                "the reason is a correct answer");
        assertFalse(answeredRight.isAcceptingAnswers(), "a decided contest stops accepting answers");
        assertTrue(answeredRight.getResult().isPresent(), "a decided contest reports its result");

        final TriviaContest answeredWrong = new TriviaContest(question);
        final ContestResult wrongResult = answeredWrong.submitAnswer(PlayerColor.BLACK, incorrect);
        assertEquals(PlayerColor.WHITE, wrongResult.getWinner(),
                "an incorrect answer hands the contest to the opponent");
        assertEquals(ContestResult.Reason.OPPONENT_ANSWERED_INCORRECTLY, wrongResult.getReason(),
                "the reason is the opponent answering incorrectly");

        final ContestResult latePress = answeredWrong.submitAnswer(PlayerColor.BLACK, correct);
        assertEquals(PlayerColor.WHITE, latePress.getWinner(), "a late answer cannot change the winner");
        assertEquals(ContestResult.Reason.OPPONENT_ANSWERED_INCORRECTLY, latePress.getReason(),
                "a late answer cannot change the reason");

        final TriviaContest unanswered = new TriviaContest(question);
        final ContestResult drawn = unanswered.resolveByTimeout(new Random(SEED));
        assertFalse(unanswered.isAcceptingAnswers(), "a timeout decides the contest");
        assertEquals(ContestResult.Reason.NO_ANSWER_IN_TIME, drawn.getReason(), "the reason is that nobody answered");
        assertEquals(ContestResult.Reason.ANSWERED_CORRECTLY,
                answeredRight.resolveByTimeout(new Random(SEED)).getReason(),
                "a timeout cannot override a decided contest");

        assertThrowsDocumented(() -> new TriviaContest(question).submitAnswer(PlayerColor.WHITE, 99),
                "an out-of-range answer index is rejected");
    }

    /** Checks the title screen's topic and difficulty choice, offline. */
    @Test
    void settings() {
        final Random random = new Random(SEED);
        assertTrue(TriviaSettings.ANY.allows(TriviaCategory.ANIMALS, Difficulty.HARD), "any allows everything");

        final TriviaSettings geography = new TriviaSettings(
                EnumSet.of(TriviaCategory.GEOGRAPHY), EnumSet.of(Difficulty.EASY));
        assertTrue(geography.allows(TriviaCategory.GEOGRAPHY, Difficulty.EASY), "a choice allows a match");
        assertFalse(geography.allows(TriviaCategory.HISTORY, Difficulty.EASY), "a choice rejects another topic");
        assertFalse(geography.allows(TriviaCategory.GEOGRAPHY, Difficulty.HARD),
                "a choice rejects another difficulty");

        // Bundled questions honour the choice where they can...
        for (final TriviaQuestion question : new OfflineTriviaSource(random, geography).fetchQuestions(5)) {
            assertTrue(question.getCategory().equals(TriviaCategory.GEOGRAPHY.getDisplayName())
                    && question.getDifficulty() == Difficulty.EASY, "bundled questions follow the choice");
        }
        // ...and fall back to the whole pool when nothing bundled matches.
        final TriviaSettings unmatched = new TriviaSettings(
                EnumSet.of(TriviaCategory.ANIME_MANGA), EnumSet.noneOf(Difficulty.class));
        assertEquals(3, new OfflineTriviaSource(random, unmatched).fetchQuestions(3).size(),
                "an unmatched choice still yields questions");
    }

    /** Checks a live fetch, skipping if the API cannot be reached. */
    @Test
    void liveApi() {
        final List<TriviaQuestion> batch;
        try {
            batch = new OpenTriviaDatabase().fetchQuestions(3);
        } catch (final TriviaException unreachable) {
            assumeTrue(false, "live API unreachable: " + unreachable.getMessage());
            return;
        }
        assertFalse(batch.isEmpty(), "a live batch is not empty");
        for (final TriviaQuestion question : batch) {
            assertEquals(4, question.getAnswerCount(), "a live question offers four answers");
            assertFalse(question.getPrompt().isBlank(), "a live prompt is not blank");
            assertEquals(1, countCorrect(question), "a live question has exactly one correct answer");
            assertTrue(!question.getPrompt().contains("%20") && !question.getPrompt().contains("%27"),
                    "live text was decoded out of its URL encoding");
        }
    }

    /** Checks a live fetch narrowed to one topic and difficulty, skipping if offline. */
    @Test
    void liveApiWithSettings() {
        final List<TriviaQuestion> batch;
        try {
            batch = new OpenTriviaDatabase(new Random(SEED), TriviaCategory.SCIENCE_NATURE, Difficulty.EASY)
                    .fetchQuestions(3);
        } catch (final TriviaException unreachable) {
            assumeTrue(false, "narrowed live API unreachable: " + unreachable.getMessage());
            return;
        }
        for (final TriviaQuestion question : batch) {
            assertEquals(Difficulty.EASY, question.getDifficulty(),
                    "a narrowed live question has the chosen difficulty");
            assertTrue(question.getCategory().contains("Nature"), "a narrowed live question has the chosen topic");
        }
    }

    /** Checks that the deck always yields a question, and buffers live ones. */
    @Test
    void deck() throws InterruptedException {
        final QuestionDeck deck = QuestionDeck.createDefault(TriviaSettings.ANY);
        try {
            // Before any fetch has returned, the deck must still serve a question.
            assertNotNull(deck.next(), "the deck serves a question before any fetch returns");

            deck.start();
            final long deadline = System.currentTimeMillis() + REFILL_TIMEOUT_MILLIS;
            while (deck.getBufferedCount() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(200L);
            }

            if (deck.getBufferedCount() > 0) {
                final int before = deck.getBufferedCount();
                assertNotNull(deck.next(), "the deck serves a buffered question");
                assertTrue(deck.getBufferedCount() < before, "serving a question drains the buffer");
            } else {
                System.out.println("SKIPPED live refill: no live questions arrived in time");
            }
        } finally {
            deck.shutdown();
        }
        assertNotNull(QuestionDeck.createDefault(TriviaSettings.ANY).next(),
                "the deck still serves a question after shutdown");
    }

    /**
     * Checks that a deck with several supplies mixes them question by question,
     * rather than serving one topic's batch and then the next. Uses stand-in
     * sources, so it needs no network.
     */
    @Test
    void deckMixesTopics() throws InterruptedException {
        final QuestionDeck deck = new QuestionDeck(
                List.of(stubSource("Topic A"), stubSource("Topic B")), new OfflineTriviaSource(new Random(SEED)));
        try {
            deck.start();
            final long deadline = System.currentTimeMillis() + REFILL_TIMEOUT_MILLIS;
            while (deck.getBufferedCount() < 20 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20L);
            }
            assertTrue(deck.getBufferedCount() >= 20, "both supplies fill");

            // With two full supplies of ten, a deck that served one batch at a
            // time would give ten of one topic first. A mixing deck almost
            // certainly shows both within the first ten.
            final Set<String> firstTen = new HashSet<>();
            for (int i = 0; i < 10; i++) {
                firstTen.add(deck.next().getCategory());
            }
            assertEquals(2, firstTen.size(), "the first ten questions mix both topics");
        } finally {
            deck.shutdown();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Counts how many of a question's answers are marked correct.
     *
     * @param question the question to inspect
     * @return the number of answers reported correct, which must be one
     */
    private static int countCorrect(final TriviaQuestion question) {
        int correct = 0;
        for (int index = 0; index < question.getAnswerCount(); index++) {
            if (question.isCorrect(index)) {
                correct++;
            }
        }
        return correct;
    }

    /**
     * Finds the correct answer by asking the question about each position, the
     * same way the game does.
     *
     * @param question the question to inspect
     * @return the index of the first answer reported correct, or -1 if none is
     */
    private static int correctIndexOf(final TriviaQuestion question) {
        for (int index = 0; index < question.getAnswerCount(); index++) {
            if (question.isCorrect(index)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Builds a stand-in live source that instantly returns questions of one topic.
     *
     * @param topic the category every question will report
     * @return the source
     */
    private static TriviaSource stubSource(final String topic) {
        return amount -> {
            final List<TriviaQuestion> batch = new ArrayList<>();
            for (int i = 0; i < amount; i++) {
                batch.add(TriviaQuestion.of(topic, Difficulty.EASY, topic + " question " + i,
                        "right", List.of("a", "b", "c"), new Random(i)));
            }
            return batch;
        };
    }

    /**
     * Fails unless running {@code access} throws one of the exception types the
     * trivia classes document, so an unexpected failure such as a
     * {@code NullPointerException} is still seen rather than counted as a pass.
     *
     * @param access      the call to attempt
     * @param description what was being checked
     */
    private static void assertThrowsDocumented(final Executable access, final String description) {
        try {
            access.execute();
        } catch (final IllegalArgumentException | IllegalStateException | IndexOutOfBoundsException expected) {
            return;
        } catch (final Throwable other) {
            throw new AssertionError(description + ": unexpected exception " + other, other);
        }
        throw new AssertionError(description + ": expected an exception");
    }
}
