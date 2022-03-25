package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;

import java.io.IOException;

public class OuterHtmlVisitor implements NodeVisitor {
    private final Appendable accum;
    private final Document.OutputSettings out;

    OuterHtmlVisitor(Appendable accum, Document.OutputSettings out) {
        this.accum = accum;
        this.out = out;
        out.prepareEncoder();
    }

    public void head(Node node, int depth) {
        try {
            node.outerHtmlHead(accum, depth, out);
        } catch (IOException exception) {
            throw new SerializationException(exception);
        }
    }

    public void tail(Node node, int depth) {
        if (!node.nodeName().equals("#text")) { // saves a void hit.
            try {
                node.outerHtmlTail(accum, depth, out);
            } catch (IOException exception) {
                throw new SerializationException(exception);
            }
        }
    }
}