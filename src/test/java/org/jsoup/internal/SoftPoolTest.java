package org.jsoup.internal;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class SoftPoolTest {

    private static final int BufSize = 12;
    private static final int NumThreads = 5;
    private static final int NumObjects = 3;

    @Test
    public void testSoftLocalPool() throws InterruptedException {
        SoftPool<char[]> softLocalPool = new SoftPool<>(() -> new char[BufSize]);

        ExecutorService executorService = Executors.newFixedThreadPool(NumThreads);
        CountDownLatch latch = new CountDownLatch(NumThreads);

        Set<char[]> allBuffers = new HashSet<>();
        Set<char[]>[] threadLocalBuffers = new Set[NumThreads];

        for (int i = 0; i < NumThreads; i++) {
            threadLocalBuffers[i] = new HashSet<>();
        }

        AtomicInteger threadCount = new AtomicInteger();

        Runnable task = () -> {
            try {
                int threadIndex = threadCount.getAndIncrement();
                Set<char[]> localBuffers = new HashSet<>();
                // First borrow
                for (int i = 0; i < NumObjects; i++) {
                    char[] buffer = softLocalPool.borrow();
                    assertEquals(BufSize, buffer.length);
                    localBuffers.add(buffer);
                }

                // Release buffers back to the pool
                for (char[] buffer : localBuffers) {
                    softLocalPool.release(buffer);
                }

                // Borrow again and ensure buffers are reused
                for (int i = 0; i < NumObjects; i++) {
                    char[] buffer = softLocalPool.borrow();
                    assertTrue(localBuffers.contains(buffer), "Buffer was not reused in the same thread");
                    threadLocalBuffers[threadIndex].add(buffer);
                }

                synchronized (allBuffers) {
                    allBuffers.addAll(threadLocalBuffers[threadIndex]);
                }
            } finally {
                latch.countDown();
            }
        };

        // Run the tasks
        for (int i = 0; i < NumThreads; i++) {
            executorService.submit(task::run);
        }

        // Wait for all threads to complete
        latch.await();
        executorService.shutdown();

        // Ensure no buffers are shared between threads
        Set<char[]> uniqueBuffers = new HashSet<>();
        for (Set<char[]> bufferSet : threadLocalBuffers) {
            for (char[] buffer : bufferSet) {
                assertTrue(uniqueBuffers.add(buffer), "Buffer was shared between threads");
            }
        }
    }

    @Test
    public void testSoftReferenceBehavior() {
        SoftPool<char[]> softLocalPool = new SoftPool<>(() -> new char[BufSize]);

        // Borrow and release an object
        char[] buffer = softLocalPool.borrow();
        assertEquals(BufSize, buffer.length);
        softLocalPool.release(buffer);

        // Fake a GC
        softLocalPool.threadLocalStack.get().clear();

        // Ensure the object is garbage collected
        assertNull(softLocalPool.threadLocalStack.get().get());

        char[] second = softLocalPool.borrow();
        // should be different, but same size
        assertNotEquals(buffer, second);
        assertEquals(BufSize, second.length);
    }

    @Test
    public void testBorrowFromEmptyPool() {
        SoftPool<char[]> softLocalPool = new SoftPool<>(() -> new char[BufSize]);

        // Borrow from an empty pool
        char[] buffer = softLocalPool.borrow();
        assertNotNull(buffer, "Borrowed null from an empty pool");
        assertEquals(BufSize, buffer.length);
    }

    @Test
    public void testReleaseMoreThanMaxIdle() {
        SoftPool<char[]> softLocalPool = new SoftPool<>(() -> new char[BufSize]);

        // Borrow more than MaxIdle objects
        List<char[]> borrowedBuffers = new ArrayList<>();
        for (int i = 0; i < SoftPool.MaxIdle + 5; i++) {
            char[] buffer = softLocalPool.borrow();
            borrowedBuffers.add(buffer);
        }

        // Release all borrowed objects back to the pool
        for (char[] buffer : borrowedBuffers) {
            softLocalPool.release(buffer);
        }

        // Ensure the pool size does not exceed MaxIdle
        ArrayDeque<char[]> stack = softLocalPool.getStack();
        assertTrue(stack.size() <= SoftPool.MaxIdle, "Pool size exceeded MaxIdle limit");
    }
}
