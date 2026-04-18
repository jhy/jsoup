package org.jsoup.integration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 Holds the shared authentication constants and checks used by the Netty test harness
 */
public final class TestAuth {
    public static final String WantsServerAuthentication = "X-Wants-ServerAuthentication";
    public static final String ServerUser = "admin";
    public static final String ServerRealm = "jsoup test server authentication realm";
    private static volatile String ServerPassword = newServerPassword();

    public static final String WantsProxyAuthentication = "X-Wants-ProxyAuthentication";
    public static final String ProxyUser = "foxyproxy";
    public static final String ProxyRealm = "jsoup test proxy authentication realm";
    private static volatile String ProxyPassword = newProxyPassword();

    private TestAuth() {
    }

    private static String newPassword() {
        return "pass-" + Math.random();
    }

    // passwords get rotated in tests so that Java's auth cache is invalidated and a new auth callback occurs.
    // requires tests hitting these are called serially.
    public static String newServerPassword() {
        return ServerPassword = newPassword() + "-server";
    }

    public static String newProxyPassword() {
        return ProxyPassword = newPassword() + "-proxy";
    }

    public static String wantsHeader(boolean forProxy) {
        return forProxy ? WantsProxyAuthentication : WantsServerAuthentication;
    }

    public static String authorizationHeader(boolean forProxy) {
        return forProxy ? "Proxy-Authorization" : "Authorization";
    }

    public static String challengeHeader(boolean forProxy) {
        return forProxy ? "Proxy-Authenticate" : "WWW-Authenticate";
    }

    public static String realm(boolean forProxy) {
        return forProxy ? ProxyRealm : ServerRealm;
    }

    public static int failureStatus(boolean forProxy) {
        return forProxy ? 407 : 401;
    }

    public static String challengeValue(boolean forProxy) {
        return "Basic realm=\"" + realm(forProxy) + "\"";
    }

    public static boolean checkAuth(boolean alwaysWantsAuth, boolean forProxy, String wantsHeaderValue, String authHeader) {
        if (alwaysWantsAuth || wantsHeaderValue != null) {
            return hasExpectedAuth(forProxy, authHeader);
        }
        return true;
    }

    private static boolean hasExpectedAuth(boolean forProxy, String authHeader) {
        if (authHeader != null) {
            int space = authHeader.indexOf(' ');
            if (space > 0) {
                String value = authHeader.substring(space + 1);
                String expected = forProxy ?
                    (ProxyUser + ":" + ProxyPassword) :
                    (ServerUser + ":" + ServerPassword);
                String base64 = Base64.getEncoder().encodeToString(expected.getBytes(StandardCharsets.UTF_8));
                return base64.equals(value);
            }
        }
        return false;
    }
}
