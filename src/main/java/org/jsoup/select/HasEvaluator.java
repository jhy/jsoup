package org.jsoup.select;

import org.jsoup.internal.SoftPool;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.NodeIterator;
import org.jsoup.nodes.TextNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Evaluates the relative selectors inside {@code :has()}. */
final class HasEvaluator extends Evaluator {
    private final String selector;
    final List<Traversal> traversals;
    private final boolean wantsNodes;
    private final int cost;

    /** Creates an evaluator from the canonical selector text and its planned traversals. */
    HasEvaluator(String selector, List<Traversal> traversals) {
        this.selector = selector;
        this.traversals = Collections.unmodifiableList(new ArrayList<>(traversals));
        boolean wantsNodes = false;
        int cost = 0;
        for (Traversal traversal : traversals) {
            wantsNodes |= traversal.evaluator.wantsNodes();
            cost += traversal.evaluator.cost();
        }
        this.wantsNodes = wantsNodes;
        this.cost = 10 * cost;
    }

    @Override public boolean matches(Element root, Element element) {
        for (Traversal traversal : traversals) {
            if (traversal.matchesAny(element)) return true;
        }
        return false;
    }

    @Override boolean wantsNodes() {
        return wantsNodes;
    }

    @Override protected void reset() {
        for (Traversal traversal : traversals) traversal.evaluator.reset();
    }

    @Override protected int cost() {
        return cost;
    }

    @Override public String toString() {
        return ":has(" + selector + ')';
    }

    /** A traversal shared by one or more {@code :has()} alternatives. */
    static final class Traversal {
        static final SoftPool<NodeIterator<Node>> NodeIterPool =
            new SoftPool<>(() -> new NodeIterator<>(new TextNode(""), Node.class)); // restart() replaces the placeholder root

        final Evaluator evaluator;
        final Scope scope;

        /** Creates a traversal that applies the evaluator within the given scope. */
        Traversal(Evaluator evaluator, Scope scope) {
            this.evaluator = evaluator;
            this.scope = scope;
        }

        /** Traverses relative to the {@code :has()} subject, stopping at the first match. */
        boolean matchesAny(Element subject) {
            switch (scope) {
                case Children: return matchesChild(subject);
                case NextSibling: return matchesNextSibling(subject);
                case NextSiblingTree: return matchesNextSiblingTree(subject);
                case FollowingSiblings: return matchesSibling(subject);
                case FollowingSiblingTrees: return matchesFollowingSiblingTrees(subject);
                case Descendants: return matchesDescendants(subject);
                default: throw new IllegalStateException("Unknown :has() traversal scope: " + scope);
            }
        }

        /** Tests only the subject's immediate child nodes. */
        private boolean matchesChild(Element subject) {
            for (Node child = subject.firstChild(); child != null; child = child.nextSibling()) {
                if (evaluator.matches(subject, child))
                    return true;
            }
            return false;
        }

        /** Tests the subject's next element sibling for a simple {@code +} selector. */
        private boolean matchesNextSibling(Element subject) {
            Element sibling = subject.nextElementSibling();
            return sibling != null && evaluator.matches(subject, sibling);
        }

        /** Searches the next element sibling tree, as needed by {@code :has(+ div span)}. */
        private boolean matchesNextSiblingTree(Element subject) {
            Element sibling = subject.nextElementSibling();
            return sibling != null && matchesNodeAndDescendants(subject, sibling);
        }

        /** Tests sibling nodes after the subject, including non-element nodes when the evaluator accepts them. */
        private boolean matchesSibling(Element subject) {
            boolean nodes = evaluator.wantsNodes();
            for (Node sibling = nodes ? subject.nextSibling() : subject.nextElementSibling(); sibling != null;
                 sibling = nodes ? sibling.nextSibling() : sibling.nextElementSibling()) {
                if (evaluator.matches(subject, sibling))
                    return true;
            }
            return false;
        }

        /**
         Searches following sibling trees for selectors such as {@code + div + section}.
         The evaluator enforces the sibling relationships within this broader search.
         */
        private boolean matchesFollowingSiblingTrees(Element subject) {
            for (Node sibling = subject.nextSibling(); sibling != null; sibling = sibling.nextSibling()) {
                if (matchesNodeAndDescendants(subject, sibling)) return true;
            }
            return false;
        }

        /** Tests descendants of the subject, excluding the subject itself. */
        private boolean matchesDescendants(Element subject) {
            NodeIterator<Node> it = NodeIterPool.borrow();
            try {
                it.restart(subject);
                it.next(); // skip the subject
                return matchesAnyNode(subject, it);
            } finally {
                NodeIterPool.release(it);
            }
        }

        /** Tests the starting node and all of its descendants. */
        private boolean matchesNodeAndDescendants(Element subject, Node start) {
            NodeIterator<Node> it = NodeIterPool.borrow();
            try {
                it.restart(start);
                return matchesAnyNode(subject, it);
            } finally {
                NodeIterPool.release(it);
            }
        }

        /** Tests nodes from the iterator until the evaluator matches one. */
        private boolean matchesAnyNode(Element subject, NodeIterator<Node> nodes) {
            while (nodes.hasNext()) {
                if (evaluator.matches(subject, nodes.next()))
                    return true;
            }
            return false;
        }
    }

    /** Where a traversal runs relative to the {@code :has()} subject. */
    enum Scope {
        /** The subject's immediate child nodes, as in {@code :has(> div)}. */
        Children,
        /** The subject's next element sibling, as in {@code :has(+ div)}. */
        NextSibling,
        /** The subject's next element sibling and all of its descendants, as in {@code :has(+ div span)}. */
        NextSiblingTree,
        /** The sibling nodes after the subject, as in {@code :has(~ div)}. */
        FollowingSiblings,
        /** The sibling nodes after the subject and all of their descendants, as in {@code :has(~ div span)}. */
        FollowingSiblingTrees,
        /** The subject's descendants, excluding the subject itself. */
        Descendants
    }
}
