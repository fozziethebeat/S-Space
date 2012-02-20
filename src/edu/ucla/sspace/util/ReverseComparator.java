package edu.ucla.sspace.util;

import java.util.Comparator;


/**
 * A comparator that results in the opposite ordering of the natural
 * ordering from {@link Comparator#compareTo(Object,Object) compareTo}.
 */
public class ReverseComparator<K extends Comparable<? super K>> 
        implements Comparator<K>, java.io.Serializable {

    private static final long serialVersionUID = 1;
        
    /**
     * {@inheritDoc}
     */
    public int compare(K c1, K c2) {
        return -c1.compareTo(c2);
    }
}
