package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 Node visitor interface, used to walk the DOM and visit each node. Execute via {@link #traverse(Node)} or
 {@link Node#traverse(NodeVisitor)}. The traversal is depth-first.
 <p>
 This interface provides two methods, {@link #head} and {@link #tail}. The head method is called when a node is first
 seen, and the tail method when all that node's children have been visited. As an example, {@code head} can be used to
 emit a start tag for a node, and {@code tail} to emit the end tag. The {@code tail} method defaults to a no-op, so
 this interface can be used as a {@link FunctionalInterface}, with {@code head} as its single abstract method.
 </p>
 <p><b>Example:</b></p>
 <pre><code>
 doc.body().traverse((node, depth) -&gt; {
     switch (node) {
         case Element el     -&gt; print(el.tag() + ": " + el.ownText());
         case DataNode data  -&gt; print("Data: " + data.getWholeData());
         default             -&gt; print(node.nodeName() + " at depth " + depth);
     }
 });
 </code></pre>
 */
@FunctionalInterface
public interface NodeVisitor {
    /**
     Callback for when a node is first visited.
     <p>The node may be modified (for example via {@link Node#attr(String)}), removed with
     {@link Node#remove()}, or replaced with {@link Node#replaceWith(Node)}. If the node is an
     {@link Element}, you may cast it and access those methods.</p>
     <p>Traversal uses a forward cursor. After {@code head()} completes:</p>
     <ul>
     <li>If the current node is still attached, traversal continues into its current children and then its following
     siblings. Nodes inserted before the current node are not visited.</li>
     <li>If the current node was detached and another node now occupies its former sibling position, the node now at
     that position is not passed to {@code head()} again. Traversal continues from there: its children are visited,
     then the node is passed to {@link #tail(Node, int)}, then later siblings are visited.</li>
     <li>If the current node was detached and no node occupies its former sibling position, the current node is not
     passed to {@code tail()}, and traversal resumes at the node that originally followed it.</li>
     </ul>
     <p>Traversal never advances outside the original root subtree. If the traversal root is detached during
     {@code head()}, traversal stops at the original root boundary.</p>

     @param node the node being visited.
     @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     of that will have depth 1.
     */
    void head(Node node, int depth);

    /**
     Callback for when a node is last visited, after all of its descendants have been visited.
     <p>This method defaults to a no-op.</p>
     <p>The node passed to {@code tail()} is the node at the current traversal position when the subtree completes.
     If {@code head()} replaced the original node, this may be the replacement node instead.</p>
     <p>Structural changes to the current node are not supported during {@code tail()}.</p>

     @param node the node being visited.
     @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     of that will have depth 1.
     */
    default void tail(Node node, int depth) {
        // no-op by default, to allow just specifying the head() method
    }

    /**
     Run a depth-first traverse of the root and all of its descendants.
     @param root the initial node point to traverse.
     @since 1.21.1
     */
    default void traverse(Node root) {
        NodeTraversor.traverse(this, root);
    }
}
