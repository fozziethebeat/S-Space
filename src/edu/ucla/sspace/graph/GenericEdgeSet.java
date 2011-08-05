/*
 * Copyright 2011 David Jurgens
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

package edu.ucla.sspace.graph;

import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.IntSet;
import edu.ucla.sspace.util.MultiMap;


/**
 * An {@link EdgeSet} implementation that imposes no restrictions on the type of
 * edges that may be contained within.  This class keeps track of which vertices
 * are in the set as well, allowing for efficient vertex-based operations.
 *
 * <p> Due not knowing the {@link Edge} type, this class prevents modification via
 * adding or removing vertices.
 *
 * @author David Jurgens
 *
 * @param T the type of edge to be stored in the set
 */
public class GenericEdgeSet<T extends Edge> extends AbstractSet<T> 
        implements EdgeSet<T> {
        
    private final int rootVertex;
    
//     private final BitSet vertices;
    
//     private final Set<T> edges;

    private final MultiMap<Integer,T> vertexToEdges ;
   
    public GenericEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
//         edges = new HashSet<T>();
//         vertices = new BitSet();
        vertexToEdges = new HashMultiMap<Integer,T>();
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(T e) {
//         if (e.from() == rootVertex && edges.add(e)) {
//             connected.set(e.to());
//             return true;
//         }
//         else if (e.to() == rootVertex && edges.add(e)) {
//             connected.add(e.from());
//             return true;
//         }
//         return false;

        return (e.from() == rootVertex && vertexToEdges.put(e.to(), e))
            || (e.to() == rootVertex && vertexToEdges.put(e.from(), e));
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> connected() {
//         String s = "{";
//         for (int i = vertices.nextSetBit(0); i >= 0; i = vertices.nextSetBit(i+1)) 
//             s += i + ", ";
//         s += "}";
//         System.out.printf("%d connected to %d: %s%n", vertices.hashCode(), rootVertex, s);
//         return Collections.unmodifiableSet(IntSet.wrap(vertices));
        return Collections.unmodifiableSet(vertexToEdges.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        return vertexToEdges.containsKey(vertex); //vertices.get(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        if (o instanceof Edge) {
            Edge e = (Edge)o;
            return (e.from() == rootVertex && vertexToEdges.containsMapping(e.to(), e))
                || (e.to() == rootVertex && vertexToEdges.containsMapping(e.from(), e));            
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> getEdges(int vertex) {
        Set<T> s = vertexToEdges.get(vertex);
        return (s == null) 
            ? Collections.<T>emptySet()
            : s;
    }    

    /**
     * {@inheritDoc}
     */
    public int getRoot() {
        return rootVertex;
    }

    /**
     * {@inheritDoc}
     */ 
    public Iterator<T> iterator() {
        return vertexToEdges.values().iterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        if (o instanceof Edge) {
            Edge e = (Edge)o;
            return (e.from() == rootVertex && vertexToEdges.remove(e.to(), e))
                || (e.to() == rootVertex && vertexToEdges.remove(e.from(), e));            
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */    
    public int size() {
        return vertexToEdges.range();
    }
}