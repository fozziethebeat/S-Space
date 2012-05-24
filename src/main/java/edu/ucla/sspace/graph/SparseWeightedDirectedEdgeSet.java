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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.CombinedIterator;

import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.TroveIntSet;

import gnu.trove.TDecorators;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;


/**
 * A set for containing {@link WeightedEdge} instances.  Note that the equality
 * condition for {@link WeightedEdge} is treated specially in this set such that
 * two vertices will only have at most one edge between them.  If an edge exists
 * for vertices {@code i} and {@code j} with weight {@code w}<sub>1</sub>, then
 * adding a new edge to the same vertices with weight {@code w}<sub>2</sub> will
 * not add a parallel edge and increase the size of this set, even though the
 * edges are not equal.  Rather, the weight on the edge between the two vertices
 * is changed to {@code w}<sub>2</sub>.  Similarly, any contains or removal
 * operation will return its value based on the {@code WeightedEdge}'s vertices
 * but not on the weight of the edge itself.
 */
public abstract class SparseWeightedDirectedEdgeSet extends AbstractSet<WeightedEdge> 
        implements EdgeSet<WeightedEdge>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

//     // IMPLEMENTATION NOTE: the TIntDoubleMap operations will return 0 for
//     // methods that would normally return null if the key was not present.  For
//     // example, remove() could return 0 if either the key was mapped to 0, or if
//     // the key was not already present.  Therefore, we precede most of these
//     // calls with a containsKey operation to ensure that this class's return
//     // values are correct.

//     /**
//      * The vertex that is connected to all the edges in this set
//      */
//     private final int rootVertex;

//     /**
//      * The mapping from set of vertices that have an edge to the root vertex,
//      * with the weight associated with each connection.
//      */
//     private final TIntDoubleMap inEges;

//     /**
//      * The mapping from set of vertices that have an edge from the root vertex,
//      * with the weight associated with each connection.
//      */
//     private final TIntDoubleMap outEges;
    
//     /**
//      * Creates a new {@code SparseWeightedEdgeSet} where all edges in this set
//      * must connect to {@code rootVertex}.
//      */
//     public SparseWeightedEdgeSet(int rootVertex) {
//         this.rootVertex = rootVertex;
//         inEdges = new TIntDoubleHashMap();
//         outEdges = new TIntDoubleHashMap();
//     }
    
//     /**
//      * Adds the edge to this set if one of the vertices is the root vertex.
//      * 
//      * @return {@code true} if e was added or if the weight was changed for the
//      *         conenction between an existing edge that matched
//      */
//     public boolean add(WeightedEdge e) {
//         if (e.from() == rootVertex) {
//             double w = e.weight();
//             int toAdd = e.to();
//             if (edges.containsKey(toAdd)) {
//                 double w2 = edges.put(toAdd, w);
//                 // The weight was changed but a new edge was not added
//                 return false;
//             }
//             else {
//                 edges.put(toAdd, w);
//                 return true;
//             }
//         }
//         else if (e.to() == rootVertex) {
//             double w = e.weight();
//             int toAdd = e.from();
//             if (edges.containsKey(toAdd)) {
//                 double w2 = edges.put(toAdd, w);
//                 // The weight was changed but a new edge was not added
//                 return false;
//             }
//             else {
//                 edges.put(toAdd, w);
//                 return true;
//         }
//         else 
//             return false;
//     }

//     /**
//      * {@inheritDoc}
//      */
//     public IntSet connected() {
//         return new CombinedSet();
//     }

//     /**
//      * {@inheritDoc}
//      */
//     public boolean connects(int vertex) {
//         return inEdges.contains(vertex) || outEdges.contains(vertex);
//     }

