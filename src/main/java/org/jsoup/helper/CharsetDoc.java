package org.jsoup.helper;

import org.jsoup.nodes.Document;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * A struct to return a detected charset, and a document (if fully read).
 */
class CharsetDoc {
    Charset charset;
    InputStream input;
    @Nullable Document doc;

    CharsetDoc(Charset charset, @Nullable Document doc, InputStream input) {
        this.charset = charset;
        this.input = input;
        this.doc = doc;
    }
}
