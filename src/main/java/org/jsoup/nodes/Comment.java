package org.jsoup.nodes;

/**
 A comment node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Comment extends Node {
    private static final String COMMENT_KEY = "comment";

    /**
     Create a new comment node.
     @param data The contents of the comment
     @param baseUri base URI
     */
    public Comment(String data, String baseUri) {
        super(baseUri);
        attributes.put(COMMENT_KEY, data);
    }

    public String nodeName() {
        return "#comment";
    }

    /**
     Get the contents of the comment.
     @return comment content
     */
    public String getData() {
        return attributes.get(COMMENT_KEY);
    }

    void outerHtml(StringBuilder accum) {
        indent(accum);
        accum.append(String.format("<!--%s-->", getData()));
    }

    public String toString() {
        return outerHtml();
    }
}
