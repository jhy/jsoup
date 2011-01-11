package org.jsoup.select.ng;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class SelectMatch {
	Selector sel;
	
	public SelectMatch(Selector sel) {
		this.sel = sel;
	}
	
	public Elements match(Node root) {
		return new Elements(match(root, new ArrayList<Element>()));
	}
	
	public List<Element> match(Node start, List<Element> matched) {
		if((start instanceof Element) && sel.select((Element)start))
			matched.add((Element)start);
		
		for(Node n : start.childNodes())
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

	

	
	
	

}
