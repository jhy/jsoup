package org.jsoup.integration;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 Shares the localhost test certificate setup across the integration harnesses
 */
public final class TestTls {
    private static final String KeystorePassword = "hunter2";
    private static volatile boolean defaultTrustConfigured;

    private TestTls() {
    }

    /**
     Loads the shared localhost certificate bundle
     */
    public static File getKeystoreFile() {
        File keystoreFile = ParseTest.getFile("/local-cert/server.pfx");
        if (!keystoreFile.exists()) {
            throw new IllegalStateException("Test keystore not found: " + keystoreFile);
        }
        return keystoreFile;
    }

    public static SslContext createNettyServerContext() {
        try {
            KeyStore keyStore = loadKeyStore();
            KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, passwordChars());
            return SslContextBuilder.forServer(keyManagerFactory).build();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     Configure HttpsUrlConnection (jsoup) to trust (only) this cert
     */
    public static synchronized void setupDefaultTrust() {
        if (defaultTrustConfigured) return;

        try {
            KeyStore trustStore = loadKeyStore();
            TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            TrustManager[] managers = trustManagerFactory.getTrustManagers();
            SSLContext tls = SSLContext.getInstance("TLS");
            tls.init(null, managers, null);
            SSLContext.setDefault(tls);
            defaultTrustConfigured = true;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static KeyStore loadKeyStore() throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream stream = Files.newInputStream(getKeystoreFile().toPath())) {
            keyStore.load(stream, passwordChars());
        }
        return keyStore;
    }

    private static char[] passwordChars() {
        return KeystorePassword.toCharArray();
    }
}
