package org.jsoup;

import org.jsoup.nodes.Document;

import java.net.URL;
import java.util.Map;
import java.util.Collection;

/**
 * DRAFT interface to support HTTP connections.
 */
public interface Connection {
    public enum Method {
        GET, POST
    }

    public Connection url(URL url);

    public Connection url(String url);

    public Connection userAgent(String userAgent);

    public Connection timeout(int seconds);

    public Connection referrer(String referrer);

    public Connection method(Method method);

    public Connection data(String key, String value);

    public Connection data(Map<String, String> data);

    public Connection data(String... keyvals);

    public Connection header(String name, String value);

    public Connection cookie(String name, String value);

    public Document get();

    public Document post();

    public Response execute();

    public Request request();

    public Connection request(Request request);

    public Response response();

    public Connection response(Response response);


    interface Base<T extends Base> { // todo: better name for request / response base.

        public URL url();

        public T url(URL url);

        public Method method();

        public T method(Method method);

        public String header(String name);

        public T header(String name, String value);

        public boolean hasHeader(String name);

        public T removeHeader(String name);

        public Map<String, String> headers();

        public String cookie(String name);

        public T cookie(String name, String value);

        public boolean hasCookie(String name);

        public T removeCookie(String name);

        public Map<String, String> cookies();

    }

    public interface Request extends Base<Request> {
        public int timeout();

        public Request timeout(int seconds);

        public Request data(KeyVal keyval);

        public Collection<KeyVal> data();

    }

    public interface Response extends Base<Response> {
        public int statusCode();

        public String body();

        public byte[] bodyAsBytes();
    }

    public interface KeyVal {
        public KeyVal key(String key);
        public String key();

        public KeyVal value(String value);
        public String value();
    }
}

