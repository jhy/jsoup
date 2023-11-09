package org.jsoup.integration;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jsoup.integration.servlets.AuthFilter;
import org.jsoup.integration.servlets.BaseServlet;
import org.jsoup.integration.servlets.ProxyServlet;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicInteger;

public class TestServer {
    static int Port;
    static int TlsPort;

    private static final String Localhost = "localhost";
    private static final String KeystorePassword = "hunter2";

    private static final Server Jetty = newServer();
    private static final ServletHandler JettyHandler = new ServletHandler();
    private static final Server Proxy = newServer();
    private static final Server AuthedProxy = newServer();
    private static final HandlerWrapper ProxyHandler = new HandlerWrapper();
    private static final HandlerWrapper AuthedProxyHandler = new HandlerWrapper();
    private static final ProxySettings ProxySettings = new ProxySettings();

    private static Server newServer() {
        return new Server(new InetSocketAddress(Localhost, 0));
    }

    static {
        Jetty.setHandler(JettyHandler);
        Proxy.setHandler(ProxyHandler);
        AuthedProxy.setHandler(AuthedProxyHandler);

        // TLS setup:
        try {
            File keystoreFile = ParseTest.getFile("/local-cert/server.pfx");
            if (!keystoreFile.exists()) throw new FileNotFoundException(keystoreFile.toString());
            addHttpsConnector(keystoreFile, Jetty);
            setupDefaultTrust(keystoreFile);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private TestServer() {
    }

    public static void start() {
        synchronized (Jetty) {
            if (Jetty.isStarted()) return;

            try {
                Jetty.start();
                JettyHandler.addFilterWithMapping(new FilterHolder(new AuthFilter(false, false)), "/*", FilterMapping.ALL);
                Connector[] jcons = Jetty.getConnectors();
                Port = ((ServerConnector) jcons[0]).getLocalPort();
                TlsPort = ((ServerConnector) jcons[1]).getLocalPort();

                ProxyHandler.setHandler(ProxyServlet.createHandler(false)); // includes proxy, CONNECT proxy, and Auth filters
                Proxy.start();
                ProxySettings.port = ((ServerConnector) Proxy.getConnectors()[0]).getLocalPort();

                AuthedProxyHandler.setHandler(ProxyServlet.createHandler(true));
                AuthedProxy.start();
                ProxySettings.authedPort = ((ServerConnector) AuthedProxy.getConnectors()[0]).getLocalPort();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     Close any current connections to the authed proxy. Tunneled connections only authenticate in their first
     CONNECT, and may be kept alive and reused. So when we want to test unauthed - authed flows, we need to disconnect
     them first.
     */
    static int closeAuthedProxyConnections() {
        ServerConnector connector = (ServerConnector) AuthedProxy.getConnectors()[0];
        AtomicInteger count = new AtomicInteger();
        connector.getConnectedEndPoints().forEach(endPoint -> {
            endPoint.close();
            count.getAndIncrement();
        });
        return count.get();
    }

    public static ServletUrls map(Class<? extends BaseServlet> servletClass) {
        synchronized (Jetty) {
            if (!Jetty.isStarted())
                start(); // if running out of the test cases

            String path = "/" + servletClass.getSimpleName();
            JettyHandler.addServletWithMapping(servletClass, path + "/*");
            String url = "http://" + Localhost + ":" + Port + path;
            String tlsUrl = "https://" + Localhost + ":" + TlsPort + path;

            return new ServletUrls(url, tlsUrl);
        }
    }

    public static class ServletUrls {
        public final String url;
        public final String tlsUrl;

        public ServletUrls(String url, String tlsUrl) {
            this.url = url;
            this.tlsUrl = tlsUrl;
        }
    }

    public static ProxySettings proxySettings() {
        synchronized (Jetty) {
            if (!Jetty.isStarted())
                start();

            return ProxySettings;
        }
    }

    //public static String proxy
    public static class ProxySettings {
        final String hostname = Localhost;
        int port;
        int authedPort;
    }

    private static void addHttpsConnector(File keystoreFile, Server server) {
        // Cribbed from https://github.com/jetty/jetty.project/blob/jetty-9.4.x/examples/embedded/src/main/java/org/eclipse/jetty/embedded/LikeJettyXml.java
        SslContextFactory sslContextFactory = new SslContextFactory.Server();
        String path = keystoreFile.getAbsolutePath();
        sslContextFactory.setKeyStorePath(path);
        sslContextFactory.setKeyStorePassword(KeystorePassword);
        sslContextFactory.setKeyManagerPassword(KeystorePassword);
        sslContextFactory.setTrustStorePath(path);
        sslContextFactory.setTrustStorePassword(KeystorePassword);

        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        ServerConnector sslConnector = new ServerConnector(
            server,
            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
            new HttpConnectionFactory(httpsConfig));
        server.addConnector(sslConnector);
    }

    private static void setupDefaultTrust(File keystoreFile) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, KeyManagementException {
        // Configure HttpsUrlConnection (jsoup) to trust (only) this cert
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(Files.newInputStream(keystoreFile.toPath()), KeystorePassword.toCharArray());
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        TrustManager[] managers = trustManagerFactory.getTrustManagers();
        SSLContext tls = SSLContext.getInstance("TLS");
        tls.init(null, managers, null);
        SSLSocketFactory socketFactory = tls.getSocketFactory();
        HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
    }
}
