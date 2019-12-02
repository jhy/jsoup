package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter.FilterResult;

public class TailFirstPreorderNodeTraversor implements NodeTraversor {
    /**
     * Start a depth-first traverse of the root and all of its descendants.
     * @param visitor Node visitor.
     * @param root the root node point to traverse.
     */
	public void traverse(NodeVisitor visitor, Node root) {
        Node node = root;
        int depth = 0;

        while (node != null) {
            visitor.tail(node, depth);
            if (node.childNodeSize() > 0) {
                node = node.childNode(node.childNodeSize()-1);
                depth++;
            } else {
                while (node.previousSibling() == null && depth > 0) {
                    visitor.head(node, depth);
                    node = node.parentNode();
                    depth--;
                }
                visitor.head(node, depth);
                if (node == root)
                    break;
                node = node.previousSibling();
            }
        }
	}

    /**
     * Start a depth-first traverse of all elements.
     * @param visitor Node visitor.
     * @param elements Elements to filter.
     */
	public void traverse(NodeVisitor visitor, Elements elements) {
        Validate.notNull(visitor);
        Validate.notNull(elements);
        for (int index = elements.size()-1; index > -1  ; index--)
            traverse(visitor, elements.get(index));
    }

    /**
     * Start a depth-first filtering of the root and all of its descendants.
     * @param filter Node visitor.
     * @param root the root node point to traverse.
     * @return The filter result of the root node, or {@link FilterResult#STOP}.
     */
	public FilterResult filter(NodeFilter filter, Node root) {
        Node node = root;
        int depth = 0;

        while (node != null) {
            FilterResult result = filter.tail(node, depth);
            if (result == FilterResult.STOP)
                return result;
            // Descend into child nodes:
            if (result == FilterResult.CONTINUE && node.childNodeSize() > 0) {
                node = node.childNode(node.childNodeSize()-1);
                ++depth;
                continue;
            }
            // No siblings, move upwards:
            while (node.previousSibling() == null && depth > 0) {
                // 'tail' current node:
                if (result == FilterResult.CONTINUE || result == FilterResult.SKIP_CHILDREN) {
                    result = filter.head(node, depth);
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
                result = filter.head(node, depth);
                if (result == FilterResult.STOP)
                    return result;
            }
            if (node == root)
                return result;
            Node prev = node; // In case we need to remove it below.
            node = node.previousSibling();
            if (result == FilterResult.REMOVE)
                prev.remove(); // Remove AFTER finding sibling.
        }
        // root == null?
        return FilterResult.CONTINUE;
	}

    /**
     * Start a depth-first filtering of all elements.
     * @param filter Node filter.
     * @param elements Elements to filter.
     */
	public void filter(NodeFilter filter, Elements elements) {
        Validate.notNull(filter);
        Validate.notNull(elements);
        for (int index = elements.size()-1; index > -1  ; index--)
            if (filter(filter, elements.get(index)) == FilterResult.STOP)
                break;
	}
}
