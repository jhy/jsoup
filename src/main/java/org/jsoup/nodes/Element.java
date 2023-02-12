package org.jsoup.nodes;

import org.jsoup.helper.ChangeNotifyingArrayList;
import org.jsoup.helper.Validate;
import org.jsoup.internal.NonnullByDefault;
import org.jsoup.internal.StringUtil;
import org.jsoup.parser.ParseSettings;
import org.jsoup.parser.Tag;
import org.jsoup.select.Collector;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeFilter;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.QueryParser;
import org.jsoup.select.Selector;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static org.jsoup.internal.Normalizer.normalize;
import static org.jsoup.nodes.TextNode.lastCharIsWhitespace;
import static org.jsoup.parser.TokenQueue.escapeCssIdentifier;

/**
 An HTML Element consists of a tag name, attributes, and child nodes (including text nodes and other elements).
 <p>
 From an Element, you can extract data, traverse the node graph, and manipulate the HTML.
*/
@NonnullByDefault
public class Element extends Node {
    private static final List<Element> EmptyChildren = Collections.emptyList();
    private static final Pattern ClassSplit = Pattern.compile("\\s+");
    private static final String BaseUriKey = Attributes.internalKey("baseUri");
    private Tag tag;
    private @Nullable WeakReference<List<Element>> shadowChildrenRef; // points to child elements shadowed from node children
    List<Node> childNodes;
    @Nullable Attributes attributes; // field is nullable but all methods for attributes are non-null

    /**
     * Create a new, standalone element.
     * @param tag tag name
     */
    public Element(String tag) {
        this(Tag.valueOf(tag), "", null);
    }

    /**
     * Create a new, standalone Element. (Standalone in that it has no parent.)
     *
     * @param tag tag of this element
     * @param baseUri the base URI (optional, may be null to inherit from parent, or "" to clear parent's)
     * @param attributes initial attributes (optional, may be null)
     * @see #appendChild(Node)
     * @see #appendElement(String)
     */
    public Element(Tag tag, @Nullable String baseUri, @Nullable Attributes attributes) {
        Validate.notNull(tag);
        childNodes = EmptyNodes;
        this.attributes = attributes;
        this.tag = tag;
        if (baseUri != null)
            this.setBaseUri(baseUri);
    }

    /**
     * Create a new Element from a Tag and a base URI.
     *
     * @param tag element tag
     * @param baseUri the base URI of this element. Optional, and will inherit from its parent, if any.
     * @see Tag#valueOf(String, ParseSettings)
     */
    public Element(Tag tag, @Nullable String baseUri) {
        this(tag, baseUri, null);
    }

    /**
     Internal test to check if a nodelist object has been created.
     */
    protected boolean hasChildNodes() {
        return childNodes != EmptyNodes;
    }

    protected List<Node> ensureChildNodes() {
        if (childNodes == EmptyNodes) {
            childNodes = new NodeList(this, 4);
        }
        return childNodes;
    }

    @Override
    protected boolean hasAttributes() {
        return attributes != null;
    }

    @Override
    public Attributes attributes() {
        if (attributes == null) // not using hasAttributes, as doesn't clear warning
            attributes = new Attributes();
        return attributes;
    }

    @Override
    public String baseUri() {
        return searchUpForAttribute(this, BaseUriKey);
    }

    private static String searchUpForAttribute(final Element start, final String key) {
        Element el = start;
        while (el != null) {
            if (el.attributes != null && el.attributes.hasKey(key))
                return el.attributes.get(key);
            el = el.parent();
        }
        return "";
    }

    @Override
    protected void doSetBaseUri(String baseUri) {
        attributes().put(BaseUriKey, baseUri);
    }

    @Override
    public int childNodeSize() {
        return childNodes.size();
    }

    @Override
    public String nodeName() {
        return tag.getName();
    }

    /**
     * Get the name of the tag for this element. E.g. {@code div}. If you are using {@link ParseSettings#preserveCase
     * case preserving parsing}, this will return the source's original case.
     *
     * @return the tag name
     */
    public String tagName() {
        return tag.getName();
    }

    /**
     * Get the normalized name of this Element's tag. This will always be the lower-cased version of the tag, regardless
     * of the tag case preserving setting of the parser. For e.g., {@code <DIV>} and {@code <div>} both have a
     * normal name of {@code div}.
     * @return normal name
     */
    public String normalName() {
        return tag.normalName();
    }

    /**
     * Change (rename) the tag of this element. For example, convert a {@code <span>} to a {@code <div>} with
     * {@code el.tagName("div");}.
     *
     * @param tagName new tag name for this element
     * @return this element, for chaining
     * @see Elements#tagName(String)
     */
    public Element tagName(String tagName) {
        Validate.notEmptyParam(tagName, "tagName");
        tag = Tag.valueOf(tagName, NodeUtils.parser(this).settings()); // maintains the case option of the original parse
        return this;
    }

    /**
     * Get the Tag for this element.
     *
     * @return the tag object
     */
    public Tag tag() {
        return tag;
    }

    /**
     * Test if this element is a block-level element. (E.g. {@code <div> == true} or an inline element
     * {@code <span> == false}).
     *
     * @return true if block, false if not (and thus inline)
     */
    public boolean isBlock() {
        return tag.isBlock();
    }

    /**
     * Get the {@code id} attribute of this element.
     *
     * @return The id attribute, if present, or an empty string if not.
     */
    public String id() {
        return attributes != null ? attributes.getIgnoreCase("id") :"";
    }

    /**
     Set the {@code id} attribute of this element.
     @param id the ID value to use
     @return this Element, for chaining
     */
    public Element id(String id) {
        Validate.notNull(id);
        attr("id", id);
        return this;
    }

    /**
     * Set an attribute value on this element. If this element already has an attribute with the
     * key, its value is updated; otherwise, a new attribute is added.
     *
     * @return this element
     */
    public Element attr(String attributeKey, String attributeValue) {
        super.attr(attributeKey, attributeValue);
        return this;
    }

    /**
     * Set a boolean attribute value on this element. Setting to <code>true</code> sets the attribute value to "" and
     * marks the attribute as boolean so no value is written out. Setting to <code>false</code> removes the attribute
     * with the same key if it exists.
     *
     * @param attributeKey the attribute key
     * @param attributeValue the attribute value
     *
     * @return this element
     */
    public Element attr(String attributeKey, boolean attributeValue) {
        attributes().put(attributeKey, attributeValue);
        return this;
    }

    /**
     * Get this element's HTML5 custom data attributes. Each attribute in the element that has a key
     * starting with "data-" is included the dataset.
     * <p>
     * E.g., the element {@code <div data-package="jsoup" data-language="Java" class="group">...} has the dataset
     * {@code package=jsoup, language=java}.
     * <p>
     * This map is a filtered view of the element's attribute map. Changes to one map (add, remove, update) are reflected
     * in the other map.
     * <p>
     * You can find elements that have data attributes using the {@code [^data-]} attribute key prefix selector.
     * @return a map of {@code key=value} custom data attributes.
     */
    public Map<String, String> dataset() {
        return attributes().dataset();
    }

    @Override @Nullable
    public final Element parent() {
        return (Element) parentNode;
    }

