package org.jsoup.helper;
import org.jsoup.internal.SharedConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class HttpClientExecutorTest {
    @Test void getsHttpClient() {
        try {
            enableHttpClient();
            RequestExecutor executor = RequestDispatch.get(null, null);
            //assertInstanceOf(HttpClientExecutor.class, executor);
            assertEquals("org.jsoup.helper.HttpClientExecutor", executor.getClass().getName());
            // Haven't figured out how to get Maven to allow this mjar code to be on the classpath for the surefire tests, hence not instanceof
        } finally {
            disableHttpClient(); // reset to default off (currently)
        }
    }

    @Test void getsHttpUrlConnectionByDefault() {
        RequestExecutor executor = RequestDispatch.get(null, null);
        assertInstanceOf(UrlConnectionExecutor.class, executor);
    }

    public static void enableHttpClient() {
        System.setProperty(SharedConstants.UseHttpClient, "true");
    }

    public static void disableHttpClient() {
        System.setProperty(SharedConstants.UseHttpClient, "false");
    }
}
