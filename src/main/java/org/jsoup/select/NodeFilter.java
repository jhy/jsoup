package org.jsoup.select;

import org.jsoup.nodes.Node;

/**
 A controllable Node visitor interface. Execute via {@link #traverse(Node)}.
 <p>
 This interface provides two methods, {@code head} and {@code tail}. The head method is called when a node is first seen,
 and the tail method when all that node's children have been visited.
 </p>
 <p>
 For each visited node, the resulting action may be:
 <ul>
 <li>continue ({@link FilterResult#CONTINUE}),</li>
 <li>skip all children ({@link FilterResult#SKIP_CHILDREN}),</li>
 <li>skip node entirely ({@link FilterResult#SKIP_ENTIRELY}),</li>
 <li>remove the subtree ({@link FilterResult#REMOVE}),</li>
 <li>interrupt the iteration and return ({@link FilterResult#STOP}).</li>
 </ul>
 The difference between {@link FilterResult#SKIP_CHILDREN} and {@link FilterResult#SKIP_ENTIRELY} is that the first
 will invoke {@link NodeFilter#tail(Node, int)} on the node, while the latter will not.
 Within {@link NodeFilter#tail(Node, int)}, both are equivalent to {@link FilterResult#CONTINUE}.
 </p>
 */
public interface NodeFilter {
    /**
     Traversal action.
     */
    enum FilterResult {
        /** Continue processing the tree */
        CONTINUE,
        /** Skip the child nodes, but do call {@link NodeFilter#tail(Node, int)} next. */
        SKIP_CHILDREN,
        /** Skip the subtree, and do not call {@link NodeFilter#tail(Node, int)}. */
        SKIP_ENTIRELY,
        /** Remove the node and its children */
        REMOVE,
        /** Stop processing */
        STOP
    }

    /**
     * Callback for when a node is first visited.
     * @param node the node being visited.
     * @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node of that will have depth 1.
     * @return Traversal action
     */
    FilterResult head(Node node, int depth);

    /**
     * Callback for when a node is last visited, after all of its descendants have been visited.
     * <p>This method has a default implementation to return {@link FilterResult#CONTINUE}.</p>
     * @param node the node being visited.
     * @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node of that will have depth 1.
     * @return Traversal action
     */
    default FilterResult tail(Node node, int depth) {
        return FilterResult.CONTINUE;
    }

    /**
     Run a depth-first controlled traverse of the root and all of its descendants.
     @param root the initial node point to traverse.
     @since 1.21.1
     */
    default void traverse(Node root) {
        NodeTraversor.filter(this, root);
    }
}
