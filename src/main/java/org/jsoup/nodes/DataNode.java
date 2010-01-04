package org.jsoup.nodes;

import org.apache.commons.lang.StringEscapeUtils;

/**
 A data node, for contents of style, script tags etc, where contents should not show in text().

 @author Jonathan Hedley, jonathan@hedley.net */
public class DataNode extends Node{
    private static final String DATA_KEY = "data";

    public DataNode(String data) {
        super();
        attributes.put(DATA_KEY, data);
    }

    public String nodeName() {
        return "#data";
    }

    public String getWholeData() {
        return attributes.get(DATA_KEY);
    }

    public String outerHtml() {
        return StringEscapeUtils.escapeHtml(getWholeData());
    }

    public String toString() {
        return outerHtml();
    }

    public static DataNode createFromEncoded(String encodedData) {
        String data = StringEscapeUtils.unescapeHtml(encodedData);
        return new DataNode(data);
    }
}
