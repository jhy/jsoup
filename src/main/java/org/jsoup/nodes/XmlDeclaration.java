package org.jsoup.nodes;

/**
 An XML Declaration.

 @author Jonathan Hedley, jonathan@hedley.net */
public class XmlDeclaration extends Node {
    static final String DECL_KEY = "declaration";
    private final boolean isProcessingInstruction; // <! if true, <? if false, declaration (and last data char should be ?)

    /**
     Create a new XML declaration
     @param data data
     @param baseUri base uri
     @param isProcessingInstruction is processing instruction
     */
    public XmlDeclaration(String data, String baseUri, boolean isProcessingInstruction) {
        super(baseUri);
        attributes.put(DECL_KEY, data);
        this.isProcessingInstruction = isProcessingInstruction;
    }

    public String nodeName() {
        return "#declaration";
    }

    /**
     Get the unencoded XML declaration.
     @return XML declaration
     */
    public String getWholeDeclaration() {
        final String decl = attributes.get(DECL_KEY);
        
        if( decl.equals("xml") == true && attributes.size() > 1 ) {
            StringBuilder sb = new StringBuilder(decl);
            final String version = attributes.get("version");
            
            if( version != null ) {
                sb.append(" version=\"").append(version).append("\"");
            }
            
            final String encoding = attributes.get("encoding");
            
            if( encoding != null ) {
                sb.append(" encoding=\"").append(encoding).append("\"");
            }
            
            return sb.toString();
        }
        else {
            return attributes.get(DECL_KEY);
        }
    }
    
    void outerHtmlHead(StringBuilder accum, int depth, Document.OutputSettings out) {
        accum
                .append("<")
                .append(isProcessingInstruction ? "!" : "?")
                .append(getWholeDeclaration())
                .append(">");
    }

    void outerHtmlTail(StringBuilder accum, int depth, Document.OutputSettings out) {}

    @Override
    public String toString() {
        return outerHtml();
    }
}
