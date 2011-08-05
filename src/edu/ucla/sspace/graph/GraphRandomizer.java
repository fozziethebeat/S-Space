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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class GraphRandomizer {

    public static <T extends Edge> void shufflePreserveDegreeInMemory(Graph<T> g) {
        
        List<T> edges = new ArrayList<T>(g.edges());

        // Decide on number of iterations
        int swapIterations = 3 * g.size();
        for (int s = 0; s < swapIterations; ++s) {

            // Pick two vertices from which the edges will be selected
            int i = (int)(Math.random() * edges.size());
            int j = i; 
            // Pick another vertex that is not v1
            while (i == j)
                j = (int)(Math.random() * edges.size());

            T e1 = edges.get(i);
            T e2 = edges.get(j);
            
            // Swap their end points
            T swapped1 = e1.<T>clone(e1.from(), e2.to());
            T swapped2 = e2.<T>clone(e2.from(), e1.to());
            
            // Check that the new edges do not already exist in the graph
            if (g.contains(swapped1) 
                || g.contains(swapped2))
                continue;
            
            // Remove the old edges
            g.remove(e1);
            g.remove(e2);
            
            // Put in the swapped-end-point edges
            g.add(swapped1);
            g.add(swapped2);

            // Update the in-memory edges set so that if these edges are drawn
            // again, they don't point to old edges
            edges.set(i, swapped1);
            edges.set(j, swapped2);
        }
    }

    public static <T extends Edge> void shufflePreserveDegree(Graph<T> g) {

        // Copy the vertices to a list to support random access
        List<Integer> vertices = new ArrayList<Integer>(g.vertices());       

        // Decide on number of iterations
        int swapIterations = 3 * g.size();
        for (int i = 0; i < swapIterations; ++i) {
        
            // Pick two vertices from which the edges will be selected
            int v1 = vertices.get((int)(Math.random() * vertices.size()));
            int v2 = v1; 
            // Pick another vertex that is not v1
            while (v1 == v2)
                v2 = vertices.get((int)(Math.random() * vertices.size()));
            
            // From the two vertices, select an edge from each of their adjacency
            // lists.
            T e1 = getRandomEdge(g.getAdjacencyList(v1));
            T e2 = getRandomEdge(g.getAdjacencyList(v2));
            
            // Swap their end points
            T swapped1 = e1.<T>clone(e1.from(), e2.to());
            T swapped2 = e2.<T>clone(e2.from(), e1.to());
            
            // Check that the new edges do not already exist in the graph
            if (g.contains(swapped1) 
                || g.contains(swapped2))
                continue;
            
            // Remove the old edges
            g.remove(e1);
            g.remove(e2);
            
            // Put in the swapped-end-point edges
            g.add(swapped1);
            g.add(swapped2);
        }
    }

    private static <T> T getRandomEdge(Set<T> edges) {
        int edgeToSelect = (int)(edges.size() * Math.random());
        Iterator<T> it = edges.iterator();
        int i = 0;
        while (it.hasNext()) {
            T edge = it.next();
            if (i == edgeToSelect)
                return edge;
            i++;
        }
        throw new AssertionError("Random edge selection logic is incorrect");
    }
}