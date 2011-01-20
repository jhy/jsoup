package org.jsoup.select.selectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

/**
 * Sequencing 'or' evaluator.
 * Matches only if any underlying evaluators have matched
 */
public class OrSelector extends Evaluator {
	private List<Evaluator> selectors;

	public OrSelector(Collection<Evaluator> selectors) {
		super();
		this.selectors = new ArrayList<Evaluator>();
		this.selectors.addAll(selectors);
	}
	
	public void add(Evaluator e) {
		selectors.add(e);
	}

	@Override
	public boolean matches(Element root, Element node) {
		for(Evaluator s : selectors) {
			if(s.matches(root, node))
				return true;
		}
		
		return false;
	}
	
	@Override
	public String toString() {
		return String.format(":or%s", selectors);
	}
}
