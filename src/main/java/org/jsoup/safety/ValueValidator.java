package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;

public interface ValueValidator {

	boolean isRequired();

	boolean isSafe(Element el, Attribute attr);
}
