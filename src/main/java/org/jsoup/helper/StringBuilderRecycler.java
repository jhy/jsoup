package org.jsoup.helper;

import java.util.ArrayList;

public class StringBuilderRecycler {
    protected final ArrayList<StringBuilder> stringBuilders = new ArrayList<>();

    public StringBuilder get(int minSize) {
        if (!stringBuilders.isEmpty()) {
            StringBuilder stringBuilder = stringBuilders.remove(stringBuilders.size() - 1);
            // Too small string builders are thrown away
            if (stringBuilder.capacity() >= minSize) {
                stringBuilder.setLength(0);
                return stringBuilder;
            }
        }
        return new StringBuilder(minSize);
    }

    public void releaseByteBuffer(StringBuilder stringBuilder) {
        stringBuilders.add(stringBuilder);
    }
}
