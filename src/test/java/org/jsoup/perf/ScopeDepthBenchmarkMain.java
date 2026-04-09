package org.jsoup.perf;

import org.jsoup.Jsoup;

/**
 * Targeted benchmark to validate the inSpecificScope hotspot hypothesis.
 * 
 * Theory: HtmlTreeBuilder.inSpecificScope is 51% of CPU in full-suite profiling.
 * This method searches the active formatting element stack (O(n) per call).
 * 
 * Hypothesis: Parse throughput degrades with document nesting depth because
 * inSpecificScope is called frequently during tree reconstruction.
 * 
 * Test: Generate HTML with varying nesting depths and measure throughput.
 */
public class ScopeDepthBenchmarkMain {

    public static void main(String[] args) throws Exception {
        int warmup = getProperty("jsoup.perf.warmup", 5);
        int measurements = getProperty("jsoup.perf.measurements", 10);
        int opsPerMeasurement = getProperty("jsoup.perf.opsPerMeasurement", 50);
        int maxDepth = getProperty("scope.max.depth", 100);

        System.out.printf("Scope Depth Benchmark%n");
        System.out.printf("Warmup: %d, Measurements: %d, Ops/measurement: %d%n", warmup, measurements, opsPerMeasurement);
        System.out.printf("Max nesting depth: %d%n", maxDepth);
        System.out.printf("%n");

        // Test different nesting depths
        int[] depths = {10, 25, 50, 100, 250, maxDepth};

        System.out.printf("Depth,AvgMicros,P95Micros,OpsPerSecond%n");

        for (int depth : depths) {
            if (depth > maxDepth) continue;

            String html = buildNestedFormattingHtml(depth);
            long[] times = new long[measurements];

            // Warmup
            for (int w = 0; w < warmup; w++) {
                Jsoup.parse(html);
            }

            // Measurement
            for (int m = 0; m < measurements; m++) {
                long start = System.nanoTime();
                for (int op = 0; op < opsPerMeasurement; op++) {
                    Jsoup.parse(html);
                }
                long duration = System.nanoTime() - start;
                times[m] = duration / opsPerMeasurement;
            }

            long[][] stats = computeStats(times);
            long avgNanos = stats[0][0];
            long p95Nanos = stats[1][0];
            long opsPerSec = (long) (1_000_000_000.0 / avgNanos);

            System.out.printf("%d,%d,%d,%d%n", depth, avgNanos / 1_000, p95Nanos / 1_000, opsPerSec);
        }

        System.out.printf("%nDone.%n");
    }

    /**
     * Generates HTML with deeply nested formatting elements.
     * All formatting tags (b, i, em, strong) are kept open to maximize
     * calls to inSpecificScope during tree reconstruction.
     * 
     * Example with depth=3:
     * <html><body>
     * <b><i><em>text</em></i></b>
     * <b><i><em>text</em></i></b>
     * ...etc... (repeated many times with same nesting)
     * </body></html>
     */
    static String buildNestedFormattingHtml(int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>\n");

        // Repeat the nesting pattern 100 times to create parsing work
        for (int rep = 0; rep < 100; rep++) {
            // Open formatting tags
            for (int d = 0; d < depth; d++) {
                String tag = getFormattingTag(d % 4);
                sb.append("<").append(tag).append(">");
            }

            // Text content
            sb.append("Content ");

            // Close formatting tags
            for (int d = depth - 1; d >= 0; d--) {
                String tag = getFormattingTag(d % 4);
                sb.append("</").append(tag).append(">");
            }
            sb.append("\n");
        }

        sb.append("</body></html>\n");
        return sb.toString();
    }

    static String getFormattingTag(int idx) {
        switch (idx) {
            case 0: return "b";
            case 1: return "i";
            case 2: return "em";
            case 3: return "strong";
            default: return "b";
        }
    }

    static long[][] computeStats(long[] times) {
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (long t : times) {
            sum += t;
            min = Math.min(min, t);
            max = Math.max(max, t);
        }

        long avg = sum / times.length;

        // P95
        long[] sorted = times.clone();
        java.util.Arrays.sort(sorted);
        int p95Idx = (int) (times.length * 0.95);
        long p95 = sorted[p95Idx];

        return new long[][] {{avg}, {p95}};
    }

    static int getProperty(String name, int defaultValue) {
        String val = System.getProperty(name);
        return val != null ? Integer.parseInt(val) : defaultValue;
    }
}
