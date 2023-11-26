package org.jsoup.safety;

import org.jsoup.nodes.Element;

public class ElementMeta {
    public Element getEl() {
        return el;
    }

    public void setEl(Element el) {
        this.el = el;
    }

    private Element el;

    public int getNumAttribsDiscarded() {
        return numAttribsDiscarded;
    }

    public void setNumAttribsDiscarded(int numAttribsDiscarded) {
        this.numAttribsDiscarded = numAttribsDiscarded;
    }

    private int numAttribsDiscarded;

    ElementMeta(Element el, int numAttribsDiscarded) {
        this.el = el;
        this.numAttribsDiscarded = numAttribsDiscarded;
    }
}
