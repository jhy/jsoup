package org.jsoup.internal;

/**
 jsoup constants used between packages. Do not use as they may change without warning. Users will not be able to see
 this package when modules are enabled.
 */
public final class SharedConstants {
    public static final String UserDataKey = "/jsoup.userdata";
    public final static String AttrRangeKey = "jsoup.attrs";
    public static final String RangeKey = "jsoup.start";
    public static final String EndRangeKey = "jsoup.end";

    public static final int DefaultBufferSize = 8 * 1024;

    public static final String[] FormSubmitTags = {
        "input", "keygen", "object", "select", "textarea"
    };

    private SharedConstants() {}
}
