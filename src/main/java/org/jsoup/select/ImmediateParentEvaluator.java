package org.jsoup.select;

import org.jsoup.nodes.Element;

public class ImmediateParentEvaluator extends Evaluator {

    public ImmediateParentEvaluator() {
    }

    @Override
    public boolean matches(Element root, Element element) {
        return false;
    }

    public Evaluator getEvaluator(Evaluator currentEval, Evaluator newEval) {
        currentEval = new CombiningEvaluator.And(new StructuralEvaluator.ImmediateParent(currentEval), newEval);
        return currentEval;
    }
}