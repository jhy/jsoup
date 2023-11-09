package org.jsoup.integration.servlets;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 A filter to test basic authenticated requests. If the request header "X-Wants-Authentication" is set, or if
 alwaysWantsAuth is enabled, the filter is invoked, and requests must send the correct user authentication details.
 */
public class AuthFilter implements Filter {
    public static final String WantsServerAuthentication = "X-Wants-ServerAuthentication";
    public static final String ServerUser = "admin";
    public static final String ServerRealm = "jsoup test server authentication realm";
    private static volatile String ServerPassword = newServerPassword();

    public static final String WantsProxyAuthentication = "X-Wants-ProxyAuthentication";
    public static final String ProxyUser = "foxyproxy";
    public static final String ProxyRealm = "jsoup test proxy authentication realm";
    private static volatile String ProxyPassword = newProxyPassword();

    private final boolean alwaysWantsAuth; // we run a particular port that always wants auth - so the CONNECT tunnels can be authed. (The Java proxy tunnel CONNECT request strips the wants-auth headers)
    private final boolean forProxy;
    private final String wantsHeader;
    private final String authorizationHeader;

    /**
     Creates an Authentication Filter with hardcoded credential expectations.
     * @param alwaysWantsAuth true if this filter should always check for authentication, regardless of the Wants Auth header
     * @param forProxy true if this wraps a Proxy and should use Proxy-Authenticate headers, credentials etc. False
     * if wrapping the web server.
     */
    public AuthFilter(boolean alwaysWantsAuth, boolean forProxy) {
        this.alwaysWantsAuth = alwaysWantsAuth;
        this.forProxy = forProxy;

        wantsHeader = forProxy ? WantsProxyAuthentication : WantsServerAuthentication;
        authorizationHeader = forProxy ? "Proxy-Authorization" : "Authorization";
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

    @Override public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        boolean accessGranted = checkAuth(req);
        if (accessGranted) {
            chain.doFilter(request, response);
            return;
        }

        // Wants but failed auth - send appropriate header:
        if (forProxy) {
            res.setHeader("Proxy-Authenticate", "Basic realm=\"" + ProxyRealm + "\"");
            // ^^ Duped in ProxyServlet for CONNECT
            res.sendError(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
        } else {
            res.setHeader("WWW-Authenticate", "Basic realm=\"" + ServerRealm + "\"");
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override public void destroy() {}

    public boolean checkAuth(HttpServletRequest req) {
        if (alwaysWantsAuth || req.getHeader(wantsHeader) != null) {
            String authHeader = req.getHeader(authorizationHeader);
            if (authHeader != null) {
                int space = authHeader.indexOf(' ');
                if (space > 0) {
                    String value = authHeader.substring(space + 1);
                    String expected = forProxy ?
                        (ProxyUser + ":" + ProxyPassword) :
                        (ServerUser + ":" + ServerPassword);
                    String base64 = Base64.getEncoder().encodeToString(expected.getBytes(StandardCharsets.UTF_8));
                    return base64.equals(value); // if passed auth
                }
            }
            return false; // unexpected header value
        }
        return true; // auth not required
    }
}
