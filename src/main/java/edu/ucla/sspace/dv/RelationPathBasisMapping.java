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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class RelationPathBasisMapping implements DependencyPathBasisMapping {

    /**
     * A map that represents the word space by mapping terms and their relations
     * to the dimension they are represented by.
     */
    private Map<PathSignature,Integer> pathToIndex;

    /**
     * A cache of the reverse {@code pathToIndex} mapping.  This field is only
     * updated on calls to {@link #getDimensionDescription(int)} when the
     * mapping has chanaged since the previous call.
     */
    private String[] indexToPathCache;

    /**
     * If set to true, the basis mapping will not create mappings for unseen
     * dimensions.
     */
    private boolean readOnly;

    /**
     * Creates an empty {@code PathBasedBasisMapping}.
     */
    public RelationPathBasisMapping() {
        pathToIndex = new HashMap<PathSignature,Integer>();
        indexToPathCache = new String[0];
        readOnly = false;
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
        return getDimension(new PathSignature(path));
    }

    /**
     * Returns the dimension represention the occurrence of the provided path.
     * If the path was not previously assigned an index, this method adds one
     * for it and returns that index.
     */
    private int getDimension(PathSignature path) {

        Integer index = pathToIndex.get(path);
        if (index == null && !readOnly) {     
            synchronized(this) {
                // recheck to see if the term was added while blocking
                index = pathToIndex.get(path);
                // if another thread has not already added this word while the
                // current thread was blocking waiting on the lock, then add it.
                if (index == null) {
                    int i = pathToIndex.size();
                    pathToIndex.put(path, i);
                    return i; // avoid the auto-boxing to assign i to index
                }
            }
        }
        return index;
    }

    /**
     * Returns the path mapped to the provided dimension.  Each path is a
     * tab-delineated sequence of word and relations, e.g. "cat SBJ eats"
     */
    public String getDimensionDescription(int dimension) {
        if (dimension < 0 || dimension > pathToIndex.size())
            throw new IllegalArgumentException(
                "invalid dimension: " + dimension);
        // If the cache is out of date, rebuild the reverse mapping.
        if (pathToIndex.size() > indexToPathCache.length) {
            // Lock to ensure safe iteration
            synchronized(this) {
                indexToPathCache = 
                    new String[pathToIndex.size()];
                for (Map.Entry<PathSignature,Integer> e : 
                         pathToIndex.entrySet())
                    indexToPathCache[e.getValue()] = e.getKey().toString();
            }
        }
        return indexToPathCache[dimension];
    }

    /**
     * {@inheritDoc}
     */
    public int numDimensions() { 
        return pathToIndex.size();
    }

    /**
     * {@inheritDoc}
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Returns the set of keys known by this {@link BasisMapping}
     */
    public Set<String> keySet() {
        return null;
    }

    /**
     */
    private static class PathSignature {

        /**
         * The relations connecting the words in the path, in order from the
         * root to the last node
         */
        private final String[] relations;

        /**
         * The lazily computed hash code of the signature
         */
        private int hashCode = 0; // lazily computed

        public PathSignature(DependencyPath path) {
            relations = new String[path.length() - 1];
            for (int i = 0; i < path.length(); ++i) {
                if (i + 1 < path.length()) 
                    relations[i] = path.getRelation(i);
            }
        }

        public boolean equals(Object o) {
            if (o instanceof PathSignature) {
                PathSignature p = (PathSignature)o;
                return hashCode() == p.hashCode() &&
                       Arrays.equals(relations, p.relations);
            }
            return false;
        }

        public int hashCode() {
            if (hashCode == 0)
                hashCode = Arrays.hashCode(relations);
            return hashCode;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(relations[0]);
            for (int i = 0; i < relations.length; ++i)
                sb.append("-").append(relations[i]);
            return sb.toString();
        }
    }
}
