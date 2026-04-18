package org.jsoup.integration.routes;

import org.jsoup.integration.netty.TestRequest;
import org.jsoup.integration.netty.TestResponse;

/**
 Slowly, interminably writes output. For the purposes of testing timeouts and interrupts.
 */
public final class SlowRider {
    private static final String TextHtml = "text/html; charset=UTF-8";
    private static final int SleepTime = 2000;
    public static final String MaxTimeParam = "maxTime";
    public static final String IntroSizeParam = "introSize";

    private SlowRider() {
    }

    /**
     Streams a deliberately slow chunked response so timeout and interrupt handling can be exercised
     */
    public static void handle(TestRequest request, TestResponse response) {
        int maxTime = intParam(request, MaxTimeParam, -1);
        int introSize = intParam(request, IntroSizeParam, 0);
        long startTime = System.currentTimeMillis();

        response.defer();
        response.schedule(1000, () -> startRide(response, startTime, maxTime, introSize));
    }

    private static void startRide(TestResponse response, long startTime, int maxTime, int introSize) {
        if (!response.isOpen()) return;

        response.setContentType(TextHtml);
        response.setStatus(200);
        response.startChunked();
        response.writeChunk("<title>Slow Rider</title>\n");

        if (introSize != 0) {
            response.writeChunk(intro(introSize));
        }

        response.writeChunk("<p>Are you still there?</p>\n");
        schedulePing(response, startTime, maxTime);
    }

    /**
     Continues the slow stream until the client disconnects or the configured max time elapses
     */
    private static void schedulePing(TestResponse response, long startTime, int maxTime) {
        response.schedule(SleepTime, () -> {
            if (!response.isOpen()) return;

            if (maxTime > 0 && System.currentTimeMillis() > startTime + maxTime) {
                response.writeChunk("<h1>outatime</h1>\n");
                response.finish();
                return;
            }

            response.writeChunk("<p>Are you still there?</p>\n");
            schedulePing(response, startTime, maxTime);
        });
    }

    /**
     Builds a large initial body chunk so the client can get past its first buffer before blocking
     */
    private static String intro(int introSize) {
        StringBuilder s = new StringBuilder();
        while (s.length() < introSize) {
            s.append("<p>Hello and welcome to the Slow Rider!</p>\n");
        }
        return s.toString();
    }

    private static int intParam(TestRequest request, String name, int defaultValue) {
        String value = request.parameter(name);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

}
