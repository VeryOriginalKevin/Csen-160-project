package backgammon.trivia;

/**
 * The topics the Open Trivia Database offers, with the ids its API uses.
 *
 * <p>Hard-coded rather than downloaded from the API's category list, so the
 * title screen can show topics instantly and with no network. The ids are
 * stable; they have not changed since the API launched.</p>
 */
public enum TriviaCategory {

    /** General knowledge. */
    GENERAL_KNOWLEDGE(9, "General Knowledge"),

    /** Books and literature. */
    BOOKS(10, "Books"),

    /** Film. */
    FILM(11, "Film"),

    /** Music. */
    MUSIC(12, "Music"),

    /** Musicals and theatre. */
    MUSICALS_THEATRE(13, "Musicals & Theatre"),

    /** Television. */
    TELEVISION(14, "Television"),

    /** Video games. */
    VIDEO_GAMES(15, "Video Games"),

    /** Board games. */
    BOARD_GAMES(16, "Board Games"),

    /** Science and nature. */
    SCIENCE_NATURE(17, "Science & Nature"),

    /** Computers. */
    COMPUTERS(18, "Computers"),

    /** Mathematics. */
    MATHEMATICS(19, "Mathematics"),

    /** Mythology. */
    MYTHOLOGY(20, "Mythology"),

    /** Sports. */
    SPORTS(21, "Sports"),

    /** Geography. */
    GEOGRAPHY(22, "Geography"),

    /** History. */
    HISTORY(23, "History"),

    /** Politics. */
    POLITICS(24, "Politics"),

    /** Art. */
    ART(25, "Art"),

    /** Celebrities. */
    CELEBRITIES(26, "Celebrities"),

    /** Animals. */
    ANIMALS(27, "Animals"),

    /** Vehicles. */
    VEHICLES(28, "Vehicles"),

    /** Comics. */
    COMICS(29, "Comics"),

    /** Gadgets. */
    GADGETS(30, "Gadgets"),

    /** Japanese anime and manga. */
    ANIME_MANGA(31, "Anime & Manga"),

    /** Cartoons and animation. */
    CARTOONS(32, "Cartoons");

    /** The number the API uses for this topic in its {@code category} parameter. */
    private final int apiId;

    /** A short name for the title screen. */
    private final String displayName;

    /**
     * Creates a topic.
     *
     * @param apiId       the API's id for it
     * @param displayName the name shown to players
     */
    TriviaCategory(final int apiId, final String displayName) {
        this.apiId = apiId;
        this.displayName = displayName;
    }

    /**
     * Returns the id the API uses for this topic.
     *
     * @return the value for the API's {@code category} parameter
     */
    public int getApiId() {
        return apiId;
    }

    /**
     * Returns the name shown to players.
     *
     * @return a short name such as {@code "Science & Nature"}
     */
    public String getDisplayName() {
        return displayName;
    }
}
