package org.jsoup.nodes;

import org.jsoup.internal.StringUtil;
import org.jsoup.parser.Tag;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeVisitor;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.jsoup.nodes.TextNode.lastCharIsWhitespace;

/**
 Package-private helper that handles text extraction operations for {@link Element}.
 Extracted from Element to reduce its size (God Class / Large Class smell).
 */
class ElementText {

    static String text(Element el) {
        final StringBuilder accum = StringUtil.borrowBuilder();
        new TextAccumulator(accum).traverse(el);
        return StringUtil.releaseBuilder(accum).trim();
    }

    static String wholeText(Element el) {
        return wholeTextOf(el.nodeStream());
    }

    static String wholeOwnText(Element el) {
        return wholeTextOf(el.childNodes.stream());
    }

    static String ownText(Element el) {
        StringBuilder sb = StringUtil.borrowBuilder();
        ownText(el, sb);
        return StringUtil.releaseBuilder(sb).trim();
    }

    static boolean hasText(Element el) {
        AtomicBoolean hasText = new AtomicBoolean(false);
        el.filter((node, depth) -> {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                if (!textNode.isBlank()) {
                    hasText.set(true);
                    return NodeFilter.FilterResult.STOP;
                }
            }
            return NodeFilter.FilterResult.CONTINUE;
        });
        return hasText.get();
    }

    static String data(Element el) {
        StringBuilder sb = StringUtil.borrowBuilder();
        el.traverse((childNode, depth) -> {
            if (childNode instanceof DataNode) {
                DataNode data = (DataNode) childNode;
                sb.append(data.getWholeData());
            } else if (childNode instanceof Comment) {
                Comment comment = (Comment) childNode;
                sb.append(comment.getData());
            } else if (childNode instanceof CDataNode) {
                CDataNode cDataNode = (CDataNode) childNode;
                sb.append(cDataNode.getWholeText());
            }
        });
        return StringUtil.releaseBuilder(sb);
    }

    private static void ownText(Element el, StringBuilder accum) {
        for (int i = 0; i < el.childNodeSize(); i++) {
            Node child = el.childNodes.get(i);
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                appendNormalisedText(accum, textNode);
            } else if (child.nameIs("br") && !lastCharIsWhitespace(accum)) {
                accum.append(" ");
            }
        }
    }

    static void appendNormalisedText(StringBuilder accum, TextNode textNode) {
        String text = textNode.getWholeText();
        if (preserveWhitespace(textNode.parentNode) || textNode instanceof CDataNode)
            accum.append(text);
        else
            StringUtil.appendNormalisedWhitespace(accum, text, lastCharIsWhitespace(accum));
    }

    static boolean preserveWhitespace(@Nullable Node node) {
        if (node instanceof Element) {
            Element el = (Element) node;
            int i = 0;
            do {
                if (el.tag.preserveWhitespace())
                    return true;
                el = el.parent();
                i++;
            } while (i < 6 && el != null);
        }
        return false;
    }

    private static String wholeTextOf(Stream<Node> stream) {
        return stream.map(node -> {
            if (node instanceof TextNode) return ((TextNode) node).getWholeText();
            if (node.nameIs("br")) return "\n";
            return "";
        }).collect(StringUtil.joining(""));
    }

    private static class TextAccumulator implements NodeVisitor {
        private final StringBuilder accum;

        TextAccumulator(StringBuilder accum) {
            this.accum = accum;
        }

        @Override public void head(Node node, int depth) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                appendNormalisedText(accum, textNode);
            } else if (node instanceof Element) {
                Element element = (Element) node;
                if (accum.length() > 0 && needsLeadingTextSeparator(element) && !lastCharIsWhitespace(accum))
                    accum.append(' ');
            }
        }

        @Override public void tail(Node node, int depth) {
            if (node instanceof Element) {
                Element element = (Element) node;
                Node next = node.nextSibling();
                if (needsTrailingTextSeparator(element) &&
                    (next instanceof TextNode || next instanceof Element && ((Element) next).tag.isInline()) &&
                    !lastCharIsWhitespace(accum))
                    accum.append(' ');
            }
        }

        private static boolean needsLeadingTextSeparator(Element element) {
            return element.isBlock()
                || element.nameIs("br")
                || element.tag.is(Tag.TextBoundary) && element.childNodeSize() > 0 && element.hasText();
        }

        private static boolean needsTrailingTextSeparator(Element element) {
            return element.tag.is(Tag.TextBoundary)
                || !element.tag.isInline()
                || hasBlockChild(element);
        }

        private static boolean hasBlockChild(Element element) {
            for (int i = 0; i < element.childNodeSize(); i++) {
                Node child = element.childNode(i);
                if (child instanceof Element && ((Element) child).isBlock())
                    return true;
            }
            return false;
        }
    }
}
