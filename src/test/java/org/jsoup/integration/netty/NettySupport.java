package org.jsoup.integration.netty;

import java.io.IOException;
import java.net.SocketException;

/**
 Shared low-level Netty harness constants and socket helpers
 */
final class NettySupport {
    static final String Localhost = "localhost";
    static final int MaxBodyBytes = 10 * 1024 * 1024;

    private NettySupport() {
    }

    /**
     Detects routine client disconnects so the test harness does not log expected socket noise
     */
    static boolean isClientDisconnect(Throwable cause) {
        if (cause instanceof IOException) {
            String message = cause.getMessage();
            return message != null && (message.contains("Connection reset") || message.contains("Broken pipe"));
        }
        return false;
    }
}
