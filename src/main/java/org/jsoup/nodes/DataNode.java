package org.jsoup.nodes;

import java.io.IOException;

/**
 A data node, for contents of style, script tags etc, where contents should not show in text().

 @author Jonathan Hedley, jonathan@hedley.net */
public class DataNode extends LeafNode {

    /**
     Create a new DataNode.
     @param data data contents
     */
    public DataNode(String data) {
        super(data);
    }

    @Override public String nodeName() {
        return "#data";
    }

    /**
     Get the data contents of this node. Will be unescaped and with original new lines, space etc.
     @return data
     */
    public String getWholeData() {
        return coreValue();
    }

    /**
     * Set the data contents of this node.
     * @param data un-encoded data
     * @return this node, for chaining
     */
    public DataNode setWholeData(String data) {
        coreValue(data);
        return this;
    }

    @Override
    void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
        /* For XML output, escape the DataNode in a CData section. The data may contain pseudo-CData content if it was
        parsed as HTML, so don't double up Cdata. Output in polyglot HTML / XHTML / XML format. */
        final String data = getWholeData();
        
        if (isXmlSyntax(out) && !data.contains("<![CDATA[")) {
            handleXmlOutput(accum, data);
        } else {
            handleHtmlOutput(accum, data);
        }
    }
    private boolean isXmlSyntax(Document.OutputSettings out) {
        return out.syntax() == Document.OutputSettings.Syntax.xml;
    }
    
    private void handleXmlOutput(Appendable accum, String data) throws IOException {
        if (parentNameIs("script")) {
            appendCData(accum, data, "//<![CDATA[\n", "\n//]]>");
        } else if (parentNameIs("style")) {
            appendCData(accum, data, "/*<![CDATA[*/\n", "\n/*]]>*/");
        } else {
            accum.append("<![CDATA[").append(data).append("]]>");
        }
    }
    
    private void handleHtmlOutput(Appendable accum, String data) throws IOException {
        // In HTML, data is not escaped in the output of data nodes, so < and & in script, style are OK
        accum.append(data);
    }
    
    private void appendCData(Appendable accum, String data, String openingTag, String closingTag) throws IOException {
        accum.append(openingTag).append(data).append(closingTag);
    }

    @Override
    void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) {}

    @Override
    public DataNode clone() {
        return (DataNode) super.clone();
    }
}
