package org.jsoup.helper;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.UncheckedIOException;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.internal.ControllableInputStream;
import org.jsoup.internal.Functions;
import org.jsoup.internal.SharedConstants;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.parser.StreamParser;
import org.jsoup.parser.TokenQueue;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static org.jsoup.Connection.Method.HEAD;
import static org.jsoup.helper.DataUtil.UTF_8;
import static org.jsoup.internal.Normalizer.lowerCase;

/**
 * Implementation of {@link Connection}.
 * @see org.jsoup.Jsoup#connect(String)
 */
@SuppressWarnings("CharsetObjectCanBeUsed")
public class HttpConnection implements Connection {
    public static final String CONTENT_ENCODING = "Content-Encoding";
    /**
     * Many users would get caught by not setting a user-agent and therefore getting different responses on their desktop
     * vs in jsoup, which would otherwise default to {@code Java}. So by default, use a desktop UA.
     */
    public static final String DEFAULT_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.130 Safari/537.36";
    private static final String USER_AGENT = "User-Agent";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    private static final int HTTP_TEMP_REDIR = 307; // http/1.1 temporary redirect, not in Java's set.
    private static final String DefaultUploadType = "application/octet-stream";
    private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

    /**
     Create a new Connection, with the request URL specified.
     @param url the URL to fetch from
     @return a new Connection object
     */
    public static Connection connect(String url) {
        Connection con = new HttpConnection();
        con.url(url);
        return con;
    }

    /**
     Create a new Connection, with the request URL specified.
     @param url the URL to fetch from
     @return a new Connection object
     */
    public static Connection connect(URL url) {
        Connection con = new HttpConnection();
        con.url(url);
        return con;
    }

    /**
     Create a new, empty HttpConnection.
     */
    public HttpConnection() {
        req = new Request();
    }

    /**
     Create a new Request by deep-copying an existing Request. Note that the data and body of the original are not
     copied. All other settings (proxy, parser, cookies, etc) are copied.
     @param copy the request to copy
     */
    HttpConnection(Request copy) {
        req = new Request(copy);
    }

    private static String encodeMimeName(String val) {
        return val.replace("\"", "%22");
    }

    private HttpConnection.Request req;
    private Connection.@Nullable Response res;

    @Override
    public Connection newRequest() {
        // copy the prototype request for the different settings, cookie manager, etc
        return new HttpConnection(req);
    }

    /** Create a new Connection that just wraps the provided Request and Response */
    private HttpConnection(Request req, Response res) {
        this.req = req;
        this.res = res;
    }

    @Override
    public Connection url(URL url) {
        req.url(url);
        return this;
    }

    @Override
    public Connection url(String url) {
        Validate.notEmptyParam(url, "url");
        try {
            req.url(new URL(url));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(String.format("The supplied URL, '%s', is malformed. Make sure it is an absolute URL, and starts with 'http://' or 'https://'. See https://jsoup.org/cookbook/extracting-data/working-with-urls", url), e);
        }
        return this;
    }

    @Override
    public Connection proxy(@Nullable Proxy proxy) {
        req.proxy(proxy);
        return this;
    }

    @Override
    public Connection proxy(String host, int port) {
        req.proxy(host, port);
        return this;
    }

    @Override
    public Connection userAgent(String userAgent) {
        Validate.notNullParam(userAgent, "userAgent");
        req.header(USER_AGENT, userAgent);
        return this;
    }

    @Override
    public Connection timeout(int millis) {
        req.timeout(millis);
        return this;
    }

    @Override
    public Connection maxBodySize(int bytes) {
        req.maxBodySize(bytes);
        return this;
    }

    @Override
    public Connection followRedirects(boolean followRedirects) {
        req.followRedirects(followRedirects);
        return this;
    }

    @Override
    public Connection referrer(String referrer) {
        Validate.notNullParam(referrer, "referrer");
        req.header("Referer", referrer);
        return this;
    }

    @Override
    public Connection method(Method method) {
        req.method(method);
        return this;
    }

    @Override
    public Connection ignoreHttpErrors(boolean ignoreHttpErrors) {
		req.ignoreHttpErrors(ignoreHttpErrors);
		return this;
	}

    @Override
    public Connection ignoreContentType(boolean ignoreContentType) {
        req.ignoreContentType(ignoreContentType);
        return this;
    }

    @Override
    public Connection data(String key, String value) {
        req.data(KeyVal.create(key, value));
        return this;
    }

    @Override
    public Connection sslSocketFactory(SSLSocketFactory sslSocketFactory) {
	    req.sslSocketFactory(sslSocketFactory);
	    return this;
    }

    @Override
    public Connection data(String key, String filename, InputStream inputStream) {
        req.data(KeyVal.create(key, filename, inputStream));
        return this;
    }

