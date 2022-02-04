package org.jsoup.parser;

import java.util.ArrayList;

/**
 * A container for ParseErrors.
 * 
 * @author Jonathan Hedley
 */
public class ParseErrorList extends ArrayList<ParseError>{
    private static final int INITIAL_CAPACITY = 16;
    private final int initialCapacity;
    private final int maxSize;
    
    ParseErrorList(int initialCapacity, int maxSize) {
        super(initialCapacity);
        this.initialCapacity = initialCapacity;
        this.maxSize = maxSize;
    }

    /**
     Create a new ParseErrorList with the same settings, but no errors in the list
     @param copy initial and max size details to copy
     */
    ParseErrorList(ParseErrorList copy) {
        this(copy.initialCapacity, copy.maxSize);
    }
    
    boolean canAddError() {
        return size() < maxSize;
    }

    int getMaxSize() {
        return maxSize;
    }

    public static ParseErrorList noTracking() {
        return new ParseErrorList(0, 0);
    }
    
    public static ParseErrorList tracking(int maxSize) {
        return new ParseErrorList(INITIAL_CAPACITY, maxSize);
    }

    @Override
    public Object clone() {
        // all class fields are primitive, so native clone is enough.
        return super.clone();
    }
}
