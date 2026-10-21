package org.jsoup.select;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 Runs queries in Web mode to report compatibility differences, while callers retain legacy results.
 Scheduled for removal with check mode in 1.26.1.
 */
final class QueryCompat {
    private QueryCompat() {}

    /**
     Checks that collection results contain the same elements in the same order.
     */
    static void check(String query, Elements legacy, Function<Evaluator, Elements> execute) {
        check("select", query, evaluator -> {
            Elements web = execute.apply(evaluator);
            return sameResults(legacy, web) ? null :
                String.format("legacy=%s, web=%s; results differ", legacy.size(), web.size());
        });
    }

    /**
     Checks a single-element result by identity, including null results.
     */
    static void check(String operation, String query, @Nullable Element legacy, Function<Evaluator, @Nullable Element> execute) {
        check(operation, query, evaluator -> legacy == execute.apply(evaluator) ? null : "results differ");
    }

    /**
     Checks whether both modes agree that an element matches.
     */
    static void check(String query, boolean legacy, Predicate<Evaluator> execute) {
        check("is", query, evaluator -> {
            boolean web = execute.test(evaluator);
            return legacy == web ? null : String.format("legacy=%s, web=%s", legacy, web);
        });
    }

    /**
     Runs a Web query comparison, reporting non-null difference details or query errors.
     */
    private static void check(String operation, String query, Function<Evaluator, @Nullable String> compare) {
        Evaluator web;
        try {
            web = QueryParser.parse(query, true);
        } catch (RuntimeException e) {
            warn(operation, query, String.format("web parse failed: %s", e.getMessage()));
            return;
        }
        String detail;
        try {
            detail = compare.apply(web);
        } catch (RuntimeException e) {
            warn(operation, query, String.format("web evaluation failed: %s", e.getMessage()));
            return;
        }
        if (detail != null) warn(operation, query, detail);
    }

    /**
     Compares collections by element identity and order.
     */
    private static boolean sameResults(Elements legacy, Elements web) {
        Iterator<Element> first = legacy.iterator();
        Iterator<Element> second = web.iterator();
        while (first.hasNext() && second.hasNext()) {
            if (first.next() != second.next()) return false;
        }
        return !first.hasNext() && !second.hasNext();
    }

    /**
     Prints a single-line warning with up to two caller locations.
     */
    private static void warn(String operation, String query, String detail) {
        StringUtil.StringJoiner message = new StringUtil.StringJoiner("; ")
            .add(String.format("jsoup query compat: %s(\"%s\"): %s", operation, query, detail));
        int callers = 0;
        for (StackTraceElement frame : new Throwable().getStackTrace()) {
            if (callers == 0 && isInternalFrame(frame.getClassName())) continue;
            message.add("at ").append(frame);
            if (++callers == 2) break;
        }
        System.err.println(StringUtil.normaliseWhitespace(message.complete()));
    }

    /**
     Identifies query implementation classes, allowing callers elsewhere in org.jsoup to appear in warnings.
     */
    private static boolean isInternalFrame(String name) {
        for (Class<?> type : InternalClasses) {
            String className = type.getName();
            if (name.equals(className) || name.startsWith(className + "$")) return true;
        }
        return false;
    }

    private static final Class<?>[] InternalClasses = {
        QueryCompat.class, Selector.class, EvaluatorInternals.class, Element.class, Elements.class
    };
}
