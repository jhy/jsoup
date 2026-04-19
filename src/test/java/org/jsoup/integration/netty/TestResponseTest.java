package org.jsoup.integration.netty;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestResponseTest {
    @Test
    public void writerUsesUtf8() {
        ContextRecorder recorder = new ContextRecorder();
        EmbeddedChannel channel = new EmbeddedChannel(recorder);
        FullHttpResponse response = null;

        try {
            TestResponse testResponse = new TestResponse(recorder.context(), "GET", false);
            PrintWriter writer = testResponse.writer();
            writer.write("鍵=値");
            testResponse.finish();

            response = channel.readOutbound();
            assertNotNull(response);
            assertArrayEquals("鍵=値".getBytes(StandardCharsets.UTF_8), ByteBufUtil.getBytes(response.content()));
        } finally {
            if (response != null) {
                response.release();
            }
            channel.finishAndReleaseAll();
        }
    }

    private static final class ContextRecorder extends ChannelInboundHandlerAdapter {
        private ChannelHandlerContext context;

        @Override
        public void handlerAdded(ChannelHandlerContext ctx) {
            context = ctx;
        }

        /**
         * Returns the embedded handler context so tests can drive one response directly.
         */
        ChannelHandlerContext context() {
            return context;
        }
    }
}
