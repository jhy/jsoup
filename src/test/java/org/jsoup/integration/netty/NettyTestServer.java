package org.jsoup.integration.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import org.jsoup.integration.TestAuth;
import org.jsoup.integration.TestServer;
import org.jsoup.integration.TestTls;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 Hosts the jsoup origin test routes on Netty
 */
public final class NettyTestServer {
    private static Map<String, RouteHandler> routes = Collections.emptyMap();

    private static volatile int port;
    private static volatile int tlsPort;
    private static EventLoopGroup acceptorGroup;
    private static EventLoopGroup ioGroup;
    private static Channel httpChannel;
    private static Channel tlsChannel;

    private NettyTestServer() {
    }

    /**
     Returns whether the shared origin listeners are already running
     */
    public static boolean isStarted() {
        return httpChannel != null && tlsChannel != null;
    }

    /**
     Starts the shared HTTP and HTTPS listeners for the origin routes
     */
    public static synchronized void start(Iterable<TestServer.Endpoint> endpoints) {
        if (httpChannel != null) return;

        try {
            routes = routeMap(endpoints);

            acceptorGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
            ioGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

            httpChannel = bind(null);
            port = ((InetSocketAddress) httpChannel.localAddress()).getPort();

            tlsChannel = bind(TestTls.createNettyServerContext());
            tlsPort = ((InetSocketAddress) tlsChannel.localAddress()).getPort();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String url(String routePath) {
        return "http://" + NettySupport.Localhost + ":" + port + routePath;
    }

    public static String tlsUrl(String routePath) {
        return "https://" + NettySupport.Localhost + ":" + tlsPort + routePath;
    }

    /**
     Binds a listener using the shared routing pipeline
     */
    private static Channel bind(SslContext sslContext) throws InterruptedException {
        return new ServerBootstrap()
            .group(acceptorGroup, ioGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    if (sslContext != null) {
                        ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                    }
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new RoutingHandler());
                }
            })
            .bind(NettySupport.Localhost, 0)
            .sync()
            .channel();
    }

    /**
     Builds the routing table from the stable endpoint descriptors on TestServer
     */
    private static Map<String, RouteHandler> routeMap(Iterable<TestServer.Endpoint> endpoints) {
        Map<String, RouteHandler> routeMap = new LinkedHashMap<String, RouteHandler>();
        for (TestServer.Endpoint endpoint : endpoints) {
            routeMap.put(endpoint.path(), endpoint.handler());
        }
        return Collections.unmodifiableMap(routeMap);
    }

    private static final class RoutingHandler extends SimpleChannelInboundHandler<HttpObject> {
        private RequestState current;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject message) throws Exception {
            if (message instanceof HttpRequest) {
                startRequest(ctx, (HttpRequest) message);
                return;
            }

            if (current == null || !(message instanceof HttpContent)) {
                return;
            }

            HttpContent content = (HttpContent) message;
            current.append(content);
            if (message instanceof LastHttpContent) {
                finishRequest(ctx);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (NettySupport.isClientDisconnect(cause)) {
                ctx.close();
                return;
            }

            cause.printStackTrace(System.err);
            ctx.close();
        }

        private void startRequest(ChannelHandlerContext ctx, HttpRequest request) {
            current = null;
            if (!isAuthorized(request)) {
                sendAuthChallenge(ctx);
                return;
            }

            String path = new QueryStringDecoder(request.uri()).path();
            String routePath = routePath(path);
            RouteHandler handler = routes.get(routePath);

            if (handler == null) {
                sendNotFound(ctx);
                return;
            }

            current = RequestState.parsed(request, routePath, handler);
        }

        private static boolean isAuthorized(HttpRequest request) {
            return TestAuth.checkAuth(false, false,
                request.headers().get(TestAuth.wantsHeader(false)),
                request.headers().get(TestAuth.authorizationHeader(false)));
        }

        private static void sendAuthChallenge(ChannelHandlerContext ctx) {
            DefaultFullHttpResponse response = plainResponse(
                HttpResponseStatus.valueOf(TestAuth.failureStatus(false)),
                "");
            response.headers().set(TestAuth.challengeHeader(false), TestAuth.challengeValue(false));
            ctx.writeAndFlush(response);
        }

        /**
         Finishes the current request once the last body chunk arrives
         */
        private void finishRequest(ChannelHandlerContext ctx) throws Exception {
            RequestState state = current;
            current = null;

            TestRequest request = state.toTestRequest();
            TestResponse response = new TestResponse(ctx, request.method(), HttpUtil.isKeepAlive(state.request));
            state.handler.handle(request, response);
            response.sendIfReady();
        }

        private static void sendNotFound(ChannelHandlerContext ctx) {
            ctx.writeAndFlush(plainResponse(HttpResponseStatus.NOT_FOUND, "Not Found"));
        }

        private static DefaultFullHttpResponse plainResponse(HttpResponseStatus status, String bodyText) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                status,
                Unpooled.copiedBuffer(bodyText, io.netty.util.CharsetUtil.UTF_8));
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            HttpUtil.setContentLength(response, response.content().readableBytes());
            return response;
        }

        private static String routePath(String path) {
            if (path == null || path.isEmpty() || "/".equals(path)) return path;
            int nextSlash = path.indexOf('/', 1);
            return nextSlash == -1 ? path : path.substring(0, nextSlash);
        }

    }

    /**
     Buffers a parsed request body after the route has been selected
     */
    private static final class RequestState {
        private final HttpRequest request;
        private final String routePath;
        private final RouteHandler handler;
        private final ByteArrayOutputStream body;

        private RequestState(HttpRequest request, String routePath, RouteHandler handler) {
            this.request = request;
            this.routePath = routePath;
            this.handler = handler;
            this.body = new ByteArrayOutputStream();
        }

        static RequestState parsed(HttpRequest request, String routePath, RouteHandler handler) {
            return new RequestState(request, routePath, handler);
        }

        /**
         Appends a inbound body chunk to the buffered request body
         */
        void append(HttpContent content) {
            if (body.size() + content.content().readableBytes() > NettySupport.MaxBodyBytes) {
                throw new IllegalStateException(
                    "Request body exceeded test harness limit of " + NettySupport.MaxBodyBytes + " bytes");
            }

            byte[] bytes = ByteBufUtil.getBytes(content.content());
            body.write(bytes, 0, bytes.length);
        }

        TestRequest toTestRequest() {
            HttpHeaders headers = request.headers().copy();
            return new TestRequest(request.method().name(), request.uri(), routePath, headers, body.toByteArray());
        }
    }
}
