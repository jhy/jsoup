package org.jsoup.hamcrest;

import org.hamcrest.DiagnosingMatcher;

/**
 * Internal API
 */
public abstract class CssMatcher<T> extends DiagnosingMatcher<T> implements DocumentMatcher {
    private final String css;

    CssMatcher(String css) {
        this.css = css;
    }

    String getCss() {
        return css;
    }
}
