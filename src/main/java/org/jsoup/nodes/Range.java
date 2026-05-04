package org.jsoup.nodes;

import org.jsoup.internal.LineMap;
import org.jsoup.internal.StringUtil;

import java.util.Arrays;
import java.util.Objects;

/**
 A Range tracks the source offsets where a Node starts or ends. Line and column coordinates are derived from the
 line map retained during parsing. To track these positions, enable {@link org.jsoup.parser.Parser#setTrackPosition(boolean)}
 before parsing.
 @see Node#sourceRange()
 @since 1.15.2
 */
public class Range {
    // sentinels
    private static final LineMap UnsetLineMap  = new LineMap();
    private static final int[] UnsetAttrRanges = new int[0];
    private static final Position UntrackedPos = new Position(-1, -1, -1);
    private static final Range Untracked       = new Range();

    private final LineMap lineMap;
    private final int startPos;
    private final int endPos;

    /**
     Creates the untracked source range sentinel.
     */
    private Range() {
        lineMap = UnsetLineMap;
        startPos = -1;
        endPos = -1;
    }

    /**
     Creates a new Range from source offsets.
     */
    private Range(LineMap lineMap, int startPos, int endPos) {
        this.lineMap = lineMap;
        if (startPos < 0 || endPos < 0)
            throw new IllegalArgumentException("Range positions must be non-negative");
        this.startPos = startPos;
        this.endPos = endPos;
    }

