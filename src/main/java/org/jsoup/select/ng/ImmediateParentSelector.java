package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

/**
 * Tree-based evaluator for matching Element's immediate parent.
 * For evaluating 'E > F' construction
 * @author ant
 *
 */
public class ImmediateParentSelector extends Evaluator {
	Evaluator sel;
	
	public ImmediateParentSelector(Evaluator sel) {
		this.sel = sel;
	}

	@Override
	public boolean matches(Element root, Element element) {

		if(root == element)
			return false;
		
		Element parent = element.parent();

		if(parent != null)
			return sel.matches(root, parent);

		return false;
	}


}
