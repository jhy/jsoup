package org.jsoup.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 A Token of HTML. Internal use only.

 @author Jonathan Hedley, jonathan@hedley.net */
class Token {
    private static final Pattern tagPattern = Pattern.compile("^<\\s*(/?)\\s*(\\w+)\\b\\s*(.*?)\\s*(/?)\\s*>$");
    // pattern: <, opt space, opt closer, tagname, opt attribs, opt empty closer, >

    private String data;
    private Position pos;

    private boolean startTag;
    private boolean endTag;
    private boolean textNode;
    private String tagName;
    private String attributes;

    Token(String data, Position pos) {
        this.data = data;
        this.pos = pos;

        Matcher tagMatch = tagPattern.matcher(data);
        if (tagMatch.matches()) {
            startTag = (tagMatch.group(1).isEmpty()); // 1: closer
            endTag = (!tagMatch.group(1).isEmpty()) || (!tagMatch.group(4).isEmpty()); // 4: empty tag
            tagName = tagMatch.group(2);
            attributes = (tagMatch.group(3).isEmpty() ? null : tagMatch.group(3));
        } else {
            // TODO: comments
            textNode = true;
        }
    }


    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Position getPos() {
        return pos;
    }

    public void setPos(Position pos) {
        this.pos = pos;
    }

    public boolean isStartTag() {
        return startTag;
    }

    public boolean isEndTag() {
        return endTag;
    }

    public boolean isTextNode() {
        return textNode;
    }

    public String getTagName() {
        return tagName;
    }

    public String getAttributeString() {
        return attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Token token = (Token) o;

        if (data != null ? !data.equals(token.data) : token.data != null) return false;
        if (pos != null ? !pos.equals(token.pos) : token.pos != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = data != null ? data.hashCode() : 0;
        result = 31 * result + (pos != null ? pos.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", data, pos);
    }
}
