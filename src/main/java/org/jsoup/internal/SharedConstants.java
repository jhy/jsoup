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
    public static final String XmlnsAttr = "jsoup.xmlns-";

    public static final int DefaultBufferSize = 8 * 1024;

    public static final String[] FormSubmitTags = {
        "input", "keygen", "object", "select", "textarea"
    };

    public static final String DummyUri = "https://dummy.example/"; // used as a base URI if none provided, to allow abs url resolution to preserve relative links

    public static final String UseHttpClient = "jsoup.useHttpClient";

    public static final String UseRe2j = "jsoup.useRe2j"; // enables use of the re2j regular expression engine when true and it's on the classpath

    private SharedConstants() {}
}
