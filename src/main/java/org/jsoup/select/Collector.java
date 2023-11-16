package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Collects a list of elements that match the supplied criteria.
 *
 * @author Jonathan Hedley
 */
public class Collector {

    private Collector() {}

    /**
     Build a list of elements, by visiting root and every descendant of root, and testing it against the evaluator.
     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @return list of matches; empty if none
     */
    public static Elements collect (Evaluator eval, Element root) {
        eval.reset();

        return root.stream()
            .filter(eval.asPredicate(root))
            .collect(Collectors.toCollection(Elements::new));
    }

    /**
     Finds the first Element that matches the Evaluator that descends from the root, and stops the query once that first
     match is found.
     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @return the first match; {@code null} if none
     */
    public static @Nullable Element findFirst(Evaluator eval, Element root) {
        eval.reset();

        Optional<Element> first = root.stream().filter(eval.asPredicate(root)).findFirst();
        return first.orElse(null);
    }
}
