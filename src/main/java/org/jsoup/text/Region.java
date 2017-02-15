package org.jsoup.text;

import java.util.LinkedList;
import java.util.List;

import org.jsoup.helper.StringUtil;

import static org.jsoup.helper.StringUtil.appendNormalisedWhitespace;
import static org.jsoup.helper.StringUtil.isWhitespace;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import org.jsoup.parser.Parser;


/**
 *  A part of a document, consisting of a start and an end Point.
 *
 *  <p>The Region class permits you to find text by text (rather than by
 *  structure, as {@Link Element#select()} does) and remove, wrap or
 *  replace it.
 *
 *  <p>Since a Region may span elements, even block elements, changing
 *  it can necessarily involve fairly deep changes in the DOM
 *  structure. For example, wrapping the last word of one paragraph
 *  and the first of the following in &lt;a href&gt; moves the words
 *  to two new one-word paragraphs wrapped in an &lt;a href&gt;.  You
 *  can detect and/or avoid such deep changes using
 *  spansBlockElements(), splitByBlockElements(), spansElements() and
 *  splitByElements(), so that you can disturb nothing, only inline
 *  elements or all ones, as appropriate for each modification.
 */
public class Region {
    private Point start;
    private Point end;

    /**
     *  Create a Region from p1 to p2, which must be at or after p1.
     */
    public Region(final Point p1, final Point p2) {
        start = p1.rightBound();
        end = p2.leftBound();
        if(p1.equals(end) || p2.equals(start))
            end = start;
    }

    /**
     *  Create a Region spanning all of n.
     */
    public Region(final Element e) {
        start = Point.atStartOfElement(e);
        end = Point.atEndOfElement(e);
    }

    /**
     * Return the start of this region.
     */
    public Point getStart() {
        return start;
    }

    /**
     * Return the end of this Region.
     */
    public Point getEnd() {
        return end;
    }

    /**
     * Return the textual content of this Region, normalised.
     * Crossing a block element boundary results in a single space,
     * crossing an inline element boundary results in nothing.
     */
    public String getText() {
        if(start.getTextNode() == end.getTextNode()) {
            String whole = start.getTextNode().getWholeText()
                .substring(start.getOffset(), end.getOffset());
            return StringUtil.normaliseWhitespace(whole).trim();
        }

        StringBuilder result = new StringBuilder();
        appendNormalisedWhitespace(result,
                                   start.getTextNode().getWholeText()
                                   .substring(start.getOffset()),
                                   false);
        NeighbouringTextNode next =
            NeighbouringTextNode.next(start.getTextNode(), null);
        while(next.textNode != null && next.textNode != end.getTextNode()) {
            if(next.hasSeenBlockElement &&
               (result.length() > 0 &&
                !isWhitespace(result.charAt(result.length()-1))))
               result.append(" ");
            appendNormalisedWhitespace(result,
                                       next.textNode.getWholeText(),
                                       next.hasSeenBlockElement);
            next = NeighbouringTextNode.next(next.textNode, null);
        }
        if(next.hasSeenBlockElement &&
           (result.length() > 0 &&
            !isWhitespace(result.charAt(result.length()-1))))
            result.append(" ");
        appendNormalisedWhitespace(result,
                                   end.getTextNode().getWholeText()
                                   .substring(0, end.getOffset()),
                                   next.hasSeenBlockElement);
        return result.toString().trim();
    }



    /**
     *  Split the Region into Regions such that each Region is entirely
     *  within a single Element.
     *
     *  <p>This is useful if you want to, say, elide the region: You
     *  can replace the first Region with an ellipsis and drop the
     *  remainder outright.
     */
    public Regions splitByElements() {
        return regionSplitHelper(false);
    }

    /**
     *  Split the Region into Regions such that each Region is entirely
     *  within a single Element.
     *
     *  <p>This is useful if you want to, say, wrap the region in
     *  &lt;a href...&gt; or &lt;b&gt;.
     */
    public Regions splitByBlockElements() {
        return regionSplitHelper(true);
    }

    Regions regionSplitHelper(boolean block) {
        Regions result = new Regions();
        Point s = start;
        TextNode t = start.getTextNode();
        while(t != null && t != end.getTextNode()) {
            NeighbouringTextNode n = NeighbouringTextNode.next(t, null);
            if(n.textNode != null &&
               (n.hasSeenBlockElement ||
                (n.hasSeenElement && !block))) {
                result.add(new Region(s,
                                      new Point(t, t.getWholeText().length())));
                s = new Point(n.textNode, 0);
            }
            t = n.textNode;
        }
        if(!s.equals(end))
            result.add(new Region(s, end));
        return result;
    }

