package org.jsoup.integration.netty;

/**
 Handles one routed test request on the Netty-backed origin server
 */
public interface RouteHandler {
    /**
     Writes the response for one routed request
     */
    void handle(TestRequest request, TestResponse response) throws Exception;
}
