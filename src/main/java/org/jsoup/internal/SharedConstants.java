package org.jsoup.internal;

/**
 jsoup constants used between packages. Do not use as they may change without warning. Users will not be able to see
 this package when modules are enabled.
 */
public final class SharedConstants {
    // Indicates a jsoup internal key. Can't be set via HTML. (It could be set via accessor, but not too worried about
    // that. Suppressed from list, iter.
    public static final char    InternalPrefix  = '/';
    public static final String  PrivatePrefix   = "/jsoup.";

    public static final String  AttrRange     = PrivatePrefix + "attrRange.";

    private SharedConstants() {}
}
