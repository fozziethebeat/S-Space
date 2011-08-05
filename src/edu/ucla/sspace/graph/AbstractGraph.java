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

import java.lang.ref.WeakReference;

import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import edu.ucla.sspace.util.CombinedIterator; 
import edu.ucla.sspace.util.IntegerMap;
import edu.ucla.sspace.util.OpenIntSet; 


/**
 * A base class for many {@link Graph} implementations.  The core functionality
 * of this class is provided by the {@link EdgeSet} instances returned by the
 * subclass for specifying how edges are to be stored and which edges are valid.
 * All calls to these sets are wrapped to ensure proper state is maintained by
 * this {@code AbstractGraph} instance.
 *
 * <p> This class support all optional {@link Graph} methods provided that the
 * {@link EdgeSet} implementations used by the subclass also support them.
 * Furthermore, all methods that return non-empty collections of {@link Edge}
 * instance can be used to modify the state of this graph by any of their
 * respective mutation methods (e.g., adding or removing {@code Edge}
 * instances).  In addition, changes to the set of vertices returned by {@link
 * #vertices()} has the same effect as adding and removing vertices to this
 * graph.  Subclasses that wish to avoid this behavior may override these calls
 * and wrap this classes return value in a {@link
 * Collections#unmodifiableSet(Set)}.
 *
 * @author David Jurgens
 */
