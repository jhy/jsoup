package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

public class PrevSiblingSelector extends Evaluator {
	Evaluator sel;
	
	public PrevSiblingSelector(Evaluator sel) {
		this.sel = sel;
	}

	@Override
	public boolean matches(Element root, Element element) {
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
