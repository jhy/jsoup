package org.jsoup.parser;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.*;

import java.util.*;

/**
 Parses HTML into a {@link Document}

 @author Jonathan Hedley, jonathan@hedley.net */
public class Parser {
    private static String SQ = "'";
    private static String DQ = "\"";

    private static Tag htmlTag = Tag.valueOf("html");
    private static Tag headTag = Tag.valueOf("head");
    private static Tag bodyTag = Tag.valueOf("body");
    private static Tag titleTag = Tag.valueOf("title");

    private LinkedList<Element> stack;
    private TokenQueue tq;
    private Document doc;
    private String baseUri;

    public Parser(String html, String baseUri) {
        Validate.notNull(html);
        Validate.notNull(baseUri);

        stack = new LinkedList<Element>();
        tq = new TokenQueue(html);
        this.baseUri = baseUri;

        doc = new Document(baseUri);
        stack.add(doc);
    }

    public static Document parse(String html, String baseUri) {
        Parser parser = new Parser(html, baseUri);
        return parser.parse();
    }

    public Document parse() {
        while (!tq.isEmpty()) {
            if (tq.matches("<!--")) {
                parseComment();
            } else if (tq.matches("<![CDATA[")) {
                parseCdata();
            } else if (tq.matches("<?") || tq.matches("<!")) {
                parseXmlDecl();
            } else if (tq.matches("</")) {
                parseEndTag();
            } else if (tq.matches("<")) {
                parseStartTag();
            } else {
                parseTextNode();
            }
        }
        return doc;
    }

    private void parseComment() {
        tq.consume("<!--");
        String data = tq.chompTo("->");

        if (data.endsWith("-")) // i.e. was -->
            data = data.substring(0, data.length()-1);
        Comment comment = new Comment(data, baseUri);
        last().addChild(comment);
    }

    private void parseXmlDecl() {
        tq.consume("<");
        Character firstChar = tq.consume(); // <? or <!, from initial match.
        boolean procInstr = firstChar.toString().equals("!");
        String data = tq.chompTo(">");

        XmlDeclaration decl = new XmlDeclaration(data, baseUri, procInstr);
        last().addChild(decl);
    }

    private void parseEndTag() {
        tq.consume("</");
        String tagName = tq.consumeWord();
        tq.chompTo(">");

        if (!tagName.isEmpty()) {
            Tag tag = Tag.valueOf(tagName);
            popStackToClose(tag);
        }
    }

    private void parseStartTag() {
        tq.consume("<");
        String tagName = tq.consumeWord();

        if (tagName.isEmpty()) { // doesn't look like a start tag after all; put < back on stack and handle as text
            tq.addFirst("&lt;");
            parseTextNode();
            return;
        }

        Attributes attributes = new Attributes();
        while (!tq.matchesAny("<", "/>", ">") && !tq.isEmpty()) {
            Attribute attribute = parseAttribute();
            if (attribute != null)
                attributes.put(attribute);
        }

        Tag tag = Tag.valueOf(tagName);
        StartTag startTag = new StartTag(tag, baseUri, attributes);
        Element child = new Element(startTag);

        boolean isEmptyElement = tag.isEmpty(); // empty element if empty tag (e.g. img) or self-closed el (<div/>
        if (tq.matchChomp("/>")) { // close empty element or tag
            isEmptyElement = true;
        } else {
            tq.matchChomp(">");
        }

        // pc data only tags (textarea, script): chomp to end tag, add content as text node
        if (tag.isData()) {
            String data = tq.chompTo("</" + tagName);
            tq.chompTo(">");
            DataNode dataNode = DataNode.createFromEncoded(data, baseUri);
            child.addChild(dataNode);

            if (tag.equals(titleTag))
                doc.setTitle(child.data());
        }

        // <base href>: update the base uri
        if (child.tagName().equals("base")) {
            baseUri = child.absUrl("href");
            doc.setBaseUri(baseUri); // set on the doc so doc.createElement(Tag) will get updated base
        }

        addChildToParent(child, isEmptyElement);
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
        if (!key.isEmpty())
            return Attribute.createFromEncoded(key, value);
        else {
            tq.consume(); // unknown char, keep popping so not get stuck
            return null;
        }
    }

    private void parseTextNode() {
        String text = tq.consumeTo("<");
        TextNode textNode = TextNode.createFromEncoded(text, baseUri);
        last().addChild(textNode);
    }

    private void parseCdata() {
        tq.consume("<![CDATA[");
        String rawText = tq.chompTo("]]>");
        TextNode textNode = new TextNode(rawText, baseUri); // constructor does not escape
        last().addChild(textNode);
    }

    private Element addChildToParent(Element child, boolean isEmptyElement) {
        Element parent = popStackToSuitableContainer(child.getTag());
        Tag childTag = child.getTag();
        boolean validAncestor = stackHasValidParent(childTag);

        if (!validAncestor) {
            // create implicit parent around this child
            Tag parentTag = childTag.getImplicitParent();
            StartTag parentStart = new StartTag(parentTag, baseUri);
            Element implicit = new Element(parentStart);
            // special case: make sure there's a head before putting in body
            if (child.getTag().equals(bodyTag)) {
                Element head = new Element(new StartTag(headTag, baseUri));
                implicit.addChild(head);
            }
            implicit.addChild(child);

            // recurse to ensure somewhere to put parent
            Element root = addChildToParent(implicit, false);
            if (!isEmptyElement)
                stack.addLast(child);
            return root;
        }

        parent.addChild(child);

        if (!isEmptyElement)
            stack.addLast(child);
        return parent;
    }

    private boolean stackHasValidParent(Tag childTag) {
        if (stack.size() == 1 && childTag.equals(htmlTag))
            return true; // root is valid for html node
        
        for (int i = stack.size() -1; i > 0; i--) { // not all the way to end
            Element el = stack.get(i);
            Tag parent2 = el.getTag();
            if (parent2.isValidParent(childTag)) {
                return true;
            }
        }
        return false;
    }

    private Element popStackToSuitableContainer(Tag tag) {
        while (!stack.isEmpty()) {
            if (last().getTag().canContain(tag))
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
            Tag elTag = el.getTag();
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
