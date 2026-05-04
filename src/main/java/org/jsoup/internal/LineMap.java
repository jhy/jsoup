package org.jsoup.internal;

import java.util.Arrays;

/**
 Maps source offsets to line and column coordinates. Jsoup internal; API subject to change.
 */
public final class LineMap {
    private static final int InitialLineCapacity = 16;
    private static final int CapacityGrowthFactor = 2;
    private static final int[] Empty = new int[0];
    private int[] lineStarts = Empty;
    private int size;

    /**
     Creates a shared line map for tracked source ranges.
     */
    public LineMap() {}

    /**
     Records the source offset immediately after a newline.
     */
    public void addLineStart(int pos) {
        // Buffer scans overlap after compaction, so ignore line starts already recorded.
        if (size == 0 || pos > lineStarts[size - 1]) {
            ensureCapacity(size + 1);
            lineStarts[size++] = pos;
        }
    }

    /**
     Trims the backing array after the parse has completed.
     */
    public void complete() {
        if (lineStarts.length != size)
            lineStarts = size == 0 ? Empty : Arrays.copyOf(lineStarts, size);
    }

    /**
     Gets the 1-based line number for a source offset.
     */
    public int lineNumber(int pos) {
        if (pos < 0)
            return -1;
        int lineIndex = lineStartIndex(pos);
        // -1 means before the first newline, so line 1. lineStarts[0] is the start of line 2.
        return lineIndex + 2;
    }

    /**
     Gets the 1-based column number for a source offset.
     */
    public int columnNumber(int pos) {
        if (pos < 0)
            return -1;
        int index = lineStartIndex(pos);
        return index == -1 ? pos + 1 : pos - lineStarts[index] + 1;
    }

    /**
     Finds the preceding line-start entry for a source offset.
     */
    private int lineStartIndex(int pos) {
        int index = Arrays.binarySearch(lineStarts, 0, size, pos);
        return index >= 0 ? index : -index - 2;
    }

    /**
     Grows line-start storage with a small initial allocation and doubling growth.
     */
    private void ensureCapacity(int minSize) {
        if (lineStarts.length >= minSize)
            return;
        int newSize = lineStarts.length == 0 ? InitialLineCapacity : lineStarts.length * CapacityGrowthFactor;
        if (newSize < minSize)
            newSize = minSize;
        lineStarts = Arrays.copyOf(lineStarts, newSize);
    }
}
