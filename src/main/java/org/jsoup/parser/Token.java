package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Attributes;

import javax.annotation.Nullable;

/**
 * Parse tokens for the Tokeniser.
 */
abstract class Token {
    TokenType type;
    static final int Unset = -1;
    private int startPos, endPos = Unset; // position in CharacterReader this token was read from

    private Token() {
    }
    
    String tokenType() {
        return this.getClass().getSimpleName();
    }

    /**
     * Reset the data represent by this token, for reuse. Prevents the need to create transfer objects for every
     * piece of data, which immediately get GCed.
     */
    Token reset() {
        startPos = Unset;
        endPos = Unset;
        return this;
    }

    int startPos() {
        return startPos;
    }

    void startPos(int pos) {
        startPos = pos;
    }

    int endPos() {
        return endPos;
    }

    void endPos(int pos) {
        endPos = pos;
    }

    static void reset(StringBuilder sb) {
        if (sb != null) {
            sb.delete(0, sb.length());
        }
    }

    static final class Doctype extends Token {
        final StringBuilder name = new StringBuilder();
        String pubSysKey = null;
        final StringBuilder publicIdentifier = new StringBuilder();
        final StringBuilder systemIdentifier = new StringBuilder();
        boolean forceQuirks = false;

        Doctype() {
            type = TokenType.Doctype;
        }

        @Override
        Token reset() {
            super.reset();
            reset(name);
            pubSysKey = null;
            reset(publicIdentifier);
            reset(systemIdentifier);
            forceQuirks = false;
            return this;
        }

        String getName() {
            return name.toString();
        }

        String getPubSysKey() {
            return pubSysKey;
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

        @Override
        public String toString() {
            return "<!doctype " + getName() + ">";
        }
    }

    static abstract class Tag extends Token {
        @Nullable protected String tagName;
        @Nullable protected String normalName; // lc version of tag name, for case insensitive tree build

        private final StringBuilder attrName = new StringBuilder(); // try to get attr names and vals in one shot, vs Builder
        @Nullable private String attrNameS;
        private boolean hasAttrName = false;

        private final StringBuilder attrValue = new StringBuilder();
        @Nullable private String attrValueS;
        private boolean hasAttrValue = false;
        private boolean hasEmptyAttrValue = false; // distinguish boolean attribute from empty string value

        boolean selfClosing = false;
        @Nullable Attributes attributes; // start tags get attributes on construction. End tags get attributes on first new attribute (but only for parser convenience, not used).

        @Override
        Tag reset() {
            super.reset();
            tagName = null;
            normalName = null;
            reset(attrName);
            attrNameS = null;
            hasAttrName = false;
            reset(attrValue);
            attrValueS = null;
            hasEmptyAttrValue = false;
            hasAttrValue = false;
            selfClosing = false;
            attributes = null;
            return this;
        }

        /* Limits runaway crafted HTML from spewing attributes and getting a little sluggish in ensureCapacity.
        Real-world HTML will P99 around 8 attributes, so plenty of headroom. Implemented here and not in the Attributes
        object so that API users can add more if ever required. */
        private static final int MaxAttributes = 512;

        final void newAttribute() {
            if (attributes == null)
                attributes = new Attributes();

            if (hasAttrName && attributes.size() < MaxAttributes) {
                // the tokeniser has skipped whitespace control chars, but trimming could collapse to empty for other control codes, so verify here
                String name = attrName.length() > 0 ? attrName.toString() : attrNameS;
                name = name.trim();
                if (name.length() > 0) {
                    String value;
                    if (hasAttrValue)
                        value = attrValue.length() > 0 ? attrValue.toString() : attrValueS;
                    else if (hasEmptyAttrValue)
                        value = "";
                    else
                        value = null;
                    // note that we add, not put. So that the first is kept, and rest are deduped, once in a context where case sensitivity is known (the appropriate tree builder).
                    attributes.add(name, value);
                }
            }
            reset(attrName);
            attrNameS = null;
            hasAttrName = false;

            reset(attrValue);
            attrValueS = null;
            hasAttrValue = false;
            hasEmptyAttrValue = false;
        }

        final boolean hasAttributes() {
            return attributes != null;
        }

        final boolean hasAttribute(String key) {
            return attributes != null && attributes.hasKey(key);
        }

        final void finaliseTag() {
            // finalises for emit
            if (hasAttrName) {
                newAttribute();
            }
        }

        /** Preserves case */
        final String name() { // preserves case, for input into Tag.valueOf (which may drop case)
            Validate.isFalse(tagName == null || tagName.length() == 0);
            return tagName;
        }

        /** Lower case */
        final String normalName() { // lower case, used in tree building for working out where in tree it should go
            return normalName;
        }

        final String toStringName() {
            return tagName != null ? tagName : "[unset]";
        }

        final Tag name(String name) {
            tagName = name;
            normalName = ParseSettings.normalName(tagName);
            return this;
        }

        final boolean isSelfClosing() {
            return selfClosing;
        }

