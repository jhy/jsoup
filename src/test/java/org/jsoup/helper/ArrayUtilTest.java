package org.jsoup.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArrayUtilTest {
    //CS304 (manually written) Issue link: https://github.com/jhy/jsoup/issues/1492
    @Test
    public void equalOrderInsensitiveArray() {
        Integer[] a = new Integer[]{1, 2, 3};
        Integer[] b = new Integer[]{2, 3, 1};
        assertTrue(ArrayUtil.equalOrderInsensitiveArray(a, b));
    }

    //CS304 (manually written) Issue link: https://github.com/jhy/jsoup/issues/1492
    @Test
    public void notEqualArray() {
        Integer[] a = new Integer[]{2, null, null};
        Integer[] b = new Integer[]{2, 2, null};
        assertFalse(ArrayUtil.equalOrderInsensitiveArray(a, b));
    }
}
