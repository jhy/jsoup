package org.jsoup.internal;

import java.util.*;
import java.util.function.Function;

/**
 * An internal class containing functions for use with {@link Map#computeIfAbsent(Object, Function)}.
 */
public class Functions {
    private static final Function LIST_FUNCTION = key -> new ArrayList<>();
    private static final Function SET_FUNCTION = key -> new HashSet<>();
    private static final Function MAP_FUNCTION = key -> new HashMap<>();
    private static final Function IDENTITY_MAP_FUNCTION = key -> new IdentityHashMap<>();

    private Functions() {
    }

    public static <T, U> Function<T, List<U>> listFunction() {
        return (Function<T, List<U>>) LIST_FUNCTION;
    }

    public static <T, U> Function<T, Set<U>> setFunction() {
        return (Function<T, Set<U>>) SET_FUNCTION;
    }

    public static <T, K, V> Function<T, Map<K, V>> mapFunction() {
        return (Function<T, Map<K, V>>) MAP_FUNCTION;
    }

    public static <T, K, V> Function<T, IdentityHashMap<K, V>> identityMapFunction() {
        return (Function<T, IdentityHashMap<K, V>>) IDENTITY_MAP_FUNCTION;
    }
}
