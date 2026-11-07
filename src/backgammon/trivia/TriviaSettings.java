package backgammon.trivia;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Which topics and difficulties the players chose on the title screen.
 *
 * <p>An empty choice means "any": no topics chosen means questions may come
 * from every topic, and likewise for difficulty. That is how the title screen's
 * "Random" option is represented, and {@link #ANY} is the setting with both
 * left open. Immutable.</p>
 */
public final class TriviaSettings {

    /** Any topic, any difficulty. */
    public static final TriviaSettings ANY = new TriviaSettings(
            EnumSet.noneOf(TriviaCategory.class), EnumSet.noneOf(Difficulty.class));

    /** The topics chosen; empty for any. */
    private final Set<TriviaCategory> categories;

    /** The difficulties chosen; empty for any. */
    private final Set<Difficulty> difficulties;

    /**
     * Records a choice.
     *
     * @param categories   the topics allowed, or an empty set for any topic
     * @param difficulties the difficulties allowed, or an empty set for any
     */
    public TriviaSettings(final Set<TriviaCategory> categories, final Set<Difficulty> difficulties) {
        // EnumSet copies keep a stable order and cannot be changed by the caller afterwards.
        this.categories = Collections.unmodifiableSet(categories.isEmpty()
                ? EnumSet.noneOf(TriviaCategory.class) : EnumSet.copyOf(categories));
        this.difficulties = Collections.unmodifiableSet(difficulties.isEmpty()
                ? EnumSet.noneOf(Difficulty.class) : EnumSet.copyOf(difficulties));
    }

    /**
     * Returns the topics chosen.
     *
     * @return an unmodifiable set; empty means any topic
     */
    public Set<TriviaCategory> getCategories() {
        return categories;
    }

    /**
     * Returns the difficulties chosen.
     *
     * @return an unmodifiable set; empty means any difficulty
     */
    public Set<Difficulty> getDifficulties() {
        return difficulties;
    }

    /**
     * Returns whether a question of a given topic and difficulty fits this choice.
     *
     * @param category   the question's topic
     * @param difficulty the question's difficulty
     * @return {@code true} if both are allowed
     */
    public boolean allows(final TriviaCategory category, final Difficulty difficulty) {
        return (categories.isEmpty() || categories.contains(category))
                && (difficulties.isEmpty() || difficulties.contains(difficulty));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "TriviaSettings(categories=" + (categories.isEmpty() ? "any" : categories)
                + ", difficulties=" + (difficulties.isEmpty() ? "any" : difficulties) + ")";
    }
}
