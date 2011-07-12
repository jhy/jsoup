package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.parser.TokenQueue;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Implementation of {@link Connection}.
 * @see org.jsoup.Jsoup#connect(String) 
 */
public class HttpConnection implements Connection {
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
        Validate.notEmpty(url, "Must supply a valid URL");
        try {
            req.url(new URL(url));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL: " + url, e);
        }
        return this;
    }

    public Connection userAgent(String userAgent) {
        Validate.notNull(userAgent, "User agent must not be null");
        req.header("User-Agent", userAgent);
        return this;
    }

    public Connection timeout(int millis) {
        req.timeout(millis);
        return this;
    }

    public Connection followRedirects(boolean followRedirects) {
        req.followRedirects(followRedirects);
        return this;
    }

    public Connection referrer(String referrer) {
        Validate.notNull(referrer, "Referrer must not be null");
        req.header("Referer", referrer);
        return this;
    }

    public Connection method(Method method) {
        req.method(method);
        return this;
    }

    public Connection ignoreHttpErrors(boolean ignoreHttpErrors) {
		req.ignoreHttpErrors(ignoreHttpErrors);
		return this;
	}

    public Connection ignoreContentType(boolean ignoreContentType) {
        req.ignoreContentType(ignoreContentType);
        return this;
    }

    public Connection data(String key, String value) {
        req.data(KeyVal.create(key, value));
        return this;
    }

    public Connection data(Map<String, String> data) {
        Validate.notNull(data, "Data map must not be null");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            req.data(KeyVal.create(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    public Connection data(String... keyvals) {
        Validate.notNull(keyvals, "Data key value pairs must not be null");
        Validate.isTrue(keyvals.length %2 == 0, "Must supply an even number of key value pairs");
        for (int i = 0; i < keyvals.length; i += 2) {
            String key = keyvals[i];
            String value = keyvals[i+1];
            Validate.notEmpty(key, "Data key must not be empty");
            Validate.notNull(value, "Data value must not be null");
            req.data(KeyVal.create(key, value));
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
            Validate.notNull(url, "URL must not be null");
            this.url = url;
            return (T) this;
        }

        public Method method() {
            return method;
        }

        public T method(Method method) {
            Validate.notNull(method, "Method must not be null");
            this.method = method;
            return (T) this;
        }

        public String header(String name) {
            Validate.notNull(name, "Header name must not be null");
            return getHeaderCaseInsensitive(name);
        }

        public T header(String name, String value) {
            Validate.notEmpty(name, "Header name must not be empty");
            Validate.notNull(value, "Header value must not be null");
            removeHeader(name); // ensures we don't get an "accept-encoding" and a "Accept-Encoding"
            headers.put(name, value);
            return (T) this;
        }

        public boolean hasHeader(String name) {
            Validate.notEmpty(name, "Header name must not be empty");
            return getHeaderCaseInsensitive(name) != null;
        }

        public T removeHeader(String name) {
            Validate.notEmpty(name, "Header name must not be empty");
            Map.Entry<String, String> entry = scanHeaders(name); // remove is case insensitive too
            if (entry != null)
                headers.remove(entry.getKey()); // ensures correct case
            return (T) this;
        }

        public Map<String, String> headers() {
            return headers;
        }

        private String getHeaderCaseInsensitive(String name) {
            Validate.notNull(name, "Header name must not be null");
            // quick evals for common case of title case, lower case, then scan for mixed
            String value = headers.get(name);
            if (value == null)
                value = headers.get(name.toLowerCase());
            if (value == null) {
                Map.Entry<String, String> entry = scanHeaders(name);
                if (entry != null)
                    value = entry.getValue();
            }
            return value;
        }

        private Map.Entry<String, String> scanHeaders(String name) {
            String lc = name.toLowerCase();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase().equals(lc))
                    return entry;
            }
            return null;
        }

        public String cookie(String name) {
            Validate.notNull(name, "Cookie name must not be null");
            return cookies.get(name);
        }

        public T cookie(String name, String value) {
            Validate.notEmpty(name, "Cookie name must not be empty");
            Validate.notNull(value, "Cookie value must not be null");
            cookies.put(name, value);
            return (T) this;
        }

        public boolean hasCookie(String name) {
            Validate.notEmpty("Cookie name must not be empty");
            return cookies.containsKey(name);
        }

        public T removeCookie(String name) {
            Validate.notEmpty("Cookie name must not be empty");
            cookies.remove(name);
            return (T) this;
        }

        public Map<String, String> cookies() {
            return cookies;
        }
    }

    public static class Request extends Base<Connection.Request> implements Connection.Request {
        private int timeoutMilliseconds;
        private boolean followRedirects;
        private Collection<Connection.KeyVal> data;
        private boolean ignoreHttpErrors = false;
        private boolean ignoreContentType = false;

      	private Request() {
            timeoutMilliseconds = 3000;
            followRedirects = true;
            data = new ArrayList<Connection.KeyVal>();
            method = Connection.Method.GET;
            headers.put("Accept-Encoding", "gzip");
        }

        public int timeout() {
            return timeoutMilliseconds;
        }

        public Request timeout(int millis) {
            Validate.isTrue(millis >= 0, "Timeout milliseconds must be 0 (infinite) or greater");
            timeoutMilliseconds = millis;
            return this;
        }

        public boolean followRedirects() {
            return followRedirects;
        }

        public Connection.Request followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public boolean ignoreHttpErrors() {
            return ignoreHttpErrors;
        }

        public void ignoreHttpErrors(boolean ignoreHttpErrors) {
            this.ignoreHttpErrors = ignoreHttpErrors;
        }

        public boolean ignoreContentType() {
            return ignoreContentType;
        }

        public void ignoreContentType(boolean ignoreContentType) {
            this.ignoreContentType = ignoreContentType;
        }

        public Request data(Connection.KeyVal keyval) {
            Validate.notNull(keyval, "Key val must not be null");
            data.add(keyval);
            return this;
        }

        public Collection<Connection.KeyVal> data() {
            return data;
        }
    }

    public static class Response extends Base<Connection.Response> implements Connection.Response {
        private static final int MAX_REDIRECTS = 20;
        private int statusCode;
        private String statusMessage;
        private ByteBuffer byteData;
        private String charset;
        private String contentType;
        private boolean executed = false;
        private int numRedirects = 0;
        private Connection.Request req;

        Response() {
            super();
        }

        private Response(Response previousResponse) throws IOException {
            super();
            if (previousResponse != null) {
                numRedirects = previousResponse.numRedirects + 1;
                if (numRedirects >= MAX_REDIRECTS)
                    throw new IOException(String.format("Too many redirects occurred trying to load URL %s", previousResponse.url()));
            }
        }

        static Response execute(Connection.Request req) throws IOException {
            return execute(req, null);
        }

        static Response execute(Connection.Request req, Response previousResponse) throws IOException {
            Validate.notNull(req, "Request must not be null");
            String protocol = req.url().getProtocol();
            Validate
                .isTrue(protocol.equals("http") || protocol.equals("https"), "Only http & https protocols supported");

            // set up the request for execution
            if (req.method() == Connection.Method.GET && req.data().size() > 0)
                serialiseRequestUrl(req); // appends query string
            HttpURLConnection conn = createConnection(req);
            conn.connect();
            if (req.method() == Connection.Method.POST)
                writePost(req.data(), conn.getOutputStream());          

            int status = conn.getResponseCode();
            boolean needsRedirect = false;
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER)
                    needsRedirect = true;
                else if (!req.ignoreHttpErrors())
                    throw new IOException(status + " error loading URL " + req.url().toString());
            }
            Response res = new Response(previousResponse);
            res.setupFromConnection(conn, previousResponse);
            if (needsRedirect && req.followRedirects()) {
                req.url(new URL(req.url(), res.header("Location")));
                for (Map.Entry<String, String> cookie : res.cookies.entrySet()) { // add response cookies to request (for e.g. login posts)
                    req.cookie(cookie.getKey(), cookie.getValue());
                }
                return execute(req, res);
            }
            res.req = req;

            InputStream bodyStream = null;
            InputStream dataStream = null;
            try {
                dataStream = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
            	bodyStream = res.hasHeader("Content-Encoding") && res.header("Content-Encoding").equalsIgnoreCase("gzip") ?
                        new BufferedInputStream(new GZIPInputStream(dataStream)) :
                        new BufferedInputStream(dataStream);
                
                res.byteData = DataUtil.readToByteBuffer(bodyStream);
                res.charset = DataUtil.getCharsetFromContentType(res.contentType); // may be null, readInputStream deals with it
            } finally {
                if (bodyStream != null) bodyStream.close();
                if (dataStream != null) dataStream.close();
            }

            res.executed = true;
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

        public String contentType() {
            return contentType;
        }

        public Document parse() throws IOException {
            Validate.isTrue(executed, "Request must be executed (with .execute(), .get(), or .post() before parsing response");
            if (!req.ignoreContentType() && (contentType == null || !(contentType.startsWith("text/") || contentType.startsWith("application/xml") || contentType.startsWith("application/xhtml+xml"))))
                throw new IOException(String.format("Unhandled content type \"%s\" on URL %s. Must be text/*, application/xml, or application/xhtml+xml",
                    contentType, url.toString()));
            Document doc = DataUtil.parseByteData(byteData, charset, url.toExternalForm());
            byteData.rewind();
            charset = doc.outputSettings().charset().name(); // update charset from meta-equiv, possibly
            return doc;
        }

        public String body() {
            Validate.isTrue(executed, "Request must be executed (with .execute(), .get(), or .post() before getting response body");
            // charset gets set from header on execute, and from meta-equiv on parse. parse may not have happened yet
            String body;
            if (charset == null)
                body = Charset.forName(DataUtil.defaultCharset).decode(byteData).toString();
            else
                body = Charset.forName(charset).decode(byteData).toString();
            byteData.rewind();
            return body;
        }

        public byte[] bodyAsBytes() {
            Validate.isTrue(executed, "Request must be executed (with .execute(), .get(), or .post() before getting response body");
            return byteData.array();
        }

        // set up connection defaults, and details from request
        private static HttpURLConnection createConnection(Connection.Request req) throws IOException {
            HttpURLConnection conn = (HttpURLConnection) req.url().openConnection();
            conn.setRequestMethod(req.method().name());
            conn.setInstanceFollowRedirects(false); // don't rely on native redirection support
            conn.setConnectTimeout(req.timeout());
            conn.setReadTimeout(req.timeout());
            if (req.method() == Method.POST)
                conn.setDoOutput(true);
            if (req.cookies().size() > 0)
                conn.addRequestProperty("Cookie", getRequestCookieString(req));
            for (Map.Entry<String, String> header : req.headers().entrySet()) {
                conn.addRequestProperty(header.getKey(), header.getValue());
            }
            return conn;
        }

        // set up url, method, header, cookies
        private void setupFromConnection(HttpURLConnection conn, Connection.Response previousResponse) throws IOException {
            method = Connection.Method.valueOf(conn.getRequestMethod());
            url = conn.getURL();
            statusCode = conn.getResponseCode();
            statusMessage = conn.getResponseMessage();
            contentType = conn.getContentType();

            Map<String, List<String>> resHeaders = conn.getHeaderFields();
            processResponseHeaders(resHeaders);

            // if from a redirect, map previous response cookies into this response
            if (previousResponse != null) {
                for (Map.Entry<String, String> prevCookie : previousResponse.cookies().entrySet()) {
                    if (!hasCookie(prevCookie.getKey()))
                        cookie(prevCookie.getKey(), prevCookie.getValue());
                }
            }
        }

        void processResponseHeaders(Map<String, List<String>> resHeaders) {
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                String name = entry.getKey();
                if (name == null)
                    continue; // http/1.1 line

                List<String> values = entry.getValue();
                if (name.equalsIgnoreCase("Set-Cookie")) {
                    for (String value : values) {
                        if (value == null)
                            continue;
                        TokenQueue cd = new TokenQueue(value);
                        String cookieName = cd.chompTo("=").trim();
                        String cookieVal = cd.consumeTo(";").trim();
                        if (cookieVal == null)
                            cookieVal = "";
                        // ignores path, date, domain, secure et al. req'd?
                        // name not blank, value not null
                        if (cookieName != null && cookieName.length() > 0)
                            cookie(cookieName, cookieVal);
                    }
                } else { // only take the first instance of each header
                    if (!values.isEmpty())
                        header(name, values.get(0));
                }
            }
        }

        private static void writePost(Collection<Connection.KeyVal> data, OutputStream outputStream) throws IOException {
            OutputStreamWriter w = new OutputStreamWriter(outputStream, DataUtil.defaultCharset);
            boolean first = true;
            for (Connection.KeyVal keyVal : data) {
                if (!first) 
                    w.append('&');
                else
                    first = false;
                
                w.write(URLEncoder.encode(keyVal.key(), DataUtil.defaultCharset));
                w.write('=');
                w.write(URLEncoder.encode(keyVal.value(), DataUtil.defaultCharset));
            }
            w.close();
        }
        
        private static String getRequestCookieString(Connection.Request req) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> cookie : req.cookies().entrySet()) {
                if (!first)
                    sb.append("; ");
                else
                    first = false;
                sb.append(cookie.getKey()).append('=').append(cookie.getValue());
                // todo: spec says only ascii, no escaping / encoding defined. validate on set? or escape somehow here?
            }
            return sb.toString();
        }

        // for get url reqs, serialise the data map into the url
        private static void serialiseRequestUrl(Connection.Request req) throws IOException {
            URL in = req.url();
            StringBuilder url = new StringBuilder();
            boolean first = true;
            // reconstitute the query, ready for appends
            url
                .append(in.getProtocol())
                .append("://")
                .append(in.getAuthority()) // includes host, port
                .append(in.getPath())
                .append("?");
            if (in.getQuery() != null) {
                url.append(in.getQuery());
                first = false;
            }
            for (Connection.KeyVal keyVal : req.data()) {
                if (!first)
                    url.append('&');
                else
                    first = false;
                url
                    .append(URLEncoder.encode(keyVal.key(), DataUtil.defaultCharset))
                    .append('=')
                    .append(URLEncoder.encode(keyVal.value(), DataUtil.defaultCharset));
            }
            req.url(new URL(url.toString()));
            req.data().clear(); // moved into url as get params
        }
    }

    public static class KeyVal implements Connection.KeyVal {
        private String key;
        private String value;

        public static KeyVal create(String key, String value) {
            Validate.notEmpty(key, "Data key must not be empty");
            Validate.notNull(value, "Data value must not be null");
            return new KeyVal(key, value);
        }

        private KeyVal(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public KeyVal key(String key) {
            Validate.notEmpty(key, "Data key must not be empty");
            this.key = key;
            return this;
        }

        public String key() {
            return key;
        }

        public KeyVal value(String value) {
            Validate.notNull(value, "Data value must not be null");
            this.value = value;
            return this;
        }

        public String value() {
            return value;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }      
    }
}
