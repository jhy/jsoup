package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collection;
import java.util.ArrayList;

/**
 * DRAFT implementation of Connection.
 */
public class HttpConnection implements Connection {
    private Connection.Request req;
    private Connection.Response res;

    private HttpConnection() {
        req = new Request();
        res = new Response();
    }

    public Connection url(URL url) {
        req.url(url);
        return this;
    }

    public Connection url(String url) {
        try {
            req.url(new URL(url));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL: " + url, e);
        }
        return this;
    }

    public Connection userAgent(String userAgent) {
        req.header("User-Agent", userAgent);
        return this;
    }

    public Connection timeout(int seconds) {
        req.timeout(seconds);
        return this;
    }

    public Connection referrer(String referrer) {
        req.header("Referer", referrer);
        return this;
    }

    public Connection method(Method method) {
        req.method(method);
        return this;
    }

    public Connection data(String key, String value) {
        req.data(KeyVal.create(key, value));
        return this;
    }

    public Connection data(Map<String, String> data) {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            req.data(KeyVal.create(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    public Connection data(String... keyvals) {
        for (int i = 0; i < keyvals.length; i+=2) {
            req.data(KeyVal.create(keyvals[i], keyvals[i+1]));
        }
        return this;
    }

    public Connection header(String name, String value) {
        req.header(name, value);
        return this;
    }

    public Connection cookie(String name, String value) {
        req.cookie(name, value);
        return this;
    }

    public Document get() {
        req.method(Method.GET);
        execute();
        // todo: parse for doc
        return null;
    }

    public Document post() {
        req.method(Method.POST);
        execute();
        // todo: parse for doc
        return null;
    }

    public Connection.Response execute() {
        // todo: execute
        return res;
    }

    public Connection.Request request() {
        return req;
    }

    public Connection request(Connection.Request request) {
        req = request;
        return this;
    }

    public Connection.Response response() {
        return res;
    }

    public Connection response(Connection.Response response) {
        res = response;
        return this;
    }

    @SuppressWarnings({"unchecked"})
    private static abstract class Base<T extends Connection.Base> implements Connection.Base<T> {
        private URL url;
        private Method method;
        private Map<String, String> headers;
        private Map<String, String> cookies;

        private Base() {
            headers = new LinkedHashMap<String, String>();
            cookies = new LinkedHashMap<String, String>();
        }

        public URL url() {
            return url;
        }

        public T url(URL url) {
            this.url = url;
            return (T) this;
        }

        public Method method() {
            return method;
        }

        public T method(Method method) {
            this.method = method;
            return (T) this;
        }

        public String header(String name) {
            return headers.get(name);
        }

        public T header(String name, String value) {
            headers.put(name, value);
            return (T) this;
        }

        public boolean hasHeader(String name) {
            return headers.containsKey(name);
        }

        public T removeHeader(String name) {
            headers.remove(name);
            return (T) this;
        }

        public Map<String, String> headers() {
            return headers;
        }

        public String cookie(String name) {
            return cookies.get(name);
        }

        public T cookie(String name, String value) {
            cookies.put(name, value);
            return (T) this;
        }

        public boolean hasCookie(String name) {
            return cookies.containsKey(name);
        }

        public T removeCookie(String name) {
            cookies.remove(name);
            return (T) this;
        }

        public Map<String, String> cookies() {
            return cookies;
        }
    }

    public static class Request extends Base<Connection.Request> implements Connection.Request {
        private int timeoutSeconds;
        private Collection<Connection.KeyVal> data;

        private Request() {
            data = new ArrayList<Connection.KeyVal>();
        }

        public int timeout() {
            return timeoutSeconds;
        }

        public Request timeout(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        public Request data(Connection.KeyVal keyval) {
            data.add(keyval);
            return this;
        }

        public Collection<Connection.KeyVal> data() {
            return data;
        }
    }

    public static class Response extends Base<Connection.Response> implements Connection.Response {
        private int statusCode;

        public int statusCode() {
            return statusCode;
        }

        public String body() {
            return null;
        }

        public byte[] bodyAsBytes() {
            return new byte[0];
        }
    }

    public static class KeyVal implements Connection.KeyVal {
        private String key;
        private String value;

        public static KeyVal create(String key, String value) {
            return new KeyVal(key, value);
        }

        public KeyVal(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public KeyVal key(String key) {
            this.key = key;
            return this;
        }

        public String key() {
            return key;
        }

        public KeyVal value(String value) {
            this.value = value;
            return this;
        }

        public String value() {
            return value;
        }
    }
}
