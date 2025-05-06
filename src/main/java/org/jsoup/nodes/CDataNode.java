package org.jsoup.nodes;

import org.jsoup.internal.QuietAppendable;

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
     * Get the un-encoded, <b>non-normalized</b> text content of this CDataNode.
     * @return un-encoded, non-normalized text
     */
    @Override
    public String text() {
        return getWholeText();
    }

    @Override
    void outerHtmlHead(QuietAppendable accum, Document.OutputSettings out) {
        accum
            .append("<![CDATA[")
            .append(getWholeText())
            .append("]]>");
    }

    @Override
    public CDataNode clone() {
        return (CDataNode) super.clone();
    }
}
