package org.jsoup.select;

import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects a list of elements that match the supplied criteria.
 *
 * @author Jonathan Hedley
 */
public class Collector {
    public static List<Element> collect (Evaluator eval, Element root) {
        List<Element> elements = new ArrayList<Element>();
        accumulateMatches(eval, elements, root);
        return Collections.unmodifiableList(elements);
    }

    private static void accumulateMatches(Evaluator eval, List<Element> elements, Element element) {
        if (eval.matches(element))
            elements.add(element);
        for (Element child: element.children())
            accumulateMatches(eval, elements, child);
    }
}
