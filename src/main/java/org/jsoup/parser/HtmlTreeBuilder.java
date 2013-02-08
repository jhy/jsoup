package org.jsoup.parser;

import org.jsoup.helper.DescendableLinkedList;
import org.jsoup.helper.StringUtil;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * HTML Tree Builder; creates a DOM from Tokens.
 */
class HtmlTreeBuilder extends TreeBuilder {

    private HtmlTreeBuilderState state; // the current state
    private HtmlTreeBuilderState originalState; // original / marked state

    private boolean baseUriSetFromDoc = false;
    private Element headElement; // the current head element
    private FormElement formElement; // the current form element
    private Element contextElement; // fragment parse context -- could be null even if fragment parsing
    private DescendableLinkedList<Element> formattingElements = new DescendableLinkedList<Element>(); // active (open) formatting elements
    private List<Token.Character> pendingTableCharacters = new ArrayList<Token.Character>(); // chars in table to be shifted out

    private boolean framesetOk = true; // if ok to go into frameset
    private boolean fosterInserts = false; // if next inserts should be fostered
    private boolean fragmentParsing = false; // if parsing a fragment of html

    HtmlTreeBuilder() {}

    @Override
    Document parse(String input, String baseUri, ParseErrorList errors) {
        state = HtmlTreeBuilderState.Initial;
        return super.parse(input, baseUri, errors);
    }

    List<Node> parseFragment(String inputFragment, Element context, String baseUri, ParseErrorList errors) {
        // context may be null
        state = HtmlTreeBuilderState.Initial;
        initialiseParse(inputFragment, baseUri, errors);
        contextElement = context;
        fragmentParsing = true;
        Element root = null;

        if (context != null) {
            if (context.ownerDocument() != null) // quirks setup:
                doc.quirksMode(context.ownerDocument().quirksMode());

            // initialise the tokeniser state:
            String contextTag = context.tagName();
            if (StringUtil.in(contextTag, "title", "textarea"))
                tokeniser.transition(TokeniserState.Rcdata);
            else if (StringUtil.in(contextTag, "iframe", "noembed", "noframes", "style", "xmp"))
                tokeniser.transition(TokeniserState.Rawtext);
            else if (contextTag.equals("script"))
                tokeniser.transition(TokeniserState.ScriptData);
            else if (contextTag.equals(("noscript")))
                tokeniser.transition(TokeniserState.Data); // if scripting enabled, rawtext
            else if (contextTag.equals("plaintext"))
                tokeniser.transition(TokeniserState.Data);
            else
                tokeniser.transition(TokeniserState.Data); // default

            root = new Element(Tag.valueOf("html"), baseUri);
            doc.appendChild(root);
            stack.push(root);
            resetInsertionMode();

            // setup form element to nearest form on context (up ancestor chain). ensures form controls are associated
            // with form correctly
            Elements contextChain = context.parents();
            contextChain.add(0, context);
            for (Element parent: contextChain) {
                if (parent instanceof FormElement) {
                    formElement = (FormElement) parent;
                    break;
                }
            }
        }

        runParser();
        if (context != null)
            return root.childNodes();
        else
            return doc.childNodes();
    }

    @Override
    protected boolean process(Token token) {
        currentToken = token;
        return this.state.process(token, this);
    }

    boolean process(Token token, HtmlTreeBuilderState state) {
        currentToken = token;
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
        if (errors.canAddError())
            errors.add(new ParseError(reader.pos(), "Unexpected token [%s] when in state [%s]", currentToken.tokenType(), state));
    }

    Element insert(Token.StartTag startTag) {
        // handle empty unknown tags
        // when the spec expects an empty tag, will directly hit insertEmpty, so won't generate this fake end tag.
        if (startTag.isSelfClosing()) {
            Element el = insertEmpty(startTag);
            stack.add(el);
            tokeniser.emit(new Token.EndTag(el.tagName()));  // ensure we get out of whatever state we are in. emitted for yielded processing
            return el;
        }
        
        Element el = new Element(Tag.valueOf(startTag.name()), baseUri, startTag.attributes);
        insert(el);
        return el;
    }

    Element insert(String startTagName) {
        Element el = new Element(Tag.valueOf(startTagName), baseUri);
        insert(el);
        return el;
    }

