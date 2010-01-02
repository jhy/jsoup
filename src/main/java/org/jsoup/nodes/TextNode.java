package org.jsoup.nodes;

import org.apache.commons.lang.StringEscapeUtils;

/**
 A text node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextNode extends Node {
    private static final String TEXT_KEY = "text";

    /**
     * Create a new TextNode representing the supplied (unencoded) text).
     * @param text raw text
     * @see #createFromEncoded(String)
     */
    public TextNode(String text) {
        super();
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
    public static TextNode createFromEncoded(String encodedText) {
        String text = StringEscapeUtils.unescapeHtml(encodedText);
        return new TextNode(text);
    }
}
