package org.jsoup.select;

import org.jsoup.nodes.Element;

public class PreviousSiblingEvaluator extends Evaluator {

    public PreviousSiblingEvaluator() {
    }

    @Override
    public boolean matches(Element root, Element element) {
        return false;
    }

    public Evaluator getEvaluator(Evaluator currentEval, Evaluator newEval) {
        currentEval = new CombiningEvaluator.And(new StructuralEvaluator.PreviousSibling(currentEval), newEval);
        return currentEval;
    }
}