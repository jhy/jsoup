package org.jsoup.select;

import org.jsoup.internal.SoftPool;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.NodeIterator;
import org.jsoup.nodes.TextNode;

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

    boolean memoMatches(final Element root, final Node node) {
        Map<Node, Map<Node, Boolean>> rootMemo = threadMemo.get();
        Map<Node, Boolean> memo = rootMemo.computeIfAbsent(root, r -> new WeakHashMap<>());
        return memo.computeIfAbsent(node, test -> evaluator.matches(root, test));
    }

    @Override protected void reset() {
        threadMemo.remove();
        evaluator.reset();
        super.reset();
    }

    @Override
    public boolean matches(Element root, Element element) {
        return evaluateMatch(root, element);
    }

    @Override
    boolean matches(Element root, LeafNode leafNode) {
        return evaluateMatch(root, leafNode);
    }

    abstract boolean evaluateMatch(Element root, Node node);

    static class Root extends Evaluator {
        @Override
        public boolean matches(Element root, Element element) {
            return root == element;
        }

        @Override protected int cost() {
            return 1;
        }

        @Override public String toString() {
            return ">";
        }
    }

    static class Has extends StructuralEvaluator {
        static final SoftPool<NodeIterator<Node>> NodeIterPool =
            new SoftPool<>(() -> new NodeIterator<>(new TextNode(""), Node.class));
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
            NodeIterator<Node> it = NodeIterPool.borrow();
            it.restart(element);
            try {
                while (it.hasNext()) {
                    Node node = it.next();
                    if (node == element) continue; // don't match self, only descendants
                    if (evaluator.matches(element, node)) {
                        return true;
                    }
                }
            } finally {
                NodeIterPool.release(it);
            }
            return false;
        }

        @Override
        boolean evaluateMatch(Element root, Node node) {
            return false; // unused; :has(::comment)) goes via implicit root combinator
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
        boolean evaluateMatch(Element root, Node node) {
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
        boolean evaluateMatch(Element root, Node node) {
            return !memoMatches(root, node);
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
        boolean evaluateMatch(Element root, Node node) {
            if (root == node)
                return false;

            for (Node parent = node.parent(); parent != null; parent = parent.parent()) {
                if (memoMatches(root, parent))
                    return true;
                if (parent == root)
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

        @Override boolean evaluateMatch(Element root, Node node) {
            if (node == root)
                return false; // cannot match as the second eval (first parent test) would be above the root

            for (int i = evaluators.size() -1; i >= 0; --i) {
                if (node == null)
                    return false;
                Evaluator eval = evaluators.get(i);
                if (!eval.matches(root, node))
                    return false;
                node = node.parent();
            }
            return true;
        }

        @Override protected int cost() {
            return cost;
        }

        @Override
        protected void reset() {
            for (Evaluator evaluator : evaluators) {
                evaluator.reset();
            }
            super.reset();
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
        @Override boolean evaluateMatch(Element root, Node node) {
            if (root == node) return false;

            for (Node sib = node.firstSibling(); sib != null; sib = sib.nextSibling()) {
                if (sib == node) break;
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

        @Override boolean evaluateMatch(Element root, Node node) {
            if (root == node) return false;

            Node prev = wantsNodes ? node.previousSibling() : node.previousElementSibling();
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
