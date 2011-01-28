package org.jsoup.select;

import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Base combining (and, or) evaluator.
 */
abstract class CombiningEvaluator extends Evaluator {
    final List<Evaluator> evaluators;

    CombiningEvaluator() {
        super();
        evaluators = new ArrayList<Evaluator>();
    }

    CombiningEvaluator(Collection<Evaluator> evaluators) {
        this();
        this.evaluators.addAll(evaluators);
    }

    static final class And extends CombiningEvaluator {
        And(Collection<Evaluator> evaluators) {
            super(evaluators);
        }

        And(Evaluator... evaluators) {
            this(Arrays.asList(evaluators));
        }

        @Override
        public boolean matches(Element root, Element node) {
            for (Evaluator s : evaluators) {
                if (!s.matches(root, node))
                    return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return StringUtil.join(evaluators, " ");
        }
    }

    static final class Or extends CombiningEvaluator {
        Or(Collection<Evaluator> evaluators) {
            super(evaluators);
        }

        public void add(Evaluator e) {
            evaluators.add(e);
        }

        @Override
        public boolean matches(Element root, Element node) {
            for (Evaluator s : evaluators) {
                if (s.matches(root, node))
                    return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format(":or%s", evaluators);
        }
    }
}
