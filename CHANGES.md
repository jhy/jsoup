# jsoup Changelog

## 1.22.1 (PENDING)

### Improvements
* Added an instance method `Parser#unescape(String, boolean)` that unescapes HTML entities using the parser’s configuration (e.g. to support error tracking), complementing the existing static utility `Parser.unescapeEntities(String, boolean)`. [#2396](https://github.com/jhy/jsoup/pull/2396)

### Bug Fixes
* Previously cached child Elements of an Element were not correctly invalidated in `Node#replaceWith(Node)`, which could lead to incorrect results when subsequently calling `Element#children()`. [#2391](https://github.com/jhy/jsoup/issues/2391)
* Attribute selector values are now compared literally without trimming. Previously, jsoup trimmed whitespace from selector values and from element attribute values, which could cause mismatches with browser behavior (e.g. `[attr=" foo "]`). Now matches align with the CSS specification and browser engines. [#2380](https://github.com/jhy/jsoup/issues/2380)
* When using the JDK HttpClient, any system default proxy (`ProxySelector.getDefault()`) was ignored. Now, the system proxy is used if a per-request proxy is not set. [#2388](https://github.com/jhy/jsoup/issues/2388), [#2390](https://github.com/jhy/jsoup/pull/2390)
* A ValidationException could be thrown in the adoption agency algorithm with particularly broken input. Now logged as a parse error. [#2393](https://github.com/jhy/jsoup/issues/2393)
* Null characters in the HTML body were not consistently removed; and in foreign content were not correctly replaced. [#2395](https://github.com/jhy/jsoup/issues/2395)
* An IndexOutOfBoundsException could be thrown when parsing a body fragment with crafted input. Now logged as a parse error. [#2397](https://github.com/jhy/jsoup/issues/2397)


## 1.21.2 (2025-Aug-25)

### Changes
* Deprecated internal (yet visible) methods `Normalizer#normalize(String, bool)` and `Attribute#shouldCollapseAttribute(Document.OutputSettings)`. These will be removed in a future version.
* Deprecated `Connection#sslSocketFactory(SSLSocketFactory)` in favor of the new `Connection#sslContext(SSLContext)`. Using `sslSocketFactory` will force the use of the legacy `HttpUrlConnection` implementation, which does not support HTTP/2. [#2370](https://github.com/jhy/jsoup/pull/2370)

### Improvements
* When pretty-printing, if there are consecutive text nodes (via DOM manipulation), the non-significant whitespace between them will be collapsed. [#2349](https://github.com/jhy/jsoup/pull/2349).
* Updated `Connection.Response#statusMessage()` to return a simple loggable string message (e.g. "OK") when using the `HttpClient` implementation, which doesn't otherwise return any server-set status message. [#2356](https://github.com/jhy/jsoup/issues/2346) 
* `Attributes#size()` and `Attributes#isEmpty()` now exclude any internal attributes (such as user data) from their count. This aligns with the attributes' serialized output and iterator. [#2369](https://github.com/jhy/jsoup/pull/2369)
* Added `Connection#sslContext(SSLContext)` to provide a custom SSL (TLS) context to requests, supporting both the `HttpClient` and the legacy `HttUrlConnection` implementations. [#2370](https://github.com/jhy/jsoup/pull/2370)
* Performance optimizations for DOM manipulation methods including when repeatedly removing an element's first child (`element.child(0).remove()`, and when using `Parser#parseBodyFragement()` to parse a large number of direct children. [#2373](https://github.com/jhy/jsoup/pull/2373).

### Bug Fixes
* When parsing from an InputStream and a multibyte character happened to straddle a buffer boundary, the stream would not be completely read. [#2353](https://github.com/jhy/jsoup/issues/2353).
* In `NodeTraversor`, if a last child element was removed during the `head()` call, the parent would be visited twice. [#2355](https://github.com/jhy/jsoup/issues/2355).
* Cloning an Element that has an Attributes object would add an empty internal user-data attribute to that clone, which would cause unexpected results for `Attributes#size()` and `Attributes#isEmpty()`. [#2356](https://github.com/jhy/jsoup/issues/2356)
* In a multithreaded application where multiple threads are calling `Element#children()` on the same element concurrently, a race condition could happen when the method was generating the internal child element cache (a filtered view of its child nodes). Since concurrent reads of DOM objects should be threadsafe without external synchronization, this method has been updated to execute atomically. [#2366](https://github.com/jhy/jsoup/issues/2366)
* When parsing HTML with svg:script elements in SVG elements, don't enter the Text insertion mode, but continue to parse as foreign content. Otherwise, misnested HTML could then cause an IndexOutOfBoundsException. [#2374](https://github.com/jhy/jsoup/issues/2374)
* Malformed HTML could throw an IndexOutOfBoundsException during the adoption agency. [#2377](https://github.com/jhy/jsoup/pull/2377).

## 1.21.1 (2025-Jun-23)

### Changes

* Removed previously deprecated methods. [#2317](https://github.com/jhy/jsoup/pull/2317)
* Deprecated the `:matchText` pseduo-selector due to its side effects on the DOM; use the new `::textnode` selector and the `Element#selectNodes(String css, Class type)` method instead. [#2343](https://github.com/jhy/jsoup/pull/2343)
* Deprecated `Connection.Response#bufferUp()` in lieu of `Connection.Response#readFully()` which can throw a checked IOException.
* Deprecated internal methods `Validate#ensureNotNull` (replaced by typed `Validate#expectNotNull`); protected HTML appenders from Attribute and Node.
* If you happen to be using any of the deprecated methods, please take the opportunity now to migrate away from them, as they will be removed in a future release.

### Improvements
* Enhanced the `Selector` to support direct matching against nodes such as comments and text nodes. For example, you can now find an element that follows a specific comment: `::comment:contains(prices) + p` will select `p` elements immediately after a `<!-- prices: -->` comment. Supported types include `::node`, `::leafnode`, `::comment`, `::text`, `::data`, and `::cdata`. Node contextual selectors like `::node:contains(text)`, `:matches(regex)`, and `:blank` are also supported. Introduced `Element#selectNodes(String css)` and `Element#selectNodes(String css, Class nodeType)` for direct node selection. [#2324](https://github.com/jhy/jsoup/pull/2324)
* Added `TagSet#onNewTag(Consumer<Tag> customizer)`: register a callback that’s invoked for each new or cloned Tag when it’s inserted into the set. Enables dynamic tweaks of tag options (for example, marking all custom tags as self-closing, or everything in a given namespace as preserving whitespace).
* Made `TokenQueue` and `CharacterReader` autocloseable, to ensure that they will release their buffers back to the buffer pool, for later reuse.
* Added `Selector#evaluatorOf(String css)`, as a clearer way to obtain an Evaluator from a CSS query. An alias of `QueryParser.parse(String css)`.
* Custom tags (defined via the `TagSet`) in a foreign namespace (e.g. SVG) can be configured to parse as data tags.
* Added `NodeVisitor#traverse(Node)` to simplify node traversal calls (vs. importing `NodeTraversor`).
* Updated the default user-agent string to improve compatibility. [#2341](https://github.com/jhy/jsoup/issues/2341) 
* The HTML parser now allows the specific text-data type (Data, RcData) to be customized for known tags. (Previously, that was only supported on custom tags.) [#2326](https://github.com/jhy/jsoup/issues/2326).
* Added `Connection#readFully()` as a replacement for `Connection#bufferUp()` with an explicit IOException. Similarly, added `Connection#readBody()` over `Connection#body()`. Deprecated `Connection#bufferUp()`. [#2327](https://github.com/jhy/jsoup/pull/2327) 
* When serializing HTML, the `<` and `>` characters are now escaped in attributes. This helps prevent a class of mutation XSS attacks. [#2337](https://github.com/jhy/jsoup/pull/2337)
* Changed `Connection` to prefer using the JDK's HttpClient over HttpUrlConnection, if available, to enable HTTP/2 support by default. Users can disable via `-Djsoup.useHttpClient=false`. [#2340](https://github.com/jhy/jsoup/pull/2340)

### Bug Fixes
* The contents of a `script` in a `svg` foreign context should be parsed as script data, not text. [#2320](https://github.com/jhy/jsoup/issues/2320)
* `Tag#isFormSubmittable()` was updating the Tag's options. [#2323](https://github.com/jhy/jsoup/issues/2323)
* The HTML pretty-printer would incorrectly trim whitespace when text followed an inline element in a block element. [#2325](https://github.com/jhy/jsoup/issues/2325)
* Custom tags with hyphens or other non-letter characters in their names now work correctly as Data or RcData tags. Their closing tags are now tokenized properly. [#2332](https://github.com/jhy/jsoup/issues/2332)
* When cloning an Element, the clone would retain the source's cached child Element list (if any), which could lead to incorrect results when modifying the clone's child elements. [#2334](https://github.com/jhy/jsoup/issues/2334)

## 1.20.1 (2025-Apr-29)

### Changes

* To better follow the HTML5 spec and current browsers, the HTML parser no longer allows self-closing tags (`<foo />`)
  to close HTML elements by default. Foreign content (SVG, MathML), and content parsed with the XML parser, still
  supports self-closing tags. If you need specific HTML tags to support self-closing, you can register a custom tag via
  the `TagSet` configured in `Parser.tagSet()`, using `Tag#set(Tag.SelfClose)`. Standard void tags (such as `<img>`,
  `<br>`, etc.) continue to behave as usual and are not affected by this
  change. [#2300](https://github.com/jhy/jsoup/issues/2300).
* The following internal components have been **deprecated**. If you do happen to be using any of these, please take the opportunity now to migrate away from them, as they will be removed in jsoup 1.21.1.
  * `ChangeNotifyingArrayList`, `Document.updateMetaCharsetElement()`, `Document.updateMetaCharsetElement(boolean)`, `HtmlTreeBuilder.isContentForTagData(String)`, `Parser.isContentForTagData(String)`, `Parser.setTreeBuilder(TreeBuilder)`, `Tag.formatAsBlock()`, `Tag.isFormListed()`, `TokenQueue.addFirst(String)`, `TokenQueue.chompTo(String)`, `TokenQueue.chompToIgnoreCase(String)`, `TokenQueue.consumeToIgnoreCase(String)`, `TokenQueue.consumeWord()`, `TokenQueue.matchesAny(String...)`

### Functional Improvements

* Rebuilt the HTML pretty-printer, to simplify and consolidate the implementation, improve consistency, support custom
  Tags, and provide a cleaner path for ongoing improvements. The specific HTML produced by the pretty-printer may be
  different from previous versions. [#2286](https://github.com/jhy/jsoup/issues/2286).
* Added the ability to define custom tags, and to modify properties of known tags, via the `TagSet` tag collection.
  Their properties can impact both the parse and how content is
  serialized (output as HTML or XML). [#2285](https://github.com/jhy/jsoup/issues/2285).
* `Element.cssSelector()` will prefer to return shorter selectors by using ancestor IDs when available and unique. E.g.
  `#id > div > p` instead of  `html > body > div > div > p` [#2283](https://github.com/jhy/jsoup/pull/2283).
* Added `Elements.deselect(int index)`, `Elements.deselect(Object o)`, and `Elements.deselectAll()` methods to remove
  elements from the `Elements` list without removing them from the underlying DOM. Also added `Elements.asList()` method
  to get a modifiable list of elements without affecting the DOM. (Individual Elements remain linked to the
  DOM.) [#2100](https://github.com/jhy/jsoup/issues/2100).
* Added support for sending a request body from an InputStream with
  `Connection.requestBodyStream(InputStream stream)`. [#1122](https://github.com/jhy/jsoup/issues/1122).
* The XML parser now supports scoped xmlns: prefix namespace declarations, and applies the correct namespace to Tags and
  Attributes. Also, added `Tag#prefix()`, `Tag#localName()`, `Attribute#prefix()`, `Attribute#localName()`, and
  `Attribute#namespace()` to retrieve these. [#2299](https://github.com/jhy/jsoup/issues/2299).
* CSS identifiers are now escaped and unescaped correctly to the CSS spec. `Element#cssSelector()` will emit
  appropriately escaped selectors, and the QueryParser supports those. Added `Selector.escapeCssIdentifier()` and
  `Selector.unescapeCssIdentifier()`. [#2297](https://github.com/jhy/jsoup/pull/2297), [#2305](https://github.com/jhy/jsoup/pull/2305)

### Structure and Performance Improvements

* Refactored the CSS `QueryParser` into a clearer recursive descent
  parser. [#2310](https://github.com/jhy/jsoup/pull/2310).
* CSS selectors with consecutive combinators (e.g. `div >> p`) will throw an explicit parse
  exception. [#2311](https://github.com/jhy/jsoup/pull/2311).
* Performance: reduced the shallow size of an Element from 40 to 32 bytes, and the NodeList from 32 to 24. 
  [#2307](https://github.com/jhy/jsoup/pull/2307).
* Performance: reduced GC load of new StringBuilders when tokenizing input
  HTML. [#2304](https://github.com/jhy/jsoup/pull/2304).
* Made `Parser` instances threadsafe, so that inadvertent use of the same instance across threads will not lead to
  errors. For actual concurrency, use `Parser#newInstance()` per
  thread. [#2314](https://github.com/jhy/jsoup/pull/2314).

### Bug Fixes

* Element names containing characters invalid in XML are now normalized to valid XML names when
  serializing. [#1496](https://github.com/jhy/jsoup/issues/1496).
* When serializing to XML, characters that are invalid in XML 1.0 should be removed (not
  encoded). [#1743](https://github.com/jhy/jsoup/issues/1743).
* When converting a `Document` to the W3C DOM in `W3CDom`, elements with an attribute in an undeclared namespace now
  get a declaration of `xmlns:prefix="undefined"`. This allows subsequent serialization to XML via `W3CDom.asString()`
  to succeed. [#2087](https://github.com/jhy/jsoup/issues/2087).
* The `StreamParser` could emit the final elements of a document twice, due to how `onNodeCompleted` was fired when closing out the stack. [#2295](https://github.com/jhy/jsoup/issues/2295).
* When parsing with the XML parser and error tracking enabled, the trailing `?` in `<?xml version="1.0"?>` would
  incorrectly emit an error. [#2298](https://github.com/jhy/jsoup/issues/2298).
* Calling `Element#cssSelector()` on an element with combining characters in the class or ID now produces the correct output. [#1984](https://github.com/jhy/jsoup/issues/1984). 

## 1.19.1 (2025-Mar-04)

### Changes

* Added support for **http/2** requests in `Jsoup.connect()`, when running on Java 11+, via the Java HttpClient
  implementation. [#2257](https://github.com/jhy/jsoup/pull/2257).
  * In this version of jsoup, the default is to make requests via the HttpUrlConnection implementation: use
    **`System.setProperty("jsoup.useHttpClient", "true");`** to enable making requests via the HttpClient instead ,
    which will enable http/2 support, if available. This will become the default in a later version of jsoup, so now is
    a good time to validate it.
  * If you are repackaging the jsoup jar in your deployment (i.e. creating a shaded- or a fat-jar), make sure to specify
    that as a Multi-Release
    JAR.
  * If the `HttpClient` impl is not available in your JRE, requests will continue to be made via
    `HttpURLConnection` (in `http/1.1` mode).
* Updated the minimum Android API Level validation from 10 to **21**. As with previous jsoup versions, Android
  developers need to enable core library desugaring. The minimum Java version remains Java 8.
  [#2173](https://github.com/jhy/jsoup/pull/2173)
* Removed previously deprecated class: `org.jsoup.UncheckedIOException` (replace with `java.io.UncheckedIOException`);
  moved previously deprecated method `Element Element#forEach(Consumer)` to
  `void Element#forEach(Consumer())`. [#2246](https://github.com/jhy/jsoup/pull/2246)
* Deprecated the methods `Document#updateMetaCharsetElement(boolean)` and `Document#updateMetaCharsetElement()`, as the
  setting had no effect. When `Document#charset(Charset)` is called, the document's meta charset or XML encoding
  instruction is always set. [#2247](https://github.com/jhy/jsoup/pull/2247)

### Improvements

* When cleaning HTML with a `Safelist` that preserves relative links, the `isValid()` method will now consider these
  links valid. Additionally, the enforced attribute `rel=nofollow` will only be added to external links when configured
  in the safelist. [#2245](https://github.com/jhy/jsoup/pull/2245)
* Added `Element#selectStream(String query)` and `Element#selectStream(Evaluator)` methods, that return a `Stream` of
  matching elements. Elements are evaluated and returned as they are found, and the stream can be
  terminated early. [#2092](https://github.com/jhy/jsoup/pull/2092)
* `Element` objects now implement `Iterable`, enabling them to be used in enhanced for loops.
* Added support for fragment parsing from a `Reader` via
  `Parser#parseFragmentInput(Reader, Element, String)`. [#1177](https://github.com/jhy/jsoup/issues/1177)
* Reintroduced CLI executable examples, in `jsoup-examples.jar`. [#1702](https://github.com/jhy/jsoup/issues/1702)
* Optimized performance of selectors like `#id .class` (and other similar descendant queries) by around 4.6x, by better
  balancing the Ancestor evaluator's cost function in the query
  planner. [#2254](https://github.com/jhy/jsoup/issues/2254)
* Removed the legacy parsing rules for `<isindex>` tags, which would autovivify a `form` element with labels. This is no
  longer in the spec.
* Added `Elements.selectFirst(String cssQuery)` and `Elements.expectFirst(String cssQuery)`, to select the first
  matching element from an `Elements` list.  [#2263](https://github.com/jhy/jsoup/pull/2263/)
* When parsing with the XML parser, XML Declarations and Processing Instructions are directly handled, vs bouncing
  through the HTML parser's bogus comment handler. Serialization for non-doctype declarations no longer end with a
  spurious `!`. [#2275](https://github.com/jhy/jsoup/pull/2275)
* When converting parsed HTML to XML or the W3C DOM, element names containing `<` are normalized to `_` to ensure valid
  XML. For example, `<foo<bar>` becomes `<foo_bar>`, as XML does not allow `<` in element names, but HTML5
  does. [#2276](https://github.com/jhy/jsoup/pull/2276)
* Reimplemented the HTML5 Adoption Agency Algorithm to the current spec. This handles mis-nested formating / structural elements. [#2278](https://github.com/jhy/jsoup/pull/2278)

### Bug Fixes

* If an element has an `;` in an attribute name, it could not be converted to a W3C DOM element, and so subsequent XPath
  queries could miss that element. Now, the attribute name is more completely
  normalized. [#2244](https://github.com/jhy/jsoup/issues/2244)
* For backwards compatibility, reverted the internal attribute key for doctype names to 
  "name". [#2241](https://github.com/jhy/jsoup/issues/2241)
* In `Connection`, skip cookies that have no name, rather than throwing a validation
  exception. [#2242](https://github.com/jhy/jsoup/issues/2242)
* When running on JDK 1.8, the error `java.lang.NoSuchMethodError: java.nio.ByteBuffer.flip()Ljava/nio/ByteBuffer;`
  could be thrown when calling `Response#body()` after parsing from a URL and the buffer size was
  exceeded. [#2250](https://github.com/jhy/jsoup/pull/2250)
* For backwards compatibility, allow `null` InputStream inputs to `Jsoup.parse(InputStream stream, ...)`, by returning
  an empty `Document`. [#2252](https://github.com/jhy/jsoup/issues/2252)
* A `template` tag containing an `li` within an open `li` would be parsed incorrectly, as it was not recognized as a
  "special" tag (which have additional processing rules). Also, added the SVG and MathML namespace tags to the list of
  special tags. [#2258](https://github.com/jhy/jsoup/issues/2258)
* A `template` tag containing a `button` within an open `button` would be parsed incorrectly, as the "in button scope"
  check was not aware of the `template` element. Corrected other instances including MathML and SVG elements,
  also. [#2271](https://github.com/jhy/jsoup/issues/2271)
* An `:nth-child` selector with a negative digit-less step, such as `:nth-child(-n+2)`, would be parsed incorrectly as a
  positive step, and so would not match as expected. [#1147](https://github.com/jhy/jsoup/issues/1147)
* Calling `doc.charset(charset)` on an empty XML document would throw an
  `IndexOutOfBoundsException`. [#2266](https://github.com/jhy/jsoup/issues/2266)
* Fixed a memory leak when reusing a nested `StructuralEvaluator` (e.g., a selector ancestor chain like `A B C`) by
  ensuring cache reset calls cascade to inner members. [#2277](https://github.com/jhy/jsoup/issues/2277)
* Concurrent calls to `doc.clone().append(html)` were not supported. When a document was cloned, its `Parser` was not cloned but was a shallow copy of the original parser. [#2281](https://github.com/jhy/jsoup/issues/2281)

## 1.18.3 (2024-Dec-02)

### Bug Fixes

* When serializing to XML, attribute names containing `-`, `.`, or digits were incorrectly marked as invalid and
  removed. [2235](https://github.com/jhy/jsoup/issues/2235)

## 1.18.2 (2024-Nov-27)

### Improvements

* Optimized the throughput and memory use throughout the input read and parse flows, with heap allocations and GC 
  down between -6% and -89%, and throughput improved up to +143% for small inputs. Most inputs sizes will see 
  throughput increases of ~ 20%. These performance improvements come through recycling the backing `byte[]` and `char[]` 
  arrays used to read and parse the input. [2186](https://github.com/jhy/jsoup/pull/2186) 
* Speed optimized `html()` and `Entities.escape()` when the input contains UTF characters in a supplementary plane, by
  around 49%. [2183](https://github.com/jhy/jsoup/pull/2183)
* The form associated elements returned by `FormElement.elements()` now reflect changes made to the DOM,
  subsequently to the original parse. [2140](https://github.com/jhy/jsoup/issues/2140)
* In the `TreeBuilder`, the `onNodeInserted()` and `onNodeClosed()` events are now also fired for the outermost /
  root `Document` node. This enables source position tracking on the Document node (which was previously unset). And
  it also enables the node traversor to see the outer Document node. [2182](https://github.com/jhy/jsoup/pull/2182)
* Selected Elements can now be position swapped inline using
  `Elements#set()`. [2212](https://github.com/jhy/jsoup/issues/2212)

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
* When cookie names in a response are duplicated, the simple view of cookies available via
  `Connection.Response#cookies()` will provide the last one set. Generally it is better to use
  the [Jsoup.newSession](https://jsoup.org/cookbook/web/request-session) method to maintain a cookie jar, as that
  applies appropriate path selection on cookies when making requests. [1831](https://github.com/jhy/jsoup/issues/1831)
* When parsing named HTML entities, base entities should resolve if they are a prefix of the input token (and not in an
  attribute). [2207](https://github.com/jhy/jsoup/issues/2207)
* Fixed incorrect tracking of source ranges for attributes merged from late-occurring elements that were implicitly
  created (`html` or `body`). [2204](https://github.com/jhy/jsoup/issues/2204)
* Follow the current HTML specification in the tokenizer to allow `<` as part of a tag name, instead of emitting it as a
  character node. [2230](https://github.com/jhy/jsoup/issues/2230)
* Similarly, allow a `<` as the start of an attribute name, vs creating a new element. The previous behavior was
  intended to parse closer to what we anticipated the author's intent to be, but that does not align to the spec or to
  how browsers behave. [1483](https://github.com/jhy/jsoup/issues/1483)

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
