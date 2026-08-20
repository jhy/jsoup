# jsoup: Java HTML Parser

**jsoup** is a Java library that makes it easy to work with real-world HTML and XML. It offers an easy-to-use API for URL fetching, data parsing, extraction, and manipulation using DOM API methods, CSS, and xpath selectors.

**jsoup** implements the [WHATWG HTML5](https://html.spec.whatwg.org/multipage/syntax.html) specification, and parses HTML to the same DOM as modern browsers.

- [Scrape and parse HTML](https://jsoup.org/cookbook/input/parse-document-from-string) from a URL, file, or string.
- [Find and extract data](https://jsoup.org/cookbook/extracting-data/selector-syntax) using DOM traversal or CSS selectors.
- [Manipulate HTML elements](https://jsoup.org/cookbook/modifying-data/set-html), attributes, and text.
- [Clean user-submitted content](https://jsoup.org/cookbook/cleaning-html/safelist-sanitizer) against a safelist to prevent XSS attacks.
- [Output tidy HTML](https://jsoup.org/apidocs/org/jsoup/nodes/Element.html#html()).

jsoup is designed to deal with all varieties of HTML found in the wild; from pristine and validating, to invalid tag-soup; jsoup will create a sensible parse tree.

See [**jsoup.org**](https://jsoup.org/) for downloads and the full [API documentation](https://jsoup.org/apidocs/).

[![Build Status](https://github.com/jhy/jsoup/workflows/Build/badge.svg)](https://github.com/jhy/jsoup/actions?query=workflow%3ABuild)

## Example
Fetch the [Wikipedia homepage](https://en.wikipedia.org/wiki/Main_Page), parse it to a [DOM](https://developer.mozilla.org/en-US/docs/Web/API/Document_Object_Model/Introduction), and select the headlines from the *In the news* section into a list of [Elements](https://jsoup.org/apidocs/org/jsoup/select/Elements.html) ([online sample](https://try.jsoup.org/~LGB7rk_atM2roavV0d-czMt3J_g), [full source](https://github.com/jhy/jsoup/blob/master/src/main/java/org/jsoup/examples/Wikipedia.java)):

```java
Document doc = Jsoup.connect("https://en.wikipedia.org/").get();
log(doc.title());
Elements newsHeadlines = doc.select("#mp-itn b a");
for (Element headline : newsHeadlines) {
    log("%s\n\t%s",
        headline.attr("title"), headline.absUrl("href"));
}
```

## Open Source
jsoup is an open source project distributed under the liberal [MIT license](https://jsoup.org/license). The source code is available on [GitHub](https://github.com/jhy/jsoup).

## Getting Started
1. [**Download**](https://jsoup.org/download) the [latest](https://github.com/jhy/jsoup/releases/tag/jsoup-1.23.1) jsoup jar (or add it to your Maven/Gradle build)
2. Read the [cookbook introduction](https://jsoup.org/cookbook/introduction/parsing-a-document).
3. Enjoy!

Maven:

```xml
<dependency>
  <!-- jsoup HTML parser library @ https://jsoup.org/ -->
  <groupId>org.jsoup</groupId>
  <artifactId>jsoup</artifactId>
  <version>1.23.1</version>
</dependency>
```

Gradle:

```groovy
// jsoup HTML parser library @ https://jsoup.org/
implementation 'org.jsoup:jsoup:1.23.1'
```

### Android Support
When used in Android projects, [core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring) with the [NIO specification](https://developer.android.com/studio/write/java11-nio-support-table) should be enabled to support Java 8+ features.

## Development and Support
If you have any questions on how to use jsoup or have ideas for future development, please get in touch via [jsoup Discussions](https://github.com/jhy/jsoup/discussions).

If you find any issues, please file a [bug](https://jsoup.org/bugs) after checking for duplicates.

The [colophon](https://jsoup.org/colophon) talks about the history of and tools used to build jsoup.

## Status
jsoup is in general, stable release.

## Author
jsoup was created and is maintained by [Jonathan Hedley](//jhedley.com), its primary author.

jsoup is an open-source project, and many contributors have helped improve it over the years. You can see their contributions and join the development on [GitHub](https://github.com/jhy/jsoup/graphs/contributors).

## Citing jsoup
If you use jsoup in research or technical documentation, you can cite it as:

> **Jonathan Hedley & jsoup contributors. jsoup: Java HTML Parser (2009–present).** Available at: https://jsoup.org

```plaintext
@misc{jsoup,
  author = {Jonathan Hedley and jsoup contributors},
  title = {jsoup: Java HTML Parser},
  year = {2026},
  url = {https://jsoup.org}
}
```
