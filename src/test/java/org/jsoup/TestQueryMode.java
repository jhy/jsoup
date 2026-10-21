package org.jsoup;

import org.jsoup.internal.SharedConstants;

import java.util.function.Supplier;

/**
 Constructs and checks queries under an explicit mode without changing the surrounding default.
 */
public final class TestQueryMode {
    private TestQueryMode() {
    }

    /**
     Runs a construction or assertion in one mode and restores the surrounding JVM default.
     */
    public static <T> T mode(boolean web, Supplier<T> action) {
        String previous = System.getProperty(SharedConstants.QueryMode);
        try {
            System.setProperty(SharedConstants.QueryMode, web ? "web" : "legacy");
            return action.get();
        } finally {
            if (previous == null) System.clearProperty(SharedConstants.QueryMode);
            else System.setProperty(SharedConstants.QueryMode, previous);
        }
    }
}
