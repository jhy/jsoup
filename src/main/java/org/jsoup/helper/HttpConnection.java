package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.parser.TokenQueue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** DRAFT implementation of Connection. */
public class HttpConnection implements Connection {
    private static final Pattern charsetPattern = Pattern.compile("(?i)\\bcharset=([^\\s;]*)");

    public static Connection connect(String url) {
        Connection con = new HttpConnection();
        con.url(url);
        return con;
    }

    public static Connection connect(URL url) {
        Connection con = new HttpConnection();
        con.url(url);
        return con;
    }

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
        for (int i = 0; i < keyvals.length; i += 2) {
            req.data(KeyVal.create(keyvals[i], keyvals[i + 1]));
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

    public Document get() throws IOException {
        req.method(Method.GET);
        execute();
        return res.parse();
    }

    public Document post() throws IOException {
        req.method(Method.POST);
        execute();
        return res.parse();
    }

    public Connection.Response execute() throws IOException {
        res = Response.execute(req);
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
        URL url;
        Method method;
        Map<String, String> headers;
        Map<String, String> cookies;

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
            method = Connection.Method.GET;
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
        private String statusMessage;
        private ByteBuffer byteData;
        private String charset;

        static Response execute(Connection.Request req) throws IOException {
            URL url = req.url();
            String protocol = url.getProtocol();
            Validate
                .isTrue(protocol.equals("http") || protocol.equals("https"), "Only http & https protocols supported");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(req.method().name());
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(req.timeout() * 1000);
            conn.setReadTimeout(req.timeout() * 1000);
            // todo: handle get params not in url, and post params
            conn.connect();

            // todo: error handling options, allow user to get !200 without exception
            int status = conn.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK)
                throw new IOException(status + " error loading URL " + url.toString());
            Response res = new Response();
            res.setupFromConnection(conn);

            // todo: move to parse
            String contentType = conn.getContentType();
            if (contentType == null || !contentType.startsWith("text/"))
                throw new IOException(String.format("Unhandled content type \"%s\" on URL %s. Must be text/*",
                    contentType, url.toString()));

            InputStream inStream = new BufferedInputStream(conn.getInputStream());
            res.byteData = DataUtil.readToByteBuffer(inStream);
            res.charset = getCharsetFromContentType(contentType); // may be null, readInputStream deals with it
            inStream.close();

            return res;
        }

        public int statusCode() {
            return statusCode;
        }

        public String statusMessage() {
            return statusMessage;
        }

        public String charset() {
            return charset;
        }

        public Document parse() {
            Document doc = DataUtil.parseByteData(byteData, charset, url.toExternalForm());
            byteData.rewind();
            charset = doc.outputSettings().charset().name(); // update charset from meta-equiv, possibly
            return doc;
        }

        public String body() {
            // gets set from header on execute, and from meta-equiv on parse. parse may not have happened yet
            String body;
            if (charset == null)
                body = Charset.forName(DataUtil.defaultCharset).decode(byteData).toString();
            else
                body = Charset.forName(charset).decode(byteData).toString();
            byteData.rewind();
            return body;
        }

        public byte[] bodyAsBytes() {
            return byteData.array();
        }

        // set up url, method, header, cookies
        private void setupFromConnection(HttpURLConnection conn) throws IOException {
            method = Connection.Method.valueOf(conn.getRequestMethod());
            url = conn.getURL();
            statusCode = conn.getResponseCode();
            statusMessage = conn.getResponseMessage();

            Map<String, List<String>> resHeaders = conn.getHeaderFields();
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                String name = entry.getKey();
                if (name == null)
                    continue; // http/1.1 line

                List<String> values = entry.getValue();

                if (name.equals("Set-Cookie")) {
                    for (String value : values) {
                        TokenQueue cd = new TokenQueue(value);
                        String cookieName = cd.chompTo("=").trim();
                        String cookieVal = cd.consumeTo(";").trim();
                        // ignores path, date, domain, secure et al. req'd?
                        cookie(cookieName, cookieVal);
                    }
                } else { // only take the first instance of each header
                    header(name, values.get(0));
                }
            }
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

    /**
     * Parse out a charset from a content type header.
     *
     * @param contentType e.g. "text/html; charset=EUC-JP"
     * @return "EUC-JP", or null if not found. Charset is trimmed and uppercased.
     */
    private static String getCharsetFromContentType(String contentType) {
        if (contentType == null) return null;

        Matcher m = charsetPattern.matcher(contentType);
        if (m.find()) {
            return m.group(1).trim().toUpperCase();
        }
        return null;
    }
}
