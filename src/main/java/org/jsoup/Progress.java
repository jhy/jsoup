package org.jsoup;

@FunctionalInterface

public interface Progress<ProgressContext> {
    /**
     Called to report progress. Note that this will be executed by the same thread that is doing the work, so either
     don't take to long, or hand it off to another thread.
     @param processed the number of bytes processed so far.
     @param total the total number of expected bytes, or -1 if unknown.
     @param percent the percentage of completion, 0.0..100.0. If the expected total is unknown, % will remain at zero
     until complete.
     @param context the object that progress was made on.
     @since 1.18.1
     */
    void onProgress(int processed, int total, float percent, ProgressContext context);
}
