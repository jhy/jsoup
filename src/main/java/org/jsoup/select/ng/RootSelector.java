package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

public class RootSelector extends Evaluator {

	@Override
	public boolean matches(Element root, Element element) {
		return root == element;
	}

}
