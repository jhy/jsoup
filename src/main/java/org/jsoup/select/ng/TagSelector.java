package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

public class TagSelector implements Selector {
	String tag;
	

	public TagSelector(String tag) {
		super();
		this.tag = tag;
	}



	@Override
	public boolean select(Element node) {
			Element el = (Element)node;
			return el.tagName().equals(tag);
	}

}