    /**
     * Get this element's parent and ancestors, up to the document root.
     * @return this element's stack of parents, starting with the closest first.
     */
    public Elements parents() {
        Elements parents = new Elements();
        Element parent = this.parent();
        while (parent != null && !parent.isNode("#root")) {
            parents.add(parent);
            parent = parent.parent();
        }
        return parents;
    }

    /**
     * Get a child element of this element, by its 0-based index number.
     * <p>
     * Note that an element can have both mixed Nodes and Elements as children. This method inspects
     * a filtered list of children that are elements, and the index is based on that filtered list.
     * </p>
     *
     * @param index the index number of the element to retrieve
     * @return the child element, if it exists, otherwise throws an {@code IndexOutOfBoundsException}
     * @see #childNode(int)
     */
    public Element child(int index) {
        return childElementsList().get(index);
    }

    /**
     * Get the number of child nodes of this element that are elements.
     * <p>
     * This method works on the same filtered list like {@link #child(int)}. Use {@link #childNodes()} and {@link
     * #childNodeSize()} to get the unfiltered Nodes (e.g. includes TextNodes etc.)
     * </p>
     *
     * @return the number of child nodes that are elements
     * @see #children()
     * @see #child(int)
     */
    public int childrenSize() {
        return childElementsList().size();
    }

    /**
     * Get this element's child elements.
     * <p>
     * This is effectively a filter on {@link #childNodes()} to get Element nodes.
     * </p>
     * @return child elements. If this element has no children, returns an empty list.
     * @see #childNodes()
     */
    public Elements children() {
        return new Elements(childElementsList());
    }

    /**
     * Maintains a shadow copy of this element's child elements. If the nodelist is changed, this cache is invalidated.
     * TODO - think about pulling this out as a helper as there are other shadow lists (like in Attributes) kept around.
     * @return a list of child elements
     */
    List<Element> childElementsList() {
        if (childNodeSize() == 0)
            return EmptyChildren; // short circuit creating empty

        List<Element> children;
        if (shadowChildrenRef == null || (children = shadowChildrenRef.get()) == null) {
            final int size = childNodes.size();
            children = new ArrayList<>(size);
            //noinspection ForLoopReplaceableByForEach (beacause it allocates an Iterator which is wasteful here)
            for (int i = 0; i < size; i++) {
                final Node node = childNodes.get(i);
                if (node instanceof Element)
                    children.add((Element) node);
            }
            shadowChildrenRef = new WeakReference<>(children);
        }
        return children;
    }

    /**
     * Clears the cached shadow child elements.
     */
    @Override
    void nodelistChanged() {
        super.nodelistChanged();
        shadowChildrenRef = null;
    }

    /**
     * Get this element's child text nodes. The list is unmodifiable but the text nodes may be manipulated.
     * <p>
     * This is effectively a filter on {@link #childNodes()} to get Text nodes.
     * @return child text nodes. If this element has no text nodes, returns an
     * empty list.
     * </p>
     * For example, with the input HTML: {@code <p>One <span>Two</span> Three <br> Four</p>} with the {@code p} element selected:
     * <ul>
     *     <li>{@code p.text()} = {@code "One Two Three Four"}</li>
     *     <li>{@code p.ownText()} = {@code "One Three Four"}</li>
     *     <li>{@code p.children()} = {@code Elements[<span>, <br>]}</li>
     *     <li>{@code p.childNodes()} = {@code List<Node>["One ", <span>, " Three ", <br>, " Four"]}</li>
     *     <li>{@code p.textNodes()} = {@code List<TextNode>["One ", " Three ", " Four"]}</li>
     * </ul>
     */
    public List<TextNode> textNodes() {
        List<TextNode> textNodes = new ArrayList<>();
        for (Node node : childNodes) {
            if (node instanceof TextNode)
                textNodes.add((TextNode) node);
        }
        return Collections.unmodifiableList(textNodes);
    }

    /**
     * Get this element's child data nodes. The list is unmodifiable but the data nodes may be manipulated.
     * <p>
     * This is effectively a filter on {@link #childNodes()} to get Data nodes.
     * </p>
     * @return child data nodes. If this element has no data nodes, returns an
     * empty list.
     * @see #data()
     */
    public List<DataNode> dataNodes() {
        List<DataNode> dataNodes = new ArrayList<>();
        for (Node node : childNodes) {
            if (node instanceof DataNode)
                dataNodes.add((DataNode) node);
        }
        return Collections.unmodifiableList(dataNodes);
    }

    /**
     * Find elements that match the {@link Selector} CSS query, with this element as the starting context. Matched elements
     * may include this element, or any of its children.
     * <p>This method is generally more powerful to use than the DOM-type {@code getElementBy*} methods, because
     * multiple filters can be combined, e.g.:</p>
     * <ul>
     * <li>{@code el.select("a[href]")} - finds links ({@code a} tags with {@code href} attributes)
     * <li>{@code el.select("a[href*=example.com]")} - finds links pointing to example.com (loosely)
     * </ul>
     * <p>See the query syntax documentation in {@link org.jsoup.select.Selector}.</p>
     * <p>Also known as {@code querySelectorAll()} in the Web DOM.</p>
     *
     * @param cssQuery a {@link Selector} CSS-like query
     * @return an {@link Elements} list containing elements that match the query (empty if none match)
     * @see Selector selector query syntax
     * @see QueryParser#parse(String)
     * @throws Selector.SelectorParseException (unchecked) on an invalid CSS query.
     */
    public Elements select(String cssQuery) {
        return Selector.select(cssQuery, this);
    }

    /**
     * Find elements that match the supplied Evaluator. This has the same functionality as {@link #select(String)}, but
     * may be useful if you are running the same query many times (on many documents) and want to save the overhead of
     * repeatedly parsing the CSS query.
     * @param evaluator an element evaluator
     * @return an {@link Elements} list containing elements that match the query (empty if none match)
     */
    public Elements select(Evaluator evaluator) {
        return Selector.select(evaluator, this);
    }


    /**
     * Find the first Element that matches the {@link Selector} CSS query, with this element as the starting context.
     * <p>This is effectively the same as calling {@code element.select(query).first()}, but is more efficient as query
     * execution stops on the first hit.</p>
     * <p>Also known as {@code querySelector()} in the Web DOM.</p>
     * @param cssQuery cssQuery a {@link Selector} CSS-like query
     * @return the first matching element, or <b>{@code null}</b> if there is no match.
     * @see #expectFirst(String)
     */
    public @Nullable Element selectFirst(String cssQuery) {
        return Selector.selectFirst(cssQuery, this);
    }

    /**
     * Finds the first Element that matches the supplied Evaluator, with this element as the starting context, or
     * {@code null} if none match.
     *
     * @param evaluator an element evaluator
     * @return the first matching element (walking down the tree, starting from this element), or {@code null} if none
     * match.
     */
    public @Nullable Element selectFirst(Evaluator evaluator) {
        return Collector.findFirst(evaluator, this);
    }

