package org.jsoup.hamcrest;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;

public class ElementMatchers {
    private ElementMatchers(){}

    /**
     * Asserts that the element has given value.
     *
     * See also {@link Element#val()}
     * @param value expected value
     * @return matcher
     */
    public static Matcher<Element> hasValue(String value) {
        return hasValue(equalTo(value));
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the value.
     *
     * See also {@link Element#val()}
     * @param valueMatcher hamcrest matcher to use
     * @return matcher asserting this
     */
    public static Matcher<Element> hasValue(Matcher<? super String> valueMatcher) {
        return valueMatcher(valueMatcher);
    }

    /**
     * Asserts that the element has a given attribute, e.g. 'disabled'
     *
     * See also {@link Element#hasAttr(String)}
     * @param attribute name of the attribute to check for existence
     * @return matcher asserting this
     */
    public static Matcher<Element> hasAttribute(String attribute) {
        return attributesMatcher(new PredicateMatcher<>(
                item -> item.hasKey(attribute),
                description -> description.appendText("has attribute ").appendValue(attribute).appendText(" defined"),
                (item, mismatch) -> mismatch.appendText("attribute was not preset")));
    }

    /**
     * Asserts that the element has the attribute with the given value.
     *
     * See also {@link Element#attr(String)}
     * @param attribute name of the attribute
     * @param value expected value
     * @return matcher asserting this
     */
    public static Matcher<Element> hasAttribute(String attribute, String value) {
        return hasAttribute(attribute, equalTo(value));
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the value of the element's attribute .
     *
     * See also {@link Element#attr(String)}
     * @param attribute name of the attribute
     * @param valueMatcher the matcher to use for the attribute value
     * @return matcher asserting this
     */
    public static Matcher<Element> hasAttribute(String attribute, Matcher<? super String> valueMatcher) {
        return attributesMatcher(new PredicateMatcher<>(
                item -> valueMatcher.matches(item.get(attribute)),
                description -> description.appendText("has attribute ").appendValue(attribute)
                        .appendText(" with value ").appendDescriptionOf(valueMatcher),
                (item, mismatch) -> mismatch.appendText("attribute value was ").appendValue(item.get(attribute))));
    }

    /**
     * Asserts that the element has the given css class
     *
     * See also {@link Element#hasClass(String)}
     * @param cssClass expected name of the class
     * @return matcher asserting this
     */
    public static Matcher<Element> hasCssClass(String cssClass) {
        return hasCss(hasItem(cssClass));
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the set of class names.
     *
     * See also {@link Element#classNames()}
     * @param matcher hamcrest matcher to use
     * @return matcher asserting this
     */
    public static Matcher<Element> hasCss(Matcher<? super Set<String>> matcher) {
        return cssClassMatcher(matcher);
    }

    /**
     * Asserts that the element has the given text (including child elements).
     *
     * See also {@link Element#text()}
     * @param text expected text
     * @return matcher asserting this
     */
    public static Matcher<Element> hasText(String text) {
        return hasText(equalTo(text));
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the text (including child elements).
     *
     * See also {@link Element#text()}
     * @param textMatcher hamcrest matcher to use
     * @return matcher asserting this
     */
    public static Matcher<Element> hasText(Matcher<? super String> textMatcher) {
        return textMatcher(textMatcher);
    }

    /**
     * Asserts that the element has the given text (excluding child elements).
     *
     * See also {@link Element#ownText()}
     * @param text expected text
     * @return matcher asserting this
     */
    public static Matcher<Element> hasOwnText(String text) {
        return hasOwnText(equalTo(text));
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the text (excluding child elements).
     *
     * See also {@link Element#ownText()}
     * @param textMatcher hamcrest matcher to use
     * @return matcher asserting this
     */
    public static Matcher<Element> hasOwnText(Matcher<? super String> textMatcher) {
        return ownTextMatcher(textMatcher);
    }

    /**
     * Asserts that the element has the given data. This is used to check the content of scripts, comments, CSS styles, etc.
     *
     * Use {@link #hasAttribute(String, String)} if you want to assert data attributes.
     *
     * See also {@link Element#data()}
     * @param data expected text
     * @return matcher asserting this
     */
    public static Matcher<Element> hasData(String data) {
        return hasData(equalTo(data));
    }

    /**
     * Asserts that the element has the given data using a hamcrest matcher.
     * This is used to check the content of scripts, comments, CSS styles, etc.
     *
     * Use {@link #hasAttribute(String, Matcher)} if you want to assert data attributes.
     *
     * See also {@link Element#data()}
     * @param dataMatcher hamcrest matcher to use
     * @return matcher asserting this
     */
    public static Matcher<Element> hasData(Matcher<? super String> dataMatcher) {
        return dataMatcher(dataMatcher);
    }

    /**
     * Let's you construct your own powerful matchers with relative ease.
     *
     * @param predicate custom condition
     * @param description the description of the condition
     * @param mismatchDescription the description if the condition did not match
     * @return matcher asserting this
     */
    public static Matcher<Element> predicate(Predicate<Element> predicate,
            Consumer<Description> description,
            BiConsumer<Element, Description> mismatchDescription) {
        return new PredicateMatcher<>(predicate, description, mismatchDescription);
    }

    private static Matcher<Element> attributesMatcher(Matcher<? super Attributes> matcher) {
        return new ElementPropertyMatcher<>(Element::attributes, matcher,
                description -> description.appendText("element attributes ").appendDescriptionOf(matcher));
    }


    private static Matcher<Element> cssClassMatcher(Matcher<? super Set<String>> matcher) {
        return new ElementPropertyMatcher<>(Element::classNames, matcher,
                description -> description.appendText("element css classes ").appendDescriptionOf(matcher));
    }

    private static Matcher<Element> valueMatcher(Matcher<? super String> matcher) {
        return new ElementPropertyMatcher<>(Element::val, matcher,
                description -> description.appendText("element value ").appendDescriptionOf(matcher));
    }

    private static Matcher<Element> textMatcher(Matcher<? super String> matcher) {
        return new ElementPropertyMatcher<>(Element::text, matcher,
                description -> description.appendText("text ").appendDescriptionOf(matcher));
    }

    private static Matcher<Element> ownTextMatcher(Matcher<? super String> matcher) {
        return new ElementPropertyMatcher<>(Element::ownText, matcher,
                description -> description.appendText("own text ").appendDescriptionOf(matcher));
    }

    private static Matcher<Element> dataMatcher(Matcher<? super String> matcher) {
        return new ElementPropertyMatcher<>(Element::data, matcher,
                description -> description.appendText("data ").appendDescriptionOf(matcher));
    }

    private static class ElementPropertyMatcher<T> extends PropertyMatcher<Element, T> {
        ElementPropertyMatcher(Function<Element, T> extractor, Matcher<T> matcher, Consumer<Description> description) {
            super(description, extractor, matcher);
        }
    }
}
