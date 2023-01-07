package org.jsoup.internal;

import org.jsoup.helper.Validate;

import javax.annotation.Nullable;
import java.lang.ref.SoftReference;
import java.util.Stack;

/**
 jsoup internal use only - maintains a pool of various threadlocal, soft-reference buffers used by the parser and for
 serialization. */
public final class BufferPool<T> {
    // char[] used in CharacterReader (input we are reading)
    // String[] used in CharacterReader (token cache)
    // String Builders (accumulators)

    public interface Lifecycle<T> {
        /**
         Called when an object is borrowed but none are in the pool. Should return a new object of the desired min
         capacity.
         */
        T create();

        /**
         Called when an object is returned. Should resize the object (via create) if it has grown too large, and empty
         its contents if not for reuse.
         */
        T reset(T obj);
    }

    final Lifecycle<T> lifecycle;
    final ThreadLocal<SoftReference<Stack<T>>> pool;
    final int maxIdle;

    /**
     Creats a new BufferPool. Prefer to reuse an existing pool - centralize if required.
     @param maxIdle the maximum number of objects to hold that are not actively in use. Per thread - used so that
     e.g. multiple StringBuilders can be in use at once.
     @param lifecycle the implemention of the pooled objects lifecycle -- create and reset
     */
    public BufferPool(int maxIdle, Lifecycle<T> lifecycle) {
        this.lifecycle = lifecycle;
        this.maxIdle = maxIdle;
        pool = ThreadLocal.withInitial(() -> new SoftReference<>(new Stack<>()));
    }

    /**
     Grab an object from the pool, or create one if required.
     */
    public T borrow() {
        Stack<T> stack = getStack();
        return stack.empty() ?
            lifecycle.create() :
            stack.pop();
    }

    /**
     Place an object back in the pool. Will reset() the object, and trims the pool to maxIdle.
     */
    public void release(T obj) {
        Validate.notNull(obj);
        obj = lifecycle.reset(obj); // clear the contents, and prevent from growing too large (per impl)
        Stack<T> stack = getStack();
        stack.push(obj);

        // trim stack to maxIdle objects
        while (stack.size() > maxIdle) stack.pop();
    }

    private Stack<T> getStack() {
        SoftReference<Stack<T>> ref = pool.get();
        @Nullable Stack<T> stack = ref.get();
        if (stack == null) { // got GCed, reset it
            stack = new Stack<>();
            pool.set(new SoftReference<>(stack));
        }
        return stack;
    }
}
