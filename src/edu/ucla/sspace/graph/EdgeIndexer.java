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

import edu.ucla.sspace.util.Indexer;
import edu.ucla.sspace.util.ObjectIndexer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;


/**
 * A special-purpose {@link Indexer} implementation for representing a compact,
 * space-efficient index of edges.
 */
class EdgeIndexer implements Indexer<Edge> {

    private final int[] vertexToEdgeSum;
    
    private final Graph<? extends Edge> g;
    
    public EdgeIndexer(Graph<? extends Edge> g) {
        this.g = g;
        
        vertexToEdgeSum = new int[g.order()];
        int[] edgeCounts = new int[g.order()];
        // Sum how many edges are connect to each vertex, only counting edges
        // once (by picking the smaller of the two vertices in the edge)
        for (Edge e : g.edges()) {
            int v = (e.from() < e.to()) ? e.from() : e.to();
            vertexToEdgeSum[v]++;
        }
        // Then sum how many edges were connected to all vertices with a
        // smaller index
        for (int i = 1; i < vertexToEdgeSum.length; ++i) {
            //vertexToEdgeSum[i] += vertexToEdgeSum[i-1] + edgeCounts[i-1];
            vertexToEdgeSum[i] += vertexToEdgeSum[i-1]; // + edgeCounts[i-1];
        }
        // REMINDER: assumes graph has a contiguous ordering starting at
        // 0...
    }
    
    public int index(Edge e) {
        int index = find(e);
        if (index == -1)
            throw new IllegalStateException(
                "Cannot index new edge: " + e);
        return index;
    }    

    public void clear() {
        throw new Error();
    }

    public boolean contains(Edge e) {
        return g.contains(e);
    }
    
    public int find(Edge e) {
        int v = (e.from() < e.to()) ? e.from() : e.to();
        // The starting index is based on the number of edges seen prior to
        // this vertex
        int index = (v == 0) ? 0 : vertexToEdgeSum[v-1];

        // Assuming a consistent ordering of the neighboring vertices, iterate
        // until we find the edge in the neighbors list and then return the
        // index.  We increment the index for each edge seen.
        for (Edge neighbor : g.getAdjacencyList(v)) {
            // We only count edges once, so skip edges that aren't valid for
            // indexing purposes
            if (neighbor.from() >= neighbor.to())
                continue;
            else if (e.equals(neighbor))
                return index;
            index++;
        }
        return -1;
    }

    public int getStartingIndex(int vertex) {
        if (vertex < 0 || vertex >= g.order())
            throw new IllegalArgumentException("Invalid vertex: " + vertex);
        return (vertex == 0) ? 0 : vertexToEdgeSum[vertex-1];
    }

    public int highestIndex() {
        return g.size() - 1;
    }

    public Iterator<Map.Entry<Edge,Integer>> iterator() {
        throw new Error();
    }

    public Edge lookup(int edgeIndex) {
        int v = Arrays.binarySearch(vertexToEdgeSum, edgeIndex);
        // v is positive if the edgeIndex is the first edge for the next vertex
        // (so increment it to point to that vertex), otherwise the should
        // points to the next vertex's spot in the array
        if (v >= 0) {
            v++;

            // This index might not be valid if there are vertices with a
            // greater index but who have no edges since they would be tied in
            // the array.  Therefore, search forwards for a different vertex
            // while there are still ties
            for (; v <vertexToEdgeSum.length 
                     && vertexToEdgeSum[v]==vertexToEdgeSum[v-1]; v++)
                ;            
        }
        // The complement of the value is where the vertex is
        else 
            v = -(v + 1);
        
        int curIndex = (v == 0) ? 0 : vertexToEdgeSum[v-1];
        for (Edge neighbor : g.getAdjacencyList(v)) {
            // We only count edges once, so skip edges that aren't valid for
            // indexing purposes
            if (neighbor.from() >= neighbor.to())
                continue;
            if (curIndex == edgeIndex)
                return neighbor;
            curIndex++;
        }
        throw new IllegalStateException(
            "Requested an edge for an invalid index: " + edgeIndex);
    }
    
    public Map<Integer,Edge> mapping() {
        throw new Error();
    }

    public int size() {
        return g.size();
    }
}