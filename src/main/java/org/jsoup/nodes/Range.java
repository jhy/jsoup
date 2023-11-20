package org.jsoup.nodes;

import org.jsoup.helper.Validate;

import static org.jsoup.internal.SharedConstants.PrivatePrefix;

/**
 A Range object tracks the character positions in the original input source where a Node starts or ends. If you want to
 track these positions, tracking must be enabled in the Parser with
 {@link org.jsoup.parser.Parser#setTrackPosition(boolean)}.
 @see Node#sourceRange()
 @since 1.15.2
 */
public class Range {
    private final Position start, end;

    private static final String RangeKey = PrivatePrefix + "sourceRange";
    private static final String EndRangeKey = PrivatePrefix + "endSourceRange";
    private static final Position UntrackedPos = new Position(-1, -1, -1);
    private static final Range Untracked = new Range(UntrackedPos, UntrackedPos);

    /**
     Creates a new Range with start and end Positions. Called by TreeBuilder when position tracking is on.
     * @param start the start position
     * @param end the end position
     */
    public Range(Position start, Position end) {
        this.start = start;
        this.end = end;
    }

    /**
     Get the start position of this node.
     * @return the start position
     */
    public Position start() {
        return start;
    }

    /**
     Get the starting cursor position of this range.
     @return the 0-based start cursor position.
     @since 1.17.1
     */
    public int startPos() {
        return start.pos;
    }

    /**
     Get the end position of this node.
     * @return the end position
     */
    public Position end() {
        return end;
    }

    /**
     Get the ending cursor position of this range.
     @return the 0-based ending cursor position.
     @since 1.17.1
     */
    public int endPos() {
        return end.pos;
    }

    /**
     Test if this source range was tracked during parsing.
     * @return true if this was tracked during parsing, false otherwise (and all fields will be {@code -1}).
     */
    public boolean isTracked() {
        return this != Untracked;
    }

    /**
     Checks if the range represents a node that was implicitly created / closed.
     <p>For example, with HTML of {@code <p>One<p>Two}, both {@code p} elements will have an explicit
     {@link Element#sourceRange()} but an implicit {@link Element#endSourceRange()} marking the end position, as neither
     have closing {@code </p>} tags. The TextNodes will have explicit sourceRanges.
     <p>A range is considered implicit if its start and end positions are the same.
     @return true if the range is tracked and its start and end positions are the same, false otherwise.
     @since 1.17.1
     */
    public boolean isImplicit() {
        if (!isTracked()) return false;
        return start.equals(end);
    }

    /**
     Retrieves the source range for a given Node.
     * @param node the node to retrieve the position for
     * @param start if this is the starting range. {@code false} for Element end tags.
     * @return the Range, or the Untracked (-1) position if tracking is disabled.
     */
    static Range of(Node node, boolean start) {
        final String key = start ? RangeKey : EndRangeKey;
        if (!node.hasAttr(key))
            return Untracked;
        else
            return (Range) Validate.ensureNotNull(node.attributes().userData(key));
    }

    /**
     Internal jsoup method, called by the TreeBuilder. Tracks a Range for a Node.
     * @param node the node to associate this position to
     * @param start if this is the starting range. {@code false} for Element end tags.
     */
    public void track(Node node, boolean start) {
        node.attributes().userData(start ? RangeKey : EndRangeKey, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range range = (Range) o;

        if (!start.equals(range.start)) return false;
        return end.equals(range.end);
    }

    @Override
    public int hashCode() {
        int result = start.hashCode();
        result = 31 * result + end.hashCode();
        return result;
    }

    /**
     Gets a String presentation of this Range, in the format {@code line,column:pos-line,column:pos}.
     * @return a String
     */
    @Override
    public String toString() {
        return start + "-" + end;
    }

    /**
     A Position object tracks the character position in the original input source where a Node starts or ends. If you want to
     track these positions, tracking must be enabled in the Parser with
     {@link org.jsoup.parser.Parser#setTrackPosition(boolean)}.
     @see Node#sourceRange()
     */
    public static class Position {
        private final int pos, lineNumber, columnNumber;

        /**
         Create a new Position object. Called by the TreeBuilder if source position tracking is on.
         * @param pos position index
         * @param lineNumber line number
         * @param columnNumber column number
         */
        public Position(int pos, int lineNumber, int columnNumber) {
            this.pos = pos;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }

        /**
         Gets the position index (0-based) of the original input source that this Position was read at. This tracks the
         total number of characters read into the source at this position, regardless of the number of preceding lines.
         * @return the position, or {@code -1} if untracked.
         */
        public int pos() {
            return pos;
        }

        /**
         Gets the line number (1-based) of the original input source that this Position was read at.
         * @return the line number, or {@code -1} if untracked.
         */
        public int lineNumber() {
            return lineNumber;
        }

        /**
         Gets the cursor number (1-based) of the original input source that this Position was read at. The cursor number
         resets to 1 on every new line.
         * @return the cursor number, or {@code -1} if untracked.
         */
        public int columnNumber() {
            return columnNumber;
        }

        /**
         Test if this position was tracked during parsing.
         * @return true if this was tracked during parsing, false otherwise (and all fields will be {@code -1}).
         */
        public boolean isTracked() {
            return this != UntrackedPos;
        }

        /**
         Gets a String presentation of this Position, in the format {@code line,column:pos}.
         * @return a String
         */
        @Override
        public String toString() {
            return lineNumber + "," + columnNumber + ":" + pos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            if (pos != position.pos) return false;
            if (lineNumber != position.lineNumber) return false;
            return columnNumber == position.columnNumber;
        }

        @Override
        public int hashCode() {
            int result = pos;
            result = 31 * result + lineNumber;
            result = 31 * result + columnNumber;
            return result;
        }

    }
}
