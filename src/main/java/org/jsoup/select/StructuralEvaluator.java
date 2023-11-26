package org.jsoup.select;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.NodeIterator;

import java.util.ArrayList;
import java.util.IdentityHashMap;

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
        // not using computeIfAbsent, as the lambda impl requires a new Supplier closure object on every hit: tons of GC
        IdentityHashMap<Element, IdentityHashMap<Element, Boolean>> rootMemo = threadMemo.get();
        IdentityHashMap<Element, Boolean> memo = rootMemo.get(root);
        if (memo == null) {
            memo = new IdentityHashMap<>();
            rootMemo.put(root, memo);
        }
        Boolean matches = memo.get(element);
        if (matches == null) {
            matches = evaluator.matches(root, element);
            memo.put(element, matches);
        }
        return matches;
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
        final NodeIterator<Element> it = new NodeIterator<>(new Element("html"), Element.class);
        // the element here is just a placeholder so this can be final - gets set in restart()

        public Has(Evaluator evaluator) {
            super(evaluator);
        }

        @Override public boolean matches(Element root, Element element) {
            // for :has, we only want to match children (or below), not the input element. And we want to minimize GCs
            it.restart(element);
            while (it.hasNext()) {
                Element el = it.next();
                if (el == element) continue; // don't match self, only descendants
                if (evaluator.matches(element, el))
                    return true;
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

            Element parent = element.parent();
            while (parent != null) {
                if (memoMatches(root, parent))
                    return true;
                if (parent == root)
                    break;
                parent = parent.parent();
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
     @deprecated replaced by {@link  ImmediateParentRun}
     */
    @Deprecated
    static class ImmediateParent extends StructuralEvaluator {
        public ImmediateParent(Evaluator evaluator) {
            super(evaluator);
        }

        @Override
        public boolean matches(Element root, Element element) {
            if (root == element)
                return false;

            Element parent = element.parent();
            return parent != null && memoMatches(root, parent);
        }

        @Override protected int cost() {
            return 1 + evaluator.cost();
        }

        @Override
        public String toString() {
            return String.format("%s > ", evaluator);
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

            Element sibling = element.firstElementSibling();
            while (sibling != null) {
                if (sibling == element) break;
                if (memoMatches(root, sibling)) return true;
                sibling = sibling.nextElementSibling();
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
