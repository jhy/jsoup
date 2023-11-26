package org.jsoup.internal;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 @deprecated Previously indicated that all components (methods, returns, fields) are not nullable, unless otherwise specified by @Nullable.
 */
@Deprecated
@Documented
@NullMarked
@Retention(value = RetentionPolicy.CLASS)
public @interface NonnullByDefault {
}
