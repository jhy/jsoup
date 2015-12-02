# jsoup: Java HTML parser that makes sense of real-world HTML soup.

**jsoup** is a Java library for working with real-world HTML. It provides a very convenient API for extracting and manipulating data, using the best of DOM, CSS, and jquery-like methods.

## What is jsoup for?

**jsoup** implements the [WHATWG HTML5 specification](http://whatwg.org/html), and parses HTML to the same DOM as modern browsers do.

* parse HTML from a URL, file, or string
* find and extract data, using DOM traversal or CSS selectors
* manipulate the HTML elements, attributes, and text
* clean user-submitted content against a safe white-list, to prevent XSS
* output tidy HTML

**jsoup** is designed to deal with all varieties of HTML found in the wild; from pristine and validating, to invalid tag-soup; **jsoup** will create a sensible parse tree.

## Installation

**Requirements**: jsoup runs on Java 1.5 and up.

### Using Maven

Add this into your `pom.xml`,

```xml
<dependency>
  <!-- jsoup HTML parser library @ http://jsoup.org/ -->
  <groupId>org.jsoup</groupId>
  <artifactId>jsoup</artifactId>
  <version>1.8.3</version>
</dependency>
```

### Manually

If you prefer to manually install the library, click to download the JAR file,

* [jsoup-1.8.3.jar](https://jsoup.org/packages/jsoup-1.8.3.jar) core library.
* [jsoup-1.8.3-sources.jar](https://jsoup.org/packages/jsoup-1.8.3-sources.jar) optional sources jar.
* [jsoup-1.8.3-javadoc.jar](https://jsoup.org/packages/jsoup-1.8.3-javadoc.jar) optional javadoc jar.

Also, make sure to visit the [download section](https://jsoup.org/download) to get the latest release version.

## Documentation

The officially generated API documentation for **jsoup** is located at [jsoup API page](https://jsoup.org/apidocs/).

You can also generate the documentation from source using `javadoc`,

```
$ cd jsoup

$ javadoc -d apidocs -sourcepath ./src/main/java -subpackages org.jsoup
```

## References

See http://jsoup.org/ for downloads and documentation.
