package org.jsoup.hamcrest;


import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.jsoup.hamcrest.DocumentMatchers.hasTitle;
import static org.jsoup.hamcrest.ElementMatchers.predicate;
import static org.jsoup.hamcrest.fluent.DocumentAssertions.anElement;
import static org.jsoup.hamcrest.fluent.JsoupAssertions.html;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class AllMatcherHappyTest extends HtmlBaseTest {


    @Test
    public void hasValue() {
        html(source)
                .expect(anElement("form input[name=_csrf]")
                        .hasValue("5ee91155-9809-4630-81a5-47d478eccd11")
                        .hasValue(equalTo("5ee91155-9809-4630-81a5-47d478eccd11")));
    }

    @Test
    public void hasAttribute() {
        html(source)
                .expect(anElement("#exampleInputEmail")
                        .hasAttribute("placeholder")
                        .hasAttribute("placeholder", "Email")
                        .hasAttribute("placeholder", equalTo("Email")));
    }

    @Test
    public void hasCssClass() {
        html(source)
                .expect(anElement("button[type=submit]")
                        .hasCssClass("btn-default")
                        .hasCss(hasItem("btn")));
    }

    @Test
    public void hasText() {
        html(source)
                .expect(anElement(".content > h1")
                        .hasText("This is a demo site")
                        .hasText(containsString("demo")));
    }

    @Test
    public void hasOwnText() {
        html(source)
                .expect(anElement(".content > h1")
                        .hasOwnText("This is a site")
                        .hasOwnText(equalTo("This is a site")));
    }

    @Test
    public void hasData() {
        html(source)
                .expect(anElement("script[language=JavaScript]")
                        .hasData("\n        var xsrf_token = \"5ee91155-9809-4630-81a5-47d478eccd11\";\n    ")
                        .hasData(containsString("5ee91155-9809-4630-81a5-47d478eccd11")));
    }

    @Test
    public void custom() {
        html(source)
                .expect(anElement(".content > h1")
                        .matches(predicate(element -> element.child(0).tagName().equals("small"),
                                description -> {
                                }, (element, description) -> {
                                })));
    }

    @Test
    public void hasTitleMatcher() {
        html(source).expect(hasTitle("Demo"));
    }

    @Test
    public void existingDocument() {
        Document document = Jsoup.parse(source);
        html(document).expect(hasTitle("Demo"));
    }
}
