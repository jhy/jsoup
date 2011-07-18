package org.jsoup.nodes;

/**
 A comment node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Comment extends Node {
    private static final String COMMENT_KEY = "comment";

    public Comment(String data) {
        super();
        attributes.put(COMMENT_KEY, data);
    }

    public String nodeName() {
        return "#comment";
    }

    public String getData() {
        return attributes.get(COMMENT_KEY);
    }
}
