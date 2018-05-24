package org.jsoup.hamcrest.fluent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jsoup.hamcrest.DocumentMatcher;
import org.jsoup.hamcrest.ElementMatchers;
import org.jsoup.nodes.Element;

/**
 * Base class for element based assertions.
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractElementAssertions<T extends AbstractElementAssertions> implements DocumentMatcher {

    private List<Matcher<? super Element>> matchers = new ArrayList<>();

    private final String css;

    AbstractElementAssertions(String css) {
        this.css = css;
    }

    protected List<Matcher<? super Element>> getMatchers() {
        return matchers;
    }

    protected String getCss() {
        return css;
    }

    @SuppressWarnings("unchecked")
    private T self() {
        return (T) this;
    }


    /**
     * Asserts that the element has given value.
     *
     * See also {@link Element#val()}
     * @param value expected value
     * @return this for fluent chaining
     */
    public T hasValue(String value) {
        matchers.add(ElementMatchers.hasValue(value));
        return self();
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the value.
     *
     * See also {@link Element#val()}
     * @param valueMatcher hamcrest matcher to use
     * @return this for fluent chaining
     */
    public T hasValue(Matcher<? super String> valueMatcher) {
        matchers.add(ElementMatchers.hasValue(valueMatcher));
        return self();
    }

    /**
     * Asserts that the element has a given attribute, e.g. 'disabled'
     *
     * See also {@link Element#hasAttr(String)}
     * @param attribute name of the attribute to check for existence
     * @return this for fluent chaining
     */
    public T hasAttribute(String attribute) {
        matchers.add(ElementMatchers.hasAttribute(attribute));
        return self();
    }

    /**
     * Asserts that the element has the attribute with the given value.
     *
     * See also {@link Element#attr(String)}
     * @param attribute name of the attribute
     * @param value expected value
     * @return this for fluent chaining
     */
    public T hasAttribute(String attribute, String value) {
        matchers.add(ElementMatchers.hasAttribute(attribute, value));
        return self();
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the value of the element's attribute .
     *
     * See also {@link Element#attr(String)}
     * @param attribute name of the attribute
     * @param valueMatcher the matcher to use for the attribute value
     * @return this for fluent chaining
     */
    public T hasAttribute(String attribute, Matcher<? super String> valueMatcher) {
        matchers.add(ElementMatchers.hasAttribute(attribute, valueMatcher));
        return self();
    }

    /**
     * Asserts that the element has the given css class
     *
     * See also {@link Element#hasClass(String)}
     * @param cssClass expected name of the class
     * @return this for fluent chaining
     */
    public T hasCssClass(String cssClass) {
        matchers.add(ElementMatchers.hasCssClass(cssClass));
        return self();
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the set of class names.
     *
     * See also {@link Element#classNames()}
     * @param matcher hamcrest matcher to use
     * @return this for fluent chaining
     */
    public T hasCss(Matcher<? super Set<String>> matcher) {
        matchers.add(ElementMatchers.hasCss(matcher));
        return self();
    }

    /**
     * Asserts that the element has the given text (including child elements).
     *
     * See also {@link Element#text()}
     * @param text expected text
     * @return this for fluent chaining
     */
    public T hasText(String text) {
        matchers.add(ElementMatchers.hasText(text));
        return self();
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the text (including child elements).
     *
     * See also {@link Element#text()}
     * @param textMatcher hamcrest matcher to use
     * @return this for fluent chaining
     */
    public T hasText(Matcher<? super String> textMatcher) {
        matchers.add(ElementMatchers.hasText(textMatcher));
        return self();
    }

    /**
     * Asserts that the element has the given text (excluding child elements).
     *
     * See also {@link Element#ownText()}
     * @param text expected text
     * @return this for fluent chaining
     */
    public T hasOwnText(String text) {
        matchers.add(ElementMatchers.hasOwnText(text));
        return self();
    }

    /**
     * Uses the given a Hamcrest Matcher to assert the text (excluding child elements).
     *
     * See also {@link Element#ownText()}
     * @param textMatcher hamcrest matcher to use
     * @return this for fluent chaining
     */
    public T hasOwnText(Matcher<? super String> textMatcher) {
        matchers.add(ElementMatchers.hasOwnText(textMatcher));
        return self();
    }

    /**
     * Asserts that the element has the given data. This is used to check the content of scripts, comments, CSS styles, etc.
     *
     * Use {@link #hasAttribute(String, String)} if you want to assert data attributes.
     *
     * See also {@link Element#data()}
     * @param data expected text
     * @return this for fluent chaining
     */
    public T hasData(String data) {
        matchers.add(ElementMatchers.hasData(data));
        return self();
    }

    /**
     * Asserts that the element has the given data using a hamcrest matcher.
     * This is used to check the content of scripts, comments, CSS styles, etc.
     *
     * Use {@link #hasAttribute(String, Matcher)} if you want to assert data attributes.
     *
     * See also {@link Element#data()}
     * @param dataMatcher hamcrest matcher to use
     * @return this for fluent chaining
     */
    public T hasData(Matcher<? super String> dataMatcher) {
        matchers.add(ElementMatchers.hasData(dataMatcher));
        return self();
    }

    /**
     * Let's you construct your own powerful matchers with relative ease.
     *
     * If you want to have an reusable matcher use {@link ElementMatchers#predicate(Predicate, Consumer, BiConsumer)}
     * in conjunction with {@link #matches(Matcher)}.
     *
     * @param predicate custom condition
     * @param description the description of the condition
     * @param mismatchDescription the description if the condition did not match
     * @return this for fluent chaining
     */
    public T predicate(Predicate<Element> predicate,
            Consumer<Description> description,
            BiConsumer<Element, Description> mismatchDescription) {
        matchers.add(ElementMatchers.predicate(predicate, description, mismatchDescription));
        return self();
    }

    /**
     * Apply the given matcher against the element.
     *
     * @param matcher the matcher to use
     * @return this for fluent chaining
     */
    public T matches(Matcher<? super Element> matcher) {
        matchers.add(matcher);
        return self();
    }
}
