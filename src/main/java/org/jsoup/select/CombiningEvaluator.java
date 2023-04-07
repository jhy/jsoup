package org.jsoup.select;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Base combining (and, or) evaluator.
 */
public abstract class CombiningEvaluator extends Evaluator {
    final ArrayList<Evaluator> evaluators;
    int evaluatorCount = 0;

    CombiningEvaluator() {
        super();
        evaluators = new ArrayList<>();
    }

    CombiningEvaluator(Collection<Evaluator> evaluators) {
        this();
        this.evaluators.addAll(evaluators);
        updateNumEvaluators();
    }

    @Nullable Evaluator rightMostEvaluator() {
        return evaluatorCount > 0 ? evaluators.get(evaluatorCount - 1) : null;
    }
    
    void replaceRightMostEvaluator(Evaluator replacement) {
        evaluators.set(evaluatorCount - 1, replacement);
    }

    void updateNumEvaluators() {
        // used so we don't need to bash on size() for every match test
        evaluatorCount = evaluators.size();
    }

    public static final class And extends CombiningEvaluator {
        And(Collection<Evaluator> evaluators) {
            super(evaluators);
        }

        And(Evaluator... evaluators) {
            this(Arrays.asList(evaluators));
        }

        @Override
        public boolean matches(Element root, Element node) {
            for (int i = evaluatorCount - 1; i >= 0; i--) { // process backwards so that :matchText is evaled earlier, to catch parent query. todo - should redo matchText to virtually expand during match, not pre-match (see SelectorTest#findBetweenSpan)
                Evaluator evaluator = evaluators.get(i);
                if (!evaluator.matches(root, node))
                    return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return StringUtil.join(evaluators, "");
        }
    }

    public static final class Or extends CombiningEvaluator {
        /**
         * Create a new Or evaluator. The initial evaluators are ANDed together and used as the first clause of the OR.
         * @param evaluators initial OR clause (these are wrapped into an AND evaluator).
         */
        Or(Collection<Evaluator> evaluators) {
            super();
            if (evaluatorCount > 1)
                this.evaluators.add(new And(evaluators));
            else // 0 or 1
                this.evaluators.addAll(evaluators);
            updateNumEvaluators();
        }

        Or(Evaluator... evaluators) { this(Arrays.asList(evaluators)); }

        Or() {
            super();
        }

        public void add(Evaluator e) {
            evaluators.add(e);
            updateNumEvaluators();
        }

        @Override
        public boolean matches(Element root, Element node) {
            for (int i = 0; i < evaluatorCount; i++) {
                Evaluator evaluator = evaluators.get(i);
                if (evaluator.matches(root, node))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return StringUtil.join(evaluators, ", ");
        }
    }
}
