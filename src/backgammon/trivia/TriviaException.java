package backgammon.trivia;

/**
 * Thrown when questions cannot be fetched.
 *
 * <p>Checked, because a network call failing is an ordinary thing that callers
 * must handle rather than a programming mistake. {@link QuestionDeck} handles it
 * by falling back to its offline questions, so a failure here never stops a
 * game in progress.</p>
 */
public class TriviaException extends Exception {

    /** Identifies this version of the class for serialisation. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with a message.
     *
     * @param message what went wrong
     */
    public TriviaException(final String message) {
        super(message);
    }

    /**
     * Creates an exception wrapping an underlying failure.
     *
     * @param message what went wrong
     * @param cause   the failure that caused it
     */
    public TriviaException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