    /**
     Just like {@link #selectFirst(String)}, but if there is no match, throws an {@link IllegalArgumentException}. This
     is useful if you want to simply abort processing on a failed match.
     @param cssQuery a {@link Selector} CSS-like query
     @return the first matching element
     @throws IllegalArgumentException if no match is found
     @since 1.15.2
     */
    public Element expectFirst(String cssQuery) {
        return (Element) Validate.ensureNotNull(
            Selector.selectFirst(cssQuery, this),
            parent() != null ?
                "No elements matched the query '%s' on element '%s'.":
                "No elements matched the query '%s' in the document."
            , cssQuery, this.tagName()
        );
    }

    /**
     * Checks if this element matches the given {@link Selector} CSS query. Also knows as {@code matches()} in the Web
     * DOM.
     *
     * @param cssQuery a {@link Selector} CSS query
     * @return if this element matches the query
     */
    public boolean is(String cssQuery) {
        return is(QueryParser.parse(cssQuery));
    }

    /**
     * Check if this element matches the given evaluator.
     * @param evaluator an element evaluator
     * @return if this element matches
     */
    public boolean is(Evaluator evaluator) {
        return evaluator.matches(this.root(), this);
    }

    /**
     * Find the closest element up the tree of parents that matches the specified CSS query. Will return itself, an
     * ancestor, or {@code null} if there is no such matching element.
     * @param cssQuery a {@link Selector} CSS query
     * @return the closest ancestor element (possibly itself) that matches the provided evaluator. {@code null} if not
     * found.
     */
    public @Nullable Element closest(String cssQuery) {
        return closest(QueryParser.parse(cssQuery));
    }

    /**
     * Find the closest element up the tree of parents that matches the specified evaluator. Will return itself, an
     * ancestor, or {@code null} if there is no such matching element.
     * @param evaluator a query evaluator
     * @return the closest ancestor element (possibly itself) that matches the provided evaluator. {@code null} if not
     * found.
     */
    public @Nullable Element closest(Evaluator evaluator) {
        Validate.notNull(evaluator);
        Element el = this;
        final Element root = root();
        do {
            if (evaluator.matches(root, el))
                return el;
            el = el.parent();
        } while (el != null);
        return null;
    }

    /**
     Find Elements that match the supplied XPath expression.
     <p>Note that for convenience of writing the Xpath expression, namespaces are disabled, and queries can be
     expressed using the element's local name only.</p>
     <p>By default, XPath 1.0 expressions are supported. If you would to use XPath 2.0 or higher, you can provide an
     alternate XPathFactory implementation:</p>
     <ol>
     <li>Add the implementation to your classpath. E.g. to use <a href="https://www.saxonica.com/products/products.xml">Saxon-HE</a>, add <a href="https://mvnrepository.com/artifact/net.sf.saxon/Saxon-HE">net.sf.saxon:Saxon-HE</a> to your build.</li>
     <li>Set the system property <code>javax.xml.xpath.XPathFactory:jsoup</code> to the implementing classname. E.g.:<br>
     <code>System.setProperty(W3CDom.XPathFactoryProperty, "net.sf.saxon.xpath.XPathFactoryImpl");</code>
     </li>
     </ol>

     @param xpath XPath expression
     @return matching elements, or an empty list if none match.
     @see #selectXpath(String, Class)
     @since 1.14.3
     */
    public Elements selectXpath(String xpath) {
        return new Elements(NodeUtils.selectXpath(xpath, this, Element.class));
    }

    /**
     Find Nodes that match the supplied XPath expression.
     <p>For example, to select TextNodes under {@code p} elements: </p>
     <pre>List&lt;TextNode&gt; textNodes = doc.selectXpath("//body//p//text()", TextNode.class);</pre>
     <p>Note that in the jsoup DOM, Attribute objects are not Nodes. To directly select attribute values, do something
     like:</p>
     <pre>List&lt;String&gt; hrefs = doc.selectXpath("//a").eachAttr("href");</pre>
     @param xpath XPath expression
     @param nodeType the jsoup node type to return
     @see #selectXpath(String)
     @return a list of matching nodes
     @since 1.14.3
     */
    public <T extends Node> List<T> selectXpath(String xpath, Class<T> nodeType) {
        return NodeUtils.selectXpath(xpath, this, nodeType);
    }

    /**
     * Insert a node to the end of this Element's children. The incoming node will be re-parented.
     *
     * @param child node to add.
     * @return this Element, for chaining
     * @see #prependChild(Node)
     * @see #insertChildren(int, Collection)
     */
    public Element appendChild(Node child) {
        Validate.notNull(child);

        // was - Node#addChildren(child). short-circuits an array create and a loop.
        reparentChild(child);
        ensureChildNodes();
        childNodes.add(child);
        child.setSiblingIndex(childNodes.size() - 1);
        return this;
    }

    /**
     Insert the given nodes to the end of this Element's children.

     @param children nodes to add
     @return this Element, for chaining
     @see #insertChildren(int, Collection)
     */
    public Element appendChildren(Collection<? extends Node> children) {
        insertChildren(-1, children);
        return this;
    }

    /**
     * Add this element to the supplied parent element, as its next child.
     *
     * @param parent element to which this element will be appended
     * @return this element, so that you can continue modifying the element
     */
    public Element appendTo(Element parent) {
        Validate.notNull(parent);
        parent.appendChild(this);
        return this;
    }

    /**
     * Add a node to the start of this element's children.
     *
     * @param child node to add.
     * @return this element, so that you can add more child nodes or elements.
     */
    public Element prependChild(Node child) {
        Validate.notNull(child);

        addChildren(0, child);
        return this;
    }

    /**
     Insert the given nodes to the start of this Element's children.

     @param children nodes to add
     @return this Element, for chaining
     @see #insertChildren(int, Collection)
     */
    public Element prependChildren(Collection<? extends Node> children) {
        insertChildren(0, children);
        return this;
    }


    /**
     * Inserts the given child nodes into this element at the specified index. Current nodes will be shifted to the
     * right. The inserted nodes will be moved from their current parent. To prevent moving, copy the nodes first.
     *
     * @param index 0-based index to insert children at. Specify {@code 0} to insert at the start, {@code -1} at the
     * end
     * @param children child nodes to insert
     * @return this element, for chaining.
     */
    public Element insertChildren(int index, Collection<? extends Node> children) {
        Validate.notNull(children, "Children collection to be inserted must not be null.");
        int currentSize = childNodeSize();
        if (index < 0) index += currentSize +1; // roll around
        Validate.isTrue(index >= 0 && index <= currentSize, "Insert position out of bounds.");

        ArrayList<Node> nodes = new ArrayList<>(children);
        Node[] nodeArray = nodes.toArray(new Node[0]);
        addChildren(index, nodeArray);
        return this;
    }

    /**
     * Inserts the given child nodes into this element at the specified index. Current nodes will be shifted to the
     * right. The inserted nodes will be moved from their current parent. To prevent moving, copy the nodes first.
     *
     * @param index 0-based index to insert children at. Specify {@code 0} to insert at the start, {@code -1} at the
     * end
     * @param children child nodes to insert
     * @return this element, for chaining.
     */
    public Element insertChildren(int index, Node... children) {
        Validate.notNull(children, "Children collection to be inserted must not be null.");
        int currentSize = childNodeSize();
        if (index < 0) index += currentSize +1; // roll around
        Validate.isTrue(index >= 0 && index <= currentSize, "Insert position out of bounds.");

        addChildren(index, children);
        return this;
    }

