package org.jsoup.select;

import org.jsoup.nodes.Node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 A list of {@link Node} objects, with methods that act on every node in the list.
 <p>Methods that {@link #set(int, T) set}, {@link #remove(int) remove}, or {@link #replaceAll(UnaryOperator)
replace} Elements in the list will also act on the underlying {@link org.jsoup.nodes.Document DOM}.</p>
 */
public class Nodes<T extends Node> extends ArrayList<T> {
    // todo push applicable methods down from Elements to Nodes

    public Nodes() {
    }

    public Nodes(int initialCapacity) {
        super(initialCapacity);
    }

    public Nodes(Collection<T> nodes) {
        super(nodes);
    }

    public Nodes(List<T> nodes) {
        super(nodes);
    }

    @SafeVarargs
    public Nodes(T... nodes) {
        super(Arrays.asList(nodes));
    }

    /**
     * Creates a deep copy of these nodes.
     * @return a deep copy
     */
    @Override
    public Nodes<T> clone() {
        Nodes<T> clone = new Nodes<>(size());
        for (T node : this)
            clone.add((T) node.clone());
        return clone;
    }
}