    @Override
    public Connection data(String key, String filename, InputStream inputStream, String contentType) {
        req.data(KeyVal.create(key, filename, inputStream).contentType(contentType));
        return this;
    }

    @Override
    public Connection data(Map<String, String> data) {
        Validate.notNullParam(data, "data");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            req.data(KeyVal.create(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    @Override
    public Connection data(String... keyvals) {
        Validate.notNullParam(keyvals, "keyvals");
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

    @Override
    public Connection data(Collection<Connection.KeyVal> data) {
        Validate.notNullParam(data, "data");
        for (Connection.KeyVal entry: data) {
            req.data(entry);
        }
        return this;
    }

    @Override
    public Connection.KeyVal data(String key) {
        Validate.notEmptyParam(key, "key");
        for (Connection.KeyVal keyVal : request().data()) {
            if (keyVal.key().equals(key))
                return keyVal;
        }
        return null;
    }

    @Override
    public Connection requestBody(String body) {
        req.requestBody(body);
        return this;
    }

    @Override
    public Connection header(String name, String value) {
        req.header(name, value);
        return this;
    }

    @Override
    public Connection headers(Map<String,String> headers) {
        Validate.notNullParam(headers, "headers");
        for (Map.Entry<String,String> entry : headers.entrySet()) {
            req.header(entry.getKey(),entry.getValue());
        }
        return this;
    }

    @Override
    public Connection cookie(String name, String value) {
        req.cookie(name, value);
        return this;
    }

    @Override
    public Connection cookies(Map<String, String> cookies) {
        Validate.notNullParam(cookies, "cookies");
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            req.cookie(entry.getKey(), entry.getValue());
        }
        return this;
    }

    @Override
    public Connection cookieStore(CookieStore cookieStore) {
        // create a new cookie manager using the new store
        req.cookieManager = new CookieManager(cookieStore, null);
        return this;
    }

    @Override
    public CookieStore cookieStore() {
        return req.cookieManager.getCookieStore();
    }

    @Override
    public Connection parser(Parser parser) {
        req.parser(parser);
        return this;
    }

    @Override
    public Document get() throws IOException {
        req.method(Method.GET);
        execute();
        Validate.notNull(res);
        return res.parse();
    }

    @Override
    public Document post() throws IOException {
        req.method(Method.POST);
        execute();
        Validate.notNull(res);
        return res.parse();
    }

    @Override
    public Connection.Response execute() throws IOException {
        res = Response.execute(req);
        return res;
    }

    @Override
    public Connection.Request request() {
        return req;
    }

    @Override
    public Connection request(Connection.Request request) {
        req = (HttpConnection.Request) request; // will throw a class-cast exception if the user has extended some but not all of Connection; that's desired
        return this;
    }

    @Override
    public Connection.Response response() {
        if (res == null) {
            throw new IllegalArgumentException("You must execute the request before getting a response.");
        }
        return res;
    }

    @Override
    public Connection response(Connection.Response response) {
        res = response;
        return this;
    }

    @Override
    public Connection postDataCharset(String charset) {
        req.postDataCharset(charset);
        return this;
    }

    @Override public Connection auth(RequestAuthenticator authenticator) {
        req.auth(authenticator);
        return this;
    }

    @SuppressWarnings("unchecked")
    private static abstract class Base<T extends Connection.Base<T>> implements Connection.Base<T> {
        private static final URL UnsetUrl; // only used if you created a new Request()
        static {
            try {
                UnsetUrl = new URL("http://undefined/");
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }

        URL url = UnsetUrl;
        Method method = Method.GET;
        Map<String, List<String>> headers;
        Map<String, String> cookies;

        private Base() {
            headers = new LinkedHashMap<>();
            cookies = new LinkedHashMap<>();
        }

        private Base(Base<T> copy) {
            url = copy.url; // unmodifiable object
            method = copy.method;
            headers = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : copy.headers.entrySet()) {
                headers.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            cookies = new LinkedHashMap<>(); cookies.putAll(copy.cookies); // just holds strings
        }

        @Override
        public URL url() {
            if (url == UnsetUrl)
                throw new IllegalArgumentException("URL not set. Make sure to call #url(...) before executing the request.");
            return url;
        }

        @Override
        public T url(URL url) {
            Validate.notNullParam(url, "url");
            this.url = new UrlBuilder(url).build();
            return (T) this;
        }

        @Override
        public Method method() {
            return method;
        }

        @Override
        public T method(Method method) {
            Validate.notNullParam(method, "method");
            this.method = method;
            return (T) this;
        }

        @Override
        public String header(String name) {
            Validate.notNullParam(name, "name");
            List<String> vals = getHeadersCaseInsensitive(name);
            if (vals.size() > 0) {
                // https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
                return StringUtil.join(vals, ", ");
            }

            return null;
        }

        @Override
        public T addHeader(String name, @Nullable String value) {
            Validate.notEmptyParam(name, "name");
            //noinspection ConstantConditions
            value = value == null ? "" : value;

            List<String> values = headers(name);
            if (values.isEmpty()) {
                values = new ArrayList<>();
                headers.put(name, values);
            }
            values.add(value);

            return (T) this;
        }

        @Override
        public List<String> headers(String name) {
            Validate.notEmptyParam(name, "name");
            return getHeadersCaseInsensitive(name);
        }

        @Override
        public T header(String name, String value) {
            Validate.notEmptyParam(name, "name");
            removeHeader(name); // ensures we don't get an "accept-encoding" and a "Accept-Encoding"
            addHeader(name, value);
            return (T) this;
        }

        @Override
        public boolean hasHeader(String name) {
            Validate.notEmptyParam(name, "name");
            return !getHeadersCaseInsensitive(name).isEmpty();
        }

        /**
         * Test if the request has a header with this value (case insensitive).
         */
        @Override
        public boolean hasHeaderWithValue(String name, String value) {
            Validate.notEmpty(name);
            Validate.notEmpty(value);
            List<String> values = headers(name);
            for (String candidate : values) {
                if (value.equalsIgnoreCase(candidate))
                    return true;
            }
            return false;
        }

        @Override
        public T removeHeader(String name) {
            Validate.notEmptyParam(name, "name");
            Map.Entry<String, List<String>> entry = scanHeaders(name); // remove is case-insensitive too
            if (entry != null)
                headers.remove(entry.getKey()); // ensures correct case
            return (T) this;
        }

        @Override
        public Map<String, String> headers() {
            LinkedHashMap<String, String> map = new LinkedHashMap<>(headers.size());
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String header = entry.getKey();
                List<String> values = entry.getValue();
                if (values.size() > 0)
                    map.put(header, values.get(0));
            }
            return map;
        }

        @Override
        public Map<String, List<String>> multiHeaders() {
            return headers;
        }

        private List<String> getHeadersCaseInsensitive(String name) {
            Validate.notNull(name);

            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (name.equalsIgnoreCase(entry.getKey()))
                    return entry.getValue();
            }

            return Collections.emptyList();
        }

        private Map.@Nullable Entry<String, List<String>> scanHeaders(String name) {
            String lc = lowerCase(name);
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (lowerCase(entry.getKey()).equals(lc))
                    return entry;
            }
            return null;
        }

        @Override
        public String cookie(String name) {
            Validate.notEmptyParam(name, "name");
            return cookies.get(name);
        }

        @Override
        public T cookie(String name, String value) {
            Validate.notEmptyParam(name, "name");
            Validate.notNullParam(value, "value");
            cookies.put(name, value);
            return (T) this;
        }

        @Override
        public boolean hasCookie(String name) {
            Validate.notEmptyParam(name, "name");
            return cookies.containsKey(name);
        }

        @Override
        public T removeCookie(String name) {
            Validate.notEmptyParam(name, "name");
            cookies.remove(name);
            return (T) this;
        }

        @Override
        public Map<String, String> cookies() {
            return cookies;
        }
    }

    public static class Request extends HttpConnection.Base<Connection.Request> implements Connection.Request {
        static {
            System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
            // make sure that we can send Sec-Fetch-Site headers etc.
        }

        private @Nullable Proxy proxy;
        private int timeoutMilliseconds;
        private int maxBodySizeBytes;
        private boolean followRedirects;
        private final Collection<Connection.KeyVal> data;
        private @Nullable String body = null;
        private boolean ignoreHttpErrors = false;
        private boolean ignoreContentType = false;
        private Parser parser;
        private boolean parserDefined = false; // called parser(...) vs initialized in ctor
        private String postDataCharset = DataUtil.defaultCharsetName;
        private @Nullable SSLSocketFactory sslSocketFactory;
        private CookieManager cookieManager;
        private @Nullable RequestAuthenticator authenticator;
        private volatile boolean executing = false;

        Request() {
            super();
            timeoutMilliseconds = 30000; // 30 seconds
            maxBodySizeBytes = 1024 * 1024 * 2; // 2MB
            followRedirects = true;
            data = new ArrayList<>();
            method = Method.GET;
            addHeader("Accept-Encoding", "gzip");
            addHeader(USER_AGENT, DEFAULT_UA);
            parser = Parser.htmlParser();
            cookieManager = new CookieManager(); // creates a default InMemoryCookieStore
        }

        Request(Request copy) {
            super(copy);
            proxy = copy.proxy;
            postDataCharset = copy.postDataCharset;
            timeoutMilliseconds = copy.timeoutMilliseconds;
            maxBodySizeBytes = copy.maxBodySizeBytes;
            followRedirects = copy.followRedirects;
            data = new ArrayList<>(); // data not copied
            //body not copied
            ignoreHttpErrors = copy.ignoreHttpErrors;
            ignoreContentType = copy.ignoreContentType;
            parser = copy.parser.newInstance(); // parsers and their tree-builders maintain state, so need a fresh copy
            parserDefined = copy.parserDefined;
            sslSocketFactory = copy.sslSocketFactory; // these are all synchronized so safe to share
            cookieManager = copy.cookieManager;
            authenticator = copy.authenticator;
            executing = false;
        }

        @Override
        public Proxy proxy() {
            return proxy;
        }

        @Override
        public Request proxy(@Nullable Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        @Override
        public Request proxy(String host, int port) {
            this.proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port));
            return this;
        }

        @Override
        public int timeout() {
            return timeoutMilliseconds;
        }

        @Override
        public Request timeout(int millis) {
            Validate.isTrue(millis >= 0, "Timeout milliseconds must be 0 (infinite) or greater");
            timeoutMilliseconds = millis;
            return this;
        }

        @Override
        public int maxBodySize() {
            return maxBodySizeBytes;
        }

        @Override
        public Connection.Request maxBodySize(int bytes) {
            Validate.isTrue(bytes >= 0, "maxSize must be 0 (unlimited) or larger");
            maxBodySizeBytes = bytes;
            return this;
        }

        @Override
        public boolean followRedirects() {
            return followRedirects;
        }

        @Override
        public Connection.Request followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        @Override
        public boolean ignoreHttpErrors() {
            return ignoreHttpErrors;
        }

        @Override
        public SSLSocketFactory sslSocketFactory() {
            return sslSocketFactory;
        }

        @Override
        public void sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
        }

