package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

/**
 * Tree-based evaluator for matching Element's parent.
 * For evaluating 'E ~ F' construction
 * @author ant
 *
 */
public class PrevioustSiblingSelector extends Evaluator {
	Evaluator sel;
	
	

	public PrevioustSiblingSelector(Evaluator sel) {
		this.sel = sel;
	}



	@Override
	public boolean matches(Element root, Element element) {
		if(root == element)
			return false;
		
		Element prev = element.previousElementSibling();
		
		while(prev != null) {
			if(sel.matches(root, prev))
				return true;
			
			prev = prev.previousElementSibling();
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return String.format(":prev*%s", sel);
	}


}
