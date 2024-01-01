package org.jsoup.parser;

import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Evaluator;
import org.jsoup.select.NodeVisitor;
import org.jsoup.select.QueryParser;
import org.jspecify.annotations.Nullable;

import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamParser {
    final private Parser parser;
    final private TreeBuilder treeBuilder;
    final private ElementIterator it = new ElementIterator();
    @Nullable private Document document;
    private boolean stopped = false;

    public StreamParser(Parser parser) {
        this.parser = parser;
        treeBuilder = parser.getTreeBuilder();
        treeBuilder.nodeListener(it);
    }

    public StreamParser parse(Reader input, String baseUri) {
        close(); // probably a no-op, but ensures any previous reader is closed
        it.reset();
        treeBuilder.initialiseParse(input, baseUri, parser);
        document = treeBuilder.doc;
        return this;
    }

    public StreamParser parse(String input, String baseUri) {
        return parse(new StringReader(input), baseUri);
    }

    public Stream<Element> stream() {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                it, Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.ORDERED),
            false);
    }

    public Iterator<Element> iterator() {
        return it;
    }

    public void stop() {
        stopped = true;
    }

    public void close() {
        treeBuilder.completeParse(); // closes the reader, frees resources
    }

    public Document document() {
        document = treeBuilder.doc;
        Validate.notNull(document, "Must run parse() before calling.");
        return document;
    }

    public Optional<Element> selectFirst(String query) {
        return selectFirst(QueryParser.parse(query));
    }

    public Optional<Element> selectFirst(Evaluator eval) {
        final Document doc = document();

        // run the query on the existing (partial) doc first, as there may be a hit already parsed
        Element first = doc.selectFirst(eval);
        if (first != null) return Optional.of(first);

        return selectNext(eval);
    }

    public Optional<Element> selectNext(String query) {
        return selectNext(QueryParser.parse(query));
    }

    public Optional<Element> selectNext(Evaluator eval) {
        final Document doc = document();

        return stream()
            .filter(eval.asPredicate(doc))
            .findFirst();
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
        @Override public boolean hasNext() {
            maybeFindNext();
            return next != null;
        }

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