        @Override
        public Connection.Request ignoreHttpErrors(boolean ignoreHttpErrors) {
            this.ignoreHttpErrors = ignoreHttpErrors;
            return this;
        }

        @Override
        public boolean ignoreContentType() {
            return ignoreContentType;
        }

        @Override
        public Connection.Request ignoreContentType(boolean ignoreContentType) {
            this.ignoreContentType = ignoreContentType;
            return this;
        }

        @Override
        public Request data(Connection.KeyVal keyval) {
            Validate.notNullParam(keyval, "keyval");
            data.add(keyval);
            return this;
        }

        @Override
        public Collection<Connection.KeyVal> data() {
            return data;
        }

        @Override
        public Connection.Request requestBody(@Nullable String body) {
            this.body = body;
            return this;
        }

        @Override
        public String requestBody() {
            return body;
        }

        @Override
        public Request parser(Parser parser) {
            this.parser = parser;
            parserDefined = true;
            return this;
        }

        @Override
        public Parser parser() {
            return parser;
        }

        @Override
        public Connection.Request postDataCharset(String charset) {
            Validate.notNullParam(charset, "charset");
            if (!Charset.isSupported(charset)) throw new IllegalCharsetNameException(charset);
            this.postDataCharset = charset;
            return this;
        }

        @Override
        public String postDataCharset() {
            return postDataCharset;
        }

