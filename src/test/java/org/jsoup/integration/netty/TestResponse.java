package org.jsoup.integration.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.EventExecutor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 Tthe outbound response state for a Netty-backed test request
 */
public final class TestResponse {
    private enum Mode {
        Buffered,
        Deferred,
        Chunked,
        Raw,
        Committed
    }

    private final ChannelHandlerContext ctx;
    private final String method;
    private final boolean keepAlive;
    private final HttpHeaders headers = new DefaultHttpHeaders();
    private final ByteArrayOutputStream body = new ByteArrayOutputStream();
    private HttpResponseStatus status = HttpResponseStatus.OK;
    private Mode mode = Mode.Buffered;
    private PrintWriter writer;
    private boolean forceClose;
    private RawResponseWriter rawWriter;

    TestResponse(ChannelHandlerContext ctx, String method, boolean keepAlive) {
        this.ctx = ctx;
        this.method = method;
        this.keepAlive = keepAlive;
    }

    public void setStatus(int statusCode) {
        status = HttpResponseStatus.valueOf(statusCode);
    }

    public void setContentType(String contentType) {
        headers.set("Content-Type", contentType);
    }

    public void setHeader(String name, String value) {
        headers.set(name, value);
    }

    public void addHeader(String name, String value) {
        headers.add(name, value);
    }

    /**
     Sets the response content length without letting the buffered path overwrite it
     */
    public void setContentLength(long contentLength) {
        headers.set("Content-Length", contentLength);
    }

    /**
     Adds a simple response cookie
     */
    public void addCookie(String name, String value) {
        addCookie(new DefaultCookie(name, value));
    }

    /**
     Adds a response cookie with explicit attributes
     */
    public void addCookie(Cookie cookie) {
        headers.add("Set-Cookie", ServerCookieEncoder.LAX.encode(cookie));
    }

    public void sendRedirect(String location) {
        setStatus(HttpResponseStatus.FOUND.code());
        setHeader("Location", location);
    }

    /**
     Forces the connection closed after the response has been written
     */
    public void closeConnection() {
        forceClose = true;
        headers.set("Connection", "close");
    }

    /**
     Clears the current body and sets the error status
     */
    public void sendError(int statusCode) {
        ensureMutable("modify response");
        setStatus(statusCode);
        body.reset();
    }

    /**
     Defers automatic response flushing so the handler can complete it asynchronously
     */
    public void defer() {
        ensureMutable("modify response");
        mode = Mode.Deferred;
    }

    /**
     Returns whether the client channel is still open
     */
    public boolean isOpen() {
        return ctx.channel().isActive();
    }

    public void schedule(long delayMillis, Runnable task) {
        executor().schedule(task, delayMillis, TimeUnit.MILLISECONDS);
    }

    public void write(String text) {
        ensureMutable("modify response");
        try {
            body.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public void write(byte[] bytes) {
        ensureMutable("modify response");
        try {
            body.write(bytes);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public OutputStream bodyStream() {
        ensureMutable("modify response");
        return body;
    }

    public PrintWriter writer() {
        ensureMutable("modify response");
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(body, StandardCharsets.UTF_8), false);
        }
        return writer;
    }

    /**
     Starts a chunked response so later writes stream straight to the channel
     */
    public void startChunked() {
        if (mode == Mode.Chunked) return;
        ensureMutable("start chunked response");

        flushWriter();
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(headers);
        HttpUtil.setTransferEncodingChunked(response, true);
        writeResponse(response, false);
        mode = Mode.Chunked;
    }

    public void writeChunk(String text) {
        writeChunk(Unpooled.copiedBuffer(text, CharsetUtil.UTF_8));
    }

    public void writeChunk(byte[] bytes) {
        writeChunk(Unpooled.wrappedBuffer(bytes));
    }

    /**
     Switches the response into raw mode so handlers can write custom framing directly
     */
    public RawResponseWriter raw() {
        if (mode == Mode.Raw) return rawWriter;
        ensureMutable("start raw response");
        flushWriter();
        if (body.size() > 0) {
            throw new IllegalStateException("Raw response requires an empty buffered body");
        }
        if (rawWriter == null) {
            rawWriter = new RawResponseWriter(ctx, status, headers);
        }
        mode = Mode.Raw;
        return rawWriter;
    }

    public void finish() {
        if (mode == Mode.Raw) {
            throw new IllegalStateException("Raw response must be closed explicitly");
        }
        if (mode == Mode.Chunked) {
            ChannelFuture future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            closeAfterWrite(future);
            mode = Mode.Committed;
            return;
        }

        sendBufferedResponse();
    }

    void sendIfReady() {
        if (mode != Mode.Buffered) return;
        sendBufferedResponse();
    }

    private void writeChunk(ByteBuf data) {
        if (mode != Mode.Chunked) startChunked();
        ctx.writeAndFlush(new DefaultHttpContent(data));
    }

    /**
     Sends the buffered response body with normal HTTP framing
     */
    private void sendBufferedResponse() {
        if (mode == Mode.Committed) return;

        flushWriter();
        byte[] bytes = body.toByteArray();
        ByteBuf content = isHeadRequest() ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(bytes);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(headers);
        if (!response.headers().contains("Content-Length")) {
            HttpUtil.setContentLength(response, bytes.length);
        }
        writeResponse(response, true);
        mode = Mode.Committed;
    }

    /**
     Sends a framed HTTP response while preserving keep-alive semantics
     */
    private void writeResponse(HttpResponse response, boolean closeWhenDone) {
        if (keepAlive && !forceClose) {
            HttpUtil.setKeepAlive(response, true);
        }

        ChannelFuture future = ctx.writeAndFlush(response);
        if (closeWhenDone) {
            closeAfterWrite(future);
        }
    }

    /**
     Closes non-keepalive connections after the current write completes
     */
    private void closeAfterWrite(ChannelFuture future) {
        if (!keepAlive || forceClose) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     Prevents buffered APIs from being used after raw mode starts
     */
    private void ensureMutable(String action) {
        if (mode == Mode.Chunked) throw new IllegalStateException("Chunked response already started");
        if (mode == Mode.Raw) throw new IllegalStateException("Raw response already started");
        if (mode == Mode.Committed)
            throw new IllegalStateException("Cannot " + action + " after the response committed");
    }

    private void flushWriter() {
        if (writer != null) writer.flush();
    }

    /**
     Returns the event loop that owns this response
     */
    private EventExecutor executor() {
        return ctx.executor();
    }

    private boolean isHeadRequest() {
        return "HEAD".equals(method);
    }
}
