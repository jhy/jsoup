package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;

import java.util.ArrayList;

/**
 * A class for tracking what elements and attributes are discarded by the Cleaner class.
 *  Disabled by default. Tracking of Elements or Attributes can be enabled individually.
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
     *  The tracking are disabled by default. MaxSize being -1 means "no max size".
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
     * Set the max nr of saved elements, -1 means "no max size".
     * @param elemsMaxSize
     */
    public void setElemsMaxSize(int elemsMaxSize) {
        this.elemsMaxSize = elemsMaxSize;
    }
    /**
     * Set the max nr of saved attributes, -1 means "no max size".
     * @param attribsMaxSize
     */
    public void setAttribsMaxSize(int attribsMaxSize) {
        this.attribsMaxSize = attribsMaxSize;
    }

    /**
     * Enable tracking of Elements
     */
    public void trackDiscElems(){
        trackDiscElems = true;
    }
    /**
     * Enable tracking of Attributes
     */
    public void trackDiscAttr(){
        trackDiscAttr = true;
    }
    /**
     * Disable tracking of Elements
     */
    public void stopElemTracking(){
        trackDiscElems = false;
    }
    /**
     * Disable tracking of Attributes
     */
    public void stopAttribTracking(){
        trackDiscAttr = false;
    }
    /**
     * Adds a copy of elem to discElems list.
     */
    public void addElem(Node elem){
        if(trackDiscElems && (elemsMaxSize == -1 || elemsMaxSize > discElems.size())){
            discElems.add(elem.clone());
        }
    }
    /**
     * Adds a copy of elem with only the attribute attr to discAttribs.
     */
    public void addAttribute(Element elem, Attribute attr){
        if(trackDiscAttr && (attribsMaxSize == -1 || attribsMaxSize > discAttribs.size())){
            Element copy = new Element(Tag.valueOf(elem.tagName()), elem.baseUri(), null);
            copy.attributes().add(attr.getKey(), attr.getValue());
            discAttribs.add(copy);
        }
    }

    /**
     * Returns a ArrayList with the discarded elements.
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
     * Returns a Arraylist with discarded attributes each attached to a copy of their original element.
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
