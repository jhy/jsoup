package org.jsoup.nodes;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.StringUtils;

/**
 A text node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextNode extends Node {
    private static final String TEXT_KEY = "text";

    /**
     Create a new TextNode representing the supplied (unencoded) text).

     @param text raw text
     @param baseUri base uri
     @see #createFromEncoded(String, String)
     */
    public TextNode(String text, String baseUri) {
        super(baseUri);
        attributes.put(TEXT_KEY, text);
    }

    public String nodeName() {
        return "#text";
    }
    
    /**
     * Get the text content of this text node.
     * @return Unencoded, normalised text.
     * @see TextNode#getWholeText()
     */
    public String text() {
        return outerHtml();
    }
    
    /**
     * Set the text content of this text node.
     * @param text unencoded text
     * @return this, for chaining
     */
    public TextNode text(String text) {
        attributes.put(TEXT_KEY, text);
        return this;
    }

    /**
     Get the (unencoded) text of this text node, including any newlines and spaces present in the original.
     @return text
     */
    public String getWholeText() {
        return attributes.get(TEXT_KEY);
    }

    /**
     Test if this text node is blank -- that is, empty or only whitespace (including newlines).
     @return true if this document is empty or only whitespace, false if it contains any text content.
     */
    public boolean isBlank() {
        return StringUtils.isBlank(normaliseWhitespace(getWholeText()));
    }

    void outerHtml(StringBuilder accum) {
        String html = StringEscapeUtils.escapeHtml(getWholeText());
        if (parent() instanceof Element && !((Element) parent()).preserveWhitespace()) {
            html = normaliseWhitespace(html);
        }

        if (!isBlank() && parentNode instanceof Element && ((Element) parentNode).tag().canContainBlock()  && siblingIndex() == 0)
            indent(accum);
        accum.append(html);
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
        return text.replaceFirst("^\\s+", "");
    }

    static boolean lastCharIsWhitespace(StringBuilder sb) {
        if (sb.length() == 0)
            return false;
        String lastChar = sb.substring(sb.length()-1, sb.length());
        Validate.isTrue(lastChar.length() == 1); // todo: remove check
        return lastChar.equals(" ");
    }
}
