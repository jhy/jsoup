package org.jsoup.integration.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 Writes a custom HTTP response directly to the channel for malformed-response tests
 */
public final class RawResponseWriter {
    private final ChannelHandlerContext ctx;
    private final List<String> headerLines = new ArrayList<String>();
    private String statusLine;
    private boolean headersFlushed;

    RawResponseWriter(ChannelHandlerContext ctx, HttpResponseStatus status, HttpHeaders headers) {
        this.ctx = ctx;
        statusLine = "HTTP/1.1 " + status.code() + " " + status.reasonPhrase();
        for (Map.Entry<String, String> header : headers) {
            headerLines.add(header.getKey() + ": " + header.getValue());
        }
    }

    /**
     Flushes the current status line and headers to the channel
     */
    public void flushHeaders() {
        if (headersFlushed) return;

        StringBuilder builder = new StringBuilder();
        builder.append(statusLine).append("\r\n");
        for (String headerLine : headerLines) {
            builder.append(headerLine).append("\r\n");
        }
        builder.append("\r\n");
        headersFlushed = true;
        ctx.writeAndFlush(Unpooled.copiedBuffer(builder.toString(), StandardCharsets.US_ASCII));
    }

    /**
     Writes UTF-8 text after ensuring the raw headers are on the wire
     */
    public void write(String text) {
        write(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     Writes raw bytes after ensuring the raw headers are on the wire
     */
    public void write(byte[] bytes) {
        flushHeaders();
        ctx.writeAndFlush(Unpooled.wrappedBuffer(bytes));
    }

    /**
     Closes the connection immediately after any pending writes complete
     */
    public void close() {
        flushHeaders();
        ChannelFuture future = ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
        future.addListener(ChannelFutureListener.CLOSE);
    }
}
