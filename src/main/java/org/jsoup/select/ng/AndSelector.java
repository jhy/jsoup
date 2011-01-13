package org.jsoup.select.ng;

import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;
import org.jsoup.nodes.Node;

public class AndSelector extends Evaluator {
	List<Evaluator> selectors;

	public AndSelector(List<Evaluator> selectors) {
		super();
		this.selectors = selectors;
	}

	@Override
	public boolean matches(Element node) {
		for(Evaluator s : selectors) {
			if(!s.matches(node))
				return false;
		}
		
		return true;
	}

	

}
