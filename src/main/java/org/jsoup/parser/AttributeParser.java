package org.jsoup.parser;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;

import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 Parse attribute strings into attributes.

 @author Jonathan Hedley, jonathan@hedley.net */
public class AttributeParser {
    private static final char SQ = '\'';
    private static final char DQ = '"';
    private static final char EQ = '=';
    private static final Pattern keyOk = Pattern.compile("[^\\s'\"=]");
    private static final Pattern space = Pattern.compile("[\\s]");

    public Attributes parse(String attributeString) {
        Attributes attributes = new Attributes();
        if (attributeString == null || attributeString.trim().isEmpty())
            return attributes;

        char[] charArray = attributeString.trim().toCharArray();
        Queue<Character> chars = new LinkedList<Character>();
        for (char c : charArray) {
            chars.add(c);
        }

        while (chars.size() > 0) {
            Attribute attribute = nextAttribute(chars);
            if (attribute != null)
                attributes.put(attribute);
        }
        return attributes;
    }

    private Attribute nextAttribute(Queue<Character> chars) {
        StringBuilder key = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean seenEquals = false;
        Character quoteChar = null;

        // From: foo="bar" foo = 'bar "qux" zap' foo = bar foo foo foo="data = something"
        // To: <foo=bar>, <foo=bar "qux" zap>, <foo=bar>, <foo=>, <foo=>, <foo=data = something>
        while (chars.size() > 0) {
            Character c = chars.remove();
            String s = c.toString();

            // this is a bit gnarly. ideas on rewrite with expect + consume. Doesn't seem like a regular expression though.

            if (!seenEquals && keyOk.matcher(s).matches()) {
                // accum the key
                key.append(s);
            } else if (!seenEquals && key.length() > 0 && space.matcher(s).matches()) {
                // if we have a key, then a run of space, then a key-like char: that's a new key and we need to break before
                Character nextC = chars.peek();
                if (nextC != null && keyOk.matcher(nextC.toString()).matches())
                    break;
            } else if (!seenEquals && c == EQ) { //
                seenEquals = true;
            } else if (seenEquals) {
                // working on the value
                if (quoteChar == null && (c == SQ || c == DQ)) {
                    // match until closing quote
                    quoteChar = c;
                } else if (c == quoteChar) {
                    // closing quote
                    break;
                } else if (quoteChar == null && value.length() > 0 && space.matcher(s).matches()) {
                    // we have found a space in a naked value (foo=bar): end of the line
                    break;
                } else {
                    // accum the value
                    value.append(s);
                }
            }
        }

        // TODO[must] de-entify / unescape attribute values (and keys too I guess)

        // return an attribute if we have a key (val can be empty)
        if (key.length() > 0)
            return new Attribute(key.toString().trim(), value.toString().trim());
        else
            return null;
    }
}
