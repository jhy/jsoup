package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

/**
 * Evaluator for matching elements
 * Designed to be faster than separated chained Tag/Class/Id evaluators
 * @author ant
 *
 */
public class ElementSelector extends Evaluator {
	String tag;
	String cls;
	String id;
	class AttrSelector {
		String name;
		String value;
		
		
	}
	
	public ElementSelector(String tag, String cls, String id) {
		super();
		this.tag = tag;
		this.cls = cls;
		this.id = id;
	}

	@Override
	public boolean matches(Element root, Element node) {
			Element el = (Element) node;
			
			if(tag != null && !el.tagName().equals(tag))
				return false;
			
			
			if(cls != null && !el.classNames().contains(cls))
				return false;
			
			if(id != null && !el.id().equals(id))
				return false;
			
			return true;
	}
	
	@Override
	public String toString() {
		return String.format(":element(tag=%s,class=%s,id=%s)", tag, cls, id);
	}

}
