package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

public class NextSiblingSelector extends Evaluator {
	Evaluator sel;
	
	public NextSiblingSelector(Evaluator sel) {
		this.sel = sel;
	}

	@Override
	public boolean matches(Element element) {
		Element next = element.nextElementSibling();
		
		if(next != null && sel.matches(next))
			return true;
		
		return false;
	}

}
