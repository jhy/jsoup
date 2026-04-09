package org.jsoup.perf;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.integration.ParseTest;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.select.Evaluator;
import org.jsoup.select.Selector;

/**
 * Manual performance harness for selector execution.
 *
 * <p>Focuses on one concrete constraint: repeated selector parsing versus repeated evaluator reuse.
 * The harness produces console output plus a CSV artifact so runs can be compared over time.</p>
 */
public final class SelectorBenchmarkMain {
    private static final DecimalFormat NumberFormat = new DecimalFormat("0.00");
    private static volatile int sink;

    private SelectorBenchmarkMain() {
    }

    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);

        BenchmarkConfig config = BenchmarkConfig.load();
        List<DocumentCase> documents = buildDocuments(config);
        List<ResultRow> results = new ArrayList<>();

        for (DocumentCase documentCase : documents) {
            BenchmarkPrinter.printDocumentHeader(documentCase);
            for (SelectorScenario scenario : documentCase.scenarios) {
                results.addAll(runScenario(documentCase, scenario, config));
            }
        }

        writeCsv(results, config.outputCsv);
        BenchmarkPrinter.printSummary(results, config.outputCsv);
    }

    private static List<DocumentCase> buildDocuments(BenchmarkConfig config) throws IOException {
        List<DocumentCase> documents = new ArrayList<>();
        documents.add(buildSyntheticDocument(config.syntheticSections, config.syntheticCardsPerSection));
        documents.add(buildXwikiDocument());
        return documents;
    }

    private static DocumentCase buildSyntheticDocument(int sections, int cardsPerSection) {
        StringBuilder html = new StringBuilder(sections * cardsPerSection * 550);
        html.append("<!doctype html><html><head><title>Selector Benchmark</title></head><body>");
        for (int section = 0; section < sections; section++) {
            html.append("<section class=feed id=section-").append(section).append('>');
            for (int card = 0; card < cardsPerSection; card++) {
                int feature = card % 50;
                html.append("<article class='card tier-").append(card % 5)
                    .append("' data-section='").append(section)
                    .append("' data-card='").append(card)
                    .append("'><h2 class=title>Feature ")
                    .append(feature)
                    .append("</h2><div class=meta><span class=badge>Badge ")
                    .append(card % 7)
                    .append("</span><span class=author>Author ")
                    .append(section % 11)
                    .append("</span></div><ul class=tags>");

                for (int tag = 0; tag < 6; tag++) {
                    html.append("<li class='tag tag-").append(tag).append("'>tag-").append(tag).append("</li>");
                }

                html.append("</ul><p class=summary>Feature ")
                    .append(feature)
                    .append(" section ")
                    .append(section)
                    .append(" card ")
                    .append(card)
                    .append(" has enough text for contains and regex selectors.</p><a class=detail-link href='https://example.com/")
                    .append(section)
                    .append('/')
                    .append(card)
                    .append("'>Open</a></article>");
            }
            html.append("</section>");
        }
        html.append("</body></html>");

        Document document = Jsoup.parse(html.toString());
        List<SelectorScenario> scenarios = Arrays.asList(
            new SelectorScenario("class-scan", ".card"),
            new SelectorScenario("descendant-chain", "section.feed article.card span.badge"),
            new SelectorScenario("structural-nth", "section.feed > article.card:nth-child(2n) > ul.tags > li:nth-child(3)"),
            new SelectorScenario("contains-text", "article.card:contains(Feature 17)"),
            new SelectorScenario("regex-text", "article.card:matches((?i)feature\\s+1[0-9])")
        );
        return new DocumentCase("synthetic-feed", document, scenarios);
    }

    private static DocumentCase buildXwikiDocument() throws IOException {
        File input = ParseTest.getFile("/htmltests/xwiki-1324.html.gz");
        Document document = Jsoup.parse(input, null, "https://localhost/");
        List<SelectorScenario> scenarios = Arrays.asList(
            new SelectorScenario("link-scan", "a[href]"),
            new SelectorScenario("descendant-links", "div a[href]"),
            new SelectorScenario("has-links", "div:has(a[href])"),
            new SelectorScenario("exact-attribute", "[data-id=userdirectory]"),
            new SelectorScenario("attribute-contains", "a[href*=XWiki]")
        );
        return new DocumentCase("xwiki-1324", document, scenarios);
    }

    private static List<ResultRow> runScenario(DocumentCase documentCase, SelectorScenario scenario, BenchmarkConfig config) {
        List<ResultRow> rows = new ArrayList<>();
        Evaluator evaluator = Selector.evaluatorOf(scenario.query);

        rows.add(measure(documentCase, scenario, config, "parse-only",
            () -> System.identityHashCode(Selector.evaluatorOf(scenario.query))));

        rows.add(measure(documentCase, scenario, config, "select-string", () -> {
            Elements elements = documentCase.document.select(scenario.query);
            return elements.size();
        }));

        rows.add(measure(documentCase, scenario, config, "select-evaluator", () -> {
            Elements elements = documentCase.document.select(evaluator);
            return elements.size();
        }));

        BenchmarkPrinter.printScenario(rows);
        return rows;
    }

    private static ResultRow measure(DocumentCase documentCase, SelectorScenario scenario, BenchmarkConfig config, String mode, Operation operation) {
        for (int i = 0; i < config.warmupIterations; i++) {
            consume(operation.run());
        }

        long[] samples = new long[config.measurementIterations];
        int matchCount = 0;
        for (int sample = 0; sample < config.measurementIterations; sample++) {
            long start = System.nanoTime();
            for (int op = 0; op < config.operationsPerMeasurement; op++) {
                matchCount = operation.run();
                consume(matchCount);
            }
            long elapsed = System.nanoTime() - start;
            samples[sample] = elapsed / config.operationsPerMeasurement;
        }

        return new ResultRow(documentCase, scenario, mode, matchCount, summarize(samples));
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
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(outputCsv, StandardCharsets.UTF_8))) {
            out.println("dataset,elementCount,nodeCount,scenario,query,mode,matchCount,avgMicros,p95Micros,opsPerSecond");
            for (ResultRow row : results) {
                out.printf(Locale.ROOT, "%s,%d,%d,%s,%s,%s,%d,%.2f,%.2f,%.2f%n",
                    csv(row.documentCase.name),
                    row.documentCase.elementCount,
                    row.documentCase.nodeCount,
                    csv(row.scenario.name),
                    csv(row.scenario.query),
                    csv(row.mode),
                    row.matchCount,
                    nanosToMicros(row.measurement.averageNanos),
                    nanosToMicros(row.measurement.p95Nanos),
                    row.measurement.opsPerSecond);
            }
        }
    }

    private static double nanosToMicros(double nanos) {
        return nanos / 1_000d;
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static void consume(int value) {
        sink = 31 * sink + value;
    }

    private interface Operation {
        int run();
    }

    private static final class BenchmarkConfig {
        final int warmupIterations;
        final int measurementIterations;
        final int operationsPerMeasurement;
        final int syntheticSections;
        final int syntheticCardsPerSection;
        final Path outputCsv;

        BenchmarkConfig(int warmupIterations, int measurementIterations, int operationsPerMeasurement, int syntheticSections, int syntheticCardsPerSection, Path outputCsv) {
            this.warmupIterations = warmupIterations;
            this.measurementIterations = measurementIterations;
            this.operationsPerMeasurement = operationsPerMeasurement;
            this.syntheticSections = syntheticSections;
            this.syntheticCardsPerSection = syntheticCardsPerSection;
            this.outputCsv = outputCsv;
        }

        static BenchmarkConfig load() {
            int warmup = Integer.getInteger("jsoup.perf.warmup", 5);
            int measurements = Integer.getInteger("jsoup.perf.measurements", 10);
            int ops = Integer.getInteger("jsoup.perf.opsPerMeasurement", 25);
            int sections = Integer.getInteger("jsoup.perf.synthetic.sections", 120);
            int cardsPerSection = Integer.getInteger("jsoup.perf.synthetic.cardsPerSection", 60);
            Path output = Paths.get(System.getProperty("jsoup.perf.output", "target/perf/selector-benchmark.csv"));
            return new BenchmarkConfig(warmup, measurements, ops, sections, cardsPerSection, output);
        }
    }

    private static final class SelectorScenario {
        final String name;
        final String query;

        SelectorScenario(String name, String query) {
            this.name = name;
            this.query = query;
        }
    }

    private static final class DocumentCase {
        final String name;
        final Document document;
        final List<SelectorScenario> scenarios;
        final int elementCount;
        final int nodeCount;

        DocumentCase(String name, Document document, List<SelectorScenario> scenarios) {
            this.name = name;
            this.document = document;
            this.scenarios = Collections.unmodifiableList(new ArrayList<>(scenarios));
            this.elementCount = document.getAllElements().size();
            this.nodeCount = document.nodeStream().mapToInt(node -> 1).sum();
        }
    }

    private static final class Measurement {
        final double averageNanos;
        final long p95Nanos;
        final double opsPerSecond;

        Measurement(double averageNanos, long p95Nanos, double opsPerSecond) {
            this.averageNanos = averageNanos;
            this.p95Nanos = p95Nanos;
            this.opsPerSecond = opsPerSecond;
        }
    }

    private static final class ResultRow {
        final DocumentCase documentCase;
        final SelectorScenario scenario;
        final String mode;
        final int matchCount;
        final Measurement measurement;

        ResultRow(DocumentCase documentCase, SelectorScenario scenario, String mode, int matchCount, Measurement measurement) {
            this.documentCase = documentCase;
            this.scenario = scenario;
            this.mode = mode;
            this.matchCount = matchCount;
            this.measurement = measurement;
        }
    }

    private static final class BenchmarkPrinter {
        private BenchmarkPrinter() {
        }

        static void printDocumentHeader(DocumentCase documentCase) {
            System.out.printf(Locale.ROOT, "%nDataset: %s (%d elements, %d nodes)%n",
                documentCase.name,
                documentCase.elementCount,
                documentCase.nodeCount);
        }

        static void printScenario(List<ResultRow> rows) {
            ResultRow baseline = rows.get(1);
            ResultRow evaluator = rows.get(2);
            double speedup = baseline.measurement.averageNanos / evaluator.measurement.averageNanos;

            System.out.printf(Locale.ROOT, "  Scenario: %s%n", rows.get(0).scenario.name);
            for (ResultRow row : rows) {
                System.out.printf(Locale.ROOT,
                    "    %-16s avg=%8s us p95=%8s us ops/s=%10s matches=%d%n",
                    row.mode,
                    NumberFormat.format(nanosToMicros(row.measurement.averageNanos)),
                    NumberFormat.format(nanosToMicros(row.measurement.p95Nanos)),
                    NumberFormat.format(row.measurement.opsPerSecond),
                    row.matchCount);
            }
            System.out.printf(Locale.ROOT, "    evaluator reuse speedup: %sx%n", NumberFormat.format(speedup));
        }

        static void printSummary(List<ResultRow> rows, Path outputCsv) {
            ResultRow strongest = null;
            double bestSpeedup = Double.NEGATIVE_INFINITY;

            for (int i = 0; i < rows.size(); i += 3) {
                ResultRow stringRow = rows.get(i + 1);
                ResultRow evaluatorRow = rows.get(i + 2);
                double speedup = stringRow.measurement.averageNanos / evaluatorRow.measurement.averageNanos;
                if (speedup > bestSpeedup) {
                    bestSpeedup = speedup;
                    strongest = evaluatorRow;
                }
            }

            System.out.printf(Locale.ROOT, "%nCSV report: %s%n", outputCsv);
            if (strongest != null) {
                System.out.printf(Locale.ROOT,
                    "Strongest selector reuse win: %s / %s at %sx%n",
                    strongest.documentCase.name,
                    strongest.scenario.name,
                    NumberFormat.format(bestSpeedup));
            }
            System.out.printf(Locale.ROOT, "Blackhole: %d%n", sink);
        }
    }
}