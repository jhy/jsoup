package org.jsoup.select.ng;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

public class ListSelector extends Evaluator {
	List<Evaluator> selectors;

	public ListSelector(List<Evaluator> selectors) {
		super();
		this.selectors = selectors;
	}

	@Override
	public boolean matches(Element node) {
		for(Evaluator s : selectors) {
			if(s.matches(node))
				return true;
		}
		
		return false;
	}
	
	
	
	

}
