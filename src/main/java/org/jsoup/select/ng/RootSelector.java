package org.jsoup.select.ng;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Evaluator;

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
