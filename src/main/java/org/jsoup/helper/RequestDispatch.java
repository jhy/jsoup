package org.jsoup.helper;

import org.jsoup.internal.SharedConstants;
import org.jspecify.annotations.Nullable;

import static org.jsoup.helper.HttpConnection.Request;
import static org.jsoup.helper.HttpConnection.Response;

import java.lang.reflect.Constructor;

/**
 Dispatches requests to either HttpClient (JDK 11+) or HttpURLConnection implementations. At startup, if we
 can instantiate the HttpClientExecutor class, requests will use that if the system property
 {@link SharedConstants#UseHttpClient} is set to {@code true}.
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
        if (Boolean.getBoolean(SharedConstants.UseHttpClient) && clientConstructor != null) {
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
