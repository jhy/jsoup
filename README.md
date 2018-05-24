# jsoup: Java HTML Parser

**jsoup** is a Java library for working with real-world HTML. It provides a very convenient API for extracting and manipulating data, using the best of DOM, CSS, and jquery-like methods.


**jsoup** implements the [WHATWG HTML5](http://whatwg.org/html) specification, and parses HTML to the same DOM as modern browsers do.

* scrape and [parse](https://jsoup.org/cookbook/input/parse-document-from-string) HTML from a URL, file, or string
* find and [extract data](https://jsoup.org/cookbook/extracting-data/selector-syntax), using DOM traversal or CSS selectors
* manipulate the [HTML elements](https://jsoup.org/cookbook/modifying-data/set-html), attributes, and text
* [clean](https://jsoup.org/cookbook/cleaning-html/whitelist-sanitizer) user-submitted content against a safe white-list, to prevent XSS attacks
* output tidy HTML

jsoup is designed to deal with all varieties of HTML found in the wild; from pristine and validating, to invalid tag-soup; jsoup will create a sensible parse tree.

See [**jsoup.org**](https://jsoup.org/) for downloads and the full [API documentation](https://jsoup.org/apidocs/).

## Example
Fetch the [Wikipedia](http://en.wikipedia.org/wiki/Main_Page) homepage, parse it to a [DOM](https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model/Introduction), and select the headlines from the *In the News* section into a list of [Elements](https://jsoup.org/apidocs/index.html?org/jsoup/select/Elements.html):

```java
Document doc = Jsoup.connect("http://en.wikipedia.org/").get();
log(doc.title());
Elements newsHeadlines = doc.select("#mp-itn b a");
for (Element headline : newsHeadlines) {
  log("%s\n\t%s", 
    headline.attr("title"), headline.absUrl("href"));
}
```
[Online sample](https://try.jsoup.org/~LGB7rk_atM2roavV0d-czMt3J_g), [full source](https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/Wikipedia.java).

## Use in Testing
With `jsoup-hamcrest` you can use powerful easy to read assertions in your tests.

```java
@Test
public void form_every_non_hidden_input_has_the_correct_class() {
    html(source)
            .expect(elements("form input:not([type=hidden])").hasCount(3).each().hasCssClass("form-control"));
}

@Test
public void multiple_assertions_combined() {
    html(source)
            .expect(anElement("form input[name=_csrf]").exists())
            .and(elements("form input[type=submit], form button[type=submit]").hasCount(1))
            .and(eachElement("form input:not([type=hidden])").hasCssClass("form-control"))
            .and(hasTitle("Demo"));
}
```

For more examples see [Fluent Examples](jsoup-hamcrest/src/test/java/org/jsoup/hamcrest/FluentExampleTest.java) or
[All Matchers Examples](jsoup-hamcrest/src/test/java/org/jsoup/hamcrest/AllMatcherHappyTest.java).

Note that `jsoup-hamcrest` requires Java 8 or higher.

## Open source
jsoup is an open source project distributed under the liberal [MIT license](https://jsoup.org/license). The source code is available at [GitHub](https://github.com/jhy/jsoup/tree/master/src/main/java/org/jsoup).

## Getting started
1. [Download](https://jsoup.org/download) the latest jsoup jar (or add it to your Maven/Gradle build)
2. Read the [cookbook](https://jsoup.org/cookbook/)
3. Enjoy!

## Development and support
If you have any questions on how to use jsoup, or have ideas for future development, please get in touch via the [mailing list](https://jsoup.org/discussion).

If you find any issues, please file a [bug](https://jsoup.org/bugs) after checking for duplicates.

The [colophon](https://jsoup.org/colophon) talks about the history of and tools used to build jsoup.

## Status
jsoup is in general, stable release.
