package org.jsoup.integration;

import org.jsoup.helper.HttpClientExecutorTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

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
}
