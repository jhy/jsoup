package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;

import java.util.ArrayList;

public class DiscardList {

    private ArrayList<Node> discElems;
    private int elemsMaxSize;
    private boolean trackDiscElems;

    private ArrayList<Node> discAttribs;
    private int attribsMaxSize;
    private boolean trackDiscAttr;

    public DiscardList(){
        elemsMaxSize = -1;
        attribsMaxSize = -1;
        discElems = new ArrayList<>();
        discAttribs = new ArrayList<>();

        trackDiscElems = false;
        trackDiscAttr = false;

    }
    public void setElemsMaxSize(int elemsMaxSize) {
        this.elemsMaxSize = elemsMaxSize;
    }
    public void setAttribsMaxSize(int attribsMaxSize) {
        this.attribsMaxSize = attribsMaxSize;
    }
    public void trackDiscElems(){
        trackDiscElems = true;
    }
    public void trackDiscAttr(){
        trackDiscAttr = true;
    }
    public void stopElemTracking(){
        trackDiscElems = false;
    }
    public void stopAttribTracking(){
        trackDiscAttr = false;
    }
    public void addElem(Node elem){
        if(trackDiscElems && (elemsMaxSize == -1 || elemsMaxSize > discElems.size())){
            discElems.add(elem.clone());
        }
    }
    public void addAttribute(Element elem, Attribute attr){
        if(trackDiscAttr && (attribsMaxSize == -1 || attribsMaxSize > discAttribs.size())){
            Element copy = new Element(Tag.valueOf(elem.tagName()), elem.baseUri(), null);
            copy.attributes().add(attr.getKey(), attr.getValue());
            discAttribs.add(copy);
        }
    }
    public ArrayList<Node> getDiscElems(){
        ArrayList<Node> copy = new ArrayList<>(discElems.size());
        for (Node node:discElems){
            copy.add(node.clone());
        }
        return copy;
    }
    public ArrayList<Node> getDiscAttribs(){
        ArrayList<Node> copy = new ArrayList<>(discAttribs.size());
        for (Node node:discAttribs){
            copy.add(node.clone());
        }
        return copy;
    }

}
