package org.jsoup.select;

public abstract class AbstractEvaluator {
      Evaluator  getEvaluator(char combinator, Evaluator currentEval, Evaluator newEval){
        return currentEval;
    };
}
