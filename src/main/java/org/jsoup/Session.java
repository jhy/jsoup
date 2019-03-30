package org.jsoup;

import org.jsoup.helper.HttpConnection;
import org.jsoup.helper.Validate;
import org.jsoup.internal.StringUtil;

import javax.net.ssl.SSLSocketFactory;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Borrowing from <a href="http://www.python-requests.org/en/master/user/advanced/#session-objects">python-requests</a>.
 *
 * @author Fang Zhuo
 */
public class Session implements Serializable {
    //Custom extension variable
    public Map<String, Object> ext = new ConcurrentHashMap();
    private Map<String, String> cookies = new ConcurrentHashMap();
    private int timeoutMilliseconds = 30000; // 30 seconds
    private String proxyHost;
    private int proxyPort = -1;
    private Proxy.Type proxyType = Proxy.Type.HTTP;
    private boolean ignoreHttpErrors = false;
    private SSLSocketFactory sslSocketFactory;
    private boolean ignoreContentType = true;

    protected Session() {
    }

    /**
     * Creates a new {@link Connection} to a URL. Use to fetch and parse a HTML page.
     * <p>
     * Use examples:
     * <ul>
     * <li><code>Document doc = Jsoup.connect("http://example.com").userAgent("Mozilla").data("name", "jsoup").get();</code></li>
     * <li><code>Document doc = Jsoup.connect("http://example.com").cookie("auth", "token").post();</code></li>
     * </ul>
     *
     * @param url URL to connect to. The protocol must be {@code http} or {@code https}.
     * @return the connection. You can add data, cookies, and headers; set the user-agent, referrer, method; and then execute.
     */
    public Connection connect(String url) {
        return HttpConnection.connect(url, this);
    }

    /**
     * Get a cookie value by name from this Session.
     *
     * @param name name of cookie to retrieve.
     * @return value of cookie, or null if not set
     */
    public String cookie(String name) {
        return cookies.get(name);
    }

    /**
     * Set a cookie to be sent in the request.
     *
     * @param name name of cookie
     * @param val  value of cookie
     * @return this Connection, for chaining
     */
    public Session cookie(String name, String val) {
        this.cookies.put(name, val);
        return this;
    }

    /**
     * Retrieve all of the Session cookies as a map
     *
     * @return cookies
     */
    public Map<String, String> cookies() {
        return cookies;
    }

    /**
     * Adds each of the supplied cookies to the request.
     *
     * @param cookies map of cookie name {@literal ->} value pairs
     * @return this Session, for chaining
     */
    public Session cookies(Map<String, String> cookies) {
        this.cookies.putAll(cookies);
        return this;
    }

    /**
     * Set the proxy to use for this request. Set to <code>null</code> to disable.
     *
     * @param proxy proxy to use
     * @return this Session, for chaining
     */
    public Session proxy(Proxy proxy) {
        InetSocketAddress address = (InetSocketAddress) proxy.address();
        this.proxyHost = address.getHostName();
        this.proxyPort = address.getPort();
        this.proxyType = proxy.type();
        return this;
    }

    /**
     * Set the HTTP proxy to use for this request.
     *
     * @param host the proxy hostname
     * @param port the proxy port
     * @return this Session, for chaining
     */
    public Session proxy(String host, int port) {
        this.proxyHost = host;
        this.proxyPort = port;
        this.proxyType = Proxy.Type.HTTP;
        return this;
    }

    /**
     * Get the proxy used for this request.
     *
     * @return the proxy; <code>null</code> if not enabled.
     */
    public Proxy proxy() {
        if (proxyPort > 0 && !StringUtil.isBlank(proxyHost)) {
            return new Proxy(this.proxyType, InetSocketAddress.createUnresolved(proxyHost, proxyPort));
        }
        return null;
    }

    /**
     * Get the request timeout, in milliseconds.
     *
     * @return the timeout in milliseconds.
     */
    public int timeout() {
        return timeoutMilliseconds;
    }

    /**
     * Update the request timeout.
     *
     * @param millis timeout, in milliseconds
     * @return this Request, for chaining
     */
    public Session timeout(int millis) {
        Validate.isTrue(millis >= 0, "Timeout milliseconds must be 0 (infinite) or greater");
        timeoutMilliseconds = millis;
        return this;
    }

    /**
     * Get the current ignoreHttpErrors configuration.
     *
     * @return true if errors will be ignored; false (default) if HTTP errors will cause an IOException to be
     * thrown.
     */
    public boolean ignoreHttpErrors() {
        return ignoreHttpErrors;
    }

    /**
     * Configures the request to ignore HTTP errors in the response.
     *
     * @param ignoreHttpErrors set to true to ignore HTTP errors.
     * @return this Request, for chaining
     */
    public Session ignoreHttpErrors(boolean ignoreHttpErrors) {
        this.ignoreHttpErrors = ignoreHttpErrors;
        return this;
    }

    /**
     * Get the current custom SSL socket factory, if any.
     *
     * @return custom SSL socket factory if set, null otherwise
     */
    public SSLSocketFactory sslSocketFactory() {
        return sslSocketFactory;
    }

    /**
     * Set a custom SSL socket factory.
     *
     * @param sslSocketFactory SSL socket factory
     */
    public Session sslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
        return this;
    }

    /**
     * Get the current ignoreContentType configuration.
     *
     * @return true if invalid content-types will be ignored; false (default) if they will cause an IOException to
     * be thrown.
     */
    public boolean ignoreContentType() {
        return ignoreContentType;
    }

    /**
     * Configures the request to ignore the Content-Type of the response.
     *
     * @param ignoreContentType set to true to ignore the content type.
     * @return this Request, for chaining
     */
    public Session ignoreContentType(boolean ignoreContentType) {
        this.ignoreContentType = ignoreContentType;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Session session = (Session) o;

        if (timeoutMilliseconds != session.timeoutMilliseconds) return false;
        if (proxyPort != session.proxyPort) return false;
        if (ignoreHttpErrors != session.ignoreHttpErrors) return false;
        if (ignoreContentType != session.ignoreContentType) return false;
        if (ext != null ? !ext.equals(session.ext) : session.ext != null) return false;
        if (cookies != null ? !cookies.equals(session.cookies) : session.cookies != null) return false;
        if (proxyHost != null ? !proxyHost.equals(session.proxyHost) : session.proxyHost != null) return false;
        if (proxyType != session.proxyType) return false;
        return sslSocketFactory != null ? sslSocketFactory.equals(session.sslSocketFactory) : session.sslSocketFactory == null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ext, cookies, timeoutMilliseconds, proxyHost, proxyPort, proxyType, ignoreHttpErrors, sslSocketFactory, ignoreContentType);
    }

    @Override
    public String toString() {
        return "Session{" +
                "ext=" + ext +
                ", cookies=" + cookies +
                ", timeoutMilliseconds=" + timeoutMilliseconds +
                ", proxyHost='" + proxyHost + '\'' +
                ", proxyPort=" + proxyPort +
                ", proxyType=" + proxyType +
                ", ignoreHttpErrors=" + ignoreHttpErrors +
                ", sslSocketFactory=" + sslSocketFactory +
                ", ignoreContentType=" + ignoreContentType +
                '}';
    }
}
