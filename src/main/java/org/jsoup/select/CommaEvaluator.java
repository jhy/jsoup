package org.jsoup.select;


import org.jsoup.nodes.Element;

public class CommaEvaluator extends Evaluator{
    public CommaEvaluator() {
    }

    @Override
    public boolean matches(Element root, Element element) {
        return false;
    }

    public Evaluator getEvaluator(Evaluator currentEval, Evaluator newEval) {
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