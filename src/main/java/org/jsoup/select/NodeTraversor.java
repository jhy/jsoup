package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter.FilterResult;

/**
 A depth-first node traversor. Use to walk through all nodes under and including the specified root node, in document
 order. The {@link NodeVisitor#head(Node, int)} and {@link NodeVisitor#tail(Node, int)} methods will be called for
 each node.
 <p>During the <code>head()</code> visit, DOM structural changes around the node currently being visited are
 supported, including {@link Node#replaceWith(Node)} and {@link Node#remove()}. See
 {@link NodeVisitor#head(Node, int) head()} for the traversal contract after mutation. Other non-structural node
 changes are also supported.</p>
 <p>DOM structural changes to the current node are not supported during the <code>tail()</code> visit.</p>
 */
public class NodeTraversor {
    // cursor state
    private static final byte VisitHead = 0;
    private static final byte AfterHead = 1;
    private static final byte VisitTail = 2;

    /**
     Run a depth-first traverse of the root and all of its descendants.
     @param visitor Node visitor.
     @param root the initial node point to traverse.
     @see NodeVisitor#traverse(Node root)
     */
    public static void traverse(NodeVisitor visitor, Node root) {
        Validate.notNull(visitor);
        Validate.notNull(root);
        Node node = root;
        final Node rootNext = root.nextSibling(); // don't traverse siblings beyond the original root
        int depth = 0;
        byte state = VisitHead;

        while (true) {
            if (state == VisitHead) {
                // snapshot the current cursor position so we can recover if head() structurally changes it:
                Node parent   = node.parentNode();
                Node next     = node.nextSibling();
                int  sibIndex = parent != null ? node.siblingIndex() : 0;

                visitor.head(node, depth);

                // any structural changes?
                if (parent != null && node.parentNode() != parent) { // removed / replaced / moved
                    Node occupant = sibIndex < parent.childNodeSize() ? parent.childNode(sibIndex) : null;
                    // ^^ the node now at this node's former position
                    Node boundary = depth == 0 ? rootNext : next;   // don't advance beyond this node when resuming
                    if (occupant != null && occupant != boundary) {
                        node = occupant;
                        state = AfterHead;                          // continue from that slot without re-heading it
                    } else if (depth == 0) {                        // root detached or replaced
                        break;
                    } else if (next != null && next.parentNode() == parent) {
                        node = next;                                // old slot is empty or shifted to the original next, visit
                    } else {                                        // removed last child; tail the parent next
                        node = parent;
                        depth--;
                        state = VisitTail;
                    }
                } else {
                    state = AfterHead;
                }
                continue;                                           // next loop handles the updated node/state
            }

            if (state == AfterHead && node.childNodeSize() > 0) { // descend into current children
                node = node.childNode(0);
                depth++;
                state = VisitHead;
                continue;
            }

            visitor.tail(node, depth);

            Node next = node.nextSibling();
            if (depth == 0) {
                if (next == null || next == rootNext) break; // done with the original root range
                node = next;
                state = VisitHead;
            } else if (next != null) { // traverse siblings
                node = next;
                state = VisitHead;
            } else {                // no siblings left, ascend
                node = node.parentNode();
                depth--;
                state = VisitTail;
            }
        }
    }

    /**
     Run a depth-first traversal of each Element.
     @param visitor Node visitor.
     @param elements Elements to traverse.
     */
    public static void traverse(NodeVisitor visitor, Elements elements) {
        Validate.notNull(visitor);
        Validate.notNull(elements);
        for (Element el : elements)
            traverse(visitor, el);
    }

    /**
     Run a depth-first controllable traversal of the root and all of its descendants.
     @param filter NodeFilter visitor.
     @param root the root node point to traverse.
     @return The filter result of the root node, or {@link FilterResult#STOP}.

     @see NodeFilter
     */
    public static FilterResult filter(NodeFilter filter, Node root) {
        Node node = root;
        int depth = 0;

        while (node != null) {
            FilterResult result = filter.head(node, depth);
            if (result == FilterResult.STOP)
                return result;
            // Descend into child nodes:
            if (result == FilterResult.CONTINUE && node.childNodeSize() > 0) {
                node = node.childNode(0);
                ++depth;
                continue;
            }
            // No siblings, move upwards:
            while (true) {
                assert node != null; // depth > 0, so has parent
                if (!(node.nextSibling() == null && depth > 0)) break;
                // 'tail' current node:
                if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                    result = filter.tail(node, depth);
                    if (result == FilterResult.STOP)
                        return result;
                }
                Node prev = node; // In case we need to remove it below.
                node = node.parentNode();
                depth--;
                if (result == FilterResult.REMOVE)
                    prev.remove(); // Remove AFTER finding parent.
                result = FilterResult.CONTINUE; // Parent was not pruned.
            }
            // 'tail' current node, then proceed with siblings:
            if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                result = filter.tail(node, depth);
                if (result == FilterResult.STOP)
                    return result;
            }
            if (node == root)
                return result;
            Node prev = node; // In case we need to remove it below.
            node = node.nextSibling();
            if (result == FilterResult.REMOVE)
                prev.remove(); // Remove AFTER finding sibling.
        }
        // root == null?
        return FilterResult.CONTINUE;
    }

    /**
     Run a depth-first controllable traversal of each Element.
     @param filter NodeFilter visitor.
     @see NodeFilter
     */
    public static void filter(NodeFilter filter, Elements elements) {
        Validate.notNull(filter);
        Validate.notNull(elements);
        for (Element el : elements)
            if (filter(filter, el) == FilterResult.STOP)
                break;
    }
}
