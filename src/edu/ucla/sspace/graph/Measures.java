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

import java.util.HashSet;
import java.util.Set;

import edu.ucla.sspace.matrix.Matrix;


public class Measures {
    
    private Measures() { }

    public static double avgClusteringCoefficient(Graph<? extends Edge> g) {
        double ccSum = 0;
        int counter = 0;
        for (int v : g.vertices()) {
            Set<Integer> neighbors = g.getNeighbors(v);
            int numTriangles = 0;
            for (int i : neighbors) {
                for (int j : neighbors) {
                    if (i == j)
                        break;
                    if (g.contains(i, j))
                        numTriangles++;
                }
            }
            int numNeighbors = neighbors.size();
            double localClusteringCoefficient = (numNeighbors <= 1)
                ? 0
                : (2d * numTriangles) / (numNeighbors * (numNeighbors - 1));
            ccSum += localClusteringCoefficient;
//             if (++counter % 100 == 0) {
//                 System.out.printf("Avg after %d: %f%n", counter, (ccSum / counter));
//             }
        }
        return ccSum / g.order();
    }

    public static double meanShorestPathDistance(Graph<? extends Edge> g) {
//         Matrix shortestPaths = new FloydsAlgorithm().computeAllPairsDistance(g);
//         int rows = shortestPaths.rows();
        long pathLengthSum = 0;
//         for (int i = 0; i < rows++; ++i) {
//             for (int j = 0; j < rows; ++j) {
//                 if (i == j)
//                     continue;
//                 pathLengthSum += shortestPaths.get(i, j);
//             }
//         }
//         return pathLengthSum / ((rows * rows) - rows);
        
        Set<Integer> vertices = g.vertices();
        int s = 0;
        for (int i : vertices) {
            //System.out.println(i);
            for (int j : vertices) {
                if (i == j)
                    break;
                pathLengthSum += shortestPathLength(g, i, j);
                s++;
            }
        }
        double size = vertices.size();
        return pathLengthSum / (double)s;
    }

    public static int shortestPathLength(Graph<? extends Edge> g, int from, int to) {
        Set<Integer> frontier = new HashSet<Integer>();
        Set<Integer> seen = new HashSet<Integer>();
        if (from == to)
            return 0;
        // NOTE: add step here for directed graph
        frontier.addAll(g.getNeighbors(from));
        int depth = 1;
        boolean found = false;
        found:
        while (!frontier.isEmpty()) {
            Set<Integer> next = new HashSet<Integer>();
            for (int v : frontier) {                
                if (v == to) {
                    found = true;
                    break found;
                }
                seen.add(v);
                for (int k : g.getNeighbors(v)) {
                    if (k == to) {
                        found = true;
                        depth++;
                        break found;
                    }
                    else if (!seen.contains(k))
                        next.add(k);
                }
            }
            frontier.clear();
            frontier.addAll(next);
            depth++;
        }
        return (found) ? depth : Integer.MAX_VALUE;
    }
}