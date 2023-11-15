package org.jsoup.nodes;

import org.jsoup.helper.Validate;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 Iterate through a Node and its tree of descendants, in document order, and returns nodes of the specified type. This
 iterator supports structural changes to the tree during the traversal, such as {@link Node#remove()},
 {@link Node#replaceWith(Node)}, {@link Node#wrap(String)}, etc.
 <p>See also the {@link org.jsoup.select.NodeTraversor NodeTraversor} if {@code head} and {@code tail} callbacks are
 desired for each node.</p>
 @since 1.17.1
 */
public class NodeIterator<T extends Node> implements Iterator<T> {
    private Node root;                      // root / starting node
    private @Nullable T next;               // the next node to return
    private Node current;                   // the current (last emitted) node
    private Node previous;                  // the previously emitted node; used to recover from structural changes
    private @Nullable Node currentParent;   // the current node's parent; used to detect structural changes
    private final Class<T> type;            // the desired node class type

    /**
     Create a NoteIterator that will iterate the supplied node, and all of its descendants. The returned {@link #next}
     type will be filtered to the input type.
     * @param start initial node
     * @param type node type to filter for
     */
    public NodeIterator(Node start, Class<T> type) {
        Validate.notNull(start);
        Validate.notNull(type);
        this.type = type;

        restart(start);
    }

    /**
     Create a NoteIterator that will iterate the supplied node, and all of its descendants. All node types will be
     returned.
     * @param start initial node
     */
    public static NodeIterator<Node> from(Node start) {
        return new NodeIterator<>(start, Node.class);
    }

    /**
     Restart this Iterator from the specified start node. Will act as if it were newly constructed. Useful for e.g. to
     save some GC if the iterator is used in a tight loop.
     * @param start the new start node.
     */
    public void restart(Node start) {
        if (type.isInstance(start))
            //noinspection unchecked
            next = (T) start; // first next() will be the start node

        root = previous = current = start;
        currentParent = current.parent();
    }

    @Override public boolean hasNext() {
        maybeFindNext();
        return next != null;
    }

    @Override public T next() {
        maybeFindNext();
        if (next == null) throw new NoSuchElementException();

        T result = next;
        previous = current;
        current = next;
        currentParent = current.parent();
        next = null;
        return result;
    }

    /**
     If next is not null, looks for and sets next. If next is null after this, we have reached the end.
     */
    private void maybeFindNext() {
        if (next != null) return;

        //  change detected (removed or replaced), redo from previous
        if (currentParent != null && !current.hasParent())
            current = previous;

        next = findNextNode();
    }

    private @Nullable T findNextNode() {
        Node node = current;
        while (true) {
            if (node.childNodeSize() > 0)
                node = node.childNode(0);                   // descend children
            else if (root.equals(node))
                node = null;                                // complete when all children of root are fully visited
            else if (node.nextSibling() != null)
                node = node.nextSibling();                  // in a descendant with no more children; traverse
            else {
                while (true) {
                    node = node.parent();                   // pop out of descendants
                    if (node == null || root.equals(node))
                        return null;                        // got back to root; complete
                    if (node.nextSibling() != null) {
                        node = node.nextSibling();          // traverse
                        break;
                    }
                }
            }
            if (node == null)
                return null;                                // reached the end

            if (type.isInstance(node))
                //noinspection unchecked
                return (T) node;
        }
    }

    @Override public void remove() {
        current.remove();
    }
}
