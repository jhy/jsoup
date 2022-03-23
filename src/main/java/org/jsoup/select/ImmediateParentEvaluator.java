package org.jsoup.select;

public class ImmediateParentEvaluator extends abstractEvaluator {

    public ImmediateParentEvaluator() {
    }

     Evaluator getEvaluator(Evaluator currentEval, Evaluator newEval) {
        currentEval = new CombiningEvaluator.And(new StructuralEvaluator.ImmediateParent(currentEval), newEval);
        return currentEval;
    }
}