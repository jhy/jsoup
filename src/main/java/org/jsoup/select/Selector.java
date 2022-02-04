package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Element;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;

/**
 * CSS-like element selector, that finds elements matching a query.
 *
 * <h2>Selector syntax</h2>
 * <p>
 * A selector is a chain of simple selectors, separated by combinators. Selectors are <b>case insensitive</b> (including against
 * elements, attributes, and attribute values).
 * </p>
 * <p>
 * The universal selector (*) is implicit when no element selector is supplied (i.e. {@code *.header} and {@code .header}
 * is equivalent).
 * </p>
 * <style>table.syntax tr td {vertical-align: top; padding-right: 2em; padding-top:0.5em; padding-bottom:0.5em; } table.syntax tr:hover{background-color: #eee;} table.syntax {border-spacing: 0px 0px;}</style>
 * <table summary="" class="syntax"><colgroup><col span="1" style="width: 20%;"><col span="1" style="width: 40%;"><col span="1" style="width: 40%;"></colgroup>
 * <tr><th align="left">Pattern</th><th align="left">Matches</th><th align="left">Example</th></tr>
 * <tr><td><code>*</code></td><td>any element</td><td><code>*</code></td></tr>
 * <tr><td><code>tag</code></td><td>elements with the given tag name</td><td><code>div</code></td></tr>
 * <tr><td><code>*|E</code></td><td>elements of type E in any namespace (including non-namespaced)</td><td><code>*|name</code> finds <code>&lt;fb:name&gt;</code> and <code>&lt;name&gt;</code> elements</td></tr>
 * <tr><td><code>ns|E</code></td><td>elements of type E in the namespace <i>ns</i></td><td><code>fb|name</code> finds <code>&lt;fb:name&gt;</code> elements</td></tr>
 * <tr><td><code>#id</code></td><td>elements with attribute ID of "id"</td><td><code>div#wrap</code>, <code>#logo</code></td></tr>
 * <tr><td><code>.class</code></td><td>elements with a class name of "class"</td><td><code>div.left</code>, <code>.result</code></td></tr>
 * <tr><td><code>[attr]</code></td><td>elements with an attribute named "attr" (with any value)</td><td><code>a[href]</code>, <code>[title]</code></td></tr>
 * <tr><td><code>[^attrPrefix]</code></td><td>elements with an attribute name starting with "attrPrefix". Use to find elements with HTML5 datasets</td><td><code>[^data-]</code>, <code>div[^data-]</code></td></tr>
 * <tr><td><code>[attr=val]</code></td><td>elements with an attribute named "attr", and value equal to "val"</td><td><code>img[width=500]</code>, <code>a[rel=nofollow]</code></td></tr>
 * <tr><td><code>[attr=&quot;val&quot;]</code></td><td>elements with an attribute named "attr", and value equal to "val"</td><td><code>span[hello="Cleveland"][goodbye="Columbus"]</code>, <code>a[rel=&quot;nofollow&quot;]</code></td></tr>
 * <tr><td><code>[attr^=valPrefix]</code></td><td>elements with an attribute named "attr", and value starting with "valPrefix"</td><td><code>a[href^=http:]</code></td></tr>
 * <tr><td><code>[attr$=valSuffix]</code></td><td>elements with an attribute named "attr", and value ending with "valSuffix"</td><td><code>img[src$=.png]</code></td></tr>
 * <tr><td><code>[attr*=valContaining]</code></td><td>elements with an attribute named "attr", and value containing "valContaining"</td><td><code>a[href*=/search/]</code></td></tr>
 * <tr><td><code>[attr~=<em>regex</em>]</code></td><td>elements with an attribute named "attr", and value matching the regular expression</td><td><code>img[src~=(?i)\\.(png|jpe?g)]</code></td></tr>
 * <tr><td></td><td>The above may be combined in any order</td><td><code>div.header[title]</code></td></tr>
 * <tr><td><td colspan="3"><h3>Combinators</h3></td></tr>
 * <tr><td><code>E F</code></td><td>an F element descended from an E element</td><td><code>div a</code>, <code>.logo h1</code></td></tr>
 * <tr><td><code>E {@literal >} F</code></td><td>an F direct child of E</td><td><code>ol {@literal >} li</code></td></tr>
 * <tr><td><code>E + F</code></td><td>an F element immediately preceded by sibling E</td><td><code>li + li</code>, <code>div.head + div</code></td></tr>
 * <tr><td><code>E ~ F</code></td><td>an F element preceded by sibling E</td><td><code>h1 ~ p</code></td></tr>
 * <tr><td><code>E, F, G</code></td><td>all matching elements E, F, or G</td><td><code>a[href], div, h3</code></td></tr>
 * <tr><td><td colspan="3"><h3>Pseudo selectors</h3></td></tr>
 * <tr><td><code>:lt(<em>n</em>)</code></td><td>elements whose sibling index is less than <em>n</em></td><td><code>td:lt(3)</code> finds the first 3 cells of each row</td></tr>
 * <tr><td><code>:gt(<em>n</em>)</code></td><td>elements whose sibling index is greater than <em>n</em></td><td><code>td:gt(1)</code> finds cells after skipping the first two</td></tr>
 * <tr><td><code>:eq(<em>n</em>)</code></td><td>elements whose sibling index is equal to <em>n</em></td><td><code>td:eq(0)</code> finds the first cell of each row</td></tr>
 * <tr><td><code>:has(<em>selector</em>)</code></td><td>elements that contains at least one element matching the <em>selector</em></td><td><code>div:has(p)</code> finds <code>div</code>s that contain <code>p</code> elements.<br><code>div:has(&gt; a)</code> selects <code>div</code> elements that have at least one direct child <code>a</code> element.</td></tr>
 * <tr><td><code>:not(<em>selector</em>)</code></td><td>elements that do not match the <em>selector</em>. See also {@link Elements#not(String)}</td><td><code>div:not(.logo)</code> finds all divs that do not have the "logo" class.<p><code>div:not(:has(div))</code> finds divs that do not contain divs.</p></td></tr>
 * <tr><td><code>:contains(<em>text</em>)</code></td><td>elements that contains the specified text. The search is case insensitive. The text may appear in the found element, or any of its descendants. The text is whitespace normalized. <p>To find content that includes parentheses, escape those with a {@code \}.</p></td><td><code>p:contains(jsoup)</code> finds p elements containing the text "jsoup".<p>{@code p:contains(hello \(there\) finds p elements containing the text "Hello (There)"}</p></td></tr>
 * <tr><td><code>:containsOwn(<em>text</em>)</code></td><td>elements that directly contain the specified text. The search is case insensitive. The text must appear in the found element, not any of its descendants.</td><td><code>p:containsOwn(jsoup)</code> finds p elements with own text "jsoup".</td></tr>
 * <tr><td><code>:containsData(<em>data</em>)</code></td><td>elements that contains the specified <em>data</em>. The contents of {@code script} and {@code style} elements, and {@code comment} nodes (etc) are considered data nodes, not text nodes. The search is case insensitive. The data may appear in the found element, or any of its descendants.</td><td><code>script:contains(jsoup)</code> finds script elements containing the data "jsoup".</td></tr>
 * <tr><td><code>:containsWholeText(<em>text</em>)</code></td><td>elements that contains the specified <b>non-normalized</b> text. The search is case sensitive, and will match exactly against spaces and newlines found in the original input. The text may appear in the found element, or any of its descendants. <p>To find content that includes parentheses, escape those with a {@code \}.</p></td><td><code>p:containsWholeText(jsoup\nThe Java HTML Parser)</code> finds p elements containing the text <code>"jsoup\nThe Java HTML Parser"</code> (and not other variations of whitespace or casing, as <code>:contains()</code> would. Note that {@code br} elements are presented as a newline.</p></td></tr>
 * <tr><td><code>:containsWholeOwnText(<em>text</em>)</code></td><td>elements that <b>directly</b> contain the specified <b>non-normalized</b> text. The search is case sensitive, and will match exactly against spaces and newlines found in the original input. The text may appear in the found element, but not in its descendants. <p>To find content that includes parentheses, escape those with a {@code \}.</p></td><td><code>p:containsWholeOwnText(jsoup\nThe Java HTML Parser)</code> finds p elements directly containing the text <code>"jsoup\nThe Java HTML Parser"</code> (and not other variations of whitespace or casing, as <code>:contains()</code> would. Note that {@code br} elements are presented as a newline.</p></td></tr>
 * <tr><td><code>:matches(<em>regex</em>)</code></td><td>elements containing <b>whitespace normalized</b> text that matches the specified regular expression. The text may appear in the found element, or any of its descendants.</td><td><code>td:matches(\\d+)</code> finds table cells containing digits. <code>div:matches((?i)login)</code> finds divs containing the text, case insensitively.</td></tr>
 * <tr><td><code>:matchesWholeText(<em>regex</em>)</code></td><td>elements containing <b>non-normalized</b> whole text that matches the specified regular expression. The text may appear in the found element, or any of its descendants.</td><td><code>td:matchesWholeText(\\s{2,})</code> finds table cells a run of at least two space characters.</td></tr>
 * <tr><td><code>:matchesWholeOwnText(<em>regex</em>)</code></td><td>elements whose own <b>non-normalized</b> whole text matches the specified regular expression. The text must appear in the found element, not any of its descendants.</td><td><code>td:matchesWholeOwnText(\n\\d+)</code> finds table cells directly containing digits following a neewline.</td></tr>
 * <tr><td></td><td>The above may be combined in any order and with other selectors</td><td><code>.light:contains(name):eq(0)</code></td></tr>
 * <tr><td><code>:matchText</code></td><td>treats text nodes as elements, and so allows you to match against and select text nodes.<p><b>Note</b> that using this selector will modify the DOM, so you may want to {@code clone} your document before using.</td><td>{@code p:matchText:firstChild} with input {@code <p>One<br />Two</p>} will return one {@link org.jsoup.nodes.PseudoTextElement} with text "{@code One}".</td></tr>
 * <tr><td colspan="3"><h3>Structural pseudo selectors</h3></td></tr>
 * <tr><td><code>:root</code></td><td>The element that is the root of the document. In HTML, this is the <code>html</code> element</td><td><code>:root</code></td></tr>
 * <tr><td><code>:nth-child(<em>a</em>n+<em>b</em>)</code></td><td><p>elements that have <code><em>a</em>n+<em>b</em>-1</code> siblings <b>before</b> it in the document tree, for any positive integer or zero value of <code>n</code>, and has a parent element. For values of <code>a</code> and <code>b</code> greater than zero, this effectively divides the element's children into groups of a elements (the last group taking the remainder), and selecting the <em>b</em>th element of each group. For example, this allows the selectors to address every other row in a table, and could be used to alternate the color of paragraph text in a cycle of four. The <code>a</code> and <code>b</code> values must be integers (positive, negative, or zero). The index of the first child of an element is 1.</p>
 * In addition to this, <code>:nth-child()</code> can take <code>odd</code> and <code>even</code> as arguments instead. <code>odd</code> has the same signification as <code>2n+1</code>, and <code>even</code> has the same signification as <code>2n</code>.</td><td><code>tr:nth-child(2n+1)</code> finds every odd row of a table. <code>:nth-child(10n-1)</code> the 9th, 19th, 29th, etc, element. <code>li:nth-child(5)</code> the 5h li</td></tr>
 * <tr><td><code>:nth-last-child(<em>a</em>n+<em>b</em>)</code></td><td>elements that have <code><em>a</em>n+<em>b</em>-1</code> siblings <b>after</b> it in the document tree. Otherwise like <code>:nth-child()</code></td><td><code>tr:nth-last-child(-n+2)</code> the last two rows of a table</td></tr>
 * <tr><td><code>:nth-of-type(<em>a</em>n+<em>b</em>)</code></td><td>pseudo-class notation represents an element that has <code><em>a</em>n+<em>b</em>-1</code> siblings with the same expanded element name <em>before</em> it in the document tree, for any zero or positive integer value of n, and has a parent element</td><td><code>img:nth-of-type(2n+1)</code></td></tr>
 * <tr><td><code>:nth-last-of-type(<em>a</em>n+<em>b</em>)</code></td><td>pseudo-class notation represents an element that has <code><em>a</em>n+<em>b</em>-1</code> siblings with the same expanded element name <em>after</em> it in the document tree, for any zero or positive integer value of n, and has a parent element</td><td><code>img:nth-last-of-type(2n+1)</code></td></tr>
 * <tr><td><code>:first-child</code></td><td>elements that are the first child of some other element.</td><td><code>div {@literal >} p:first-child</code></td></tr>
 * <tr><td><code>:last-child</code></td><td>elements that are the last child of some other element.</td><td><code>ol {@literal >} li:last-child</code></td></tr>
 * <tr><td><code>:first-of-type</code></td><td>elements that are the first sibling of its type in the list of children of its parent element</td><td><code>dl dt:first-of-type</code></td></tr>
 * <tr><td><code>:last-of-type</code></td><td>elements that are the last sibling of its type in the list of children of its parent element</td><td><code>tr {@literal >} td:last-of-type</code></td></tr>
 * <tr><td><code>:only-child</code></td><td>elements that have a parent element and whose parent element have no other element children</td><td></td></tr>
 * <tr><td><code>:only-of-type</code></td><td> an element that has a parent element and whose parent element has no other element children with the same expanded element name</td><td></td></tr>
 * <tr><td><code>:empty</code></td><td>elements that have no children at all</td><td></td></tr>
 * </table>
 *
 * <p>A word on using regular expressions in these selectors: depending on the content of the regex, you will need to quote the pattern using <b><code>Pattern.quote("regex")</code></b> for it to parse correclty through both the selector parser and the regex parser. E.g. <code>String query = "div:matches(" + Pattern.quote(regex) + ");"</code>.</p>
 *
 * @author Jonathan Hedley, jonathan@hedley.net
 * @see Element#select(String)
 */
