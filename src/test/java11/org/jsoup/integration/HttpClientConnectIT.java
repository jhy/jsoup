package org.jsoup.integration;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpClientExecutorTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.jsoup.integration.TestServer.ProxyVia;
import static org.jsoup.integration.TestServer.origin;
import static org.jsoup.integration.TestServer.proxySettings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class HttpClientConnectIT extends ConnectIT {
    @BeforeAll
    static void useHttpClient() {
        HttpClientExecutorTest.enableHttpClient();
    }

    @AfterAll
    static void resetClient() {
        HttpClientExecutorTest.disableHttpClient();
    }

    @Override @Disabled
    public void canInterruptBodyStringRead() throws InterruptedException {
        // noop; can't interrupt the client via the calling thread; probably not required as timeouts are robust
    }

    @Override @Disabled
    public void canInterruptDocumentRead() throws InterruptedException {
    }

    @Override @Disabled
    public void canInterruptThenJoinASpawnedThread() throws InterruptedException {
    }

    @Test public void concurrentRequestsKeepProxyRoutesIsolated() throws Exception {
        int requestCount = 32;
        TestServer.ProxySettings proxy = proxySettings();
        String url = origin().hello.url();
        ExecutorService callers = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(requestCount);
        try {
            // mix direct and proxied requests to verify that sharing the client does not share proxy settings
            for (int i = 0; i < requestCount; i++) {
                boolean viaProxy = i % 2 == 0;
                futures.add(callers.submit(() -> {
                    ready.countDown();
                    start.await();
                    Connection connection = Jsoup.connect(url);
                    if (viaProxy) connection.proxy(proxy.hostname, proxy.port);
                    Connection.Response response = connection.execute();
                    response.parse();
                    if (viaProxy) assertEquals(ProxyVia, response.header("Via"));
                    else assertNull(response.header("Via"));
                    return null;
                }));
            }
            ready.await();
            start.countDown();
            for (Future<?> future : futures) future.get();
        } finally {
            callers.shutdownNow();
        }
    }
}
