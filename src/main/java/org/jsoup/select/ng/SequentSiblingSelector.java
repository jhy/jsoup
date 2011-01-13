package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

public class SequentSiblingSelector extends Evaluator {
	Evaluator sel;
	
	

	public SequentSiblingSelector(Evaluator sel) {
		this.sel = sel;
	}



	@Override
	public boolean matches(Element element) {
		Element next = element.nextElementSibling();
		
		while(next != null) {
			if(sel.matches(next))
				return true;
			
			next = next.nextElementSibling();
		}
		
		return false;
	}

}
