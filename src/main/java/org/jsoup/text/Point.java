package org.jsoup.text;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;


/**
 * A single point in a document. This is either before the first
 * character in a document, between two, or after the last
 * character. Point is usually used only as start/end of a
 * {@link Region}.
 *
 * <p>A Point is always in a TextNode, which means that in a sequence
 * such as abc&lt;u&gt;def, there are two possible Points between c
 * and d, namely, tied to the c and to the d. The former
 * ({@link #leftBound()}) is usually better if you want to operate on a
 * {@link Region} that ends with the c, the latter ({@link #rightBound()})
 * is usually better if you want to operate on a {@link Region} that
 * starts with the d.
 *
 * <p>This class contains both a normal constructor and a number of
 * functions that create new points: If you have a point, you can get
 * a new point at the start or end of the surrounding block/inline
 * element, and if you have an {@link org.jsoup.node.Element}, you can
 * get a point at the start or end of that Element.
 */
public class Point {
    private TextNode in;
    private int offset;

    /**
     *  Creates a Point before that character in that node.
     *
     *  <p>If character is larger than the number of characters in the
     *  node, then the new point is after the last character in the
     *  node (which is subtly different from being before the first
     *  character of the following TextNode).
     *
     *  This is immutable, except that the TextNode may be changed.
     */
    public Point(final TextNode node, final int character) {
        in = node;
        offset = Math.min(character, node.getWholeText().length());
    }

    /**
     * Returns the TextNode containing this Point.
     */
    public TextNode getTextNode() {
        revalidate();
        return in;
    }

    /**
     * Returns the offset of this Point in getTextNode().
     */
    public int getOffset() {
        revalidate();
        return offset;
    }

    /**
     * Return a Point at the same location as this, and in the same
     * {@link org.jsoup.nodes.TextNode} as to the character on its
     * left.
     *
     * <p>There is one exception: If this Point is before the first
     * character within the specified Node, then the Point returned is
     * (necessarily) tied to the character on its right.
     */
    public Point leftBound(final Node within) {
        revalidate();
        if(offset > 0)
            return this;
        Region.NeighbouringTextNode previous =
            Region.NeighbouringTextNode.previous(in, within);
        if(previous.textNode == null)
            return this;
        return new Point(previous.textNode,
                         previous.textNode.getWholeText().length());
    }

    /**
     * Return a Point at the same location as this, and in the same
     * {@link org.jsoup.nodes.TextNode} as to the character on its
     * left.
     *
     * <p>There is one exception: If this Point is before the first
     * character in the {@link org.jsoup.nodes.Document}, then the
     * Point returned is (necessarily) tied to the character on its
     * right.
     */
    public Point leftBound() {
        return leftBound(null);
    }

    /**
     * Return a Point at the same location as this, and in the same
     * {@link org.jsoup.nodes.TextNode} as to the character on its
     * right. This is usually self.
     *
     * <p>There is one exception: If this Point is after the last
     * character within the Node supplied, then the Point returned is
     * (necessarily) tied to the character on its left.
     */
    public Point rightBound(final Node within) {
        revalidate();
        if(offset < in.getWholeText().length())
            return this;
        Region.NeighbouringTextNode next =
            Region.NeighbouringTextNode.next(in, within);
        if(next.textNode == null)
            return this;
        return new Point(next.textNode, 0);
    }

    /**
     * Return a Point at the same location as this, and in the same
     * {@link org.jsoup.nodes.TextNode} as to the character on its
     * right.
     *
     * <p>There is one exception: If this Point is after the last
     * character in the {@link org.jsoup.nodes.Document}, then
     * the Point is (necessarily) tied to the character on its left.
     */
    public Point rightBound() {
        return rightBound(null);
    }

    /**
     *  Make sure this Point points after the last character of a
     *  TextNode, which is the last child of its parent, which is the
     *  last child, etc, until the supplied grandParent. That node is
     *  not affected (but may receive one extra child).
     */
    void splitAfterUpTo(final Node grandParent) {
        revalidate();
        if(offset > 0 && offset < in.getWholeText().length())
            in.splitText(offset);
        Node n = in;
        while(n.parent() != null && n.parent() != grandParent) {
            n.parent().splitAfter(n);
            n = n.parent();
        }
    }

    /**
     *  Return the next region after this Point and within the
     *  specified Node that contains text, or null if no such region
     *  exists. (Whitespace matches whitespace, any other characters
     *  must match exactly.)
     */
    public Region findNext(final String text, final Node within) {
        revalidate();
        return Region.findNext(text, getTextNode(), getOffset(),
                               within);
    }

    /**
     *  Return a Region containing up to about the specified number of
     *  characters starting at this point. The limit is counted along
     *  the lines of {@link org.jsoup.nodes.TextNode#getWholeText()}. Note
     *  that {@link org.jsoup.text.Region#getText()} may add spaces at
     *  block element boundaries and collapse adjacent spaces, so the
     *  two counts may be slightly different.
     *
     *  <p>The returned node can be much shorter, if this point is
     *  close to the end of the document.
     */
    public Region followingRegion(final int characters) {
        return followingRegion(characters, null);
    }

