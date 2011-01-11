package org.jsoup.select.ng;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

public class ListSelector implements Selector {
	List<Selector> selectors;

	public ListSelector(List<Selector> selectors) {
		super();
		this.selectors = selectors;
	}

	@Override
	public boolean select(Element node) {
		for(Selector s : selectors) {
			if(s.select(node))
				return true;
		}
		
		return false;
	}
	
	
	
	

}
