package org.jsoup.hamcrest.fluent;

/**
 * Collection of assertion methods
 */
public class DocumentAssertions {
    private DocumentAssertions() {
    }

    /**
     * Perform assertions on a single {@link org.jsoup.nodes.Element}.
     *
     * Will throw if more than one element is found.
     *
     * @param css the selector to use
     * @return fluent assertions for single element
     */
    public static SingleElementAssertions anElement(String css) {
        return new SingleElementAssertions(css);
    }

    /**
     * Perform assertions on each {@link org.jsoup.nodes.Element} selected by the selector.
     *
     * @param css the selector to use
     * @return fluent assertions for each element
     */
    public static EachElementAssertions eachElement(String css) {
        return new EachElementAssertions(css);
    }

    /**
     * Perform assertions on the {@link org.jsoup.select.Elements} collection returned.
     *
     * @param css the selector to use
     * @return fluent assertions for elements collection
     */
    public static ElementsAssertions elements(String css) {
        return new ElementsAssertions(css);
    }
}
