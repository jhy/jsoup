package org.jsoup.hamcrest;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jsoup.nodes.Document;

public class DocumentMatchers {
    private DocumentMatchers(){}

    /**
     * Asserts that the document has the given title
     * @param value expeted title
     * @return matcher
     */
    public static Matcher<Document> hasTitle(String value) {
        return hasTitle(equalTo(value));
    }

    /**
     * Asserts that the document's title matches the given matcher
     * @param matcher expeted title
     * @return matcher
     */
    public static Matcher<Document> hasTitle(Matcher<String> matcher) {
        return new DocumentPropertyMatcher<>(Document::title, matcher,
                description -> description.appendText("document has title ").appendDescriptionOf(matcher));
    }

    private static class DocumentPropertyMatcher<T> extends PropertyMatcher<Document, T> {
        DocumentPropertyMatcher(Function<Document, T> extractor, Matcher<T> matcher, Consumer<Description> description) {
            super(description, extractor, matcher);
        }
    }
}
