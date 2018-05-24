package org.jsoup.hamcrest;


import static org.jsoup.hamcrest.DocumentMatchers.hasTitle;
import static org.jsoup.hamcrest.fluent.DocumentAssertions.anElement;
import static org.jsoup.hamcrest.fluent.DocumentAssertions.eachElement;
import static org.jsoup.hamcrest.fluent.DocumentAssertions.elements;
import static org.jsoup.hamcrest.fluent.JsoupAssertions.html;

import org.junit.Test;

public class FluentExampleTest extends HtmlBaseTest {

    @Test
    public void form_contains_a_csrf_token_hidden_field_jsoup() {
        html(source)
                .expect(anElement("form input[name=_csrf]").exists());
    }

    @Test
    public void form_has_a_submit_button() {
        html(source)
                .expect(elements("form input[type=submit], form button[type=submit]").hasCount(1));
    }

    @Test
    public void form_every_non_hidden_input_has_the_correct_class() {
        html(source)
                .expect(eachElement("form input:not([type=hidden])").hasCssClass("form-control"));
    }


    @Test
    public void form_every_non_hidden_input_has_the_correct_class_alternative() {
        html(source)
                .expect(elements("form input:not([type=hidden])").hasCount(3).each().hasCssClass("form-control"));
    }

    @Test
    public void form_every_non_hidden_input_has_corresponding_label() {
        html(source).expect(eachElement("form input:not([type=hidden])").predicate(
                element -> !element.id().isEmpty()
                        && element.parent().selectFirst("label[for=" + element.id() + "]") != null,
                description -> description.appendText("an input with corresponding label"),
                (element, description) -> description.appendText("element ").appendValue(element.cssSelector())
                        .appendText(" does not have a corresponding label.")
        ));
    }

    @Test
    public void document_title() {
        html(source)
                .expect(hasTitle("Demo"));
    }

    @Test
    public void multiple_assertions_combined() {
        html(source)
                .expect(anElement("form input[name=_csrf]").exists())
                .and(elements("form input[type=submit], form button[type=submit]").hasCount(1))
                .and(eachElement("form input:not([type=hidden])").hasCssClass("form-control"))
                .and(hasTitle("Demo"));
    }
}
