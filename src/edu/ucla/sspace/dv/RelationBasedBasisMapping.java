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

package edu.ucla.sspace.dv;

import edu.ucla.sspace.dependency.DependencyPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A {@link BasisMapping} implementation where each word and relation
 * combination corresponds to a unique dimension.  For example "bowl" occuring
 * with the "{@code SBJ}" relation will be treated as a seperate dimension than
 * "bowl" with the "{@code OBJ}" relation.
 *
 * @author David Jurgens
 */
public class RelationBasedBasisMapping implements DependencyPathBasisMapping {

    /**
     * A map that represents the word space by mapping terms and their relations
     * to the dimension they are represented by.
     */
    private Map<String,Integer> termAndRelationToIndex;

    /**
     * A cache of the reverse {@code termToIndex} mapping.  This field is only
     * updated on calls to {@link #getDimensionDescription(int)} when the
     * mapping has chanaged since the previous call.
     */
    private String[] indexToTermAndRelationCache;

    /**
     * Creates an empty {@code RelationBasedBasisMapping}.
     */
    public RelationBasedBasisMapping() {
        termAndRelationToIndex = new HashMap<String,Integer>();
        indexToTermAndRelationCache = new String[0];
    }

    /**
     * Returns the dimension number corresponding to the term at the end of the
     * provided path.
     *
     * @param path a path whose end represents a semantic connection
     *
     * @return the dimension for the occurrence of the last word in the path
     */
    public int getDimension(DependencyPath path) {
        String endToken = path.last().word();
        // Extract out how the current word is related to the last word in the
        // path.  The last relation is the length - 2, due to length - 1 being
        // the last node index and there are one-fewer relations than nodes.
        String relation = path.getRelation(path.length() - 2);
        return getDimension(endToken + "+" + relation);
    }

    /**
     * Returns the dimension represention the occurrence of the provided token.
     * If the token was not previously assigned an index, this method adds one
     * for it and returns that index.
     */
    private int getDimension(String tokenAndRelation) {

        Integer index = termAndRelationToIndex.get(tokenAndRelation);
        if (index == null) {     
            synchronized(this) {
                // recheck to see if the term was added while blocking
                index = termAndRelationToIndex.get(tokenAndRelation);
                // if another thread has not already added this word while the
                // current thread was blocking waiting on the lock, then add it.
                if (index == null) {
                    int i = termAndRelationToIndex.size();
                    termAndRelationToIndex.put(tokenAndRelation, i);
                    return i; // avoid the auto-boxing to assign i to index
                }
            }
        }
        return index;
    }

    /**
     * Returns the word mapped to each dimension appended with a "+" and the
     * relation with which the word occurs.  For example, "clock" occurring with
     * the OBJ relation would return "clock+OBJ" for its dimension description.
     */
    public String getDimensionDescription(int dimension) {
        if (dimension < 0 || dimension > termAndRelationToIndex.size())
            throw new IllegalArgumentException(
                "invalid dimension: " + dimension);
        // If the cache is out of date, rebuild the reverse mapping.
        if (termAndRelationToIndex.size() > indexToTermAndRelationCache.length) {
            // Lock to ensure safe iteration
            synchronized(this) {
                indexToTermAndRelationCache = 
                    new String[termAndRelationToIndex.size()];
                for (Map.Entry<String,Integer> e : 
                         termAndRelationToIndex.entrySet())
                    indexToTermAndRelationCache[e.getValue()] = e.getKey();
            }
        }
        return indexToTermAndRelationCache[dimension];
    }

    /**
     * {@inheritDoc}
     */
    public int numDimensions() { 
        return termAndRelationToIndex.size();
    }
}