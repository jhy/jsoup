package org.jsoup.integration.netty;

import org.jspecify.annotations.Nullable;

/**
 A multipart request part from the Netty-backed test harness.
 */
public final class TestPart {
    private final String name;
    private final @Nullable String contentType;
    private final @Nullable String submittedFileName;
    private final long size;
    private final byte[] bodyBytes;

    TestPart(String name, @Nullable String contentType, @Nullable String submittedFileName, long size, byte[] bodyBytes) {
        this.name = name;
        this.contentType = contentType;
        this.submittedFileName = submittedFileName;
        this.size = size;
        this.bodyBytes = bodyBytes;
    }

    public String name() {
        return name;
    }

    public @Nullable String contentType() {
        return contentType;
    }

    public @Nullable String submittedFileName() {
        return submittedFileName;
    }

    public long size() {
        return size;
    }

    public byte[] bodyBytes() {
        return bodyBytes;
    }
}
