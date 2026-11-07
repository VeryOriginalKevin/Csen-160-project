package backgammon.trivia;

import backgammon.model.PlayerColor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Checks for the trivia package. From the project root:
 * <pre>
 * javac -d out -sourcepath src test/backgammon/trivia/*.java
 * java -cp out backgammon.trivia.TriviaChecks
 * </pre>
 *
 * <p>Hand-rolled rather than written against JUnit, so the project keeps its
 * promise of no external dependencies and the checks can be run with nothing but
 * a JDK. Each check prints only when it fails; the run ends with a count and a
 * non-zero exit status if anything failed.</p>
 *
 * <p>The class sits in {@code backgammon.trivia} so it can reach package-private
 * members such as {@link JsonParser} if a check ever needs to.</p>
 *
 * <p>Network checks are skipped, not failed, when opentdb.com cannot be reached:
 * the game is designed to work offline, so an unreachable API is not a defect.</p>
 */
public final class TriviaChecks {

    /** Seed used wherever a check needs reproducible randomness. */
    private static final long SEED = 42L;

    /** How long to wait for a background refill before giving up, in milliseconds. */
    private static final long REFILL_TIMEOUT_MILLIS = 20_000L;

    /** How many checks have run. */
    private int checksRun;

    /** How many checks have failed. */
    private int checksFailed;

    /** Not instantiated from outside; {@link #main} makes the one instance. */
    private TriviaChecks() {
        this.checksRun = 0;
        this.checksFailed = 0;
    }

