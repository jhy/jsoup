package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;

import java.util.ArrayList;

/**
 * A container for DiscardList
 *
 * @author Sebastian Fagerlind sebene@kth.se and Eleonora Borzi borzi@kth.se
 */

public class DiscardList {

    private ArrayList<Node> discElems;
    private int elemsMaxSize;
    private boolean trackDiscElems;

    private ArrayList<Node> discAttribs;
    private int attribsMaxSize;
    private boolean trackDiscAttr;

    /**
     * Creates two ArrayLists, discElems and discAttribs. The lists are not tracked by default
     * and the max size is set to -1 by default when the lists are not used.
     */
    public DiscardList(){
        elemsMaxSize = -1;
        attribsMaxSize = -1;
        discElems = new ArrayList<>();
        discAttribs = new ArrayList<>();

        trackDiscElems = false;
        trackDiscAttr = false;
    }
    /**
     * Set the max size of DiscElems
     * @param elemsMaxSize
     */
    public void setElemsMaxSize(int elemsMaxSize) {
        this.elemsMaxSize = elemsMaxSize;
    }
    /**
     * Set the max size of discAttribs
     * @param attribsMaxSize
     */
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
    /**
     * Adds the deleted tags to discElems list.
     */
    public void addElem(Node elem){
        if(trackDiscElems && (elemsMaxSize == -1 || elemsMaxSize > discElems.size())){
            discElems.add(elem.clone());
        }
    }
    /**
     * Adds the deleted attributes to discAttribs list.
     */
    public void addAttribute(Element elem, Attribute attr){
        if(trackDiscAttr && (attribsMaxSize == -1 || attribsMaxSize > discAttribs.size())){
            Element copy = new Element(Tag.valueOf(elem.tagName()), elem.baseUri(), null);
            copy.attributes().add(attr.getKey(), attr.getValue());
            discAttribs.add(copy);
        }
    }

    /**
     * @return deep copy of discElems list
     */
    public ArrayList<Node> getDiscElems(){
        ArrayList<Node> copy = new ArrayList<>(discElems.size());
        for (Node node:discElems){
            copy.add(node.clone());
        }
        return copy;
    }
    /**
     * @return deep copy of discAttribs list
     */
    public ArrayList<Node> getDiscAttribs(){
        ArrayList<Node> copy = new ArrayList<>(discAttribs.size());
        for (Node node:discAttribs){
            copy.add(node.clone());
        }
        return copy;
    }
}
