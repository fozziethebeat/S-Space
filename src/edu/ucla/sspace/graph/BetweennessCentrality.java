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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

/**
 * An implementation of Brandes (2001) "A Faster Algorithm for Betweenness
 * Centrality" (available <a
 * href="http://www.cs.ucc.ie/~rb4/resources/Brandes.pdf">here</a>) for
 * computing the <a
 * href="http://en.wikipedia.org/wiki/Betweenness_Centrality">betweenness
 * centrality</a> for all the vertices in a graph.
 */
public class BetweennessCentrality implements java.io.Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of a {@code BetweennessCentrality}
     */
    public BetweennessCentrality() { }

    /**
     * Returns a mapping from each vertex to its betweenness centrality measure.
     */
    public <E extends Edge> double[] compute(Graph<E> g) {
        // Perform a quick test for whether the vertices of g are a contiguous
        // sequence starting at 0, which makes the vertex mapping trivial
        if (!hasContiguousVertices(g))
            throw new Error("fix this case");
        
        double[] centralities = new double[g.order()];
        for (int s : g.vertices()) {
            Deque<Integer> S = new ArrayDeque<Integer>();

            // Initialize P to an empty list for each vertex
            List<List<Integer>> P = new ArrayList<List<Integer>>(g.order());
            for (int i = 0; i < g.order(); ++i)
                P.add(new ArrayList<Integer>());

            double[] sigma = new double[g.order()];
            sigma[s] = 1;

            double[] d = new double[g.order()];
            Arrays.fill(d, -1);
            d[s] = 0;
            
            Queue<Integer> Q = new ArrayDeque<Integer>();
            Q.add(s);
            while (!Q.isEmpty()) {
                int v = Q.poll();
                S.offer(v);
                for (int w : g.getNeighbors(v)) {
                    // Check whether this is the first time we've seen vertex w
                    if (d[w] < 0) {
                        Q.offer(w);
                        d[w] = d[v] + 1;
                    }
                    // Check whether the shortest path to w is through v
                    if (d[w] == d[v] + 1) {
                        sigma[w] += sigma[v];
                        P.get(w).add(v);
                    }
                }
            }
            double[] delta = new double[g.order()];
            // S as a stack returns vertices in order of their non-increasing
            // distance from vertex s
            while (!S.isEmpty()) {
                int w = S.pollLast(); // get the top of the stack
                for (int v : P.get(w)) {
                    delta[v] += (sigma[v] / sigma[w]) * (1 + delta[w]);
                }
                if (w != s) {
                    centralities[w] += delta[w];
                }            
            }
        }
        return centralities;
    }

    private static boolean hasContiguousVertices(Graph<?> g) {
        int order = g.order();
        for (int v : g.vertices()) {
            if (v >= order)
                return false;
        }
        return true;
    }
}