# jsoup Changelog

## 1.18.2 (Pending)

### Improvements

* Optimized the throughput and memory use throughout the input read and parse flows, with heap allocations and GC 
  down between -6% and -89%, and throughput improved up to +143% for small inputs. Most inputs sizes will see 
  throughput increases of ~ 20%. These performance improvements come through recycling the backing byte[] and char[] 
  arrays used to read and parse the input. [2186](https://github.com/jhy/jsoup/pull/2186) 
* Speed optimized `html()` and `Entities.escape()` when the input contains UTF characters in a supplementary plane, by
  around 49%. [2183](https://github.com/jhy/jsoup/pull/2183)
* The form associated elements returned by `FormElement.elements()` now reflect changes made to the DOM,
  subsequently to the original parse. [2140](https://github.com/jhy/jsoup/issues/2140)
* In the `TreeBuilder`, the `onNodeInserted()` and `onNodeClosed()` events are now also fired for the outermost /
  root `Document` node. This enables source position tracking on the Document node (which was previously unset). And
  it also enables the node traversor to see the outer Document node. [2182](https://github.com/jhy/jsoup/pull/2182)

### Bug Fixes

* `Element.cssSelector()` would fail if the element's class contained a `*`
  character. [2169](https://github.com/jhy/jsoup/issues/2169)
* When tracking source ranges, a text node following an invalid self-closing element may be left
  untracked. [2175](https://github.com/jhy/jsoup/issues/2175)
* When a document has no doctype, or a doctype not named `html`, it should be parsed in Quirks
  Mode. [2197](https://github.com/jhy/jsoup/issues/2197)
* With a selector like `div:has(span + a)`, the `has()` component was not working correctly, as the inner combining
  query caused the evaluator to match those against the outer's siblings, not
  children. [2187](https://github.com/jhy/jsoup/issues/2187)
* A selector query that included multiple `:has()` components in a nested `:has()` might incorrectly
  execute. [2131](https://github.com/jhy/jsoup/issues/2131)
* Updated the simple view of cookies available via `Connection.Response#cookies()` to reflect the contents of the 
  current cookie jar for the current URL. [1831](https://github.com/jhy/jsoup/issues/1831)

## 1.18.1 (2024-Jul-10)

### Improvements

* **Stream Parser**: A `StreamParser` provides a progressive parse of its input. As each `Element` is completed, it is
  emitted via a `Stream` or `Iterator` interface. Elements returned will be complete with all their children, and an
  (empty) next sibling, if applicable. Elements (or their children) may be removed from the DOM during the parse,
  for e.g. to conserve memory, providing a mechanism to parse an input document that would otherwise be too large to fit
  into memory, yet still providing a DOM interface to the document and its elements. Additionally, the parser provides
  a `selectFirst(String query)` / `selectNext(String query)`, which will run the parser until a hit is found, at which
  point the parse is suspended. It can be resumed via another `select()` call, or via the `stream()` or `iterator()`
  methods. [2096](https://github.com/jhy/jsoup/pull/2096)
* **Download Progress**: added a Response Progress event interface, which reports progress and URLs are downloaded (and
  parsed). Supported on both a session and a single connection
  level. [2164](https://github.com/jhy/jsoup/pull/2164), [656](https://github.com/jhy/jsoup/issues/656)
* Added `Path` accepting parse methods: `Jsoup.parse(Path)`, `Jsoup.parse(path, charsetName, baseUri, parser)`,
  etc. [2055](https://github.com/jhy/jsoup/pull/2055)
* Updated the `button` tag configuration to include a space between multiple button elements in the `Element.text()`
  method. [2105](https://github.com/jhy/jsoup/issues/2105)
* Added support for the `ns|*` all elements in namespace Selector. [1811](https://github.com/jhy/jsoup/issues/1811)
* When normalising attribute names during serialization, invalid characters are now replaced with `_`, vs being
  stripped. This should make the process clearer, and generally prevent an invalid attribute name being coerced
  unexpectedly. [2143](https://github.com/jhy/jsoup/issues/2143)

### Changes

* Removed previously deprecated internal classes and methods. [2094](https://github.com/jhy/jsoup/pull/2094)
* Build change: the built jar's OSGi manifest no longer imports itself. [2158](https://github.com/jhy/jsoup/issues/2158)

### Bug Fixes

* When tracking source positions, if the first node was a TextNode, its position was incorrectly set
  to `-1.` [2106](https://github.com/jhy/jsoup/issues/2106)
* When connecting (or redirecting) to URLs with characters such as `{`, `}` in the path, a Malformed URL exception would
  be thrown (if in development), or the URL might otherwise not be escaped correctly (if in
  production). The URL encoding process has been improved to handle these characters
  correctly. [2142](https://github.com/jhy/jsoup/issues/2142)
* When using `W3CDom` with a custom output Document, a Null Pointer Exception would be
  thrown. [2114](https://github.com/jhy/jsoup/pull/2114)
* The `:has()` selector did not match correctly when using sibling combinators (like
  e.g.: `h1:has(+h2)`). [2137](https://github.com/jhy/jsoup/issues/2137)
* The `:empty` selector incorrectly matched elements that started with a blank text node and were followed by 
  non-empty nodes, due to an incorrect short-circuit. [2130](https://github.com/jhy/jsoup/issues/2130) 
* `Element.cssSelector()` would fail with "Did not find balanced marker" when building a selector for elements that had
  a `(` or `[` in their class names. And selectors with those characters escaped would not match as
  expected. [2146](https://github.com/jhy/jsoup/issues/2146)
* Updated `Entities.escape(string)` to make the escaped text suitable for both text nodes and attributes (previously was
  only for text nodes). This does not impact the output of `Element.html()` which correctly applies a minimal escape
  depending on if the use will be for text data or in a quoted
  attribute. [1278](https://github.com/jhy/jsoup/issues/1278)
* Fuzz: a Stack Overflow exception could occur when resolving a crafted `<base href>` URL, in the normalizing regex.
  [2165](https://github.com/jhy/jsoup/issues/2165)

---

## 1.17.2 (2023-Dec-29)

### Improvements

* **Attribute object accessors**: Added `Element.attribute(String)` and `Attributes.attribute(String)` to more simply
  obtain an `Attribute` object. [2069](https://github.com/jhy/jsoup/issues/2069)
* **Attribute source tracking**: If source tracking is on, and an Attribute's key is changed (
  via `Attribute.setKey(String)`), the source range is now still tracked
  in `Attribute.sourceRange()`. [2070](https://github.com/jhy/jsoup/issues/2070)
* **Wildcard attribute selector**: Added support for the `[*]` element with any attribute selector. And also restored
  support for selecting by an empty attribute name prefix (`[^]`). [2079](https://github.com/jhy/jsoup/issues/2079)

### Bug Fixes

* **Mixed-cased source position**: When tracking the source position of attributes, if the source attribute name was
  mix-cased but the parser was lower-case normalizing attribute names, the source position for that attribute was not
  tracked correctly. [2067](https://github.com/jhy/jsoup/issues/2067)
* **Source position NPE**: When tracking the source position of a body fragment parse, a null pointer
  exception was thrown. [2068](https://github.com/jhy/jsoup/issues/2068)
* **Multi-point emoji entity**: A multi-point encoded emoji entity may be incorrectly decoded to the replacement
  character. [2074](https://github.com/jhy/jsoup/issues/2074)
* **Selector sub-expressions**: (Regression) in a selector like `parent [attr=va], other`, the `, OR` was binding
  to `[attr=va]` instead of `parent [attr=va]`, causing incorrect selections. The fix includes a EvaluatorDebug class
  that generates a sexpr to represent the query, allowing simpler and more thorough query parse
  tests. [2073](https://github.com/jhy/jsoup/issues/2073)
* **XML CData output**: When generating XML-syntax output from parsed HTML, script nodes containing (pseudo) CData
  sections would have an extraneous CData section added, causing script execution errors. Now, the data content is
  emitted in a HTML/XML/XHTML polyglot format, if the data is not already within a CData
  section. [2078](https://github.com/jhy/jsoup/issues/2078)
* **Thread safety**: The `:has` evaluator held a non-thread-safe Iterator, and so if an Evaluator object was
  shared across multiple concurrent threads, a NoSuchElement exception may be thrown, and the selected results may be
  incorrect. Now, the iterator object is a thread-local. [2088](https://github.com/jhy/jsoup/issues/2088)

---
Older changes for versions 0.1.1 (2010-Jan-31) through 1.17.1 (2023-Nov-27) may be found in
[change-archive.txt](./change-archive.txt).
