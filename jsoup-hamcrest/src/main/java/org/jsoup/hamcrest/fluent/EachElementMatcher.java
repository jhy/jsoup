package org.jsoup.hamcrest.fluent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Internal API
 */
class EachElementMatcher extends TypeSafeDiagnosingMatcher<Elements> {

    private final Matcher<? super Element> matcher;

    EachElementMatcher(Matcher<? super Element> matcher) {
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(Elements item, Description mismatchDescription) {
        boolean result = true;
        for (Element element : item) {
            if(!matcher.matches(element)) {
                if (!result) {
                    mismatchDescription.appendText("\n");
                }
                mismatchDescription.appendText("element ").appendValue(element.cssSelector()).appendText(" did not match ");
                matcher.describeMismatch(element, mismatchDescription);
                result = false;
            }
        }
        return result;
    }

    @Override
    public void describeTo(Description description) {
        description.appendDescriptionOf(matcher);
    }
}
