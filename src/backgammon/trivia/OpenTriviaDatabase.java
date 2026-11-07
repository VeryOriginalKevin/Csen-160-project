package backgammon.trivia;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fetches questions from the Open Trivia Database at
 * <a href="https://opentdb.com">opentdb.com</a>.
 *
 * <p>Only multiple-choice questions are requested, so every question offers the
 * same four answers and each player's panel always needs the same four buttons.
 * Answers are requested URL-encoded ({@code encode=url3986}) rather than in the
 * API's default HTML-escaped form, which means text can be recovered with
 * {@link URLDecoder} instead of needing an HTML entity decoder.</p>
 *
 * <p>The API allows one request per IP address every five seconds, so this class
 * holds requests back to that pace itself. Since a held-back request sleeps,
 * {@link #fetchQuestions(int)} must be called from a background thread &mdash;
 * which is what {@link QuestionDeck} does.</p>
 *
 * <p>An instance can be narrowed to one topic and one difficulty, because the
 * API accepts only one of each per request. To mix several topics,
 * {@link QuestionDeck} uses one instance per topic and difficulty chosen.</p>
 *
 * <p>Thread safe: the pacing lock serialises requests.</p>
 */
public final class OpenTriviaDatabase implements TriviaSource {

    /** Most questions the API will return in one response. */
    public static final int MAX_BATCH_SIZE = 50;

    /** Shortest gap the API permits between two requests from one address. */
    private static final Duration MIN_REQUEST_INTERVAL = Duration.ofSeconds(5);

    /** How long to wait for the connection, and then for the response. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** The API endpoint, before query parameters. */
    private static final String ENDPOINT = "https://opentdb.com/api.php";

    /** Response code the API returns when a request succeeded. */
    private static final int CODE_SUCCESS = 0;

    /** Response code the API returns when it has fewer questions than asked for. */
    private static final int CODE_NO_RESULTS = 1;

    /**
     * Batch sizes to try, largest first, when a narrow topic and difficulty
     * do not have enough questions for a full batch.
     */
    private static final int[] FALLBACK_AMOUNTS = {5, 1};

    /** HTTP status the API returns when requests arrive too quickly. */
    private static final int STATUS_TOO_MANY_REQUESTS = 429;

    /**
     * Guards {@link #earliestNextRequestMillis} and serialises requests.
     *
     * <p>Static, because the API counts requests per IP address rather than per
     * client object. Were the pacing per instance, two instances would each
     * believe they were free to send and the second would be refused.</p>
     */
    private static final Object PACING_LOCK = new Object();

    /**
     * Wall-clock time, in milliseconds, before which no request may be sent by
     * any instance. Zero until the first request has been made.
     *
     * <p>Only read and written while holding {@link #PACING_LOCK}.</p>
     */
    private static long earliestNextRequestMillis;

    /** The HTTP client, reused across requests so connections are pooled. */
    private final HttpClient client;

    /** Randomness used to shuffle each question's answers. */
    private final Random shuffleSource;

    /** The topic to ask for, or {@code null} for any topic. */
    private final TriviaCategory category;

    /** The difficulty to ask for, or {@code null} for any difficulty. */
    private final Difficulty difficulty;

    /** Creates a client for any topic and difficulty, with unseeded randomness. */
    public OpenTriviaDatabase() {
        this(new Random(), null, null);
    }

    /**
     * Creates a client narrowed to one topic and difficulty.
     *
     * @param shuffleSource randomness for shuffling each question's answers;
     *                      pass a seeded {@code Random} to make a test
     *                      deterministic
     * @param category      the topic to ask for, or {@code null} for any
     * @param difficulty    the difficulty to ask for, or {@code null} for any
     */
    public OpenTriviaDatabase(final Random shuffleSource, final TriviaCategory category,
                              final Difficulty difficulty) {
        this.shuffleSource = shuffleSource;
        this.category = category;
        this.difficulty = difficulty;
        this.client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                // The API redirects http to https; following it keeps the
                // request working if the endpoint ever moves.
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public List<TriviaQuestion> fetchQuestions(final int amount) throws TriviaException {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
        final int capped = Math.min(amount, MAX_BATCH_SIZE);
        try {
            return readQuestions(send(buildRequestUri(capped)));
        } catch (final NotEnoughQuestionsException tooFew) {
            // A narrow choice (say, hard Comics questions) may have fewer
            // questions than a full batch. Ask for fewer before giving up.
            for (final int smaller : FALLBACK_AMOUNTS) {
                if (smaller >= capped) {
                    continue;
                }
                try {
                    return readQuestions(send(buildRequestUri(smaller)));
                } catch (final NotEnoughQuestionsException stillTooFew) {
                    // Try the next, smaller size.
                }
            }
            throw tooFew;
        }
    }

    /**
     * Builds the request URI for one batch of this client's topic and difficulty.
     *
     * @param amount how many questions to ask for, already capped
     * @return the URI to request
     */
    private URI buildRequestUri(final int amount) {
        final StringBuilder uri = new StringBuilder(ENDPOINT)
                .append("?amount=").append(amount)
                .append("&type=multiple")
                .append("&encode=url3986");
        if (category != null) {
            uri.append("&category=").append(category.getApiId());
        }
        if (difficulty != null) {
            uri.append("&difficulty=").append(difficulty.getApiValue());
        }
        return URI.create(uri.toString());
    }

    /**
     * Sends a request, waiting first if the API's pacing requires it.
     *
     * @param uri the URI to request
     * @return the response body
     * @throws TriviaException if the request fails or returns a non-200 status
     */
    private String send(final URI uri) throws TriviaException {
        final HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        synchronized (PACING_LOCK) {
            awaitRequestWindow();
            try {
                final HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == STATUS_TOO_MANY_REQUESTS) {
                    throw new TriviaException("Trivia API refused the request as too "
                            + "frequent; it allows one every five seconds");
                }
                if (response.statusCode() != 200) {
                    throw new TriviaException(
                            "Trivia API returned HTTP " + response.statusCode());
                }
                return response.body();
            } catch (final IOException cause) {
                throw new TriviaException("Could not reach the trivia API", cause);
            } catch (final InterruptedException cause) {
                // Restore the flag so the thread's owner still sees the
                // interruption after this call unwinds.
                Thread.currentThread().interrupt();
                throw new TriviaException("Interrupted while fetching questions", cause);
            } finally {
                earliestNextRequestMillis =
                        System.currentTimeMillis() + MIN_REQUEST_INTERVAL.toMillis();
            }
        }
    }

    /**
     * Sleeps until enough time has passed since the previous request.
     *
     * @throws TriviaException if the wait is interrupted
     */
    private void awaitRequestWindow() throws TriviaException {
        final long waitMillis = earliestNextRequestMillis - System.currentTimeMillis();
        if (waitMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(waitMillis);
        } catch (final InterruptedException cause) {
            Thread.currentThread().interrupt();
            throw new TriviaException("Interrupted while waiting to fetch questions", cause);
        }
    }

    /**
     * Turns a response body into questions.
     *
     * @param body the response body
     * @return the questions it describes
     * @throws TriviaException if the body is not valid JSON, reports a failure
     *                         code, or carries no questions
     */
    private List<TriviaQuestion> readQuestions(final String body) throws TriviaException {
        final JsonValue root;
        try {
            root = JsonValue.parse(body);
        } catch (final IllegalArgumentException cause) {
            throw new TriviaException("Trivia API returned malformed JSON", cause);
        }

        try {
            final int responseCode = root.field("response_code").asInt();
            if (responseCode == CODE_NO_RESULTS) {
                throw new NotEnoughQuestionsException();
            }
            if (responseCode != CODE_SUCCESS) {
                throw new TriviaException("Trivia API error: " + describeCode(responseCode));
            }
            final List<JsonValue> results = root.field("results").elements();
            final List<TriviaQuestion> questions = new ArrayList<>(results.size());
            for (final JsonValue result : results) {
                questions.add(readQuestion(result));
            }
            if (questions.isEmpty()) {
                throw new TriviaException("Trivia API returned no questions");
            }
            return questions;
        } catch (final IllegalStateException | IllegalArgumentException cause) {
            // A strict accessor rejected the document: the API's shape changed.
            throw new TriviaException("Trivia API response was not shaped as expected", cause);
        }
    }

    /**
     * Turns one entry of the {@code results} array into a question.
     *
     * @param result the entry to read
     * @return the question it describes
     */
    private TriviaQuestion readQuestion(final JsonValue result) {
        final List<String> incorrect = new ArrayList<>();
        for (final JsonValue answer : result.field("incorrect_answers").elements()) {
            incorrect.add(decode(answer.asString()));
        }
        return TriviaQuestion.of(
                decode(result.field("category").asString()),
                Difficulty.fromApiValue(decode(result.field("difficulty").asString())),
                decode(result.field("question").asString()),
                decode(result.field("correct_answer").asString()),
                incorrect,
                shuffleSource);
    }

    /**
     * Reverses the API's {@code url3986} encoding.
     *
     * @param encoded the encoded text
     * @return the text as it should be displayed
     */
    private static String decode(final String encoded) {
        return URLDecoder.decode(encoded, StandardCharsets.UTF_8);
    }

    /**
     * Explains one of the API's response codes.
     *
     * @param code the code the API returned
     * @return a description suitable for an error message
     */
    private static String describeCode(final int code) {
        switch (code) {
            case 1:
                return "not enough questions available for that request";
            case 2:
                return "a request parameter was rejected";
            case 3:
                return "session token not found";
            case 4:
                return "session token has returned every available question";
            case 5:
                return "too many requests; the API allows one every five seconds";
            default:
                return "unrecognised response code " + code;
        }
    }

    /**
     * Thrown when the API has fewer questions matching a request than were
     * asked for, so the caller can retry with a smaller batch.
     */
    private static final class NotEnoughQuestionsException extends TriviaException {

        /** Serialisation version; exceptions are serialisable. */
        private static final long serialVersionUID = 1L;

        /** Creates the exception with a message for the status line. */
        NotEnoughQuestionsException() {
            super("Trivia API error: " + describeCode(CODE_NO_RESULTS));
        }
    }
}