    /**
     * Runs every check and reports the tally.
     *
     * @param args ignored
     * @throws InterruptedException if waiting for a background refill is interrupted
     */
    public static void main(final String[] args) throws InterruptedException {
        final TriviaChecks checks = new TriviaChecks();
        checks.checkJsonParsing();
        checks.checkJsonRejectsMalformedInput();
        checks.checkQuestionInvariants();
        checks.checkOfflineSource();
        checks.checkContestRules();
        checks.checkSettings();
        checks.checkLiveApi();
        checks.checkLiveApiWithSettings();
        checks.checkDeck();
        checks.checkDeckMixesTopics();
        checks.report();
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

    /**
     * Prints the tally and exits non-zero if any check failed.
     */
    private void report() {
        System.out.println(checksRun + " checks, " + checksFailed + " failures");
        if (checksFailed > 0) {
            System.exit(1);
        }
    }

    /** Checks that the parser reads every construct the API uses. */
    private void checkJsonParsing() {
        final String document = "{\"response_code\":0,\"results\":[{"
                + "\"text\":\"a\\u0026b \\\"quoted\\\"\","
                + "\"number\":-12.5e1,\"flag\":true,\"nothing\":null,"
                + "\"emptyArray\":[],\"emptyObject\":{}}]}";
        final JsonValue root = JsonValue.parse(document);

        check("reads a top-level number", root.field("response_code").asInt() == 0);

        final JsonValue entry = root.field("results").elements().get(0);
        check("resolves unicode and quote escapes",
                entry.field("text").asString().equals("a&b \"quoted\""));
        check("reads a negative exponent number", entry.field("number").asInt() == -125);
        // Booleans, null and an empty object must parse even though the game
        // never reads them: a real response could contain any of them.
        check("reads an empty array", entry.field("emptyArray").elements().isEmpty());

        check("rejects a request for an absent field",
                throwsOnAccess(() -> root.field("absent")));
        check("rejects an accessor of the wrong kind",
                throwsOnAccess(() -> root.field("response_code").asString()));
    }

    /** Checks that malformed documents are rejected rather than half-read. */
    private void checkJsonRejectsMalformedInput() {
        check("rejects trailing junk", isRejected("{} stray"));
        check("rejects an unterminated string", isRejected("{\"a\":\"b}"));
        check("rejects an unknown escape", isRejected("{\"a\":\"\\q\"}"));
        check("rejects a missing colon", isRejected("{\"a\" 1}"));
        check("rejects a missing comma", isRejected("[1 2]"));
        check("rejects empty input", isRejected(""));
        check("rejects a truncated keyword", isRejected("{\"a\":tru}"));
    }

    /**
     * Returns whether parsing a document fails as it should.
     *
     * @param document the text to parse
     * @return {@code true} if parsing was rejected
     */
    private static boolean isRejected(final String document) {
        try {
            JsonValue.parse(document);
            return false;
        } catch (final IllegalArgumentException expected) {
            return true;
        }
    }

    /**
     * Returns whether a call refused a bad request, as it should.
     *
     * <p>Only the three exception types the trivia classes document are treated
     * as a pass, so an unexpected failure such as a {@code NullPointerException}
     * still escapes and is seen rather than being counted as success.</p>
     *
     * @param access the call to attempt
     * @return {@code true} if the call threw one of the documented exceptions
     */
    private static boolean throwsOnAccess(final Runnable access) {
        try {
            access.run();
            return false;
        } catch (final IllegalArgumentException
                | IllegalStateException
                | IndexOutOfBoundsException expected) {
            return true;
        }
    }

    /** Checks the invariants a question must hold however it was built. */
    private void checkQuestionInvariants() {
        final TriviaQuestion question = TriviaQuestion.of("Category", Difficulty.EASY,
                "Prompt?", "right", List.of("wrong one", "wrong two", "wrong three"),
                new Random(SEED));

        check("keeps every answer", question.getAnswerCount() == 4);
        check("marks exactly one answer correct", countCorrect(question) == 1);
        check("the shuffled-in correct answer is the right one",
                question.getAnswers().get(correctIndexOf(question)).equals("right"));
        check("does not leak the answer through toString",
                !question.toString().contains("right"));

        check("rejects an out-of-range answer index", throwsOnAccess(() -> {
            question.isCorrect(question.getAnswerCount());
        }));

        boolean rejectedTooFewAnswers = false;
        try {
            new TriviaQuestion("Category", Difficulty.EASY, "Prompt?",
                    List.of("only one"), 0);
        } catch (final IllegalArgumentException expected) {
            rejectedTooFewAnswers = true;
        }
        check("rejects a question with fewer than two answers", rejectedTooFewAnswers);

        boolean rejectedBadIndex = false;
        try {
            new TriviaQuestion("Category", Difficulty.EASY, "Prompt?",
                    List.of("a", "b"), 5);
        } catch (final IllegalArgumentException expected) {
            rejectedBadIndex = true;
        }
        check("rejects a correct index outside the answers", rejectedBadIndex);

        check("maps an unknown difficulty to a usable default",
                Difficulty.fromApiValue("impossible") == Difficulty.MEDIUM);
        check("maps a known difficulty", Difficulty.fromApiValue("HARD") == Difficulty.HARD);
        check("labels a difficulty for display",
                Difficulty.HARD.getDisplayLabel().equals("Hard"));
    }

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

    /** Checks the bundled source, including its behaviour past the pool size. */
    private void checkOfflineSource() {
        final OfflineTriviaSource source = new OfflineTriviaSource(new Random(SEED));
        final List<TriviaQuestion> batch = source.fetchQuestions(4);

        check("returns the number of questions asked for", batch.size() == 4);
        for (final TriviaQuestion question : batch) {
            check("every bundled question offers four answers",
                    question.getAnswerCount() == 4);
            check("every bundled question has exactly one correct answer",
                    countCorrect(question) == 1);
            check("every bundled question has a prompt", !question.getPrompt().isBlank());
        }

        // More than the bundled pool holds, to force repeats.
        final int beyondPool = 100;
        check("serves more questions than the pool holds by repeating",
                source.fetchQuestions(beyondPool).size() == beyondPool);

        // A new game gets a new source, even one seeded identically, and must
        // still not reopen with the questions the last game showed.
        OfflineTriviaSource.forgetShown();
        final Set<String> lastGame = new HashSet<>();
        for (final TriviaQuestion question : new OfflineTriviaSource(new Random(7)).fetchQuestions(6)) {
            lastGame.add(question.getPrompt());
        }
        check("no bundled question repeats within a game", lastGame.size() == 6);
        for (final TriviaQuestion question : new OfflineTriviaSource(new Random(7)).fetchQuestions(6)) {
            check("a new game does not repeat the last game's questions",
                    !lastGame.contains(question.getPrompt()));
        }

        check("rejects a non-positive amount", throwsOnAccess(() -> {
            source.fetchQuestions(0);
        }));
    }

    /** Checks the contest rules, which decide who moves first and drafts dice. */
    private void checkContestRules() {
        final TriviaQuestion question =
                new OfflineTriviaSource(new Random(1)).fetchQuestions(1).get(0);
        final int correct = correctIndexOf(question);
        final int incorrect = (correct + 1) % question.getAnswerCount();

        final TriviaContest answeredRight = new TriviaContest(question);
        check("a new contest accepts answers", answeredRight.isAcceptingAnswers());
        check("a new contest has no result", answeredRight.getResult().isEmpty());

        final ContestResult rightResult = answeredRight.submitAnswer(PlayerColor.WHITE, correct);
        check("a correct answer wins the contest",
                rightResult.getWinner() == PlayerColor.WHITE);
        check("the other side loses", rightResult.getLoser() == PlayerColor.BLACK);
        check("the reason is a correct answer",
                rightResult.getReason() == ContestResult.Reason.ANSWERED_CORRECTLY);
        check("a decided contest stops accepting answers",
                !answeredRight.isAcceptingAnswers());
        check("a decided contest reports its result",
                answeredRight.getResult().isPresent());

        final TriviaContest answeredWrong = new TriviaContest(question);
        final ContestResult wrongResult =
                answeredWrong.submitAnswer(PlayerColor.BLACK, incorrect);
        check("an incorrect answer hands the contest to the opponent",
                wrongResult.getWinner() == PlayerColor.WHITE);
        check("the reason is the opponent answering incorrectly",
                wrongResult.getReason()
                        == ContestResult.Reason.OPPONENT_ANSWERED_INCORRECTLY);

        final ContestResult latePress = answeredWrong.submitAnswer(PlayerColor.BLACK, correct);
        check("a late answer cannot change the winner",
                latePress.getWinner() == PlayerColor.WHITE);
        check("a late answer cannot change the reason",
                latePress.getReason()
                        == ContestResult.Reason.OPPONENT_ANSWERED_INCORRECTLY);

        final TriviaContest unanswered = new TriviaContest(question);
        final ContestResult drawn = unanswered.resolveByTimeout(new Random(SEED));
        check("a timeout decides the contest", !unanswered.isAcceptingAnswers());
        check("the reason is that nobody answered",
                drawn.getReason() == ContestResult.Reason.NO_ANSWER_IN_TIME);
        check("a timeout cannot override a decided contest",
                answeredRight.resolveByTimeout(new Random(SEED)).getReason()
                        == ContestResult.Reason.ANSWERED_CORRECTLY);

        check("an out-of-range answer index is rejected", throwsOnAccess(() -> {
            new TriviaContest(question).submitAnswer(PlayerColor.WHITE, 99);
        }));
    }

    /** Checks the title screen's topic and difficulty choice, offline. */
    private void checkSettings() {
        final Random random = new Random(SEED);
        check("any allows everything",
                TriviaSettings.ANY.allows(TriviaCategory.ANIMALS, Difficulty.HARD));

        final TriviaSettings geography = new TriviaSettings(
                EnumSet.of(TriviaCategory.GEOGRAPHY), EnumSet.of(Difficulty.EASY));
        check("a choice allows a match",
                geography.allows(TriviaCategory.GEOGRAPHY, Difficulty.EASY));
        check("a choice rejects another topic",
                !geography.allows(TriviaCategory.HISTORY, Difficulty.EASY));
        check("a choice rejects another difficulty",
                !geography.allows(TriviaCategory.GEOGRAPHY, Difficulty.HARD));

        // Bundled questions honour the choice where they can...
        for (final TriviaQuestion question
                : new OfflineTriviaSource(random, geography).fetchQuestions(5)) {
            check("bundled questions follow the choice",
                    question.getCategory().equals(TriviaCategory.GEOGRAPHY.getDisplayName())
                            && question.getDifficulty() == Difficulty.EASY);
        }
        // ...and fall back to the whole pool when nothing bundled matches.
        final TriviaSettings unmatched = new TriviaSettings(
                EnumSet.of(TriviaCategory.ANIME_MANGA), EnumSet.noneOf(Difficulty.class));
        check("an unmatched choice still yields questions",
                new OfflineTriviaSource(random, unmatched).fetchQuestions(3).size() == 3);
    }

    /** Checks a live fetch, skipping if the API cannot be reached. */
    private void checkLiveApi() {
        try {
            final List<TriviaQuestion> batch = new OpenTriviaDatabase().fetchQuestions(3);
            check("a live batch is not empty", !batch.isEmpty());
            for (final TriviaQuestion question : batch) {
                check("a live question offers four answers", question.getAnswerCount() == 4);
                check("a live prompt is not blank", !question.getPrompt().isBlank());
                check("a live question has exactly one correct answer",
                        countCorrect(question) == 1);
                check("live text was decoded out of its URL encoding",
                        !question.getPrompt().contains("%20")
                                && !question.getPrompt().contains("%27"));
            }
        } catch (final TriviaException unreachable) {
            System.out.println("SKIPPED live API: " + unreachable.getMessage());
        }
    }

    /** Checks a live fetch narrowed to one topic and difficulty, skipping if offline. */
    private void checkLiveApiWithSettings() {
        try {
            final List<TriviaQuestion> batch = new OpenTriviaDatabase(new Random(SEED),
                    TriviaCategory.SCIENCE_NATURE, Difficulty.EASY).fetchQuestions(3);
            for (final TriviaQuestion question : batch) {
                check("a narrowed live question has the chosen difficulty",
                        question.getDifficulty() == Difficulty.EASY);
                check("a narrowed live question has the chosen topic",
                        question.getCategory().contains("Nature"));
            }
        } catch (final TriviaException unreachable) {
            System.out.println("SKIPPED narrowed live API: " + unreachable.getMessage());
        }
    }

    /**
     * Checks that the deck always yields a question, and buffers live ones.
     *
     * @throws InterruptedException if the wait for a refill is interrupted
     */
    private void checkDeck() throws InterruptedException {
        final QuestionDeck deck = QuestionDeck.createDefault(TriviaSettings.ANY);
        try {
            // Before any fetch has returned, the deck must still serve a question.
            check("the deck serves a question before any fetch returns",
                    deck.next() != null);

            deck.start();
            final long deadline = System.currentTimeMillis() + REFILL_TIMEOUT_MILLIS;
            while (deck.getBufferedCount() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(200L);
            }

            if (deck.getBufferedCount() > 0) {
                final int before = deck.getBufferedCount();
                check("the deck serves a buffered question", deck.next() != null);
                check("serving a question drains the buffer",
                        deck.getBufferedCount() < before);
            } else {
                System.out.println("SKIPPED live refill: no live questions arrived in time");
            }
        } finally {
            deck.shutdown();
        }
        check("the deck still serves a question after shutdown",
                QuestionDeck.createDefault(TriviaSettings.ANY).next() != null);
    }

    /**
     * Checks that a deck with several supplies mixes them question by question,
     * rather than serving one topic's batch and then the next. Uses stand-in
     * sources, so it needs no network.
     *
     * @throws InterruptedException if the wait for the supplies is interrupted
     */
    private void checkDeckMixesTopics() throws InterruptedException {
        final QuestionDeck deck = new QuestionDeck(
                List.of(stubSource("Topic A"), stubSource("Topic B")),
                new OfflineTriviaSource(new Random(SEED)));
        try {
            deck.start();
            final long deadline = System.currentTimeMillis() + REFILL_TIMEOUT_MILLIS;
            while (deck.getBufferedCount() < 20 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20L);
            }
            check("both supplies fill", deck.getBufferedCount() >= 20);

            // With two full supplies of ten, a deck that served one batch at a
            // time would give ten of one topic first. A mixing deck almost
            // certainly shows both within the first ten.
            final Set<String> firstTen = new HashSet<>();
            for (int i = 0; i < 10; i++) {
                firstTen.add(deck.next().getCategory());
            }
            check("the first ten questions mix both topics", firstTen.size() == 2);
        } finally {
            deck.shutdown();
        }
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
}