        // these appenders are rarely hit in not null state-- caused by null chars.
        final void appendTagName(String append) {
            // might have null chars - need to replace with null replacement character
            append = append.replace(TokeniserState.nullChar, Tokeniser.replacementChar);
            tagName = tagName == null ? append : tagName.concat(append);
            normalName = ParseSettings.normalName(tagName);
        }

        final void appendTagName(char append) {
            appendTagName(String.valueOf(append));
        }

        final void appendAttributeName(String append) {
            // might have null chars because we eat in one pass - need to replace with null replacement character
            append = append.replace(TokeniserState.nullChar, Tokeniser.replacementChar);

            ensureAttrName();
            if (attrName.length() == 0) {
                attrNameS = append;
            } else {
                attrName.append(append);
            }
        }

        final void appendAttributeName(char append) {
            ensureAttrName();
            attrName.append(append);
        }

        final void appendAttributeValue(String append) {
            ensureAttrValue();
            if (attrValue.length() == 0) {
                attrValueS = append;
            } else {
                attrValue.append(append);
            }
        }

        final void appendAttributeValue(char append) {
            ensureAttrValue();
            attrValue.append(append);
        }

        final void appendAttributeValue(char[] append) {
            ensureAttrValue();
            attrValue.append(append);
        }

        final void appendAttributeValue(int[] appendCodepoints) {
            ensureAttrValue();
            for (int codepoint : appendCodepoints) {
                attrValue.appendCodePoint(codepoint);
            }
        }
        
        final void setEmptyAttributeValue() {
            hasEmptyAttrValue = true;
        }

        private void ensureAttrName() {
            hasAttrName = true;
            // if on second hit, we'll need to move to the builder
            if (attrNameS != null) {
                attrName.append(attrNameS);
                attrNameS = null;
            }
        }

        private void ensureAttrValue() {
            hasAttrValue = true;
            // if on second hit, we'll need to move to the builder
            if (attrValueS != null) {
                attrValue.append(attrValueS);
                attrValueS = null;
            }
        }

        @Override
        abstract public String toString();
    }

    final static class StartTag extends Tag {
        StartTag() {
            super();
            type = TokenType.StartTag;
        }

        @Override
        Tag reset() {
            super.reset();
            attributes = null;
            return this;
        }

        StartTag nameAttr(String name, Attributes attributes) {
            this.tagName = name;
            this.attributes = attributes;
            normalName = ParseSettings.normalName(tagName);
            return this;
        }

        @Override
        public String toString() {
            if (hasAttributes() && attributes.size() > 0)
                return "<" + toStringName() + " " + attributes.toString() + ">";
            else
                return "<" + toStringName() + ">";
        }
    }

    final static class EndTag extends Tag{
        EndTag() {
            super();
            type = TokenType.EndTag;
        }

        @Override
        public String toString() {
            return "</" + toStringName() + ">";
        }
    }

    final static class Comment extends Token {
        private final StringBuilder data = new StringBuilder();
        private String dataS; // try to get in one shot
        boolean bogus = false;

        @Override
        Token reset() {
            super.reset();
            reset(data);
            dataS = null;
            bogus = false;
            return this;
        }

        Comment() {
            type = TokenType.Comment;
        }

        String getData() {
            return dataS != null ? dataS : data.toString();
        }

        final Comment append(String append) {
            ensureData();
            if (data.length() == 0) {
                dataS = append;
            } else {
                data.append(append);
            }
            return this;
        }

        final Comment append(char append) {
            ensureData();
            data.append(append);
            return this;
        }

        private void ensureData() {
            // if on second hit, we'll need to move to the builder
            if (dataS != null) {
                data.append(dataS);
                dataS = null;
            }
        }

        @Override
        public String toString() {
            return "<!--" + getData() + "-->";
        }
    }

    static class Character extends Token {
        private String data;

        Character() {
            super();
            type = TokenType.Character;
        }

        @Override
        Token reset() {
            super.reset();
            data = null;
            return this;
        }

        Character data(String data) {
            this.data = data;
            return this;
        }

        String getData() {
            return data;
        }

        @Override
        public String toString() {
            return getData();
        }
    }

    final static class CData extends Character {
        CData(String data) {
            super();
            this.data(data);
        }

        @Override
        public String toString() {
            return "<![CDATA[" + getData() + "]]>";
        }

    }

    final static class EOF extends Token {
        EOF() {
            type = Token.TokenType.EOF;
        }

        @Override
        Token reset() {
            super.reset();
            return this;
        }

        @Override
        public String toString() {
            return "";
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

    final boolean isCData() {
        return this instanceof CData;
    }

    final Character asCharacter() {
        return (Character) this;
    }

    final boolean isEOF() {
        return type == TokenType.EOF;
    }

    public enum TokenType {
        Doctype,
        StartTag,
        EndTag,
        Comment,
        Character, // note no CData - treated in builder as an extension of Character
        EOF
    }
}
