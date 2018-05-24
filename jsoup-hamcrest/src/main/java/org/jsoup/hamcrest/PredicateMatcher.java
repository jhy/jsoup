package org.jsoup.hamcrest;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/**
 * Internal API
 */
class PredicateMatcher<T> extends TypeSafeDiagnosingMatcher<T> {
    private final Predicate<T> predicate;

    private final Consumer<Description> description;
    private final BiConsumer<T, Description> mismatchDescription;

    PredicateMatcher(Predicate<T> predicate, Consumer<Description> description, BiConsumer<T, Description>
            mismatchDescription) {
        this.predicate = predicate;
        this.description = description;
        this.mismatchDescription = mismatchDescription;
    }


    @Override
    public void describeTo(Description description) {
        this.description.accept(description);
    }

    @Override
    protected boolean matchesSafely(T item, Description mismatchDescription) {
        boolean match = predicate.test(item);
        if (!match) {
            this.mismatchDescription.accept(item, mismatchDescription);
        }
        return match;
    }
}