    /**
     *  Return true if this Region contains characters in different
     *  elements, as e.g. "...baq&lt;qux..." does, and false if all
     *  characters are in the same element.
     */

    public boolean spansMultipleElements() {
        return spanHelper(false);
    }

    /**
     *  Return true if this Region contains characters in different
     *  block elements, as e.g. "... Great
     *  Again&lt;/h1&gt;&lt;p&gt;Just ..." does, and false if all
     *  characters are in the same block element.
     */

    public boolean spansMultipleBlockElements() {
        return spanHelper(true);
    }

    boolean spanHelper(boolean block) {
        TextNode t = start.getTextNode();
        while(t != end.getTextNode()) {
            NeighbouringTextNode n = NeighbouringTextNode.next(t, null);
            if(n.hasSeenBlockElement ||
               (n.hasSeenElement && !block))
                return true;
            t = n.textNode;
        }
        return false;
    }

    /**
     *  Split the start and end nodes such that each TextNode is
     *  either inside or outside this Region.
     *
     *  <p>Note that if two Region start/end in the same
     *  {@link org.jsoup.nodes.TextNode}, then splitting one may
     *  invalidate the other, which can make working with
     *  {@link Regions} tricky. The simplest way to avoid this problem
     *  is to call splitTextNodes() on each Region immediately after
     *  construction, as {@link org.jsoup.nodes.Element#find()} does.
     */
    public void splitTextNodes() {
        if(start.getOffset() == 0 &&
           end.getOffset() == end.getTextNode().getWholeText().length())
            return;

        end.splitAfterUpTo(end.getTextNode().parent());
        if(start.getOffset() == 0)
            return;

        start.splitAfterUpTo(start.getTextNode().parent());
        if(end.getTextNode() == start.getTextNode()) {
            int length = end.getOffset() - start.getOffset();
            start = start.rightBound();
            end = new Point(start.getTextNode(), length);
        } else {
            start = start.rightBound();
        }
    }


    /**
     *  Remove this Region from the Document.
     *
     *  <p>This removes the TextNodes, and those parents which would
     *  be empty afterwards. It does not remove a strict subtree, but
     *  rather a number of siblings and their children. (If a div
     *  contains four paragraphs, and you remove the middle two, the
     *  div is not affected, it merely loses two of its four children.)
     */
    public void remove() {
        for(Node n : parents())
            n.remove();
    }


    /**
     *  Wrap this Region in the new HTML and returns the wrapper.
     *
     *  <p>This splits {@link TextNode}s and {@link Element}s just
     *  sufficiently to that the new HTML can wrap the region. (If a
     *  div contains four paragraphs, and you wrap the middle two, the
     *  div has three children afterwards, and the middle child has
     *  two.)
     *
     *  <p>This returns null if the region is completely empty or if html
     *  cannot be parsed.
     */
    public Element wrap(final String html) {
        List<Node> region = parents();
        if(region.isEmpty())
            return null;
        Node parent = region.get(0).parent();
        Element context = parent instanceof Element ? (Element) parent : null;
        List<Node> wrappers =
            Parser.parseFragment(html, context, parent.root().baseUri());
        Element wrapper = (Element)wrappers.get(0);
        if (wrapper == null || !(wrapper instanceof Element))
            return null;
        region.get(0).before(wrapper);
        wrapper.insertChildren(0, region);
        return wrapper;
    }


    /**
     *  Return a list of siblings such that all of this region is
     *  within the return value, and nothing outside the Region is.
     *
     *  <p>Note that this changes the document (just) enough that
     *  there is such a flock of siblings.
     */
    public List<Node> parents() {
        ensureRemovability();
        List<Node> result = new LinkedList<Node>();
        Node common = parentElement();
        if(common == null)
            return result;
        Node first = parentChain(start.getTextNode(), common).get(0);
        Node last = parentChain(end.getTextNode(), common).get(0);
        if(last == first) {
            result.add(first);
            return result;
        }
        Node n = first;
        while(n != null && n != last) {
            result.add(n);
            n = n.nextSibling();
        };
        result.add(last);
        return result;
    }


    /**
     *  Split the parent nodes of the start and end point such that if
     *  a node is a parent if anything outside the region, then it
     *  either is also the parent of the complete region, or is not
     *  the parent of anything in the region.
     */
    public void ensureRemovability() {
        splitTextNodes();
        Node common = parentElement();
        if(common == null)
            return;
        end.splitAfterUpTo(common);
        Point before = start.leftBound(common);
        if(before != start && before.getTextNode() != null)
            before.splitAfterUpTo(common);
    }


