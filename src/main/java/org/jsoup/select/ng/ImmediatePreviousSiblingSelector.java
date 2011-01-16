package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

/**
 * Tree-based evaluator for matching Element's immediate previous sibling.
 * For evaluating 'E + F' construction
 * @author ant
 *
 */
public class ImmediatePreviousSiblingSelector extends Evaluator {
	Evaluator sel;
	
	public ImmediatePreviousSiblingSelector(Evaluator sel) {
		this.sel = sel;
	}

	@Override
	public boolean matches(Element root, Element element) {
		if(root == element)
			return false;

		
		Element prev = element.previousElementSibling();
		
		if(prev != null && sel.matches(root, prev))
			return true;
		
		return false;
	}
	
	@Override
	public String toString() {
		return String.format(":prev%s", sel);
	}


}
