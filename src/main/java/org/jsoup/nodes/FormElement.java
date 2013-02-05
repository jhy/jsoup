package org.jsoup.nodes;

import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

public class FormElement extends Element {
	private Elements formElements = new Elements();
	public FormElement(Tag tag, String baseUri, Attributes attributes) {
        super(tag, baseUri, attributes);
			// TODO Auto-generated constructor stub
	}
	
	public void addElement(Element element){
		formElements.add(element);
	}
	
	public Elements getElements(){
		return formElements;
	}	
}
