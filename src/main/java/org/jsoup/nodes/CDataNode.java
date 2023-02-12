package org.jsoup.nodes;

import java.io.IOException;

/**
 * A Character Data node, to support CDATA sections.
 */
public class CDataNode extends TextNode {
    public CDataNode(String text) {
        super(text);
    }

    @Override
    public String nodeName() {
        return "#cdata";
    }

    /**
     * Get the unencoded, <b>non-normalized</b> text content of this CDataNode.
     * @return unencoded, non-normalized text
     */
    @Override
    public String text() {
        return getWholeText();
    }

    @Override
    void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
        accum
            .append("<![CDATA[")
            .append(getWholeText());
    }

    @Override
    void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
        accum.append("]]>");
    }

    @Override
    public CDataNode clone() {
        return (CDataNode) super.clone();
    }
}