    /**
     *  Return a Region containing up to about the specified number of
     *  characters starting at this point. The limit is counted along
     *  the lines of {@link org.jsoup.nodes.TextNode#getWholeText()}. Note
     *  that {@link org.jsoup.text.Region#getText()} may add spaces at
     *  block element boundaries and collapse adjacent spaces, so the
     *  two counts may be slightly different.
     *
     *  <p>The returned node can be much shorter, since it's
     *  constrained to be within the specified Node (if non-null).
     */
    public Region followingRegion(final int characters, final Node within) {
        revalidate();
        int have = in.getWholeText().length() - offset;
        TextNode n = in;
        while(have < characters) {
            TextNode next =
                Region.NeighbouringTextNode.next(n, within).textNode;
            if(next == null)
                return new Region(this,
                                  new Point(n, n.getWholeText().length()));
            n = next;
            have += n.getWholeText().length();
        }
        int unneeded = have - characters;
        Point end = new Point(n, n.getWholeText().length() - unneeded);
        return new Region(this, end);
    }

    /**
     *  Return a Region containing up to about the specified number of
     *  characters end at this point. The limit is counted along
     *  the lines of {@link org.jsoup.nodes.TextNode#getWholeText()}. Note
     *  that {@link org.jsoup.text.Region#getText()} may add spaces at
     *  block element boundaries and collapse adjacent spaces, so the
     *  two counts may be slightly different.
     *
     *  <p>The returned node can be much shorter, if this point is
     *  close to the start of the document.
     */
    public Region precedingRegion(final int characters) {
        return precedingRegion(characters, null);
    }

    /**
     *  Return a Region containing up to about the specified number of
     *  characters end at this point. The limit is counted along
     *  the lines of {@link org.jsoup.nodes.TextNode#getWholeText()}. Note
     *  that {@link org.jsoup.text.Region#getText()} may add spaces at
     *  block element boundaries and collapse adjacent spaces, so the
     *  two counts may be slightly different.
     *
     *  <p>The returned node can be much shorter, since it's
     *  constrained to be within the specified Node (if non-null).
     */
    public Region precedingRegion(final int characters, final Node within) {
        revalidate();
        int have = offset;
        TextNode n = in;
        while(have < characters) {
            TextNode previous =
                Region.NeighbouringTextNode.previous(n, within).textNode;
            if(previous == null)
                return new Region(new Point(n, 0), this);
            n = previous;
            have += n.getWholeText().length();
        }
        int unneeded = have - characters;
        Point start = new Point(n, unneeded);
        return new Region(start, this);
    }

    private Point boundaryHelper(boolean block, boolean end) {
        Node n = in;
        TextNode result = null;
        while(n != null &&
              (!(n instanceof Element) ||
               (block && !((Element)n).isBlock())))
            n = n.parent();
        if(end)
            result = Region.NeighbouringTextNode.previous(n, n).textNode;
        else
            result = Region.NeighbouringTextNode.next(n, n).textNode;
        if(result == null)
            result = in;
        if(end)
            return new Point(result, result.getWholeText().length());
        else
            return new Point(result, 0);
    }

    /**
     *  Return a Point just before the first character in the same
     *  Element as this Point.
     */

    public Point atStartOfElement() {
        return boundaryHelper(false, false);
    }

    /**
     *  Return a Point just before the first character in the same
     *  block Element as this Point; typically this is the start of a
     *  paragraph.
     */

    public Point atStartOfBlock() {
        return boundaryHelper(true, false);
    }

    /**
     *  Return a Point just after the last character in the same
     *  Element as this Point.
     */

    public Point atEndOfElement() {
        return boundaryHelper(false, true);
    }

    /**
     *  Return a Point just after the last character in the same block
     *  Element as this Point; typically this is the end of a
     *  paragraph.
     */

    public Point atEndOfBlock() {
        return boundaryHelper(true, true);
    }

    /**
     *  Return a Point just before the first character within e, if
     *  necessary creating an empty TextNode.
     */

    static public Point atStartOfElement(final Element e) {
        TextNode result = Region.NeighbouringTextNode.next(e, e).textNode;
        if(result == null) {
            e.prependText("");
            result = Region.NeighbouringTextNode.next(e, e).textNode;
        }
        return new Point(result, 0);
    }

    /**
     *  Return a Point just after the last character within e, if
     *  necessary creating an empty TextNode.
     */

    static public Point atEndOfElement(final Element e) {
        TextNode result = Region.NeighbouringTextNode.previous(e, e).textNode;
        if(result == null) {
            e.appendText("");
            result = Region.NeighbouringTextNode.previous(e, e).textNode;
        }
        return new Point(result, result.getWholeText().length());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Point)) return false;

        revalidate();
        Point other = (Point)o;
        return in == other.in && offset == other.offset;
    }

    @Override
    public int hashCode() {
        revalidate();
        return in.hashCode() * 31 + offset;
    }

    @Override
    public String toString() {
        revalidate();
        return in + ": " + offset;
    }

    /**
     *  If a Point has been rendered invalid by
     *  Region.splitTextNodes() or Point.splitAfterUpTo(), then this
     *  private helper function undoes the damage. However, it does no
     *  magic, and of course cannot recover from substantive changes to
     *  the document.
     */

    private void revalidate() {
        while(offset > in.getWholeText().length()) {
            Node n = in.nextSibling();
            if(!(n instanceof TextNode))
                throw new IllegalArgumentException("Invalid point used");
            offset -= in.getWholeText().length();
            in = (TextNode)n;
        }
    }
}
