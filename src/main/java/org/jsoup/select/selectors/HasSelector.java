package org.jsoup.select.selectors;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

/**
 * Evaluator for :has() construction
 * Matches if element's descendants matches underlying evaluator
 */
public class HasSelector extends Evaluator {
    private Evaluator sel;

    public HasSelector(Evaluator sel) {
        this.sel = sel;
    }

    @Override
    public boolean matches(Element root, Element element) {
        for (Element e : element.getAllElements()) {
            if (e != element && sel.matches(root, e))
                return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format(":has(%s)", sel);
    }
}
