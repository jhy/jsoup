package org.jsoup.select;

import org.jsoup.nodes.Element;

public class ImmediatePreviousSiblingEvaluator extends Evaluator {
    public ImmediatePreviousSiblingEvaluator() {
    }

    @Override
    public boolean matches(Element root, Element element) {
        return false;
    }

    @Override
    public CombiningEvaluator.And getEvaluator(Evaluator currentEval, Evaluator newEval) {
        return new CombiningEvaluator.And(new StructuralEvaluator.ImmediatePreviousSibling(currentEval), newEval);
    }
}