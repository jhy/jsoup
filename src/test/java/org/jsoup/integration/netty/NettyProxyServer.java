package org.jsoup.integration.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.jsoup.integration.TestAuth;
import org.jsoup.integration.TestServer;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 Hosts the jsoup HTTP proxy listeners on Netty.
 */
public final class NettyProxyServer {
    private static final String ProxyAuthorization = "Proxy-Authorization";
    private static final String ProxyConnection = "Proxy-Connection";
    private static final String KeepAlive = "Keep-Alive";
    private static final String ConnectionEstablished = "Connection Established";

    private static final ChannelGroup AuthedConnections = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private static volatile int port;
    private static volatile int authedPort;
    private static EventLoopGroup acceptorGroup;
    private static EventLoopGroup ioGroup;
    private static Channel proxyChannel;
    private static Channel authedProxyChannel;

    private NettyProxyServer() {
    }

    /**
     Returns if the shared proxy listeners are already running
     */
    public static boolean hasStarted() {
        return proxyChannel != null && authedProxyChannel != null;
    }

    /**
     Start the plain and authenticated proxy listeners
     */
    public static synchronized void start() {
        if (hasStarted()) return;

        try {
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
            // removes Basic, which is otherwise excluded from auth for CONNECT tunnels

            acceptorGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
            ioGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

            proxyChannel = bind(false);
            port = ((java.net.InetSocketAddress) proxyChannel.localAddress()).getPort();

            authedProxyChannel = bind(true);
            authedPort = ((java.net.InetSocketAddress) authedProxyChannel.localAddress()).getPort();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     The plain proxy listener port
     */
    public static int port() {
        return port;
    }

    /**
     The always-authenticated proxy listener port
     */
    public static int authedPort() {
        return authedPort;
    }

    /**
     Close all active client channels on the authenticated proxy listener
     */
    public static int closeAuthedConnections() {
        start();
        int count = AuthedConnections.size();
        AuthedConnections.close().awaitUninterruptibly();
        return count;
    }

    /**
     Binds one proxy listener with the supplied auth mode
     */
    private static Channel bind(boolean alwaysAuth) throws InterruptedException {
        return new ServerBootstrap()
            .group(acceptorGroup, ioGroup)
            .channel(NioServerSocketChannel.class)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    if (alwaysAuth) {
                        AuthedConnections.add(ch);
                    }
                    ch.pipeline().addLast(new HttpServerCodec());
                    ch.pipeline().addLast(new ProxyFrontendHandler(alwaysAuth));
                }
            })
            .bind(NettySupport.Localhost, 0)
            .sync()
            .channel();
    }

    /**
     Handles parsed proxy requests on the client-facing listener
     */
    private static final class ProxyFrontendHandler extends SimpleChannelInboundHandler<HttpObject> {
        private final boolean alwaysAuth;
        private BufferedHttpRequest current;

        private ProxyFrontendHandler(boolean alwaysAuth) {
            this.alwaysAuth = alwaysAuth;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, HttpObject message) {
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
                finishHttpRequest(ctx);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!NettySupport.isClientDisconnect(cause)) {
                cause.printStackTrace(System.err);
            }
            ctx.close();
        }

        /**
         Starts one parsed proxy request or CONNECT tunnel setup
         */
        private void startRequest(ChannelHandlerContext ctx, HttpRequest request) {
            current = null;
            if (HttpMethod.CONNECT.equals(request.method())) {
                startConnectRequest(ctx, request);
                return;
            }

            startHttpRequest(ctx, request);
        }

        /**
         Starts an ordinary buffered HTTP proxy request after auth succeeds
         */
        private void startHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
            if (!isAuthorized(request, alwaysAuth)) {
                sendProxyAuthRequired(ctx, request);
                return;
            }

            current = BufferedHttpRequest.parsed(request);
        }

        /**
         Finishes a buffered HTTP proxy request and forwards it upstream
         */
        private void finishHttpRequest(ChannelHandlerContext ctx) {
            BufferedHttpRequest state = current;
            current = null;

            HttpTarget target = httpTargetFrom(state.request);
            if (target == null) {
                sendSimpleResponse(ctx, state.request, HttpResponseStatus.BAD_REQUEST, "Bad Request");
                return;
            }

            connectHttpUpstream(ctx, state, target);
        }

        /**
         Starts a CONNECT tunnel after proxy authentication succeeds
         */
        private void startConnectRequest(ChannelHandlerContext ctx, HttpRequest request) {
            if (!isAuthorized(request, alwaysAuth)) {
                sendProxyAuthRequired(ctx, request);
                return;
            }

            ConnectTarget target = connectTargetFrom(request.uri());
            if (target == null) {
                sendSimpleResponse(ctx, request, HttpResponseStatus.BAD_REQUEST, "Bad Request");
                return;
            }

            Bootstrap bootstrap = new Bootstrap()
                .group(ioGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new TunnelRelayHandler(ctx.channel()));
                    }
                });

            bootstrap.connect(target.host, target.port).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    sendSimpleResponse(ctx, request, HttpResponseStatus.BAD_GATEWAY, "Bad Gateway");
                    return;
                }

                establishTunnel(ctx, future.channel());
            });
        }

        /**
         Connects to the upstream HTTP server and relays one buffered request and response
         */
        private void connectHttpUpstream(ChannelHandlerContext clientCtx, BufferedHttpRequest clientRequest, HttpTarget target) {
            FullHttpRequest upstreamRequest = clientRequest.toUpstreamRequest(target);
            Bootstrap bootstrap = new Bootstrap()
                .group(ioGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(NettySupport.MaxBodyBytes));
                        ch.pipeline().addLast(new HttpUpstreamHandler(clientCtx, clientRequest.request));
                    }
                });

            bootstrap.connect(target.host, target.port).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    sendSimpleResponse(clientCtx, clientRequest.request, HttpResponseStatus.BAD_GATEWAY, "Bad Gateway");
                    return;
                }

                future.channel().writeAndFlush(upstreamRequest).addListener(writeFuture -> {
                    if (!writeFuture.isSuccess()) {
                        future.channel().close();
                        sendSimpleResponse(clientCtx, clientRequest.request, HttpResponseStatus.BAD_GATEWAY,
                            "Bad Gateway");
                    }
                });
            });
        }

        /**
         Switches the client pipeline into raw byte relay mode for a CONNECT tunnel
         */
        private void establishTunnel(ChannelHandlerContext ctx, Channel upstream) {
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                new HttpResponseStatus(200, ConnectionEstablished),
                Unpooled.EMPTY_BUFFER);
            HttpUtil.setContentLength(response, 0);

            ctx.writeAndFlush(response).addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    upstream.close();
                    ctx.close();
                    return;
                }

                ChannelPipeline pipeline = ctx.pipeline();
                if (pipeline.get(HttpServerCodec.class) != null) {
                    pipeline.remove(HttpServerCodec.class);
                }
                pipeline.replace(this, "proxyTunnelRelay", new TunnelRelayHandler(upstream));
            });
        }
    }

    /**
     Buffers a parsed HTTP proxy request body before forwarding it upstream
     */
    private static final class BufferedHttpRequest {
        private final HttpRequest request;
        private final ByteArrayOutputStream body;

        private BufferedHttpRequest(HttpRequest request) {
            this.request = request;
            this.body = new ByteArrayOutputStream();
        }

        /**
         Creates the parsed request state for a matched proxy request.
         */
        static BufferedHttpRequest parsed(HttpRequest request) {
            return new BufferedHttpRequest(request);
        }

        /**
         Appends inbound body chunk to the buffered request body
         */
        void append(HttpContent content) {
            if (body.size() + content.content().readableBytes() > NettySupport.MaxBodyBytes) {
                throw new IllegalStateException(
                    "Proxy request body exceeded test harness limit of " + NettySupport.MaxBodyBytes + " bytes");
            }

            byte[] bytes = ByteBufUtil.getBytes(content.content());
            body.write(bytes, 0, bytes.length);
        }

        /**
         Builds a upstream origin-form request for the parsed proxy target
         */
        FullHttpRequest toUpstreamRequest(HttpTarget target) {
            byte[] bytes = body.toByteArray();
            DefaultFullHttpRequest upstreamRequest = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                request.method(),
                target.originForm,
                Unpooled.wrappedBuffer(bytes));

            upstreamRequest.headers().add(request.headers());
            sanitizeForwardHeaders(upstreamRequest, target);
            HttpUtil.setContentLength(upstreamRequest, bytes.length);
            return upstreamRequest;
        }
    }

    /**
     Buffers an upstream HTTP response and relays it back
     */
    private static final class HttpUpstreamHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
        private final ChannelHandlerContext clientCtx;
        private final HttpRequest clientRequest;

        private HttpUpstreamHandler(ChannelHandlerContext clientCtx, HttpRequest clientRequest) {
            this.clientCtx = clientCtx;
            this.clientRequest = clientRequest;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext upstreamCtx, FullHttpResponse upstreamResponse) {
            if (!clientCtx.channel().isActive()) {
                upstreamCtx.close();
                return;
            }

            DefaultFullHttpResponse clientResponse = copyUpstreamResponse(upstreamResponse);
            sendToClient(clientCtx, clientRequest, clientResponse);
            upstreamCtx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext upstreamCtx, Throwable cause) {
            if (!NettySupport.isClientDisconnect(cause)) {
                cause.printStackTrace(System.err);
            }

            if (clientCtx.channel().isActive()) {
                sendSimpleResponse(clientCtx, clientRequest, HttpResponseStatus.BAD_GATEWAY, "Bad Gateway");
            }
            upstreamCtx.close();
        }
    }

    /**
     Relay raw bytes between a CONNECT tunnel client and upstream
     */
    private static final class TunnelRelayHandler extends ChannelInboundHandlerAdapter {
        private final Channel peer;

        private TunnelRelayHandler(Channel peer) {
            this.peer = peer;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (peer.isActive()) {
                peer.writeAndFlush(ReferenceCountUtil.retain(msg));
            }
            ReferenceCountUtil.release(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (peer.isActive()) {
                peer.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!NettySupport.isClientDisconnect(cause)) {
                cause.printStackTrace(System.err);
            }
            ctx.close();
        }
    }

    /**
     Holder for the parsed target details of a proxied request.
     */
    private static final class HttpTarget {
        private final String host;
        private final int port;
        private final String originForm;

        private HttpTarget(String host, int port, String originForm) {
            this.host = host;
            this.port = port;
            this.originForm = originForm;
        }
    }

    private static final class ConnectTarget {
        private final String host;
        private final int port;

        /**
         Creates a parsed CONNECT target.
         */
        private ConnectTarget(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    private static HttpTarget httpTargetFrom(HttpRequest request) {
        try {
            URI uri = new URI(request.uri());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!"http".equalsIgnoreCase(scheme) || host == null) {
                throw new IllegalStateException("Invalid request");
            }

            int port = uri.getPort() == -1 ? 80 : uri.getPort();
            String originForm = originForm(uri);
            return new HttpTarget(host, port, originForm);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid request");
        }
    }

    /**
     Parses a CONNECT target in host:port form.
     */
    private static ConnectTarget connectTargetFrom(String target) {
        int colon = target.lastIndexOf(':');
        if (colon <= 0 || colon == target.length() - 1) {
            throw new IllegalStateException("Invalid request");
        }

        String host = target.substring(0, colon);
        try {
            int port = Integer.parseInt(target.substring(colon + 1));
            return new ConnectTarget(host, port);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid request");
        }
    }

    /**
     Builds the origin-form path and query for an upstream HTTP request.
     */
    private static String originForm(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (uri.getRawQuery() != null) {
            path += "?" + uri.getRawQuery();
        }
        return path;
    }

    /**
     Removes proxy-only and hop-by-hop headers before forwarding upstream
     */
    private static void sanitizeForwardHeaders(FullHttpRequest request, HttpTarget target) {
        request.headers().remove(ProxyAuthorization);
        request.headers().remove(ProxyConnection);
        request.headers().remove(HttpHeaderNames.CONNECTION);
        request.headers().remove(KeepAlive);
        request.headers().remove(HttpHeaderNames.TE);
        request.headers().remove(HttpHeaderNames.TRAILER);
        request.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
        request.headers().remove(HttpHeaderNames.UPGRADE);
        request.headers().set(HttpHeaderNames.HOST, hostHeader(target));
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    }

    private static String hostHeader(HttpTarget target) {
        return target.port == 80 ? target.host : target.host + ":" + target.port;
    }

    /**
     Check if the client request is authorized
     */
    private static boolean isAuthorized(HttpRequest request, boolean alwaysAuth) {
        return TestAuth.checkAuth(alwaysAuth, true,
            request.headers().get(TestAuth.wantsHeader(true)),
            request.headers().get(TestAuth.authorizationHeader(true)));
    }

    /**
     Sends a proxy-auth challenge back to the client request
     */
    private static void sendProxyAuthRequired(ChannelHandlerContext ctx, HttpRequest request) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.valueOf(TestAuth.failureStatus(true)),
            Unpooled.EMPTY_BUFFER);
        response.headers().set(TestAuth.challengeHeader(true), TestAuth.challengeValue(true));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpUtil.setContentLength(response, 0);
        sendToClient(ctx, request, response);
    }

    /**
     Send a simple text response
     */
    private static void sendSimpleResponse(ChannelHandlerContext ctx, HttpRequest request, HttpResponseStatus status, String bodyText) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            status,
            Unpooled.copiedBuffer(bodyText, io.netty.util.CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpUtil.setContentLength(response, response.content().readableBytes());
        sendToClient(ctx, request, response);
    }

    /**
     Sends a full response to the client while preserving keep-alive semantics
     */
    private static void sendToClient(ChannelHandlerContext ctx, HttpRequest request, FullHttpResponse response) {
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            HttpUtil.setKeepAlive(response, true);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        }

        ChannelFuture future = ctx.writeAndFlush(response);
        if (!keepAlive) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    /**
     Copies the upstream HTTP response into a client-facing full response and adds Via
     */
    private static DefaultFullHttpResponse copyUpstreamResponse(FullHttpResponse upstreamResponse) {
        byte[] bytes = ByteBufUtil.getBytes(upstreamResponse.content());
        DefaultFullHttpResponse clientResponse = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            upstreamResponse.status(),
            Unpooled.wrappedBuffer(bytes));

        clientResponse.headers().add(upstreamResponse.headers());
        clientResponse.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
        clientResponse.headers().remove(HttpHeaderNames.CONNECTION);
        clientResponse.headers().remove(KeepAlive);
        clientResponse.headers().add(HttpHeaderNames.VIA, TestServer.ProxyVia);
        HttpUtil.setContentLength(clientResponse, bytes.length);
        return clientResponse;
    }
}
