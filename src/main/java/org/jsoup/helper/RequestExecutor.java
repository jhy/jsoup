package org.jsoup.helper;

import static org.jsoup.helper.HttpConnection.Request;
import static org.jsoup.helper.HttpConnection.Response;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 A shim interface to support both HttpURLConnection and HttpClient implementations, in a multi-version jar.
 */
abstract class RequestExecutor {
    final Request req;
    final @Nullable Response prevRes;

    RequestExecutor(Request request, @Nullable Response previousResponse) {
        this.req = request;
        this.prevRes = previousResponse;
    }

    abstract Response execute() throws IOException;

    abstract InputStream responseBody() throws IOException;

    abstract void safeClose();
}
