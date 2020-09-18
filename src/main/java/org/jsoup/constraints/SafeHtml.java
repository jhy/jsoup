package org.jsoup.constraints;

import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.jsoup.constraints.SafeHtml.List;
import org.jsoup.validators.SafeHtmlValidator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.jsoup.constraints.SafeHtml.WhiteListType.RELAXED;

/**
 * Validate a rich text value provided by the user to ensure that it contains no malicious code, such as embedded
 * &lt;script&gt; elements.
 * <p>
 * Note that this constraint assumes you want to validate input which represents a body fragment of an HTML document. If
 * you instead want to validate input which represents a complete HTML document, add the {@code html}, {@code head} and
 * {@code body} tags to the used whitelist as required.
 *
 * @author George Gastaldi
 * @author Craig Andrews
 */
@Documented
@Constraint(validatedBy = { SafeHtmlValidator.class })
@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
@Retention(RUNTIME)
@Repeatable(List.class)
public @interface SafeHtml {

	String message() default "{org.jsoup.constraints.SafeHtml.message}";

	Class<?>[] groups() default { };

	Class<? extends Payload>[] payload() default { };

	/**
	 * @return The built-in whitelist type which will be applied to the rich text value
	 */
	WhiteListType whitelistType() default RELAXED;

	/**
	 * @return Additional whitelist tags which are allowed on top of the tags specified by the
	 * {@link #whitelistType()}.
	 */
	String[] additionalTags() default { };

	/**
	 * @return Allows to specify additional whitelist tags with optional attributes and protocols.
	 */
	Tag[] additionalTagsWithAttributes() default { };

	/**
	 * @return Base URI used to resolve relative URIs to absolute ones. If not set, validation
	 * of HTML containing relative URIs will fail.
	 *
	 * @since 6.0
	 */
	String baseURI() default "";

	/**
	 * Allows to specify whitelist tags with specified optional attributes. Adding a tag with a given attribute also
	 * whitelists the tag itself without any attribute.
	 */
	@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
	@Retention(RUNTIME)
	@Documented
	public @interface Tag {

		/**
		 * @return the tag name to whitelist.
		 */
		String name();

		/**
		 * @return list of tag attributes which are whitelisted.
		 */
		String[] attributes() default { };

		/**
		 * @return list of tag attributes with corresponding allowed protocols which are whitelisted.
		 * @since 6.0
		 */
		Attribute[] attributesWithProtocols() default { };
	}

	/**
	 * Allows to specify whitelisted attributes with whitelisted protocols.
	 *
	 * @since 6.0
	 */
	@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
	@Retention(RUNTIME)
	@Documented @interface Attribute {

		/**
		 * @return the attribute name to whitelist.
		 */
		String name();

		/**
		 * @return list of attribute protocols which are whitelisted.
		 */
		String[] protocols();
	}

	/**
	 * Defines several {@code @SafeHtml} annotations on the same element.
	 */
	@Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
	@Retention(RUNTIME)
	@Documented
	public @interface List {
		SafeHtml[] value();
	}

	/**
	 * Defines default whitelist implementations.
	 */
	enum WhiteListType {
		/**
		 * This whitelist allows only text nodes: all HTML will be stripped.
		 */
		NONE,

		/**
		 * This whitelist allows only simple text formatting: {@code b, em, i, strong, u}. All other HTML (tags and
		 * attributes) will be removed.
		 */
		SIMPLE_TEXT,

		/**
		 * This whitelist allows a fuller range of text nodes:
		 * {@code a, b, blockquote, br, cite, code, dd, dl, dt, em, i, li, ol, p, pre, q, small, span, strike, strong, sub,
		 * sup, u, ul}, and appropriate attributes.
		 * <p>
		 * Links ({@code a} elements) can point to {@code http, https, ftp, mailto}, and have an enforced
		 * {@code rel=nofollow} attribute.
		 * </p>
		 * Does not allow images.
		 */
		BASIC,

		/**
		 * This whitelist allows the same text tags as {@link WhiteListType#BASIC}, and also allows {@code img}
		 * tags,
		 * with
		 * appropriate attributes, with {@code src} pointing to {@code http} or {@code https}.
		 */
		BASIC_WITH_IMAGES,

		/**
		 * This whitelist allows a full range of text and structural body HTML:
		 * {@code a, b, blockquote, br, caption, cite, code, col, colgroup, dd, div, dl, dt, em, h1, h2, h3, h4, h5, h6,
		 * i, img, li, ol, p, pre, q, small, span, strike, strong, sub, sup, table, tbody, td, tfoot, th, thead, tr, u,
		 * ul}
		 * <p>
		 * Links do not have an enforced {@code rel=nofollow} attribute, but you can add that if desired.
		 * </p>
		 */
		RELAXED
	}
}
