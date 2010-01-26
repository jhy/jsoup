package org.jsoup.nodes;

/**
 An XML Declaration.

 @author Jonathan Hedley, jonathan@hedley.net */
public class XmlDeclaration extends Node {
    private static final String DECL_KEY = "declaration";
    private final boolean isProcessingInstruction; // <! if true, <? if false, declaration (and last data char should be ?)

    public XmlDeclaration(String data, String baseUri, boolean isProcessingInstruction) {
        super(baseUri);
        attributes.put(DECL_KEY, data);
        this.isProcessingInstruction = isProcessingInstruction;
    }

    public String nodeName() {
        return "#declaration";
    }

    public String getWholeDeclaration() {
        return attributes.get(DECL_KEY);
    }

    void outerHtml(StringBuilder accum) {
        accum.append(String.format("<%s%s>", isProcessingInstruction ? "!" : "?", getWholeDeclaration()));
    }

    public String toString() {
        return outerHtml();
    }
}
