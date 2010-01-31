package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

import java.util.List;

/**
 * Collects a list of elements that match the supplied criteria.
 *
 * @author Jonathan Hedley
 */
public class Collector {

    /**
     Build a list of elements, by visiting root and every descendant of root, and testing it against the evaluator.
     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @return list of matches; empty if none
     */
    public static Elements collect (Evaluator eval, Element root) {
        Elements elements = new Elements();
        accumulateMatches(eval, elements, root);
        return elements;
    }

    private static void accumulateMatches(Evaluator eval, List<Element> elements, Element element) {
        if (eval.matches(element))
            elements.add(element);
        for (Element child: element.children())
            accumulateMatches(eval, elements, child);
    }
}
