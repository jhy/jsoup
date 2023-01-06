package org.jsoup.helper;

/**
 A functional interface (ala Java's {@link java.util.function.Consumer} interface, previously implemented here for cross
 compatibility with Android before desugaring support was introduced.
 @param <T> the input type
 @deprecated Use {@link java.util.function.Consumer} instead. This interface will be removed in the next release. If you
 are targeting Android, see how to enable
 <a href="https://developer.android.com/studio/write/java8-support#library-desugaring">desugaring</a>. */
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
