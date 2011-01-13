package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

public class ParentSelector extends Evaluator {
	Evaluator sel;
	
	public ParentSelector(Evaluator sel) {
		this.sel = sel;
	}

	@Override
	public boolean matches(Element element) {
		
		Element parent = element.parent();
		
		while(parent != null) {
			if(sel.matches(parent))
				return true;
			
			parent = parent.parent();
		}

		return false;
	}
	
	@Override
	public String toString() {
		return String.format(":parent%s", sel);
	}


}
