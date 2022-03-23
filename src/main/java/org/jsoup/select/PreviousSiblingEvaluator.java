package org.jsoup.select;

public class PreviousSiblingEvaluator extends AbstractEvaluator {

    public PreviousSiblingEvaluator() {
    }

       Evaluator getEvaluator(Evaluator currentEval, Evaluator newEval) {
        currentEval = new CombiningEvaluator.And(new StructuralEvaluator.PreviousSibling(currentEval), newEval);
        return currentEval;
    }
}
