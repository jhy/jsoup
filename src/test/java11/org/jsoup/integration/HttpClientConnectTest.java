package org.jsoup.integration;

import org.jsoup.helper.HttpClientExecutorTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

public class HttpClientConnectTest extends ConnectTest {
    @BeforeAll
    static void useHttpClient() {
        HttpClientExecutorTest.enableHttpClient();
    }

    @AfterAll
    static void resetClient() {
        HttpClientExecutorTest.disableHttpClient();
    }

}
