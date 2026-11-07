package backgammon.trivia;

/**
 * How hard a question is, as reported by the Open Trivia Database.
 *
 * <p>Shown next to each question, and choosable on the title screen to narrow
 * which questions are requested.</p>
 */
public enum Difficulty {

    /** The API's {@code easy} tier. */
    EASY("easy"),

    /** The API's {@code medium} tier. */
    MEDIUM("medium"),

    /** The API's {@code hard} tier. */
    HARD("hard");

    /** The lowercase spelling the API uses for this tier. */
    private final String apiValue;

    /**
     * Creates a tier.
     *
     * @param apiValue the spelling the API uses
     */
    Difficulty(final String apiValue) {
        this.apiValue = apiValue;
    }

    /**
     * Returns the spelling the API uses for this tier.
     *
     * @return the API's spelling, in lowercase
     */
    public String getApiValue() {
        return apiValue;
    }

    /**
     * Returns a label suitable for showing to players.
     *
     * @return the tier name with an initial capital, such as {@code "Medium"}
     */
    public String getDisplayLabel() {
        final String lower = apiValue;
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /**
     * Finds the tier matching one of the API's spellings.
     *
     * @param apiValue the spelling from the API response, in any letter case
     * @return the matching tier, or {@link #MEDIUM} if the spelling is
     *         unrecognised, so an added tier cannot break a game in progress
     */
    public static Difficulty fromApiValue(final String apiValue) {
        for (final Difficulty candidate : values()) {
            if (candidate.apiValue.equalsIgnoreCase(apiValue)) {
                return candidate;
            }
        }
        return MEDIUM;
    }
}
