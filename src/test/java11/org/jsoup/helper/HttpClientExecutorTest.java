package org.jsoup.helper;
import org.jsoup.internal.SharedConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class HttpClientExecutorTest {
    @Test void getsHttpClient() {
        try {
            enableHttpClient();
            RequestExecutor executor = RequestDispatch.get(new HttpConnection.Request(), null);
            assertInstanceOf(HttpClientExecutor.class, executor);
        } finally {
            disableHttpClient(); // reset to previous default for JDK8 compat tests
        }
    }

    @Test void getsHttpUrlConnectionByDefault() {
        System.clearProperty(SharedConstants.UseHttpClient);
        RequestExecutor executor = RequestDispatch.get(new HttpConnection.Request(), null);
        assertInstanceOf(HttpClientExecutor.class, executor);
    }

    public static void enableHttpClient() {
        System.setProperty(SharedConstants.UseHttpClient, "true");
    }

    public static void disableHttpClient() {
        System.setProperty(SharedConstants.UseHttpClient, "false");
    }
}
