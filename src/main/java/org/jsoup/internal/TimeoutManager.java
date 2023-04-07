package org.jsoup.internal;

public class TimeoutManager {
    private long startTime;
    private long timeout = 0; // optional max time of request
    public TimeoutManager() {
        startTime = System.nanoTime();
    }
    public void set(long timeoutMillis) {
        this.timeout = timeoutMillis * 1000000;
    }
    public void start(long startTimeNanos) {
        startTime = startTimeNanos;
    }
    public boolean expired() {
        if (timeout == 0)
            return false;

        final long now = System.nanoTime();
        final long dur = now - startTime;
        return (dur > timeout);
    }
}
