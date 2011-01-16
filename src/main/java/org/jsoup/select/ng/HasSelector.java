package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

/**
 * Evaluator for :has() construction
 * Matches if element's descendents matches underlying evaluator
 * @author ant
 *
 */
public class HasSelector extends Evaluator {
	Evaluator sel;
	
	public HasSelector(Evaluator sel) {
		this.sel = sel;
	}

	@Override
	public boolean matches(Element root, Element element) {
		

		for(Element e : element.getAllElements()) {
			if(e != element && sel.matches(root, e))
				return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return String.format(":has(%s)", sel);
	}
	
	

}
