package org.jsoup.select.ng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class SelectMatch {
	Evaluator sel;
	
	public SelectMatch(Evaluator sel) {
		this.sel = sel;
	}
	
	public Elements match(Node root) {
		return new Elements(match(root, new ArrayList<Element>()));
	}
	
	public Elements match(Elements elements) {
		List<Element> matched = new ArrayList<Element>();
		
		for(Element el : elements) {
			match(el, matched);
		}
		
		return new Elements(matched);
	}
	
	public List<Element> match(Node root, List<Element> matched) {
		if((root instanceof Element) && sel.matches((Element)root))
			matched.add((Element)root);
		
		for(Node n : root.childNodes())
			if(n instanceof Element)
				match(n, matched);
		
		return matched;
	}
	
	/*public List<Node> match1(Node start, List<Node> matched) {
		Deque<Node> queue = new ArrayDeque<Node>();
		queue.add(start);
		
		while(!queue.isEmpty()) {
			Node n = queue.pop();
			
			if(sel.select(n))
				matched.add(n);
			
			queue.addAll(n.childNodes());
			
		}
		
		
		return matched;
	}*/
	
	public static Elements match(Node root, Evaluator sel) {
		SelectMatch sm = new SelectMatch(sel);
		
		return sm.match(root);
	}
	
	public static Elements match(Elements elements, Evaluator sel) {
		SelectMatch sm = new SelectMatch(sel);
		
		return sm.match(elements);
	}
	
	public static Elements match(Elements elements, Evaluator... sel) {
		SelectMatch sm = new SelectMatch(new AndSelector(Arrays.asList(sel)));
		
		return sm.match(elements);
	}
	
	public static Elements match(Node root, Evaluator... sel) {
		SelectMatch sm = new SelectMatch(new AndSelector(Arrays.asList(sel)));
		
		return sm.match(root);
	}




	

	
	
	

}
