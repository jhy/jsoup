package org.jsoup.helper;

/**
 A functional interface (ala Java's {@link java.util.function.Consumer} interface, implemented here for cross compatibility with Android.
 @param <T> the input type

 @deprecated This will be removed in favor of using {@link java.util.function.Consumer} instead in the next release.
 */
@Deprecated
@FunctionalInterface
public interface Consumer<T> extends java.util.function.Consumer<T> {
    /**
     * Execute this operation on the supplied argument. It is expected to have side effects.
     *
     * @param t the input argument
     */
    @Override
    void accept(T t);
}
