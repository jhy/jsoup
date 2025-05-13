package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 Node visitor interface, used to walk the DOM, and visit each Node. Execute via {@link #traverse(Node)} or
 {@link Node#traverse(NodeVisitor)}. The traversal is depth-first.
 <p>
 This interface provides two methods, {@link #head} and {@link #tail}. The head method is called when a node is first
 seen, and the tail method when all that node's children have been visited. As an example, {@code head} can be used to
 emit a start tag for a node, and {@code tail} to create the end tag. The {@code tail} method defaults to a no-op, so
 the {@code head} method is the {@link FunctionalInterface}.
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
     <p>The node may be modified (e.g. {@link Node#attr(String)}, replaced {@link Node#replaceWith(Node)}) or removed
     {@link Node#remove()}. If it's {@code instanceOf Element}, you may cast it to an {@link Element} and access those
     methods.</p>

     @param node the node being visited.
     @param depth the depth of the node, relative to the root node. E.g., the root node has depth 0, and a child node
     of that will have depth 1.
     */
    void head(Node node, int depth);

    /**
     Callback for when a node is last visited, after all of its descendants have been visited.
     <p>This method has a default no-op implementation.</p>
     <p>Note that neither replacement with {@link Node#replaceWith(Node)} nor removal with {@link Node#remove()} is
     supported during {@code tail()}.

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
