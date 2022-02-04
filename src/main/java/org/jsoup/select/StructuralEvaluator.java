package org.jsoup.select;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

/**
 * Base structural evaluator.
 */
abstract class StructuralEvaluator extends Evaluator {
    Evaluator evaluator;

    static class Root extends Evaluator {
        @Override
        public boolean matches(Element root, Element element) {
            return root == element;
        }
    }

    static class Has extends StructuralEvaluator {
        final Collector.FirstFinder finder;

        public Has(Evaluator evaluator) {
            this.evaluator = evaluator;
            finder = new Collector.FirstFinder(evaluator);
        }

        @Override
        public boolean matches(Element root, Element element) {
            // for :has, we only want to match children (or below), not the input element. And we want to minimize GCs
            for (int i = 0; i < element.childNodeSize(); i++) {
                Node node = element.childNode(i);
                if (node instanceof Element) {
                    Element match = finder.find(element, (Element) node);
                    if (match != null)
                        return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format(":has(%s)", evaluator);
        }
    }

    static class Not extends StructuralEvaluator {
        public Not(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public boolean matches(Element root, Element node) {
            return !evaluator.matches(root, node);
        }

        @Override
        public String toString() {
            return String.format(":not(%s)", evaluator);
        }
    }

    static class Parent extends StructuralEvaluator {
        public Parent(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (root == element)
                return false;

            Element parent = element.parent();
            while (parent != null) {
                if (evaluator.matches(root, parent))
                    return true;
                if (parent == root)
                    break;
                parent = parent.parent();
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s ", evaluator);
        }
    }

    static class ImmediateParent extends StructuralEvaluator {
        public ImmediateParent(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (root == element)
                return false;

            Element parent = element.parent();
            return parent != null && evaluator.matches(root, parent);
        }

        @Override
        public String toString() {
            return String.format("%s > ", evaluator);
        }
    }

    static class PreviousSibling extends StructuralEvaluator {
        public PreviousSibling(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (root == element)
                return false;

            Element prev = element.previousElementSibling();

            while (prev != null) {
                if (evaluator.matches(root, prev))
                    return true;

                prev = prev.previousElementSibling();
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("%s ~ ", evaluator);
        }
    }

    static class ImmediatePreviousSibling extends StructuralEvaluator {
        public ImmediatePreviousSibling(Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (root == element)
                return false;

            Element prev = element.previousElementSibling();
            return prev != null && evaluator.matches(root, prev);
        }

        @Override
        public String toString() {
            return String.format("%s + ", evaluator);
        }
    }
}
