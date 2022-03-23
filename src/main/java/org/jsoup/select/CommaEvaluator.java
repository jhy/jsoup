package org.jsoup.select;


public class CommaEvaluator extends AbstractEvaluator{
    public CommaEvaluator() {
    }

      Evaluator getEvaluator(Evaluator currentEval, Evaluator newEval) {
        CombiningEvaluator.Or or;
        if (currentEval instanceof CombiningEvaluator.Or) {
            or = (CombiningEvaluator.Or) currentEval;
        } else {
            or = new CombiningEvaluator.Or();
            or.add(currentEval);
        }
        or.add(newEval);
        currentEval = or;
        return currentEval;
    }
}
