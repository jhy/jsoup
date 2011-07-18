package org.jsoup.parser;

import org.apache.commons.lang.Validate;
import org.jsoup.nodes.*;

import java.util.LinkedList;
import java.util.Queue;

/**
 Parses a {@link TokenStream} into a {@link Document}

 @author Jonathan Hedley, jonathan@hedley.net */
public class Parser {
    private TokenStream tokenStream;
    private LinkedList<Element> stack;
    private AttributeParser attributeParser;

    public Parser(TokenStream tokenStream) {
        Validate.notNull(tokenStream);

        this.tokenStream = tokenStream;
        this.stack = new LinkedList<Element>();
        this.attributeParser = new AttributeParser();
    }

    public Document parse() {
        // TODO: figure out implicit head & body elements
        Document doc = new Document();
        stack.add(doc);

        while (tokenStream.hasNext()) {
            Token token = tokenStream.next();

            if (token.isStartTag()) {
                Attributes attributes = attributeParser.parse(token.getAttributeString());
                Tag tag = Tag.valueOf(token.getTagName());
                StartTag startTag = new StartTag(tag, attributes);

                Element parent = popStackToSuitableContainer(tag);
                Validate.notNull(parent, "Should always have a viable container");
                Element node = new Element(parent, startTag);
                parent.addChild(node);
                stack.add(node);
            }

            if (token.isEndTag()) { // empty tags are both start and end tags
                stack.removeLast();
            }

            // TODO[must] handle comments

            else if (token.isTextNode()) {
                String text = token.getData();
                TextNode textNode = new TextNode(stack.peek(), text);
                stack.getLast().addChild(textNode);
            }
        }
        return doc;
    }

    private Element popStackToSuitableContainer(Tag tag) {
        while (stack.size() > 0) {
            if (stack.getLast().getTag().canContain(tag))
                return stack.getLast();
            else
                stack.removeLast();
        }
        return null;
    }
}
