package org.jsoup.select.selectors;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

/**
 * Logical 'not' evaluator. Matches only if underlying evaluator didn't match.
 */
public class NotSelector extends Evaluator {
    private Evaluator sel;

    public NotSelector(Evaluator sel) {
        super();
        this.sel = sel;
    }

    @Override
    public boolean matches(Element root, Element node) {
        return !sel.matches(root, node);
    }

    @Override
    public String toString() {
        return String.format(":not%s", sel);
    }


}
