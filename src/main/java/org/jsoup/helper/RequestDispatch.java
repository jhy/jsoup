package org.jsoup.helper;

import org.jsoup.internal.SharedConstants;
import org.jspecify.annotations.Nullable;

import static org.jsoup.helper.HttpConnection.Request;
import static org.jsoup.helper.HttpConnection.Response;

import java.lang.reflect.Constructor;

/**
 Handles requests using either HttpClient (available in JDK 11+) or HttpURLConnection. During initialization, the
 HttpClientExecutor class is used if it can be instantiated, unless the system property
 {@link SharedConstants#UseHttpClient} is explicitly set to {@code false}.
 */
class RequestDispatch {

    @Nullable
    static Constructor<RequestExecutor> clientConstructor;

    static {
        try {
            //noinspection unchecked
            Class<RequestExecutor> httpClass =
                (Class<RequestExecutor>) Class.forName("org.jsoup.helper.HttpClientExecutor");
            clientConstructor = httpClass.getConstructor(Request.class, Response.class);
        } catch (Exception ignored) {
            // either not on Java11+, or on Android; will provide UrlConnectionExecutor
        }

    }

    static RequestExecutor get(Request request, @Nullable Response previousResponse) {
        boolean useHttpClient = Boolean.parseBoolean(System.getProperty(SharedConstants.UseHttpClient, "true"));
        if (useHttpClient && clientConstructor != null) {
            try {
                return clientConstructor.newInstance(request, previousResponse);
            } catch (Exception e) {
                return new UrlConnectionExecutor(request, previousResponse);
            }
        } else {
            return new UrlConnectionExecutor(request, previousResponse);
        }
    }
}
