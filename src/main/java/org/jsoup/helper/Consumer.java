package org.jsoup.helper;

/**
 A functional interface (ala Java's {@link java.util.function.Consumer} interface, implemented here for cross compatibility with Android.
 @param <T> the input type
 */
@FunctionalInterface
public interface Consumer<T> {

    /**
     * Execute this operation on the supplied argument. It is expected to have side effects.
     *
     * @param t the input argument
     */
    void accept(T t);
}
