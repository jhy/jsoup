package org.jsoup.helper;

import org.jsoup.Connection;
import org.jspecify.annotations.Nullable;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;

/**
 A {@code RequestAuthenticator} is used in {@link Connection} to authenticate if required to proxies and web
 servers. See {@link Connection#auth(RequestAuthenticator)}.
 */
@FunctionalInterface
public interface RequestAuthenticator {

    /**
     Provide authentication credentials for the provided Request Context.
     * @param auth the request context including URL, type (Server or Proxy), and realm.
     * @return credentials for the request. May return {@code null} if they are not applicable -- but the request will
     * likely fail, as this method is only called if the request asked for authentication.
     */
    @Nullable
    PasswordAuthentication authenticate(Context auth);

    /**
     Provides details for the request, to determine the appropriate credentials to return.
     */
    class Context {
        private final URL url;
        private final Authenticator.RequestorType type;
        private final String realm;

        Context(URL url, Authenticator.RequestorType type, String realm) {
            this.url = url;
            this.type = type;
            this.realm = realm;
        }

        /**
         Get he URL that is being requested.
         * @return URL
         */
        public URL url() {
            return url;
        }

        /**
         Get the requestor type: {@link Authenticator.RequestorType#PROXY PROXY} if a proxy is requesting
         authentication, or {@link Authenticator.RequestorType#SERVER SERVER} if the URL's server is requesting.
         * @return requestor type
         */
        public Authenticator.RequestorType type() {
            return type;
        }

        /**
         Get the realm of the authentication request.
         * @return realm of the authentication request
         */
        public String realm() {
            return realm;
        }

        /**
         Gets if the authentication request is for a proxy.
         * @return true if type==proxy.
         */
        public boolean isProxy() {
            return type == Authenticator.RequestorType.PROXY;
        }

        /**
         Gets if the authentication request is for a server.
         * @return true if type==server.
         */
        public boolean isServer() {
            return type == Authenticator.RequestorType.SERVER;
        }

        /**
         Helper method to return a PasswordAuthentication object.
         * @param username username credential
         * @param password password credential
         * @return a constructed PasswordAuthentication
         */
        public PasswordAuthentication credentials(String username, String password) {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }

}
