/*
 * Copyright 2010 David Jurgens
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.dependency;

import edu.ucla.sspace.util.CombinedIterator;

import java.util.Collections;
import java.util.Iterator;


/**
 * A utility subclass for extending a dependency path without requring that all
 * the relations in the path be copied.  This class essentially creates a view
 * of the existing path data with an additional relation that is treated as the
 * last in the path.
 */
class ExtendedPathView implements DependencyPath {

    /**
     * The path whose relations form the starting relations of this path.
     */
    private final DependencyPath original;
    
    /**
     * The relation that is now the last relation in the path
     */
    private final DependencyRelation extension;

    /**
     * The length of this path.
     */
    private final int length;
    
    /**
     * Creates a new {@code DependencyPath} based on the nodes in an existing
     * path with the provided relation becoming the end of the new path.
     */
    public ExtendedPathView(DependencyPath original,
                            DependencyRelation extension) {
        this.original = original;
        this.extension = extension;
        // Due to the recursive nature of this class (i.e. multiple extensions
        // nested on top of each other).  The length is computed once during
        // construction and cached to avoid a possible linear overhead per
        // length() call.
        length = original.length() + 1;
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode first() {
        return original.first();
    }
    
    /**
     * {@inheritDoc}
     */
    public DependencyRelation firstRelation() {
        return original.firstRelation();
    }    

    /**
     * Given the nodes in the previous relation, determine which of the nodes in
     * the next relation is new and return that.
     *
     * @param prev the dependency relation that was previously seen in the path
     * @param cur the current dependency relation 
     */
    private DependencyTreeNode getNextNode(DependencyRelation prev,
                                           DependencyRelation cur) {
        return (prev.headNode() == cur.headNode() 
                || prev.dependentNode() ==  cur.headNode())
            ? cur.dependentNode()
            : cur.headNode();
    }
    
    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode getNode(int position) {
        if (position < length - 1)
            return original.getNode(position);
        // Check that the request isn't for an invalid index
        else if (position > length)
            throw new IllegalArgumentException("invalid node: " + position);
        else
            return last();
    }

    /**
     * {@inheritDoc}
     */
    public String getRelation(int position) {
        if (position < length - 1)
            return original.getRelation(position);
        // Check that the request isn't for an invalid index
        else if (position >= length)
            throw new IllegalArgumentException("invalid relation: " + position);
        else 
            return extension.relation();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DependencyRelation> iterator() {
        // Wrap the nodes returned by the existing path and the extended
        // relation into a combined iterator.
        //
        // NB: The type inferencer has problems equating the return type of
        // Collections.singleton().iterator() with the desired type, so we need
        // the suppress warnings to make the compile clean.
        @SuppressWarnings("unchecked")
            Iterator<DependencyRelation> it = 
            new CombinedIterator<DependencyRelation>(original.iterator(), 
            Collections.<DependencyRelation>singleton(extension).iterator());
        return it;
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode last() {
        return getNextNode(original.lastRelation(), extension);
    }
    
    /**
     * {@inheritDoc}
     */
    public DependencyRelation lastRelation() {
        return extension;
    }    
    
    /**
     * {@inheritDoc}
     */
    public int length() {
        return length;
    }    

    /**
     * Returns the path in order with words and relations space delimited.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(8 * length);
        sb.append('[');
        for (int i = 0; i < length; ++i) {
            sb.append(getNode(i).word());
            if (i < length - 1)
                sb.append(' ').append(getRelation(i)).append(' ');
        }
        return sb.append(']').toString();        
    }
}
