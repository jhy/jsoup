package org.jsoup.select.ng;

import java.util.ArrayList;
import java.util.Collection;
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
	
	public AndSelector(Collection<Evaluator> selectors) {
		super();
		this.selectors = new ArrayList<Evaluator>();
		this.selectors.addAll(selectors);
	}
	
	public void add(Evaluator e) {
		selectors.add(e);
	}


	@Override
	public boolean matches(Element node) {
		for(Evaluator s : selectors) {
			if(!s.matches(node))
				return false;
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		return String.format(":and%s", selectors);
	}

	

}
