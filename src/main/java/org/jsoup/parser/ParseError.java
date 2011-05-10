package org.jsoup.parser;

/**
 */
public class ParseError {
    private String errorMsg;
    private int pos;
    private char c;
    private TokeniserState tokeniserState;
    private TreeBuilderState treeBuilderState;
    private Token token;

    public ParseError(String errorMsg, char c, TokeniserState tokeniserState, int pos) {
        this.errorMsg = errorMsg;
        this.c = c;
        this.tokeniserState = tokeniserState;
        this.pos = pos;
    }

    public ParseError(String errorMsg, TokeniserState tokeniserState, int pos) {
        this.errorMsg = errorMsg;
        this.tokeniserState = tokeniserState;
        this.pos = pos;
    }

    public ParseError(String errorMsg, int pos) {
        this.errorMsg = errorMsg;
        this.pos = pos;
    }

    public ParseError(String errorMsg, TreeBuilderState treeBuilderState, Token token, int pos) {
        this.errorMsg = errorMsg;
        this.treeBuilderState = treeBuilderState;
        this.token = token;
        this.pos = pos;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public int getPos() {
        return pos;
    }
}
