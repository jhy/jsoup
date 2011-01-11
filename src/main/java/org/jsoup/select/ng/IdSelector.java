package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

public class IdSelector implements Selector {
	String id;
	

	public IdSelector(String id) {
		super();
		this.id = id;
	}



	@Override
	public boolean select(Element node) {
			Element el = (Element)node;
			return el.id().equals(id);
	}

	

}
