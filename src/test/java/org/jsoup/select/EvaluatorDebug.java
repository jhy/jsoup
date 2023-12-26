package org.jsoup.select;

import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

public class EvaluatorDebug {

    /**
     Cast an Evaluator into a pseudo Document, to help visualize the query. Quite coupled to the current impl.
     */
    public static Document asDocument(Evaluator eval) {
        Document doc = new Document(null);
        doc.outputSettings().outline(true).indentAmount(2);

        Element el = asElement(eval);
        doc.appendChild(el);

        return doc;
    }

    public static Document asDocument(String query) {
        Evaluator eval = QueryParser.parse(query);
        return asDocument(eval);
    }

    public static Element asElement(Evaluator eval) {
        Class<? extends Evaluator> evalClass = eval.getClass();
        Element el = new Element(evalClass.getSimpleName());
        el.attr("css", eval.toString());
        el.attr("cost", Integer.toString(eval.cost()));

        if (eval instanceof CombiningEvaluator) {
            for (Evaluator inner : ((CombiningEvaluator) eval).sortedEvaluators) {
                el.appendChild(asElement(inner));
            }
        } else if (eval instanceof StructuralEvaluator.ImmediateParentRun) {
            for (Evaluator inner : ((StructuralEvaluator.ImmediateParentRun) eval).evaluators) {
                el.appendChild(asElement(inner));
            }
        } else if (eval instanceof StructuralEvaluator) {
            Evaluator inner = ((StructuralEvaluator) eval).evaluator;
            el.appendChild(asElement(inner));
        }

        return el;
    }

    public static String sexpr(String query) {
        Document doc = asDocument(query);

        SexprVisitor sv = new SexprVisitor();
        doc.childNode(0).traverse(sv); // skip outer #document
        return sv.result();
    }

    static class SexprVisitor implements NodeVisitor {
        StringBuilder sb = StringUtil.borrowBuilder();

        @Override public void head(Node node, int depth) {
            sb
                .append('(')
                .append(node.nodeName());

            if (node.childNodeSize() == 0)
                sb
                    .append(" '")
                    .append(node.attr("css"))
                    .append("'");
            else
                sb.append(" ");
        }

        @Override public void tail(Node node, int depth) {
            sb.append(')');
        }

        String result() {
            return StringUtil.releaseBuilder(sb);
        }
    }
}