    /**
     * Create a new element by tag name, and add it as the last child.
     *
     * @param tagName the name of the tag (e.g. {@code div}).
     * @return the new element, to allow you to add content to it, e.g.:
     *  {@code parent.appendElement("h1").attr("id", "header").text("Welcome");}
     */
    public Element appendElement(String tagName) {
        Element child = new Element(Tag.valueOf(tagName, NodeUtils.parser(this).settings()), baseUri());
        appendChild(child);
        return child;
    }

    /**
     * Create a new element by tag name, and add it as the first child.
     *
     * @param tagName the name of the tag (e.g. {@code div}).
     * @return the new element, to allow you to add content to it, e.g.:
     *  {@code parent.prependElement("h1").attr("id", "header").text("Welcome");}
     */
    public Element prependElement(String tagName) {
        Element child = new Element(Tag.valueOf(tagName, NodeUtils.parser(this).settings()), baseUri());
        prependChild(child);
        return child;
    }

    /**
     * Create and append a new TextNode to this element.
     *
     * @param text the (un-encoded) text to add
     * @return this element
     */
    public Element appendText(String text) {
        Validate.notNull(text);
        TextNode node = new TextNode(text);
        appendChild(node);
        return this;
    }

    /**
     * Create and prepend a new TextNode to this element.
     *
     * @param text the decoded text to add
     * @return this element
     */
    public Element prependText(String text) {
        Validate.notNull(text);
        TextNode node = new TextNode(text);
        prependChild(node);
        return this;
    }

    /**
     * Add inner HTML to this element. The supplied HTML will be parsed, and each node appended to the end of the children.
     * @param html HTML to add inside this element, after the existing HTML
     * @return this element
     * @see #html(String)
     */
    public Element append(String html) {
        Validate.notNull(html);
        List<Node> nodes = NodeUtils.parser(this).parseFragmentInput(html, this, baseUri());
        addChildren(nodes.toArray(new Node[0]));
        return this;
    }

    /**
     * Add inner HTML into this element. The supplied HTML will be parsed, and each node prepended to the start of the element's children.
     * @param html HTML to add inside this element, before the existing HTML
     * @return this element
     * @see #html(String)
     */
    public Element prepend(String html) {
        Validate.notNull(html);
        List<Node> nodes = NodeUtils.parser(this).parseFragmentInput(html, this, baseUri());
        addChildren(0, nodes.toArray(new Node[0]));
        return this;
    }

    /**
     * Insert the specified HTML into the DOM before this element (as a preceding sibling).
     *
     * @param html HTML to add before this element
     * @return this element, for chaining
     * @see #after(String)
     */
    @Override
    public Element before(String html) {
        return (Element) super.before(html);
    }

    /**
     * Insert the specified node into the DOM before this node (as a preceding sibling).
     * @param node to add before this element
     * @return this Element, for chaining
     * @see #after(Node)
     */
    @Override
    public Element before(Node node) {
        return (Element) super.before(node);
    }

    /**
     * Insert the specified HTML into the DOM after this element (as a following sibling).
     *
     * @param html HTML to add after this element
     * @return this element, for chaining
     * @see #before(String)
     */
    @Override
    public Element after(String html) {
        return (Element) super.after(html);
    }

    /**
     * Insert the specified node into the DOM after this node (as a following sibling).
     * @param node to add after this element
     * @return this element, for chaining
     * @see #before(Node)
     */
    @Override
    public Element after(Node node) {
        return (Element) super.after(node);
    }

    /**
     * Remove all the element's child nodes. Any attributes are left as-is.
     * @return this element
     */
    @Override
    public Element empty() {
        childNodes.clear();
        return this;
    }

    /**
     * Wrap the supplied HTML around this element.
     *
     * @param html HTML to wrap around this element, e.g. {@code <div class="head"></div>}. Can be arbitrarily deep.
     * @return this element, for chaining.
     */
    @Override
    public Element wrap(String html) {
        return (Element) super.wrap(html);
    }

    /**
     * Get a CSS selector that will uniquely select this element.
     * <p>
     * If the element has an ID, returns #id;
     * otherwise returns the parent (if any) CSS selector, followed by {@literal '>'},
     * followed by a unique selector for the element (tag.class.class:nth-child(n)).
     * </p>
     *
     * @return the CSS Path that can be used to retrieve the element in a selector.
     */
    public String cssSelector() {
        if (id().length() > 0) {
            // prefer to return the ID - but check that it's actually unique first!
            String idSel = "#" + escapeCssIdentifier(id());
            Document doc = ownerDocument();
            if (doc != null) {
                Elements els = doc.select(idSel);
                if (els.size() == 1 && els.get(0) == this) // otherwise, continue to the nth-child impl
                    return idSel;
            } else {
                return idSel; // no ownerdoc, return the ID selector
            }
        }

        // Escape tagname, and translate HTML namespace ns:tag to CSS namespace syntax ns|tag
        String tagName = escapeCssIdentifier(tagName()).replace("\\:", "|");
        StringBuilder selector = StringUtil.borrowBuilder().append(tagName);
        // String classes = StringUtil.join(classNames().stream().map(TokenQueue::escapeCssIdentifier).iterator(), ".");
        // todo - replace with ^^ in 1.16.1 when we enable Android support for stream etc
        StringUtil.StringJoiner escapedClasses = new StringUtil.StringJoiner(".");
        for (String name : classNames()) escapedClasses.add(escapeCssIdentifier(name));
        String classes = escapedClasses.complete();
        if (classes.length() > 0)
            selector.append('.').append(classes);

        if (parent() == null || parent() instanceof Document) // don't add Document to selector, as will always have a html node
            return StringUtil.releaseBuilder(selector);

        selector.insert(0, " > ");
        if (parent().select(selector.toString()).size() > 1)
            selector.append(String.format(
                ":nth-child(%d)", elementSiblingIndex() + 1));

        return parent().cssSelector() + StringUtil.releaseBuilder(selector);
    }

    /**
     * Get sibling elements. If the element has no sibling elements, returns an empty list. An element is not a sibling
     * of itself, so will not be included in the returned list.
     * @return sibling elements
     */
    public Elements siblingElements() {
        if (parentNode == null)
            return new Elements(0);

        List<Element> elements = parent().childElementsList();
        Elements siblings = new Elements(elements.size() - 1);
        for (Element el: elements)
            if (el != this)
                siblings.add(el);
        return siblings;
    }

    /**
     * Gets the next sibling element of this element. E.g., if a {@code div} contains two {@code p}s,
     * the {@code nextElementSibling} of the first {@code p} is the second {@code p}.
     * <p>
     * This is similar to {@link #nextSibling()}, but specifically finds only Elements
     * </p>
     * @return the next element, or null if there is no next element
     * @see #previousElementSibling()
     */
    public @Nullable Element nextElementSibling() {
        if (parentNode == null) return null;
        List<Element> siblings = parent().childElementsList();
        int index = indexInList(this, siblings);
        if (siblings.size() > index+1)
            return siblings.get(index+1);
        else
            return null;
    }

    /**
     * Get each of the sibling elements that come after this element.
     *
     * @return each of the element siblings after this element, or an empty list if there are no next sibling elements
     */
    public Elements nextElementSiblings() {
        return nextElementSiblings(true);
    }

