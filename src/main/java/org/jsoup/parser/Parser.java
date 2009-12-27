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

    private LinkedList<Element> stack;
    private LinkedList<Character> queue;
    private Document doc;

    public Parser(String html) {
        Validate.notNull(html);

        this.stack = new LinkedList<Element>();
        this.queue = new LinkedList<Character>();
        char[] chars = html.toCharArray();
        for (char c : chars) {
            queue.add(c);
        }

        doc = new Document();
        stack.add(doc);
        stack.add(doc.getHead());
    }

    public static Document parse(String html) {
        Parser parser = new Parser(html);
        return parser.parse();
    }

    public Document parse() {
        while (!queue.isEmpty()) {
            if (matches("<!--")) {
                parseComment();
            } else if (matches("<?") || matches("<!")) {
                parseXmlDecl();
            } else if (matches("</")) {
                parseEndTag();
            } else if (matches("<")) {
                parseStartTag();
            } else {
                parseText();
            }
        }
        return doc;
    }

    private void parseComment() {
        consume("<!--");
        String data = chompTo("->");

        if (data.endsWith("-")) // i.e. was -->
            data = data.substring(0, data.length()-1);
        Comment comment = new Comment(data);
        last().addChild(comment);
    }

    private void parseXmlDecl() {
        consume("<"); consume(); // <? or <!, from initial match.
        String data = chompTo(">");

        XmlDeclaration decl = new XmlDeclaration(data);
        last().addChild(decl);
    }

    private void parseEndTag() {
        consume("</");
        String tagName = consumeWord();
        chompTo(">");

        if (!tagName.isEmpty()) {
            Tag tag = Tag.valueOf(tagName);
            popStackToClose(tag);
        }
    }

    private void parseStartTag() {
        consume("<");
        Attributes attributes = new Attributes();

        String tagName = consumeWord();
        while (!matches("/>") && !matches(">")) {
            Attribute attribute = parseAttribute();
            if (attribute != null)
                attributes.put(attribute);
        }

        Tag tag = Tag.valueOf(tagName);
        StartTag startTag = new StartTag(tag, attributes);
        Element child = new Element(startTag);

        boolean emptyTag;
        if (matches("/>")) { // empty tag, don't add to stack
            consume("/>");
            emptyTag = true;
        } else {
            consume(">");
            emptyTag = false;
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
            if (!emptyTag)
                stack.addLast(child);
        }
    }

    private Attribute parseAttribute() {
        consumeWhitespace();
        String key = consumeWord();
        String value = "";
        consumeWhitespace();
        if (matches("=")) {
            consume("=");
            consumeWhitespace();

            if (matches(SQ)) {
                consume(SQ);
                value = chompTo(SQ);
            } else if (matches(DQ)) {
                consume(DQ);
                value = chompTo(DQ);
            } else {
                StringBuilder valueAccum = new StringBuilder();
                while (!matches("/>") && !matches(">") && !Character.isWhitespace(queue.peekFirst())) {
                    valueAccum.append(consume());
                }
                value = valueAccum.toString();
            }
            consumeWhitespace();
        }
        if (!key.isEmpty())
            return new Attribute(key, value);
        else {
            consume(); // unknown char, keep popping so not get stuck
            return null;
        }
    }

    private void parseText() {
        // TODO: work out whitespace requirements (between blocks, between inlines)
        StringBuilder textAccum = new StringBuilder();
        while (!matches("<")) {
            textAccum.append(consume());
        }
        TextNode textNode = new TextNode(textAccum.toString());
        last().addChild(textNode);
    }

    /**
     * Pulls a string off the queue, up to but exclusive of the match sequence, or to the queue running out.
     * @param seq String to end on (and not include in return, but leave on queue)
     * @return The matched data consumed from queue.
     */
    private String consumeTo(String seq) {
        StringBuilder accum = new StringBuilder();
        while (!queue.isEmpty() && !matches(seq))
            accum.append(consume());

        return accum.toString();
    }

    /**
     * Pulls a string off the queue (like consumeTo), and then pulls off the matched string (but does not return it).
     * @param seq String to match up to, and not include in return, and to pull off queue
     * @return Data matched from queue.
     */
    private String chompTo(String seq) {
        String data = consumeTo(seq);
        consume(seq);
        return data;
    }

    /**
     * Consume one character off queue.
     * @return first character on queue.
     */
    private Character consume() {
        return queue.removeFirst();
    }

    private void consume(String seq) {
        int len = seq.length();
        if (len > queue.size())
            throw new IllegalStateException("Queue not long enough to consume sequence");
        char[] seqChars = seq.toCharArray();
        for (int i = 0; i < len; i++) {
            Character qChar = consume();
            if (!qChar.equals(seqChars[i]))
                throw new IllegalStateException("Queue did not match expected sequence");
        }
    }

    private void consumeWhitespace() {
        while (Character.isWhitespace(queue.peekFirst())) {
            consume();
        }
    }

    private String consumeWord() {
        StringBuilder wordAccum = new StringBuilder();
        while (Character.isLetterOrDigit(queue.peekFirst())) {
            wordAccum.append(queue.removeFirst());
        }
        return wordAccum.toString();
    }

    private boolean matches(String seq) {
        int len = seq.length();
        if (len > queue.size())
            return false;
        List<Character> chars = queue.subList(0, len);
        char[] seqChars = seq.toCharArray();
        for (int i = 0; i < len; i++) {
            if (!chars.get(i).equals(seqChars[i]))
                return false;
        }
        return true;
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
