package org.jsoup.select;

import org.jsoup.internal.CssMatching;
import org.jsoup.internal.QueryMode;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 Shared query operations for {@link Element}, {@link Collector}, and {@link org.jsoup.parser.StreamParser}.
 Public for access between jsoup packages; not a supported user API.
 String-query methods report compatibility differences when in check mode.
 */
public final class EvaluatorInternals {
    private EvaluatorInternals() {}

    /**
     Filters candidates using the evaluator's captured query mode, excluding {@code root} in Web mode.
     */
    public static Stream<Element> stream(Evaluator evaluator, Element root, Stream<Element> candidates) {
        return stream(evaluator, root, candidates, evaluator.asPredicate(root));
    }

    /**
     Shares stream filtering between element and node queries.
     */
    static <T extends Node> Stream<T> stream(Evaluator evaluator, Element root, Stream<T> candidates,
                                             Predicate<? super T> predicate) {
        evaluator.reset();
        if (evaluator.web)
            candidates = candidates.filter(node -> node != root);
        return candidates.filter(documentPredicate(evaluator, root, predicate));
    }

    /**
     Resolves the context document lazily and reuses it for Web matching.
     */
    private static <T> Predicate<T> documentPredicate(Evaluator evaluator, Element root, Predicate<T> predicate) {
        return evaluator.web ? CssMatching.withDocument(root, predicate) : predicate;
    }

    /**
     Matches ID values exactly for {@link Element#getElementById(String)}, even in quirks mode.
     */
    public static Evaluator byId(String id) {
        return new Evaluator() {
            @Override
            public boolean matches(Element root, Element element) {
                String actual = web ?
                    CssMatching.attributeValue(element, "id", CssMatching.isHtmlElement(root, element)) : element.id();
                return id.equals(actual);
            }
        };
    }

    /**
     Tests whether the supplied element matches the query.
     */
    public static boolean matches(String query, Element element) {
        QueryMode mode = QueryMode.current();
        boolean result = matches(QueryParser.parse(query, mode == QueryMode.Web), element);
        if (mode == QueryMode.Check)
            QueryCompat.check(query, result, evaluator -> matches(evaluator, element));
        return result;
    }

    /**
     Returns the element if it matches the query, or its nearest matching ancestor.
     */
    public static @Nullable Element closest(String query, Element element) {
        QueryMode mode = QueryMode.current();
        Element result = closest(QueryParser.parse(query, mode == QueryMode.Web), element);
        if (mode == QueryMode.Check)
            QueryCompat.check("closest", query, result, evaluator -> closest(evaluator, element));
        return result;
    }

    /**
     Tests the element itself, using it as {@code :scope} in Web mode.
     */
    public static boolean matches(Evaluator evaluator, Element element) {
        evaluator.reset();
        Element root = evaluator.web ? element : element.root();
        boolean matches = evaluator.web ?
            CssMatching.<Element>withDocument(root, candidate -> evaluator.matches(root, candidate)).test(element) :
            evaluator.matches(root, element);
        evaluator.reset();
        return matches;
    }

    /**
     Returns the element if it matches the evaluator, or its nearest matching ancestor.
     In Web mode, {@code :scope} remains the supplied element.
     */
    public static @Nullable Element closest(Evaluator evaluator, Element element) {
        evaluator.reset();
        Element root = evaluator.web ? element : element.root();
        Predicate<Element> predicate = documentPredicate(evaluator, root, candidate -> evaluator.matches(root, candidate));
        Element candidate = element;
        while (candidate != null && !predicate.test(candidate))
            candidate = candidate.parent();
        evaluator.reset();
        return candidate;
    }
}
