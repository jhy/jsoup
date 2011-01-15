package org.jsoup.select.ng;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

public class ElementContainerSelector extends Evaluator {
	
	List<Evaluator> sels;
	
	public ElementContainerSelector() {
		sels = new ArrayList<Evaluator>();
	}
	
	public ElementContainerSelector add(Evaluator e) {
		sels.add(e);
		return this;
	}

	@Override
	public boolean matches(Element root, Element element) {
		for(Evaluator e: sels)
			if(!e.matches(root, element))
				return false;
		
		return true;
	}
	
	@Override
	public String toString() {
		return String.format(":ecs%s", sels);
	}

	

}
