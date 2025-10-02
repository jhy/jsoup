package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jspecify.annotations.Nullable;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toCollection;

/**
 * Collects a list of elements that match the supplied criteria.
 *
 * @author Jonathan Hedley
 */
public class Collector {

    private Collector() {}

    /**
     Build a list of elements, by visiting the root and every descendant of root, and testing it against the Evaluator.
     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @return list of matches; empty if none
     */
    public static Elements collect(Evaluator eval, Element root) {
        Stream<Element> stream = eval.wantsNodes() ?
            streamNodes(eval, root, Element.class) :
            stream(eval, root);
        Elements els = stream.collect(toCollection(Elements::new));
        eval.reset(); // drops any held memos
        return els;
    }

    /**
     Obtain a Stream of elements by visiting the root and every descendant of root and testing it against the evaluator.

     @param evaluator Evaluator to test elements against
     @param root root of tree to descend
     @return A {@link Stream} of matches
     @since 1.19.1
     */
    public static Stream<Element> stream(Evaluator evaluator, Element root) {
        evaluator.reset();
        return root.stream().filter(evaluator.asPredicate(root));
    }

    /**
     Obtain a Stream of nodes, of the specified type, by visiting the root and every descendant of root and testing it
     against the evaluator.

     @param evaluator Evaluator to test elements against
     @param root root of tree to descend
     @param type the type of node to collect (e.g. {@link Element}, {@link LeafNode}, {@link TextNode} etc)
     @param <T> the type of node to collect
     @return A {@link Stream} of matches
     @since 1.21.1
     */
    public static <T extends Node> Stream<T> streamNodes(Evaluator evaluator, Element root, Class<T> type) {
        evaluator.reset();
        return root.nodeStream(type).filter(evaluator.asNodePredicate(root));
    }

    /**
     Finds the first Element that matches the Evaluator that descends from the root, and stops the query once that first
     match is found.
     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @return the first match; {@code null} if none
     */
    public static @Nullable Element findFirst(Evaluator eval, Element root) {
        return stream(eval, root).findFirst().orElse(null);
    }

    /**
     Finds the first Node that matches the Evaluator that descends from the root, and stops the query once that first
     match is found.

     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @param type the type of node to collect (e.g. {@link Element}, {@link LeafNode}, {@link TextNode} etc)
     @return the first match; {@code null} if none
     @since 1.21.1
     */
    public static <T extends Node> @Nullable T findFirstNode(Evaluator eval, Element root, Class<T> type) {
        return streamNodes(eval, root, type).findFirst().orElse(null);
    }

    /**
     Build a list of nodes that match the supplied criteria, by visiting the root and every descendant of root, and
     testing it against the Evaluator.

     @param evaluator Evaluator to test elements against
     @param root root of tree to descend
     @param type the type of node to collect (e.g. {@link Element}, {@link LeafNode}, {@link TextNode} etc)
     @param <T> the type of node to collect
     @return list of matches; empty if none
     */
    public static <T extends Node> Nodes<T> collectNodes(Evaluator evaluator, Element root, Class<T> type) {
        return streamNodes(evaluator, root, type).collect(toCollection(Nodes::new));
    }
}