    /**
     *  Return the closest Element that spans all of this Region and also
     *  spans at least one character outside it.
     *
     *  <p>If the Region spans all of the document, then the result is
     *  the closest parent Element that spans the entire Region. If
     *  the Region is empty, then the result is null.
     */
    public Element parentElement() {
        if(start == end)
            return null;
        List<Node> before = parentChain(start.leftBound().getTextNode(), null);
        List<Node> first = parentChain(start.rightBound().getTextNode(), null);
        List<Node> last = parentChain(end.leftBound().getTextNode(), null);
        List<Node> after = parentChain(end.rightBound().getTextNode(), null);
        Element common = null;
        while(!first.isEmpty() && !last.isEmpty() &&
              first.get(0) == last.get(0) &&
              ((!before.isEmpty() && first.get(0) == before.get(0)) ||
               (!after.isEmpty() && first.get(0) == after.get(0)) ||
               (before.isEmpty() && after.isEmpty()))) {
            if(first.get(0) instanceof Element)
                common = (Element)first.get(0);
            first.remove(0);
            if(!before.isEmpty())
                before.remove(0);
            last.remove(0);
            if(!after.isEmpty())
                after.remove(0);

        }
        return common;
    }

    /**
     *  Return a list of nodes starting with the root and ending with
     *  the argument. If within is non-null, then the list starts with
     *  a child of within instead of with the root.
     */
    static List<Node> parentChain(final Node child, final Node within) {
        List<Node> result = new LinkedList<Node>();
        Node n = child;
        while(n != null && n != within) {
            result.add(0, n);
            n = n.parent();
        }
        return result;
    }

    /**
     *  This helper returns a Region starting at start/offset if text
     *  matches the text in that region. (Whitespace matches
     *  whitespace, any other characters must match exactly.)
     *
     *  <p>Return null if text does not match.
     */
    static Region regionOrNull(final String text,
                               final TextNode start,
                               final int offset) {
        int tp = 0;
        int np = offset;
        TextNode end = start;
        boolean ts = false;
        boolean ns = false;
        while(tp < text.length()) {
            int wp = tp;
            while(np >= end.getWholeText().length()) {
                NeighbouringTextNode next =
                    NeighbouringTextNode.next(end, null);
                end = next.textNode;
                if(next.hasSeenBlockElement)
                    ns = true;
                np = 0;
                if(end == null)
                    return null;
            }
            if(ts) {
                int c = end.getWholeText().charAt(np);
                if(Character.isSpaceChar(c) || Character.isWhitespace(c)) {
                    ns = true;
                    np++;
                } else if(ns) {
                    tp++;
                } else {
                    return null;
                }
            } else if(text.charAt(tp) == end.getWholeText().charAt(np)) {
                np++;
                tp++;
            } else if(end.getWholeText().charAt(np) == '\u00AD') {
                np++;
            } else {
                return null;
            }
            if(tp > wp && tp < text.length()) {
                int st = text.charAt(tp);
                boolean ws = ts;
                ts = Character.isSpaceChar(st) || Character.isWhitespace(st);
                if(ws && !ts && !ns)
                    return null;
            }
        }
        return new Region(new Point(start, offset), new Point(end, np));
    }

    static class NeighbouringTextNode {
        public final boolean hasSeenBlockElement;
        public final boolean hasSeenElement;
        public final TextNode textNode;

        private NeighbouringTextNode(final Node n, boolean s, boolean b) {
            hasSeenElement = s;
            hasSeenBlockElement = b;
            textNode = (TextNode)n;
        }

        private static boolean isElement(final boolean was, final Node n) {
            return was || (n != null && n instanceof Element);
        }

        private static boolean isBlockElement(final boolean was, final Node n) {
            return was ||
                ((n != null) &&
                 (n instanceof Element) &&
                 (((Element)n).isBlock() ||
                  ((Element)n).tag().getName().equals("br")));
        }

        public static NeighbouringTextNode next(final Node startingPoint,
                                                final Node within) {
            Node next = startingPoint;
            boolean element = false;
            boolean block = false;
            do {
                element = isElement(element, next);
                block = isBlockElement(block, next);
                if(next.childNodeSize() > 0) {
                    next = next.childNode(0);
                } else if(next.nextSibling() != null) {
                    next = next.nextSibling();
                } else {
                    next = next.parent();
                    element = isElement(element, next);
                    block = isBlockElement(block, next);
                    while(next != null && next != within &&
                          next.nextSibling() == null) {
                        next = next.parent();
                        element = isElement(element, next);
                        block = isBlockElement(block, next);
                    }
                    if(next == within)
                        next = null;
                    if(next != null)
                        next = next.nextSibling();
                }
            } while(next != null && !(next instanceof TextNode));
            return new NeighbouringTextNode(next, element, block);
        }

