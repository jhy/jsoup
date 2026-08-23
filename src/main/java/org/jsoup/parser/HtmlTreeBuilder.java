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
import org.jsoup.nodes.NodeInternals;
import org.jsoup.nodes.TextNode;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.jsoup.internal.StringUtil.inSorted;
import static org.jsoup.parser.HtmlTreeBuilderState.Constants.Headings;
import static org.jsoup.parser.HtmlTreeBuilderState.Constants.InTableFoster;
import static org.jsoup.parser.HtmlTreeBuilderState.ForeignContent;
import static org.jsoup.parser.Parser.*;

/**
 * HTML Tree Builder; creates a DOM from Tokens.
 */
public class HtmlTreeBuilder extends TreeBuilder {
    static final String[] TagMathMlTextIntegration = new String[]{"mi", "mn", "mo", "ms", "mtext"};
    static final String[] TagSvgHtmlIntegration = new String[]{"desc", "foreignObject", "title"};
    static final String[] TagFormListed = {
        "button", "fieldset", "input", "keygen", "object", "output", "select", "textarea"
    };

    /** @deprecated Not used anymore; configure parser depth via {@link Parser#setMaxDepth(int)}. Will be removed in jsoup 1.24.1. */
    @Deprecated
    public static final int MaxScopeSearchDepth = 100;

    private HtmlTreeBuilderState state; // the current state
    private HtmlTreeBuilderState originalState; // original / marked state

    private boolean baseUriSetFromDoc;
    private @Nullable Element headElement; // the current head element
    private @Nullable FormElement formElement; // the current form element
    private @Nullable Element contextElement; // fragment parse root; shallow copy of context, may be null during fragment parsing
    ArrayList<Element> formattingElements; // active (open) formatting elements
    private ArrayList<HtmlTreeBuilderState> tmplInsertMode; // stack of Template Insertion modes
    private @Nullable NoscriptState noscriptState; // active noscript island state
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
        noscriptState = null;
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
            contextElement = new Element(context.tag(), baseUri);
            contextElement.attributes().addAll(context.attributes());
            if (context.ownerDocument() != null) // quirks setup:
                doc.quirksMode(context.ownerDocument().quirksMode());

            // initialise the tokeniser state
            Tag contextTag = contextElement.tag();
            boolean htmlContext = NamespaceHtml.equals(contextTag.namespace());
            TokeniserState contextState = contextTag.textState(); // style, xmp, title, textarea, etc; or custom
            if (contextState == null) contextState = TokeniserState.Data;

            switch (contextName) {
                case "script":
                    if (htmlContext) {
                        contextState = TokeniserState.ScriptData;
                    } else if (NamespaceSvg.equals(contextTag.namespace())) {
                        // svg script enters script data during document parsing, but fragments start in data so markup creates svg children
                        contextState = TokeniserState.Data;
                    }
                    break;
                case "plaintext":
                    if (htmlContext) contextState = TokeniserState.PLAINTEXT;
                    break;
                case "template":
                    if (htmlContext) {
                        contextState = TokeniserState.Data;
                        pushTemplateMode(HtmlTreeBuilderState.InTemplate);
                    }
                    break;
            }
            tokeniser.transition(contextState);
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

