package org.jsoup.parser;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;

public class TokenisetStateTest {
    @Test
    public void ensureSearchArraysAreSorted() {
        char[][] arrays = {
            TokeniserState.attributeSingleValueCharsSorted,
            TokeniserState.attributeDoubleValueCharsSorted,
            TokeniserState.attributeNameCharsSorted,
            TokeniserState.attributeValueUnquoted
        };

        for (char[] array : arrays) {
            char[] copy = Arrays.copyOf(array, array.length);
            Arrays.sort(array);
            assertArrayEquals(array, copy);
        }
    }
}
