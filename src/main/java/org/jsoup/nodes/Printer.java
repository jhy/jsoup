package org.jsoup.nodes;

import org.jsoup.internal.QuietAppendable;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.parser.Tag;
import org.jsoup.select.NodeVisitor;
import org.jspecify.annotations.Nullable;

/** Base Printer */
class Printer implements NodeVisitor {
    final Node root;
    final QuietAppendable accum;
    final OutputSettings settings;

    Printer(Node root, QuietAppendable accum, OutputSettings settings) {
        this.root = root;
        this.accum = accum;
        this.settings = settings;
    }

    void addHead(Element el, int depth) {
        el.outerHtmlHead(accum, settings);
    }

    void addTail(Element el, int depth) {
        el.outerHtmlTail(accum, settings);
    }

    void addText(TextNode textNode, int textOptions, int depth) {
        int options = Entities.ForText | textOptions;
        Entities.escape(accum, textNode.coreValue(), settings, options);
    }

    void addNode(LeafNode node, int depth) {
        node.outerHtmlHead(accum, settings);
    }

    void indent(int depth) {
        accum.append('\n').append(StringUtil.padding(depth * settings.indentAmount(), settings.maxPaddingWidth()));
    }

    @Override
    public void head(Node node, int depth) {
        if (node.getClass() == TextNode.class)  addText((TextNode) node, 0, depth); // Excludes CData; falls to addNode
        else if (node instanceof Element)       addHead((Element) node, depth);
        else                                    addNode((LeafNode) node, depth);
    }

    @Override
    public void tail(Node node, int depth) {
        if (node instanceof Element) { // otherwise a LeafNode
            addTail((Element) node, depth);
        }
    }

    /** Pretty Printer */
    static class Pretty extends Printer {
        boolean preserveWhitespace = false;

        Pretty(Node root, QuietAppendable accum, OutputSettings settings) {
            super(root, accum, settings);

            // check if there is a pre on stack
            for (Node node = root; node != null; node = node.parentNode()) {
                if (tagIs(Tag.PreserveWhitespace, node)) {
                    preserveWhitespace = true;
                    break;
                }
            }
        }

        @Override
        void addHead(Element el, int depth) {
            if (shouldIndent(el))
                indent(depth);
            super.addHead(el, depth);
            if (tagIs(Tag.PreserveWhitespace, el)) preserveWhitespace = true;
        }

        @Override
        void addTail(Element el, int depth) {
            if (shouldIndent(nextNonBlank(el.firstChild()))) {
                indent(depth);
            }
            super.addTail(el, depth);

            // clear the preserveWhitespace if this element is not, and there are none on the stack above
            if (preserveWhitespace && el.tag.is(Tag.PreserveWhitespace)) {
                for (Element parent = el.parent(); parent != null; parent = parent.parent()) {
                    if (parent.tag().preserveWhitespace()) return; // keep
                }
                preserveWhitespace = false;
            }
        }

        @Override
        void addNode(LeafNode node, int depth) {
            if (shouldIndent(node))
                indent(depth);
            super.addNode(node, depth);
        }

        @Override
        void addText(TextNode node, int textOptions, int depth) {
            if (!preserveWhitespace) {
                textOptions |= Entities.Normalise;
                textOptions = textTrim(node, textOptions);

                if (!node.isBlank() && isBlockEl(node.parentNode) && shouldIndent(node))
                    indent(depth);
            }

            super.addText(node, textOptions, depth);
        }

        int textTrim(TextNode node, int options) {
            if (!isBlockEl(node.parentNode)) return options; // don't trim inline, whitespace significant
            Node prev = node.previousSibling();
            Node next = node.nextSibling();

            // if previous is not an inline element
            if (!(prev instanceof Element && !isBlockEl(prev))) {
                // if there is no previous sib; or not a text node and should be indented
                if (prev == null || !(prev instanceof TextNode) && shouldIndent(prev))
                    options |= Entities.TrimLeading;
            }

            if (next == null || !(next instanceof TextNode) && shouldIndent(next))
                options |= Entities.TrimTrailing; // don't trim if there is a TextNode sequence

            return options;
        }

        boolean shouldIndent(@Nullable Node node) {
            if (node == null || node == root || preserveWhitespace || isBlankText(node))
                return false;
            if (isBlockEl(node))
                return true;

            Node prevSib = previousNonblank(node);
            if (isBlockEl(prevSib)) return true;

            Element parent = (Element) node.parentNode;
            if (!isBlockEl(parent) || parent.tag().is(Tag.InlineContainer) || !hasNonTextNodes(parent))
                return false;

            return prevSib == null ||
                (!(prevSib instanceof TextNode) &&
                    (isBlockEl(prevSib) || !(prevSib instanceof Element)));
        }

        boolean isBlockEl(@Nullable Node node) {
            if (node == null) return false;
            if (node instanceof Element) {
                Element el = (Element) node;
                return el.isBlock() ||
                    (!el.tag.isKnownTag() && (el.parentNode instanceof Document || hasChildBlocks(el)));
            }

            return false;
        }

        /**
         Returns true if any of the Element's child nodes should indent. Checks the last 5 nodes only (to minimize
         scans).
         */
        static boolean hasChildBlocks(Element el) {
            Element child = el.firstElementChild();
            for (int i = 0; i < maxScan && child != null; i++) {
                if (child.isBlock() || !child.tag.isKnownTag()) return true;
                child = child.nextElementSibling();
            }
            return false;
        }
        static private final int maxScan = 5;

        static boolean hasNonTextNodes(Element el) {
            Node child = el.firstChild();
            for (int i = 0; i < maxScan && child != null; i++) {
                if (!(child instanceof TextNode)) return true;
                child = child.nextSibling();
            }
            return false;
        }

        static @Nullable Node previousNonblank(Node node) {
            Node prev = node.previousSibling();
            while (isBlankText(prev)) prev = prev.previousSibling();
            return prev;
        }

        static @Nullable Node nextNonBlank(@Nullable Node node) {
            while (isBlankText(node)) node = node.nextSibling();
            return node;
        }

        static boolean isBlankText(@Nullable Node node) {
            return node instanceof TextNode && ((TextNode) node).isBlank();
        }

        static boolean tagIs(int option, @Nullable Node node) {
            return node instanceof Element && ((Element) node).tag.is(option);
        }
    }

    /** Outline Printer */
    static class Outline extends Pretty {
        Outline(Node root, QuietAppendable accum, OutputSettings settings) {
            super(root, accum, settings);
        }

        @Override
        boolean isBlockEl(@Nullable Node node) {
            return node != null;
        }

        @Override
        boolean shouldIndent(@Nullable Node node) {
            if (node == null || node == root || preserveWhitespace || isBlankText(node))
                return false;
            if (node instanceof TextNode) {
                return node.previousSibling() != null || node.nextSibling() != null;
            }
            return true;
        }
    }

    static Printer printerFor(Node root, QuietAppendable accum) {
        OutputSettings settings = NodeUtils.outputSettings(root);
        if (settings.outline())     return new Printer.Outline(root, accum, settings);
        if (settings.prettyPrint()) return new Printer.Pretty(root, accum, settings);
        return new Printer(root, accum, settings);
    }
}