public class Selector {
    // not instantiable
    private Selector() {}

    /**
     * Find elements matching selector.
     *
     * @param query CSS selector
     * @param root  root element to descend into
     * @return matching elements, empty if none
     * @throws Selector.SelectorParseException (unchecked) on an invalid CSS query.
     */
    public static Elements select(String query, Element root) {
        Validate.notEmpty(query);
        return select(QueryParser.parse(query), root);
    }

    /**
     * Find elements matching selector.
     *
     * @param evaluator CSS selector
     * @param root root element to descend into
     * @return matching elements, empty if none
     */
    public static Elements select(Evaluator evaluator, Element root) {
        Validate.notNull(evaluator);
        Validate.notNull(root);
        return Collector.collect(evaluator, root);
    }

    /**
     * Find elements matching selector.
     *
     * @param query CSS selector
     * @param roots root elements to descend into
     * @return matching elements, empty if none
     */
    public static Elements select(String query, Iterable<Element> roots) {
        Validate.notEmpty(query);
        Validate.notNull(roots);
        Evaluator evaluator = QueryParser.parse(query);
        Elements elements = new Elements();
        IdentityHashMap<Element, Boolean> seenElements = new IdentityHashMap<>();
        // dedupe elements by identity, not equality

        for (Element root : roots) {
            final Elements found = select(evaluator, root);
            for (Element el : found) {
                if (seenElements.put(el, Boolean.TRUE) == null) {
                    elements.add(el);
                }
            }
        }
        return elements;
    }

    // exclude set. package open so that Elements can implement .not() selector.
    static Elements filterOut(Collection<Element> elements, Collection<Element> outs) {
        Elements output = new Elements();
        for (Element el : elements) {
            boolean found = false;
            for (Element out : outs) {
                if (el.equals(out)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                output.add(el);
        }
        return output;
    }

    /**
     * Find the first element that matches the query.
     * @param cssQuery CSS selector
     * @param root root element to descend into
     * @return the matching element, or <b>null</b> if none.
     */
    public static @Nullable Element selectFirst(String cssQuery, Element root) {
        Validate.notEmpty(cssQuery);
        return Collector.findFirst(QueryParser.parse(cssQuery), root);
    }

    public static class SelectorParseException extends IllegalStateException {
        public SelectorParseException(String msg) {
            super(msg);
        }

        public SelectorParseException(String msg, Object... params) {
            super(String.format(msg, params));
        }
    }
}
