package org.jsoup.helper;

public class ArrayUtil {
    /**
     * Check if two arrays insensitive in their orders are equal.
     * Note that arrays with primitive types should be wrapped into their Wrapper classes.
     *
     * @param a,b two arrays with the same data type
     * @return if both two arrays have same elements (order of elements in two arrays doesn't matter)
     * @author Lexam Chen, lexamxu1231@gmail.com
     *
     * This added method aims to solve this issue:
     * CS304 Issue link: https://github.com/jhy/jsoup/issues/1492
     **/
    public static <T> boolean equalOrderInsensitiveArray(T[] a, T[] b) {
        if (a.length != b.length)
            return false;
        int size = a.length;
        boolean[] isMatched = new boolean[size];
        for (T elementA : a) {
            for (int j = 0; j < size; ++j) {
                T elementB = b[j];
                // compare null value
                if (elementA == null && elementB == null && !isMatched[j]) {
                    isMatched[j] = true;
                    break;
                }
                if (elementA == null) {
                    continue;
                }
                // compare not null value
                if (elementA.equals(elementB) && !isMatched[j]) {
                    isMatched[j] = true;
                    break;
                }
            }
        }
        for (boolean flag : isMatched) {
            if (!flag)
                return false;
        }
        return true;
    }
}
