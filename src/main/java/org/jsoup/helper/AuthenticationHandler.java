package org.jsoup.helper;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;

/**
 Handles per request Authenticator-based authentication. Loads the class `org.jsoup.helper.RequestAuthHandler` if
 per-request Authenticators are supported (Java 9+), or installs a system-wide Authenticator that delegates to a request
 ThreadLocal.
 */
class AuthenticationHandler extends Authenticator {
    static final int MaxAttempts = 5; // max authentication attempts per request. allows for multiple auths (e.g. proxy and server) in one request, but saves otherwise 20 requests if credentials are incorrect.
    static AuthShim handler;

    static {
        try {
            //noinspection unchecked
            Class<AuthShim> perRequestClass = (Class<AuthShim>) Class.forName("org.jsoup.helper.RequestAuthHandler");
            Constructor<AuthShim> constructor = perRequestClass.getConstructor();
            handler = constructor.newInstance();
        } catch (ClassNotFoundException e) {
            handler = new GlobalHandler();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Nullable RequestAuthenticator auth;
    int attemptCount = 0;

    AuthenticationHandler() {}

    AuthenticationHandler(RequestAuthenticator auth) {
        this.auth = auth;
    }

    /**
     Authentication callback, called by HttpURLConnection - either as system-wide default (Java 8) or per HttpURLConnection (Java 9+)
     * @return credentials, or null if not attempting to auth.
     */
    @Nullable @Override public final PasswordAuthentication getPasswordAuthentication() {
        AuthenticationHandler delegate = handler.get(this);
        if (delegate == null) return null; // this request has no auth handler
        delegate.attemptCount++;
        // if the password returned fails, Java will repeatedly retry the request with a new password auth hit (because
        // it may be an interactive prompt, and the user could eventually get it right). But in Jsoup's context, the
        // auth will either be correct or not, so just abandon
        if (delegate.attemptCount > MaxAttempts)
            return null;
        if (delegate.auth == null)
            return null; // detached - would have been the Global Authenticator (not a delegate)

        RequestAuthenticator.Context ctx = new RequestAuthenticator.Context(
            this.getRequestingURL(), this.getRequestorType(), this.getRequestingPrompt());
        return delegate.auth.authenticate(ctx);
    }

    interface AuthShim {
        void enable(RequestAuthenticator auth, HttpURLConnection con);

        void remove();

        @Nullable AuthenticationHandler get(AuthenticationHandler helper);
    }

    /**
     On Java 8 we install a system-wide Authenticator, which pulls the delegating Auth from a ThreadLocal pool.
     */
    static class GlobalHandler implements AuthShim {
        static ThreadLocal<AuthenticationHandler> authenticators = new ThreadLocal<>();
        static {
            Authenticator.setDefault(new AuthenticationHandler());
        }

        @Override public void enable(RequestAuthenticator auth, HttpURLConnection con) {
            authenticators.set(new AuthenticationHandler(auth));
        }

        @Override public void remove() {
            authenticators.remove();
        }

        @Override public AuthenticationHandler get(AuthenticationHandler helper) {
            return authenticators.get();
        }
    }
}
