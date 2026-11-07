package backgammon.trivia;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An immutable node in a parsed JSON document.
 *
 * <p>Written by hand so the project ships with no external dependencies. It
 * supports exactly what the Open Trivia Database returns: objects, arrays,
 * strings, numbers, booleans, and null.</p>
 *
 * <p>Accessors are deliberately strict. {@link #field(String)} and
 * {@link #elements()} throw rather than return null when the document is not
 * shaped as expected, so a change in the API surfaces as one clear error at the
 * point of the mistake instead of a {@code NullPointerException} later.</p>
 */
public final class JsonValue {

    /** The six kinds of value JSON can hold. */
    private enum Kind {
        /** A {@code {"key": value}} mapping. */
        OBJECT,
        /** A {@code [value, value]} sequence. */
        ARRAY,
        /** A quoted string. */
        STRING,
        /** Any JSON number; stored as a {@code double}. */
        NUMBER,
        /** {@code true} or {@code false}. */
        BOOLEAN,
        /** {@code null}. */
        NULL
    }

    /** The shared instance representing JSON {@code null}. */
    static final JsonValue NULL = new JsonValue(Kind.NULL, null);

    /** Which kind of value this node holds. */
    private final Kind kind;

    /**
     * The value itself: a {@code Map}, {@code List}, {@code String},
     * {@code Double}, {@code Boolean}, or {@code null} to match {@link #kind}.
     */
    private final Object payload;

    /**
     * Creates a node. Callers use the package-private factories instead.
     *
     * @param kind    which kind of value this node holds
     * @param payload the value, matching {@code kind}
     */
    private JsonValue(final Kind kind, final Object payload) {
        this.kind = kind;
        this.payload = payload;
    }

    /**
     * Parses a complete JSON document.
     *
     * @param text the document text
     * @return the root node
     * @throws IllegalArgumentException if {@code text} is not well-formed JSON
     */
    public static JsonValue parse(final String text) {
        return new JsonParser(text).parseDocument();
    }

    /**
     * Wraps a map as an object node. The map is not copied; the parser hands
     * over a map it never touches again.
     *
     * @param fields the object's fields
     * @return an object node
     */
    static JsonValue object(final Map<String, JsonValue> fields) {
        return new JsonValue(Kind.OBJECT, Collections.unmodifiableMap(fields));
    }

    /**
     * Wraps a list as an array node.
     *
     * @param elements the array's elements
     * @return an array node
     */
    static JsonValue array(final List<JsonValue> elements) {
        return new JsonValue(Kind.ARRAY, Collections.unmodifiableList(elements));
    }

    /**
     * Wraps a string.
     *
     * @param text the string's contents, already unescaped
     * @return a string node
     */
    static JsonValue string(final String text) {
        return new JsonValue(Kind.STRING, text);
    }

    /**
     * Wraps a number.
     *
     * @param number the numeric value
     * @return a number node
     */
    static JsonValue number(final double number) {
        return new JsonValue(Kind.NUMBER, number);
    }

    /**
     * Wraps a boolean.
     *
     * @param flag the boolean value
     * @return a boolean node
     */
    static JsonValue bool(final boolean flag) {
        return new JsonValue(Kind.BOOLEAN, flag);
    }

    /**
     * Returns the named field of this object.
     *
     * @param name the field name
     * @return the field's value
     * @throws IllegalStateException    if this node is not an object
     * @throws IllegalArgumentException if the field is absent
     */
    public JsonValue field(final String name) {
        require(Kind.OBJECT);
        @SuppressWarnings("unchecked")
        final Map<String, JsonValue> fields = (Map<String, JsonValue>) payload;
        final JsonValue value = fields.get(name);
        if (value == null) {
            throw new IllegalArgumentException("No such JSON field: " + name);
        }
        return value;
    }

    /**
     * Returns this array's elements, in order.
     *
     * @return an unmodifiable view of the elements
     * @throws IllegalStateException if this node is not an array
     */
    public List<JsonValue> elements() {
        require(Kind.ARRAY);
        @SuppressWarnings("unchecked")
        final List<JsonValue> elements = (List<JsonValue>) payload;
        return elements;
    }

    /**
     * Returns this string's contents.
     *
     * @return the string value
     * @throws IllegalStateException if this node is not a string
     */
    public String asString() {
        require(Kind.STRING);
        return (String) payload;
    }

    /**
     * Returns this number as an {@code int}, truncating any fraction.
     *
     * @return the numeric value as an {@code int}
     * @throws IllegalStateException if this node is not a number
     */
    public int asInt() {
        require(Kind.NUMBER);
        return (int) (double) (Double) payload;
    }

    /**
     * Throws unless this node is of the expected kind.
     *
     * @param expected the kind this node must be
     * @throws IllegalStateException if this node is of a different kind
     */
    private void require(final Kind expected) {
        if (kind != expected) {
            throw new IllegalStateException("Expected JSON " + expected + " but found " + kind);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return kind + (payload == null ? "" : "(" + payload + ")");
    }
}
