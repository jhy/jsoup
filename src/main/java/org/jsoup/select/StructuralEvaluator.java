package org.jsoup.select;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Base structural evaluator.
 */
abstract class StructuralEvaluator extends Evaluator {
    final Evaluator evaluator;
    boolean wantsNodes; // if the evaluator requested nodes, not just elements

    public StructuralEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
        wantsNodes = evaluator.wantsNodes();
    }

    @Override
    boolean wantsNodes() {
        return wantsNodes;
    }

    // Memoize inner matches, to save repeated re-evaluations of parent, sibling etc.
    // root + element: Boolean matches. ThreadLocal in case the Evaluator is compiled then reused across multi threads
    final ThreadLocal<Map<Node, Map<Node, Boolean>>> threadMemo = ThreadLocal.withInitial(WeakHashMap::new);

    /** Memoizes inner matches for the current query or relative traversal. */
    boolean memoMatches(final Element root, final Node node, @Nullable Element anchor) {
        Map<Node, Map<Node, Boolean>> rootMemo = threadMemo.get();
        Map<Node, Boolean> memo = rootMemo.computeIfAbsent(root, r -> new WeakHashMap<>());
        // avoid allocating a capturing function for every cached lookup
        Boolean matches = memo.get(node);
        if (matches == null) {
            matches = evaluator.matches(root, node, anchor);
            memo.put(node, matches);
        }
        return matches;
    }

    @Override protected void reset() {
        threadMemo.remove();
        evaluator.reset();
        super.reset();
    }

    @Override
    public boolean matches(Element root, Element element) {
        return evaluateMatch(root, element, null);
    }

    @Override
    boolean matches(Element root, LeafNode leafNode) {
        return evaluateMatch(root, leafNode, null);
    }

    @Override
    boolean matchesRelative(Element root, Node node, Element anchor) {
        return evaluateMatch(root, node, anchor);
    }

    abstract boolean evaluateMatch(Element root, Node node, @Nullable Element anchor);

    /**
     Anchors jsoup's leading-combinator queries to their query context.
     */
    static class QueryRoot extends Evaluator {
        @Override
        public boolean matches(Element root, Element element) {
            return (web ? documentElementOrSelf(root) : root) == element;
        }

        @Override
        protected int cost() {
            return 1;
        }

        @Override
        public String toString() {
            return ">";
        }
    }

    /**
     Anchors only the relative selector being traversed, independently of nested query roots.
     */
    static class RelativeAnchor extends Evaluator {
        @Override public boolean matches(Element root, Element element) {
            return false;
        }

        @Override boolean matchesRelative(Element root, Node node, Element anchor) {
            return node == anchor;
        }

        @Override protected int cost() {
            return 1;
        }

        @Override public String toString() {
            return ">";
        }
    }

    /** Implements the :is(sub-query) pseudo-selector */
    static class Is extends StructuralEvaluator {
        public Is(Evaluator evaluator) {
            super(evaluator);
        }

        @Override
        boolean evaluateMatch(Element root, Node node, @Nullable Element anchor) {
            return evaluator.matches(root, node);
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
        boolean evaluateMatch(Element root, Node node, @Nullable Element anchor) {
            return !memoMatches(root, node, null);
        }

        @Override protected int cost() {
            return 2 + evaluator.cost();
        }

        @Override
        public String toString() {
            return String.format(":not(%s)", evaluator);
        }
    }

    /**
     Any Ancestor (i.e., ascending parent chain.).
     */
    static class Ancestor extends StructuralEvaluator {
        public Ancestor(Evaluator evaluator) {
            super(evaluator);
        }

        @Override
        boolean evaluateMatch(Element root, Node node, @Nullable Element anchor) {
            if (!web && root == node)
                return false;

            for (Node parent = node.parent(); parent != null; parent = parent.parent()) {
                if (memoMatches(root, parent, anchor))
                    return true;
                if (!web && parent == root)
                    break;
            }
            return false;
        }

        @Override
        protected int cost() {
            return 8 * evaluator.cost(); // probably lower than has(), but still significant, depending on doc and el depth.
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
    static class ImmediateParentRun extends StructuralEvaluator {
        final ArrayList<Evaluator> evaluators = new ArrayList<>();
        int cost = 2;

        public ImmediateParentRun(Evaluator evaluator) {
            super(evaluator);
            evaluators.add(evaluator);
            cost += evaluator.cost();
        }

        void add(Evaluator evaluator) {
            evaluators.add(evaluator);
            cost += evaluator.cost();
            wantsNodes |= evaluator.wantsNodes();
        }

        @Override boolean evaluateMatch(Element root, Node node, @Nullable Element anchor) {
            if (!web && node == root)
                return false; // cannot match as the second eval (first parent test) would be above the root

            for (int i = evaluators.size() -1; i >= 0; --i) {
                if (node == null)
                    return false;
                Evaluator eval = evaluators.get(i);
                boolean matches = anchor != null && node instanceof LeafNode ?
                    eval.matchesRelative(root, node, anchor) : eval.matches(root, node, anchor);
                if (!matches)
                    return false;
                if (i > 0) {
                    if (anchor != null && !web && node == root) return false;
                    node = node.parent();
                }
            }
            return true;
        }

        @Override protected int cost() {
            return cost;
        }

        @Override
        protected void reset() {
            threadMemo.remove();
            for (Evaluator evaluator : evaluators) {
                evaluator.reset();
            }
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

        // matches any previous sibling, so can be same in Element only or wantsNodes context
        @Override boolean evaluateMatch(Element root, Node node, @Nullable Element anchor) {
            if (!web && root == node) return false;

            for (Node sib = node.firstSibling(); sib != null; sib = sib.nextSibling()) {
                if (sib == node) break;
                if (memoMatches(root, sib, anchor)) return true;
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

        @Override boolean evaluateMatch(Element root, Node node, @Nullable Element anchor) {
            if (!web && root == node) return false;

            Node prev = wantsNodes ? node.previousSibling() : node.previousElementSibling();
            return prev != null && memoMatches(root, prev, anchor);
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