public abstract class AbstractGraph<T extends Edge,S extends EdgeSet<T>>
        implements Graph<T>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The highest vertex seen thusfar in the graph.  This value is used by
     * Graph instances created with {@link #subgraph(Set)} to add new vertices
     * to the graph without overwriting existing vertices' indices.
     */
    private int highestVertex;

    /**
     * Records the number of modifications to the number of vertices within this
     * Graph.  Instances created with {@link #subgraph(Set)} use this to detect
     * structural changes that may invalidate their cached view.
     */
    private int mods;

    /**
     * The number of edges in this graph.
     */
    private int numEdges;

    /**
     * A mapping from a vertex's index to the the set of {@link Edge} instances
     * that connect it to other members of the graph.
     */
    private final Map<Integer,S> vertexToEdges;

    /**
     * A collection of all the subgraphs that have been returned from this
     * graph.  This collection is necessary to inform subgraphs of any vertex
     * changes (removals) to this graph without requiring them to constantly
     * check for updates.  We use a {@link WeakReference} in order to keep track
     * of the canonical {@code Subgraph} instance while ensuring that it is
     * garbage collected when it is no longer referred to (which would never
     * happen if this list contained strong references).
     */
    private Collection<WeakReference<Subgraph>> subgraphs;

    /**
     * Creates an empty {@code AbstractGraph}
     */
    public AbstractGraph() {
        mods = 0;
        vertexToEdges = new HashMap<Integer,S>();
        subgraphs = new ArrayList<WeakReference<Subgraph>>();
    }    

    /**
     * Creates a new {@code AbstractGraph} with the provided set of vertices.
     */
    public AbstractGraph(Set<Integer> vertices) {
        this();
        for (Integer v : vertices)
            vertexToEdges.put(v, createEdgeSet(v));
    }    

    /**
     * Returns the {@link EdgeSet} for this vertex, adding the vertex to this
     * graph if abstent.
     */
    private EdgeSet<T> addIfAbsent(int v) {
        S edges = getEdgeSet(v);
        if (edges == null) {
            edges = createEdgeSet(v);
            vertexToEdges.put(v, edges);
            if (v > highestVertex)
                highestVertex = v;
            mods++;
        }
        return edges;
    }
        
    /**
     * {@inheritDoc}
     */
    public boolean add(int v) {
        S edges = getEdgeSet(v);
        if (edges == null) {
            edges = createEdgeSet(v);
            vertexToEdges.put(v, edges);
            if (v > highestVertex)
                highestVertex = v;
            mods++;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(T e) {
        EdgeSet<T> from = addIfAbsent(e.from());
        EdgeSet<T> to = addIfAbsent(e.to());

        // Add this edge for the vertex to which the edge is pointing.  This
        // double-add behavior is necessary to ensure that the EdgeSet for each
        // vertices contains all the edges that connect to that vertex.
        boolean isNew = from.add(e);
        to.add(e);
        if (isNew) 
            numEdges++;
        
        assert from.contains(e) : "error in EdgeSet contains logic";
        assert to.contains(e) : "error in EdgeSet contains logic";

        return isNew;
    }

    private void checkIndex(int vertex) {
        if (vertex < 0)
            throw new IllegalArgumentException("vertices must be non-negative");
    }

    /**
     * {@inheritDoc} 
     */
    public void clear() {
        vertexToEdges.clear();
        numEdges = 0;
        mods++;
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges() {
        for (EdgeSet<T> e : vertexToEdges.values())
            e.clear();
        numEdges = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex) {
        return vertexToEdges.containsKey(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2) {
        EdgeSet<T> e1 = getEdgeSet(vertex1);
        return e1 != null && e1.connects(vertex2);
    }

    /**
     * {@inheritDoc}
     *
     * <p> This method is sensitive to the vertex ordering; a call will check
     * whether the edge set for {@code e.from()} contains {@code e}.  Subclasses
     * should override this method if their {@link EdgeSet} implementations are
     * sensitive to the ordering of the vertex indices, or if a more advanced
     * behavior is needed.
     */
    public boolean contains(Edge e) {
        EdgeSet<T> e1 = getEdgeSet(e.from());        
        return e1 != null && e1.contains(e);
    }

    /**
     * {@inheritDoc}
     */
    public abstract Graph<T> copy(Set<Integer> vertices);

    /**
     * Returns a {@link EdgeSet} that will be used to store the edges of the
     * specified vertex
     */
    protected abstract S createEdgeSet(int vertex);
    
    /**
     * {@inheritDoc}
     */
    public int degree(int vertex) {
        EdgeSet<T> e = getEdgeSet(vertex);
        return (e == null) ? 0 : e.size();
    }

    /**
     * A utility method for casting an {@link Edge} instance to the
     * parameterized type of this graph.  
     */    
    @SuppressWarnings("unchecked") private T edgeCast(Edge e) {
        // This method isolates the suppress warnings annotation to a single
        // method in order that if future language feaures make it safer to make
        // this cast, the code may be easily updated for all code paths.
        T edge = (T)e;
        return edge;
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> edges() {
        return new EdgeView();
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (o instanceof Graph) {
            Graph<?> g = (Graph<?>)o;
            return g.order() == order()
                && g.size() == size()
                && g.vertices().equals(vertices())
                && g.edges().equals(edges());                    
        }
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<T> getAdjacencyList(int vertex) {
        EdgeSet<T> e = getEdgeSet(vertex);
        return (e == null) 
            ? Collections.<T>emptySet() 
            : new AdjacencyListView(e);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> getNeighbors(int vertex) {
        EdgeSet<T> e = getEdgeSet(vertex);
//         System.out.printf("%d AS's neighbors of %d: %s%n", System.identityHashCode(this), vertex,
//                           (e == null)  ? null : e.connected());
        return (e == null) 
            ? Collections.<Integer>emptySet()
            : new AdjacentVerticesView(e.connected());
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> getEdges(int vertex1, int vertex2) {
        EdgeSet<T> e = getEdgeSet(vertex1);
        if (e == null)
            return Collections.<T>emptySet();
        Set<T> edges = e.getEdges(vertex2);
        return edges.isEmpty() ? Collections.<T>emptySet() : edges;
    }

    /**
     * Returns the set of edges assocated with the vertex, or {@code null} if
     * this vertex is not in this graph.
     */
    protected S getEdgeSet(int vertex) {
        return vertexToEdges.get(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasCycles() {
        throw new UnsupportedOperationException("fix me");
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return vertices().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Integer> iterator() {
        return new VertexView().iterator();
    }
    
    /**
     * An internal method used by subgraphs to add new vertices to this graph
     * without clobbering the existing vertices.
     *
     * @see #subgraph(Set)
     */
    int nextAvailableVertex() {
        return highestVertex + 1;
    }

    /**
     * {@inheritDoc}
     */
    public int order() {
        return vertexToEdges.size();
    }

    /**
     * {@inheritDoc}  
     *
     * <p> This method is sensitive to the vertex ordering; a call will remove
     * the vertex for {@code e.to()} from the edge for {@code e.from()}.
     * Subclasses should override this method if their {@link EdgeSet}
     * implementations are sensitive to the ordering of the vertex indices, or
     * if a more advanced behavior is needed.
     */
    public boolean remove(Edge e) {
        EdgeSet<T> from = getEdgeSet(e.from());
        EdgeSet<T> to = getEdgeSet(e.to());
        
        int before = numEdges;
        if (from != null && from.remove(e)) {
            numEdges--;
            assert to.contains(e)
                : "Complementary edge set " + to + "  did not contain " + e;
            // Remove the edge from the EdgeSet for the vertex to which this
            // edge points.
            to.remove(e);
        }

        return before != numEdges;        
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(int vertex) {
        EdgeSet<T> edges = vertexToEdges.remove(vertex);
        if (edges == null)
            return false;
        // Call the internal remove method to perform the remaining
        // removal logic.
        removeInternal(vertex, edges);
        return true;
    }

    /**
     * Removes the edges of the provided vertex from this graph, accounting for
     * the presence of the edges in the corresponding {@link EdgeSet}'s for the
     * other vertex in each edge.  This method should only be called once a
     * vertex has been removed from the {@link #vertexToEdges} mapping.
     */
    private void removeInternal(int vertex, EdgeSet<T> edges) {
        // Discount all the edges that were stored in this vertices edge set
        numEdges -= edges.size();

        // Now find all the edges stored in other vertices that might point to
        // this vertex and remove them
        for (Edge e : edges) {
            // Identify the other vertex in the removed edge and remove the
            // edge from the vertex's corresponding EdgeSet.
            int otherVertex = (e.from() == vertex) ? e.to() : e.from();
            EdgeSet<T> otherEdges = vertexToEdges.get(otherVertex);
            assert otherEdges.contains(e) 
                : "Error in ensuring consistent from/to edge sets";
            otherEdges.remove(e);            
        }

        // Update any of the subgraphs that had this vertex to notify them
        // that it was removed
        Iterator<WeakReference<Subgraph>> iter = subgraphs.iterator();
        while (iter.hasNext()) {
            WeakReference<Subgraph> ref = iter.next();
            Subgraph s = ref.get();
            // Check whether this subgraph was already gc'd (the subgraph
            // was no longer in use) and if so, remove the ref from the list
            // to avoid iterating over it again
            if (s == null) {
                iter.remove();
                continue;
            }

            // Remove the vertex from the subgraph
            s.vertexSubset.remove(vertex);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public int size() {
        return numEdges;
    }

    /**
     * {@inheritDoc}
     */
    public Graph<T> subgraph(Set<Integer> vertices) {
        Subgraph sub = new Subgraph(vertices);
        subgraphs.add(new WeakReference<Subgraph>(sub));
        return sub;
    }

    /**
     * Returns a description of the graph as the sequence of its edges.
     */
    public String toString() {
        // REMINDER: make this more efficient with a StringBuilder
        return "{ vertices: " + vertices() + ", edges: " + edges() + "}";
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> vertices() {
        return new VertexView();
    }

    /**
     * A view of this graph's vertices, which provides support for adding,
     * removing, and iterating.  This class implements all optional methods for
     * {@link Set} and {@link Iterator}.
     */
    private class VertexView extends AbstractSet<Integer> {
        
        public VertexView() { }

        public boolean add(Integer vertex) {
            return AbstractGraph.this.add(vertex);
        }

        public boolean contains(Integer vertex) {
            return AbstractGraph.this.contains(vertex);
        }

        public Iterator<Integer> iterator() {
            return new VertexIterator();
        }

        public boolean remove(Integer i) {
            return AbstractGraph.this.remove(i);
        }

        public int size() {
            return order();
        }

        private class VertexIterator implements Iterator<Integer> {

            /**
             * The Iterator over the current vertex set
             */
            private Iterator<Map.Entry<Integer,S>> vertices;

            /**
             * The next vertex to return
             */
            private Map.Entry<Integer,S> lastReturned;

            public VertexIterator() {
                vertices = vertexToEdges.entrySet().iterator();
                lastReturned = null;
            }

            public boolean hasNext() {
                return vertices.hasNext();
            }

            public Integer next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return (lastReturned = vertices.next()).getKey();
            }

            public void remove() {
                if (lastReturned == null)
                    throw new IllegalStateException("no element to remove");
                // Note that because we need to remove edges from the vertex,
                // ideally, we would want to call remove().  However, this
                // would modify the state of the vertexToEdge Map, which would
                // break the iterator (concurrent modification).  Therefore, we
                // remove the vertex from the iterator and then perform the rest
                // of the logic using removeInternal()
                vertices.remove();
                Integer removed = lastReturned.getKey();
                EdgeSet<T> edges = lastReturned.getValue();
                removeInternal(removed, edges);
                // Finally, null out the last removed value to prevent a double
                // removal
                lastReturned = null;
            }                
        }
    }

    /**
     * A view for the {@code Edge} adjacency list of a vertex.  This class
     * monitors for changes to edge set to update the state of this graph
     */
    private class AdjacencyListView extends AbstractSet<T> {

        private final EdgeSet<T> adjacencyList;

        public AdjacencyListView(EdgeSet<T> adjacencyList) {            
            this.adjacencyList = adjacencyList;
        }

        /**
         * Adds an edge to this vertex and adds the vertex to the graph if it
         * was not present before.
         */
        @Override public boolean add(T edge) {
            // If we've added a new edge to this vertex's adjacency list, check
            // whether we've added a new vertex to the graph
            if (adjacencyList.add(edge)) { 

                // Figure out which vertex was newly connected
                int otherVertex = (edge.from() == adjacencyList.getRoot())
                    ? edge.to() : edge.from();
                if (!vertexToEdges.containsKey(otherVertex)) {
                    AbstractGraph.this.add(otherVertex);
                }
                // Last, add this edge to the EdgeSet for the other vertex in
                // the edge
                vertexToEdges.get(otherVertex).add(edge);
                numEdges++;
                return true;
            }
            return false;
        }

        @Override public boolean contains(Object edge) {
            return adjacencyList.contains(edge);
        }

        public Iterator<T> iterator() {
            return new AdjacencyListIterator();
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof Edge))
                return false;
            Edge edge = (Edge)o;
            // If the vertex was successfully removed, we need to remove the
            // edge from the edge set for the other vertex in the edge
            if (adjacencyList.remove(edge)) {
                // Determine the non-root vertex in the edge
                int otherVertex = (edge.from() == adjacencyList.getRoot())
                    ? edge.to() : edge.from();
                // Then remove the edge from its adjacency list as well
                vertexToEdges.get(otherVertex).remove(edge);

                numEdges--;
                return true;
            }
            return false;
        }

        public int size() {
            return adjacencyList.size();
        }

        /**
         * A decorator around the iterator for an adjacency list, which tracks
         * edges removal to update the number of edges in the graph.
         */
        private class AdjacencyListIterator implements Iterator<T> {

            private final Iterator<T> edges;

            public AdjacencyListIterator() {
                edges = adjacencyList.iterator();
            }

            public boolean hasNext() {
                return edges.hasNext();
            }

            public T next() {
                return edges.next();
            }

            public void remove() {
                edges.remove();
                numEdges--;
            }            
        }

    }

    /**
     * A view of a vertex's adjacencent vertices that monitors for additions and
     * removals to the set in order to update the state of this {@code Graph}.
     */
    private class AdjacentVerticesView extends AbstractSet<Integer> {

        /**
         * The set of adjacent vertices to a vertex.  This set is itself a view
         * to the data and is updated by the {@link EdgeList} for a vertex.
         */
        private final Set<Integer> adjacent;
        
        /**
         * Constructs a view around the set of adjacent vertices
         */
        public AdjacentVerticesView(Set<Integer> adjacent) {
            this.adjacent = adjacent;
        }

        /**
         * Throws an {@link UnsupportedOperationException} if called.
         */
        @Override public boolean add(Integer vertex) {
            throw new UnsupportedOperationException("cannot create edges "
                + "using an adjacenct vertices set; use add() instead");
        }

        @Override public boolean contains(Object o) {
            return o instanceof Integer
                && adjacent.contains((Integer)o);
        }

        @Override public boolean isEmpty() { 
            return adjacent.isEmpty();
        }

        public Iterator<Integer> iterator() {
            return new AdjacentVerticesIterator();
        }

        @Override public boolean remove(Object o) {
            throw new UnsupportedOperationException("cannot remove edges "
                + "using an adjacenct vertices set; use remove() instead");
        }

        public int size() {
            return adjacent.size();
        }

        /**
         * A decorator around the iterator for an adjacency list's vertices that tracks
         * vertex removal to update the number of edges in the graph.
         */
        private class AdjacentVerticesIterator implements Iterator<Integer> {

            private final Iterator<Integer> vertices;

            public AdjacentVerticesIterator() {
                vertices = adjacent.iterator();
            }

            public boolean hasNext() {
                return vertices.hasNext();
            }

            public Integer next() {
                return vertices.next();
            }

            public void remove() {
                throw new UnsupportedOperationException("cannot remove an edge "
                    + "to an adjacenct vertices using this iterator; use " 
                    + "remove() instead");
            }            
        }
    }

    /**
     * A view class that exposes the {@link Edge} information in this graph as a
     * {@link Set}.
     */
    private class EdgeView extends AbstractSet<T> {
        
        public EdgeView() { }

        public boolean add(T e) {
            return AbstractGraph.this.add(e);
        }

        public boolean contains(Object o) {
            return (o instanceof Edge) && AbstractGraph.this.contains((Edge)o);
        }

        public Iterator<T> iterator() {
            return new EdgeViewIterator();
        }

        public boolean remove(Object o) {
            return (o instanceof Edge) && AbstractGraph.this.remove((Edge)o);
        }

        public int size() {
            return numEdges;
        }

        /**
         * A wrapper iterator around all the graph's {@link EdgeSet} iterators.
         * We use this class instead of a {@link
         * edu.ucla.sspace.util.CombinedIterator} in order to construct this
         * edge iterator on the fly (i.e., only one {@code Iterator} is held in
         * memory at a time), which offers some memory savings for graphs with a
         * large number of vertices or where the {@code EdgeSet} iterators have
         * a larger memory overhead.
         *
         * <p> This iterator also tracks successful iterator removals to update
         * the number of edges in this graph.
         */
        private class EdgeViewIterator implements Iterator<T> {

            /**
             * An iterator over the {@link EdgeSet} instances for each of the
             * vertices
             */
            private final Iterator<S> vertices;
            
            /**
             * The iterator from which the next element will be returned
             */
            private Iterator<T> edges;
                                                
            /**
             * The iterator from which the last element was returned.  Any call
             * to remove() will use this iterator
             */
            private Iterator<T> toRemoveFrom;

            private T next;

            private T cur;
            
            private int curRoot = -1;
            
            public EdgeViewIterator() {
                vertices = vertexToEdges.values().iterator();
                advance();
            }
            
            /**
             * Updates the state of the iterator so that {@code edges} is set to
             * an {@code Iterator} with an element to return, if such an
             * iterator exists.
             */
            private void advance() {
                next = null;
                if ((edges == null || !edges.hasNext()) && !vertices.hasNext())
                    return;
                do {
                    // Find an edge iterator with at least one edge
                    while ((edges == null || !edges.hasNext()) && vertices.hasNext()) {
                        S edgeSet = vertices.next();
                        curRoot = edgeSet.getRoot();
                        edges = edgeSet.iterator();
                    }
                    // If we didn't find one, short circuit
                    if (edges == null || !edges.hasNext())
                        return;                   
                    // Get the next edge to examine
                    T e = edges.next();

                    // The backing graph stores symmetric edges in order to
                    // maintain the adjacency lists.  To account for this,
                    // we toss out edges that will have their symmetric
                    // version counted, using the edge's to and from to make
                    // the distinction.
//                     System.out.printf("Root: %d, e.from(): %d, e.to(): %d -> %s%n",
//                                       curRoot, e.from(), e.to(), 
//                                       (curRoot == e.from() && curRoot < e.to())
//                                       || (curRoot == e.to() && curRoot < e.from()));
                    if ((curRoot == e.from() && curRoot < e.to())
                        || (curRoot == e.to() && curRoot < e.from()))
                        next = e;
                } while (next == null); 
            }
            
            public boolean hasNext() {
                return next != null;
            }

            public T next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                cur = next;
                // Once we've returned an element from the set of edges, the
                // next call to remove() should remove from the current iterator
                if (toRemoveFrom != edges)
                    toRemoveFrom = edges;
                advance();
                return cur;
            }
            
            public void remove() {
                if (cur == null)
                    throw new IllegalStateException("no element to remove");
                AbstractGraph.this.remove(cur);
                cur = null;
            }
        }
    }

    /**
     *
     *
     * All edge information is stored in the backing graph.  {@code Subgraph}
     * instances only retain information on which vertices are currently
     * represented as a part of the subgraph.  Changes to edge by other
     * subgraphs or to the backing graph are automatically detected and
     * incorporated at call time.
     */
    protected class Subgraph implements Graph<T> {
        
        private final OpenIntSet vertexSubset;

        /**
         * Creates a new subgraph from the provided set of vertices
         */
        public Subgraph(Set<Integer> vertices) {
            vertexSubset = new OpenIntSet(vertices);
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean add(int v) {
            if (vertexSubset.contains(v))
                return false;
            throw new UnsupportedOperationException(
                "Cannot add a vertext to a subgraph");
        }
    
        /**
         * {@inheritDoc}
         */
        public boolean add(T e) {
            return vertexSubset.contains(e.from())
                && vertexSubset.contains(e.to())
                && AbstractGraph.this.add(e);
        }

        /**
         * {@inheritDoc} 
         */
        public void clear() {
            for (Integer v : vertexSubset)
                AbstractGraph.this.remove(v);
            vertexSubset.clear();
        }
        
        /**
         * {@inheritDoc}
         */
        public void clearEdges() {
            for (T e : edges())
                AbstractGraph.this.remove(e);
        }
    
        /**
         * {@inheritDoc}
         */
        public boolean contains(int vertex1, int vertex2) {
            return vertexSubset.contains(vertex1)
                && vertexSubset.contains(vertex2)
                && AbstractGraph.this.contains(vertex1, vertex2);
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(Edge e) {
            return vertexSubset.contains(e.from())
                && vertexSubset.contains(e.to())
                && AbstractGraph.this.contains(e);
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(int v) {
            return vertexSubset.contains(v)
                && AbstractGraph.this.contains(v);
        }

        public Graph<T> copy(Set<Integer> vertices) {
            Graph<T> g = 
                AbstractGraph.this.copy(Collections.<Integer>emptySet());
            for (int v : vertices) {
                if (!contains(v))
                    throw new IllegalArgumentException(
                        "Requested copy with non-existant vertex: " + v);
                g.add(v);
                for (T e : getAdjacencyList(v))
                    if (vertices.contains(e.from()) 
                            && vertices.contains(e.to()))
                        g.add(e);
            }
            return g;
        }

        /**
         * {@inheritDoc} 
         */
        public int degree(int vertex) {
            int d = 0;
            for (int n : AbstractGraph.this.getNeighbors(vertex))
                if (vertexSubset.contains(n))
                    d++;
            return d;
        }

        /**
         * {@inheritDoc}
         */
        public Set<T> edges() {
            return new SubgraphEdgeView();
        }

        /**
         * {@inheritDoc}
         */
        @Override public boolean equals(Object o) {
            if (o instanceof Graph) {
                Graph<?> g = (Graph<?>)o;
                return g.order() == order()
                    && g.size() == size()
                    && g.vertices().equals(vertices())
                    && g.edges().equals(edges());                    
            }
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<T> getAdjacencyList(int vertex) {
            Set<T> adjList = AbstractGraph.this.getAdjacencyList(vertex);
            return (adjList.isEmpty())
                ? adjList
                : new SubgraphAdjacencyListView(vertex);
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<Integer> getNeighbors(int vertex) {
            if (!vertexSubset.contains(vertex))
                return Collections.<Integer>emptySet();
            return new SubgraphNeighborsView(vertex, vertexSubset);
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<T> getEdges(int vertex1, int vertex2) {
            return (vertexSubset.contains(vertex1)
                    && vertexSubset.contains(vertex2))
                ? AbstractGraph.this.getEdges(vertex1, vertex2)
                : Collections.<T>emptySet();
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean hasCycles() {
            throw new UnsupportedOperationException("fix me");
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return vertices().hashCode();
        }
        
        /**
         * {@inheritDoc}
         */
        public Iterator<Integer> iterator() {
            return Collections.<Integer>unmodifiableSet(vertexSubset).iterator();
        }
        /**
         * {@inheritDoc}
         */
        public int order() {
            return vertexSubset.size();
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean remove(Edge e) {
            return vertexSubset.contains(e.from())
                && vertexSubset.contains(e.to())
                && AbstractGraph.this.remove(e);
        }

        /**
         * {@inheritDoc}
         */
        public boolean remove(int vertex) {
            return vertexSubset.contains(vertex)
                && AbstractGraph.this.remove(vertex);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            // Because this is only a view of the backing graph, we can't keep
            // view-local state of the number of edges.  Therefore, we have to
            // calculate how many edges are present on the fly.
            int numEdges = 0;
            for (Integer v : vertexSubset) {
                EdgeSet<T> edges = AbstractGraph.this.getEdgeSet(v);
                for (Integer v2 : vertexSubset) {
                    if (v == v2)
                        break;
                    if (edges.connects(v2))
                        numEdges++;
                }
            }
            return numEdges;
        }
            
        /**
         * {@inheritDoc}
         */
        public Graph<T> subgraph(Set<Integer> vertices) {
            if (!vertexSubset.containsAll(vertices)) 
                throw new IllegalArgumentException("provided set is not a " +
                    "subset of the vertices of this graph");
            return AbstractGraph.this.subgraph(vertices);
        }
        
        public String toString() {
            return "{ vertices: " + vertices() + ", edges: " + edges() + "}";
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> vertices() {
            return Collections.<Integer>unmodifiableSet(vertexSubset);
        }    

        /**
         * A view for the {@code Edge} adjacent edges of a vertex within a
         * subgraph.  This class monitors for changes to edge set to update the
         * state of this graph
         */
        private class SubgraphAdjacencyListView  extends AbstractSet<T> {

            private final int root;

            public SubgraphAdjacencyListView(int root) {
                this.root = root;
            }

            public boolean add(T edge) {
                return (edge.from() == root 
                        || edge.to() == root) 
                    && Subgraph.this.add(edge);
            }

            public boolean contains(Object o) {
                if (!(o instanceof Edge))
                    return false;
                Edge e = (Edge)o;
                return (e.to() == root ||
                         e.from() == root)
                        && Subgraph.this.contains(e);
            }
            
            public Iterator<T> iterator() {
                return new SubgraphAdjacencyListIterator();
            }
            
            public boolean remove(Object o) {
                if (!(o instanceof Edge))
                    return false;
                Edge e = (Edge)o;
                return (e.to() == root ||
                        e.from() == root)
                    && Subgraph.this.remove(e);
            }

            public int size() {                
                int sz = 0;
                for (int v : vertexSubset) {
                    if (v == root)
                        continue;
                    Set<T> edges = AbstractGraph.this.getEdges(v, root);
                    sz += edges.size();
                }
                return sz;
            }

            /**
             * A decorator around the iterator for the adjacency list for a
             * vertex in a subgraph, which tracks edges removal to update the
             * number of edges in the graph.
             */
            private class SubgraphAdjacencyListIterator 
                    implements Iterator<T> {

                private final Iterator<T> edges;

                public SubgraphAdjacencyListIterator() {
                    List<Iterator<T>> iters = new LinkedList<Iterator<T>>();

                    for (int v : vertexSubset) {
                        if (v == root)
                            continue;
                        Set<T> edges = AbstractGraph.this.getEdges(v, root);
                        if (!edges.isEmpty())
                            iters.add(edges.iterator());
                    }
                    edges = new CombinedIterator<T>(iters);
                }

                public boolean hasNext() {
                    return edges.hasNext();
                }

                public T next() {
                    return edges.next();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }
        }

        /**
         * A view-based class over the edges in a {@link Subgraph}.  This class
         * masks the state of the edges that are in the backing graph but not in
         * the subgraph.
         */
        private class SubgraphEdgeView extends AbstractSet<T> {           

            public SubgraphEdgeView() { }

            public boolean add(T e) {
                return Subgraph.this.add(e);
            }

            public boolean contains(T o) {
                return Subgraph.this.contains(o);
            }

            public Iterator<T> iterator() {
                return new SubgraphEdgeIterator();
            }

            public boolean remove(Object o) {
                return (o instanceof Edge)
                    && Subgraph.this.remove((Edge)o);
            }

            public int size() {
                return Subgraph.this.size();
            }

            /**
             *
             */
            private class SubgraphEdgeIterator implements Iterator<T> {

                private final Queue<T> edgesToReturn;

                private final Queue<Integer> remainingVertices;

                private Iterator<Integer> possibleNeighbors;

                private Integer curVertex;

                public SubgraphEdgeIterator() {
                    remainingVertices = new ArrayDeque<Integer>(vertexSubset);
                    edgesToReturn = new ArrayDeque<T>();
                    curVertex = null;
                    advance();
                }
                
                private void advance() {
                    // If there are still edges from the current pair, skip
                    // processing any further connections
                    if (!edgesToReturn.isEmpty())
                        return;
                    
                    do {
                        // Find the next vertex to analyze for connections
                        while ((possibleNeighbors == null
                                    || !possibleNeighbors.hasNext())
                               && !remainingVertices.isEmpty()) {
                            // Pull off the next vertex and then use the remaining
                            // vertices to check for connections
                            curVertex = remainingVertices.poll();
                            possibleNeighbors = remainingVertices.iterator();
                        }
                        
                        // Iterate through the possible neighbors of this
                        // vertex, stopping when we find another vertex that has
                        // at least one edge
                        while (edgesToReturn.isEmpty()
                                   && possibleNeighbors.hasNext()) {
                            Integer v = possibleNeighbors.next();
                            edgesToReturn.addAll(getEdges(curVertex, v));
                        }
                    } while (edgesToReturn.isEmpty()
                             && !remainingVertices.isEmpty());
                }

                public boolean hasNext() {
                    return edgesToReturn.size() > 0;
                }

                public T next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    T n = edgesToReturn.poll();
                    advance();
                    return n;
                }

                public void remove() {
                    throw new IllegalStateException();
                }
            }
        }



        /**
         * A view of a subgraph's vertex's neighbors that are also in the
         * subview.  This view monitors for additions and removals to the set in
         * order to update the state of this {@code Subgraph}.
         */
        private class SubgraphNeighborsView extends AbstractSet<Integer> {

            /**
             * The set of adjacent vertices to a vertex.  This set is itself a view
             * to the data and is updated by the {@link EdgeList} for a vertex.
             */
            private Set<Integer> allPossibleNeighbors;

            private int root;
            
            /**
             * Constructs a view around the set of adjacent vertices
             */
            public SubgraphNeighborsView(int root, Set<Integer> adjacent) {
                this.root = root;
                this.allPossibleNeighbors = adjacent;
            }
            
            /**
             * Adds an edge to this vertex and adds the vertex to the graph if it
             * was not present before.
             */
            public boolean add(Integer vertex) {
                throw new UnsupportedOperationException(
                    "Cannot add vertices to subgraph");
            }
            
            public boolean contains(Object o) {
                if (!(o instanceof Integer))
                    return false;
                Integer i = (Integer)o;
                return allPossibleNeighbors.contains(i) && checkVertex(i);
            }

            /**
             * Returns {@code true} if the vertex is connected to the root
             */
            private boolean checkVertex(Integer i) {
                return AbstractGraph.this.contains(i, root);
            }

            public Iterator<Integer> iterator() {
                return new SubgraphNeighborsIterator();
            }
            
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }
            
            public int size() {
                int sz = 0;
                for (Integer v : allPossibleNeighbors) {
                    if (checkVertex(v))
                        sz++;
                }
                return sz;
            }

            /**
             * A decorator around the iterator for a subgraphs's neighboring
             * vertices set, which keeps track of which neighboring vertices are
             * actually in this subview.
             */
            private class SubgraphNeighborsIterator implements Iterator<Integer> {

                private final Iterator<Integer> iter;

                private Integer next;

                public SubgraphNeighborsIterator() {
                    iter = allPossibleNeighbors.iterator();
                    advance();
                }
                
                /**
                 * Finds the next adjacent vertex that is also in this subview.
                 */
                private void advance() {
                    next = null;
                    while (iter.hasNext() && next == null) {
                        Integer v = iter.next();
                        if (checkVertex(v))
                            next = v;
                    }                    
                }

                public boolean hasNext() {
                    return next != null;
                }

                public Integer next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    Integer cur = next;
                    advance();
                    return cur;
                }

                /**
                 * Throws an {@link UnsupportedOperationException} if called.
                 */
                public void remove() {
                    throw new UnsupportedOperationException();
                }            
            }
        }

//         /**
//          * 
//          */
//         private class SubgraphEdgeView extends AbstractSet<T> {           

//             public SubgraphEdgeView() { }

//             public boolean add(T e) {
//                 return Subgraph.this.add(e);
//             }

//             public boolean contains(T o) {
//                 return Subgraph.this.contains(o);
//             }

//             public Iterator<T> iterator() {
//                 return new SubgraphEdgeIterator();
//             }

//             public boolean remove(Object o) {
//                 return (o instanceof Edge)
//                     && Subgraph.this.remove((Edge)o);
//             }

//             public int size() {
//                 return Subgraph.this.size();
//             }

//             /**
//              * An {@code Iterator} that combines all the iterators returned by
//              * {@link #getAdjacencyList(int)} for the vertices in this subgraph
//              * and filters the results to remove symmetric edges found in two
//              * lists.
//              */
//             private class SubgraphEdgeIterator implements Iterator<T> {

//                 private final Iterator<SubgraphAdjacencyListView> adjacencyLists;

//                 private Iterator<T> curIter;

//                 private int curRoot;

//                 private T next;
                
//                 private T cur;

//                 /**
//                  * Creates an iterator that combines the adjacency lists
//                  * iterators for all the vertices in this subgraph.
//                  */
//                 public SubgraphEdgeIterator() {
//                     Subgraph.this.checkForUpdates();

//                     // Create a list for all the wrapped adjacency lists of the
//                     // vertices in this subgraph
//                     List<SubgraphAdjacencyListView> lists = 
//                         new ArrayList<SubgraphAdjacencyListView>(
//                             vertexMapping.size());
                    
//                     // Loop over all vertices in the subggraph and wrap their
//                     // adjacency lists
//                     for (Map.Entry<Integer,Integer> e 
//                              : vertexMapping.entrySet()) {

//                         // Get the list using the backing vertex
//                         S adjList = getEdgeSet(e.getValue());
//                         assert adjList != null : "subgraph modified prior to " +
//                             "iteration";

//                         // Wrap it to only return the edges present in this
//                         // subgraph
//                         SubgraphAdjacencyListView subAdjList = 
//                             new SubgraphAdjacencyListView(e.getKey(), adjList);

//                         // Add the list to the total list of lists
//                         lists.add(subAdjList);
//                     }
//                     adjacencyLists = lists.iterator();
//                     advance();
//                 }

//                 private void advance() {
//                     next = null;
//                     // If there are no more elements in the current adjacency
//                     // list and there are no futher adjacency lists to use, the
//                     // iterator is finished
//                     if ((curIter == null || !curIter.hasNext()) 
//                             && !adjacencyLists.hasNext())
//                         return;

//                     do {
//                         // Find an edge iterator with at least one edge
//                         while ((curIter == null || !curIter.hasNext()) 
//                                    && adjacencyLists.hasNext()) {
                            
//                             // Get the next adjacency list
//                             SubgraphAdjacencyListView adjList = 
//                                 adjacencyLists.next();
//                             // Record what the root vertex is for it
//                             curRoot = adjList.rootInSubgraph;
//                             // Set the current iterator to return this list's
//                             // edges
//                             curIter = adjList.iterator();
//                         }

//                         // If we didn't find one, short circuit
//                         if (curIter == null || !curIter.hasNext())
//                             return;                   

//                         // Get the next edge to examine
//                         T e = curIter.next();

//                         // The backing graph stores symmetric edges in order to
//                         // maintain the adjacency lists.  To account for this,
//                         // we toss out edges that will have their symmetric
//                         // version counted, using the edge's to and from to make
//                         // the distinction.
//                         if ((curRoot == e.from() && curRoot < e.to())
//                                 || (curRoot == e.to() && curRoot < e.from()))
//                             next = e;
//                     } while (next == null); 
//                 }

//                 public boolean hasNext() {
//                     return next != null;
//                 }

//                 public T next() {
//                     if (!hasNext())
//                         throw new NoSuchElementException();
//                     cur = next;
//                     advance();
//                     return cur;                    
//                 }

//                 public void remove() {
//                     if (cur == null) 
//                         throw new NoSuchElementException("No edge to remove");
//                     Subgraph.this.remove(cur);
//                     cur = null;
//                 }
//             }
//         }

//         /**
//          * A view of a subgraph's vertex's adjacencent vertices (also in the
//          * subgraph), which monitors for additions and removals to the set in
//          * order to update the state of this {@code Subgraph}.
//          */
//         private class SubgraphAdjacentVerticesView extends AbstractSet<Integer> {

//             /**
//              * The set of adjacent vertices to a vertex.  This set is itself a view
//              * to the data and is updated by the {@link EdgeList} for a vertex.
//              */
//             private Set<Integer> adjacent;
            
//             /**
//              * Constructs a view around the set of adjacent vertices
//              */
//             public SubgraphAdjacentVerticesView(Set<Integer> adjacent) {
//                 this.adjacent = adjacent;
//             }
            
//             /**
//              * Adds an edge to this vertex and adds the vertex to the graph if it
//              * was not present before.
//              */
//             public boolean add(Integer vertex) {
//                 throw new UnsupportedOperationException("cannot add edges "
//                     + "using an adjacenct vertices set; use add() "
//                     + "instead");
//             }
            
//             public boolean contains(Object o) {
//                 if (!(o instanceof Integer))
//                     return false;
//                 // Convert the vertex to its index in the backing graph
//                 Integer v = (Integer)o;
//                 Integer inBacking = vertexMapping.get(v);                
//                 return inBacking != null && adjacent.contains(inBacking);
//             }
            
//             public Iterator<Integer> iterator() {
//                 return new AdjacentVerticesIterator();
//             }
            
//             public boolean remove(Object o) {
//                 throw new UnsupportedOperationException("cannot remove edges "
//                     + "using an adjacenct vertices set; use remove() "
//                     + "instead");
//             }
            
//             public int size() {
//                 int sz = 0;
//                 for (Integer inBacking : adjacent) {
//                     if (isInSubgraph(inBacking))
//                         sz++;
//                 }
//                 return sz;
//             }

//             /**
//              * A decorator around the iterator for an adjacency list's vertices that tracks
//              * vertex removal to update the number of edges in the graph.
//              */
//             private class AdjacentVerticesIterator implements Iterator<Integer> {

//                 private final Iterator<Integer> vertices;

//                 private Integer next;

//                 public AdjacentVerticesIterator() {
//                     vertices = adjacent.iterator();
//                     advance();
//                 }
                
//                 /**
//                  * Finds the next adjacent vertex that is also in this subgraph.
//                  */
//                 private void advance() {
//                     next = null;
//                     while (vertices.hasNext()) {
//                         Integer v = vertices.next();
//                         Integer inSub = vertexMapping.inverse().get(v);
//                         if (inSub != null) {
//                             next = inSub;
//                             break;
//                         }
//                     }                    
//                 }

//                 public boolean hasNext() {
//                     return next != null;
//                 }

//                 public Integer next() {
//                     if (!hasNext())
//                         throw new NoSuchElementException();
//                     Integer cur = next;
//                     advance();
//                     return cur;
//                 }

//                 public void remove() {
//                     throw new UnsupportedOperationException("cannot remove an "
//                         + "edge to an adjacenct vertices using this iterator; "
//                         + "use remove() instead");
//                 }            
//             }
//         }        
//     }                
    }
}
