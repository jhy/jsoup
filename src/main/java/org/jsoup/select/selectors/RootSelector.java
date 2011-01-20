package org.jsoup.select.selectors;

import org.jsoup.nodes.Element;
import org.jsoup.select.Evaluator;

/**
 * Root matcher matches only on root node
 * @author ant
 *
 */
public class RootSelector extends Evaluator {

	@Override
	public boolean matches(Element root, Element element) {
		return root == element;
	}

}