    void insert(Element el) {
        insertNode(el);
        stack.add(el);
    }

    Element insertEmpty(Token.StartTag startTag) {
        Tag tag = Tag.valueOf(startTag.name());
        Element el = new Element(tag, baseUri, startTag.attributes);
        insertNode(el);
        if (startTag.isSelfClosing()) {
            if (tag.isKnownTag()) {
                if (tag.isSelfClosing()) tokeniser.acknowledgeSelfClosingFlag(); // if not acked, promulagates error
            } else {
                // unknown tag, remember this is self closing for output
                tag.setSelfClosing();
                tokeniser.acknowledgeSelfClosingFlag(); // not an distinct error
            }
        }
        return el;
    }

    FormElement insertForm(Token.StartTag startTag, boolean onStack) {
        Tag tag = Tag.valueOf(startTag.name());
        FormElement el = new FormElement(tag, baseUri, startTag.attributes);
        setFormElement(el);
        insertNode(el);
        if (onStack)
            stack.add(el);
        return el;
    }

    void insert(Token.Comment commentToken) {
        Comment comment = new Comment(commentToken.getData(), baseUri);
        insertNode(comment);
    }

    void insert(Token.Character characterToken) {
        Node node;
        // characters in script and style go in as datanodes, not text nodes
        if (StringUtil.in(currentElement().tagName(), "script", "style"))
            node = new DataNode(characterToken.getData(), baseUri);
        else
            node = new TextNode(characterToken.getData(), baseUri);
        currentElement().appendChild(node); // doesn't use insertNode, because we don't foster these; and will always have a stack.
    }

    private void insertNode(Node node) {
        // if the stack hasn't been set up yet, elements (doctype, comments) go into the doc
        if (stack.size() == 0)
            doc.appendChild(node);
        else if (isFosterInserts())
            insertInFosterParent(node);
        else
            currentElement().appendChild(node);

        // connect form controls to their form element
        if (node instanceof Element && ((Element) node).tag().isFormListed()) {
            if (formElement != null)
                formElement.addElement((Element) node);
        }
    }

    Element pop() {
        // todo - dev, remove validation check
        if (stack.peekLast().nodeName().equals("td") && !state.name().equals("InCell"))
            Validate.isFalse(true, "pop td not in cell");
        if (stack.peekLast().nodeName().equals("html"))
            Validate.isFalse(true, "popping html!");
        return stack.pollLast();
    }

    void push(Element element) {
        stack.add(element);
    }

    DescendableLinkedList<Element> getStack() {
        return stack;
    }

    boolean onStack(Element el) {
        return isElementInQueue(stack, el);
    }

