package backgammon.ui;

/**
 * Builds the small HTML snippets Swing labels use for wrapped text.
 *
 * <p>Swing only wraps a label's text if it is HTML with a fixed width. Question
 * text comes from the internet, so it must be escaped first: a stray
 * {@code <} in a question would otherwise be read as markup.</p>
 *
 * <p>A utility holder; not instantiable.</p>
 */
final class Html {

    /** Not instantiable. */
    private Html() {
        throw new AssertionError("Html is a utility holder and must not be instantiated.");
    }

    /**
     * Wraps plain text in HTML that Swing will word-wrap at a given width.
     *
     * @param text  plain text, escaped here
     * @param width the wrapping width in pixels
     * @return HTML for a label
     */
    static String wrapped(final String text, final int width) {
        // A plain width attribute is honoured exactly. A CSS "px" width is not:
        // Swing scales CSS pixels by screen resolution, so text would wrap
        // wider than asked and run off the panel.
        return "<html><div width='" + width + "'>" + escape(text) + "</div></html>";
    }

    /**
     * Escapes the characters HTML treats specially.
     *
     * @param text plain text
     * @return the text, safe to place inside HTML
     */
    static String escape(final String text) {
        final StringBuilder escaped = new StringBuilder(text.length());
        for (final char c : text.toCharArray()) {
            switch (c) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                default:
                    escaped.append(c);
                    break;
            }
        }
        return escaped.toString();
    }
}
