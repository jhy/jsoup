package org.jsoup.select.selectors;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

/**
 * Tree-based evaluator for matching Element's immediate previous sibling.
 * For evaluating 'E + F' construction
 */
public class ImmediatePreviousSiblingSelector extends Evaluator {
    private Evaluator sel;

    public ImmediatePreviousSiblingSelector(Evaluator sel) {
        this.sel = sel;
    }

    @Override
    public boolean matches(Element root, Element element) {
        if (root == element)
            return false;

        Element prev = element.previousElementSibling();
        return prev != null && sel.matches(root, prev);
    }

    @Override
    public String toString() {
        return String.format(":prev%s", sel);
    }
}
