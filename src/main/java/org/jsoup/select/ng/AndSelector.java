package org.jsoup.select.ng;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

public class AndSelector implements Selector {
	List<Selector> selectors;

	public AndSelector(List<Selector> selectors) {
		this.selectors = selectors;
	}

	@Override
	public boolean select(Element node) {
		for(Selector s : selectors) {
			if(!s.select(node))
				return false;
		}
		
		return true;
	}

	

}
