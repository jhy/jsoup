package org.jsoup.hamcrest;


import static org.hamcrest.CoreMatchers.hasItem;
import static org.jsoup.hamcrest.DocumentMatchers.hasTitle;
import static org.jsoup.hamcrest.fluent.DocumentAssertions.*;
import static org.jsoup.hamcrest.fluent.JsoupAssertions.html;

import org.junit.Test;

public class AllMatcherFailTest extends HtmlBaseTest {

    @Test(expected = AssertionError.class)
    public void exists() {
        html(source)
                .expect(anElement("foo").exists());
    }

    @Test(expected = AssertionError.class)
    public void tooMany() {
        html(source)
                .expect(anElement("input").exists());
    }

    @Test(expected = AssertionError.class)
    public void hasValue() {
        html(source)
                .expect(anElement("form input[name=_csrf]")
                        .hasValue("5ee91155-9809-4630"));
    }

    @Test(expected = AssertionError.class)
    public void hasAttributeExits() {
        html(source)
                .expect(anElement("#exampleInputEmail")
                        .hasAttribute("readonly"));
    }

    @Test(expected = AssertionError.class)
    public void hasAttribute() {
        html(source)
                .expect(anElement("#exampleInputEmail")
                        .hasAttribute("placeholder", "Foo"));
    }

    @Test(expected = AssertionError.class)
    public void hasCssClass() {
        html(source)
                .expect(anElement("button[type=submit]")
                        .hasCssClass("btn-primary"));
    }

    @Test(expected = AssertionError.class)
    public void hasCss() {
        html(source)
                .expect(anElement("button[type=submit]")
                        .hasCss(hasItem("btn-primary")));
    }

    @Test(expected = AssertionError.class)
    public void hasText() {
        html(source)
                .expect(anElement(".content > h1")
                        .hasText("This is a production site"));
    }

    @Test(expected = AssertionError.class)
    public void hasOwnText() {
        html(source)
                .expect(anElement(".content > h1")
                        .hasOwnText("This is not a site"));
    }

    @Test(expected = AssertionError.class)
    public void hasData() {
        html(source)
                .expect(anElement("script[language=JavaScript]")
                        .hasData("5ee91155-9809-4630-81a5-47d478eccd11"));
    }

    @Test(expected = AssertionError.class)
    public void eachElementFail() {
        html(source)
                .expect(eachElement("script[language=JavaScript]")
                        .hasData("5ee91155-9809-4630-81a5-47d478eccd11"));
    }

    @Test(expected = AssertionError.class)
    public void elementsFail() {
        html(source)
                .expect(elements("script[language=JavaScript]").hasCount(2));
    }

    @Test(expected = AssertionError.class)
    public void hasTitleFail() {
        html(source).expect(hasTitle("Fail"));
    }

    @Test(expected = AssertionError.class)
    public void elementsEachFail() {
        html(source)
                .expect(elements("script[language=JavaScript]")
                        .each().hasData("5ee91155-9809-4630-81a5-47d478eccd11"));
    }
}
