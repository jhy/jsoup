package org.jsoup.nodes;

/**
 A text node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextNode extends Node {
    private static final String TEXT_KEY = "text";
    public TextNode(Node parentNode, String text) {
        super(parentNode);
        attributes.put(TEXT_KEY, text);
    }

    public String nodeName() {
        return "#text";
    }

    public String getWholeText() {
        return attributes.get(TEXT_KEY);
    }
}
