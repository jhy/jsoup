package org.jsoup.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 A Token of HTML. Internal use only.

 @author Jonathan Hedley, jonathan@hedley.net */
class Token {
    // match tags: <, opt space, opt closer, tagname, opt attribs, opt empty closer, >
    private static final Pattern tagPattern = Pattern.compile("^<\\s*(/?)\\s*(\\w+)\\b\\s*(.*?)\\s*(/?)\\s*>$");
    // match comments
    private static final Pattern commentFullPattern = Pattern.compile("^<!--\\s*(.*?)\\s*-?->$");
    private static final Pattern commentStartPattern = Pattern.compile("^<!--\\s*(.*?)\\s*(-?->)?$");
    private static final Pattern commentEndPattern = Pattern.compile("^(<!--)?\\s*(.*?)\\s*-?->$");


    private String data;
    private Position pos;

    private boolean startTag;
    private boolean endTag;

    private boolean startComment;
    private boolean endComment;
    private String commentData;

    private boolean textNode;
    private String tagName;
    private String attributes;

    Token(String data, Position pos) {
        this.data = data;
        this.pos = pos;

        Matcher tagMatch = tagPattern.matcher(data);
        Matcher commentFullMatch = commentFullPattern.matcher(data);
        Matcher commentStartMatch = commentStartPattern.matcher(data);
        Matcher commentEndMatch = commentEndPattern.matcher(data);

        if (commentFullMatch.matches()) {
            startComment = true;
            endComment = true;
            commentData = commentFullMatch.group(1);
        }
        else if (commentStartMatch.matches()) {
            startComment = true;
            commentData = commentStartMatch.group(1);
        }
        else if (commentEndMatch.matches()) {
            endComment = true;
            commentData = commentEndMatch.group(2);
        } else if (!startComment && tagMatch.matches()) {
            startTag = (tagMatch.group(1).isEmpty()); // 1: closer
            endTag = (!tagMatch.group(1).isEmpty()) || (!tagMatch.group(4).isEmpty()); // 4: empty tag
            tagName = tagMatch.group(2);
            attributes = (tagMatch.group(3).isEmpty() ? null : tagMatch.group(3));
        } else {
            // TODO: xml decls, cdata
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

    public boolean isStartComment() {
        return startComment;
    }

    public boolean isEndComment() {
        return endComment;
    }

    public boolean isFullComment() {
        return startComment && endComment;
    }

    public String getCommentData() {
        return commentData;
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
