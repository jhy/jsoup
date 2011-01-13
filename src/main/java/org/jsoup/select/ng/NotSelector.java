package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;
import org.jsoup.nodes.Node;

public class NotSelector extends Evaluator {
	Evaluator sel;
	
	

	public NotSelector(Evaluator sel) {
		super();
		this.sel = sel;
	}



	@Override
	public boolean matches(Element node) {
		return !sel.matches(node);
	}
	
	@Override
	public String toString() {
		return String.format(":not%s", sel);
	}

	
	

}
