package org.jsoup.nodes;

/**
 A text node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextNode extends Node {
    private String text;

    public TextNode(Node parentNode, String text) {
        super(parentNode, null);
        this.text = text;
    }

    public String getNodeName() {
        return "#text";
    }

    public String getWholeText() {
        return text;
    }
}