    /**
     Deprecated parser-internal source range setup method, retained for source compatibility. The line and column values
     in the supplied Positions are not retained; they are derived from source offsets. If either supplied Position is
     untracked, this Range will also be untracked.

     @param start the start position
     @param end   the end position
     @deprecated Use parser position tracking instead. Will be removed in jsoup 1.24.1.
     */
    @Deprecated
    public Range(Position start, Position end) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        if (start.pos < -1 || end.pos < -1)
            throw new IllegalArgumentException("Range positions must be non-negative, or -1 for untracked");
        if (start.pos == -1 || end.pos == -1) {
            lineMap = UnsetLineMap;
            startPos = -1;
            endPos = -1;
        } else {
            lineMap = new LineMap();
            startPos = start.pos;
            endPos = end.pos;
        }
    }

    /**
     Get the start position of this range, with 1-based line and column coordinates.
     * @return the start position.
     */
    public Position start() {
        return startPos == -1 ? UntrackedPos : position(startPos);
    }

    /**
     Get the starting source offset of this range.
     @return the 0-based start source offset.
     @since 1.17.1
     */
    public int startPos() {
        return startPos;
    }

    /**
     Get the end position of this range, with 1-based line and column coordinates.
     * @return the end position.
     */
    public Position end() {
        return endPos == -1 ? UntrackedPos : position(endPos);
    }

    /**
     Get the ending source offset of this range.
     @return the 0-based ending source offset.
     @since 1.17.1
     */
    public int endPos() {
        return endPos;
    }

    /**
     Test if this range has source offsets available.
     * @return true if this range has source offsets, false otherwise (and all fields will be {@code -1}).
     */
    public boolean isTracked() {
        return startPos != -1;
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
        return isTracked() && startPos == endPos;
    }

    /**
     Creates a Position from a source offset and this Range's line map.
     */
    private Position position(int pos) {
        return new Position(pos, lineMap.lineNumber(pos), lineMap.columnNumber(pos));
    }

    /**
     Retrieves the start source range for a given Node.
     * @param node the node to retrieve the position for
     * @return the Range, or the Untracked (-1) position if tracking is disabled.
     */
    static Range ofStart(Node node) {
        Range.Spans rangeSpans = node.spans();
        return rangeSpans != null ? rangeSpans.sourceRange() : Untracked;
    }

    /**
     Retrieves the end source range for a given Element.
     * @param element the element to retrieve the end tag position for
     * @return the Range, or the Untracked (-1) position if tracking is disabled.
     */
    static Range ofEnd(Element element) {
        Range.Spans rangeSpans = element.spans();
        return rangeSpans != null ? rangeSpans.endSourceRange() : Untracked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Range range = (Range) o;

        return startPos == range.startPos && endPos == range.endPos;
    }

    @Override
    public int hashCode() {
        int result = startPos;
        result = 31 * result + endPos;
        return result;
    }

    /**
     Gets a String representation of this Range, in the format {@code line,column:pos-line,column:pos}.
     * @return a String
     */
    @Override
    public String toString() {
        StringBuilder sb = StringUtil.borrowBuilder()
            .append(start())
            .append('-')
            .append(end());
        return StringUtil.releaseBuilder(sb);
    }

    /**
     A Position describes a source offset and its line and column coordinates. Positions are available when position
     tracking is enabled with {@link org.jsoup.parser.Parser#setTrackPosition(boolean)} before parsing.
     @see Node#sourceRange()
     */
    public static class Position {
        private final int pos, lineNumber, columnNumber;

        /**
         Deprecated parser-internal position setup method, retained for source compatibility. Position objects are
         normally derived from a Range's retained source offsets.
         * @param pos position index
         * @param lineNumber line number
         * @param columnNumber column number
         @deprecated Use parser position tracking instead. Will be removed in jsoup 1.24.1.
         */
        @Deprecated
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
            return pos != -1;
        }

        /**
         Gets a String presentation of this Position, in the format {@code line,column:pos}.
         * @return a String
         */
        @Override
        public String toString() {
            StringBuilder sb = StringUtil.borrowBuilder()
                .append(lineNumber)
                .append(',')
                .append(columnNumber)
                .append(':')
                .append(pos);
            return StringUtil.releaseBuilder(sb);
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
            return Objects.hash(pos, lineNumber, columnNumber);
        }
    }

    public static class AttributeRange {
        static final AttributeRange UntrackedAttr = new AttributeRange();

        private final LineMap lineMap;
        private final int nameStartPos, nameEndPos, valueStartPos, valueEndPos;

        /**
         Creates the untracked attribute source range sentinel.
         */
        private AttributeRange() {
            lineMap         = UnsetLineMap;
            nameStartPos    = -1;
            nameEndPos      = -1;
            valueStartPos   = -1;
            valueEndPos     = -1;
        }

        /**
         Creates a new AttributeRange from source offsets.
         */
        private AttributeRange(LineMap lineMap, int nameStartPos, int nameEndPos, int valueStartPos, int valueEndPos) {
            this.lineMap = lineMap;
            if (nameStartPos < 0 || nameEndPos < 0 || valueStartPos < 0 || valueEndPos < 0)
                throw new IllegalArgumentException("Attribute range positions must be non-negative");
            this.nameStartPos = nameStartPos;
            this.nameEndPos = nameEndPos;
            this.valueStartPos = valueStartPos;
            this.valueEndPos = valueEndPos;
        }

        /**
         Deprecated parser-internal source range setup method, retained for source compatibility. Source ranges are
         normally produced by enabling parser position tracking before parsing. If either supplied Range is untracked,
         this AttributeRange will also be untracked.
         @deprecated Use parser position tracking instead. Will be removed in jsoup 1.24.1.
         */
        @Deprecated
        public AttributeRange(Range nameRange, Range valueRange) {
            Objects.requireNonNull(nameRange);
            Objects.requireNonNull(valueRange);
            if (!nameRange.isTracked() || !valueRange.isTracked()) {
                lineMap         = UnsetLineMap;
                nameStartPos    = -1;
                nameEndPos      = -1;
                valueStartPos   = -1;
                valueEndPos     = -1;
            } else {
                lineMap         = nameRange.lineMap;
                nameStartPos    = nameRange.startPos;
                nameEndPos      = nameRange.endPos;
                valueStartPos   = valueRange.startPos;
                valueEndPos     = valueRange.endPos;
            }
        }

        /** Get the source range for the attribute's name. */
        public Range nameRange() {
            return isTracked() ? new Range(lineMap, nameStartPos, nameEndPos) : Range.Untracked;
        }

        /** Get the source range for the attribute's value. */
        public Range valueRange() {
            return isTracked() ? new Range(lineMap, valueStartPos, valueEndPos) : Range.Untracked;
        }

        /**
         Tests if this attribute range has tracked name and value offsets.
         * @return true if the attribute's name and value ranges were tracked; false otherwise.
         @since 1.23.1
         */
        public boolean isTracked() {
            return nameStartPos != -1;
        }

        /**
         Get a String representation of this Attribute range, in the form
         {@code line,column:pos-line,column:pos=line,column:pos-line,column:pos} (name start - name end = val start - val end)
         */
        @Override
        public String toString() {
            StringBuilder sb = StringUtil.borrowBuilder()
                    .append(nameRange())
                    .append('=')
                    .append(valueRange());
            return StringUtil.releaseBuilder(sb);
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AttributeRange that = (AttributeRange) o;

            if (nameStartPos    != that.nameStartPos) return false;
            if (nameEndPos      != that.nameEndPos) return false;
            if (valueStartPos   != that.valueStartPos) return false;
            return valueEndPos  == that.valueEndPos;
        }

        @Override public int hashCode() {
            int result = nameStartPos;
            result = 31 * result + nameEndPos;
            result = 31 * result + valueStartPos;
            result = 31 * result + valueEndPos;
            return result;
        }
    }

    /**
     Internal range span storage attached to a Node or Attributes object.
     <p>Unset records use {@code -1}; once written, a node, end-tag, or attribute range record is complete.</p>
     */
    static final class Spans {
        private static final int AttrRangeWidth = 4;

        private LineMap lineMap     = UnsetLineMap;
        private int nodeStartPos    = -1;
        private int nodeEndPos      = -1;
        private int endTagStartPos  = -1;
        private int endTagEndPos    = -1;
        private int[] attrRanges    = UnsetAttrRanges;

        /**
         Gets the node start source range.
         */
        private Range sourceRange() {
            return range(nodeStartPos, nodeEndPos);
        }

        /**
         Gets the element end tag source range.
         */
        private Range endSourceRange() {
            return range(endTagStartPos, endTagEndPos);
        }

        /**
         Sets the node start source range.
         */
        void sourceRange(LineMap lineMap, int startPos, int endPos) {
            useLineMap(lineMap);
            nodeStartPos = startPos;
            nodeEndPos = endPos;
        }

        /**
         Sets the element end tag source range.
         */
        void endSourceRange(LineMap lineMap, int startPos, int endPos) {
            useLineMap(lineMap);
            endTagStartPos = startPos;
            endTagEndPos = endPos;
        }

        /**
         Gets the source ranges for an attribute slot.
         */
        Range.AttributeRange attributeRange(int index) {
            if (index < 0)
                return Range.AttributeRange.UntrackedAttr;

            int[] ranges = attrRanges;
            int nameIndex = attrNameStartIndex(index);
            int valueIndex = attrValueStartIndex(index);
            int valueEndIndex = valueIndex + 1;
            if (valueEndIndex >= ranges.length)
                return Range.AttributeRange.UntrackedAttr;

            int nameStart = ranges[nameIndex];
            int nameEnd = ranges[nameIndex + 1];
            int valueStart = ranges[valueIndex];
            int valueEnd = ranges[valueEndIndex];
            if (nameStart == -1)
                return Range.AttributeRange.UntrackedAttr;

            return new Range.AttributeRange(lineMap, nameStart, nameEnd, valueStart, valueEnd);
        }

        /**
         Sets the source ranges for an attribute slot.
         */
        void attributeRange(int index, Range.AttributeRange range) {
            attributeRange(
                index,
                range.lineMap,
                range.nameStartPos,
                range.nameEndPos,
                range.valueStartPos,
                range.valueEndPos
            );
        }

        /**
         Sets source range offsets for an attribute slot.
         */
        void attributeRange(int index, LineMap lineMap, int nameStart, int nameEnd, int valueStart, int valueEnd) {
            if (nameStart < 0 || nameEnd < 0 || valueStart < 0 || valueEnd < 0)
                throw new IllegalArgumentException("Attribute range positions must be non-negative");
            useLineMap(lineMap);
            int nameIndex = attrNameStartIndex(index);
            int valueIndex = attrValueStartIndex(index);
            ensureAttributeCapacity(valueIndex + 2);
            attrRanges[nameIndex] = nameStart;
            attrRanges[nameIndex + 1] = nameEnd;
            attrRanges[valueIndex] = valueStart;
            attrRanges[valueIndex + 1] = valueEnd;
        }

        /**
         Retains the first line map and rejects mixed-source ranges.
         */
        private void useLineMap(LineMap lineMap) {
            if (this.lineMap == UnsetLineMap) {
                this.lineMap = lineMap;
            } else if (this.lineMap != lineMap) {
                throw new IllegalArgumentException("Source ranges must come from the same parse");
            }
        }

        /**
         Removes an attribute slot and shifts following source ranges.
         */
        void removeAttributeRange(int index) {
            if (index < 0) return;
            int[] ranges = attrRanges;
            int removeIndex = attrNameStartIndex(index);
            if (removeIndex >= ranges.length) return;

            int nextIndex = removeIndex + AttrRangeWidth;
            int shifted = ranges.length - nextIndex;
            if (shifted > 0)
                System.arraycopy(ranges, nextIndex, ranges, removeIndex, shifted);
            Arrays.fill(ranges, ranges.length - AttrRangeWidth, ranges.length, -1);
        }

        /**
         Returns a copy whose source range arrays can mutate independently.
         */
        Spans copy() {
            Spans copy = new Spans();
            copy.lineMap = lineMap;
            copy.nodeStartPos = nodeStartPos;
            copy.nodeEndPos = nodeEndPos;
            copy.endTagStartPos = endTagStartPos;
            copy.endTagEndPos = endTagEndPos;
            copy.attrRanges = attrRanges.length == 0 ? UnsetAttrRanges : attrRanges.clone();
            return copy;
        }

        /**
         Grows attribute range storage to hold the requested slot count.
         */
        private void ensureAttributeCapacity(int minLength) {
            if (attrRanges.length >= minLength) return;
            int oldLength = attrRanges.length;
            attrRanges = Arrays.copyOf(attrRanges, minLength);
            Arrays.fill(attrRanges, oldLength, attrRanges.length, -1);
        }

        /**
         Creates a Range from stored offsets.
         */
        private Range range(int startPos, int endPos) {
            if (startPos == -1)
                return Range.Untracked;
            return new Range(lineMap, startPos, endPos);
        }

        /**
         Maps an attribute slot to its stored name range start slot.
         */
        private static int attrNameStartIndex(int index) {
            return index * AttrRangeWidth;
        }

        /**
         Maps an attribute slot to its stored value range start slot.
         */
        private static int attrValueStartIndex(int index) {
            return index * AttrRangeWidth + 2;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Spans spans = (Spans) o;
            return nodeStartPos == spans.nodeStartPos &&
                nodeEndPos == spans.nodeEndPos &&
                endTagStartPos == spans.endTagStartPos &&
                endTagEndPos == spans.endTagEndPos &&
                Arrays.equals(attrRanges, spans.attrRanges);
        }

        @Override public int hashCode() {
            int result = Objects.hash(nodeStartPos, nodeEndPos, endTagStartPos, endTagEndPos);
            result = 31 * result + Arrays.hashCode(attrRanges);
            return result;
        }
    }
}
