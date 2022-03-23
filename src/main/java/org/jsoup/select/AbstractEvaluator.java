package org.jsoup.select;

public abstract class AbstractEvaluator {

      Evaluator getEvaluator(Evaluator currentEval, Evaluator newEval){
        return currentEval;
    };
}
