package org.jsoup.select;

import org.jsoup.nodes.Element;

public class ParentEvaluator extends Evaluator {

    public ParentEvaluator() {
    }

    @Override
    public boolean matches(Element root, Element element) {
        return false;
    }

    public Evaluator getEvaluator(Evaluator currentEval, Evaluator newEval) {
        currentEval = new CombiningEvaluator.And(new StructuralEvaluator.Parent(currentEval), newEval);
        return currentEval;
    }
}