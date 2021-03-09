package org.jsoup.safety;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Tag;

import java.util.ArrayList;

/**
 * A class for tracking what tagents and attributes are discarded by the Cleaner class.
 *  Disabled by default. Tracking of Elements or Attributes can be enabled individually.
 * @author Sebastian Fagerlind sebene@kth.se and Eleonora Borzi borzi@kth.se
 */

public class DiscardList {

    private ArrayList<Node> discTags;
    private int tagsMaxSize;
    private boolean trackDiscTags;

    private ArrayList<Node> discAttribs;
    private int attribsMaxSize;
    private boolean trackDiscAttr;

    /**
     *  The tracking are disabled by default. MaxSize being -1 means "no max size".
     */
    public DiscardList(){
        tagsMaxSize = -1;
        attribsMaxSize = -1;
        discTags = new ArrayList<>();
        discAttribs = new ArrayList<>();

        trackDiscTags = false;
        trackDiscAttr = false;
    }
    /**
     * Set the max nr of saved tagents, -1 means "no max size".
     * @param tagsMaxSize
     */
    public void setElemsMaxSize(int tagsMaxSize) {
        this.tagsMaxSize = tagsMaxSize;
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
    public void trackDiscTags(){
        trackDiscTags = true;
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
    public void stopTagsTracking(){
        trackDiscTags = false;
    }
    /**
     * Disable tracking of Attributes
     */
    public void stopAttribTracking(){
        trackDiscAttr = false;
    }
    /**
     * Adds a copy of tag to discTags list.
     */
    public void addTag(Node tag){
        if(trackDiscTags && (tagsMaxSize == -1 || tagsMaxSize > discTags.size())){
            discTags.add(tag.clone());
        }
    }
    /**
     * Adds a copy of tag with only the attribute attr to discAttribs.
     */
    public void addAttribute(Element tag, Attribute attr){
        if(trackDiscAttr && (attribsMaxSize == -1 || attribsMaxSize > discAttribs.size())){
            Element copy = new Element(Tag.valueOf(tag.tagName()), tag.baseUri(), null);
            copy.attributes().add(attr.getKey(), attr.getValue());
            discAttribs.add(copy);
        }
    }

    /**
     * Returns a ArrayList with the discarded tagents.
     * @return deep copy of discTags list
     */
    public ArrayList<Node> getDiscTags(){
        ArrayList<Node> copy = new ArrayList<>(discTags.size());
        for (Node node:discTags){
            copy.add(node.clone());
        }
        return copy;
    }
    /**
     * Returns a Arraylist with discarded attributes each attached to a copy of their original tagent.
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
