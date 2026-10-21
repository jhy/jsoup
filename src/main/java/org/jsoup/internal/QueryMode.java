package org.jsoup.internal;

/**
 Selects query behavior during migration to Web DOM semantics.
 Defaults to Legacy in 1.24.1 and Web in 1.25.1.
 Shall be removed in 1.26.1, when only Web behavior remains.
 Public for access between jsoup packages; not a supported user API.
 */
public enum QueryMode {
    Legacy,
    Web,
    /**
     Returns legacy query results and reports differences from Web mode.
     */
    Check;

    /**
     Reads the application query mode from {@value SharedConstants#QueryMode}.
     */
    public static QueryMode current() {
        String value = System.getProperty(SharedConstants.QueryMode, "legacy");
        for (QueryMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) return mode;
        }
        throw new IllegalArgumentException(String.format(
            "Invalid %s: %s; expected legacy, web, or check", SharedConstants.QueryMode, value));
    }
}
