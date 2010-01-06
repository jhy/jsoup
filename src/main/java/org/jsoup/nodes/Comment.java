package org.jsoup.nodes;

/**
 A comment node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Comment extends Node {
    private static final String COMMENT_KEY = "comment";

    public Comment(String data, String baseUri) {
        super(baseUri);
        attributes.put(COMMENT_KEY, data);
    }

    public String nodeName() {
        return "#comment";
    }

    public String getData() {
        return attributes.get(COMMENT_KEY);
    }

    public String outerHtml() {
        return String.format("<!--%s-->", getData());
    }

    public String toString() {
        return outerHtml();
    }
}
