package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jsoup.internal.StringUtil;

import java.io.IOException;

/**
 A text node.

 @author Jonathan Hedley, jonathan@hedley.net */
public class TextNode extends LeafNode {
    /**
     Create a new TextNode representing the supplied (unencoded) text).

     @param text raw text
     @see #createFromEncoded(String)
     */
    public TextNode(String text) {
        value = text;
    }

	public String nodeName() {
        return "#text";
    }
    
    /**
     * Get the text content of this text node.
     * @return Unencoded, normalised text.
     * @see TextNode#getWholeText()
     */
    public String text() {
        return StringUtil.normaliseWhitespace(getWholeText());
    }
    
    /**
     * Set the text content of this text node.
     * @param text unencoded text
     * @return this, for chaining
     */
    public TextNode text(String text) {
        coreValue(text);
        return this;
    }

    /**
     Get the (unencoded) text of this text node, including any newlines and spaces present in the original.
     @return text
     */
    public String getWholeText() {
        return coreValue();
    }

    /**
     Test if this text node is blank -- that is, empty or only whitespace (including newlines).
     @return true if this document is empty or only whitespace, false if it contains any text content.
     */
    public boolean isBlank() {
        return StringUtil.isBlank(coreValue());
    }

    /**
     * Split this text node into two nodes at the specified string offset. After splitting, this node will contain the
     * original text up to the offset, and will have a new text node sibling containing the text after the offset.
     * @param offset string offset point to split node at.
     * @return the newly created text node containing the text after the offset.
     */
    public TextNode splitText(int offset) {
        final String text = coreValue();
        Validate.isTrue(offset >= 0, "Split offset must be not be negative");
        Validate.isTrue(offset < text.length(), "Split offset must not be greater than current text length");

        String head = text.substring(0, offset);
        String tail = text.substring(offset);
        text(head);
        TextNode tailNode = new TextNode(tail);
        if (parentNode != null)
            parentNode.addChildren(siblingIndex()+1, tailNode);

        return tailNode;
    }

    @Override
    void outerHtmlHead(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
        final boolean prettyPrint = out.prettyPrint();
        final Element parent = parentNode instanceof Element ? ((Element) parentNode) : null;
        final boolean normaliseWhite = prettyPrint && !Element.preserveWhitespace(parentNode);
        final boolean trimLikeBlock = parent != null && (parent.tag().isBlock() || parent.tag().formatAsBlock());
        boolean trimLeading = false, trimTrailing = false;

        if (normaliseWhite) {
            trimLeading = (trimLikeBlock && siblingIndex == 0) || parentNode instanceof Document;
            trimTrailing = trimLikeBlock && nextSibling() == null;

            // if this text is just whitespace, and the next node will cause an indent, skip this text:
            Node next = nextSibling();
            Node prev = previousSibling();
            boolean isBlank = isBlank();
            boolean couldSkip = (next instanceof Element && ((Element) next).shouldIndent(out)) // next will indent
                || (next instanceof TextNode && (((TextNode) next).isBlank())) // next is blank text, from re-parenting
                || (prev instanceof Element && (((Element) prev).isBlock() || prev.isNode("br"))) // br is a bit special - make sure we don't get a dangling blank line, but not a block otherwise wraps in head
                ;
            if (couldSkip && isBlank) return;

            if (
                (siblingIndex == 0 && parent != null && parent.tag().formatAsBlock() && !isBlank) ||
                (out.outline() && siblingNodes().size() > 0 && !isBlank) ||
                (siblingIndex > 0 && isNode(prev, "br")) // special case wrap on inline <br> - doesn't make sense as a block tag
            )
                indent(accum, depth, out);
        }

        Entities.escape(accum, coreValue(), out, false, normaliseWhite, trimLeading, trimTrailing);
    }

    @Override
    void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) throws IOException {}

    @Override
    public String toString() {
        return outerHtml();
    }

    @Override
    public TextNode clone() {
        return (TextNode) super.clone();
    }

    /**
     * Create a new TextNode from HTML encoded (aka escaped) data.
     * @param encodedText Text containing encoded HTML (e.g. {@code &lt;})
     * @return TextNode containing unencoded data (e.g. {@code <})
     */
    public static TextNode createFromEncoded(String encodedText) {
        String text = Entities.unescape(encodedText);
        return new TextNode(text);
    }

    static String normaliseWhitespace(String text) {
        text = StringUtil.normaliseWhitespace(text);
        return text;
    }

    static String stripLeadingWhitespace(String text) {
        return text.replaceFirst("^\\s+", "");
    }

    static boolean lastCharIsWhitespace(StringBuilder sb) {
        return sb.length() != 0 && sb.charAt(sb.length() - 1) == ' ';
    }
}
