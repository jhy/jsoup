package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.internal.Normalizer;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Range;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static org.jsoup.internal.SharedConstants.*;


/**
 * Parse tokens for the Tokeniser.
 */
abstract class Token {
    final TokenType type; // used in switches in TreeBuilder vs .getClass()
    static final int Unset = -1;
    private int startPos, endPos = Unset; // position in CharacterReader this token was read from

    private Token(TokenType type) {
        this.type = type;
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
        @Nullable String pubSysKey = null;
        final StringBuilder publicIdentifier = new StringBuilder();
        final StringBuilder systemIdentifier = new StringBuilder();
        boolean forceQuirks = false;

        Doctype() {
            super(TokenType.Doctype);
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

        @Nullable String getPubSysKey() {
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
        @Nullable protected String normalName; // lc version of tag name, for case-insensitive tree build
        boolean selfClosing = false;
        @Nullable Attributes attributes; // start tags get attributes on construction. End tags get attributes on first new attribute (but only for parser convenience, not used).

        @Nullable private String attrName; // try to get attr names and vals in one shot, vs Builder
        private final StringBuilder attrNameSb = new StringBuilder();
        private boolean hasAttrName = false;

        @Nullable private String attrValue;
        private final StringBuilder attrValueSb = new StringBuilder();
        private boolean hasAttrValue = false;
        private boolean hasEmptyAttrValue = false; // distinguish boolean attribute from empty string value

        // attribute source range tracking
        final TreeBuilder treeBuilder;
        final boolean trackSource;
        int attrNameStart, attrNameEnd, attrValStart, attrValEnd;

        Tag(TokenType type, TreeBuilder treeBuilder) {
            super(type);
            this.treeBuilder = treeBuilder;
            this.trackSource = treeBuilder.trackSourceRange;
        }

        @Override
        Tag reset() {
            super.reset();
            tagName = null;
            normalName = null;
            selfClosing = false;
            attributes = null;
            resetPendingAttr();
            return this;
        }

        private void resetPendingAttr() {
            reset(attrNameSb);
            attrName = null;
            hasAttrName = false;

            reset(attrValueSb);
            attrValue = null;
            hasEmptyAttrValue = false;
            hasAttrValue = false;

            if (trackSource)
                attrNameStart = attrNameEnd = attrValStart = attrValEnd = Unset;
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
                String name = attrNameSb.length() > 0 ? attrNameSb.toString() : attrName;
                name = name.trim();
                if (name.length() > 0) {
                    String value;
                    if (hasAttrValue)
                        value = attrValueSb.length() > 0 ? attrValueSb.toString() : attrValue;
                    else if (hasEmptyAttrValue)
                        value = "";
                    else
                        value = null;
                    // note that we add, not put. So that the first is kept, and rest are deduped, once in a context where case sensitivity is known, and we can warn for duplicates.
                    attributes.add(name, value);

                    trackAttributeRange(name);
                }
            }
            resetPendingAttr();
        }

        private void trackAttributeRange(String name) {
            if (trackSource && isStartTag()) {
                final StartTag start = asStartTag();
                final CharacterReader r = start.treeBuilder.reader;
                final boolean preserve = start.treeBuilder.settings.preserveAttributeCase();

                assert attributes != null;
                if (!preserve) name = Normalizer.lowerCase(name);
                if (attributes.sourceRange(name).nameRange().isTracked()) return; // dedupe ranges as we go; actual attributes get deduped later for error count

                // if there's no value (e.g. boolean), make it an implicit range at current
                if (!hasAttrValue) attrValStart = attrValEnd = attrNameEnd;

                Range.AttributeRange range = new Range.AttributeRange(
                    new Range(
                        new Range.Position(attrNameStart, r.lineNumber(attrNameStart), r.columnNumber(attrNameStart)),
                        new Range.Position(attrNameEnd, r.lineNumber(attrNameEnd), r.columnNumber(attrNameEnd))),
                    new Range(
                        new Range.Position(attrValStart, r.lineNumber(attrValStart), r.columnNumber(attrValStart)),
                        new Range.Position(attrValEnd, r.lineNumber(attrValEnd), r.columnNumber(attrValEnd)))
                );
                attributes.sourceRange(name, range);
            }
        }

        final boolean hasAttributes() {
            return attributes != null;
        }

        /** Case-sensitive check */
        final boolean hasAttribute(String key) {
            return attributes != null && attributes.hasKey(key);
        }

        final boolean hasAttributeIgnoreCase(String key) {
            return attributes != null && attributes.hasKeyIgnoreCase(key);
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

        final void appendAttributeName(String append, int startPos, int endPos) {
            // might have null chars because we eat in one pass - need to replace with null replacement character
            append = append.replace(TokeniserState.nullChar, Tokeniser.replacementChar);

            ensureAttrName(startPos, endPos);
            if (attrNameSb.length() == 0) {
                attrName = append;
            } else {
                attrNameSb.append(append);
            }
        }

        final void appendAttributeName(char append, int startPos, int endPos) {
            ensureAttrName(startPos, endPos);
            attrNameSb.append(append);
        }

        final void appendAttributeValue(String append, int startPos, int endPos) {
            ensureAttrValue(startPos, endPos);
            if (attrValueSb.length() == 0) {
                attrValue = append;
            } else {
                attrValueSb.append(append);
            }
        }

        final void appendAttributeValue(char append, int startPos, int endPos) {
            ensureAttrValue(startPos, endPos);
            attrValueSb.append(append);
        }

        final void appendAttributeValue(int[] appendCodepoints, int startPos, int endPos) {
            ensureAttrValue(startPos, endPos);
            for (int codepoint : appendCodepoints) {
                attrValueSb.appendCodePoint(codepoint);
            }
        }
        
        final void setEmptyAttributeValue() {
            hasEmptyAttrValue = true;
        }

        private void ensureAttrName(int startPos, int endPos) {
            hasAttrName = true;
            // if on second hit, we'll need to move to the builder
            if (attrName != null) {
                attrNameSb.append(attrName);
                attrName = null;
            }
            if (trackSource) {
                attrNameStart = attrNameStart > Unset ? attrNameStart : startPos; // latches to first
                attrNameEnd = endPos;
            }
        }

        private void ensureAttrValue(int startPos, int endPos) {
            hasAttrValue = true;
            // if on second hit, we'll need to move to the builder
            if (attrValue != null) {
                attrValueSb.append(attrValue);
                attrValue = null;
            }
            if (trackSource) {
                attrValStart = attrValStart > Unset ? attrValStart : startPos; // latches to first
                attrValEnd = endPos;
            }
        }

        @Override
        abstract public String toString();
    }

    final static class StartTag extends Tag {

        // TreeBuilder is provided so if tracking, can get line / column positions for Range; and can dedupe as we go
        StartTag(TreeBuilder treeBuilder) {
            super(TokenType.StartTag, treeBuilder);
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
            String closer = isSelfClosing() ? "/>" : ">";
            if (hasAttributes() && attributes.size() > 0)
                return "<" + toStringName() + " " + attributes.toString() + closer;
            else
                return "<" + toStringName() + closer;
        }
    }

    final static class EndTag extends Tag{
        EndTag(TreeBuilder treeBuilder) {
            super(TokenType.EndTag, treeBuilder);
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
            super(TokenType.Comment);
        }

        String getData() {
            return dataS != null ? dataS : data.toString();
        }

        Comment append(String append) {
            ensureData();
            if (data.length() == 0) {
                dataS = append;
            } else {
                data.append(append);
            }
            return this;
        }

        Comment append(char append) {
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

    static class Character extends Token implements Cloneable {
        private String data;

        Character() {
            super(TokenType.Character);
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

        @Override protected Token.Character clone() {
            try {
                return (Token.Character) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
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
            super(Token.TokenType.EOF);
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
