package org.jsoup.parser;

/**
 * A Parse Error records an error in the input HTML that occurs in either the tokenisation or the tree building phase.
 */
// todo: currently not ready for public consumption. revisit api, and exposure methods
class ParseError {
    private String errorMsg;
    private int pos;
    private char c;
    private TokeniserState tokeniserState;
    private TreeBuilderState treeBuilderState;
    private Token token;

    ParseError(String errorMsg, char c, TokeniserState tokeniserState, int pos) {
        this.errorMsg = errorMsg;
        this.c = c;
        this.tokeniserState = tokeniserState;
        this.pos = pos;
    }

    ParseError(String errorMsg, TokeniserState tokeniserState, int pos) {
        this.errorMsg = errorMsg;
        this.tokeniserState = tokeniserState;
        this.pos = pos;
    }

    ParseError(String errorMsg, int pos) {
        this.errorMsg = errorMsg;
        this.pos = pos;
    }

    ParseError(String errorMsg, TreeBuilderState treeBuilderState, Token token, int pos) {
        this.errorMsg = errorMsg;
        this.treeBuilderState = treeBuilderState;
        this.token = token;
        this.pos = pos;
    }

    String getErrorMsg() {
        return errorMsg;
    }

    int getPos() {
        return pos;
    }
}