        public static NeighbouringTextNode previous(final Node startingPoint,
                                                    final Node within) {
            Node previous = startingPoint;
            boolean element = false;
            boolean block = false;
            do {
                element = isElement(element, previous);
                block = isBlockElement(block, previous);
                if(previous.childNodeSize() > 0) {
                    previous = previous.childNode(previous.childNodeSize() - 1);
                } else if(previous.previousSibling() != null) {
                    previous = previous.previousSibling();
                } else {
                    previous = previous.parent();
                    element = isElement(element, previous);
                    block = isBlockElement(block, previous);
                    while(previous != null &&
                          previous != within &&
                          previous.previousSibling() == null) {
                        previous = previous.parent();
                        element = isElement(element, previous);
                        block = isBlockElement(block, previous);
                    }
                    if(previous == within)
                        previous = null;
                    if(previous != null)
                        previous = previous.previousSibling();
                }
            } while(previous != null && !(previous instanceof TextNode));
            return new NeighbouringTextNode(previous, element, block);
        }
    }


    /**
     *  Return the first region within the supplied Node at/after
     *  start/offset containing text, or null if no such region
     *  exists. (Whitespace matches whitespace, any other characters
     *  must match exactly.)
     *
     *  <p>Return null if text does not match.
     */
    static Region findNext(final String text,
                           TextNode start, int offset, Node within) {
        TextNode at = start;
        int pos = offset;
        while(at != null) {
            Region result = regionOrNull(text, at, pos);
            if(result != null)
                return result;

            pos = at.getWholeText().indexOf(text.charAt(0), pos+1);
            if(pos < 0) {
                pos = 0;
                at = NeighbouringTextNode.next(at, within).textNode;
            }
        }
        return null;
    }


    /**
     *  Return the first region within the specified Node that
     *  contains text, or null if no such region exists. (Whitespace
     *  matches whitespace, any other characters must match exactly.)
     *
     *  <p>Return null if text does not match.
     */
    static public Region find(final String text,
                              final Node start, final Node within) {
        if(start instanceof TextNode)
            return findNext(text, (TextNode)start, 0, within);
        return findNext(text,
                        NeighbouringTextNode.next(start, within).textNode, 0,
                        within);
    }

    /**
     *  Return the next region after this Region and within the
     *  specified Node that contains text, or null if no such region
     *  exists. (Whitespace matches whitespace, any other characters
     *  must match exactly.)
     */
    public Region findNext(final String text, final Node within) {
        return getEnd().findNext(text, within);
    }

    /**
     Test if this region is blank -- that is, empty or only whitespace (including newlines).
     @return true if this region is empty or only whitespace, false if it contains any text content.
     */

    public boolean isBlank() {
        return StringUtil.isBlank(getText());
    }

    /**
     *  Removes whitespace from the edges of the Region, leaving the
     *  document unchanged and the Region possibly shorter.
     */

    public Region trim() {
        boolean progress = true;
        String whole = start.getTextNode().getWholeText();
        while(progress && !start.equals(end)) {
            progress = false;
            if(start.getOffset() >= whole.length()) {
                start = start.rightBound();
                whole = start.getTextNode().getWholeText();
            }
            if(!start.equals(end) &&
               start.getOffset() < whole.length() &&
               isWhitespace(whole.codePointAt(start.getOffset()))) {
                progress = true;
                start = new Point(start.getTextNode(), start.getOffset()+1);
            }
        }
        progress = true;
        whole = end.getTextNode().getWholeText();
        while(progress && !start.equals(end)) {
            progress = false;
            if(end.getOffset() == 0) {
                end = end.leftBound();
                whole = end.getTextNode().getWholeText();
            }
            if(!start.equals(end) &&
               end.getOffset() > 0 &&
               isWhitespace(whole.codePointAt(end.getOffset()-1))) {
                progress = true;
                end = new Point(end.getTextNode(), end.getOffset()-1);
            }
        }
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Region)) return false;

        Region other = (Region)o;
        return start == other.start && end == other.end;
    }

    @Override
    public int hashCode() {
        return start.hashCode() * 31 + end.hashCode();
    }

    @Override
    public String toString() {
        return getText();
    }
}