    private boolean isElementInQueue(DescendableLinkedList<Element> queue, Element element) {
        Iterator<Element> it = queue.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (next == element) {
                return true;
            }
        }
        return false;
    }

    Element getFromStack(String elName) {
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (next.nodeName().equals(elName)) {
                return next;
            }
        }
        return null;
    }

    boolean removeFromStack(Element el) {
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (next == el) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    void popStackToClose(String elName) {
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (next.nodeName().equals(elName)) {
                it.remove();
                break;
            } else {
                it.remove();
            }
        }
    }

    void popStackToClose(String... elNames) {
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (StringUtil.in(next.nodeName(), elNames)) {
                it.remove();
                break;
            } else {
                it.remove();
            }
        }
    }

    void popStackToBefore(String elName) {
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (next.nodeName().equals(elName)) {
                break;
            } else {
                it.remove();
            }
        }
    }

    void clearStackToTableContext() {
        clearStackToContext("table");
    }

    void clearStackToTableBodyContext() {
        clearStackToContext("tbody", "tfoot", "thead");
    }

    void clearStackToTableRowContext() {
        clearStackToContext("tr");
    }

    private void clearStackToContext(String... nodeNames) {
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (StringUtil.in(next.nodeName(), nodeNames) || next.nodeName().equals("html"))
                break;
            else
                it.remove();
        }
    }

    Element aboveOnStack(Element el) {
        assert onStack(el);
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (next == el) {
                return it.next();
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

    private void replaceInQueue(LinkedList<Element> queue, Element out, Element in) {
        int i = queue.lastIndexOf(out);
        Validate.isTrue(i != -1);
        queue.remove(i);
        queue.add(i, in);
    }

    void resetInsertionMode() {
        boolean last = false;
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element node = it.next();
            if (!it.hasNext()) {
                last = true;
                node = contextElement;
            }
            String name = node.nodeName();
            if ("select".equals(name)) {
                transition(HtmlTreeBuilderState.InSelect);
                break; // frag
            } else if (("td".equals(name) || "td".equals(name) && !last)) {
                transition(HtmlTreeBuilderState.InCell);
                break;
            } else if ("tr".equals(name)) {
                transition(HtmlTreeBuilderState.InRow);
                break;
            } else if ("tbody".equals(name) || "thead".equals(name) || "tfoot".equals(name)) {
                transition(HtmlTreeBuilderState.InTableBody);
                break;
            } else if ("caption".equals(name)) {
                transition(HtmlTreeBuilderState.InCaption);
                break;
            } else if ("colgroup".equals(name)) {
                transition(HtmlTreeBuilderState.InColumnGroup);
                break; // frag
            } else if ("table".equals(name)) {
                transition(HtmlTreeBuilderState.InTable);
                break;
            } else if ("head".equals(name)) {
                transition(HtmlTreeBuilderState.InBody);
                break; // frag
            } else if ("body".equals(name)) {
                transition(HtmlTreeBuilderState.InBody);
                break;
            } else if ("frameset".equals(name)) {
                transition(HtmlTreeBuilderState.InFrameset);
                break; // frag
            } else if ("html".equals(name)) {
                transition(HtmlTreeBuilderState.BeforeHead);
                break; // frag
            } else if (last) {
                transition(HtmlTreeBuilderState.InBody);
                break; // frag
            }
        }
    }

    // todo: tidy up in specific scope methods
    private boolean inSpecificScope(String targetName, String[] baseTypes, String[] extraTypes) {
        return inSpecificScope(new String[]{targetName}, baseTypes, extraTypes);
    }

    private boolean inSpecificScope(String[] targetNames, String[] baseTypes, String[] extraTypes) {
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element el = it.next();
            String elName = el.nodeName();
            if (StringUtil.in(elName, targetNames))
                return true;
            if (StringUtil.in(elName, baseTypes))
                return false;
            if (extraTypes != null && StringUtil.in(elName, extraTypes))
                return false;
        }
        Validate.fail("Should not be reachable");
        return false;
    }

    boolean inScope(String[] targetNames) {
        return inSpecificScope(targetNames, new String[]{"applet", "caption", "html", "table", "td", "th", "marquee", "object"}, null);
    }

    boolean inScope(String targetName) {
        return inScope(targetName, null);
    }

    boolean inScope(String targetName, String[] extras) {
        return inSpecificScope(targetName, new String[]{"applet", "caption", "html", "table", "td", "th", "marquee", "object"}, extras);
        // todo: in mathml namespace: mi, mo, mn, ms, mtext annotation-xml
        // todo: in svg namespace: forignOjbect, desc, title
    }

    boolean inListItemScope(String targetName) {
        return inScope(targetName, new String[]{"ol", "ul"});
    }

    boolean inButtonScope(String targetName) {
        return inScope(targetName, new String[]{"button"});
    }

    boolean inTableScope(String targetName) {
        return inSpecificScope(targetName, new String[]{"html", "table"}, null);
    }

    boolean inSelectScope(String targetName) {
        Iterator<Element> it = stack.descendingIterator();
        while (it.hasNext()) {
            Element el = it.next();
            String elName = el.nodeName();
            if (elName.equals(targetName))
                return true;
            if (!StringUtil.in(elName, "optgroup", "option")) // all elements except
                return false;
        }
        Validate.fail("Should not be reachable");
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

    FormElement getFormElement() {
        return formElement;
    }

    void setFormElement(FormElement formElement) {
        this.formElement = formElement;
    }

    void newPendingTableCharacters() {
        pendingTableCharacters = new ArrayList<Token.Character>();
    }

    List<Token.Character> getPendingTableCharacters() {
        return pendingTableCharacters;
    }

    void setPendingTableCharacters(List<Token.Character> pendingTableCharacters) {
        this.pendingTableCharacters = pendingTableCharacters;
    }

    /**
     11.2.5.2 Closing elements that have implied end tags<p/>
     When the steps below require the UA to generate implied end tags, then, while the current node is a dd element, a
     dt element, an li element, an option element, an optgroup element, a p element, an rp element, or an rt element,
     the UA must pop the current node off the stack of open elements.

     @param excludeTag If a step requires the UA to generate implied end tags but lists an element to exclude from the
     process, then the UA must perform the above steps as if that element was not in the above list.
     */
    void generateImpliedEndTags(String excludeTag) {
        while ((excludeTag != null && !currentElement().nodeName().equals(excludeTag)) &&
                StringUtil.in(currentElement().nodeName(), "dd", "dt", "li", "option", "optgroup", "p", "rp", "rt"))
            pop();
    }

    void generateImpliedEndTags() {
        generateImpliedEndTags(null);
    }

    boolean isSpecial(Element el) {
        // todo: mathml's mi, mo, mn
        // todo: svg's foreigObject, desc, title
        String name = el.nodeName();
        return StringUtil.in(name, "address", "applet", "area", "article", "aside", "base", "basefont", "bgsound",
                "blockquote", "body", "br", "button", "caption", "center", "col", "colgroup", "command", "dd",
                "details", "dir", "div", "dl", "dt", "embed", "fieldset", "figcaption", "figure", "footer", "form",
                "frame", "frameset", "h1", "h2", "h3", "h4", "h5", "h6", "head", "header", "hgroup", "hr", "html",
                "iframe", "img", "input", "isindex", "li", "link", "listing", "marquee", "menu", "meta", "nav",
                "noembed", "noframes", "noscript", "object", "ol", "p", "param", "plaintext", "pre", "script",
                "section", "select", "style", "summary", "table", "tbody", "td", "textarea", "tfoot", "th", "thead",
                "title", "tr", "ul", "wbr", "xmp");
    }

    // active formatting elements
    void pushActiveFormattingElements(Element in) {
        int numSeen = 0;
        Iterator<Element> iter = formattingElements.descendingIterator();
        while (iter.hasNext()) {
            Element el =  iter.next();
            if (el == null) // marker
                break;

            if (isSameFormattingElement(in, el))
                numSeen++;

            if (numSeen == 3) {
                iter.remove();
                break;
            }
        }
        formattingElements.add(in);
    }

    private boolean isSameFormattingElement(Element a, Element b) {
        // same if: same namespace, tag, and attributes. Element.equals only checks tag, might in future check children
        return a.nodeName().equals(b.nodeName()) &&
                // a.namespace().equals(b.namespace()) &&
                a.attributes().equals(b.attributes());
        // todo: namespaces
    }

    void reconstructFormattingElements() {
        int size = formattingElements.size();
        if (size == 0 || formattingElements.getLast() == null || onStack(formattingElements.getLast()))
            return;

        Element entry = formattingElements.getLast();
        int pos = size - 1;
        boolean skip = false;
        while (true) {
            if (pos == 0) { // step 4. if none before, skip to 8
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
            Element newEl = insert(entry.nodeName()); // todo: avoid fostering here?
            // newEl.namespace(entry.namespace()); // todo: namespaces
            newEl.attributes().addAll(entry.attributes());

            // 10. replace entry with new entry
            formattingElements.add(pos, newEl);
            formattingElements.remove(pos + 1);

            // 11
            if (pos == size-1) // if not last entry in list, jump to 7
                break;
        }
    }

    void clearFormattingElementsToLastMarker() {
        while (!formattingElements.isEmpty()) {
            Element el = formattingElements.peekLast();
            formattingElements.removeLast();
            if (el == null)
                break;
        }
    }

    void removeFromActiveFormattingElements(Element el) {
        Iterator<Element> it = formattingElements.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (next == el) {
                it.remove();
                break;
            }
        }
    }

    boolean isInActiveFormattingElements(Element el) {
        return isElementInQueue(formattingElements, el);
    }

    Element getActiveFormattingElement(String nodeName) {
        Iterator<Element> it = formattingElements.descendingIterator();
        while (it.hasNext()) {
            Element next = it.next();
            if (next == null) // scope marker
                break;
            else if (next.nodeName().equals(nodeName))
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
        Element fosterParent = null;
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

    @Override
    public String toString() {
        return "TreeBuilder{" +
                "currentToken=" + currentToken +
                ", state=" + state +
                ", currentElement=" + currentElement() +
                '}';
    }
}
