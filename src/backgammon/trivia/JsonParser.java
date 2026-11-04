package backgammon.trivia;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A recursive-descent reader that turns JSON text into a {@link JsonValue} tree.
 *
 * <p>One parser instance reads one document; it holds the cursor position as it
 * walks the text. Not thread safe, and not meant to be reused &mdash;
 * {@link JsonValue#parse(String)} creates one per call.</p>
 */
final class JsonParser {

    /** The document being read. */
    private final String text;

    /** Index of the next character to read. */
    private int cursor;

    /**
     * Creates a parser positioned at the start of a document.
     *
     * @param text the JSON text to read
     */
    JsonParser(final String text) {
        this.text = text;
        this.cursor = 0;
    }

    /**
     * Reads the one value this document contains, and checks that nothing but
     * whitespace follows it.
     *
     * @return the root node
     * @throws IllegalArgumentException if the document is not well-formed
     */
    JsonValue parseDocument() {
        final JsonValue root = parseValue();
        skipWhitespace();
        if (cursor < text.length()) {
            throw error("Unexpected trailing character " + quoted(text.charAt(cursor)));
        }
        return root;
    }

    /**
     * Reads one value of any kind, dispatching on its first character.
     *
     * @return the value read
     * @throws IllegalArgumentException if no value starts here
     */
    private JsonValue parseValue() {
        skipWhitespace();
        if (cursor >= text.length()) {
            throw error("Unexpected end of input");
        }
        switch (text.charAt(cursor)) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return JsonValue.string(parseString());
            case 't':
                expectWord("true");
                return JsonValue.bool(true);
            case 'f':
                expectWord("false");
                return JsonValue.bool(false);
            case 'n':
                expectWord("null");
                return JsonValue.NULL;
            default:
                // A number is the only remaining possibility.
                return JsonValue.number(parseNumber());
        }
    }

    /**
     * Reads an object, assuming the cursor sits on its opening brace.
     *
     * @return an object node
     * @throws IllegalArgumentException if the object is malformed
     */
    private JsonValue parseObject() {
        expect('{');
        // LinkedHashMap so toString() reflects document order, which helps when
        // debugging an unexpected API response.
        final Map<String, JsonValue> fields = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            cursor++;
            return JsonValue.object(fields);
        }
        while (true) {
            skipWhitespace();
            final String name = parseString();
            skipWhitespace();
            expect(':');
            fields.put(name, parseValue());
            skipWhitespace();
            final char separator = next("a comma or closing brace");
            if (separator == '}') {
                return JsonValue.object(fields);
            }
            if (separator != ',') {
                throw error("Expected a comma or closing brace, found " + quoted(separator));
            }
        }
    }

    /**
     * Reads an array, assuming the cursor sits on its opening bracket.
     *
     * @return an array node
     * @throws IllegalArgumentException if the array is malformed
     */
    private JsonValue parseArray() {
        expect('[');
        final List<JsonValue> elements = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            cursor++;
            return JsonValue.array(elements);
        }
        while (true) {
            elements.add(parseValue());
            skipWhitespace();
            final char separator = next("a comma or closing bracket");
            if (separator == ']') {
                return JsonValue.array(elements);
            }
            if (separator != ',') {
                throw error("Expected a comma or closing bracket, found " + quoted(separator));
            }
        }
    }

    /**
     * Reads a quoted string and resolves its escape sequences.
     *
     * @return the unescaped contents
     * @throws IllegalArgumentException if the string is unterminated or holds an
     *                                  unrecognised escape
     */
    private String parseString() {
        expect('"');
        final StringBuilder unescaped = new StringBuilder();
        while (true) {
            if (cursor >= text.length()) {
                throw error("Unterminated string");
            }
            final char current = text.charAt(cursor++);
            if (current == '"') {
                return unescaped.toString();
            }
            if (current == '\\') {
                unescaped.append(parseEscape());
            } else {
                unescaped.append(current);
            }
        }
    }

    /**
     * Reads the character following a backslash and returns what it denotes.
     *
     * @return the character the escape sequence stands for
     * @throws IllegalArgumentException if the escape is not recognised
     */
    private char parseEscape() {
        final char code = next("an escape character");
        switch (code) {
            case '"':
                return '"';
            case '\\':
                return '\\';
            case '/':
                return '/';
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            case 'u':
                return parseUnicodeEscape();
            default:
                throw error("Unknown escape " + quoted(code));
        }
    }

    /**
     * Reads the four hex digits of a unicode escape.
     *
     * @return the character those digits denote
     * @throws IllegalArgumentException if four hex digits do not follow
     */
    private char parseUnicodeEscape() {
        if (cursor + 4 > text.length()) {
            throw error("Truncated unicode escape");
        }
        final String digits = text.substring(cursor, cursor + 4);
        cursor += 4;
        try {
            return (char) Integer.parseInt(digits, 16);
        } catch (final NumberFormatException cause) {
            throw error("Bad unicode escape " + digits);
        }
    }

    /**
     * Reads a number, consuming every character JSON allows inside one.
     *
     * @return the numeric value
     * @throws IllegalArgumentException if the characters do not form a number
     */
    private double parseNumber() {
        final int start = cursor;
        while (cursor < text.length() && isNumberCharacter(text.charAt(cursor))) {
            cursor++;
        }
        final String literal = text.substring(start, cursor);
        try {
            return Double.parseDouble(literal);
        } catch (final NumberFormatException cause) {
            throw error("Bad number " + literal);
        }
    }

    /**
     * Returns whether a character can appear within a JSON number literal.
     *
     * @param candidate the character to test
     * @return {@code true} if it can appear in a number
     */
    private static boolean isNumberCharacter(final char candidate) {
        return (candidate >= '0' && candidate <= '9')
                || candidate == '-' || candidate == '+'
                || candidate == '.' || candidate == 'e' || candidate == 'E';
    }

    /**
     * Consumes a bare keyword such as {@code true}.
     *
     * @param word the keyword expected at the cursor
     * @throws IllegalArgumentException if the text there is something else
     */
    private void expectWord(final String word) {
        if (!text.startsWith(word, cursor)) {
            throw error("Expected " + word);
        }
        cursor += word.length();
    }

    /**
     * Consumes one required character.
     *
     * @param required the character expected at the cursor
     * @throws IllegalArgumentException if a different character is there
     */
    private void expect(final char required) {
        final char found = next(quoted(required));
        if (found != required) {
            throw error("Expected " + quoted(required) + ", found " + quoted(found));
        }
    }

    /**
     * Returns the character at the cursor without consuming it.
     *
     * @return the character at the cursor, or a NUL character at end of input
     */
    private char peek() {
        return cursor < text.length() ? text.charAt(cursor) : '\0';
    }

    /**
     * Consumes and returns one character.
     *
     * @param expected description of what was wanted, for the error message
     * @return the character consumed
     * @throws IllegalArgumentException if the input has ended
     */
    private char next(final String expected) {
        if (cursor >= text.length()) {
            throw error("Expected " + expected + " but input ended");
        }
        return text.charAt(cursor++);
    }

    /** Advances the cursor past any whitespace. */
    private void skipWhitespace() {
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
    }

    /**
     * Renders a character for an error message, in quotes.
     *
     * @param character the character to render
     * @return the character wrapped in single quotes
     */
    private static String quoted(final char character) {
        return "'" + character + "'";
    }

    /**
     * Builds an exception naming the position at which parsing failed.
     *
     * @param message what went wrong
     * @return the exception to throw
     */
    private IllegalArgumentException error(final String message) {
        return new IllegalArgumentException(message + " at offset " + cursor);
    }
}
