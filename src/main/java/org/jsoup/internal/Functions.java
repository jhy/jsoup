package org.jsoup.internal;

import java.util.*;
import java.util.function.Function;

/**
 * An internal class containing functions for use with {@link Map#computeIfAbsent(Object, Function)}.
 */
public class Functions {
    private Functions() {
    }

    public static <T, U> Function<T, List<U>> listFunction() {
        return key -> new ArrayList<>();
    }

    public static <T, U> Function<T, Set<U>> setFunction() {
        return key -> new HashSet<>();
    }

    public static <T, K, V> Function<T, Map<K, V>> mapFunction() {
        return key -> new HashMap<>();
    }

    public static <T, K, V> Function<T, IdentityHashMap<K, V>> identityMapFunction() {
        return key -> new IdentityHashMap<>();
    }
}
