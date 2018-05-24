package org.jsoup.hamcrest.fluent;

import java.util.function.Consumer;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Matcher;

/**
 * Internal API
 */
class AllOfMatcher<T> extends DiagnosingMatcher<T> {

    private Consumer<Description> description;

    private final Iterable<Matcher<? super T>> matchers;

    AllOfMatcher(Consumer<Description> description, Iterable<Matcher<? super T>> matchers) {
        this.description = description;
        this.matchers = matchers;
    }

    @Override
    protected boolean matches(Object item, Description mismatchDescription) {
        boolean result = true;
        for (Matcher<? super T> matcher : matchers) {
            if (!matcher.matches(item)) {
                mismatchDescription.appendDescriptionOf(matcher).appendText(" ");
                matcher.describeMismatch(item, mismatchDescription);
                result = false;
            }
        }
        return result;
    }

    @Override
    public void describeTo(Description description) {
        this.description.accept(description);
        description.appendList("(", " " + "and" + " ", ")", matchers);
    }
}
