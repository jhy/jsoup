package org.jsoup.select.ng;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;

public class AttrSelector {

	static class BaseAttrSelector {
		String name;
		String value;
		public BaseAttrSelector(String name, String value) {
			super();
			this.name = name;
			this.value = value;
		}
	}
	
	public static class AttrNameSelector extends BaseAttrSelector implements Selector {
		public AttrNameSelector(String name) {
			super(name, null);
		}

		@Override
		public boolean select(Element element) {
			return element.hasAttr(name);
		}
	}
	
	public static class AttrNameValueSelector extends BaseAttrSelector implements Selector {

		public AttrNameValueSelector(String name, String value) {
			super(name, value);
		}

		@Override
		public boolean select(Element element) {
			return element.attributes().get(name).equals(value);
		}
	}
	
	public static class AttrNameValuePrefixSelector extends BaseAttrSelector implements Selector {

		public AttrNameValuePrefixSelector(String name, String value) {
			super(name, value);
		}

		@Override
		public boolean select(Element element) {
			return element.attributes().get(name).startsWith(value);
		}
	}
	
	public static class AttrNameValueSuffixSelector extends BaseAttrSelector implements Selector {

		public AttrNameValueSuffixSelector(String name, String value) {
			super(name, value);
		}

		@Override
		public boolean select(Element element) {
			return element.attributes().get(name).endsWith(value);
		}
	}
	
	public static class AttrNameValueContainsSelector extends BaseAttrSelector implements Selector {

		public AttrNameValueContainsSelector(String name, String value) {
			super(name, value);
		}

		@Override
		public boolean select(Element element) {
			return element.attributes().get(name).contains(value);
		}
	}
	
	public static class AttrNameValueMatchesSelector extends BaseAttrSelector implements Selector {

		public AttrNameValueMatchesSelector(String name, String value) {
			super(name, value);
		}

		@Override
		public boolean select(Element element) {
			return element.attributes().get(name).matches(value);
		}
	}
	
	public static class AttrNamePrefixSelector extends BaseAttrSelector implements Selector {

		public AttrNamePrefixSelector(String name) {
			super(name, null);
		}

		@Override
		public boolean select(Element element) {
			Attributes attrs = element.attributes();
			
			for(Attribute attr : attrs) {
				if(attr.getKey().startsWith(name))
					return true;
			}
			
			return false;			
		}
	}
}
