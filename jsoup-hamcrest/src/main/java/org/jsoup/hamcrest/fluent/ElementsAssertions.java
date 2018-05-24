package org.jsoup.hamcrest.fluent;

import static org.hamcrest.Matchers.hasSize;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.jsoup.hamcrest.DocumentMatcher;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class ElementsAssertions implements DocumentMatcher {

    private List<Matcher<? super Elements>> matchers = new ArrayList<>();

    private final String css;

    ElementsAssertions(String css) {
        this.css = css;
    }

    protected String getCss() {
        return css;
    }

    @Override
    public void match(Document document) {
        Elements elements = document.select(getCss());
        MatcherAssert.assertThat(elements, new AllOfMatcher<>(
                description -> description.appendText("elements selected by ").appendValue(getCss())
                        .appendText(" matching "), matchers));
    }

    /**
     * Changes the fluent style to apply to each element hereafter instead
     * @return
     */
    public EachElementAssertions each() {
        return new EachElementAssertions(css, this);
    }

    /**
     * Assert that a count of elements found by the css query.
     * @param count the expected number of elements
     * @return this for fluent chaining
     */
    public ElementsAssertions hasCount(int count) {
        matchers.add(hasSize(count));
        return this;
    }
}
