package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

public class NotSelector implements Selector {
	Selector sel;
	
	

	public NotSelector(Selector sel) {
		super();
		this.sel = sel;
	}



	@Override
	public boolean select(Element node) {
		return !sel.select(node);
	}

}
