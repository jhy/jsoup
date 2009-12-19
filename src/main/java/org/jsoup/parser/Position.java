package org.jsoup.parser;

/**
 The start position (line & column number) that a token / node was found at.

 @author Jonathan Hedley, jonathan@hedley.net */
public final class Position implements Cloneable {
    private int offset = 0;
    private int lineNum = 1;
    private int colNum = 1;

    public int incOffset() {
        return offset++;
    }

    public int incLineNum() {
        return lineNum++;
    }

    public int incColNum() {
        return colNum++;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLineNum() {
        return lineNum;
    }

    public void setLineNum(int lineNum) {
        this.lineNum = lineNum;
    }

    public int getColNum() {
        return colNum;
    }

    public void setColNum(int colNum) {
        this.colNum = colNum;
    }

    @Override
    protected Position clone() {
        try {
            return (Position) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (colNum != position.colNum) return false;
        if (lineNum != position.lineNum) return false;
        if (offset != position.offset) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = offset;
        result = 31 * result + lineNum;
        result = 31 * result + colNum;
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s:%s", lineNum, colNum);
    }
}
