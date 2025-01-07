package org.jsoup.helper;

import java.net.HttpURLConnection;
import java.net.http.HttpClient;

/**
 A per-request authentication shim, used in Java 9+.
 */
class RequestAuthHandler implements AuthenticationHandler.AuthShim {
    public RequestAuthHandler() {}

    @Override public void enable(RequestAuthenticator auth, Object connOrHttp) {
        AuthenticationHandler authenticator = new AuthenticationHandler(auth);

        // this is a bit ugly, but a simple way to support setting authentication on both urlconnection and httpclient without more multi-version shims
        if (connOrHttp instanceof HttpURLConnection) {
            HttpURLConnection conn = (HttpURLConnection) connOrHttp;
            conn.setAuthenticator(authenticator);
        } else if (connOrHttp instanceof HttpClient.Builder) {
            HttpClient.Builder builder = (HttpClient.Builder) connOrHttp;
            builder.authenticator(authenticator);
        } else {
            throw new IllegalArgumentException("Unsupported executor: " + connOrHttp.getClass().getName());
        }
    }

    @Override public void remove() {
        // noop; would remove thread-local in Global Handler
    }

    @Override public AuthenticationHandler get(AuthenticationHandler helper) {
        // would get thread-local in Global Handler
        return helper;
    }
}
