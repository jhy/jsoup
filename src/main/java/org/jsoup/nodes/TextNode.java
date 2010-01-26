package org.jsoup.nodes;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;

/**
 A text node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextNode extends Node {
    private static final String TEXT_KEY = "text";

    /**
     * Create a new TextNode representing the supplied (unencoded) text).
     * @param text raw text
     * @see #createFromEncoded(String, String)
     */
    public TextNode(String text, String baseUri) {
        super(baseUri);
        attributes.put(TEXT_KEY, text);
    }

    public String nodeName() {
        return "#text";
    }

    public String getWholeText() {
        return attributes.get(TEXT_KEY);
    }

    public String outerHtml() {
        return StringEscapeUtils.escapeHtml(getWholeText());
    }

    public String toString() {
        return outerHtml();
    }

    /**
     * Create a new TextNode from HTML encoded (aka escaped) data.
     * @param encodedText Text containing encoded HTML (e.g. &amp;lt;)
     * @return TextNode containing unencoded data (e.g. &lt;)
     */
    public static TextNode createFromEncoded(String encodedText, String baseUri) {
        String text = StringEscapeUtils.unescapeHtml(encodedText);
        return new TextNode(text, baseUri);
    }

    static String normaliseWhitespace(String text) {
        text = text.replaceAll("\\s{2,}|(\\r\\n|\\r|\\n)", " "); // more than one space, and newlines to " "
        return text;
    }

    static String stripLeadingWhitespace(String text) {
        return text.replaceFirst("\\s+", "");
    }

    static boolean lastCharIsWhitespace(StringBuilder sb) {
        if (sb.length() == 0)
            return false;
        String lastChar = sb.substring(sb.length()-1, sb.length());
        Validate.isTrue(lastChar.length() == 1); // todo: remove check
        return lastChar.equals(" ");
    }
}
