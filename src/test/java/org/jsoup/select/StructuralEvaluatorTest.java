package org.jsoup.select;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StructuralEvaluatorTest {
    private static final String Html =
        "<div id=outer>" +
        "  <div class=a>" +
        "    <p class=p1>One</p>" +
        "    <p class=p2>Two</p>" +
        "  </div>" +
        "  <div class=b>" +
        "    <span>Span1</span>" +
        "    <a href=# class=link>Link</a>" +
        "  </div>" +
        "  <div class=c>" +
        "    <div class=inner>" +
        "      <p class=target>Target</p>" +
        "    </div>" +
        "  </div>" +
        "</div>";

    @ParameterizedTest
    @MethodSource("selectorMemoData")
    void selectorMemoIsClearedOnReset(String selector, boolean expectMemos) {
        // test that the structural evaluator memos are used, and are reset

        Document doc = Jsoup.parse(Html);
        Evaluator evaluator = Selector.evaluatorOf(selector);

        // collect all StructuralEvaluator instances from the parsed evaluator tree
        List<StructuralEvaluator> structuralEvals = new ArrayList<>();
        collectEvals(evaluator, structuralEvals);

        Selector.select(evaluator, doc); // populate memos
        assertFalse(structuralEvals.isEmpty());

        boolean hadMemos = false;
        for (StructuralEvaluator se : structuralEvals) {
            if (!se.threadMemo.get().isEmpty()) {
                hadMemos = true;
                break;
            }
        }

        evaluator.reset();

        // verify all structural evaluator thread-local maps are cleared
        for (StructuralEvaluator se : structuralEvals) {
            assertTrue(se.threadMemo.get().isEmpty());
        }

        assertEquals(expectMemos, hadMemos);
    }

    private static Stream<Arguments> selectorMemoData() {
        return Stream.of(
            Arguments.of("div:not(.b)", true),       // Not (uses memoMatches)
            Arguments.of("div p", true),             // Ancestor (ancestor chain checks)
            Arguments.of("span ~ a", true),          // PreviousSibling
            Arguments.of("span + a", true),          // ImmediatePreviousSibling
            Arguments.of("div > span > a", false),   // ImmediateParentRun does not use memoMatches
            Arguments.of("div:has(p)", false)        // Has (coverage; does not use memo for these inputs)
        );
    }

    private static void collectEvals(Evaluator evaluator, List<StructuralEvaluator> out) {
        // recursive traversal of evaluator trees to find StructuralEvaluator instances
        if (evaluator instanceof CombiningEvaluator) {
            CombiningEvaluator ce = (CombiningEvaluator) evaluator;
            for (Evaluator inner : ce.evaluators) {
                collectEvals(inner, out);
            }
            return;
        }

        if (evaluator instanceof StructuralEvaluator.ImmediateParentRun) {
            StructuralEvaluator.ImmediateParentRun run = (StructuralEvaluator.ImmediateParentRun) evaluator;
            out.add(run);
            for (Evaluator inner : run.evaluators) {
                collectEvals(inner, out);
            }
            return;
        }

        if (evaluator instanceof StructuralEvaluator) {
            StructuralEvaluator se = (StructuralEvaluator) evaluator;
            out.add(se);
            collectEvals(se.evaluator, out);
        }

    }
}
