package org.jsoup.hamcrest;

import org.hamcrest.Matcher;
import org.jsoup.nodes.Element;

/**
 * Internal API
 */
abstract class ElementMatcher extends CssMatcher<Element> {
    private final Matcher<? super Element> matcher;

    protected ElementMatcher(String css, Matcher<? super Element> matcher) {
        super(css);
        this.matcher = matcher;
    }

    Matcher<? super Element> getMatcher() {
        return matcher;
    }
}
