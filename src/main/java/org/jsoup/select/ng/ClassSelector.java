package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

public class ClassSelector implements Selector{
	String cls;
	

	public ClassSelector(String cls) {
		super();
		this.cls = cls;
	}



	@Override
	public boolean select(Element node) {

			Element el = (Element)node;
			return el.classNames().contains(cls);
	}


}
