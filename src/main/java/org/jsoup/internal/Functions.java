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
    private Functions() {}

    public static <T, U> Function<T, List<U>> listFunction() {
        return CollectionFunctions.listFunction();
    }

    public static <T, U> Function<T, Set<U>> setFunction() {
        return CollectionFunctions.setFunction();
    }

    public static <T, K, V> Function<T, Map<K, V>> mapFunction() {
        return CollectionFunctions.mapFunction();
    }

    public static <T, K, V> Function<T, IdentityHashMap<K, V>> identityMapFunction() {
        return CollectionFunctions.identityMapFunction();
    }
}
final class CollectionFunctions { // No public modifier
    private CollectionFunctions() {}

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
