package org.jsoup.select.selectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

/**
 * Sequencing 'and' evaluator.
 * Matches only if all underlying evaluators have matched
 */
public class AndSelector extends Evaluator {
	private List<Evaluator> selectors;

	private AndSelector() {
		super();
		this.selectors = new ArrayList<Evaluator>();
	}
	
	public AndSelector(Evaluator... evals) {
		this();
		this.selectors.addAll(Arrays.asList(evals));
	}

	public AndSelector(Collection<Evaluator> selectors) {
		this();
		this.selectors.addAll(selectors);
	}
	
	public void add(Evaluator e) {
		selectors.add(e);
	}
	
	public void addAll(Collection<Evaluator> e) {
		selectors.addAll(e);
	}

	@Override
	public boolean matches(Element root, Element node) {
		for(Evaluator s : selectors) {
			if(!s.matches(root, node))
				return false;
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		return StringUtil.join(selectors, " ");
	}
}
