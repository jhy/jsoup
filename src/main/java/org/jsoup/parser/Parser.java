package org.jsoup.parser;


import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;

import java.util.LinkedList;

/**
 Parses HTML into a {@link Document}. Generally best to use one of the  more convenient parse methods in {@link org.jsoup.Jsoup}.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Parser {
    private static final String SQ = "'";
    private static final String DQ = "\"";

    private static final Tag htmlTag = Tag.valueOf("html");
    private static final Tag headTag = Tag.valueOf("head");
    private static final Tag bodyTag = Tag.valueOf("body");
    private static final Tag titleTag = Tag.valueOf("title");
    private static final Tag textareaTag = Tag.valueOf("textarea");

    private final LinkedList<Element> stack;
    private final TokenQueue tq;
    private final Document doc;
    private String baseUri;
    private boolean relaxed = false;

    private Parser(String html, String baseUri, boolean isBodyFragment) {
        Validate.notNull(html);
        Validate.notNull(baseUri);

        stack = new LinkedList<Element>();
        tq = new TokenQueue(html);
        this.baseUri = baseUri;

        if (isBodyFragment) {
            doc = Document.createShell(baseUri);
            stack.add(doc.body());
        } else {
            doc = new Document(baseUri);
            stack.add(doc);
        }
    }

    /**
     Parse HTML into a Document.
     @param html HTML to parse
     @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     @return parsed Document
     */
    public static Document parse(String html, String baseUri) {
        Parser parser = new Parser(html, baseUri, false);
        return parser.parse();
    }

    /**
     Parse a fragment of HTML into the {@code body} of a Document.
     @param bodyHtml fragment of HTML
     @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     @return Document, with empty head, and HTML parsed into body
     */
    public static Document parseBodyFragment(String bodyHtml, String baseUri) {
        Parser parser = new Parser(bodyHtml, baseUri, true);
        return parser.parse();
    }

    /**
     Parse a fragment of HTML into the {@code body} of a Document, with relaxed parsing enabled. Relaxed, in this
     context, means that implicit tags are not automatically created when missing.
     @param bodyHtml fragment of HTML
     @param baseUri base URI of document (i.e. original fetch location), for resolving relative URLs.
     @return Document, with empty head, and HTML parsed into body
     */
    public static Document parseBodyFragmentRelaxed(String bodyHtml, String baseUri) {
        Parser parser = new Parser(bodyHtml, baseUri, true);
        parser.relaxed = true;
        return parser.parse();
    }

    private Document parse() {
        while (!tq.isEmpty()) {
            if (tq.matchesStartTag()) {
                parseStartTag();
            } else if (tq.matchesCS("</")) {
                parseEndTag();
            } else if (tq.matchesCS("<!--")) {
                parseComment();
            } else if (tq.matches("<![CDATA[")) {
                parseCdata();
            } else if (tq.matchesCS("<?") || tq.matchesCS("<!")) {
                parseXmlDecl();
            } else {
                parseTextNode();
            }
        }
        return doc.normalise();
    }

    private void parseComment() {
        tq.consume("<!--");
        String data = tq.chompTo("->");

        if (data.endsWith("-")) // i.e. was -->
            data = data.substring(0, data.length()-1);
        Comment comment = new Comment(data, baseUri);
        last().appendChild(comment);
    }

    private void parseXmlDecl() {
        tq.consume("<");
        Character firstChar = tq.consume(); // <? or <!, from initial match.
        boolean procInstr = firstChar.toString().equals("!");
        String data = tq.chompTo(">");

        XmlDeclaration decl = new XmlDeclaration(data, baseUri, procInstr);
        last().appendChild(decl);
    }

    private void parseEndTag() {
        tq.consume("</");
        String tagName = tq.consumeTagName();
        tq.chompTo(">");

        if (tagName.length() != 0) {
            Tag tag = Tag.valueOf(tagName);
            if (!last().tag().isIgnorableEndTag(tag)) // skips </tr> if in <table>
                popStackToClose(tag);
        }
    }

    private void parseStartTag() {
        tq.consume("<");
        String tagName = tq.consumeTagName();
        Validate.notEmpty(tagName, "Unexpectedly empty tagname. (This should not occur, please report!)");
        
        tq.consumeWhitespace();
        Attributes attributes = new Attributes();
        while (!tq.matchesAny("<", "/>", ">") && !tq.isEmpty()) {
            Attribute attribute = parseAttribute();
            if (attribute != null)
                attributes.put(attribute);
        }

        Tag tag = Tag.valueOf(tagName);
        Element child = new Element(tag, baseUri, attributes);

        boolean isEmptyElement = tag.isEmpty(); // empty element if empty tag (e.g. img) or self-closed el (<div/>
        if (tq.matchChomp("/>")) { // close empty element or tag
            isEmptyElement = true;
            if (!tag.isKnownTag()) // if unknown and a self closed, allow it to be self closed on output. this doesn't force all instances to be empty
                tag.setSelfClosing();
        } else {
            tq.matchChomp(">");
        }
        addChildToParent(child, isEmptyElement);

        // pc data only tags (textarea, script): chomp to end tag, add content as text node
        if (tag.isData()) {
            String data = tq.chompToIgnoreCase("</" + tagName);
            tq.chompTo(">");
            popStackToClose(tag);
            
            Node dataNode;
            if (tag.equals(titleTag) || tag.equals(textareaTag)) // want to show as text, but not contain inside tags (so not a data tag?)
                dataNode = TextNode.createFromEncoded(data, baseUri);
            else
                dataNode = new DataNode(data, baseUri); // data not encoded but raw (for " in script)
            child.appendChild(dataNode);   
        }

        // <base href>: update the base uri
        if (child.tagName().equals("base")) {
            String href = child.absUrl("href");
            if (href.length() != 0) { // ignore <base target> etc
                baseUri = href;
                doc.setBaseUri(href); // set on the doc so doc.createElement(Tag) will get updated base
            }
        }
    }

    private Attribute parseAttribute() {
        tq.consumeWhitespace();
        String key = tq.consumeAttributeKey();
        String value = "";
        tq.consumeWhitespace();
        if (tq.matchChomp("=")) {
            tq.consumeWhitespace();

            if (tq.matchChomp(SQ)) {
                value = tq.chompTo(SQ);
            } else if (tq.matchChomp(DQ)) {
                value = tq.chompTo(DQ);
            } else {
                StringBuilder valueAccum = new StringBuilder();
                // no ' or " to look for, so scan to end tag or space (or end of stream)
                while (!tq.matchesAny("<", "/>", ">") && !tq.matchesWhitespace() && !tq.isEmpty()) {
                    valueAccum.append(tq.consume());
                }
                value = valueAccum.toString();
            }
            tq.consumeWhitespace();
        }
        if (key.length() != 0)
            return Attribute.createFromEncoded(key, value);
        else {
            if (value.length() == 0) // no key, no val; unknown char, keep popping so not get stuck
                tq.advance();
                
            return null;
        }
    }

    private void parseTextNode() {
        TextNode textNode;
        // special case: handle string like "hello < there". first char will be "<", because of matchStartTag
        if (tq.peek().equals('<')) {
            tq.advance();
            textNode = new TextNode("<", baseUri);
        } else {
            String text = tq.consumeTo("<");
            textNode = TextNode.createFromEncoded(text, baseUri);
        }
        last().appendChild(textNode);
    }

    private void parseCdata() {
        tq.consume("<![CDATA[");
        String rawText = tq.chompTo("]]>");
        TextNode textNode = new TextNode(rawText, baseUri); // constructor does not escape
        last().appendChild(textNode);
    }

    private Element addChildToParent(Element child, boolean isEmptyElement) {
        Element parent = popStackToSuitableContainer(child.tag());
        Tag childTag = child.tag();
        boolean validAncestor = stackHasValidParent(childTag);

        if (!validAncestor && !relaxed) {
            // create implicit parent around this child
            Tag parentTag = childTag.getImplicitParent();
            Element implicit = new Element(parentTag, baseUri);
            // special case: make sure there's a head before putting in body
            if (child.tag().equals(bodyTag)) {
                Element head = new Element(headTag, baseUri);
                implicit.appendChild(head);
            }
            implicit.appendChild(child);

            // recurse to ensure somewhere to put parent
            Element root = addChildToParent(implicit, false);
            if (!isEmptyElement)
                stack.addLast(child);
            return root;
        }

        parent.appendChild(child);

        if (!isEmptyElement)
            stack.addLast(child);
        return parent;
    }

    private boolean stackHasValidParent(Tag childTag) {
        if (stack.size() == 1 && childTag.equals(htmlTag))
            return true; // root is valid for html node

        if (childTag.requiresSpecificParent())
            return stack.getLast().tag().isValidParent(childTag);

        // otherwise, look up the stack for valid ancestors
        for (int i = stack.size() -1; i >= 0; i--) {
            Element el = stack.get(i);
            Tag parent2 = el.tag();
            if (parent2.isValidAncestor(childTag)) {
                return true;
            }
        }
        return false;
    }

    private Element popStackToSuitableContainer(Tag tag) {
        while (!stack.isEmpty()) {
            if (last().tag().canContain(tag))
                return last();
            else
                stack.removeLast();
        }
        return null;
    }

    private Element popStackToClose(Tag tag) {
        // first check to see if stack contains this tag; if so pop to there, otherwise ignore
        int counter = 0;
        Element elToClose = null;
        for (int i = stack.size() -1; i > 0; i--) {
            counter++;
            Element el = stack.get(i);
            Tag elTag = el.tag();
            if (elTag.equals(bodyTag) || elTag.equals(htmlTag)) { // once in body, don't close past body
                break;
            } else if (elTag.equals(tag)) {
                elToClose = el;
                break;
            }
        }
        if (elToClose != null) {
            for (int i = 0; i < counter; i++) {
                stack.removeLast();
            }
        }
        return elToClose;
    }

    private Element last() {
        return stack.getLast();
    }
}
