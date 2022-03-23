package org.jsoup.select;

public class ImmediatePreviousSiblingEvaluator extends abstractEvaluator {
    public ImmediatePreviousSiblingEvaluator() {
    }

     CombiningEvaluator.And getEvaluator(Evaluator currentEval, Evaluator newEval) {
        return new CombiningEvaluator.And(new StructuralEvaluator.ImmediatePreviousSibling(currentEval), newEval);
    }
}