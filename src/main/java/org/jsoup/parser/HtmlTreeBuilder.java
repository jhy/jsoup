package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.internal.Normalizer;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.CDataNode;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.jsoup.internal.StringUtil.inSorted;
import static org.jsoup.parser.HtmlTreeBuilderState.Constants.InTableFoster;
import static org.jsoup.parser.HtmlTreeBuilderState.ForeignContent;
import static org.jsoup.parser.Parser.NamespaceHtml;

/**
 * HTML Tree Builder; creates a DOM from Tokens.
 */
public class HtmlTreeBuilder extends TreeBuilder {
    // tag searches. must be sorted, used in inSorted. HtmlTreeBuilderTest validates they're sorted.
    static final String[] TagsSearchInScope = new String[]{"applet", "caption", "html", "marquee", "object", "table", "td", "th"};
    static final String[] TagSearchList = new String[]{"ol", "ul"};
    static final String[] TagSearchButton = new String[]{"button"};
    static final String[] TagSearchTableScope = new String[]{"html", "table"};
    static final String[] TagSearchSelectScope = new String[]{"optgroup", "option"};
    static final String[] TagSearchEndTags = new String[]{"dd", "dt", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc"};
    static final String[] TagThoroughSearchEndTags = new String[]{"caption", "colgroup", "dd", "dt", "li", "optgroup", "option", "p", "rb", "rp", "rt", "rtc", "tbody", "td", "tfoot", "th", "thead", "tr"};
    static final String[] TagSearchSpecial = new String[]{"address", "applet", "area", "article", "aside", "base", "basefont", "bgsound",
        "blockquote", "body", "br", "button", "caption", "center", "col", "colgroup", "command", "dd",
        "details", "dir", "div", "dl", "dt", "embed", "fieldset", "figcaption", "figure", "footer", "form",
        "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hgroup", "hr", "html",
        "iframe", "img", "input", "isindex", "li", "link", "listing", "marquee", "menu", "meta", "nav",
        "noembed", "noframes", "noscript", "object", "ol", "p", "param", "plaintext", "pre", "script",
        "section", "select", "style", "summary", "table", "tbody", "td", "textarea", "tfoot", "th", "thead",
        "title", "tr", "ul", "wbr", "xmp"};
    static final String[] TagMathMlTextIntegration = new String[]{"mi", "mn", "mo", "ms", "mtext"};
    static final String[] TagSvgHtmlIntegration = new String[]{"desc", "foreignObject", "title"};

    public static final int MaxScopeSearchDepth = 100; // prevents the parser bogging down in exceptionally broken pages

    private HtmlTreeBuilderState state; // the current state
    private HtmlTreeBuilderState originalState; // original / marked state

    private boolean baseUriSetFromDoc;
    private @Nullable Element headElement; // the current head element
    private @Nullable FormElement formElement; // the current form element
    private @Nullable Element contextElement; // fragment parse root; name only copy of context. could be null even if fragment parsing
    private ArrayList<Element> formattingElements; // active (open) formatting elements
    private ArrayList<HtmlTreeBuilderState> tmplInsertMode; // stack of Template Insertion modes
    private List<Token.Character> pendingTableCharacters; // chars in table to be shifted out
    private Token.EndTag emptyEnd; // reused empty end tag

    private boolean framesetOk; // if ok to go into frameset
    private boolean fosterInserts; // if next inserts should be fostered
    private boolean fragmentParsing; // if parsing a fragment of html

    @Override ParseSettings defaultSettings() {
        return ParseSettings.htmlDefault;
    }

    @Override
    HtmlTreeBuilder newInstance() {
        return new HtmlTreeBuilder();
    }

    @Override
    protected void initialiseParse(Reader input, String baseUri, Parser parser) {
        super.initialiseParse(input, baseUri, parser);

        // this is a bit mucky. todo - probably just create new parser objects to ensure all reset.
        state = HtmlTreeBuilderState.Initial;
        originalState = null;
        baseUriSetFromDoc = false;
        headElement = null;
        formElement = null;
        contextElement = null;
        formattingElements = new ArrayList<>();
        tmplInsertMode = new ArrayList<>();
        pendingTableCharacters = new ArrayList<>();
        emptyEnd = new Token.EndTag(this);
        framesetOk = true;
        fosterInserts = false;
        fragmentParsing = false;
    }

    @Override void initialiseParseFragment(@Nullable Element context) {
        // context may be null
        state = HtmlTreeBuilderState.Initial;
        fragmentParsing = true;

        if (context != null) {
            final String contextName = context.normalName();
            contextElement = new Element(tagFor(contextName, settings), baseUri);
            if (context.ownerDocument() != null) // quirks setup:
                doc.quirksMode(context.ownerDocument().quirksMode());

            // initialise the tokeniser state:
            switch (contextName) {
                case "title":
                case "textarea":
                    tokeniser.transition(TokeniserState.Rcdata);
                    break;
                case "iframe":
                case "noembed":
                case "noframes":
                case "style":
                case "xmp":
                    tokeniser.transition(TokeniserState.Rawtext);
                    break;
                case "script":
                    tokeniser.transition(TokeniserState.ScriptData);
                    break;
                case "plaintext":
                    tokeniser.transition(TokeniserState.PLAINTEXT);
                    break;
                case "template":
                    tokeniser.transition(TokeniserState.Data);
                    pushTemplateMode(HtmlTreeBuilderState.InTemplate);
                    break;
                default:
                    tokeniser.transition(TokeniserState.Data);
            }
            doc.appendChild(contextElement);
            push(contextElement);
            resetInsertionMode();

            // setup form element to nearest form on context (up ancestor chain). ensures form controls are associated
            // with form correctly
            Element formSearch = context;
            while (formSearch != null) {
                if (formSearch instanceof FormElement) {
                    formElement = (FormElement) formSearch;
                    break;
                }
                formSearch = formSearch.parent();
            }
        }
    }

    @Override List<Node> completeParseFragment() {
        if (contextElement != null) {
            // depending on context and the input html, content may have been added outside of the root el
            // e.g. context=p, input=div, the div will have been pushed out.
            List<Node> nodes = contextElement.siblingNodes();
            if (!nodes.isEmpty())
                contextElement.insertChildren(-1, nodes);
            return contextElement.childNodes();
        }
        else
            return doc.childNodes();
    }

    @Override
    protected boolean process(Token token) {
        HtmlTreeBuilderState dispatch = useCurrentOrForeignInsert(token) ? this.state : ForeignContent;
        return dispatch.process(token, this);
    }

    boolean useCurrentOrForeignInsert(Token token) {
        // https://html.spec.whatwg.org/multipage/parsing.html#tree-construction
        // If the stack of open elements is empty
        if (stack.isEmpty())
            return true;
        final Element el = currentElement();
        final String ns = el.tag().namespace();

        // If the adjusted current node is an element in the HTML namespace
        if (NamespaceHtml.equals(ns))
            return true;

        // If the adjusted current node is a MathML text integration point and the token is a start tag whose tag name is neither "mglyph" nor "malignmark"
        // If the adjusted current node is a MathML text integration point and the token is a character token
        if (isMathmlTextIntegration(el)) {
            if (token.isStartTag()
                    && !"mglyph".equals(token.asStartTag().normalName)
                    && !"malignmark".equals(token.asStartTag().normalName))
                    return true;
            if (token.isCharacter())
                    return true;
        }
        // If the adjusted current node is a MathML annotation-xml element and the token is a start tag whose tag name is "svg"
        if (Parser.NamespaceMathml.equals(ns)
            && el.nameIs("annotation-xml")
            && token.isStartTag()
            && "svg".equals(token.asStartTag().normalName))
            return true;

        // If the adjusted current node is an HTML integration point and the token is a start tag
        // If the adjusted current node is an HTML integration point and the token is a character token
        if (isHtmlIntegration(el)
            && (token.isStartTag() || token.isCharacter()))
            return true;

        // If the token is an end-of-file token
        return token.isEOF();
    }

    static boolean isMathmlTextIntegration(Element el) {
        /*
        A node is a MathML text integration point if it is one of the following elements:
        A MathML mi element
        A MathML mo element
        A MathML mn element
        A MathML ms element
        A MathML mtext element
         */
        return (Parser.NamespaceMathml.equals(el.tag().namespace())
            && StringUtil.inSorted(el.normalName(), TagMathMlTextIntegration));
    }

    static boolean isHtmlIntegration(Element el) {
        /*
        A node is an HTML integration point if it is one of the following elements:
        A MathML annotation-xml element whose start tag token had an attribute with the name "encoding" whose value was an ASCII case-insensitive match for the string "text/html"
        A MathML annotation-xml element whose start tag token had an attribute with the name "encoding" whose value was an ASCII case-insensitive match for the string "application/xhtml+xml"
        An SVG foreignObject element
        An SVG desc element
        An SVG title element
         */
        if (Parser.NamespaceMathml.equals(el.tag().namespace())
            && el.nameIs("annotation-xml")) {
            String encoding = Normalizer.normalize(el.attr("encoding"));
            if (encoding.equals("text/html") || encoding.equals("application/xhtml+xml"))
                return true;
        }
        if (Parser.NamespaceSvg.equals(el.tag().namespace())
            && StringUtil.in(el.tagName(), TagSvgHtmlIntegration)) // note using .tagName for case-sensitive hit here of foreignObject
            return true;

        return false;
    }

    boolean process(Token token, HtmlTreeBuilderState state) {
        return state.process(token, this);
    }

    void transition(HtmlTreeBuilderState state) {
        this.state = state;
    }

    HtmlTreeBuilderState state() {
        return state;
    }

    void markInsertionMode() {
        originalState = state;
    }

    HtmlTreeBuilderState originalState() {
        return originalState;
    }

    void framesetOk(boolean framesetOk) {
        this.framesetOk = framesetOk;
    }

    boolean framesetOk() {
        return framesetOk;
    }

    Document getDocument() {
        return doc;
    }

    String getBaseUri() {
        return baseUri;
    }

    void maybeSetBaseUri(Element base) {
        if (baseUriSetFromDoc) // only listen to the first <base href> in parse
            return;

        String href = base.absUrl("href");
        if (href.length() != 0) { // ignore <base target> etc
            baseUri = href;
            baseUriSetFromDoc = true;
            doc.setBaseUri(href); // set on the doc so doc.createElement(Tag) will get updated base, and to update all descendants
        }
    }

    boolean isFragmentParsing() {
        return fragmentParsing;
    }

    void error(HtmlTreeBuilderState state) {
        if (parser.getErrors().canAddError())
            parser.getErrors().add(new ParseError(reader, "Unexpected %s token [%s] when in state [%s]",
                currentToken.tokenType(), currentToken, state));
    }

    Element createElementFor(Token.StartTag startTag, String namespace, boolean forcePreserveCase) {
        // dedupe and normalize the attributes:
        Attributes attributes = startTag.attributes;
        if (!forcePreserveCase)
            attributes = settings.normalizeAttributes(attributes);
        if (attributes != null && !attributes.isEmpty()) {
            int dupes = attributes.deduplicate(settings);
            if (dupes > 0) {
                error("Dropped duplicate attribute(s) in tag [%s]", startTag.normalName);
            }
        }

        Tag tag = tagFor(startTag.tagName, namespace,
            forcePreserveCase ? ParseSettings.preserveCase : settings);

        return (tag.normalName().equals("form")) ?
            new FormElement(tag, null, attributes) :
            new Element(tag, null, attributes);
    }

    /** Inserts an HTML element for the given tag) */
    Element insertElementFor(final Token.StartTag startTag) {
        Element el = createElementFor(startTag, NamespaceHtml, false);
        doInsertElement(el, startTag);

        // handle self-closing tags. when the spec expects an empty tag, will directly hit insertEmpty, so won't generate this fake end tag.
        if (startTag.isSelfClosing()) {
            Tag tag = el.tag();
            if (tag.isKnownTag()) {
                if (!tag.isEmpty())
                    tokeniser.error("Tag [%s] cannot be self closing; not a void tag", tag.normalName());
                // else: ok
            }
            else { // unknown tag: remember this is self-closing, for output
                tag.setSelfClosing();
            }

            // effectively a pop, but fiddles with the state. handles empty style, title etc which would otherwise leave us in data state
            tokeniser.transition(TokeniserState.Data); // handles <script />, otherwise needs breakout steps from script data
            tokeniser.emit(emptyEnd.reset().name(el.tagName()));  // ensure we get out of whatever state we are in. emitted for yielded processing
        }

        return el;
    }

    /**
     Inserts a foreign element. Preserves the case of the tag name and of the attributes.
     */
    Element insertForeignElementFor(final Token.StartTag startTag, String namespace) {
        Element el = createElementFor(startTag, namespace, true);
        doInsertElement(el, startTag);

        if (startTag.isSelfClosing()) {
            el.tag().setSelfClosing(); // remember this is self-closing for output
            pop();
        }

        return el;
    }

    Element insertEmptyElementFor(Token.StartTag startTag) {
        Element el = createElementFor(startTag, NamespaceHtml, false);
        doInsertElement(el, startTag);
        pop();
        return el;
    }

    FormElement insertFormElement(Token.StartTag startTag, boolean onStack, boolean checkTemplateStack) {
        FormElement el = (FormElement) createElementFor(startTag, NamespaceHtml, false);

        if (checkTemplateStack) {
            if(!onStack("template"))
                setFormElement(el);
        } else
            setFormElement(el);

        doInsertElement(el, startTag);
        if (!onStack) pop();
        return el;
    }

    /** Inserts the Element onto the stack. All element inserts must run through this method. Performs any general
     tests on the Element before insertion.
     * @param el the Element to insert and make the current element
     * @param token the token this element was parsed from. If null, uses a zero-width current token as intrinsic insert
     */
    private void doInsertElement(Element el, @Nullable Token token) {
        if (el.tag().isFormListed() && formElement != null)
            formElement.addElement(el); // connect form controls to their form element

        // in HTML, the xmlns attribute if set must match what the parser set the tag's namespace to
        if (parser.getErrors().canAddError() && el.hasAttr("xmlns") && !el.attr("xmlns").equals(el.tag().namespace()))
            error("Invalid xmlns attribute [%s] on tag [%s]", el.attr("xmlns"), el.tagName());

        if (isFosterInserts() && StringUtil.inSorted(currentElement().normalName(), InTableFoster))
            insertInFosterParent(el);
        else
            currentElement().appendChild(el);

        push(el);
    }

    void insertCommentNode(Token.Comment token) {
        Comment node = new Comment(token.getData());
        currentElement().appendChild(node);
        onNodeInserted(node);
    }

    /** Inserts the provided character token into the current element. */
    void insertCharacterNode(Token.Character characterToken) {
        Element el = currentElement(); // will be doc if no current element; allows for whitespace to be inserted into the doc root object (not on the stack)
        insertCharacterToElement(characterToken, el);
    }

    /** Inserts the provided character token into the provided element. */
    void insertCharacterToElement(Token.Character characterToken, Element el) {
        final Node node;
        final String tagName = el.normalName();
        final String data = characterToken.getData();

        if (characterToken.isCData())
            node = new CDataNode(data);
        else if (isContentForTagData(tagName))
            node = new DataNode(data);
        else
            node = new TextNode(data);
        el.appendChild(node); // doesn't use insertNode, because we don't foster these; and will always have a stack.
        onNodeInserted(node);
    }

    ArrayList<Element> getStack() {
        return stack;
    }

    boolean onStack(Element el) {
        return onStack(stack, el);
    }

    /** Checks if there is an HTML element with the given name on the stack. */
    boolean onStack(String elName) {
        return getFromStack(elName) != null;
    }

    private static final int maxQueueDepth = 256; // an arbitrary tension point between real HTML and crafted pain
    private static boolean onStack(ArrayList<Element> queue, Element element) {
        final int bottom = queue.size() - 1;
        final int upper = bottom >= maxQueueDepth ? bottom - maxQueueDepth : 0;
        for (int pos = bottom; pos >= upper; pos--) {
            Element next = queue.get(pos);
            if (next == element) {
                return true;
            }
        }
        return false;
    }

    /** Gets the nearest (lowest) HTML element with the given name from the stack. */
    @Nullable
    Element getFromStack(String elName) {
        final int bottom = stack.size() - 1;
        final int upper = bottom >= maxQueueDepth ? bottom - maxQueueDepth : 0;
        for (int pos = bottom; pos >= upper; pos--) {
            Element next = stack.get(pos);
            if (next.elementIs(elName, NamespaceHtml)) {
                return next;
            }
        }
        return null;
    }

    boolean removeFromStack(Element el) {
        for (int pos = stack.size() -1; pos >= 0; pos--) {
            Element next = stack.get(pos);
            if (next == el) {
                stack.remove(pos);
                onNodeClosed(el);
                return true;
            }
        }
        return false;
    }

    /** Pops the stack until the given HTML element is removed. */
    @Nullable
    Element popStackToClose(String elName) {
        for (int pos = stack.size() -1; pos >= 0; pos--) {
            Element el = pop();
            if (el.elementIs(elName, NamespaceHtml)) {
                return el;
            }
        }
        return null;
    }

    /** Pops the stack until an element with the supplied name is removed, irrespective of namespace. */
    @Nullable
    Element popStackToCloseAnyNamespace(String elName) {
        for (int pos = stack.size() -1; pos >= 0; pos--) {
            Element el = pop();
            if (el.nameIs(elName)) {
                return el;
            }
        }
        return null;
    }

    /** Pops the stack until one of the given HTML elements is removed. */
    void popStackToClose(String... elNames) { // elnames is sorted, comes from Constants
        for (int pos = stack.size() -1; pos >= 0; pos--) {
            Element el = pop();
            if (inSorted(el.normalName(), elNames) && NamespaceHtml.equals(el.tag().namespace())) {
                break;
            }
        }
    }

    void clearStackToTableContext() {
        clearStackToContext("table", "template");
    }

    void clearStackToTableBodyContext() {
        clearStackToContext("tbody", "tfoot", "thead", "template");
    }

    void clearStackToTableRowContext() {
        clearStackToContext("tr", "template");
    }

    /** Removes elements from the stack until one of the supplied HTML elements is removed. */
    private void clearStackToContext(String... nodeNames) {
        for (int pos = stack.size() -1; pos >= 0; pos--) {
            Element next = stack.get(pos);
            if (NamespaceHtml.equals(next.tag().namespace()) &&
                (StringUtil.in(next.normalName(), nodeNames) || next.nameIs("html")))
                break;
            else
                pop();
        }
    }

    @Nullable Element aboveOnStack(Element el) {
        assert onStack(el);
        for (int pos = stack.size() -1; pos >= 0; pos--) {
            Element next = stack.get(pos);
            if (next == el) {
                return stack.get(pos-1);
            }
        }
        return null;
    }

    void insertOnStackAfter(Element after, Element in) {
        int i = stack.lastIndexOf(after);
        Validate.isTrue(i != -1);
        stack.add(i+1, in);
    }

    void replaceOnStack(Element out, Element in) {
        replaceInQueue(stack, out, in);
    }

    private static void replaceInQueue(ArrayList<Element> queue, Element out, Element in) {
        int i = queue.lastIndexOf(out);
        Validate.isTrue(i != -1);
        queue.set(i, in);
    }

    /**
     * Reset the insertion mode, by searching up the stack for an appropriate insertion mode. The stack search depth
     * is limited to {@link #maxQueueDepth}.
     * @return true if the insertion mode was actually changed.
     */
    boolean resetInsertionMode() {
        // https://html.spec.whatwg.org/multipage/parsing.html#the-insertion-mode
        boolean last = false;
        final int bottom = stack.size() - 1;
        final int upper = bottom >= maxQueueDepth ? bottom - maxQueueDepth : 0;
        final HtmlTreeBuilderState origState = this.state;

        if (stack.size() == 0) { // nothing left of stack, just get to body
            transition(HtmlTreeBuilderState.InBody);
        }

        LOOP: for (int pos = bottom; pos >= upper; pos--) {
            Element node = stack.get(pos);
            if (pos == upper) {
                last = true;
                if (fragmentParsing)
                    node = contextElement;
            }
            String name = node != null ? node.normalName() : "";
            if (!NamespaceHtml.equals(node.tag().namespace()))
                continue; // only looking for HTML elements here

            switch (name) {
                case "select":
                    transition(HtmlTreeBuilderState.InSelect);
                    // todo - should loop up (with some limit) and check for table or template hits
                    break LOOP;
                case "td":
                case "th":
                    if (!last) {
                        transition(HtmlTreeBuilderState.InCell);
                        break LOOP;
                    }
                    break;
                case "tr":
                    transition(HtmlTreeBuilderState.InRow);
                    break LOOP;
                case "tbody":
                case "thead":
                case "tfoot":
                    transition(HtmlTreeBuilderState.InTableBody);
                    break LOOP;
                case "caption":
                    transition(HtmlTreeBuilderState.InCaption);
                    break LOOP;
                case "colgroup":
                    transition(HtmlTreeBuilderState.InColumnGroup);
                    break LOOP;
                case "table":
                    transition(HtmlTreeBuilderState.InTable);
                    break LOOP;
                case "template":
                    HtmlTreeBuilderState tmplState = currentTemplateMode();
                    Validate.notNull(tmplState, "Bug: no template insertion mode on stack!");
                    transition(tmplState);
                    break LOOP;
                case "head":
                    if (!last) {
                        transition(HtmlTreeBuilderState.InHead);
                        break LOOP;
                    }
                    break;
                case "body":
                    transition(HtmlTreeBuilderState.InBody);
                    break LOOP;
                case "frameset":
                    transition(HtmlTreeBuilderState.InFrameset);
                    break LOOP;
                case "html":
                    transition(headElement == null ? HtmlTreeBuilderState.BeforeHead : HtmlTreeBuilderState.AfterHead);
                    break LOOP;
            }
            if (last) {
                transition(HtmlTreeBuilderState.InBody);
                break;
            }
        }
        return state != origState;
    }

    /** Places the body back onto the stack and moves to InBody, for cases in AfterBody / AfterAfterBody when more content comes */
    void resetBody() {
        if (!onStack("body")) {
            stack.add(doc.body()); // not onNodeInserted, as already seen
        }
        transition(HtmlTreeBuilderState.InBody);
    }

    // todo: tidy up in specific scope methods
    private final String[] specificScopeTarget = {null};

    private boolean inSpecificScope(String targetName, String[] baseTypes, String[] extraTypes) {
        specificScopeTarget[0] = targetName;
        return inSpecificScope(specificScopeTarget, baseTypes, extraTypes);
    }

    private boolean inSpecificScope(String[] targetNames, String[] baseTypes, @Nullable String[] extraTypes) {
        // https://html.spec.whatwg.org/multipage/parsing.html#has-an-element-in-the-specific-scope
        final int bottom = stack.size() -1;
        final int top = bottom > MaxScopeSearchDepth ? bottom - MaxScopeSearchDepth : 0;
        // don't walk too far up the tree

        for (int pos = bottom; pos >= top; pos--) {
            Element el = stack.get(pos);
            if (!el.tag().namespace().equals(NamespaceHtml)) continue;

            final String elName = el.normalName();
            if (inSorted(elName, targetNames))
                return true;
            if (inSorted(elName, baseTypes))
                return false;
            if (extraTypes != null && inSorted(elName, extraTypes))
                return false;
        }
        //Validate.fail("Should not be reachable"); // would end up false because hitting 'html' at root (basetypes)
        return false;
    }

    boolean inScope(String[] targetNames) {
        return inSpecificScope(targetNames, TagsSearchInScope, null);
    }

    boolean inScope(String targetName) {
        return inScope(targetName, null);
    }

    boolean inScope(String targetName, String[] extras) {
        return inSpecificScope(targetName, TagsSearchInScope, extras);
        // todo: in mathml namespace: mi, mo, mn, ms, mtext annotation-xml
        // todo: in svg namespace: forignOjbect, desc, title
    }

    boolean inListItemScope(String targetName) {
        return inScope(targetName, TagSearchList);
    }

    boolean inButtonScope(String targetName) {
        return inScope(targetName, TagSearchButton);
    }

    boolean inTableScope(String targetName) {
        return inSpecificScope(targetName, TagSearchTableScope, null);
    }

    boolean inSelectScope(String targetName) {
        for (int pos = stack.size() -1; pos >= 0; pos--) {
            Element el = stack.get(pos);
            String elName = el.normalName();
            if (elName.equals(targetName))
                return true;
            if (!inSorted(elName, TagSearchSelectScope)) // all elements except
                return false;
        }
        Validate.fail("Should not be reachable");
        return false;
    }

    /** Tests if there is some element on the stack that is not in the provided set. */
    boolean onStackNot(String[] allowedTags) {
        final int bottom = stack.size() -1;
        final int top = bottom > MaxScopeSearchDepth ? bottom - MaxScopeSearchDepth : 0;
        // don't walk too far up the tree

        for (int pos = bottom; pos >= top; pos--) {
            final String elName = stack.get(pos).normalName();
            if (!inSorted(elName, allowedTags))
                return true;
        }
        return false;
    }

    void setHeadElement(Element headElement) {
        this.headElement = headElement;
    }

    Element getHeadElement() {
        return headElement;
    }

    boolean isFosterInserts() {
        return fosterInserts;
    }

    void setFosterInserts(boolean fosterInserts) {
        this.fosterInserts = fosterInserts;
    }

    @Nullable FormElement getFormElement() {
        return formElement;
    }

    void setFormElement(FormElement formElement) {
        this.formElement = formElement;
    }

    void resetPendingTableCharacters() {
        pendingTableCharacters.clear();
    }

    List<Token.Character> getPendingTableCharacters() {
        return pendingTableCharacters;
    }

    void addPendingTableCharacters(Token.Character c) {
        // make a clone of the token to maintain its state (as Tokens are otherwise reset)
        Token.Character clone = c.clone();
        pendingTableCharacters.add(clone);
    }

    /**
     13.2.6.3 Closing elements that have implied end tags
     When the steps below require the UA to generate implied end tags, then, while the current node is a dd element, a dt element, an li element, an optgroup element, an option element, a p element, an rb element, an rp element, an rt element, or an rtc element, the UA must pop the current node off the stack of open elements.

     If a step requires the UA to generate implied end tags but lists an element to exclude from the process, then the UA must perform the above steps as if that element was not in the above list.

     When the steps below require the UA to generate all implied end tags thoroughly, then, while the current node is a caption element, a colgroup element, a dd element, a dt element, an li element, an optgroup element, an option element, a p element, an rb element, an rp element, an rt element, an rtc element, a tbody element, a td element, a tfoot element, a th element, a thead element, or a tr element, the UA must pop the current node off the stack of open elements.

     @param excludeTag If a step requires the UA to generate implied end tags but lists an element to exclude from the
     process, then the UA must perform the above steps as if that element was not in the above list.
     */
    void generateImpliedEndTags(String excludeTag) {
        while (inSorted(currentElement().normalName(), TagSearchEndTags)) {
            if (excludeTag != null && currentElementIs(excludeTag))
                break;
            pop();
        }
    }

    void generateImpliedEndTags() {
        generateImpliedEndTags(false);
    }

    /**
     Pops HTML elements off the stack according to the implied end tag rules
     @param thorough if we are thorough (includes table elements etc) or not
     */
    void generateImpliedEndTags(boolean thorough) {
        final String[] search = thorough ? TagThoroughSearchEndTags : TagSearchEndTags;
        while (NamespaceHtml.equals(currentElement().tag().namespace())
            && inSorted(currentElement().normalName(), search)) {
            pop();
        }
    }

    void closeElement(String name) {
        generateImpliedEndTags(name);
        if (!name.equals(currentElement().normalName())) error(state());
        popStackToClose(name);
    }

    static boolean isSpecial(Element el) {
        // todo: mathml's mi, mo, mn
        // todo: svg's foreigObject, desc, title
        String name = el.normalName();
        return inSorted(name, TagSearchSpecial);
    }

    Element lastFormattingElement() {
        return formattingElements.size() > 0 ? formattingElements.get(formattingElements.size()-1) : null;
    }

    int positionOfElement(Element el){
        for (int i = 0; i < formattingElements.size(); i++){
            if (el == formattingElements.get(i))
                return i;
        }
        return -1;
    }

    Element removeLastFormattingElement() {
        int size = formattingElements.size();
        if (size > 0)
            return formattingElements.remove(size-1);
        else
            return null;
    }

    // active formatting elements
    void pushActiveFormattingElements(Element in) {
        checkActiveFormattingElements(in);
        formattingElements.add(in);
    }

    void pushWithBookmark(Element in, int bookmark){
        checkActiveFormattingElements(in);
        // catch any range errors and assume bookmark is incorrect - saves a redundant range check.
        try {
            formattingElements.add(bookmark, in);
        } catch (IndexOutOfBoundsException e) {
            formattingElements.add(in);
        }
    }

    void checkActiveFormattingElements(Element in){
        int numSeen = 0;
        final int size = formattingElements.size() -1;
        int ceil = size - maxUsedFormattingElements; if (ceil <0) ceil = 0;

        for (int pos = size; pos >= ceil; pos--) {
            Element el = formattingElements.get(pos);
            if (el == null) // marker
                break;

            if (isSameFormattingElement(in, el))
                numSeen++;

            if (numSeen == 3) {
                formattingElements.remove(pos);
                break;
            }
        }
    }

    private static boolean isSameFormattingElement(Element a, Element b) {
        // same if: same namespace, tag, and attributes. Element.equals only checks tag, might in future check children
        return a.normalName().equals(b.normalName()) &&
                // a.namespace().equals(b.namespace()) &&
                a.attributes().equals(b.attributes());
        // todo: namespaces
    }

    void reconstructFormattingElements() {
        if (stack.size() > maxQueueDepth)
            return;
        Element last = lastFormattingElement();
        if (last == null || onStack(last))
            return;

        Element entry = last;
        int size = formattingElements.size();
        int ceil = size - maxUsedFormattingElements; if (ceil <0) ceil = 0;
        int pos = size - 1;
        boolean skip = false;
        while (true) {
            if (pos == ceil) { // step 4. if none before, skip to 8
                skip = true;
                break;
            }
            entry = formattingElements.get(--pos); // step 5. one earlier than entry
            if (entry == null || onStack(entry)) // step 6 - neither marker nor on stack
                break; // jump to 8, else continue back to 4
        }
        while(true) {
            if (!skip) // step 7: on later than entry
                entry = formattingElements.get(++pos);
            Validate.notNull(entry); // should not occur, as we break at last element

            // 8. create new element from element, 9 insert into current node, onto stack
            skip = false; // can only skip increment from 4.
            Element newEl = new Element(tagFor(entry.normalName(), settings), null, entry.attributes().clone());
            doInsertElement(newEl, null);

            // 10. replace entry with new entry
            formattingElements.set(pos, newEl);

            // 11
            if (pos == size-1) // if not last entry in list, jump to 7
                break;
        }
    }
    private static final int maxUsedFormattingElements = 12; // limit how many elements get recreated

    void clearFormattingElementsToLastMarker() {
        while (!formattingElements.isEmpty()) {
            Element el = removeLastFormattingElement();
            if (el == null)
                break;
        }
    }

    void removeFromActiveFormattingElements(Element el) {
        for (int pos = formattingElements.size() -1; pos >= 0; pos--) {
            Element next = formattingElements.get(pos);
            if (next == el) {
                formattingElements.remove(pos);
                break;
            }
        }
    }

    boolean isInActiveFormattingElements(Element el) {
        return onStack(formattingElements, el);
    }

    @Nullable
    Element getActiveFormattingElement(String nodeName) {
        for (int pos = formattingElements.size() -1; pos >= 0; pos--) {
            Element next = formattingElements.get(pos);
            if (next == null) // scope marker
                break;
            else if (next.nameIs(nodeName))
                return next;
        }
        return null;
    }

    void replaceActiveFormattingElement(Element out, Element in) {
        replaceInQueue(formattingElements, out, in);
    }

    void insertMarkerToFormattingElements() {
        formattingElements.add(null);
    }

    void insertInFosterParent(Node in) {
        Element fosterParent;
        Element lastTable = getFromStack("table");
        boolean isLastTableParent = false;
        if (lastTable != null) {
            if (lastTable.parent() != null) {
                fosterParent = lastTable.parent();
                isLastTableParent = true;
            } else
                fosterParent = aboveOnStack(lastTable);
        } else { // no table == frag
            fosterParent = stack.get(0);
        }

        if (isLastTableParent) {
            Validate.notNull(lastTable); // last table cannot be null by this point.
            lastTable.before(in);
        }
        else
            fosterParent.appendChild(in);
    }

    // Template Insertion Mode stack
    void pushTemplateMode(HtmlTreeBuilderState state) {
        tmplInsertMode.add(state);
    }

    @Nullable HtmlTreeBuilderState popTemplateMode() {
        if (tmplInsertMode.size() > 0) {
            return tmplInsertMode.remove(tmplInsertMode.size() -1);
        } else {
            return null;
        }
    }

    int templateModeSize() {
        return tmplInsertMode.size();
    }

    @Nullable HtmlTreeBuilderState currentTemplateMode() {
        return (tmplInsertMode.size() > 0) ? tmplInsertMode.get(tmplInsertMode.size() -1)  : null;
    }

    @Override
    public String toString() {
        return "TreeBuilder{" +
                "currentToken=" + currentToken +
                ", state=" + state +
                ", currentElement=" + currentElement() +
                '}';
    }

    @Override protected boolean isContentForTagData(final String normalName) {
        return (normalName.equals("script") || normalName.equals("style"));
    }
}