    /**
     * Gets the previous element sibling of this element.
     * @return the previous element, or null if there is no previous element
     * @see #nextElementSibling()
     */
    public @Nullable Element previousElementSibling() {
        if (parentNode == null) return null;
        List<Element> siblings = parent().childElementsList();
        int index = indexInList(this, siblings);
        if (index > 0)
            return siblings.get(index-1);
        else
            return null;
    }

    /**
     * Get each of the element siblings before this element.
     *
     * @return the previous element siblings, or an empty list if there are none.
     */
    public Elements previousElementSiblings() {
        return nextElementSiblings(false);
    }

    private Elements nextElementSiblings(boolean next) {
        Elements els = new Elements();
        if (parentNode == null)
            return  els;
        els.add(this);
        return next ?  els.nextAll() : els.prevAll();
    }

    /**
     * Gets the first Element sibling of this element. That may be this element.
     * @return the first sibling that is an element (aka the parent's first element child)
     */
    public Element firstElementSibling() {
        if (parent() != null) {
            List<Element> siblings = parent().childElementsList();
            return siblings.size() > 1 ? siblings.get(0) : this;
        } else
            return this; // orphan is its own first sibling
    }

    /**
     * Get the list index of this element in its element sibling list. I.e. if this is the first element
     * sibling, returns 0.
     * @return position in element sibling list
     */
    public int elementSiblingIndex() {
       if (parent() == null) return 0;
       return indexInList(this, parent().childElementsList());
    }

    /**
     * Gets the last element sibling of this element. That may be this element.
     * @return the last sibling that is an element (aka the parent's last element child)
     */
    public Element lastElementSibling() {
        if (parent() != null) {
            List<Element> siblings = parent().childElementsList();
            return siblings.size() > 1 ? siblings.get(siblings.size() - 1) : this;
        } else
            return this;
    }

    private static <E extends Element> int indexInList(Element search, List<E> elements) {
        final int size = elements.size();
        for (int i = 0; i < size; i++) {
            if (elements.get(i) == search)
                return i;
        }
        return 0;
    }

    /**
     Gets the first child of this Element that is an Element, or {@code null} if there is none.
     @return the first Element child node, or null.
     @see #firstChild()
     @see #lastElementChild()
     @since 1.15.2
     */
    public @Nullable Element firstElementChild() {
        final int size = childNodeSize();
        if (size == 0) return null;
        List<Node> children = ensureChildNodes();
        for (int i = 0; i < size; i++) {
            Node node = children.get(i);
            if (node instanceof Element) return (Element) node;
        }
        return null;
    }

    /**
     Gets the last child of this Element that is an Element, or @{code null} if there is none.
     @return the last Element child node, or null.
     @see #lastChild()
     @see #firstElementChild()
     @since 1.15.2
     */
    public @Nullable Element lastElementChild() {
        final int size = childNodeSize();
        if (size == 0) return null;
        List<Node> children = ensureChildNodes();
        for (int i = size -1; i >= 0; i--) {
            Node node = children.get(i);
            if (node instanceof Element) return (Element) node;
        }
        return null;
    }

    // DOM type methods

    /**
     * Finds elements, including and recursively under this element, with the specified tag name.
     * @param tagName The tag name to search for (case insensitively).
     * @return a matching unmodifiable list of elements. Will be empty if this element and none of its children match.
     */
    public Elements getElementsByTag(String tagName) {
        Validate.notEmpty(tagName);
        tagName = normalize(tagName);

        return Collector.collect(new Evaluator.Tag(tagName), this);
    }

    /**
     * Find an element by ID, including or under this element.
     * <p>
     * Note that this finds the first matching ID, starting with this element. If you search down from a different
     * starting point, it is possible to find a different element by ID. For unique element by ID within a Document,
     * use {@link Document#getElementById(String)}
     * @param id The ID to search for.
     * @return The first matching element by ID, starting with this element, or null if none found.
     */
    public @Nullable Element getElementById(String id) {
        Validate.notEmpty(id);

        Elements elements = Collector.collect(new Evaluator.Id(id), this);
        if (elements.size() > 0)
            return elements.get(0);
        else
            return null;
    }

    /**
     * Find elements that have this class, including or under this element. Case-insensitive.
     * <p>
     * Elements can have multiple classes (e.g. {@code <div class="header round first">}). This method
     * checks each class, so you can find the above with {@code el.getElementsByClass("header");}.
     *
     * @param className the name of the class to search for.
     * @return elements with the supplied class name, empty if none
     * @see #hasClass(String)
     * @see #classNames()
     */
    public Elements getElementsByClass(String className) {
        Validate.notEmpty(className);

        return Collector.collect(new Evaluator.Class(className), this);
    }

    /**
     * Find elements that have a named attribute set. Case-insensitive.
     *
     * @param key name of the attribute, e.g. {@code href}
     * @return elements that have this attribute, empty if none
     */
    public Elements getElementsByAttribute(String key) {
        Validate.notEmpty(key);
        key = key.trim();

        return Collector.collect(new Evaluator.Attribute(key), this);
    }

    /**
     * Find elements that have an attribute name starting with the supplied prefix. Use {@code data-} to find elements
     * that have HTML5 datasets.
     * @param keyPrefix name prefix of the attribute e.g. {@code data-}
     * @return elements that have attribute names that start with the prefix, empty if none.
     */
    public Elements getElementsByAttributeStarting(String keyPrefix) {
        Validate.notEmpty(keyPrefix);
        keyPrefix = keyPrefix.trim();

        return Collector.collect(new Evaluator.AttributeStarting(keyPrefix), this);
    }

    /**
     * Find elements that have an attribute with the specific value. Case-insensitive.
     *
     * @param key name of the attribute
     * @param value value of the attribute
     * @return elements that have this attribute with this value, empty if none
     */
    public Elements getElementsByAttributeValue(String key, String value) {
        return Collector.collect(new Evaluator.AttributeWithValue(key, value), this);
    }

    /**
     * Find elements that either do not have this attribute, or have it with a different value. Case-insensitive.
     *
     * @param key name of the attribute
     * @param value value of the attribute
     * @return elements that do not have a matching attribute
     */
    public Elements getElementsByAttributeValueNot(String key, String value) {
        return Collector.collect(new Evaluator.AttributeWithValueNot(key, value), this);
    }

    /**
     * Find elements that have attributes that start with the value prefix. Case-insensitive.
     *
     * @param key name of the attribute
     * @param valuePrefix start of attribute value
     * @return elements that have attributes that start with the value prefix
     */
    public Elements getElementsByAttributeValueStarting(String key, String valuePrefix) {
        return Collector.collect(new Evaluator.AttributeWithValueStarting(key, valuePrefix), this);
    }

    /**
     * Find elements that have attributes that end with the value suffix. Case-insensitive.
     *
     * @param key name of the attribute
     * @param valueSuffix end of the attribute value
     * @return elements that have attributes that end with the value suffix
     */
    public Elements getElementsByAttributeValueEnding(String key, String valueSuffix) {
        return Collector.collect(new Evaluator.AttributeWithValueEnding(key, valueSuffix), this);
    }

