package org.jsoup.helper;

import java.util.ArrayList;

public class BufferRecycler {
    protected final ArrayList<char[]> charBuffers = new ArrayList<>();

    public char[] allocCharBuffer(int minSize) {
        if (minSize < 1024) {
            return calloc(minSize);
        }
        char[] buffer = null;
        if (!charBuffers.isEmpty()) {
            buffer = charBuffers.remove(charBuffers.size() -1);
        }
        if (buffer == null || buffer.length < minSize) {
            buffer = calloc(minSize);
        }
        return buffer;
    }

    public void releaseCharBuffer(char[] buffer) {
        if (buffer != null && buffer.length >= 1024) {
            charBuffers.add(buffer);
        }
    }

    protected char[] calloc(int size) {
        return new char[size];
    }
}