//     /**
//      * {@inheritDoc}
//      */
//     public boolean contains(Object o) {
//         if (o instanceof WeightedEdge && o instanceof DirectedEdge) {
//             WeightedEdge e = (WeightedEdge)o;
//             int toFind = 0;
//             if (e.to() == rootVertex) {
//                 return inEdge.containsKey(e.from()) 
//                     && inEdges.get(e.from()) == e.weight();
//             }
//             else if (e.from() == rootVertex) {
//                 return outEdge.containsKey(e.to()) 
//                     && outEdges.get(e.to()) == e.weight();
//             }
//             else 
//                 return false;            
//         }
//         return false;
//     }

//     /**
//      * {@inheritDoc}
//      */
//     public SparseWeightedEdgeSet copy(IntSet vertices) {        
//         // REMINDER: optimize this
//         SparseWeightedDirectedEdgeSet copy = new SparseWeightedDirectedEdgeSet(rootVertex);
//         IntIterator iter = vertices.iterator();
//         while (iter.hasNext()) {
//             int v = iter.nextInt();
//             if (inEdges.containsKey(v))
//                 copy.inEdges.put(v, inEdges.get(v));
//             if (ouEdges.containsKey(v))
//                 copy.outEdges.put(v, outedges.get(v));
//         }
//         return copy;
//     }

//     /**
//      * {@inheritDoc}
//      */
//     public int disconnect(int vertex) {
//         int removed = 0;
//         if (inEdges.remove(vertex))
//             removed++;
//         if (outEdges.remove(vertex))
//             removed++;
//         return removed;
//     }

//     /**
//      * {@inheritDoc}
//      */
//     public Set<WeightedEdge> getEdges(int vertex) {
//         return new VertexEdgeSet(vertex);
//     }    

//     /**
//      * {@inheritDoc}
//      */
//     public int getRoot() {
//         return rootVertex;
//     }

//     /**
//      * {@inheritDoc}
//      */ 
//     public Iterator<WeightedEdge> iterator() {
//         return new WeightedEdgeIterator();
//     }
    
//     /**
//      * {@inheritDoc}
//      */
//     public boolean remove(Object o) {
//         if (o instanceof WeightedEdge && o instanceof DirectedEdge) {
//             WeightedEdge e = (WeightedEdge)o;
//             if (e.to() == rootVertex) 
//                 return inEdge.remove(e.from());
//             else if (e.from() == rootVertex) 
//                 return outEdge.remove(e.to()) 
//         }
//         return false;
//     }

//     /**
//      * {@inheritDoc}
//      */    
//     public int size() {
//         return inEdges.size() + outEdges.size();
//     }

//     /**
//      * Returns the sum of the weights of the edges contained in this set.
//      */
//     public double sum() {
//         double sum = 0;
//         TIntDoubleIterator iter = inEdges.iterator();
//         while (iter.hasNext()) {
//             iter.advance();
//             sum += iter.value();
//         }
//         iter = outEdges.iterator();
//         while (iter.hasNext()) {
//             iter.advance();
//             sum += iter.value();
//         }
//         return sum;
//     }

//     /**
//      * An iterator over the edges in this set that constructs {@link WeightedEdge}
//      * instances as it traverses through the set of connected vertices.
//      */
//     private class WeightedEdgeIterator implements Iterator<WeightedEdge> {

//         private TIntDoubleIterator iter;

//         private boolean alreadyRemoved;
        
//         public WeightedEdgeIterator() {
//             iter = edges.iterator();
//             alreadyRemoved = false;
//         }

//         public boolean hasNext() {
//             return iter.hasNext();
//         }

//         public WeightedEdge next() {
//             iter.advance();
//             alreadyRemoved = false;
//             return new SimpleWeightedEdge(rootVertex, iter.key(), iter.value());
//         }

//         public void remove() {
//             if (alreadyRemoved)
//                 throw new IllegalStateException();
//             // For some reason this doesn't through the expected Iterator API
//             // exception, so we try/rethrow as necessary.
//             try {
//                 iter.remove();
//                 alreadyRemoved = true;
//             } catch (ArrayIndexOutOfBoundsException aioobe) {
//                 throw new IllegalStateException();
//             }
//         }
//     }    
}