package org.jsoup.benchmarks;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@Disabled
public class JMHBenchmark {
    private static final String CONTENT_SMALL = "<p>small</p>";
    private static final String CONTENT_MEDIUM = "<html> \n" +
        " <head> \n" +
        "  <title>Large HTML</title> \n" +
        " </head> \n" +
        " <body> \n" +
        "  <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer nec odio. Praesent libero. Sed cursus ante dapibus diam. Sed nisi. Nulla quis sem at nibh elementum imperdiet. Duis sagittis ipsum. Praesent mauris. Fusce nec tellus sed augue semper porta. Mauris massa. Vestibulum lacinia arcu eget nulla. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Curabitur sodales ligula in libero.</p>\n" +
        "  <p>Sed dignissim lacinia nunc. Curabitur tortor. Pellentesque nibh. Aenean quam. In scelerisque sem at dolor. Maecenas mattis. Sed convallis tristique sem. Proin ut ligula vel nunc egestas porttitor. <i>Lorem ipsum dolor sit amet, consectetur adipiscing elit</i>. Morbi lectus risus, iaculis vel, suscipit quis, luctus non, massa. Fusce ac turpis quis ligula lacinia aliquet. Mauris ipsum. <b>Aenean quam</b>. Nulla metus metus, ullamcorper vel, tincidunt sed, euismod in, nibh. </p> \n" +
        "  <p>Quisque volutpat condimentum velit. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nam nec ante. Sed lacinia, urna non tincidunt mattis, tortor neque adipiscing diam, a cursus ipsum ante quis turpis. Nulla facilisi. Ut fringilla. Suspendisse potenti. Nunc feugiat mi a tellus consequat imperdiet. Vestibulum sapien. Proin quam. <b>Sed lacinia, urna non tincidunt mattis, tortor neque adipiscing diam, a cursus ipsum ante quis turpis</b>. Etiam ultrices. Suspendisse in justo eu magna luctus suscipit. Sed lectus. Integer euismod lacus luctus magna.</p>\n" +
        "  <p>Quisque cursus, metus vitae pharetra auctor, sem massa mattis sem, at interdum magna augue eget diam. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Morbi lacinia molestie dui. Praesent blandit dolor. Sed non quam. In vel mi sit amet augue congue elementum. Morbi in ipsum sit amet pede facilisis laoreet. <b>Suspendisse in justo eu magna luctus suscipit</b>. Donec lacus nunc, viverra nec, blandit vel, egestas et, augue. Vestibulum tincidunt malesuada tellus. Ut ultrices ultrices enim. Curabitur sit amet mauris. Morbi in dui quis est pulvinar ullamcorper. Nulla facilisi.</p>\n" +
        "  <p>Integer lacinia sollicitudin massa. <b>Quisque cursus, metus vitae pharetra auctor, sem massa mattis sem, at interdum magna augue eget diam</b>. Cras metus. Sed aliquet risus a tortor. Integer id quam. Morbi mi. <b>Morbi in dui quis est pulvinar ullamcorper</b>. Quisque nisl felis, venenatis tristique, dignissim in, ultrices sit amet, augue. Proin sodales libero eget ante. Nulla quam. Aenean laoreet. Vestibulum nisi lectus, commodo ac, facilisis ac, ultricies eu, pede. Ut orci risus, accumsan porttitor, cursus quis, aliquet eget, justo. Sed pretium blandit orci. <b>Quisque nisl felis, venenatis tristique, dignissim in, ultrices sit amet, augue</b>. Ut eu diam at pede suscipit sodales. Aenean lectus elit, fermentum non, convallis id, sagittis at, neque.</p>\n" +
        "  <p>Nullam mauris orci, aliquet et, iaculis et, viverra vitae, ligula. Nulla ut felis in purus aliquam imperdiet. Maecenas aliquet mollis lectus. Vivamus consectetuer risus et tortor. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer nec odio. Praesent libero. Sed cursus ante dapibus diam. Sed nisi. Nulla quis sem at nibh elementum imperdiet. Duis sagittis ipsum.</p>\n" +
        "  <p><i>Proin sodales libero eget ante</i>. Praesent mauris. Fusce nec tellus sed augue semper porta. <i>Nullam mauris orci, aliquet et, iaculis et, viverra vitae, ligula</i>. Mauris massa. Vestibulum lacinia arcu eget nulla. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Curabitur sodales ligula in libero. Sed dignissim lacinia nunc. Curabitur tortor. Pellentesque nibh. Aenean quam. In scelerisque sem at dolor. Maecenas mattis.</p>\n" +
        "  <p>Sed convallis tristique sem. Proin ut ligula vel nunc egestas porttitor. Morbi lectus risus, iaculis vel, suscipit quis, luctus non, massa. Fusce ac turpis quis ligula lacinia aliquet. Mauris ipsum. Nulla metus metus, ullamcorper vel, tincidunt sed, euismod in, nibh. Quisque volutpat condimentum velit. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. <b>Sed convallis tristique sem</b>. Nam nec ante. Sed lacinia, urna non tincidunt mattis, tortor neque adipiscing diam, a cursus ipsum ante quis turpis. Nulla facilisi. Ut fringilla. Suspendisse potenti. Nunc feugiat mi a tellus consequat imperdiet.</p>\n" +
        "  <p>Vestibulum sapien. Proin quam. <i>Fusce ac turpis quis ligula lacinia aliquet</i>. Etiam ultrices. Suspendisse in justo eu magna luctus suscipit. Sed lectus. Integer euismod lacus luctus magna. Quisque cursus, metus vitae pharetra auctor, sem massa mattis sem, at interdum magna augue eget diam. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Morbi lacinia molestie dui. <b>Suspendisse potenti</b>. Praesent blandit dolor. Sed non quam. In vel mi sit amet augue congue elementum. Morbi in ipsum sit amet pede facilisis laoreet. Donec lacus nunc, viverra nec, blandit vel, egestas et, augue.</p>\n" +
        "  <p>Vestibulum tincidunt malesuada tellus. Ut ultrices ultrices enim. Curabitur sit amet mauris. Morbi in dui quis est pulvinar ullamcorper. Nulla facilisi. <b>Quisque cursus, metus vitae pharetra auctor, sem massa mattis sem, at interdum magna augue eget diam</b>. Integer lacinia sollicitudin massa. Cras metus. Sed aliquet risus a tortor. Integer id quam. Morbi mi. Quisque nisl felis, venenatis tristique, dignissim in, ultrices sit amet, augue. Proin sodales libero eget ante. Nulla quam.</p>\n" +
        "  <p>Aenean laoreet. Vestibulum nisi lectus, commodo ac, facilisis ac, ultricies eu, pede. Ut orci risus, accumsan porttitor, cursus quis, aliquet eget, justo. Sed pretium blandit orci. Ut eu diam at pede suscipit sodales. Aenean lectus elit, fermentum non, convallis id, sagittis at, neque. Nullam mauris orci, aliquet et, iaculis et, viverra vitae, ligula. Nulla ut felis in purus aliquam imperdiet. Maecenas aliquet mollis lectus. Vivamus consectetuer risus et tortor. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer nec odio.</p>\n" +
        " </body>\n" +
        "</html>";
    private static final byte[] CONTENT_MEDIUM_AS_BYTES = CONTENT_MEDIUM.getBytes(StandardCharsets.UTF_8);

    @Test
    public void launchBenchmark() throws Exception {
        Options opt = new OptionsBuilder()
            .include(this.getClass().getName() + ".*")
            .mode(Mode.AverageTime)
            .addProfiler(GCProfiler.class)
            .timeUnit(TimeUnit.MICROSECONDS)
            .warmupTime(TimeValue.seconds(5))
            .warmupIterations(3)
            .measurementTime(TimeValue.seconds(5))
            .measurementIterations(5)
            .threads(1)
            .forks(1)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .build();

        new Runner(opt).run();
    }

    @Benchmark
    public void benchmarkSmall(Blackhole bh) throws Exception{
        bh.consume(Jsoup.parse(CONTENT_SMALL));
    }

    @Benchmark
    public void benchmarkMediumInputStream(Blackhole bh) throws Exception{
        bh.consume(Jsoup.parse(new ByteArrayInputStream(CONTENT_MEDIUM_AS_BYTES), StandardCharsets.UTF_8.name(), ""));
    }

    @Benchmark
    public void benchmarkMedium(Blackhole bh) throws Exception{
        bh.consume(Jsoup.parse(CONTENT_MEDIUM));
    }
}
