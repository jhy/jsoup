package org.jsoup.perf;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Manual benchmark harness focused on parser bottlenecks identified by JFR.
 *
 * Hotspots targeted:
 * - HtmlTreeBuilder.inSpecificScope / insertElementFor / doInsertElement
 * - TreeBuilder.enforceStackDepthLimit
 * - CharacterReader.consumeMatching / cacheString
 * - TokenData.append / flipToBuilder
 * - Normalizer.lowerCase / normalize
 */
public final class ParserBottleneckBenchmarkMain {
    private static final DecimalFormat NumberFormat = new DecimalFormat("0.00");
    private static volatile int sink;

    private ParserBottleneckBenchmarkMain() {
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);

        BenchmarkConfig config = BenchmarkConfig.load();
        List<WorkloadCase> workloads = buildWorkloads(config);
        List<ResultRow> results = new ArrayList<>();

        System.out.printf(Locale.ROOT,
            "Parser Bottleneck Benchmark%nWarmup=%d Measurements=%d Ops/measurement=%d%n%n",
            config.warmupIterations,
            config.measurementIterations,
            config.operationsPerMeasurement);

        for (WorkloadCase workload : workloads) {
            ResultRow row = measure(workload, config);
            results.add(row);
            printRow(row);
        }

        writeCsv(results, config.outputCsv);
        System.out.printf(Locale.ROOT, "%nCSV report: %s%n", config.outputCsv);
        System.out.printf(Locale.ROOT, "Blackhole: %d%n", sink);
    }

    private static List<WorkloadCase> buildWorkloads(BenchmarkConfig config) {
        List<WorkloadCase> cases = new ArrayList<>();

        for (int depth : uniqueSweep(config.maxDepth, 10, 30, 60, 120)) {
            String html = buildDeepFormattingHtml(depth, config.repetitions);
            cases.add(WorkloadCase.of(
                "scope-depth",
                String.valueOf(depth),
                "inSpecificScope|insertElementFor|doInsertElement",
                html));
        }

        for (int entitiesPerBlock : uniqueSweep(config.maxEntitiesPerBlock, 50, 120, 250)) {
            String html = buildEntityHeavyHtml(entitiesPerBlock, config.repetitions);
            cases.add(WorkloadCase.of(
                "entity-heavy",
                String.valueOf(entitiesPerBlock),
                "TokenData.append|CharacterReader.consumeMatching|cacheString",
                html));
        }

        for (int tagsPerBlock : uniqueSweep(config.maxMixedCaseTagsPerBlock, 40, 80, 140)) {
            String html = buildMixedCaseTagHtml(tagsPerBlock, config.repetitions);
            cases.add(WorkloadCase.of(
                "normalization-heavy",
                String.valueOf(tagsPerBlock),
                "Normalizer.lowerCase|normalize|StringUtil.inSorted",
                html));
        }

        for (int depth : uniqueSweep(config.maxDeepTreeDepth, 12, 25, 50)) {
            String html = buildDeepTreeHtml(depth, config.repetitions);
            cases.add(WorkloadCase.of(
                "stack-depth",
                String.valueOf(depth),
                "TreeBuilder.enforceStackDepthLimit|TreeBuilder.stepParser",
                html));
        }

        return filterWorkloads(cases, config.workloadFilter, config.parameterFilter);
    }

    private static List<WorkloadCase> filterWorkloads(List<WorkloadCase> cases, String workloadFilter, String parameterFilter) {
        if (workloadFilter == null && parameterFilter == null) return cases;

        List<WorkloadCase> filtered = new ArrayList<>();
        for (WorkloadCase workload : cases) {
            if (workloadFilter != null && !workload.name.equals(workloadFilter)) continue;
            if (parameterFilter != null && !workload.parameter.equals(parameterFilter)) continue;
            filtered.add(workload);
        }
        return filtered;
    }

    private static int[] uniqueSweep(int maxValue, int... defaults) {
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        for (int value : defaults) {
            if (value <= maxValue) values.add(value);
        }
        values.add(maxValue);

        int[] sweep = new int[values.size()];
        int i = 0;
        for (Integer value : values) {
            sweep[i++] = value;
        }
        return sweep;
    }

    private static ResultRow measure(WorkloadCase workload, BenchmarkConfig config) {
        for (int i = 0; i < config.warmupIterations; i++) {
            consume(parseAndConsume(workload.html));
        }

        long[] samples = new long[config.measurementIterations];
        int elementCount = 0;
        int nodeCount = 0;

        for (int sample = 0; sample < config.measurementIterations; sample++) {
            long start = System.nanoTime();
            for (int op = 0; op < config.operationsPerMeasurement; op++) {
                ParseStats stats = parseAndConsume(workload.html);
                elementCount = stats.elementCount;
                nodeCount = stats.nodeCount;
                consume(stats);
            }
            long elapsed = System.nanoTime() - start;
            samples[sample] = elapsed / config.operationsPerMeasurement;
        }

        Measurement measurement = summarize(samples);
        return new ResultRow(workload, elementCount, nodeCount, measurement);
    }

    private static ParseStats parseAndConsume(String html) {
        Document doc = Jsoup.parse(html);
        int elements = doc.getAllElements().size();
        int nodes = doc.nodeStream().mapToInt(n -> 1).sum();
        return new ParseStats(elements, nodes);
    }

    private static Measurement summarize(long[] samples) {
        long[] sorted = samples.clone();
        Arrays.sort(sorted);

        double total = 0;
        for (long sample : samples) {
            total += sample;
        }

        double averageNanos = total / samples.length;
        long p95Nanos = sorted[(int) Math.ceil(sorted.length * 0.95d) - 1];
        return new Measurement(averageNanos, p95Nanos, 1_000_000_000d / averageNanos);
    }

    private static void writeCsv(List<ResultRow> results, Path outputCsv) throws IOException {
        Path parent = outputCsv.getParent();
        if (parent != null) Files.createDirectories(parent);

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outputCsv, StandardCharsets.UTF_8))) {
            out.println("workload,parameter,expectedHotspot,inputBytes,elementCount,nodeCount,avgMicros,p95Micros,opsPerSecond");
            for (ResultRow row : results) {
                out.printf(Locale.ROOT, "%s,%s,%s,%d,%d,%d,%.2f,%.2f,%.2f%n",
                    csv(row.workload.name),
                    csv(row.workload.parameter),
                    csv(row.workload.expectedHotspot),
                    row.workload.html.length(),
                    row.elementCount,
                    row.nodeCount,
                    nanosToMicros(row.measurement.averageNanos),
                    nanosToMicros(row.measurement.p95Nanos),
                    row.measurement.opsPerSecond);
            }
        }
    }

    private static void printRow(ResultRow row) {
        System.out.printf(Locale.ROOT,
            "%-20s param=%-5s avg=%8s us p95=%8s us ops/s=%10s elements=%7d nodes=%7d hotspot=%s%n",
            row.workload.name,
            row.workload.parameter,
            NumberFormat.format(nanosToMicros(row.measurement.averageNanos)),
            NumberFormat.format(nanosToMicros(row.measurement.p95Nanos)),
            NumberFormat.format(row.measurement.opsPerSecond),
            row.elementCount,
            row.nodeCount,
            row.workload.expectedHotspot);
    }

    private static String buildDeepFormattingHtml(int depth, int repetitions) {
        StringBuilder sb = new StringBuilder(repetitions * depth * 20);
        sb.append("<html><body>\n");
        for (int rep = 0; rep < repetitions; rep++) {
            for (int d = 0; d < depth; d++) {
                sb.append('<').append(tagForIndex(d)).append('>');
            }
            sb.append("content");
            for (int d = depth - 1; d >= 0; d--) {
                sb.append("</").append(tagForIndex(d)).append('>');
            }
            sb.append("\n");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String buildEntityHeavyHtml(int entitiesPerBlock, int repetitions) {
        StringBuilder sb = new StringBuilder(repetitions * entitiesPerBlock * 16);
        sb.append("<html><body>\n");
        for (int rep = 0; rep < repetitions; rep++) {
            sb.append("<p data-id='p").append(rep).append("'>");
            for (int i = 0; i < entitiesPerBlock; i++) {
                sb.append("alpha&amp;beta&lt;gamma&gt;delta&quot;");
                sb.append(" &#").append(160 + (i % 30)).append(';');
            }
            sb.append("</p>\n");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String buildMixedCaseTagHtml(int tagsPerBlock, int repetitions) {
        StringBuilder sb = new StringBuilder(repetitions * tagsPerBlock * 35);
        sb.append("<html><body>\n");
        for (int rep = 0; rep < repetitions; rep++) {
            sb.append("<DiV ClAsS='Root" + rep + "'>");
            for (int i = 0; i < tagsPerBlock; i++) {
                sb.append("<SpAn DaTa-Id='X").append(i).append("' CuStOm-AtTr='V").append(i)
                    .append("'>MiXeD").append(i).append("</SpAn>");
            }
            sb.append("</DiV>\n");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String buildDeepTreeHtml(int depth, int repetitions) {
        StringBuilder sb = new StringBuilder(repetitions * depth * 20);
        sb.append("<html><body>\n");
        for (int rep = 0; rep < repetitions; rep++) {
            for (int d = 0; d < depth; d++) {
                sb.append("<div id='d").append(rep).append('-').append(d).append("'>");
            }
            sb.append("leaf");
            for (int d = depth - 1; d >= 0; d--) {
                sb.append("</div>");
            }
            sb.append("\n");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String tagForIndex(int index) {
        switch (index % 4) {
            case 0:
                return "b";
            case 1:
                return "i";
            case 2:
                return "em";
            default:
                return "strong";
        }
    }

    private static void consume(ParseStats stats) {
        sink = 31 * sink + stats.elementCount;
        sink = 31 * sink + stats.nodeCount;
    }

    private static double nanosToMicros(double nanos) {
        return nanos / 1_000d;
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static final class BenchmarkConfig {
        final int warmupIterations;
        final int measurementIterations;
        final int operationsPerMeasurement;
        final int repetitions;
        final int maxDepth;
        final int maxEntitiesPerBlock;
        final int maxMixedCaseTagsPerBlock;
        final int maxDeepTreeDepth;
        final String workloadFilter;
        final String parameterFilter;
        final Path outputCsv;

        BenchmarkConfig(int warmupIterations, int measurementIterations, int operationsPerMeasurement, int repetitions,
                        int maxDepth, int maxEntitiesPerBlock, int maxMixedCaseTagsPerBlock, int maxDeepTreeDepth,
                        String workloadFilter, String parameterFilter, Path outputCsv) {
            this.warmupIterations = warmupIterations;
            this.measurementIterations = measurementIterations;
            this.operationsPerMeasurement = operationsPerMeasurement;
            this.repetitions = repetitions;
            this.maxDepth = maxDepth;
            this.maxEntitiesPerBlock = maxEntitiesPerBlock;
            this.maxMixedCaseTagsPerBlock = maxMixedCaseTagsPerBlock;
            this.maxDeepTreeDepth = maxDeepTreeDepth;
            this.workloadFilter = workloadFilter;
            this.parameterFilter = parameterFilter;
            this.outputCsv = outputCsv;
        }

        static BenchmarkConfig load() {
            int warmup = Integer.getInteger("jsoup.perf.warmup", 5);
            int measurements = Integer.getInteger("jsoup.perf.measurements", 10);
            int ops = Integer.getInteger("jsoup.perf.opsPerMeasurement", 25);
            int repetitions = Integer.getInteger("jsoup.perf.repetitions", 80);
            int maxDepth = Integer.getInteger("jsoup.perf.maxDepth", 120);
            int maxEntities = Integer.getInteger("jsoup.perf.maxEntitiesPerBlock", 250);
            int maxMixedCaseTags = Integer.getInteger("jsoup.perf.maxMixedCaseTagsPerBlock", 140);
            int maxDeepTreeDepth = Integer.getInteger("jsoup.perf.maxDeepTreeDepth", 50);
            String workloadFilter = System.getProperty("jsoup.perf.workload");
            String parameterFilter = System.getProperty("jsoup.perf.parameter");
            Path output = Paths.get(System.getProperty("jsoup.perf.output", "target/perf/parser-bottleneck-benchmark.csv"));
            return new BenchmarkConfig(warmup, measurements, ops, repetitions, maxDepth, maxEntities,
                maxMixedCaseTags, maxDeepTreeDepth, workloadFilter, parameterFilter, output);
        }
    }

    private static final class WorkloadCase {
        final String name;
        final String parameter;
        final String expectedHotspot;
        final String html;

        private WorkloadCase(String name, String parameter, String expectedHotspot, String html) {
            this.name = name;
            this.parameter = parameter;
            this.expectedHotspot = expectedHotspot;
            this.html = html;
        }

        static WorkloadCase of(String name, String parameter, String expectedHotspot, String html) {
            return new WorkloadCase(name, parameter, expectedHotspot, html);
        }
    }

    private static final class Measurement {
        final double averageNanos;
        final long p95Nanos;
        final double opsPerSecond;

        private Measurement(double averageNanos, long p95Nanos, double opsPerSecond) {
            this.averageNanos = averageNanos;
            this.p95Nanos = p95Nanos;
            this.opsPerSecond = opsPerSecond;
        }
    }

    private static final class ParseStats {
        final int elementCount;
        final int nodeCount;

        private ParseStats(int elementCount, int nodeCount) {
            this.elementCount = elementCount;
            this.nodeCount = nodeCount;
        }
    }

    private static final class ResultRow {
        final WorkloadCase workload;
        final int elementCount;
        final int nodeCount;
        final Measurement measurement;

        private ResultRow(WorkloadCase workload, int elementCount, int nodeCount, Measurement measurement) {
            this.workload = workload;
            this.elementCount = elementCount;
            this.nodeCount = nodeCount;
            this.measurement = measurement;
        }
    }
}
