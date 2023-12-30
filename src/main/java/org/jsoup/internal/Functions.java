package org.jsoup.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * An internal class containing functions for use with {@link Map#computeIfAbsent(Object, Function)}.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class Functions {
    private static final Function ListFunction = key -> new ArrayList<>();
    private static final Function SetFunction = key -> new HashSet<>();
    private static final Function MapFunction = key -> new HashMap<>();
    private static final Function IdentityMapFunction = key -> new IdentityHashMap<>();

    private Functions() {
    }

    public static <T, U> Function<T, List<U>> listFunction() {
        return (Function<T, List<U>>) ListFunction;
    }

    public static <T, U> Function<T, Set<U>> setFunction() {
        return (Function<T, Set<U>>) SetFunction;
    }

    public static <T, K, V> Function<T, Map<K, V>> mapFunction() {
        return (Function<T, Map<K, V>>) MapFunction;
    }

    public static <T, K, V> Function<T, IdentityHashMap<K, V>> identityMapFunction() {
        return (Function<T, IdentityHashMap<K, V>>) IdentityMapFunction;
    }
}
