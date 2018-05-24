package org.jsoup.integration;

import java.util.Date;

/**
 Does an A/B test on two methods, and prints out how long each took.

 @author Jonathan Hedley, jonathan@hedley.net */
public class Benchmark {
    public static void run(Runnable a, Runnable b, int count) {
        long aMillis;
        long bMillis;

        print("Running test A (x%d)", count);
        aMillis = time(a, count);
        print("Running test B");
        bMillis = time(b, count);

        print("\nResults:");
        print("A: %.2fs", aMillis / 1000f);
        print("B: %.2fs", bMillis / 1000f);
        print("\nB ran in %.2f %% time of A\n", (bMillis *1f / aMillis * 1f) * 100f);
    }

    private static long time(Runnable test, int count) {
        Date start = new Date();
        for (int i = 0; i < count; i++) {
            test.run();
        }
        Date end = new Date();
        return end.getTime() - start.getTime();
    }

    private static void print(String msgFormat, Object... msgParams) {
        System.out.println(String.format(msgFormat, msgParams));
    }
}
