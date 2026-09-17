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
    final List<Search> searches;
    private final boolean wantsNodes;
    private final int cost;

    /** Creates an evaluator from the searches parsed inside {@code :has()}. */
    HasEvaluator(List<Search> searches) {
        this.searches = Collections.unmodifiableList(new ArrayList<>(searches));
        boolean wantsNodes = false;
        int cost = 0;
        for (Search search : searches) {
            wantsNodes |= search.evaluator.wantsNodes();
            cost += search.evaluator.cost();
        }
        this.wantsNodes = wantsNodes;
        this.cost = 10 * cost;
    }

    @Override public boolean matches(Element root, Element element) {
        for (Search search : searches) {
            if (search.matchesAny(element)) return true;
        }
        return false;
    }

    @Override boolean wantsNodes() {
        return wantsNodes;
    }

    @Override protected void reset() {
        for (Search search : searches) search.evaluator.reset();
    }

    @Override protected int cost() {
        return cost;
    }

    @Override public String toString() {
        StringBuilder out = new StringBuilder(":has(");
        for (Search search : searches) {
            if (out.length() > 5) out.append(", ");
            out.append(search.evaluator);
        }
        return out.append(')').toString();
    }

    /** Consecutive {@code :has()} alternatives that can share the same traversal. */
    static final class Search {
        static final SoftPool<NodeIterator<Node>> NodeIterPool =
            new SoftPool<>(() -> new NodeIterator<>(new TextNode(""), Node.class));
        // the node here is just a placeholder so this can be final - gets set in restart()

        final Evaluator evaluator;
        final SearchScope scope;

        /** Creates a search that applies the evaluator within the specified scope. */
        Search(Evaluator evaluator, SearchScope scope) {
            this.evaluator = evaluator;
            this.scope = scope;
        }

        /** Tests this search relative to the {@code :has()} subject, stopping at the first match. */
        boolean matchesAny(Element subject) {
            switch (scope) {
                case Children: return matchesChild(subject);
                case NextSibling: return matchesNextSibling(subject);
                case FollowingSiblings: return matchesSibling(subject);
                case FollowingSiblingSubtrees: return matchesSiblingSubtree(subject);
                default: return matchesSubtree(subject, subject);
            }
        }

        /** Tests the subject's immediate child nodes without entering their subtrees. */
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

        /** Tests sibling nodes after the subject, including non-element nodes when requested. */
        private boolean matchesSibling(Element subject) {
            boolean nodes = evaluator.wantsNodes();
            for (Node sib = nodes ? subject.nextSibling() : subject.nextElementSibling(); sib != null;
                 sib = nodes ? sib.nextSibling() : sib.nextElementSibling()) {
                if (evaluator.matches(subject, sib))
                    return true;
            }
            return false;
        }

        /**
         Tests following sibling subtrees for selectors such as {@code + div span}.
         The evaluator still enforces the relationship to the subject, including adjacency for {@code +}.
         */
        private boolean matchesSiblingSubtree(Element subject) {
            for (Node sibling = subject.nextSibling(); sibling != null; sibling = sibling.nextSibling()) {
                if (matchesSubtree(subject, sibling)) return true;
            }
            return false;
        }

        /** Tests a node and its descendants, excluding the {@code :has()} subject itself. */
        private boolean matchesSubtree(Element subject, Node start) {
            NodeIterator<Node> it = NodeIterPool.borrow();
            it.restart(start);
            try {
                while (it.hasNext()) {
                    Node node = it.next();
                    if (node == subject) continue; // don't match self, only descendants
                    if (evaluator.matches(subject, node)) {
                        return true;
                    }
                }
            } finally {
                NodeIterPool.release(it);
            }
            return false;
        }
    }

    /** Where a search runs relative to the {@code :has()} subject. */
    enum SearchScope {
        /** The subject's immediate child nodes, as in {@code :has(> div)}. */
        Children,
        /** The subject's next element sibling, as in {@code :has(+ div)}. */
        NextSibling,
        /** The sibling nodes after the subject, as in {@code :has(~ div)}. */
        FollowingSiblings,
        /** The sibling nodes after the subject and their descendants, as in {@code :has(+ div span)}. */
        FollowingSiblingSubtrees,
        /** The subject's descendants. */
        Descendants
    }
}
