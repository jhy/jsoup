package org.jsoup.hamcrest.fluent;

import static org.hamcrest.MatcherAssert.assertThat;

import org.jsoup.hamcrest.DocumentMatcher;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 * Applies the given matcher against each element separately.
 *
 * See {@link DocumentAssertions#eachElement(String)}.
 */
public class EachElementAssertions extends AbstractElementAssertions<EachElementAssertions> {

    private final DocumentMatcher parent;

    EachElementAssertions(String css) {
        super(css);
        parent = null;
    }

    EachElementAssertions(String css, DocumentMatcher parent) {
        super(css);
        this.parent = parent;
    }

    @Override
    public void match(Document document) {
        if (parent != null) {
            parent.match(document);
        }
        Elements select = document.select(getCss());
        assertThat(select, new EachElementMatcher(new AllOfMatcher<>(description ->
                description.appendText("elements selected by ").appendValue(getCss())
                        .appendText(" matching "), getMatchers())));
    }
}
