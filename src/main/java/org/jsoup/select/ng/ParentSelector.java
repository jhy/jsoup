package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

/**
 * Tree-based evaluator for matching any of Element's parents.
 * For evaluating 'E F' construction
 * @author ant
 *
 */
public class ParentSelector extends Evaluator {
	Evaluator sel;
	
	public ParentSelector(Evaluator sel) {
		this.sel = sel;
	}

	@Override
	public boolean matches(Element root, Element element) {
		
		if(root == element)
			return false;

		
		Element parent = element.parent();
		
		while(parent != root) {
			if(sel.matches(root, parent))
				return true;
			
			parent = parent.parent();
		}

		return false;
	}
	
	@Override
	public String toString() {
		return String.format(":parent%s", sel);
	}


}
