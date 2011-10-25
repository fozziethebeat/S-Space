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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.CombinedSet;

import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.TroveIntSet;


/**
 * An {@link EdgeSet} implementation that stores {@link TypedEdge} instances for
 * a vertex.  This class provides additional methods beyond the {@code EdgeSet}
 * interface for interacting with edges on the basis of their type.
 */
public class SparseTypedEdgeSet<T> extends AbstractSet<TypedEdge<T>> 
        implements EdgeSet<TypedEdge<T>> {
        
    /**
     * The vertex to which all edges in the set are connected
     */
    private final int rootVertex;

    private final Map<T,IntSet> typeToEdges;
        
    public SparseTypedEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        typeToEdges = new HashMap<T,IntSet>();
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(TypedEdge<T> e) {
        if (e.from() == rootVertex) {
            IntSet edges = getEdgesForType(e.edgeType());
            return edges.add(e.to());
        }
        else if (e.to() == rootVertex) {
            IntSet edges = getEdgesForType(e.edgeType());
            return edges.add(e.from());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public IntSet connected() {
        IntSet connected = new TroveIntSet();        
        for (IntSet s : typeToEdges.values())
            connected.addAll(s);
        return connected;
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        for (IntSet edges : typeToEdges.values()) {
            if (edges.contains(vertex))
                return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        if (!(o instanceof TypedEdge))
            return false;

        @SuppressWarnings("unchecked")
        TypedEdge<T> e = (TypedEdge<T>)o;
           
        if (e.from() == rootVertex) {
            IntSet edges = typeToEdges.get(e.edgeType());
            return edges != null && edges.contains(e.to());
        }
        else if (e.to() == rootVertex) {
            IntSet edges = typeToEdges.get(e.edgeType());
            return edges != null && edges.contains(e.from());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public SparseTypedEdgeSet copy(IntSet vertices) {        
        throw new Error();
//         SparseTypedEdgeSet copy = new SparseTypedEdgeSet(rootVertex);
//         if (edges.size() < vertices.size()) {
//             TIntIterator iter = edges.iterator();
//             while (iter.hasNext()) {
//                 int v = iter.next();
//                 if (vertices.contains(v))
//                     copy.edges.add(v);
//             }            
//         }
//         else {
//             IntIterator iter = vertices.iterator();
//             while (iter.hasNext()) {
//                 int v = iter.nextInt();
//                 if (edges.contains(v)) 
//                     copy.edges.add(v);
//             }
//         }
//         return copy;
    }

    /**
     * {@inheritDoc}
     */
    public boolean disconnect(int vertex) {
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getEdges(int vertex) {
        Set<TypedEdge<T>> output = new HashSet<TypedEdge<T>>();
        for (Map.Entry<T,IntSet> e : typeToEdges.entrySet()) {
            IntSet edges = e.getValue();
            if (edges.contains(vertex)) {
                output.add(new SimpleTypedEdge<T>(
                    e.getKey(), vertex, rootVertex));
            }
        }
        return output;
    }    

    /**
     * Returns the set of edges that have the specified type.
     */
    private IntSet getEdgesForType(T type) {
        IntSet edges = typeToEdges.get(type);
        if (edges == null) {
            edges = new TroveIntSet();
            typeToEdges.put(type, edges);
        }
        return edges;
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
    public Iterator<TypedEdge<T>> iterator() {
        return new TypedEdgeIterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        if (!(o instanceof TypedEdge))
            return false;

        @SuppressWarnings("unchecked")
        TypedEdge<T> e = (TypedEdge<T>)o;
           
        if (e.from() == rootVertex) {
            IntSet edges = typeToEdges.get(e.edgeType());
            return edges != null && edges.remove(e.to());
        }
        else if (e.to() == rootVertex) {
            IntSet edges = typeToEdges.get(e.edgeType());
            return edges != null && edges.remove(e.from());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */    
    public int size() {
        int sz = 0;
        for (IntSet edges : typeToEdges.values())
            sz += edges.size();
        return sz;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(typeToEdges.size() * 16);
        sb.append("{ from: " ).append(rootVertex).append(' ');
        for (Map.Entry<T,IntSet> e : typeToEdges.entrySet()) {
            sb.append("{type: ").append(e.getKey()).
                append(" to: ").append(e.getValue()).append('}');            
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * TypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class TypedEdgeIterator implements Iterator<TypedEdge<T>> {
        
        private Iterator<Map.Entry<T,IntSet>> edgeIter;
        
        private Iterator<Integer> curIter;

        private T curType;

        private Iterator<Integer> lastRemovedFrom;

        private TypedEdge<T> next;

        public TypedEdgeIterator() {
            edgeIter = typeToEdges.entrySet().iterator();
            advance();
        }

        private void advance() {
            next = null;
            if ((curIter == null || !curIter.hasNext()) && edgeIter.hasNext()) {
                Map.Entry<T,IntSet> e = edgeIter.next();
                curIter = e.getValue().iterator();
                curType = e.getKey();                
            }
        }

        public boolean hasNext() {
            return curIter.hasNext();
        }

        public TypedEdge<T> next() {
            if (!hasNext())
                throw new NoSuchElementException();
            TypedEdge<T> cur = 
                new SimpleTypedEdge<T>(curType, rootVertex, curIter.next());
            // Update the iterator on which remove() will be called
            if (lastRemovedFrom != curIter)
                lastRemovedFrom = curIter;
            return cur;
        }

        public void remove() {
            lastRemovedFrom.remove();
        }
    }
}