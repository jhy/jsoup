package org.jsoup.select.ng;

import java.util.Arrays;

public class BasicSelector {
	
	public static ListSelector list(Selector... sels) {
		return new ListSelector(Arrays.asList(sels));
	}
	
	public static AndSelector and(Selector... sels) {
		return new AndSelector(Arrays.asList(sels));
	}
	
	public static NotSelector not(Selector sel) {
		return new NotSelector(sel);
	}
	
	public static TagSelector tag(String tag) {
		return new TagSelector(tag);
	}
	
	public static IdSelector id(String id) {
		return new IdSelector(id);
	}
	
	public static ClassSelector cls(String cls) {
		return new ClassSelector(cls);
	}
}
