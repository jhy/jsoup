package org.jsoup.select;

import org.jsoup.internal.SoftPool;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.NodeIterator;
import org.jsoup.nodes.TextNode;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Evaluates the relative selectors inside {@code :has()}. */
final class HasEvaluator extends Evaluator {
    private final String selector;
    final List<Traversal> traversals;
    private final boolean wantsNodes;
    private final int cost;

    HasEvaluator(String selector, List<Traversal> traversals) {
        this.selector = selector;
        this.traversals = Collections.unmodifiableList(new ArrayList<>(traversals));
        boolean wantsNodes = false;
        int evaluatorCost = 0;
        for (Traversal traversal : traversals) {
            wantsNodes |= traversal.evaluator.wantsNodes();
            evaluatorCost += traversal.evaluator.cost();
        }
        this.wantsNodes = wantsNodes;
        this.cost = 10 * evaluatorCost;
    }

    @Override public boolean matches(Element root, Element element) {
        if (web) {
            element = documentElementOrSelf(element); // use the document element when testing :has() on a Document
            if (element == null) return false;
        }
        if (!web) root = element;
        for (int i = 0; i < traversals.size(); i++) {
            if (traversals.get(i).matchesAny(root, element)) return true;
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

    /**
     Searches the children, descendants, or following siblings needed by a {@code :has()} query.
     For example, {@code :has(> a)} checks children, while {@code :has(+ div span)} checks the next sibling and its descendants.
     */
    static final class Traversal {
        static final SoftPool<NodeIterator<Node>> NodeIterPool =
            new SoftPool<>(() -> new NodeIterator<>(new TextNode(""), Node.class)); // restart() replaces the placeholder root

        final Evaluator evaluator;

        final boolean startsAtSibling;
        final boolean descend;
        final boolean nextSiblingOnly;
        private final boolean wantsNodes;

        Traversal(ArrayList<Evaluator> alternatives, boolean startsAtSibling, boolean descend, boolean nextSiblingOnly) {
            evaluator = alternatives.size() == 1 ? alternatives.get(0) : new CombiningEvaluator.Or(alternatives);
            this.startsAtSibling = startsAtSibling;
            this.descend = descend;
            this.nextSiblingOnly = nextSiblingOnly;
            wantsNodes = evaluator.wantsNodes();
        }

        boolean matchesAny(Element context, Element subject) {
            Node start = first(subject);
            if (start == null) return false;
            @Nullable NodeIterator<Node> nodes = descend ? NodeIterPool.borrow() : null;
            try {
                return findMatch(context, subject, start, nodes);
            } finally {
                if (nodes != null) NodeIterPool.release(nodes);
                // cached matches are only valid for this starting element
                evaluator.reset();
            }
        }

        private boolean findMatch(Element context, Element subject, Node start, @Nullable NodeIterator<Node> nodes) {
            for (; start != null; start = next(start)) {
                if (nodes != null) {
                    nodes.restart(start);
                    if (!startsAtSibling) nodes.next(); // search the whole tree, excluding the subject
                }
                do {
                    Node candidate = nodes != null ? nodes.next() : start;
                    if (evaluator.matches(context, candidate, subject)) return true;
                } while (nodes != null && nodes.hasNext());
            }
            return false;
        }

        private @Nullable Node first(Element subject) {
            if (!startsAtSibling) {
                Node child = subject.firstChild();
                return descend && child != null ? subject : child;
            }
            return nextSiblingOnly || !descend && !wantsNodes ? subject.nextElementSibling() : subject.nextSibling();
        }

        private @Nullable Node next(Node start) {
            if (nextSiblingOnly || descend && !startsAtSibling) return null;
            return startsAtSibling && !descend && !wantsNodes ? start.nextElementSibling() : start.nextSibling();
        }
    }
}
