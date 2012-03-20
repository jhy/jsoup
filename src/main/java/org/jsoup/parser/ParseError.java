package org.jsoup.parser;

/**
 * A Parse Error records an error in the input HTML that occurs in either the tokenisation or the tree building phase.
 */
class ParseError {
    private int pos;
    private String errorMsg;

    ParseError(int pos, String errorMsg) {
        this.pos = pos;
        this.errorMsg = errorMsg;
    }

    ParseError(int pos, String errorFormat, Object... args) {
        this.errorMsg = String.format(errorFormat, args);
        this.pos = pos;
    }

    public String getErrorMessage() {
        return errorMsg;
    }

    public int getPosition() {
        return pos;
    }

    @Override
    public String toString() {
        return pos + ": " + errorMsg;
    }
}
