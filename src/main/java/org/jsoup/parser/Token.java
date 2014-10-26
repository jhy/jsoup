package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;

/**
 * Parse tokens for the Tokeniser.
 */
abstract class Token {
    TokenType type;

    private Token() {
    }
    
    String tokenType() {
        return this.getClass().getSimpleName();
    }

    static final class Doctype extends Token {
        final StringBuilder name = new StringBuilder();
        final StringBuilder publicIdentifier = new StringBuilder();
        final StringBuilder systemIdentifier = new StringBuilder();
        boolean forceQuirks = false;

        Doctype() {
            type = TokenType.Doctype;
        }

        String getName() {
            return name.toString();
        }

        String getPublicIdentifier() {
            return publicIdentifier.toString();
        }

        public String getSystemIdentifier() {
            return systemIdentifier.toString();
        }

        public boolean isForceQuirks() {
            return forceQuirks;
        }
    }

    static abstract class Tag extends Token {
        protected String tagName;
        private String pendingAttributeName; // attribute names are generally caught in one hop, not accumulated
        private StringBuilder pendingAttributeValue; // but values are accumulated, from e.g. & in hrefs

        boolean selfClosing = false;
        Attributes attributes; // start tags get attributes on construction. End tags get attributes on first new attribute (but only for parser convenience, not used).

        final void newAttribute() {
            if (attributes == null)
                attributes = new Attributes();

            if (pendingAttributeName != null) {
                Attribute attribute;
                if (pendingAttributeValue == null)
                    attribute = new Attribute(pendingAttributeName, "");
                else
                    attribute = new Attribute(pendingAttributeName, pendingAttributeValue.toString());
                attributes.put(attribute);
            }
            pendingAttributeName = null;
            if (pendingAttributeValue != null)
                pendingAttributeValue.delete(0, pendingAttributeValue.length());
        }

        final void finaliseTag() {
            // finalises for emit
            if (pendingAttributeName != null) {
                // todo: check if attribute name exists; if so, drop and error
                newAttribute();
            }
        }

        final String name() {
            Validate.isFalse(tagName == null || tagName.length() == 0);
            return tagName;
        }

        final Tag name(String name) {
            tagName = name;
            return this;
        }

        final boolean isSelfClosing() {
            return selfClosing;
        }

        @SuppressWarnings({"TypeMayBeWeakened"})
        final Attributes getAttributes() {
            return attributes;
        }

        // these appenders are rarely hit in not null state-- caused by null chars.
        final void appendTagName(String append) {
            tagName = tagName == null ? append : tagName.concat(append);
        }

        final void appendTagName(char append) {
            appendTagName(String.valueOf(append));
        }

        final void appendAttributeName(String append) {
            pendingAttributeName = pendingAttributeName == null ? append : pendingAttributeName.concat(append);
        }

        final void appendAttributeName(char append) {
            appendAttributeName(String.valueOf(append));
        }

        final void appendAttributeValue(String append) {
            ensureAttributeValue();
            pendingAttributeValue.append(append);
        }

        final void appendAttributeValue(char append) {
            ensureAttributeValue();
            pendingAttributeValue.append(append);
        }

        final void appendAttributeValue(char[] append) {
            ensureAttributeValue();
            pendingAttributeValue.append(append);
        }

        private void ensureAttributeValue() {
            if (pendingAttributeValue == null)
                pendingAttributeValue = new StringBuilder();
        }
    }

    final static class StartTag extends Tag {
        StartTag() {
            super();
            attributes = new Attributes();
            type = TokenType.StartTag;
        }

        StartTag(String name) {
            this();
            this.tagName = name;
        }

        StartTag(String name, Attributes attributes) {
            this();
            this.tagName = name;
            this.attributes = attributes;
        }

        @Override
        public String toString() {
            if (attributes != null && attributes.size() > 0)
                return "<" + name() + " " + attributes.toString() + ">";
            else
                return "<" + name() + ">";
        }
    }

    final static class EndTag extends Tag{
        EndTag() {
            super();
            type = TokenType.EndTag;
        }

        EndTag(String name) {
            this();
            this.tagName = name;
        }

        @Override
        public String toString() {
            return "</" + name() + ">";
        }
    }

    final static class Comment extends Token {
        final StringBuilder data = new StringBuilder();
        boolean bogus = false;

        Comment() {
            type = TokenType.Comment;
        }

        String getData() {
            return data.toString();
        }

        @Override
        public String toString() {
            return "<!--" + getData() + "-->";
        }
    }

    final static class Character extends Token {
        private final String data;

        Character(String data) {
            type = TokenType.Character;
            this.data = data;
        }

        String getData() {
            return data;
        }

        @Override
        public String toString() {
            return getData();
        }
    }

    final static class EOF extends Token {
        EOF() {
            type = Token.TokenType.EOF;
        }
    }

    final boolean isDoctype() {
        return type == TokenType.Doctype;
    }

    final Doctype asDoctype() {
        return (Doctype) this;
    }

    final boolean isStartTag() {
        return type == TokenType.StartTag;
    }

    final StartTag asStartTag() {
        return (StartTag) this;
    }

    final boolean isEndTag() {
        return type == TokenType.EndTag;
    }

    final EndTag asEndTag() {
        return (EndTag) this;
    }

    final boolean isComment() {
        return type == TokenType.Comment;
    }

    final Comment asComment() {
        return (Comment) this;
    }

    final boolean isCharacter() {
        return type == TokenType.Character;
    }

    final Character asCharacter() {
        return (Character) this;
    }

    final boolean isEOF() {
        return type == TokenType.EOF;
    }

    static enum TokenType {
        Doctype,
        StartTag,
        EndTag,
        Comment,
        Character,
        EOF
    }
}
