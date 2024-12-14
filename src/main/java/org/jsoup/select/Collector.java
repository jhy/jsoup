package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jspecify.annotations.Nullable;

import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        return stream(eval, root).collect(Collectors.toCollection(Elements::new));
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
     Finds the first Element that matches the Evaluator that descends from the root, and stops the query once that first
     match is found.
     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @return the first match; {@code null} if none
     */
    public static @Nullable Element findFirst(Evaluator eval, Element root) {
        return stream(eval, root).findFirst().orElse(null);
    }
}