        CookieManager cookieManager() {
            return cookieManager;
        }

        @Override public Connection.Request auth(@Nullable RequestAuthenticator authenticator) {
            this.authenticator = authenticator;
            return this;
        }

        @Override @Nullable public RequestAuthenticator auth() {
            return authenticator;
        }
    }

    public static class Response extends HttpConnection.Base<Connection.Response> implements Connection.Response {
        private static final int MAX_REDIRECTS = 20;
        private static final String LOCATION = "Location";
        private final int statusCode;
        private final String statusMessage;
        private @Nullable ByteBuffer byteData;
        private @Nullable ControllableInputStream bodyStream;
        private @Nullable HttpURLConnection conn;
        private @Nullable String charset;
        private @Nullable final String contentType;
        private boolean executed = false;
        private boolean inputStreamRead = false;
        private int numRedirects = 0;
        private final HttpConnection.Request req;

        /*
         * Matches XML content types (like text/xml, image/svg+xml, application/xhtml+xml;charset=UTF8, etc)
         */
        private static final Pattern xmlContentTypeRxp = Pattern.compile("(\\w+)/\\w*\\+?xml.*");

        /**
         <b>Internal only! </b>Creates a dummy HttpConnection.Response, useful for testing. All actual responses
         are created from the HttpURLConnection and fields defined.
         */
        Response() {
            super();
            statusCode = 400;
            statusMessage = "Request not made";
            req = new Request();
            contentType = null;
        }

        static Response execute(HttpConnection.Request req) throws IOException {
            return execute(req, null);
        }

        static Response execute(HttpConnection.Request req, @Nullable Response previousResponse) throws IOException {
            synchronized (req) {
                Validate.isFalse(req.executing, "Multiple threads were detected trying to execute the same request concurrently. Make sure to use Connection#newRequest() and do not share an executing request between threads.");
                req.executing = true;
            }
            Validate.notNullParam(req, "req");
            URL url = req.url();
            Validate.notNull(url, "URL must be specified to connect");
            String protocol = url.getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https"))
                throw new MalformedURLException("Only http & https protocols supported");
            final boolean methodHasBody = req.method().hasBody();
            final boolean hasRequestBody = req.requestBody() != null;
            if (!methodHasBody)
                Validate.isFalse(hasRequestBody, "Cannot set a request body for HTTP method " + req.method());

            // set up the request for execution
            String mimeBoundary = null;
            if (req.data().size() > 0 && (!methodHasBody || hasRequestBody))
                serialiseRequestUrl(req);
            else if (methodHasBody)
                mimeBoundary = setOutputContentType(req);

            long startTime = System.nanoTime();
            HttpURLConnection conn = createConnection(req);
            Response res = null;
            try {
                conn.connect();
                if (conn.getDoOutput()) {
                    OutputStream out = conn.getOutputStream();
                    try { writePost(req, out, mimeBoundary); }
                    catch (IOException e) { conn.disconnect(); throw e; }
                    finally { out.close(); }
                }

                int status = conn.getResponseCode();
                res = new Response(conn, req, previousResponse);

                // redirect if there's a location header (from 3xx, or 201 etc)
                if (res.hasHeader(LOCATION) && req.followRedirects()) {
                    if (status != HTTP_TEMP_REDIR) {
                        req.method(Method.GET); // always redirect with a get. any data param from original req are dropped.
                        req.data().clear();
                        req.requestBody(null);
                        req.removeHeader(CONTENT_TYPE);
                    }

                    String location = res.header(LOCATION);
                    Validate.notNull(location);
                    if (location.startsWith("http:/") && location.charAt(6) != '/') // fix broken Location: http:/temp/AAG_New/en/index.php
                        location = location.substring(6);
                    URL redir = StringUtil.resolve(req.url(), location);
                    req.url(redir);

                    req.executing = false;
                    return execute(req, res);
                }
                if ((status < 200 || status >= 400) && !req.ignoreHttpErrors())
                        throw new HttpStatusException("HTTP error fetching URL", status, req.url().toString());

                // check that we can handle the returned content type; if not, abort before fetching it
                String contentType = res.contentType();
                if (contentType != null
                        && !req.ignoreContentType()
                        && !contentType.startsWith("text/")
                        && !xmlContentTypeRxp.matcher(contentType).matches()
                        )
                    throw new UnsupportedMimeTypeException("Unhandled content type. Must be text/*, */xml, or */*+xml",
                            contentType, req.url().toString());

                // switch to the XML parser if content type is xml and not parser not explicitly set
                if (contentType != null && xmlContentTypeRxp.matcher(contentType).matches()) {
                    if (!req.parserDefined) req.parser(Parser.xmlParser());
                }

                res.charset = DataUtil.getCharsetFromContentType(res.contentType); // may be null, readInputStream deals with it
                if (conn.getContentLength() != 0 && req.method() != HEAD) { // -1 means unknown, chunked. sun throws an IO exception on 500 response with no content when trying to read body
                    InputStream stream = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
                    if (res.hasHeaderWithValue(CONTENT_ENCODING, "gzip"))
                        stream = new GZIPInputStream(stream);
                    else if (res.hasHeaderWithValue(CONTENT_ENCODING, "deflate"))
                        stream = new InflaterInputStream(stream, new Inflater(true));
                    
                    res.bodyStream = ControllableInputStream.wrap(
                        stream, SharedConstants.DefaultBufferSize, req.maxBodySize())
                        .timeout(startTime, req.timeout());
                } else {
                    res.byteData = DataUtil.emptyByteBuffer();
                }
            } catch (IOException e) {
                if (res != null) res.safeClose(); // will be non-null if got to conn
                throw e;
            } finally {
                req.executing = false;

                // detach any thread local auth delegate
                if (req.authenticator != null)
                    AuthenticationHandler.handler.remove();
            }

            res.executed = true;
            return res;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public String statusMessage() {
            return statusMessage;
        }

        @Override
        public String charset() {
            return charset;
        }

        @Override
        public Response charset(String charset) {
            this.charset = charset;
            return this;
        }

        @Override
        public String contentType() {
            return contentType;
        }

        /** Called from parse() or streamParser(), validates and prepares the input stream, and aligns common settings. */
        private InputStream prepareParse() {
            Validate.isTrue(executed, "Request must be executed (with .execute(), .get(), or .post() before parsing response");
            InputStream stream = bodyStream;
            if (byteData != null) { // bytes have been read in to the buffer, parse that
                stream = new ByteArrayInputStream(byteData.array());
                inputStreamRead = false; // ok to reparse if in bytes
            }
            Validate.isFalse(inputStreamRead, "Input stream already read and parsed, cannot re-read.");
            Validate.notNull(stream);
            inputStreamRead = true;
            return stream;
        }

        @Override public Document parse() throws IOException {
            InputStream stream = prepareParse();
            Document doc = DataUtil.parseInputStream(stream, charset, url.toExternalForm(), req.parser());
            doc.connection(new HttpConnection(req, this)); // because we're static, don't have the connection obj. // todo - maybe hold in the req?
            charset = doc.outputSettings().charset().name(); // update charset from meta-equiv, possibly
            safeClose();
            return doc;
        }

        @Override public StreamParser streamParser() throws IOException {
            InputStream stream = prepareParse();
            String baseUri = url.toExternalForm();
            DataUtil.CharsetDoc charsetDoc = DataUtil.detectCharset(stream, charset, baseUri, req.parser());
            // note that there may be a document in CharsetDoc as a result of scanning meta-data -- but as requires a stream parse, it is not used here. todo - revisit.

            // set up the stream parser and rig this connection up to the parsed doc:
            StreamParser streamer = new StreamParser(req.parser());
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charsetDoc.charset));
            DataUtil.maybeSkipBom(reader, charsetDoc);
            streamer.parse(reader, baseUri); // initializes the parse and the document, but does not step() it
            streamer.document().connection(new HttpConnection(req, this));
            charset = charsetDoc.charset.name();

            // we don't safeClose() as in parse(); caller must close streamParser to close InputStream stream
            return streamer;
        }

        private void prepareByteData() {
            Validate.isTrue(executed, "Request must be executed (with .execute(), .get(), or .post() before getting response body");
            if (bodyStream != null && byteData == null) {
                Validate.isFalse(inputStreamRead, "Request has already been read (with .parse())");
                try {
                    byteData = DataUtil.readToByteBuffer(bodyStream, req.maxBodySize());
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    inputStreamRead = true;
                    safeClose();
                }
            }
        }

        @Override
        public String body() {
            prepareByteData();
            Validate.notNull(byteData);
            // charset gets set from header on execute, and from meta-equiv on parse. parse may not have happened yet
            String body = (charset == null ? UTF_8 : Charset.forName(charset))
                .decode(byteData).toString();
            ((Buffer)byteData).rewind(); // cast to avoid covariant return type change in jdk9
            return body;
        }

        @Override
        public byte[] bodyAsBytes() {
            prepareByteData();
            Validate.notNull(byteData);
            return byteData.array();
        }

        @Override
        public Connection.Response bufferUp() {
            prepareByteData();
            return this;
        }

        @Override
        public BufferedInputStream bodyStream() {
            Validate.isTrue(executed, "Request must be executed (with .execute(), .get(), or .post() before getting response body");

            // if we have read to bytes (via buffer up), return those as a stream.
            if (byteData != null) {
                return new BufferedInputStream(new ByteArrayInputStream(byteData.array()), SharedConstants.DefaultBufferSize);
            }

            Validate.isFalse(inputStreamRead, "Request has already been read");
            Validate.notNull(bodyStream);
            inputStreamRead = true;
            return bodyStream.inputStream();
        }

        // set up connection defaults, and details from request
        private static HttpURLConnection createConnection(HttpConnection.Request req) throws IOException {
            Proxy proxy = req.proxy();
            final HttpURLConnection conn = (HttpURLConnection) (
                proxy == null ?
                req.url().openConnection() :
                req.url().openConnection(proxy)
            );

            conn.setRequestMethod(req.method().name());
            conn.setInstanceFollowRedirects(false); // don't rely on native redirection support
            conn.setConnectTimeout(req.timeout());
            conn.setReadTimeout(req.timeout() / 2); // gets reduced after connection is made and status is read

            if (req.sslSocketFactory() != null && conn instanceof HttpsURLConnection)
                ((HttpsURLConnection) conn).setSSLSocketFactory(req.sslSocketFactory());
            if (req.authenticator != null)
                AuthenticationHandler.handler.enable(req.authenticator, conn); // removed in finally
            if (req.method().hasBody())
                conn.setDoOutput(true);
            CookieUtil.applyCookiesToRequest(req, conn); // from the Request key/val cookies and the Cookie Store
            for (Map.Entry<String, List<String>> header : req.multiHeaders().entrySet()) {
                for (String value : header.getValue()) {
                    conn.addRequestProperty(header.getKey(), value);
                }
            }
            return conn;
        }

        /**
         * Call on completion of stream read, to close the body (or error) stream. The connection.disconnect allows
         * keep-alives to work (as the underlying connection is actually held open, despite the name).
         */
        private void safeClose() {
            if (bodyStream != null) {
                try {
                    bodyStream.close();
                } catch (IOException e) {
                    // no-op
                } finally {
                    bodyStream = null;
                }
            }
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
        }

        // set up url, method, header, cookies
        private Response(HttpURLConnection conn, HttpConnection.Request request, HttpConnection.@Nullable Response previousResponse) throws IOException {
            this.conn = conn;
            this.req = request;
            method = Method.valueOf(conn.getRequestMethod());
            url = conn.getURL();
            statusCode = conn.getResponseCode();
            statusMessage = conn.getResponseMessage();
            contentType = conn.getContentType();

            Map<String, List<String>> resHeaders = createHeaderMap(conn);
            processResponseHeaders(resHeaders); // includes cookie key/val read during header scan
            CookieUtil.storeCookies(req, url, resHeaders); // add set cookies to cookie store

            if (previousResponse != null) { // was redirected
                // map previous response cookies into this response cookies() object
                for (Map.Entry<String, String> prevCookie : previousResponse.cookies().entrySet()) {
                    if (!hasCookie(prevCookie.getKey()))
                        cookie(prevCookie.getKey(), prevCookie.getValue());
                }
                previousResponse.safeClose();

                // enforce too many redirects:
                numRedirects = previousResponse.numRedirects + 1;
                if (numRedirects >= MAX_REDIRECTS)
                    throw new IOException(String.format("Too many redirects occurred trying to load URL %s", previousResponse.url()));
            }
        }

        private static LinkedHashMap<String, List<String>> createHeaderMap(HttpURLConnection conn) {
            // the default sun impl of conn.getHeaderFields() returns header values out of order
            final LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
            int i = 0;
            while (true) {
                final String key = conn.getHeaderFieldKey(i);
                final String val = conn.getHeaderField(i);
                if (key == null && val == null)
                    break;
                i++;
                if (key == null || val == null)
                    continue; // skip http1.1 line

                final List<String> vals = headers.computeIfAbsent(key, Functions.listFunction());
                vals.add(val);
            }
            return headers;
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
                        // ignores path, date, domain, validateTLSCertificates et al. full details will be available in cookiestore if required
                        // name not blank, value not null
                        if (cookieName.length() > 0 && !cookies.containsKey(cookieName)) // if duplicates, only keep the first
                            cookie(cookieName, cookieVal);
                    }
                }
                for (String value : values) {
                    addHeader(name, fixHeaderEncoding(value));
                }
            }
        }

        /**
         Servers may encode response headers in UTF-8 instead of RFC defined 8859. This method attempts to detect that
         and re-decode the string as UTF-8.
         * @param val a header value string that may have been incorrectly decoded as 8859.
         * @return a potentially re-decoded string.
         */
        @Nullable
        private static String fixHeaderEncoding(@Nullable String val) {
            if (val == null) return val;
            byte[] bytes = val.getBytes(ISO_8859_1);
            if (looksLikeUtf8(bytes))
                return new String(bytes, UTF_8);
            else
                return val;
        }

        private static boolean looksLikeUtf8(byte[] input) {
            int i = 0;
            // BOM:
            if (input.length >= 3
                && (input[0] & 0xFF) == 0xEF
                && (input[1] & 0xFF) == 0xBB
                && (input[2] & 0xFF) == 0xBF) {
                i = 3;
            }

            int end;
            boolean foundNonAscii = false;
            for (int j = input.length; i < j; ++i) {
                int o = input[i];
                if ((o & 0x80) == 0) {
                    continue; // ASCII
                }
                foundNonAscii = true;

                // UTF-8 leading:
                if ((o & 0xE0) == 0xC0) {
                    end = i + 1;
                } else if ((o & 0xF0) == 0xE0) {
                    end = i + 2;
                } else if ((o & 0xF8) == 0xF0) {
                    end = i + 3;
                } else {
                    return false;
                }

                if (end >= input.length)
                    return false;

                while (i < end) {
                    i++;
                    o = input[i];
                    if ((o & 0xC0) != 0x80) {
                        return false;
                    }
                }
            }
            return foundNonAscii;
        }

        private @Nullable static String setOutputContentType(final Connection.Request req) {
            final String contentType = req.header(CONTENT_TYPE);
            String bound = null;
            if (contentType != null) {
                // no-op; don't add content type as already set (e.g. for requestBody())
                // todo - if content type already set, we could add charset

                // if user has set content type to multipart/form-data, auto add boundary.
                if(contentType.contains(MULTIPART_FORM_DATA) && !contentType.contains("boundary")) {
                    bound = DataUtil.mimeBoundary();
                    req.header(CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + bound);
                }

            }
            else if (needsMultipart(req)) {
                bound = DataUtil.mimeBoundary();
                req.header(CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + bound);
            } else {
                req.header(CONTENT_TYPE, FORM_URL_ENCODED + "; charset=" + req.postDataCharset());
            }
            return bound;
        }

        private static void writePost(final Connection.Request req, final OutputStream outputStream, @Nullable final String boundary) throws IOException {
            final Collection<Connection.KeyVal> data = req.data();
            final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, Charset.forName(req.postDataCharset())));

            if (boundary != null) {
                // boundary will be set if we're in multipart mode
                for (Connection.KeyVal keyVal : data) {
                    w.write("--");
                    w.write(boundary);
                    w.write("\r\n");
                    w.write("Content-Disposition: form-data; name=\"");
                    w.write(encodeMimeName(keyVal.key())); // encodes " to %22
                    w.write("\"");
                    final InputStream input = keyVal.inputStream();
                    if (input != null) {
                        w.write("; filename=\"");
                        w.write(encodeMimeName(keyVal.value()));
                        w.write("\"\r\nContent-Type: ");
                        String contentType = keyVal.contentType();
                        w.write(contentType != null ? contentType : DefaultUploadType);
                        w.write("\r\n\r\n");
                        w.flush(); // flush
                        DataUtil.crossStreams(input, outputStream);
                        outputStream.flush();
                    } else {
                        w.write("\r\n\r\n");
                        w.write(keyVal.value());
                    }
                    w.write("\r\n");
                }
                w.write("--");
                w.write(boundary);
                w.write("--");
            } else {
                String body = req.requestBody();
                if (body != null) {
                    // data will be in query string, we're sending a plaintext body
                    w.write(body);
                }
                else {
                    // regular form data (application/x-www-form-urlencoded)
                    boolean first = true;
                    for (Connection.KeyVal keyVal : data) {
                        if (!first)
                            w.append('&');
                        else
                            first = false;

                        w.write(URLEncoder.encode(keyVal.key(), req.postDataCharset()));
                        w.write('=');
                        w.write(URLEncoder.encode(keyVal.value(), req.postDataCharset()));
                    }
                }
            }
            w.close();
        }

        // for get url reqs, serialise the data map into the url
        private static void serialiseRequestUrl(Connection.Request req) throws IOException {
            UrlBuilder in = new UrlBuilder(req.url());

            for (Connection.KeyVal keyVal : req.data()) {
                Validate.isFalse(keyVal.hasInputStream(), "InputStream data not supported in URL query string.");
                in.appendKeyVal(keyVal);
            }
            req.url(in.build());
            req.data().clear(); // moved into url as get params
        }
    }

    private static boolean needsMultipart(Connection.Request req) {
        // multipart mode, for files. add the header if we see something with an inputstream, and return a non-null boundary
        for (Connection.KeyVal keyVal : req.data()) {
            if (keyVal.hasInputStream())
                return true;
        }
        return false;
    }

    public static class KeyVal implements Connection.KeyVal {
        private String key;
        private String value;
        private @Nullable InputStream stream;
        private @Nullable String contentType;

        public static KeyVal create(String key, String value) {
            return new KeyVal(key, value);
        }

        public static KeyVal create(String key, String filename, InputStream stream) {
            return new KeyVal(key, filename)
                .inputStream(stream);
        }

        private KeyVal(String key, String value) {
            Validate.notEmptyParam(key, "key");
            Validate.notNullParam(value, "value");
            this.key = key;
            this.value = value;
        }

        @Override
        public KeyVal key(String key) {
            Validate.notEmptyParam(key, "key");
            this.key = key;
            return this;
        }

        @Override
        public String key() {
            return key;
        }

        @Override
        public KeyVal value(String value) {
            Validate.notNullParam(value, "value");
            this.value = value;
            return this;
        }

        @Override
        public String value() {
            return value;
        }

        public KeyVal inputStream(InputStream inputStream) {
            Validate.notNullParam(value, "inputStream");
            this.stream = inputStream;
            return this;
        }

        @Override
        public InputStream inputStream() {
            return stream;
        }

        @Override
        public boolean hasInputStream() {
            return stream != null;
        }

        @Override
        public Connection.KeyVal contentType(String contentType) {
            Validate.notEmpty(contentType);
            this.contentType = contentType;
            return this;
        }

        @Override
        public String contentType() {
            return contentType;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}
