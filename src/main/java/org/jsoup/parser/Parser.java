package org.jsoup.parser;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public Parser(String html) {
        Validate.notNull(html);

        stack = new LinkedList<Element>();
        tq = new TokenQueue(html);

        doc = new Document();
        stack.add(doc);
        stack.add(doc.getHead());
    }

    public static Document parse(String html) {
        Parser parser = new Parser(html);
        return parser.parse();
    }

    public Document parse() {
        while (!tq.isEmpty()) {
            if (tq.matches("<!--")) {
                parseComment();
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
        // TODO: this puts comments into nodes that should not hold the (e.g. img).
        tq.consume("<!--");
        String data = tq.chompTo("->");

        if (data.endsWith("-")) // i.e. was -->
            data = data.substring(0, data.length()-1);
        Comment comment = new Comment(data);
        last().addChild(comment);
    }

    private void parseXmlDecl() {
        tq.consume("<"); tq.consume(); // <? or <!, from initial match.
        String data = tq.chompTo(">");

        XmlDeclaration decl = new XmlDeclaration(data);
        last().addChild(decl);
    }

    private void parseEndTag() {
        tq.consume("</");
        String tagName = tq.consumeWord();
        tq.chompTo(">");

        if (!tagName.isEmpty()) {
            Tag tag = Tag.valueOf(tagName);
            Element closed = popStackToClose(tag);
        }
    }

    private void parseStartTag() {
        tq.consume("<");
        Attributes attributes = new Attributes();

        String tagName = tq.consumeWord();
        while (!tq.matches("<") && !tq.matches("/>") && !tq.matches(">") && !tq.isEmpty()) {
            Attribute attribute = parseAttribute();
            if (attribute != null)
                attributes.put(attribute);
        }

        Tag tag = Tag.valueOf(tagName);
        StartTag startTag = new StartTag(tag, attributes);
        Element child = new Element(startTag);

        boolean emptyTag;
        if (tq.matchChomp("/>")) { // empty tag, don't add to stack
            emptyTag = true;
        } else {
            tq.matchChomp(">"); // safe because checked above (or ran out of data)
            emptyTag = false;
        }

        // pc data only tags (textarea, script): chomp to end tag, add content as text node
        if (tag.isData()) {
            String data = tq.chompTo("</" + tagName);
            tq.chompTo(">");
            TextNode textNode = TextNode.createFromEncoded(data);
            child.addChild(textNode);

            if (tag.equals(titleTag))
                doc.setTitle(child.text());
        }

        // switch between html, head, body, to preserve doc structure
        if (tag.equals(htmlTag)) {
            doc.getAttributes().mergeAttributes(attributes);
        } else if (tag.equals(headTag)) {
            doc.getHead().getAttributes().mergeAttributes(attributes);
            // head is on stack from start, no action required
        } else if (last().getTag().equals(headTag) && !headTag.canContain(tag)) {
            // switch to body
            stack.removeLast();
            stack.addLast(doc.getBody());
            last().addChild(child);
            if (!emptyTag)
                stack.addLast(child);
        } else if (tag.equals(bodyTag) && last().getTag().equals(htmlTag)) {
            doc.getBody().getAttributes().mergeAttributes(attributes);
            stack.removeLast();
            stack.addLast(doc.getBody());
        } else {
            Element parent = popStackToSuitableContainer(tag);
            parent.addChild(child);
            if (!emptyTag && !tag.isData()) // TODO: only check for data here because last() == head is wrong; should be ancestor is head
                stack.addLast(child);
        }
    }

    private Attribute parseAttribute() {
        tq.consumeWhitespace();
        String key = tq.consumeWord();
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
                while (!tq.matches("<") && !tq.matches("/>") && !tq.matches(">") && !tq.matchesWhitespace() && !tq.isEmpty()) {
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
        // TODO: work out whitespace requirements (between blocks, between inlines)
        StringBuilder textAccum = new StringBuilder();
        while (!tq.matches("<") && !tq.isEmpty()) { // scan to next tag
            textAccum.append(tq.consume());
        }
        TextNode textNode = TextNode.createFromEncoded(textAccum.toString());
        last().addChild(textNode);
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
            if (el.getTag().equals(tag)) {
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
