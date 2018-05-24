package org.jsoup.hamcrest;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * Internal API
 */
class PropertyMatcher<V, T> extends TypeSafeDiagnosingMatcher<V> {
    private final Function<V, T> extractor;

    protected final Matcher<T> matcher;

    private final Consumer<Description> description;

    PropertyMatcher(Consumer<Description> description, Function<V, T> extractor, Matcher<T> matcher) {
        this.description = description;
        this.extractor = extractor;
        this.matcher = matcher;
    }

    @Override
    protected boolean matchesSafely(V item, Description mismatchDescription) {
        T subject = extractor.apply(item);
        boolean matches = matcher.matches(subject);
        if (!matches) {
            matcher.describeMismatch(subject, mismatchDescription);
        }
        return matches;
    }

    @Override
    public void describeTo(Description description) {
        this.description.accept(description);
    }
}
