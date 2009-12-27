package org.jsoup.nodes;

/**
 An XML Declaration.

 @author Jonathan Hedley, jonathan@hedley.net */
public class XmlDeclaration extends Node {
    private static final String DECL_KEY = "declaration";

    public XmlDeclaration(String data) {
        super();
        attributes.put(DECL_KEY, data);
    }

    public String nodeName() {
        return "#declaration";
    }

    public String getWholeDeclaration() {
        return attributes.get(DECL_KEY);
    }
}
