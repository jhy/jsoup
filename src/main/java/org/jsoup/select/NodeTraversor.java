package org.jsoup.select;

import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter.FilterResult;

public interface NodeTraversor {

    public void traverse(NodeVisitor visitor, Node root);

    public void traverse(NodeVisitor visitor, Elements elements);

    public FilterResult filter(NodeFilter filter, Node root);

    public void filter(NodeFilter filter, Elements elements);
}