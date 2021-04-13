package org.jsoup.helper;

public class ArrayUtil {
    /**
     * Compare two arrays insensitive in the order.
     * Note that arrays with primitive types should be wrapped into Wrapper classes.
     *
     * @param a,b two arrays
     * @return true, false
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

