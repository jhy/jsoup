package org.jsoup.select;

import org.jsoup.helper.Validate;
import org.jsoup.internal.QueryMode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.TokenQueue;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 CSS queries for finding elements and nodes.
 <p>This reference describes jsoup 1.24.1's default query behavior. See <a href="#query-compatibility">Query compatibility changes</a>
 for Web mode and the <a href="https://jsoup.org/news/selector-migration?utm_source=jsoup&amp;utm_medium=javadoc">selector migration guide</a> for how to test and update
 your queries.</p>

 <h2 id="using-selectors">Using selectors</h2>
 <p>Call {@link Element#select(String)} to find matching descendants and, if it matches, the element it is called on. For example, given
 {@code <div id="outer"><div id="inner"></div></div>}, {@code outer.select("div")} returns both divs, while
 {@code outer.select(":scope > div")} returns only the inner div.</p>
 <p>{@link Element#selectFirst(String)} returns the first match. {@link Element#is(String)} checks whether the element itself
 matches the query. {@link Element#closest(String)} finds the nearest matching element: itself or an ancestor.
 To retrieve nodes such as text and comments, use {@link Element#selectNodes(String)}.</p>
 <p>You can try queries in the <a href="https://try.jsoup.org/?utm_source=jsoup&amp;utm_medium=javadoc">Try jsoup online playground</a>.</p>

 <h2 id="selector-syntax">Selector syntax</h2>
 <p>Combine simple selectors to narrow a match, and separate them with combinators to describe relationships.
 For example, {@code div.header[title] > a} finds links directly inside a div with class "header" and a title attribute.
 The universal selector {@code *} is implicit: {@code .header} and {@code *.header} are equivalent.</p>
 <p>See <a href="#element-attribute-selectors">element and attribute selectors</a>,
 <a href="#combinators">combinators</a>, <a href="#logical-selectors">logical selectors</a>,
 <a href="#structural-selectors">structural selectors</a>, <a href="#text-regex-selectors">text, regex, and index selectors</a>,
 and <a href="#node-selectors">node selectors</a>.</p>
 <p>The examples in the tables are CSS queries. When writing a query in a Java string literal, double each backslash;
 see <a href="#escaping-selectors">Escaping and regular expressions</a>.</p>

 <style>
 table.syntax {border-spacing: 0;}
 table.syntax th {text-align: left; width: 40%;}
 table.syntax th:first-child {width: 20%;}
 table.syntax td {vertical-align: top; padding: 0.5em 2em 0.5em 0;}
 table.syntax tr:hover {background-color: #eee;}
 </style>

 <h3 id="element-attribute-selectors">Element and attribute selectors</h3>
 <table class="syntax">
 <tr><th>Pattern</th><th>Matches</th><th>Example</th></tr>
 <tr><td><code>*</code></td><td>any element</td><td><code>*</code></td></tr>
 <tr><td><code>tag</code></td><td>elements with the given tag name</td><td><code>div</code></td></tr>
 <tr><td><code>*|E</code></td><td>elements of type E in any namespace (including non-namespaced)</td><td><code>*|name</code> finds <code>&lt;dc:name&gt;</code> and <code>&lt;name&gt;</code> elements</td></tr>
 <tr><td><code>ns|E</code></td><td>elements of type E in the namespace <i>ns</i></td><td><code>dc|name</code> finds <code>&lt;dc:name&gt;</code> elements</td></tr>
 <tr><td><code>ns|*</code></td><td>all elements in the namespace <i>ns</i></td><td><code>dc|*</code> finds <code>&lt;dc:p&gt;</code> and <code>&lt;dc:img&gt;</code> elements</td></tr>
 <tr><td><code>#id</code></td><td>elements with attribute ID of "id"</td><td><code>div#wrap</code>, <code>#logo</code></td></tr>
 <tr><td><code>.class</code></td><td>elements with a class name of "class"</td><td><code>div.left</code>, <code>.result</code></td></tr>
 <tr><td><code>[attr]</code></td><td>elements with an attribute named "attr" (with any value)</td><td><code>a[href]</code>, <code>[title]</code></td></tr>
 <tr><td><code>[^attrPrefix]</code></td><td>elements with an attribute name starting with "attrPrefix". Use to find elements with HTML5 datasets</td><td><code>[^data-]</code>, <code>div[^data-]</code></td></tr>
 <tr><td><code>[attr=val]</code></td><td>elements with an attribute named "attr", and value equal to "val"</td><td><code>img[width="500"]</code>, <code>a[rel=nofollow]</code></td></tr>
 <tr><td><code>[attr!=val]</code></td><td>elements whose attribute value differs from "val", including elements without the attribute</td><td><code>a[rel!=nofollow]</code></td></tr>
 <tr><td><code>[attr^=valPrefix]</code></td><td>elements with an attribute named "attr", and value starting with "valPrefix"</td><td><code>a[href^="http:"]</code></td></tr>
 <tr><td><code>[attr$=valSuffix]</code></td><td>elements with an attribute named "attr", and value ending with "valSuffix"</td><td><code>img[src$=".png"]</code></td></tr>
 <tr><td><code>[attr*=valContaining]</code></td><td>elements with an attribute named "attr", and value containing "valContaining"</td><td><code>a[href*="/search/"]</code></td></tr>
 <tr><td><code>[attr~=<em>regex</em>]</code></td><td>elements with an attribute value matching the regular expression; see <a href="#query-compatibility">the upcoming change to token matching</a></td><td><code>img[src~=(?i)\.(png|jpe?g)]</code></td></tr>
 <tr><td><code>[attr|=val]</code></td><td>matches an exact value or that value followed by a hyphen</td><td><code>[lang|=en]</code> matches "en" and "en-US"</td></tr>
 <tr><td><code>[attr=val i]</code>, <code>[attr=val s]</code></td><td><code>i</code> ignores ASCII case in the attribute value; <code>s</code> requires exact case. Neither changes attribute-name matching. Also applies to substring and hyphen operators; see <a href="#preview-case">case matching</a></td><td><code>[data-sku="AbC123" i]</code>, <code>[type="TEXT" s]</code></td></tr>
 <tr><td><code>:matchesAttr(attr, regex)</code></td><td>elements with an attribute value containing a regex match</td><td><code>img:matchesAttr(src, (?i)\.(png|jpe?g))</code></td></tr>
 <tr><td><code>[*]</code></td><td>elements with any attribute</td><td><code>p[*]</code> finds <code>p</code> elements that have at least one attribute; <code>p:not([*])</code> finds those with no attributes</td></tr>
 </table>

 <h3 id="combinators">Combinators</h3>
 <table class="syntax">
 <tr><th>Pattern</th><th>Matches</th><th>Example</th></tr>
 <tr><td><code>E F</code></td><td>an F element descended from an E element</td><td><code>div a</code>, <code>.logo h1</code></td></tr>
 <tr><td><code>E {@literal >} F</code></td><td>an F direct child of E</td><td><code>ol {@literal >} li</code></td></tr>
 <tr><td><code>E + F</code></td><td>an F element immediately preceded by sibling E</td><td><code>li + li</code>, <code>div.head + div</code></td></tr>
 <tr><td><code>E ~ F</code></td><td>an F element preceded by sibling E</td><td><code>h1 ~ p</code></td></tr>
 <tr><td><code>E, F, G</code></td><td>all matching elements E, F, or G</td><td><code>a[href], div, h3</code></td></tr>
 </table>

 <h3 id="logical-selectors">Logical selectors</h3>
 <table class="syntax">
 <tr><th>Pattern</th><th>Matches</th><th>Example</th></tr>
 <tr><td><code>:has(<em>selector</em>)</code></td><td>elements that contain at least one element matching the <em>selector</em></td><td><code>div:has(p)</code> finds <code>div</code>s that contain <code>p</code> elements.<br><code>div:has(&gt; a)</code> selects <code>div</code> elements that have at least one direct child <code>a</code> element.<br><code>section:has(h1, h2)</code> finds <code>section</code> elements that contain a <code>h1</code> or a <code>h2</code> element</td></tr>
 <tr><td><code>:is(<em>selector list</em>)</code></td><td>elements that match any of the selectors in the selector list</td><td><code>:is(h1, h2, h3, h4, h5, h6)</code> finds any heading element.<br><code>:is(section, article) &gt; :is(h1, h2)</code> finds a <code>h1</code> or <code>h2</code> that is a direct child of a <code>section</code> or an <code>article</code></td></tr>
 <tr><td><code>:not(<em>selector</em>)</code></td><td>elements that do not match the <em>selector</em>. See also {@link Elements#not(String)}</td><td><code>div:not(.logo)</code> finds all divs that do not have the "logo" class.<p><code>div:not(:has(div))</code> finds divs that do not contain divs.</p></td></tr>
 </table>

 <h3 id="structural-selectors">Structural selectors</h3>
 <table class="syntax">
 <tr><th>Pattern</th><th>Matches</th><th>Example</th></tr>
 <tr><td><code>:scope</code></td><td>The element you query from, or the document element (usually <code>html</code>) when querying a Document</td><td><code>:scope &gt; p</code> finds direct child paragraphs</td></tr>
 <tr><td><code>:root</code></td><td>The tree's root element: the document element, or the topmost element of a detached tree</td><td><code>:root</code></td></tr>
 <tr><td><code>:nth-child(<em>a</em>n+<em>b</em>)</code></td><td>elements at child positions <em>a</em>n+<em>b</em>, for any integer n starting at zero. Positions start at 1 and count element siblings. Use <code>odd</code> for <code>2n+1</code>, or <code>even</code> for <code>2n</code></td><td><code>tr:nth-child(2n+1)</code> finds odd rows; <code>li:nth-child(5)</code> finds the fifth list item; <code>:nth-child(10n-1)</code> finds positions 9, 19, 29, etc.</td></tr>
 <tr><td><code>:nth-last-child(<em>a</em>n+<em>b</em>)</code></td><td>like <code>:nth-child()</code>, counting from the last sibling</td><td><code>tr:nth-last-child(-n+2)</code> finds the last two rows</td></tr>
 <tr><td><code>:nth-of-type(<em>a</em>n+<em>b</em>)</code></td><td>like <code>:nth-child()</code>, counting only siblings of the same type</td><td><code>img:nth-of-type(2n+1)</code></td></tr>
 <tr><td><code>:nth-last-of-type(<em>a</em>n+<em>b</em>)</code></td><td>like <code>:nth-of-type()</code>, counting from the last sibling</td><td><code>img:nth-last-of-type(2n+1)</code></td></tr>
 <tr><td><code>:first-child</code></td><td>elements that are the first child of some other element.</td><td><code>div {@literal >} p:first-child</code></td></tr>
 <tr><td><code>:last-child</code></td><td>elements that are the last child of some other element.</td><td><code>ol {@literal >} li:last-child</code></td></tr>
 <tr><td><code>:first-of-type</code></td><td>elements that are the first sibling of its type in the list of children of its parent element</td><td><code>dl dt:first-of-type</code></td></tr>
 <tr><td><code>:last-of-type</code></td><td>elements that are the last sibling of its type in the list of children of its parent element</td><td><code>tr {@literal >} td:last-of-type</code></td></tr>
 <tr><td><code>:only-child</code></td><td>elements with a parent and no element siblings</td><td><code>p:only-child</code></td></tr>
 <tr><td><code>:only-of-type</code></td><td>elements with a parent and no siblings of the same type</td><td><code>p:only-of-type</code></td></tr>
 <tr><td><code>:empty</code></td><td>elements that contain no child elements or nodes, with the exception of blank text nodes, comments, XML declarations, and doctype declarations. In other words, it matches elements that are effectively empty of meaningful content.</td><td><code>li:not(:empty)</code></td></tr>
 </table>

 <h3 id="text-regex-selectors">Text, regex, and index selectors</h3>
 <p>These jsoup extensions match text or zero-based sibling indexes.</p>
 <table class="syntax">
 <tr><th>Pattern</th><th>Matches</th><th>Example</th></tr>
 <tr><td><code>:lt(<em>n</em>)</code></td><td>elements whose sibling index is less than <em>n</em></td><td><code>td:lt(3)</code> finds the first 3 cells of each row</td></tr>
 <tr><td><code>:gt(<em>n</em>)</code></td><td>elements whose sibling index is greater than <em>n</em></td><td><code>td:gt(1)</code> finds cells after skipping the first two</td></tr>
 <tr><td><code>:eq(<em>n</em>)</code></td><td>elements whose sibling index is equal to <em>n</em></td><td><code>td:eq(0)</code> finds the first cell of each row</td></tr>
 <tr><td><code>:contains(<em>text</em>)</code></td><td>elements that contain the specified text. The search is case insensitive. The text may appear in the found element, or any of its descendants. The text is whitespace normalized. <p>To find content that includes parentheses, escape those with a {@code \}.</p></td><td><code>p:contains(jsoup)</code> finds p elements containing the text "jsoup".<p>{@code p:contains(hello \(there\))} finds p elements containing the text "Hello (There)"</p></td></tr>
 <tr><td><code>:containsOwn(<em>text</em>)</code></td><td>elements that directly contain the specified text. The search is case insensitive. The text must appear in the found element, not any of its descendants.</td><td><code>p:containsOwn(jsoup)</code> finds p elements with own text "jsoup".</td></tr>
 <tr><td><code>:containsData(<em>data</em>)</code></td><td>elements that contain the specified <em>data</em>. The contents of {@code script} and {@code style} elements, and {@code comment} nodes (etc) are considered data nodes, not text nodes. The search is case insensitive. The data may appear in the found element, or any of its descendants.</td><td><code>script:containsData(jsoup)</code> finds script elements containing the data "jsoup".</td></tr>
 <tr><td><code>:containsWholeText(<em>text</em>)</code></td><td>elements containing the exact text, including case, spaces, and newlines, in themselves or their descendants. A <code>br</code> contributes a newline. Escape parentheses with a backslash</td><td><code>p:containsWholeText(two  spaces)</code> matches two spaces between the words</td></tr>
 <tr><td><code>:containsWholeOwnText(<em>text</em>)</code></td><td>like <code>:containsWholeText()</code>, but matches only the element's own text</td><td><code>p:containsWholeOwnText(two  spaces)</code></td></tr>
 <tr><td><code>:matches(<em>regex</em>)</code></td><td>elements containing <b>whitespace normalized</b> text that matches the specified regular expression. The text may appear in the found element, or any of its descendants.</td><td><code>td:matches(\d+)</code> finds table cells containing digits. <code>div:matches((?i)login)</code> finds divs containing the text, case insensitively.</td></tr>
 <tr><td><code>:matchesOwn(<em>regex</em>)</code></td><td>like <code>:matches()</code>, but matches only the element's own whitespace-normalized text</td><td><code>td:matchesOwn(\d+)</code></td></tr>
 <tr><td><code>:matchesWholeText(<em>regex</em>)</code></td><td>elements containing <b>non-normalized</b> whole text that matches the specified regular expression. The text may appear in the found element, or any of its descendants.</td><td><code>td:matchesWholeText(\s{2,})</code> finds table cells containing a run of at least two whitespace characters.</td></tr>
 <tr><td><code>:matchesWholeOwnText(<em>regex</em>)</code></td><td>elements whose own <b>non-normalized</b> whole text matches the specified regular expression. The text must appear in the found element, not any of its descendants.</td><td><code>td:matchesWholeOwnText(\n\d+)</code> finds table cells directly containing digits following a newline.</td></tr>
 </table>

 <h3 id="node-selectors">Node selectors</h3>
 <p>These jsoup extensions match leaf nodes, including comments and text. With {@link Element#select(String)},
 use them inside selectors such as {@code :has()} to find containing elements. Use {@link Element#selectNodes(String)}
 to retrieve matching nodes directly.</p>
 <table class="syntax">
 <tr><th>Pattern</th><th>Matches</th><th>Example</th></tr>
 <tr><td>::node</td><td>Matches any node</td><td></td></tr>
 <tr><td>::leafnode</td><td>Matches any leaf node (a node that is not an Element)</td><td></td></tr>
 <tr><td>::comment</td><td>Matches a Comment node</td><td></td></tr>
 <tr><td>::text</td><td>Matches a TextNode</td><td></td></tr>
 <tr><td>::data</td><td>Matches a DataNode (e.g. the content of a <code>script</code> or a <code>style</code> element)</td><td></td></tr>
 <tr><td>::cdata</td><td>Matches a CDataNode</td><td></td></tr>
 <tr><td>::pi</td><td>Matches any ProcessingInstruction node.</td><td><code>::pi</code></td></tr>
 <tr><td>::pi(<i>target</i>)</td><td>Matches a ProcessingInstruction node with the specified case-sensitive target.</td><td><code>::pi(marker)</code></td></tr>
 <tr><td>::pi[...]</td><td>Matches processing instructions by attributes parsed from their data.</td><td><code>::pi(marker)[name=country-options]</code></td></tr>
 <tr><td>::node:contains(text)</td><td>Matches a node that has a (normalized, case-insensitive) value containing <i>text</i>.</td><td><code>::comment:contains(foo bar)</code></td></tr>
 <tr><td>::node:matches(regex)</td><td>Matches a node that has a value matching the regex.</td><td><code>::comment:matches(\d+)</code></td></tr>
 <tr><td>::node:blank</td><td>Matches a node that has either no value, or a value of only whitespace.</td><td><code>::comment:not(:blank)</code></td></tr>
 </table>

 <h2 id="matching-rules">Matching rules</h2>
 <p>Ancestor matching stops at the element you query from. For example, {@code panel.select("body p")} does not match
 paragraphs when the body is outside the panel.</p>
 <p>Tag and attribute <b>names</b> ignore ASCII case. Class names and literal attribute <b>values</b> ignore Unicode case; IDs match
 exactly. Structural selectors such as {@code :nth-of-type()} group siblings by their normalized tag names.</p>
 <p>Use {@code i} on an attribute selector to ignore ASCII case in its value, or {@code s} for exact case; see
 <a href="#preview-case">case matching</a> for examples. These flags do not affect attribute names or the default regex
 interpretation of {@code ~=}; use regex inline flags such as {@code (?i)} there.</p>
 <p>Text selectors keep the matching rules described in their entries above. Regex selectors use Java's
 {@link java.util.regex.Pattern} syntax and flags.</p>

 <h3 id="escaping-selectors">Escaping and regular expressions</h3>
 <p>Escape CSS special characters with a backslash. To match {@code <p id="i.d">}, the CSS query is {@code #i\.d};
 in Java, write {@code document.select("#i\\.d")}. You can also use {@link Selector#escapeCssIdentifier(String)} to
 escape a tag, ID, or class name.</p>
 <p>Regex arguments are passed to the regex parser without CSS unescaping. For example, the CSS query
 {@code img:matchesAttr(src, (?i)\.(png|jpe?g))} is written in Java as
 {@code document.select("img:matchesAttr(src, (?i)\\.(png|jpe?g))")}.
 Whitespace immediately after the argument comma is skipped; use {@code \x20} for a leading literal space.</p>
 <p>Regex arguments use balanced parentheses. Escape literal parentheses that would unbalance the argument, including
 inside character classes: {@code :matchesAttr(x, ^[^\)]+$)} is written in Java as
 {@code document.select(":matchesAttr(x, ^[^\\)]+$)")}. Surrounding quotes are part of the regex, not an argument wrapper.</p>
 <p>When searching for literal text through a regex selector, use {@link java.util.regex.Pattern#quote(String)} to quote it:
 {@code String query = "div:matches(" + Pattern.quote(text) + ")";}.</p>

 <h2 id="query-compatibility">Query compatibility changes</h2>
 <p>jsoup 1.24.1 supports legacy and Web query modes. Enable Web mode with
 {@code System.setProperty("jsoup.queryMode", "web")}. Use {@code check} to compare supported queries while returning legacy
 results; see <a href="#query-check">check coverage</a> below.</p>
 <p>The default is {@code legacy} in 1.24.1 and changes to {@code web} in 1.25.1, when {@code legacy} and {@code check}
 remain available. In 1.26.1, Web behavior is the only option and the property is removed. See the
 <a href="https://jsoup.org/news/selector-migration">selector migration guide</a> for compatibility testing and query updates.</p>

 <h3 id="preview-matching">What changes in Web mode</h3>
 <table class="syntax">
 <caption>Query behavior by mode</caption>
 <tr><th>Query behavior</th><th>Legacy mode</th><th>Web mode</th></tr>
 <tr><td>Query results</td><td>Include the starting element if it matches.</td><td>Include only its descendants.</td></tr>
 <tr><td>Selector relationships</td><td>Ancestor matching stops at the element you query from.</td><td>Can match ancestors and siblings outside its subtree; results stay inside the element you query from.</td></tr>
 <tr><td>Tag and attribute names</td><td>Ignore ASCII case.</td><td>HTML names ignore ASCII case. Names in XML and foreign content (such as SVG and MathML) match exactly, as do attribute names on processing instructions.</td></tr>
 <tr><td>Classes and IDs</td><td>Classes ignore Unicode case; IDs match exactly.</td><td>Match exactly, except class and ID selectors in quirks documents ignore ASCII case.</td></tr>
 <tr><td>Literal attribute values without a flag</td><td>Ignore Unicode case.</td><td>Match exactly, except HTML attributes with case-insensitive values, such as {@code type}, {@code rel}, and {@code lang}, ignore ASCII case.</td></tr>
 <tr><td>{@code ~=}</td><td>Matches an attribute value using a regex.</td><td>Matches a complete whitespace-separated token. Supports {@code i} and {@code s} flags.</td></tr>
 <tr><td>{@code :nth-of-type()} and related selectors</td><td>Compares normalized tag names.</td><td>Compares namespace and exact local name.</td></tr>
 </table>
 <p>These rules apply to {@code select}, {@code selectFirst}, their stream and node variants, and the convenience finders on
 {@link Element}: {@code getElementsBy*}, text finders, {@code getElementById}, and {@code getAllElements}.
 {@link Element#hasClass(String)} uses the selected mode's class case rules. {@code getElementById} continues to match IDs exactly,
 including in quirks documents. Text and regex selectors retain their value-matching rules.</p>

 <h3 id="query-check">Check coverage and output</h3>
 <p>Checking covers string queries passed to {@code Element.select()}, {@code selectFirst()}, {@code expectFirst()},
 {@code is()}, and {@code closest()}, plus {@code Elements.select()}, {@code selectFirst()}, and {@code expectFirst()}.
 The corresponding {@link Selector} query methods are checked too; multi-root checks require a {@link java.util.Collection}.
 Streams, node queries, supplied evaluators, convenience finders, and other collection operations are not checked and
 retain legacy behavior in check mode.</p>
 <p>Each supported query runs in both modes. Differences and Web query errors produce a one-line warning on stderr,
 prefixed with {@code jsoup query compat:} and followed by up to two caller locations.
 See {@link Evaluator} for compiled-query behavior.</p>

 <h3 id="preview-searches">Query results and scope</h3>
 <p>{@code :scope} refers to the element the query is called on, or the document element when querying a Document.
 In Web mode, adding {@code :scope} does not include that element in query results. {@link Element#is(String)} and
 {@link Element#closest(String)} still test the supplied element; their {@code :scope} refers to that element.
 {@link Elements#not(String)} tests each supplied element directly.</p>
 <p>To include the starting element when it matches, filter {@link Element#stream()} with
 {@link Evaluator#asPredicate(Element)}.</p>
 <p>In Web mode, inside {@code :has()}, a leading combinator starts from each element being tested, but {@code :scope} still
 refers to the element the query is called on. For example, {@code outer.select("div:has(> p)")} finds divs with a direct child
 paragraph; {@code outer.select("div:has(:scope > p)")} finds none, because a paragraph inside a descendant div cannot also
 be a direct child of outer.</p>

 <h3 id="preview-regex">Attribute regular expressions</h3>
 <p>Use {@code :matchesAttr(attr, regex)} for attribute regexes in either mode, as shown in the
 <a href="#element-attribute-selectors">syntax table</a>. See <a href="#escaping-selectors">Escaping and regular expressions</a>
 for Java string examples and escaping rules.</p>

 <h3 id="preview-case">Case matching and attribute values</h3>
 <p>In Web mode, the document's parser selects HTML rules (the default) or XML rules with
 {@link org.jsoup.parser.Parser#xmlParser()}. The <a href="#preview-matching">comparison table</a> distinguishes attribute
 <b>names</b> from <b>values</b>. The {@code i} and {@code s} flags affect values only: {@code [data-sku="AbC123" i]} ignores ASCII
 case, while {@code [type="TEXT" s]} requires exact case. Both flags work in every query mode from 1.24.1.</p>
 <p>For case-insensitive class matching in Web mode, use {@code [class~="card" i]} instead of {@code .card}.
 See the <a href="#preview-matching">comparison table</a> for default class and ID case rules.</p>
 <p>Unquoted attribute values are supported in both modes, including numbers, URLs, and text with spaces. In Web mode,
 a final separate {@code i} or {@code s} is a case flag. Quote literal values ending in either token,
 such as {@code [data-x="two words i"]}.</p>

 @see Element#select(String css)
 @see Element#selectFirst(String css)
 @see Element#select(Evaluator eval)
 @see Element#selectNodes(String css)
 @see Element#selectNodes(String css, Class nodeType)
 @see Elements#select(String css)
 @see Element#selectXpath(String xpath) */
public class Selector {
    // not instantiable
    private Selector() {}

    /**
     Find Elements matching the CSS query.

     @param query CSS selector
     @param root root element to descend into
     @return matching elements, empty if none
     @throws Selector.SelectorParseException (unchecked) on an invalid CSS query.
     */
    public static Elements select(String query, Element root) {
        Validate.notEmpty(query);
        QueryMode mode = QueryMode.current();
        Elements result = select(QueryParser.parse(query, mode == QueryMode.Web), root);
        if (mode == QueryMode.Check)
            QueryCompat.check(query, result, evaluator -> select(evaluator, root));
        return result;
    }

    /**
     Find Elements matching the Evaluator.

     @param evaluator CSS Evaluator
     @param root root (context) element to start from
     @return matching elements, empty if none
     */
    public static Elements select(Evaluator evaluator, Element root) {
        Validate.notNull(evaluator);
        Validate.notNull(root);
        return Collector.collect(evaluator, root);
    }

    /**
     Finds a Stream of elements matching the CSS query.

     @param query CSS selector
     @param root root element to descend into
     @return a Stream of matching elements, empty if none
     @throws Selector.SelectorParseException (unchecked) on an invalid CSS query.
     @since 1.19.1
     */
    public static Stream<Element> selectStream(String query, Element root) {
        Validate.notEmpty(query);
        return selectStream(evaluatorOf(query), root);
    }

    /**
     Finds a Stream of elements matching the evaluator.

     @param evaluator CSS selector
     @param root root element to descend into
     @return matching elements, empty if none
     @since 1.19.1
     */
    public static Stream<Element> selectStream(Evaluator evaluator, Element root) {
        Validate.notNull(evaluator);
        Validate.notNull(root);
        return Collector.stream(evaluator, root);
    }

    /**
     Find elements matching the query, across multiple roots. Elements will be deduplicated (in the case of
     overlapping hierarchies).

     @param query CSS selector
     @param roots root elements to descend into
     @return matching elements, empty if none
     */
    public static Elements select(String query, Iterable<Element> roots) {
        Validate.notEmpty(query);
        Validate.notNull(roots);
        QueryMode mode = QueryMode.current();
        Elements result = select(QueryParser.parse(query, mode == QueryMode.Web), roots);
        if (mode == QueryMode.Check && roots instanceof Collection)
            QueryCompat.check(query, result, evaluator -> select(evaluator, roots));
        return result;
    }

    /** Reuses a compiled query across roots without changing its captured mode or result order. */
    static Elements select(Evaluator evaluator, Iterable<Element> roots) {
        Elements elements = new Elements();
        HashSet<Element> seenElements = new HashSet<>(); // dedupe elements by identity, as .equals is ==

        for (Element root : roots) {
            selectStream(evaluator, root)
                .filter(seenElements::add)
                .forEach(elements::add);
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
     Find the first Element that matches the query.

     @param cssQuery CSS selector
     @param root root element to descend into
     @return the matching element, or <b>null</b> if none.
     */
    public static @Nullable Element selectFirst(String cssQuery, Element root) {
        Validate.notEmpty(cssQuery);
        QueryMode mode = QueryMode.current();
        Element result = Collector.findFirst(QueryParser.parse(cssQuery, mode == QueryMode.Web), root);
        if (mode == QueryMode.Check)
            QueryCompat.check("selectFirst", cssQuery, result, evaluator -> Collector.findFirst(evaluator, root));
        return result;
    }

    /**
     Find the first element matching the query, across multiple roots.

     @param cssQuery CSS selector
     @param roots root elements to descend into
     @return the first matching element, or {@code null} if none
     @since 1.19.1
     */
    public static @Nullable Element selectFirst(String cssQuery, Iterable<Element> roots) {
        Validate.notEmpty(cssQuery);
        Validate.notNull(roots);
        QueryMode mode = QueryMode.current();
        Element result = selectFirst(QueryParser.parse(cssQuery, mode == QueryMode.Web), roots);
        if (mode == QueryMode.Check && roots instanceof Collection)
            QueryCompat.check("selectFirst", cssQuery, result, evaluator -> selectFirst(evaluator, roots));
        return result;
    }

    /** Finds the first result across roots using an already compiled query. */
    private static @Nullable Element selectFirst(Evaluator evaluator, Iterable<Element> roots) {
        for (Element root : roots) {
            Element first = Collector.findFirst(evaluator, root);
            if (first != null) return first;
        }

        return null;
    }

    /**
     Given a CSS identifier (such as a tag, ID, or class), escape any CSS special characters that would otherwise not be
     valid in a selector.

     @see <a href="https://www.w3.org/TR/cssom-1/#serialize-an-identifier">CSS Object Model, serialize an identifier</a>
     @since 1.20.1
     */
    public static String escapeCssIdentifier(String in) {
        return TokenQueue.escapeCssIdentifier(in);
    }

    /**
     Consume a CSS identifier (ID or class) off the queue.
     <p>Note: For backwards compatibility this method supports improperly formatted CSS identifiers, e.g. {@code 1} instead
     of {@code \31}.</p>

     @return The unescaped identifier.
     @throws IllegalArgumentException if an invalid escape sequence was found.
     @see <a href="https://www.w3.org/TR/css-syntax-3/#consume-name">CSS Syntax Module Level 3, Consume an ident sequence</a>
     @see <a href="https://www.w3.org/TR/css-syntax-3/#typedef-ident-token">CSS Syntax Module Level 3, ident-token</a>
     @since 1.20.1
     */
    public static String unescapeCssIdentifier(String in) {
        try (TokenQueue tq = new TokenQueue(in)) {
            return tq.consumeCssIdentifier();
        }
    }

    /**
     Parse a CSS query into an Evaluator. If you are evaluating the same query repeatedly, it may be more efficient to
     parse it once and reuse the Evaluator. The evaluator captures the current value of
     {@code jsoup.queryMode}; later property changes do not alter it.

     @param css CSS query
     @return Evaluator
     @see Selector selector query syntax
     @throws Selector.SelectorParseException if the CSS query is invalid
     @since 1.21.1
     */
    public static Evaluator evaluatorOf(String css) {
        return QueryParser.parse(css);
    }

    public static class SelectorParseException extends IllegalStateException {
        public SelectorParseException(String msg) {
            super(msg);
        }

        public SelectorParseException(String msg, Object... msgArgs) {
            super(String.format(msg, msgArgs));
        }

        public SelectorParseException(Throwable cause, String msg, Object... msgArgs) {
            super(String.format(msg, msgArgs), cause);
        }
    }
}
