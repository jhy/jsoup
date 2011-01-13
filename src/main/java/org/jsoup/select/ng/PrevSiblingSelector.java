package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

public class PrevSiblingSelector extends Evaluator {
	Evaluator sel;
	
	public PrevSiblingSelector(Evaluator sel) {
		this.sel = sel;
	}

	@Override
	public boolean matches(Element element) {
		Element prev = element.previousElementSibling();
		
		if(prev != null && sel.matches(prev))
			return true;
		
		return false;
	}

}
