package org.jsoup.integration.netty;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.*;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 Inbound request state used by the Netty-backed test routes
 */
public final class TestRequest {
    private final String method;
    private final String requestTarget;
    private final String requestUri;
    private final String requestPath;
    private final String routePath;
    private final @Nullable String queryString;
    private final HttpHeaders headers;
    private final Map<String, List<String>> parameters;
    private final List<Cookie> cookies;
    private final byte[] bodyBytes;
    private @Nullable List<TestPart> parts;
    private boolean multipartParsed;

    /**
     Builds a request snapshot once the full parsed request body has been buffered
     */
    TestRequest(String method, String requestTarget, String routePath, HttpHeaders headers, byte[] bodyBytes) {
        QueryStringDecoder query = new QueryStringDecoder(requestTarget, StandardCharsets.UTF_8);
        this.method = method;
        this.requestTarget = requestTarget;
        this.requestUri = stripQuery(requestTarget);
        this.requestPath = query.path();
        this.routePath = routePath;
        this.queryString = extractQueryString(requestTarget);
        this.headers = headers;
        this.parameters = copyParameters(query.parameters());
        this.cookies = decodeCookies(headers);
        this.bodyBytes = bodyBytes;
        mergeBodyParameters();
    }

    public String method() {
        return method;
    }

    public String requestUri() {
        return requestUri;
    }

    public String path() {
        return requestPath;
    }

    public @Nullable String pathInfo() {
        return requestPath.length() == routePath.length() ? null : requestPath.substring(routePath.length());
    }

    /**
     Returns the raw query string without the leading ?, if present
     */
    public @Nullable String queryString() {
        return queryString;
    }

    /**
     Returns the first request header with the supplied name
     */
    public @Nullable String header(String name) {
        return headers.get(name);
    }

    /**
     Returns all header values with the supplied name
     */
    public List<String> headers(String name) {
        return Collections.unmodifiableList(headers.getAll(name));
    }

    public Iterable<String> headerNames() {
        return headers.names();
    }

    /**
     Returns the first decoded parameter value with the supplied name
     */
    public @Nullable String parameter(String name) {
        parseMultipartIfNeeded();
        List<String> values = parameters.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    /**
     Returns all decoded parameter values with the supplied name
     */
    public List<String> parameters(String name) {
        parseMultipartIfNeeded();
        List<String> values = parameters.get(name);
        return values != null ? Collections.unmodifiableList(values) : Collections.<String>emptyList();
    }

    /**
     Returns the decoded parameter names
     */
    public Iterable<String> parameterNames() {
        parseMultipartIfNeeded();
        return parameters.keySet();
    }

    /**
     Returns the decoded request cookies in wire order.
     */
    public List<Cookie> cookies() {
        return Collections.unmodifiableList(cookies);
    }

    /**
     Returns the decoded multipart request parts in wire order
     */
    public List<TestPart> parts() {
        parseMultipartIfNeeded();
        return parts != null ? parts : Collections.<TestPart>emptyList();
    }

    public @Nullable String contentType() {
        return header("Content-Type");
    }

    /**
     Returns the buffered request body bytes
     */
    public byte[] bodyBytes() {
        return bodyBytes;
    }

    /**
     Returns the buffered request body interpreted as UTF-8 text
     */
    public String body() {
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }

    /**
     Parses urlencoded request bodies so POST routes can keep using request parameters
     */
    private void mergeBodyParameters() {
        if (!"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method)) return;

        String contentType = contentType();
        if (contentType == null || !contentType.startsWith("application/x-www-form-urlencoded")) return;
        if (bodyBytes.length == 0) return;

        QueryStringDecoder decoder = new QueryStringDecoder("?" + body(), StandardCharsets.UTF_8);
        copyParametersInto(decoder.parameters(), parameters);
    }

    /**
     Parses multipart request bodies so routes can inspect parts and form fields
     */
    private void parseMultipartIfNeeded() {
        if (multipartParsed) return;
        multipartParsed = true;

        String contentType = contentType();
        if (contentType == null || !contentType.startsWith("multipart/form-data")) {
            parts = Collections.emptyList();
            return;
        }

        DefaultHttpRequest request = new DefaultHttpRequest(
            HttpVersion.HTTP_1_1,
            HttpMethod.valueOf(method),
            requestTarget,
            headers.copy());
        DefaultHttpDataFactory factory = new DefaultHttpDataFactory(false, StandardCharsets.UTF_8);
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(factory, request, StandardCharsets.UTF_8);
        List<TestPart> decodedParts = new ArrayList<TestPart>();

        try {
            decoder.offer(new DefaultLastHttpContent(Unpooled.wrappedBuffer(bodyBytes)));
            for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
                if (!(data instanceof HttpData)) continue;

                HttpData httpData = (HttpData) data;
                byte[] bytes = httpData.get();
                decodedParts.add(toPart(data, httpData, bytes));
                if (data instanceof Attribute) {
                    addParameter(data.getName(), ((Attribute) data).getValue());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not parse multipart test request", e);
        } finally {
            decoder.destroy();
        }

        parts = Collections.unmodifiableList(decodedParts);
    }

    /**
     Copies decoded parameters into a mutable map that preserves insertion order
     */
    private static Map<String, List<String>> copyParameters(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<String, List<String>>();
        copyParametersInto(source, copy);
        return copy;
    }

    private static String stripQuery(String requestUri) {
        int pos = requestUri.indexOf('?');
        return pos >= 0 ? requestUri.substring(0, pos) : requestUri;
    }

    /**
     Copies decoded parameter lists into the supplied target map
     */
    private static void copyParametersInto(Map<String, List<String>> source, Map<String, List<String>> target) {
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            List<String> values = target.get(entry.getKey());
            if (values == null) {
                values = new ArrayList<String>();
                target.put(entry.getKey(), values);
            }
            values.addAll(entry.getValue());
        }
    }

    private void addParameter(String name, String value) {
        List<String> values = parameters.computeIfAbsent(name, key -> new ArrayList<>());
        values.add(value);
    }

    private static TestPart toPart(InterfaceHttpData data, HttpData httpData, byte[] bytes) {
        String contentType = data instanceof FileUpload ? ((FileUpload) data).getContentType() : null;
        String submittedFileName = data instanceof FileUpload ? ((FileUpload) data).getFilename() : null;
        return new TestPart(data.getName(), contentType, submittedFileName, httpData.length(), bytes);
    }

    private static List<Cookie> decodeCookies(HttpHeaders headers) {
        List<Cookie> cookies = new ArrayList<Cookie>();
        for (String header : headers.getAll("Cookie")) {
            cookies.addAll(ServerCookieDecoder.LAX.decodeAll(header));
        }
        return cookies;
    }

    private static @Nullable String extractQueryString(String requestUri) {
        int pos = requestUri.indexOf('?');
        return pos == -1 ? null : requestUri.substring(pos + 1);
    }
}