    /**
     * Find elements that have attributes whose value contains the match string. Case-insensitive.
     *
     * @param key name of the attribute
     * @param match substring of value to search for
     * @return elements that have attributes containing this text
     */
    public Elements getElementsByAttributeValueContaining(String key, String match) {
        return Collector.collect(new Evaluator.AttributeWithValueContaining(key, match), this);
    }

    /**
     * Find elements that have an attribute whose value matches the supplied regular expression.
     * @param key name of the attribute
     * @param pattern compiled regular expression to match against attribute values
     * @return elements that have attributes matching this regular expression
     */
    public Elements getElementsByAttributeValueMatching(String key, Pattern pattern) {
        return Collector.collect(new Evaluator.AttributeWithValueMatching(key, pattern), this);

    }

    /**
     * Find elements that have attributes whose values match the supplied regular expression.
     * @param key name of the attribute
     * @param regex regular expression to match against attribute values. You can use <a href="http://java.sun.com/docs/books/tutorial/essential/regex/pattern.html#embedded">embedded flags</a> (such as (?i) and (?m) to control regex options.
     * @return elements that have attributes matching this regular expression
     */
    public Elements getElementsByAttributeValueMatching(String key, String regex) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Pattern syntax error: " + regex, e);
        }
        return getElementsByAttributeValueMatching(key, pattern);
    }

    /**
     * Find elements whose sibling index is less than the supplied index.
     * @param index 0-based index
     * @return elements less than index
     */
    public Elements getElementsByIndexLessThan(int index) {
        return Collector.collect(new Evaluator.IndexLessThan(index), this);
    }

    /**
     * Find elements whose sibling index is greater than the supplied index.
     * @param index 0-based index
     * @return elements greater than index
     */
    public Elements getElementsByIndexGreaterThan(int index) {
        return Collector.collect(new Evaluator.IndexGreaterThan(index), this);
    }

    /**
     * Find elements whose sibling index is equal to the supplied index.
     * @param index 0-based index
     * @return elements equal to index
     */
    public Elements getElementsByIndexEquals(int index) {
        return Collector.collect(new Evaluator.IndexEquals(index), this);
    }

    /**
     * Find elements that contain the specified string. The search is case-insensitive. The text may appear directly
     * in the element, or in any of its descendants.
     * @param searchText to look for in the element's text
     * @return elements that contain the string, case-insensitive.
     * @see Element#text()
     */
    public Elements getElementsContainingText(String searchText) {
        return Collector.collect(new Evaluator.ContainsText(searchText), this);
    }

    /**
     * Find elements that directly contain the specified string. The search is case-insensitive. The text must appear directly
     * in the element, not in any of its descendants.
     * @param searchText to look for in the element's own text
     * @return elements that contain the string, case-insensitive.
     * @see Element#ownText()
     */
    public Elements getElementsContainingOwnText(String searchText) {
        return Collector.collect(new Evaluator.ContainsOwnText(searchText), this);
    }

    /**
     * Find elements whose text matches the supplied regular expression.
     * @param pattern regular expression to match text against
     * @return elements matching the supplied regular expression.
     * @see Element#text()
     */
    public Elements getElementsMatchingText(Pattern pattern) {
        return Collector.collect(new Evaluator.Matches(pattern), this);
    }

    /**
     * Find elements whose text matches the supplied regular expression.
     * @param regex regular expression to match text against. You can use <a href="http://java.sun.com/docs/books/tutorial/essential/regex/pattern.html#embedded">embedded flags</a> (such as (?i) and (?m) to control regex options.
     * @return elements matching the supplied regular expression.
     * @see Element#text()
     */
    public Elements getElementsMatchingText(String regex) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Pattern syntax error: " + regex, e);
        }
        return getElementsMatchingText(pattern);
    }

    /**
     * Find elements whose own text matches the supplied regular expression.
     * @param pattern regular expression to match text against
     * @return elements matching the supplied regular expression.
     * @see Element#ownText()
     */
    public Elements getElementsMatchingOwnText(Pattern pattern) {
        return Collector.collect(new Evaluator.MatchesOwn(pattern), this);
    }

    /**
     * Find elements whose own text matches the supplied regular expression.
     * @param regex regular expression to match text against. You can use <a href="http://java.sun.com/docs/books/tutorial/essential/regex/pattern.html#embedded">embedded flags</a> (such as (?i) and (?m) to control regex options.
     * @return elements matching the supplied regular expression.
     * @see Element#ownText()
     */
    public Elements getElementsMatchingOwnText(String regex) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new IllegalArgumentException("Pattern syntax error: " + regex, e);
        }
        return getElementsMatchingOwnText(pattern);
    }

    /**
     * Find all elements under this element (including self, and children of children).
     *
     * @return all elements
     */
    public Elements getAllElements() {
        return Collector.collect(new Evaluator.AllElements(), this);
    }

    /**
     Gets the <b>normalized, combined text</b> of this element and all its children. Whitespace is normalized and
     trimmed.
     <p>For example, given HTML {@code <p>Hello  <b>there</b> now! </p>}, {@code p.text()} returns {@code "Hello there
    now!"}
     <p>If you do not want normalized text, use {@link #wholeText()}. If you want just the text of this node (and not
     children), use {@link #ownText()}
     <p>Note that this method returns the textual content that would be presented to a reader. The contents of data
     nodes (such as {@code <script>} tags) are not considered text. Use {@link #data()} or {@link #html()} to retrieve
     that content.

     @return decoded, normalized text, or empty string if none.
     @see #wholeText()
     @see #ownText()
     @see #textNodes()
     */
    public String text() {
        final StringBuilder accum = StringUtil.borrowBuilder();
        NodeTraversor.traverse(new NodeVisitor() {
            public void head(Node node, int depth) {
                if (node instanceof TextNode) {
                    TextNode textNode = (TextNode) node;
                    appendNormalisedText(accum, textNode);
                } else if (node instanceof Element) {
                    Element element = (Element) node;
                    if (accum.length() > 0 &&
                        (element.isBlock() || element.isNode("br")) &&
                        !lastCharIsWhitespace(accum))
                        accum.append(' ');
                }
            }

            public void tail(Node node, int depth) {
                // make sure there is a space between block tags and immediately following text nodes or inline elements <div>One</div>Two should be "One Two".
                if (node instanceof Element) {
                    Element element = (Element) node;
                    Node next = node.nextSibling();
                    if (element.isBlock() && (next instanceof TextNode || next instanceof Element && !((Element) next).tag.formatAsBlock()) && !lastCharIsWhitespace(accum))
                        accum.append(' ');
                }

            }
        }, this);

        return StringUtil.releaseBuilder(accum).trim();
    }

    /**
     Get the non-normalized, decoded text of this element and its children, including only any newlines and spaces
     present in the original source.
     @return decoded, non-normalized text
     @see #text()
     @see #wholeOwnText()
     */
    public String wholeText() {
        final StringBuilder accum = StringUtil.borrowBuilder();
        NodeTraversor.traverse((node, depth) -> appendWholeText(node, accum), this);
        return StringUtil.releaseBuilder(accum);
    }

    private static void appendWholeText(Node node, StringBuilder accum) {
        if (node instanceof TextNode) {
            accum.append(((TextNode) node).getWholeText());
        } else if (node.isNode("br")) {
            accum.append("\n");
        }
    }

    /**
     Get the non-normalized, decoded text of this element, <b>not including</b> any child elements, including only any
     newlines and spaces present in the original source.
     @return decoded, non-normalized text that is a direct child of this Element
     @see #text()
     @see #wholeText()
     @see #ownText()
     @since 1.15.1
     */
    public String wholeOwnText() {
        final StringBuilder accum = StringUtil.borrowBuilder();
        final int size = childNodeSize();
        for (int i = 0; i < size; i++) {
            Node node = childNodes.get(i);
            appendWholeText(node, accum);
        }

        return StringUtil.releaseBuilder(accum);
    }

    /**
     * Gets the (normalized) text owned by this element only; does not get the combined text of all children.
     * <p>
     * For example, given HTML {@code <p>Hello <b>there</b> now!</p>}, {@code p.ownText()} returns {@code "Hello now!"},
     * whereas {@code p.text()} returns {@code "Hello there now!"}.
     * Note that the text within the {@code b} element is not returned, as it is not a direct child of the {@code p} element.
     *
     * @return decoded text, or empty string if none.
     * @see #text()
     * @see #textNodes()
     */
    public String ownText() {
        StringBuilder sb = StringUtil.borrowBuilder();
        ownText(sb);
        return StringUtil.releaseBuilder(sb).trim();
    }

    private void ownText(StringBuilder accum) {
        for (int i = 0; i < childNodeSize(); i++) {
            Node child = childNodes.get(i);
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                appendNormalisedText(accum, textNode);
            } else if (child.isNode("br") && !lastCharIsWhitespace(accum)) {
                accum.append(" ");
            }
        }
    }

    private static void appendNormalisedText(StringBuilder accum, TextNode textNode) {
        String text = textNode.getWholeText();
        if (preserveWhitespace(textNode.parentNode) || textNode instanceof CDataNode)
            accum.append(text);
        else
            StringUtil.appendNormalisedWhitespace(accum, text, lastCharIsWhitespace(accum));
    }

    static boolean preserveWhitespace(@Nullable Node node) {
        // looks only at this element and five levels up, to prevent recursion & needless stack searches
        if (node instanceof Element) {
            Element el = (Element) node;
            int i = 0;
            do {
                if (el.tag.preserveWhitespace())
                    return true;
                el = el.parent();
                i++;
            } while (i < 6 && el != null);
        }
        return false;
    }

    /**
     * Set the text of this element. Any existing contents (text or elements) will be cleared.
     * <p>As a special case, for {@code <script>} and {@code <style>} tags, the input text will be treated as data,
     * not visible text.</p>
     * @param text decoded text
     * @return this element
     */
    public Element text(String text) {
        Validate.notNull(text);
        empty();
        // special case for script/style in HTML: should be data node
        Document owner = ownerDocument();
        // an alternate impl would be to run through the parser
        if (owner != null && owner.parser().isContentForTagData(normalName()))
            appendChild(new DataNode(text));
        else
            appendChild(new TextNode(text));

        return this;
    }

    /**
     Checks if the current element or any of its child elements contain non-whitespace text.
     @return {@code true} if the element has non-blank text content, {@code false} otherwise.
     */
    public boolean hasText() {
        AtomicBoolean hasText = new AtomicBoolean(false);
        filter((node, depth) -> {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                if (!textNode.isBlank()) {
                    hasText.set(true);
                    return NodeFilter.FilterResult.STOP;
                }
            }
            return NodeFilter.FilterResult.CONTINUE;
        });
        return hasText.get();
    }

    /**
     * Get the combined data of this element. Data is e.g. the inside of a {@code <script>} tag. Note that data is NOT the
     * text of the element. Use {@link #text()} to get the text that would be visible to a user, and {@code data()}
     * for the contents of scripts, comments, CSS styles, etc.
     *
     * @return the data, or empty string if none
     *
     * @see #dataNodes()
     */
    public String data() {
        StringBuilder sb = StringUtil.borrowBuilder();
        traverse((childNode, depth) -> {
            if (childNode instanceof DataNode) {
                DataNode data = (DataNode) childNode;
                sb.append(data.getWholeData());
            } else if (childNode instanceof Comment) {
                Comment comment = (Comment) childNode;
                sb.append(comment.getData());
            } else if (childNode instanceof CDataNode) {
                // this shouldn't really happen because the html parser won't see the cdata as anything special when parsing script.
                // but in case another type gets through.
                CDataNode cDataNode = (CDataNode) childNode;
                sb.append(cDataNode.getWholeText());
            }
        });
        return StringUtil.releaseBuilder(sb);
    }

    /**
     * Gets the literal value of this element's "class" attribute, which may include multiple class names, space
     * separated. (E.g. on <code>&lt;div class="header gray"&gt;</code> returns, "<code>header gray</code>")
     * @return The literal class attribute, or <b>empty string</b> if no class attribute set.
     */
    public String className() {
        return attr("class").trim();
    }

    /**
     * Get each of the element's class names. E.g. on element {@code <div class="header gray">},
     * returns a set of two elements {@code "header", "gray"}. Note that modifications to this set are not pushed to
     * the backing {@code class} attribute; use the {@link #classNames(java.util.Set)} method to persist them.
     * @return set of classnames, empty if no class attribute
     */
    public Set<String> classNames() {
    	String[] names = ClassSplit.split(className());
    	Set<String> classNames = new LinkedHashSet<>(Arrays.asList(names));
    	classNames.remove(""); // if classNames() was empty, would include an empty class

        return classNames;
    }

    /**
     Set the element's {@code class} attribute to the supplied class names.
     @param classNames set of classes
     @return this element, for chaining
     */
    public Element classNames(Set<String> classNames) {
        Validate.notNull(classNames);
        if (classNames.isEmpty()) {
            attributes().remove("class");
        } else {
            attributes().put("class", StringUtil.join(classNames, " "));
        }
        return this;
    }

    /**
     * Tests if this element has a class. Case-insensitive.
     * @param className name of class to check for
     * @return true if it does, false if not
     */
    // performance sensitive
    public boolean hasClass(String className) {
        if (attributes == null)
            return false;

        final String classAttr = attributes.getIgnoreCase("class");
        final int len = classAttr.length();
        final int wantLen = className.length();

        if (len == 0 || len < wantLen) {
            return false;
        }

        // if both lengths are equal, only need compare the className with the attribute
        if (len == wantLen) {
            return className.equalsIgnoreCase(classAttr);
        }

        // otherwise, scan for whitespace and compare regions (with no string or arraylist allocations)
        boolean inClass = false;
        int start = 0;
        for (int i = 0; i < len; i++) {
            if (Character.isWhitespace(classAttr.charAt(i))) {
                if (inClass) {
                    // white space ends a class name, compare it with the requested one, ignore case
                    if (i - start == wantLen && classAttr.regionMatches(true, start, className, 0, wantLen)) {
                        return true;
                    }
                    inClass = false;
                }
            } else {
                if (!inClass) {
                    // we're in a class name : keep the start of the substring
                    inClass = true;
                    start = i;
                }
            }
        }

        // check the last entry
        if (inClass && len - start == wantLen) {
            return classAttr.regionMatches(true, start, className, 0, wantLen);
        }

        return false;
    }

    /**
     Add a class name to this element's {@code class} attribute.
     @param className class name to add
     @return this element
     */
    public Element addClass(String className) {
        Validate.notNull(className);

        Set<String> classes = classNames();
        classes.add(className);
        classNames(classes);

        return this;
    }

    /**
     Remove a class name from this element's {@code class} attribute.
     @param className class name to remove
     @return this element
     */
    public Element removeClass(String className) {
        Validate.notNull(className);

        Set<String> classes = classNames();
        classes.remove(className);
        classNames(classes);

        return this;
    }

    /**
     Toggle a class name on this element's {@code class} attribute: if present, remove it; otherwise add it.
     @param className class name to toggle
     @return this element
     */
    public Element toggleClass(String className) {
        Validate.notNull(className);

        Set<String> classes = classNames();
        if (classes.contains(className))
            classes.remove(className);
        else
            classes.add(className);
        classNames(classes);

        return this;
    }

    /**
     * Get the value of a form element (input, textarea, etc).
     * @return the value of the form element, or empty string if not set.
     */
    public String val() {
        if (normalName().equals("textarea"))
            return text();
        else
            return attr("value");
    }

    /**
     * Set the value of a form element (input, textarea, etc).
     * @param value value to set
     * @return this element (for chaining)
     */
    public Element val(String value) {
        if (normalName().equals("textarea"))
            text(value);
        else
            attr("value", value);
        return this;
    }

    /**
     Get the source range (start and end positions) of the end (closing) tag for this Element. Position tracking must be
     enabled prior to parsing the content.
     @return the range of the closing tag for this element, if it was explicitly closed in the source. {@code Untracked}
     otherwise.
     @see org.jsoup.parser.Parser#setTrackPosition(boolean)
     @see Node#sourceRange()
     @since 1.15.2
     */
    public Range endSourceRange() {
        return Range.of(this, false);
    }

    boolean shouldIndent(final Document.OutputSettings out) {
        return out.prettyPrint() && isFormatAsBlock(out) && !isInlineable(out);
    }

    @Override
    void outerHtmlHead(final Appendable accum, int depth, final Document.OutputSettings out) throws IOException {
        if (shouldIndent(out)) {
            if (accum instanceof StringBuilder) {
                if (((StringBuilder) accum).length() > 0)
                    indent(accum, depth, out);
            } else {
                indent(accum, depth, out);
            }
        }
        accum.append('<').append(tagName());
        if (attributes != null) attributes.html(accum, out);

        // selfclosing includes unknown tags, isEmpty defines tags that are always empty
        if (childNodes.isEmpty() && tag.isSelfClosing()) {
            if (out.syntax() == Document.OutputSettings.Syntax.html && tag.isEmpty())
                accum.append('>');
            else
                accum.append(" />"); // <img> in html, <img /> in xml
        }
        else
            accum.append('>');
    }

    @Override
    void outerHtmlTail(Appendable accum, int depth, Document.OutputSettings out) throws IOException {
        if (!(childNodes.isEmpty() && tag.isSelfClosing())) {
            if (out.prettyPrint() && (!childNodes.isEmpty() && (
                    tag.formatAsBlock() || (out.outline() && (childNodes.size()>1 || (childNodes.size()==1 && (childNodes.get(0) instanceof Element))))
            )))
                indent(accum, depth, out);
            accum.append("</").append(tagName()).append('>');
        }
    }

    /**
     * Retrieves the element's inner HTML. E.g. on a {@code <div>} with one empty {@code <p>}, would return
     * {@code <p></p>}. (Whereas {@link #outerHtml()} would return {@code <div><p></p></div>}.)
     *
     * @return String of HTML.
     * @see #outerHtml()
     */
    public String html() {
        StringBuilder accum = StringUtil.borrowBuilder();
        html(accum);
        String html = StringUtil.releaseBuilder(accum);
        return NodeUtils.outputSettings(this).prettyPrint() ? html.trim() : html;
    }

    @Override
    public <T extends Appendable> T html(T appendable) {
        final int size = childNodes.size();
        for (int i = 0; i < size; i++)
            childNodes.get(i).outerHtml(appendable);

        return appendable;
    }

    /**
     * Set this element's inner HTML. Clears the existing HTML first.
     * @param html HTML to parse and set into this element
     * @return this element
     * @see #append(String)
     */
    public Element html(String html) {
        empty();
        append(html);
        return this;
    }

    @Override
    public Element clone() {
        return (Element) super.clone();
    }

    @Override
    public Element shallowClone() {
        // simpler than implementing a clone version with no child copy
        return new Element(tag, baseUri(), attributes == null ? null : attributes.clone());
    }

    @Override
    protected Element doClone(@Nullable Node parent) {
        Element clone = (Element) super.doClone(parent);
        clone.attributes = attributes != null ? attributes.clone() : null;
        clone.childNodes = new NodeList(clone, childNodes.size());
        clone.childNodes.addAll(childNodes); // the children then get iterated and cloned in Node.clone

        return clone;
    }

    // overrides of Node for call chaining
    @Override
    public Element clearAttributes() {
        if (attributes != null) {
            super.clearAttributes();
            attributes = null;
        }

        return this;
    }

    @Override
    public Element removeAttr(String attributeKey) {
        return (Element) super.removeAttr(attributeKey);
    }

    @Override
    public Element root() {
        return (Element) super.root(); // probably a document, but always at least an element
    }

    @Override
    public Element traverse(NodeVisitor nodeVisitor) {
        return (Element) super.traverse(nodeVisitor);
    }

    @Override
    public Element forEachNode(Consumer<? super Node> action) {
        return (Element) super.forEachNode(action);
    }

    /**
     Perform the supplied action on this Element and each of its descendant Elements, during a depth-first traversal.
     Elements may be inspected, changed, added, replaced, or removed.
     @param action the function to perform on the element
     @return this Element, for chaining
     @see Node#forEachNode(Consumer)
     */
    public Element forEach(Consumer<? super Element> action) {
        Validate.notNull(action);
        NodeTraversor.traverse((node, depth) -> {
            if (node instanceof Element)
                action.accept((Element) node);
        }, this);
        return this;
    }

    /**
     @deprecated Use {@link #forEach(Consumer)} instead.
     */
    public Element forEach(org.jsoup.helper.Consumer<? super Element> action) {
        Validate.notNull(action);
        NodeTraversor.traverse((node, depth) -> {
            if (node instanceof Element)
                action.accept((Element) node);
        }, this);
        return this;
    }

    @Override
    public Element filter(NodeFilter nodeFilter) {
        return  (Element) super.filter(nodeFilter);
    }

    private static final class NodeList extends ChangeNotifyingArrayList<Node> {
        private final Element owner;

        NodeList(Element owner, int initialCapacity) {
            super(initialCapacity);
            this.owner = owner;
        }

        public void onContentsChanged() {
            owner.nodelistChanged();
        }
    }

    private boolean isFormatAsBlock(Document.OutputSettings out) {
        return tag.formatAsBlock() || (parent() != null && parent().tag().formatAsBlock()) || out.outline();
    }

    private boolean isInlineable(Document.OutputSettings out) {
        return tag().isInline()
            && (parent() == null || parent().isBlock())
            && previousSibling() != null
            && !out.outline();
    }
}
