package org.jsoup.helper;

import java.net.HttpURLConnection;

/**
 A per-request authentication shim, used in Java 9+.
 */
class RequestAuthHandler implements AuthenticationHandler.AuthShim {
    public RequestAuthHandler() {}

    @Override public void enable(RequestAuthenticator auth, HttpURLConnection con) {
        AuthenticationHandler authenticator = new AuthenticationHandler(auth);
        con.setAuthenticator(authenticator);
    }

    @Override public void remove() {
        // noop; would remove thread-local in Global Handler
    }

    @Override public AuthenticationHandler get(AuthenticationHandler helper) {
        // would get thread-local in Global Handler
        return helper;
    }
}
