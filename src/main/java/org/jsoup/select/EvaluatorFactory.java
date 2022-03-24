package org.jsoup.select;

public class EvaluatorFactory {
    public static Evaluator createEvaluator(char c) {
        Evaluator evaluator;
        if (c == '>') {
            return new ImmediateParentEvaluator();
        } else if (c == ' ') {
            return new ParentEvaluator();
        } else if (c == '+') {
            return new ImmediatePreviousSiblingEvaluator();
        } else if (c == '~') {
            return new PreviousSiblingEvaluator();
        } else {
            return null;
        }
    }
}
