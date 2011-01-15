package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

public class NotSelector extends Evaluator {
	Evaluator sel;
	
	

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
