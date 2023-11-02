module org.jsoup {
    exports org.jsoup;
    exports org.jsoup.helper;
    exports org.jsoup.nodes;
    exports org.jsoup.parser;
    exports org.jsoup.safety;
    exports org.jsoup.select;

    requires transitive java.xml; // for org.w3c.dom out of W3CDom
    requires static jsr305; // TODO[must] migrate to another nullable package prior to next release
}
