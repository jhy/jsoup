package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter.FilterResult;

/**
 * Depth-first node traversor. Use to iterate through all nodes under and including the specified root node.
 * <p>
 * This implementation does not use recursion, so a deep DOM does not risk blowing the stack.
 * </p>
 */
public class NodeTraversor {

    /**
     * Start a depth-first traverse of the root and all of its descendants.
     * @param visitor Node visitor.
     * @param root the root node point to traverse.
     */
    public static void traverse(NodeVisitor visitor, Node root) {
        Validate.notNull(visitor);
        Validate.notNull(root);
        Node node = root;
        int depth = 0;
        while (node != null) {
            // remember parent to find nodes that get replaced in .head
            Node parent = node.parentNode();
            int origSize = parent != null ? parent.childNodeSize() : 0;
            Node next = node.nextSibling();
            // visit current node
            visitor.head(node, depth);
            if (parent != null && !node.hasParent()) {
                // removed or replaced
                if (origSize == parent.childNodeSize()) {
                    // replaced
                    // replace ditches parent but keeps sibling index
                    node = parent.childNode(node.siblingIndex());
                } else {
                    // removed
                    node = next;
                    if (node == null) {
                        // last one, go up
                        node = parent;
                        depth--;
                    }
                    // don't tail removed
                    continue;
                }
            }
            if (node.childNodeSize() > 0) {
                // descend
                node = node.childNode(0);
                depth++;
            } else {
                while (true) {
                    // as depth > 0, will have parent
                    assert node != null;
                    if (!(node.nextSibling() == null && depth > 0))
                        break;
                    // when no more siblings, ascend
                    visitor.tail(node, depth);
                    node = node.parentNode();
                    depth--;
                }
                visitor.tail(node, depth);
                if (node == root)
                    break;
                node = node.nextSibling();
            }
        }
    }

    /**
     * Start a depth-first traverse of all elements.
     * @param visitor Node visitor.
     * @param elements Elements to filter.
     */
    public static void traverse(NodeVisitor visitor, Elements elements) {
        Validate.notNull(visitor);
        Validate.notNull(elements);
        for (Element el : elements) traverse(visitor, el);
    }

    /**
     * Start a depth-first filtering of the root and all of its descendants.
     * @param filter Node visitor.
     * @param root the root node point to traverse.
     * @return The filter result of the root node, or {@link FilterResult#STOP}.
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
                // depth > 0, so has parent
                assert node != null;
                if (!(node.nextSibling() == null && depth > 0))
                    break;
                // 'tail' current node:
                if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                    result = filter.tail(node, depth);
                    if (result == FilterResult.STOP)
                        return result;
                }
                // In case we need to remove it below.
                Node prev = node;
                node = node.parentNode();
                depth--;
                if (result == FilterResult.REMOVE)
                    // Remove AFTER finding parent.
                    prev.remove();
                // Parent was not pruned.
                result = FilterResult.CONTINUE;
            }
            // 'tail' current node, then proceed with siblings:
            if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                result = filter.tail(node, depth);
                if (result == FilterResult.STOP)
                    return result;
            }
            if (node == root)
                return result;
            // In case we need to remove it below.
            Node prev = node;
            node = node.nextSibling();
            if (result == FilterResult.REMOVE)
                // Remove AFTER finding sibling.
                prev.remove();
        }
        // root == null?
        return FilterResult.CONTINUE;
    }

    /**
     * Start a depth-first filtering of all elements.
     * @param filter Node filter.
     * @param elements Elements to filter.
     */
    public static void filter(NodeFilter filter, Elements elements) {
        Validate.notNull(filter);
        Validate.notNull(elements);
        for (Element el : elements) if (filter(filter, el) == FilterResult.STOP)
            break;
    }
}
