package org.jsoup.parser;

import org.jsoup.internal.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 A value holder for Tokens, as the stream is Tokenized. Can hold a String or a StringBuilder.
 <p>The goal is to minimize String copies -- the tokenizer tries to read the entirety of the token's data in one it, and
 set that as the simple String value. But if it turns out we need to append, fall back to a StringBuilder, which we get
 out of the pool (to reduce the GC load).</p>
 */
class TokenData {
    private @Nullable String value;
    private @Nullable StringBuilder builder;

    TokenData() {}

    void set(String str) {
        reset();
        value = str;
    }

    void append(String str) {
        if (builder != null) {
            builder.append(str);
        } else if (value != null) {
            flipToBuilder();
            builder.append(str);
        } else {
            value = str;
        }
    }

    void append(char c) {
        if (builder != null) {
            builder.append(c);
        } else if (value != null) {
            flipToBuilder();
            builder.append(c);
        } else {
            value = String.valueOf(c);
        }
    }

    void appendCodePoint(int codepoint) {
        if (builder != null) {
            builder.appendCodePoint(codepoint);
        } else if (value != null) {
            flipToBuilder();
            builder.appendCodePoint(codepoint);
        } else {
            value = String.valueOf(Character.toChars(codepoint));
        }
    }

    private void flipToBuilder() {
        builder = StringUtil.borrowBuilder();
        builder.append(value);
        value = null;
    }

    boolean hasData() {
        return builder != null || value != null;
    }

    void reset() {
        if (builder != null) {
            StringUtil.releaseBuilderVoid(builder);
            builder = null;
        }
        value = null;
    }

    String value() {
        if (builder != null) {
            // in rare case we get hit twice, don't toString the builder twice
            value = builder.toString();
            StringUtil.releaseBuilder(builder);
            builder = null;
            return value;
        }
        return value != null ? value : "";
    }

    @Override
    public String toString() {
        // for debug views; no side effects
        if (builder != null) return builder.toString();
        return value != null ? value : "";
    }

}
