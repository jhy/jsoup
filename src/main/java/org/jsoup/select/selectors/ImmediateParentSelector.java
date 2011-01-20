package org.jsoup.select.selectors;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

/**
 * Tree-based evaluator for matching Element's immediate parent.
 * For evaluating 'E > F' construction
 */
public class ImmediateParentSelector extends Evaluator {
    private Evaluator sel;

    public ImmediateParentSelector(Evaluator sel) {
        this.sel = sel;
    }

    @Override
    public boolean matches(Element root, Element element) {
        if (root == element)
            return false;

        Element parent = element.parent();
        return parent != null && sel.matches(root, parent);
    }
}
