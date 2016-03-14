package org.jsoup.safety;

public class ValidationError {
    public final String tag;
    public final String attribute;
    public final String html;

    public ValidationError(String tag, String attribute, String html) {
        this.tag = tag;
        this.attribute = attribute;
        this.html = html;
    }

    @Override public String toString() {
        return String.format(
            "{\"tag\":\"%s\",\"attribute\":%s,\"html\":\"%s\"}",
            tag == null ? "null" : ("\"" + tag + "\""),
            attribute == null ? "null" : ("\"" + attribute + "\""),
            html == null ? "null" : ("\"" + html + "\"")
        );
    }
}
