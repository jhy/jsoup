package org.jsoup.helper;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

/**
 Handles per request Authenticator-based authentication. Loads the class `org.jsoup.helper.RequestAuthHandler` if
 per-request Authenticators are supported (Java 9+), or installs a system-wide Authenticator that delegates to a request
 ThreadLocal.
 */
//{(Complex method False negative(Implementation smell)) (Used Extract Method in this (Done))
class AuthenticationHandler extends Authenticator {
    static final int MaxAttempts = 3; // Renamed for clarity
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
     * Authentication callback, called by HttpURLConnection - either as system-wide default (Java 8) or per HttpURLConnection (Java 9+)
     * @return credentials, or null if not attempting to authenticate.
     */
    @Nullable
    @Override
    public final PasswordAuthentication getPasswordAuthentication() {
        AuthenticationHandler delegate = handler.get(this);
        if (delegate == null) return null;

        if (!shouldAuthenticate(delegate)) {
            return null;
        }

        RequestAuthenticator.Context ctx = new RequestAuthenticator.Context(
                this.getRequestingURL(), this.getRequestorType(), this.getRequestingPrompt());
        return delegate.auth.authenticate(ctx);
    }

    /**
     * Extracted method to check authentication conditions.
     */
    private boolean shouldAuthenticate(AuthenticationHandler delegate) {
        delegate.attemptCount++;
        if (delegate.attemptCount > MaxAttempts) {
            return false;
        }
        return delegate.auth != null;
    }
}



    interface AuthShim {
        void enable(RequestAuthenticator auth, Object connOrHttp);

        void remove();

        @Nullable AuthenticationHandler get(AuthenticationHandler helper);
    }

    /**
     On Java 8 we install a system-wide Authenticator, which pulls the delegating Auth from a ThreadLocal pool.
     */
    class GlobalHandler implements AuthShim {
        static ThreadLocal<AuthenticationHandler> authenticators = new ThreadLocal<>();
        static {
            Authenticator.setDefault(new AuthenticationHandler());
        }

        @Override public void enable(RequestAuthenticator auth, Object ignored) {
            authenticators.set(new AuthenticationHandler(auth));
        }

        @Override public void remove() {
            authenticators.remove();
        }

        @Override public AuthenticationHandler get(AuthenticationHandler helper) {
            return authenticators.get();
        }
    }

