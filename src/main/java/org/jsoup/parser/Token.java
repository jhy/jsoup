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

    static class Doctype extends Token {
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
        private String pendingAttributeName;
        private String pendingAttributeValue;

        boolean selfClosing = false;
        Attributes attributes = new Attributes(); // todo: allow nodes to not have attributes

        void newAttribute() {
            if (pendingAttributeName != null) {
                if (pendingAttributeValue == null)
                    pendingAttributeValue = "";
                Attribute attribute = new Attribute(pendingAttributeName, pendingAttributeValue);
                attributes.put(attribute);
            }
            pendingAttributeName = null;
            pendingAttributeValue = null;
        }

        void finaliseTag() {
            // finalises for emit
            if (pendingAttributeName != null) {
                // todo: check if attribute name exists; if so, drop and error
                newAttribute();
            }
        }

        String name() {
            Validate.isFalse(tagName.length() == 0);
            return tagName;
        }

        Tag name(String name) {
            tagName = name;
            return this;
        }

        boolean isSelfClosing() {
            return selfClosing;
        }

        @SuppressWarnings({"TypeMayBeWeakened"})
        Attributes getAttributes() {
            return attributes;
        }

        // these appenders are rarely hit in not null state-- caused by null chars.
        void appendTagName(String append) {
            tagName = tagName == null ? append : tagName.concat(append);
        }

        void appendTagName(char append) {
            appendTagName(String.valueOf(append));
        }

        void appendAttributeName(String append) {
            pendingAttributeName = pendingAttributeName == null ? append : pendingAttributeName.concat(append);
        }

        void appendAttributeName(char append) {
            appendAttributeName(String.valueOf(append));
        }

        void appendAttributeValue(String append) {
            pendingAttributeValue = pendingAttributeValue == null ? append : pendingAttributeValue.concat(append);
        }

        void appendAttributeValue(char append) {
            appendAttributeValue(String.valueOf(append));
        }
    }

    static class StartTag extends Tag {
        StartTag() {
            super();
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
            return "<" + name() + " " + attributes.toString() + ">";
        }
    }

    static class EndTag extends Tag{
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
            return "</" + name() + " " + attributes.toString() + ">";
        }
    }

    static class Comment extends Token {
        final StringBuilder data = new StringBuilder();

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

    static class Character extends Token {
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

    static class EOF extends Token {
        EOF() {
            type = Token.TokenType.EOF;
        }
    }

    boolean isDoctype() {
        return type == TokenType.Doctype;
    }

    Doctype asDoctype() {
        return (Doctype) this;
    }

    boolean isStartTag() {
        return type == TokenType.StartTag;
    }

    StartTag asStartTag() {
        return (StartTag) this;
    }

    boolean isEndTag() {
        return type == TokenType.EndTag;
    }

    EndTag asEndTag() {
        return (EndTag) this;
    }

    boolean isComment() {
        return type == TokenType.Comment;
    }

    Comment asComment() {
        return (Comment) this;
    }

    boolean isCharacter() {
        return type == TokenType.Character;
    }

    Character asCharacter() {
        return (Character) this;
    }

    boolean isEOF() {
        return type == TokenType.EOF;
    }

    enum TokenType {
        Doctype,
        StartTag,
        EndTag,
        Comment,
        Character,
        EOF
    }
}
