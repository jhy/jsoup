package org.jsoup.select;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.LeafNode;
import org.jsoup.nodes.Node;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Base combining (and, or) evaluator.
 */
public abstract class CombiningEvaluator extends Evaluator {
    final ArrayList<Evaluator> evaluators; // maintain original order so that #toString() is sensible
    final List<Evaluator> sortedEvaluators; // cost ascending order
    int num = 0;
    int cost = 0;
    boolean wantsNodes;

    CombiningEvaluator(Collection<Evaluator> evaluators) {
        this(new ArrayList<>(evaluators));
    }

    /** Creates a combined evaluator from the supplied evaluators. */
    CombiningEvaluator(ArrayList<Evaluator> evaluators) {
        this.evaluators = evaluators;
        sortedEvaluators = new ArrayList<>(evaluators.size());
        updateEvaluators();
    }

    public void add(Evaluator e) {
        evaluators.add(e);
        updateEvaluators();
    }

    @Override protected void reset() {
        for (Evaluator evaluator : evaluators) {
            evaluator.reset();
        }
        super.reset();
    }

    @Override protected int cost() {
        return cost;
    }

    @Override
    boolean wantsNodes() {
        return wantsNodes;
    }

    void updateEvaluators() {
        // used so we don't need to bash on size() for every match test
        num = evaluators.size();

        // sort the evaluators by lowest cost first, to optimize the evaluation order
        cost = 0;
        for (Evaluator evaluator : evaluators) {
            cost += evaluator.cost();
        }
        sortedEvaluators.clear();
        sortedEvaluators.addAll(evaluators);
        sortedEvaluators.sort(Comparator.comparingInt(Evaluator::cost));

        // any want nodes?
        for (Evaluator evaluator : evaluators) {
            if (evaluator.wantsNodes()) {
                wantsNodes = true;
                break;
            }
        }
    }

    public static final class And extends CombiningEvaluator {
        /** Creates an evaluator that requires every supplied evaluator to match. */
        public And(Collection<Evaluator> evaluators) {
            super(evaluators);
        }

        /** Creates an evaluator that requires every supplied evaluator to match. */
        And(ArrayList<Evaluator> evaluators) {
            super(evaluators);
        }

        And(Evaluator... evaluators) {
            this(Arrays.asList(evaluators));
        }

        @Override
        public boolean matches(Element root, Element el) {
            for (int i = 0; i < num; i++) {
                Evaluator eval = sortedEvaluators.get(i);
                if (!eval.matches(root, el))
                    return false;
            }
            return true;
        }

        @Override
        public boolean matches(Element root, LeafNode leaf) {
            for (int i = 0; i < num; i++) {
                Evaluator eval = sortedEvaluators.get(i);
                if (!eval.matches(root, leaf))
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
         * Creates an evaluator that requires at least one supplied evaluator to match.
         * @param evaluators alternative evaluators
         */
        public Or(Collection<Evaluator> evaluators) {
            super(evaluators);
        }

        /** Creates an evaluator that requires at least one supplied evaluator to match. */
        Or(ArrayList<Evaluator> evaluators) {
            super(evaluators);
        }

        Or(Evaluator... evaluators) { this(Arrays.asList(evaluators)); }

        @Override
        public boolean matches(Element root, Element element) {
            for (int i = 0; i < num; i++) {
                Evaluator eval = sortedEvaluators.get(i);
                if (eval.matches(root, element))
                    return true;
            }
            return false;
        }

        @Override
        public boolean matches(Element root, LeafNode leaf) {
            for (int i = 0; i < num; i++) {
                Evaluator eval = sortedEvaluators.get(i);
                if (eval.matches(root, leaf))
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
