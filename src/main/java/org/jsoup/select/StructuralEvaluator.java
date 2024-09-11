package org.jsoup.select;

import org.jsoup.internal.Functions;
import org.jsoup.internal.SoftPool;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.NodeIterator;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Base structural evaluator.
 */
abstract class StructuralEvaluator extends Evaluator {
    final Evaluator evaluator;

    public StructuralEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
    }

    // Memoize inner matches, to save repeated re-evaluations of parent, sibling etc.
    // root + element: Boolean matches. ThreadLocal in case the Evaluator is compiled then reused across multi threads
    final ThreadLocal<IdentityHashMap<Element, IdentityHashMap<Element, Boolean>>>
        threadMemo = ThreadLocal.withInitial(IdentityHashMap::new);

    boolean memoMatches(final Element root, final Element element) {
        Map<Element, IdentityHashMap<Element, Boolean>> rootMemo = threadMemo.get();
        Map<Element, Boolean> memo = rootMemo.computeIfAbsent(root, Functions.identityMapFunction());
        return memo.computeIfAbsent(element, key -> evaluator.matches(root, key));
    }

    @Override protected void reset() {
        threadMemo.get().clear();
        super.reset();
    }

    static class Root extends Evaluator {
        @Override
        public boolean matches(Element root, Element element) {
            return root == element;
        }

        @Override protected int cost() {
            return 1;
        }

        @Override public String toString() {
            return "";
        }
    }

    static class Has extends StructuralEvaluator {
        static final SoftPool<NodeIterator<Element>> ElementIterPool =
            new SoftPool<>(() -> new NodeIterator<>(new Element("html"), Element.class));
        // the element here is just a placeholder so this can be final - gets set in restart()

        private final boolean checkSiblings; // evaluating against siblings (or children)

        public Has(Evaluator evaluator) {
            super(evaluator);
            checkSiblings = evalWantsSiblings(evaluator);
        }

        @Override public boolean matches(Element root, Element element) {
            if (checkSiblings) { // evaluating against siblings
                for (Element sib = element.firstElementSibling(); sib != null; sib = sib.nextElementSibling()) {
                    if (sib != element && evaluator.matches(element, sib)) { // don't match against self
                        return true;
                    }
                }
            }
            // otherwise we only want to match children (or below), and not the input element. And we want to minimize GCs so reusing the Iterator obj
            NodeIterator<Element> it = ElementIterPool.borrow();
            it.restart(element);
            try {
                while (it.hasNext()) {
                    Element el = it.next();
                    if (el == element) continue; // don't match self, only descendants
                    if (evaluator.matches(element, el)) {
                        return true;
                    }
                }
            } finally {
                ElementIterPool.release(it);
            }
            return false;
        }

        /* Test if the :has sub-clause wants sibling elements (vs nested elements) - will be a Combining eval */
        private static boolean evalWantsSiblings(Evaluator eval) {
            if (eval instanceof CombiningEvaluator) {
                CombiningEvaluator ce = (CombiningEvaluator) eval;
                for (Evaluator innerEval : ce.evaluators) {
                    if (innerEval instanceof PreviousSibling || innerEval instanceof ImmediatePreviousSibling)
                        return true;
                }
            }
            return false;
        }

        @Override protected int cost() {
            return 10 * evaluator.cost();
        }

        @Override
        public String toString() {
            return String.format(":has(%s)", evaluator);
        }
    }

    /** Implements the :is(sub-query) pseudo-selector */
    static class Is extends StructuralEvaluator {
        public Is(Evaluator evaluator) {
            super(evaluator);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return evaluator.matches(root, element);
        }

        @Override protected int cost() {
            return 2 + evaluator.cost();
        }

        @Override
        public String toString() {
            return String.format(":is(%s)", evaluator);
        }
    }

    static class Not extends StructuralEvaluator {
        public Not(Evaluator evaluator) {
            super(evaluator);
        }

        @Override
        public boolean matches(Element root, Element element) {
            return !memoMatches(root, element);
        }

        @Override protected int cost() {
            return 2 + evaluator.cost();
        }

        @Override
        public String toString() {
            return String.format(":not(%s)", evaluator);
        }
    }

    static class Parent extends StructuralEvaluator {
        public Parent(Evaluator evaluator) {
            super(evaluator);
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (root == element)
                return false;

            for (Element parent = element.parent(); parent != null; parent = parent.parent()) {
                if (memoMatches(root, parent))
                    return true;
                if (parent == root)
                    break;
            }
            return false;
        }

        @Override protected int cost() {
            return 2 * evaluator.cost();
        }

        @Override
        public String toString() {
            return String.format("%s ", evaluator);
        }
    }

    /**
     Holds a list of evaluators for one > two > three immediate parent matches, and the final direct evaluator under
     test. To match, these are effectively ANDed together, starting from the last, matching up to the first.
     */
    static class ImmediateParentRun extends Evaluator {
        final ArrayList<Evaluator> evaluators = new ArrayList<>();
        int cost = 2;

        public ImmediateParentRun(Evaluator evaluator) {
            evaluators.add(evaluator);
            cost += evaluator.cost();
        }

        void add(Evaluator evaluator) {
            evaluators.add(evaluator);
            cost += evaluator.cost();
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (element == root)
                return false; // cannot match as the second eval (first parent test) would be above the root

            for (int i = evaluators.size() -1; i >= 0; --i) {
                if (element == null)
                    return false;
                Evaluator eval = evaluators.get(i);
                if (!eval.matches(root, element))
                    return false;
                element = element.parent();
            }
            return true;
        }

        @Override protected int cost() {
            return cost;
        }

        @Override
        public String toString() {
            return StringUtil.join(evaluators, " > ");
        }
    }

    static class PreviousSibling extends StructuralEvaluator {
        public PreviousSibling(Evaluator evaluator) {
            super(evaluator);
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (root == element) return false;

            for (Element sib = element.firstElementSibling(); sib != null; sib = sib.nextElementSibling()) {
                if (sib == element) break;
                if (memoMatches(root, sib)) return true;
            }

            return false;
        }

        @Override protected int cost() {
            return 3 * evaluator.cost();
        }

        @Override
        public String toString() {
            return String.format("%s ~ ", evaluator);
        }
    }

    static class ImmediatePreviousSibling extends StructuralEvaluator {
        public ImmediatePreviousSibling(Evaluator evaluator) {
            super(evaluator);
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (root == element)
                return false;

            Element prev = element.previousElementSibling();
            return prev != null && memoMatches(root, prev);
        }

        @Override protected int cost() {
            return 2 + evaluator.cost();
        }

        @Override
        public String toString() {
            return String.format("%s + ", evaluator);
        }
    }
}
