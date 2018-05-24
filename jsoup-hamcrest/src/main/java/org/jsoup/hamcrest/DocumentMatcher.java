package org.jsoup.hamcrest;

import org.jsoup.nodes.Document;

/**
 * Internal API
 */
public interface DocumentMatcher {

    void match(Document document);
}
