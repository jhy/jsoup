package org.jsoup.parser;

import org.jsoup.Connection;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.QueryParser;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.Reader;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 A StreamParser provides a progressive parse of its input. As each Element is completed, it is emitted via a Stream or
 Iterator interface. Elements returned will be complete with all their children, and an (empty) next sibling, if
 applicable.
 <p>Elements (or their children) may be removed from the DOM during the parse, for e.g. to conserve memory, providing a
 mechanism to parse an input document that would otherwise be too large to fit into memory, yet still providing a DOM
 interface to the document and its elements.</p>
 <p>
 Additionally, the parser provides a {@link #selectFirst(String query)} / {@link #selectNext(String query)}, which will
 run the parser until a hit is found, at which point the parse is suspended. It can be resumed via another
 {@code select()} call, or via the {@link #stream()} or {@link #iterator()} methods.
 </p>
 <p>Once the input has been fully read, the input Reader will be closed. Or, if the whole document does not need to be
 read, call {@link #stop()} and {@link #close()}.</p>
 <p>The {@link #document()} method will return the Document being parsed into, which will be only partially complete
 until the input is fully consumed.</p>
 <p>A StreamParser can be reused via a new {@link #parse(Reader, String)}, but is not thread-safe for concurrent inputs.
 New parsers should be used in each thread.</p>
 <p>If created via {@link Connection.Response#streamParser()}, or another Reader that is I/O backed, the various methods
 that advance the parser (e.g. {@link #selectFirst(String)}, {@link #stream()}) will throw
 an {@link java.io.UncheckedIOException} if the underlying Reader errors during read.</p>
 <p>The StreamParser interface is currently in <b>beta</b> and may change in subsequent releases. Feedback on the
 feature and how you're using it is very welcome via the <a href="https://jsoup.org/discussion">jsoup
 discussions</a>.</p>
 @since 1.18.1
 */
public class StreamParser implements Closeable {
    final private Parser parser;
    final private TreeBuilder treeBuilder;
    final private ElementIterator it = new ElementIterator();
    @Nullable private Document document;
    private boolean stopped = false;

    /**
     Construct a new StreamParser, using the supplied base Parser.
     @param parser the configured base parser
     */
    public StreamParser(Parser parser) {
        this.parser = parser;
        treeBuilder = parser.getTreeBuilder();
        treeBuilder.nodeListener(it);
    }

    /**
     Provide the input for a parse. The input is not read until a consuming operation is called.
     @param input the input to be read.
     @param baseUri the URL of this input, for absolute link resolution
     @return this parser, for chaining
     */
    public StreamParser parse(Reader input, String baseUri) {
        close(); // probably a no-op, but ensures any previous reader is closed
        it.reset();
        treeBuilder.initialiseParse(input, baseUri, parser);
        document = treeBuilder.doc;
        return this;
    }

    /**
     Provide the input for a parse. The input is not read until a consuming operation is called.
     @param input the input to be read
     @param baseUri the URL of this input, for absolute link resolution
     @return this parser
     */
    public StreamParser parse(String input, String baseUri) {
        return parse(new StringReader(input), baseUri);
    }

    /**
     Creates a {@link Stream} of {@link Element}s, with the input being parsed as each element is consumed. Each
     Element returned will be complete (that is, all of its children will be included, and if it has a next sibling, that
     (empty) sibling will exist at {@link Element#nextElementSibling()}). The stream will be emitted in document order as
     each element is closed. That means that child elements will be returned prior to their parents.
     <p>The stream will start from the current position of the backing iterator and the parse.</p>
     @return a stream of Element objects
     @throws UncheckedIOException if the underlying Reader excepts during a read
     */
    public Stream<Element> stream() {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                it, Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.ORDERED),
            false);
    }

    /**
     Returns an {@link Iterator} of {@link Element}s, with the input being parsed as each element is consumed. Each
     Element returned will be complete (that is, all of its children will be included, and if it has a next sibling, that
     (empty) sibling will exist at {@link Element#nextElementSibling()}). The elements will be emitted in document order as
     each element is closed. That means that child elements will be returned prior to their parents.
     <p>The iterator will start from the current position of the parse.</p>
     <p>The iterator is backed by this StreamParser, and the resources it holds.</p>
     @return a stream of Element objects
     */
    public Iterator<Element> iterator() {
        //noinspection ReturnOfInnerClass
        return it;
    }

    /**
     Flags that the parse should be stopped; the backing iterator will not return any more Elements.
     @return this parser
     */
    public StreamParser stop() {
        stopped = true;
        return this;
    }

    /**
     Closes the input and releases resources including the underlying parser and reader.
     <p>The parser will also be closed when the input is fully read.</p>
     <p>The parser can be reused with another call to {@link #parse(Reader, String)}.</p>
     */
    @Override public void close() {
        treeBuilder.completeParse(); // closes the reader, frees resources
    }

    /**
     Get the current {@link Document} as it is being parsed. It will be only partially complete until the input is fully
     read. Structural changes (e.g. insert, remove) may be made to the Document contents.
     @return the (partial) Document
     */
    public Document document() {
        document = treeBuilder.doc;
        Validate.notNull(document, "Must run parse() before calling.");
        return document;
    }

    /**
     Runs the parser until the input is fully read, and returns the completed Document.
     @return the completed Document
     @throws UncheckedIOException if the underlying Reader errors during a read
     */
    public Document complete() {
        Document doc = document();
        treeBuilder.runParser();
        return doc;
    }

    /**
     Finds the first Element that matches the provided query. If the parsed Document does not already have a match, the
     input will be parsed until the first match is found, or the input is completely read.
     @param query the {@link org.jsoup.select.Selector} query.
     @return the first matching {@link Element}, or {@code null} if there's no match
     @throws UncheckedIOException if the underlying Reader errors during a read
     */
    public @Nullable Element selectFirst(String query) {
        return selectFirst(QueryParser.parse(query));
    }

    /**
     Just like {@link #selectFirst(String)}, but if there is no match, throws an {@link IllegalArgumentException}. This
     is useful if you want to simply abort processing on a failed match.
     @param query the {@link org.jsoup.select.Selector} query.
     @return the first matching element
     @throws IllegalArgumentException if no match is found
     @throws UncheckedIOException if the underlying Reader errors during a read
     */
    public Element expectFirst(String query) {
        return (Element) Validate.ensureNotNull(
            selectFirst(query),
            "No elements matched the query '%s' in the document."
            , query
        );
    }

    /**
     Finds the first Element that matches the provided query. If the parsed Document does not already have a match, the
     input will be parsed until the first match is found, or the input is completely read.
     @param eval the {@link org.jsoup.select.Selector} evaluator.
     @return the first matching {@link Element}, or {@code null} if there's no match
     @throws UncheckedIOException if the underlying Reader errors during a read
     */
    public @Nullable Element selectFirst(Evaluator eval) {
        final Document doc = document();

        // run the query on the existing (partial) doc first, as there may be a hit already parsed
        Element first = doc.selectFirst(eval);
        if (first != null) return first;

        return selectNext(eval);
    }

    /**
     Finds the next Element that matches the provided query. The input will be parsed until the next match is found, or
     the input is completely read.
     @param query the {@link org.jsoup.select.Selector} query.
     @return the next matching {@link Element}, or {@code null} if there's no match
     @throws UncheckedIOException if the underlying Reader errors during a read
     */
    public @Nullable Element selectNext(String query) {
        return selectNext(QueryParser.parse(query));
    }

    /**
     Just like {@link #selectFirst(String)}, but if there is no match, throws an {@link IllegalArgumentException}. This
     is useful if you want to simply abort processing on a failed match.
     @param query the {@link org.jsoup.select.Selector} query.
     @return the first matching element
     @throws IllegalArgumentException if no match is found
     @throws UncheckedIOException if the underlying Reader errors during a read
     */
    public Element expectNext(String query) {
        return (Element) Validate.ensureNotNull(
            selectNext(query),
            "No elements matched the query '%s' in the document."
            , query
        );
    }

    /**
     Finds the next Element that matches the provided query. The input will be parsed until the next match is found, or
     the input is completely read.
     @param eval the {@link org.jsoup.select.Selector} evaluator.
     @return the next matching {@link Element}, or {@code null} if there's no match
     @throws UncheckedIOException if the underlying Reader errors during a read
     */
    public @Nullable Element selectNext(Evaluator eval) {
        final Document doc = document();

        return stream()
            .filter(eval.asPredicate(doc))
            .findFirst()
            .orElse(null);
    }

    final class ElementIterator implements Iterator<Element>, NodeVisitor {
        // listeners add to a next emit queue, as a single token read step may yield multiple elements
        final private Queue<Element> emitQueue = new LinkedList<>();
        private @Nullable Element current;  // most recently emitted
        private @Nullable Element next;     // element waiting to be picked up
        private @Nullable Element tail;     // The last tailed element (</html>), on hold for final pop

        void reset() {
            emitQueue.clear();
            current = next = tail = null;
            stopped = false;
        }

        // Iterator Interface:
        /**
         {@inheritDoc}
         @throws UncheckedIOException if the underlying Reader errors during a read
         */
        @Override public boolean hasNext() {
            maybeFindNext();
            return next != null;
        }

        /**
         {@inheritDoc}
         @throws UncheckedIOException if the underlying Reader errors during a read
         */
        @Override public Element next() {
            maybeFindNext();
            if (next == null) throw new NoSuchElementException();
            current = next;
            next = null;
            return current;
        }

        private void maybeFindNext() {
            if (stopped || next != null) return;

            // drain the current queue before stepping to get more
            if (!emitQueue.isEmpty()) {
                next = emitQueue.remove();
                return;
            }

            // step the parser, which will hit the node listeners to add to the queue:
            while (treeBuilder.stepParser()) {
                if (!emitQueue.isEmpty()) {
                    next = emitQueue.remove();
                    return;
                }
            }
            stop();
            close();

            // send the final element out:
            if (tail != null) {
                next = tail;
                tail = null;
            }
        }

        @Override public void remove() {
            if (current == null) throw new NoSuchElementException();
            current.remove();
        }

        // NodeVisitor Interface:
        @Override public void head(Node node, int depth) {
            if (node instanceof Element) {
                Element prev = ((Element) node).previousElementSibling();
                // We prefer to wait until an element has a next sibling before emitting it; otherwise, get it in tail
                if (prev != null) emitQueue.add(prev);
            }
        }

        @Override public void tail(Node node, int depth) {
            if (node instanceof Element) {
                tail = (Element) node; // kept for final hit
                Element lastChild = tail.lastElementChild(); // won't get a nextsib, so emit that:
                if (lastChild != null) emitQueue.add(lastChild);
            }
        }
    }
}



