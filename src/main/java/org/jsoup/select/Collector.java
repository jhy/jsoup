package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Collects a list of elements that match the supplied criteria.
 *
 * @author Jonathan Hedley
 */
public class Collector {

    private Collector() {
    }

    /**
     Build a list of elements, by visiting root and every descendant of root, and testing it against the evaluator.
     @param eval Evaluator to test elements against
     @param root root of tree to descend
     @return list of matches; empty if none
     */
    public static Elements collect (Evaluator eval, Element root) {
        Elements elements = new Elements();
        new NodeTraversor(new Accumulator(root, elements, eval)).traverse(root);
        return elements;
    }

    private static class Accumulator implements NodeVisitor {
        private final Element root;
        private final Elements elements;
        private final Evaluator eval;

        Accumulator(Element root, Elements elements, Evaluator eval) {
            this.root = root;
            this.elements = elements;
            this.eval = eval;
        }

        public void head(Node node, int depth) {
            if (node instanceof Element) {
                Element el = (Element) node;
                if (eval.matches(root, el))
                    elements.add(el);
            }
        }

        public void tail(Node node, int depth) {
            // void
        }
    }
}
