package org.jsoup.nodes;

/**
 A comment node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Comment extends Node {
    private String data;

    protected Comment(Node parentNode) {
        super(parentNode, null);
    }

    public String getNodeName() {
        return "#comment";
    }

    public String getData() {
        return data;
    }
}
