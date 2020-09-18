package org.jsoup.validators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.jsoup.constraints.SafeHtml;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SafeHtml}
 *
 * @author Craig Andrews
 *
 */
public class SafeHtmlValidatorTest {
	private static final Validator VALIDATOR = Validation
		.byDefaultProvider()
		.configure()
		.buildValidatorFactory()
		.getValidator();
	
	public static class SafeHtmlBean {
		@SafeHtml
		public final String html;

		public SafeHtmlBean(final @SafeHtml String html) {
			super();
			this.html = html;
		}
	}

	@Test public void safeHtmlValid(){
		Set<ConstraintViolation<SafeHtmlBean>> violations = VALIDATOR.validate(new SafeHtmlBean("test"));
		assertTrue(violations.isEmpty());
	}

	@Test public void safeHtmlNotValid(){
		Set<ConstraintViolation<SafeHtmlBean>> violations = VALIDATOR.validate(new SafeHtmlBean("<script></script>"));
		assertEquals(1, violations.size());
		ConstraintViolation<SafeHtmlBean> violation = violations.iterator().next();
		assertEquals(SafeHtml.class, violation.getConstraintDescriptor().getAnnotation().annotationType());
	}
}
