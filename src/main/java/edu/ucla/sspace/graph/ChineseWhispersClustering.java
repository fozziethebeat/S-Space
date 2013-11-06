/*
 * Copyright 2012 David Jurgens 
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

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Merge;
import edu.ucla.sspace.clustering.SoftAssignment;

import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.HashIndexer;
import edu.ucla.sspace.util.Indexer;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.ObjectCounter;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.WorkQueue;

import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.IntIntMultiMap;
import edu.ucla.sspace.util.primitive.IntIntHashMultiMap;
import edu.ucla.sspace.util.primitive.PrimitiveCollections;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import static edu.ucla.sspace.util.LoggerUtil.verbose;
import static edu.ucla.sspace.util.LoggerUtil.veryVerbose;

/**
 * @author David Jurgens 
 */
public class ChineseWhispersClustering implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(ChineseWhispersClustering.class.getName());
       
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.graph.LinkClustering";

    private static final int DEFAULT_MAX_ITERATIONS = 100;

    private static final double DEFAULT_RANDOM_ASSIGNMENT_PROB = 0d;

    private static final Random RANDOM = new Random();

    /**
     *
     * @return a mapping from the cluster index to the set of graph vertices
     *         mapped to that cluster
     */
    public <E extends Edge> MultiMap<Integer,Integer> cluster(Graph<E> graph) {
        return cluster(graph, DEFAULT_MAX_ITERATIONS, 
                       DEFAULT_RANDOM_ASSIGNMENT_PROB);
    }

    /**
     *
     * @return a mapping from the cluster index to the set of graph vertices
     *         mapped to that cluster
     */
    public <E extends Edge> MultiMap<Integer,Integer> 
                      cluster(Graph<E> graph, int maxIterations) {
        return cluster(graph, maxIterations, DEFAULT_RANDOM_ASSIGNMENT_PROB);
    }

    /**
     *
     *
     * @return a mapping from the cluster index to the set of graph vertices
     *         mapped to that cluster
     */
    public <E extends Edge> MultiMap<Integer,Integer> 
                      cluster(Graph<E> graph, int maxIterations, 
                              double randomAssignmentProb) {
        if (!areVerticesContiguous(graph))
            throw new IllegalArgumentException(
                "Graph vertex indices must be contiguous");

        int[] vertexAssignments = new int[graph.order()];
        int[] vertices = new int[graph.order()];
        for (int i = 0; i < vertices.length; ++i) {
            vertices[i] = i;
            vertexAssignments[i] = i;
        }

        boolean assignmentsChanged = true;
        double mutationRate = randomAssignmentProb;
        for (int iter = 0; iter < maxIterations && assignmentsChanged; ++iter) {
            assignmentsChanged = false;
            
            // Shuffle the order in which the vertices will be accessed
            PrimitiveCollections.shuffle(vertices);

            for (int i = 0; i < vertices.length; ++i) {
                int vertex = vertices[i];

                // Allow for random mutations
                if (RANDOM.nextDouble() < mutationRate) {
                    int randomClass = RANDOM.nextInt(vertices.length);
                    int oldClass = vertexAssignments[vertex];
                    if (oldClass != randomClass) {
                        vertexAssignments[vertex] = randomClass;
                        assignmentsChanged = true;
                    }                    
                }
                // Otherwise use the regular update procedure
                else {
                    // Get the neighbors of the current vertex and identify
                    // which class label is the maximum from the neighbors
                    int maxClass = (graph instanceof WeightedGraph) 
                        ? getMaxClassWeighted(vertex, vertexAssignments,
                            (WeightedGraph<? extends WeightedEdge>)graph)
                        : getMaxClass(vertex, vertexAssignments, graph);
                    int oldClass = vertexAssignments[vertex];
                    if (oldClass != maxClass) {
                        vertexAssignments[vertex] = maxClass;
                        assignmentsChanged = true;
                    }
                }                
                
            }
        }

        MultiMap<Integer,Integer> toReturn = 
            new HashMultiMap<Integer,Integer>();
        for (int i = 0; i < vertices.length; ++i) 
            toReturn.put(vertexAssignments[i], i);
        return toReturn;
    }

    static <E extends Edge> boolean areVerticesContiguous(Graph<E> g) {
        return true;
    }

    static int getMaxClass(int v, int[] vertexAssignments, Graph g) {
        IntSet neighbors = g.getNeighbors(v);
        IntIterator iter = neighbors.iterator();
        Counter<Integer> classes = new ObjectCounter<Integer>();
        classes.count(vertexAssignments[v]);
        while (iter.hasNext()) {
            int n = iter.nextInt();
            classes.count(vertexAssignments[n]);
        }

        TIntSet ties = new TIntHashSet();
        int max = 0;
        for (Map.Entry<Integer,Integer> e : classes) {
            int clazz = e.getKey();
            int count = e.getValue();
            if (count > max) {
                ties.clear();
                max = count;
            }
            if (count == max)
                ties.add(clazz);
        }

        int[] options = ties.toArray(new int[ties.size()]);
        return (options.length == 1)
            ? options[0]
            : options[RANDOM.nextInt(options.length)];
    }

    static <E extends WeightedEdge> int 
                      getMaxClassWeighted(int v, int[] vertexAssignments, 
                                          WeightedGraph<E> g) {
        Set<E> edges = g.getAdjacencyList(v);
        TIntDoubleMap classSums = new TIntDoubleHashMap();
        for (WeightedEdge e : edges) {
            int n = (e.to() == v) ? e.from() : e.to();
            int nClass = vertexAssignments[n];
            double weight = e.weight();
            if (classSums.containsKey(nClass)) {
                double curWeight = classSums.get(nClass);
                classSums.put(nClass, weight + curWeight);
            }
            else {
                classSums.put(nClass, weight);
            }
        }

        double maxSum = -1d;
        TIntSet ties = new TIntHashSet();
        TIntDoubleIterator iter = classSums.iterator();
        while (iter.hasNext()) {
            iter.advance();
            double weight = iter.value();
            if (weight > maxSum) {
                maxSum = weight;
                ties.clear();
            }
            if (weight == maxSum)
                ties.add(iter.key());
            
        }
        
        // If there wasn't a tie after all
        int[] options = ties.toArray();
        return (options.length == 1)
            ? options[0]
            : options[RANDOM.nextInt(options.length)];
    }
}
