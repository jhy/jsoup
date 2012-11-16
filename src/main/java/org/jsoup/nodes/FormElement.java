package org.jsoup.nodes;

import java.util.ArrayList;

import org.jsoup.parser.Tag;

public class FormElement extends Element {
	private ArrayList<Element> formElements = new ArrayList<Element>();
	public FormElement(Tag tag, String baseUri, Attributes attributes) {
        super(tag, baseUri, attributes);
			// TODO Auto-generated constructor stub
	}
	
	public void addElement(Element element){
		formElements.add(element);
	}
	
	public ArrayList<Element> getElements(){
		return formElements;
	}	
}
