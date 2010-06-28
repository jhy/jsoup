package org.jsoup.select;

import org.jsoup.nodes.Node;

/**
 * Node visitor interface
 */
public interface NodeVisitor {
    public void head(Node node, int depth);
    public void tail(Node node, int depth);
}