            if (htmlContext && contextName.equals("noscript")) enterNoscript(contextElement);
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
        if (noscriptState != null && state != HtmlTreeBuilderState.Text)
            return processNoscriptToken(token);
        HtmlTreeBuilderState dispatch = useCurrentOrForeignInsert(token) ? this.state : ForeignContent;
        return dispatch.process(token, this);
    }

    /**
     Handles tokens in a noscript island as plain contained markup. This diverges from the spec intentionally so that
     content is available in the DOM and round-trip serializable, but errant content won't change the parser's context
     (e.g. an `a` won't kick out of InHead).
     */
    private boolean processNoscriptToken(Token token) {
        switch (token.type) {
            case StartTag:
                return insertNoscriptStartTag(token.asStartTag());
            case EndTag:
                return closeNoscriptEndTag(token.asEndTag());
            case Comment:
                insertCommentNode(token.asComment());
                return true;
            case Character:
                Token.Character character = token.asCharacter();
                insertCharacterNode(character);
                if (!StringUtil.isBlank(character.getData()))
                    framesetOk(false);
                return true;
            case Doctype:
                error(state);
                return false;
            case EOF:
                error(state);
                endNoscript();
                return process(token);
            default:
                Validate.wtf("Unexpected state: " + token.type); // XmlDecl only in XmlTreeBuilder
                return false;
        }
    }

    /**
     Inserts a start tag inside a noscript island as plain markup.
     */
    private boolean insertNoscriptStartTag(Token.StartTag start) {
        TokeniserState textState = tagFor(start).textState();
        Element el = insertElementFor(start);
        if (textState != null) { // plaintext is intentionally not TagSet-driven and remains plain fallback markup.
            if (start.isSelfClosing()) {
                if (currentElement() == el)
                    pop();
            } else {
                tokeniser.transition(textState);
                markInsertionMode();
                transition(HtmlTreeBuilderState.Text);
            }
        }

        framesetOk(false);
        return true;
    }

    /**
     Closes an island element if it matches above the current noscript boundary.
     */
    private boolean closeNoscriptEndTag(Token.EndTag end) {
        String name = end.normalName();
        NoscriptState island = Validate.expectNotNull(noscriptState, "Bug: noscript end tag processed with no island state");
        if (name.equals("noscript") && island.boundary != contextElement) {
            endNoscript();
            return true;
        }
        if (!inNoscriptScope(name)) {
            error(state);
            return false;
        }
        if (!currentElementIs(name))
            error(state);
        popStackToClose(name);
        return true;
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
        // note using .tagName for case-sensitive hit here of foreignObject
        return Parser.NamespaceSvg.equals(el.tag().namespace()) && StringUtil.in(el.tagName(), TagSvgHtmlIntegration);
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
        if (attributes != null && !attributes.isEmpty()) {
            if (!forcePreserveCase)
                settings.normalizeAttributes(attributes);
            int dupes = attributes.deduplicate(settings);
            if (dupes > 0) {
                error("Dropped duplicate attribute(s) in tag [%s]", startTag.normalName);
            }
            startTag.finaliseAttributeRanges(forcePreserveCase ? ParseSettings.preserveCase : settings);
        }

        Tag tag = tagFor(startTag.name(), startTag.normalName, namespace,
            forcePreserveCase ? ParseSettings.preserveCase : settings);

        return (tag.normalName().equals("form")) ?
            new FormElement(tag, null, attributes) :
            new Element(tag, null, attributes);
    }

    /** Inserts an HTML element for the given tag */
    Element insertElementFor(final Token.StartTag startTag) {
        Element el = createElementFor(startTag, NamespaceHtml, false);
        doInsertElement(el);

        // handle self-closing tags. when the spec expects an empty (void) tag, will directly hit insertEmpty, so won't generate this fake end tag.
        if (startTag.isSelfClosing()) {
            Tag tag = el.tag();
            tag.setSeenSelfClose(); // can infer output if in xml syntax
            if (tag.isEmpty()) {
                // treated as empty below; nothing further
            } else if (tag.isKnownTag() && tag.isSelfClosing()) {
                // ok, allow it. effectively a pop, but fiddles with the state. handles empty style, title etc which would otherwise leave us in data state
                tokeniser.transition(TokeniserState.Data); // handles <script />, otherwise needs breakout steps from script data
                tokeniser.emit(emptyEnd.reset().name(el.tagName()));  // ensure we get out of whatever state we are in. emitted for yielded processing
            } else {
                // error it, and leave the inserted element on
                tokeniser.error("Tag [%s] cannot be self-closing; not a void tag", tag.normalName());
            }
        }

        if (el.tag().isEmpty()) {
            pop(); // custom void tags behave like built-in voids (no children, not left on the stack); known empty go via insertEmpty
        }

        return el;
    }

    /**
     Inserts a foreign element. Preserves the case of the tag name and of the attributes.
     */
    Element insertForeignElementFor(final Token.StartTag startTag, String namespace) {
        Element el = createElementFor(startTag, namespace, true);
        doInsertElement(el);

        if (startTag.isSelfClosing()) { // foreign els are OK to self-close
            el.tag().setSeenSelfClose(); // remember this is self-closing for output
            pop();
        }

        return el;
    }

    Element insertEmptyElementFor(Token.StartTag startTag) {
        Element el = createElementFor(startTag, NamespaceHtml, false);
        doInsertElement(el);
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

        doInsertElement(el);
        if (!onStack) pop();
        return el;
    }

    /** Inserts the Element onto the stack. All element inserts must run through this method. Performs any general
     tests on the Element before insertion.
     * @param el the Element to insert and make the current element
     */
    private void doInsertElement(Element el) {
        enforceStackDepthLimit();

        if (formElement != null && el.tag().namespace.equals(NamespaceHtml) && StringUtil.inSorted(el.normalName(), TagFormListed))
            formElement.addElement(el); // connect form controls to their form element

        // in HTML, the xmlns attribute if set must match what the parser set the tag's namespace to
        if (parser.getErrors().canAddError() && el.hasAttr("xmlns") && !el.attr("xmlns").equals(el.tag().namespace()))
            error("Invalid xmlns attribute [%s] on tag [%s]", el.attr("xmlns"), el.tagName());

        Element target = currentElOrDoc();
        if (isFosterInserts() && StringUtil.inSorted(target.normalName(), InTableFoster))
            insertInFosterParent(el);
        else
            target.appendChild(el);

        push(el);
    }

    /** Inserts a comment into the current element, or the document when there is none. */
    void insertCommentNode(Token.Comment token) {
        insertCommentNode(token, currentElOrDoc());
    }

    /** Inserts a comment into the supplied target. */
    void insertCommentNode(Token.Comment token, Element target) {
        Comment node = new Comment(token.getData());
        target.appendChild(node);
        onNodeInserted(node);
    }

    /** Inserts the provided character token into the current element, or the document when there is none. */
    void insertCharacterNode(Token.Character characterToken) {
        insertCharacterNode(characterToken, false);
    }

    /**
     Inserts the provided character token into the current element, or the document when there is none.
     The tokenizer will have already raised precise character errors.

     @param characterToken the character token to insert
     @param replace if true, replaces any null chars in the data with the replacement char (U+FFFD). If false, removes
     null chars.
     */
    void insertCharacterNode(Token.Character characterToken, boolean replace) {
        characterToken.normalizeNulls(replace);
        Element el = currentElOrDoc();
        insertCharacterToElement(characterToken, el);
    }

    /** Inserts the provided character token into the provided element. */
    void insertCharacterToElement(Token.Character characterToken, Element el) {
        final Node node;
        final String data = characterToken.getData();

        if (characterToken.isCData())
            node = new CDataNode(data);
        else if (el.tag().is(Tag.Data))
            node = new DataNode(data);
        else
            node = new TextNode(data);
        el.appendChild(node); // doesn't use insertNode, because we don't foster these
        onNodeInserted(node);
    }

    ArrayList<Element> getStack() {
        return stack;
    }

    boolean onStack(Element el) {
        return stack.contains(el);
    }

    /** Checks if there is an HTML element with the given name on the stack. */
    boolean onStack(String elName) {
        return getFromStack(elName) != null;
    }

    /** Checks if there is an HTML element with the given name above the synthetic fragment context. */
    boolean onStackAboveContext(String elName) {
        Element el = getFromStack(elName);
        return el != null && el != contextElement;
    }

    /** Gets the nearest (lowest) HTML element with the given name from the stack. */
    @Nullable
    Element getFromStack(String elName) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
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

    @Override
    void onStackPrunedForDepth(Element element) {
        // handle other effects of popping to keep state correct
        if (element == headElement) headElement = null;
        if (element == formElement) setFormElement(null);
        removeFromActiveFormattingElements(element);
        if (element.nameIs("template")) {
            clearFormattingElementsToLastMarker();
            if (templateModeSize() > 0)
                popTemplateMode();
            resetInsertionMode();
        } else if (noscriptState != null && element == noscriptState.boundary) {
            restoreNoscriptState();
        }
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

    /**
     Gets the Element immediately above the supplied element on the stack. Which due to adoption, may not necessarily be
     its parent.

     @param el
     @return the Element immediately above the supplied element, or null if there is no such element.
     */
    @Nullable Element aboveOnStack(Element el) {
        if (!onStack(el)) return null;
        for (int pos = stack.size() -1; pos > 0; pos--) {
            Element next = stack.get(pos);
            if (next == el) {
                return stack.get(pos-1);
            }
        }
        return null;
    }

    void insertOnStackAfter(Element after, Element in) {
        int i = stack.lastIndexOf(after);
        if (i == -1) {
            error("Did not find element on stack to insert after");
            stack.add(in);
            // may happen on particularly malformed inputs during adoption
        } else {
            stack.add(i+1, in);
        }
    }

    void replaceOnStack(Element out, Element in) {
        replaceInQueue(stack, out, in);
    }

    private static void replaceInQueue(ArrayList<Element> queue, Element out, Element in) {
        int i = queue.lastIndexOf(out);
        Validate.isTrue(i != -1);
        queue.set(i, in);
    }

    /** Reset the insertion mode by searching up the stack for an appropriate insertion mode. */
    boolean resetInsertionMode() {
        // https://html.spec.whatwg.org/multipage/parsing.html#the-insertion-mode
        final HtmlTreeBuilderState origState = this.state;

        if (stack.size() == 0) { // nothing left of stack, just get to body
            transition(HtmlTreeBuilderState.InBody);
        }

        LOOP: for (int pos = stack.size() - 1; pos >= 0; pos--) {
            boolean last = pos == 0;
            Element node = last && fragmentParsing ? contextElement : stack.get(pos);
            String name = node != null && NamespaceHtml.equals(node.tag().namespace()) ? node.normalName() : "";

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

    /**
     Test if the target element is in the requested scope.
     */
    private boolean inSpecificScope(String targetName, int boundaryOptions) {
        // https://html.spec.whatwg.org/multipage/parsing.html#has-an-element-in-the-specific-scope
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            Element el = stack.get(pos);
            Tag tag = el.tag();
            if (NamespaceHtml.equals(tag.namespace()) && el.normalName().equals(targetName))
                return true;
            if (tag.hasParserOption(boundaryOptions))
                return false;
        }
        return false;
    }

    /**
     Test if any heading element is in scope.
     */
    boolean hasHeadingInScope() {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            Element el = stack.get(pos);
            Tag tag = el.tag();
            if (NamespaceHtml.equals(tag.namespace()) && inSorted(el.normalName(), Headings))
                return true;
            if (tag.hasParserOption(HtmlTagOptions.Scope))
                return false;
        }
        return false;
    }

    boolean inScope(String targetName) {
        return inSpecificScope(targetName, HtmlTagOptions.Scope);
    }

    boolean inListItemScope(String targetName) {
        return inSpecificScope(targetName, HtmlTagOptions.Scope | HtmlTagOptions.ListScope);
    }

    boolean inButtonScope(String targetName) {
        return inSpecificScope(targetName, HtmlTagOptions.Scope | HtmlTagOptions.ButtonScope);
    }

    boolean inTableScope(String targetName) {
        return inSpecificScope(targetName, HtmlTagOptions.TableScope);
    }

    boolean inSelectScope(String targetName) {
        for (int pos = stack.size() -1; pos >= 0; pos--) {
            Element el = stack.get(pos);
            String elName = el.normalName();
            if (elName.equals(targetName))
                return true;
            // Select scope stops at the first element that is not option / optgroup.
            if (!el.tag().hasParserOption(HtmlTagOptions.SelectScopeMember))
                return false;
        }
        return false; // nothing left on stack
    }

    /** Tests if there is some element on the stack that is not in the provided set. */
    boolean onStackNot(String[] allowedTags) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
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

    void setFormElement(@Nullable FormElement formElement) {
        this.formElement = formElement;
    }

    private static final class NoscriptState {
        final Element boundary;
        final @Nullable FormElement savedFormElement;

        /**
         Captures parser state isolated by the active noscript island.
        */
        NoscriptState(Element boundary, @Nullable FormElement formElement) {
            this.boundary = boundary;
            this.savedFormElement = formElement;
        }
    }

    /** Starts a noscript island, preserving parser-global state for restoration on close. */
    void startNoscript(Token.StartTag startTag) {
        Element boundary = insertElementFor(startTag);
        enterNoscript(boundary);
    }

    /** Enters a noscript island around the provided boundary element. */
    private void enterNoscript(Element boundary) {
        noscriptState = new NoscriptState(boundary, formElement);
        // Fallback form elements should not leak through the parser form pointer.
        setFormElement(null);
    }

    /** Tests if the named element is above the current noscript boundary. */
    private boolean inNoscriptScope(String name) {
        NoscriptState state = noscriptState;
        if (state == null)
            return false;
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            Element el = stack.get(pos);
            if (el == state.boundary)
                return false;
            if (el.nameIs(name))
                return true;
        }
        return false;
    }

    /** Closes the active noscript subtree and restores isolated parser state. */
    private void endNoscript() {
        NoscriptState state = Validate.expectNotNull(noscriptState, "Bug: noscript fallback closed with no island state");
        int boundary = noscriptBoundaryIndex(state);
        if (boundary == -1) {
            error(this.state);
            restoreNoscriptState();
            return;
        }
        if (stack.get(stack.size() - 1) != state.boundary)
            error(this.state);
        while (stack.size() > boundary)
            pop();
        restoreNoscriptState();
    }

    /** Finds the active noscript boundary on the stack by identity. */
    private int noscriptBoundaryIndex(NoscriptState state) {
        for (int pos = stack.size() - 1; pos >= 0; pos--) {
            if (stack.get(pos) == state.boundary)
                return pos;
        }
        return -1;
    }

    /** Restores parser-global state from the active noscript island. */
    private void restoreNoscriptState() {
        NoscriptState state = Validate.expectNotNull(noscriptState, "Bug: no noscript island state to restore");
        noscriptState = null;
        setFormElement(state.savedFormElement);
    }

    void resetPendingTableCharacters() {
        pendingTableCharacters.clear();
    }

    List<Token.Character> getPendingTableCharacters() {
        return pendingTableCharacters;
    }

    void addPendingTableCharacters(Token.Character c) {
        // make a copy of the token to maintain its state (as Tokens are otherwise reset)
        Token.Character copy = new Token.Character(c);
        pendingTableCharacters.add(copy);
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
        while (hasCurrentElement() && currentElement().tag().hasParserOption(HtmlTagOptions.ImpliedEnd)) {
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
        final int option = thorough ? HtmlTagOptions.ThoroughImpliedEnd : HtmlTagOptions.ImpliedEnd;
        while (true) {
            if (!hasCurrentElement()) break;
            Tag tag = currentElement().tag();
            if (!tag.hasParserOption(option))
                break;
            pop();
        }
    }

    void closeElement(String name) {
        generateImpliedEndTags(name);
        if (!currentElementIs(name)) error(state());
        popStackToClose(name);
    }

    static boolean isSpecial(Element el) {
        return el.tag().hasParserOption(HtmlTagOptions.Special);
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
            Element newEl = recreateElement(entry);
            doInsertElement(newEl);

            // 10. replace entry with new entry
            formattingElements.set(pos, newEl);

            // 11
            if (pos == size-1) // if not last entry in list, jump to 7
                break;
        }
    }

    /** Copies an element's originating source ranges, ready to track a new close. */
    Element recreateElement(Element source) {
        Element element = new Element(source.tag(), null, source.attributes().clone());
        NodeInternals.clearEndSourceRange(element);
        return element;
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
        return formattingElements.contains(el);
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
                ", currentElement=" + (hasCurrentElement() ? currentElement() : "none") +
                '}';
    }

}
