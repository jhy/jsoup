package org.jsoup.parser;

/**
 * A Parse Error records an error in the input HTML that occurs in either the tokenisation or the tree building phase.
 */
public class ParseError {
    private int pos;
    private String cursorPos;
    private String errorMsg;

    ParseError(CharacterReader reader, String errorMsg) {
        pos = reader.pos();
        cursorPos = reader.cursorPos();
        this.errorMsg = errorMsg;
    }

    ParseError(CharacterReader reader, String errorFormat, Object... args) {
        pos = reader.pos();
        cursorPos = reader.cursorPos();
        this.errorMsg = String.format(errorFormat, args);
    }

    ParseError(int pos, String errorMsg) {
        this.pos = pos;
        cursorPos = String.valueOf(pos);
        this.errorMsg = errorMsg;
    }

    ParseError(int pos, String errorFormat, Object... args) {
        this.pos = pos;
        cursorPos = String.valueOf(pos);
        this.errorMsg = String.format(errorFormat, args);
    }

    /**
     * Retrieve the error message.
     * @return the error message.
     */
    public String getErrorMessage() {
        return errorMsg;
    }

    /**
     * Retrieves the offset of the error.
     * @return error offset within input
     */
    public int getPosition() {
        return pos;
    }

    /**
     Get the formatted line:column cursor position where the error occurred.
     @return line:number cursor position
     */
    public String getCursorPos() {
        return cursorPos;
    }

    @Override
    public String toString() {
        return "<" + cursorPos + ">: " + errorMsg;
    }
}
