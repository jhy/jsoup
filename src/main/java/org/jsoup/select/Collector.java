package org.jsoup.select;

import org.jsoup.nodes.Element;

import java.util.List;

/**
 * Collects a list of elements that match the supplied criteria.
 *
 * @author Jonathan Hedley
 */
public class Collector {
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
