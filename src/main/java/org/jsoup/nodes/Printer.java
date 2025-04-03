package org.jsoup.nodes;

import org.jsoup.SerializationException;
import org.jsoup.select.NodeVisitor;

import java.io.IOException;

class Printer implements NodeVisitor {
    final Appendable accum;
    final Document.OutputSettings out;

    public Printer(Appendable accum, Document.OutputSettings out) {
        this.accum = accum;
        this.out = out;
    }

    void addHead(Node node, int depth) throws IOException {
        node.outerHtmlHead(accum, out);
    }

    void addTail(Node node, int depth) throws IOException {
        node.outerHtmlTail(accum, out);
    }

    void addText(TextNode textNode, int depth) throws IOException {
        textNode.outerHtmlHead(accum, out);
    }

    @Override
    public void head(Node node, int depth) {
        try {
            if (node.getClass() == TextNode.class) addText((TextNode) node, depth);
            else addHead(node, depth);
        } catch (IOException exception) {
            throw new SerializationException(exception);
        }
    }

    @Override
    public void tail(Node node, int depth) {
        if (!(node.getClass() == TextNode.class)) // saves a void hit
            try {
                addTail(node, depth);
            } catch (IOException exception) {
                throw new SerializationException(exception);
            }
    }

    static class Pretty extends Printer {
        public Pretty(Appendable accum, Document.OutputSettings out) {
            super(accum, out);
        }
    }

    static class Outline extends Printer {
        public Outline(Appendable accum, Document.OutputSettings out) {
            super(accum, out);
        }
    }

    static Printer printerFor(Appendable accum, Document.OutputSettings settings) {
        if (settings.prettyPrint()) return new Printer.Pretty(accum, settings);
        if (settings.outline()) return new Printer.Outline(accum, settings);
        return new Printer(accum, settings);
    }
}
