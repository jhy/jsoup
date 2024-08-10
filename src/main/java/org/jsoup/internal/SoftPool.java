package org.jsoup.internal;

import java.lang.ref.SoftReference;
import java.util.Stack;
import java.util.function.Supplier;

/**
 A SoftPool is a ThreadLocal that holds a SoftReference to a pool of initializable objects. This allows us to reuse
 expensive objects (buffers, etc.) between invocations (the ThreadLocal), but also for those objects to be reaped if
 they are no longer in use.
 <p>Like a ThreadLocal, should be stored in a static field.</p>
 @param <T> the type of object to pool.
 @since 1.18.2
 */
public class SoftPool<T> {
    final ThreadLocal<SoftReference<Stack<T>>> threadLocalStack;
    private final Supplier<T> initializer;
    /**
     How many total uses of the creating object might be instantiated on the same thread at once. More than this and
     those objects aren't recycled. Doesn't need to be too conservative, as they can still be GCed as SoftRefs.
     */
    static final int MaxIdle = 12;

    /**
     Create a new SoftPool.
     @param initializer a supplier that creates a new object when one is needed.
     */
    public SoftPool(Supplier<T> initializer) {
        this.initializer = initializer;
        this.threadLocalStack = ThreadLocal.withInitial(() -> new SoftReference<>(new Stack<>()));
    }

    /**
     Borrow an object from the pool, creating a new one if the pool is empty. Make sure to release it back to the pool
     when done, so that it can be reused.
     @return an object from the pool, as defined by the initializer.
     */
    public T borrow() {
        Stack<T> stack = getStack();
        if (!stack.isEmpty()) {
            return stack.pop();
        }
        return initializer.get();
    }

    /**
     Release an object back to the pool. If the pool is full, the object is not retained. If you don't want to reuse a
     borrowed object (for e.g. a StringBuilder that grew too large), just don't release it.
     @param value the object to release back to the pool.
     */
    public void release(T value) {
        Stack<T> stack = getStack();
        if (stack.size() < MaxIdle) {
            stack.push(value);
        }
    }

    Stack<T> getStack() {
        Stack<T> stack = threadLocalStack.get().get();
        if (stack == null) {
            stack = new Stack<>();
            threadLocalStack.set(new SoftReference<>(stack));
        }
        return stack;
    }
}
