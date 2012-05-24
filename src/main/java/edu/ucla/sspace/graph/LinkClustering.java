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

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Merge;
import edu.ucla.sspace.clustering.SoftAssignment;

import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.HashIndexer;
import edu.ucla.sspace.util.Indexer;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.WorkQueue;

import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.IntIntMultiMap;
import edu.ucla.sspace.util.primitive.IntIntHashMultiMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.verbose;
import static edu.ucla.sspace.util.LoggerUtil.veryVerbose;


/**
 * An implmentation of the link clustering described in Ahn, Bagrow, and Lehman
 * (2010).  This algorithm is a multi-class clustering algorithm that instead of
 * clustering the nodes in a graph according to their similarity with eacher,
 * clusters the <i>links</i> connecting the nodes to reveal communities that
 * connect the nodes.  For full information on the algorithm see, <ul>
 *
 *   <li> Yong-Yeol Ahn, James P. Bagrow and Sune Lehmann.  Link communities
 *   reveal multiscale complexity in networks.  Nature 466, 761–764 (05 August
 *   2010).  Available online <a
 *   href="http://www.nature.com/nature/journal/v466/n7307/full/nature09182.html">here</a>.
 * 
 * </ul>
 *
 * This algorithm automatically determines the number of clusters based on a
 * partition density function.  Accordingly, the clustering methods take no
 * parameters.  Calling the {@code cluster} method with a fixed number of
 * elements will still cluster the rows, but will ignore the requester number of
 * clusters.
 *
 * <p> Note that this class is <i>not</i> thread-safe.  Each call to clustering
 * will cache local information about the clustering result to facilitate the
 * {@link #getSolution(int)} and {@link #getSolutionDensity(int)} functions.
 *
 * This class provides one configurable property:
 *
 * <dl style="margin-left: 1em">
 * <dt> <i>Property:</i> <code><b>{@value #KEEP_SIMILARITY_MATRIX_IN_MEMORY_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code true}
 *
 * <dd style="padding-top: .5em"> If {@code true}, this property specifies the
 *      edge similarity matrix used by {@link
 *      HierarchicalAgglomerativeClustering} should be computed once and then
 *      kept in memory, which is the default behavior.  If {@code false}, this
 *      causes the similarity of two edges to be recomputed on-the-fly whenever
 *      it is requester.  By computing these values on-the-fly, the performance
 *      will be slowed down, depending on the complexity of the edge similarity
 *      function.  However, this on-the-fly setting allows for clustering large
 *      graphs whose edge similarity matrix would not regularly fit into memory.
 *      It is advised that users not tune this parameter unless it is known that
 *      the similarity matrix will not fit in memory. </p>
 *
 * </dl>
 *
 * @author David Jurgens 
 */
public class LinkClustering implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(LinkClustering.class.getName());

    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.graph.LinkClustering";
    
    public static final String MINIMUM_EDGE_SIMILARITY_PROPERTY
        = PROPERTY_PREFIX + ".minEdgeSimilarity";

    /**
     * The work used by all {@code LinkClustering} instances to perform
     * multi-threaded operations.
     */
    private static final WorkQueue WORK_QUEUE = WorkQueue.getWorkQueue();    

    /**
     * Instantiates a new {@code LinkClustering} instance.
     */
    public LinkClustering() {  }

    /**
     * Computes the similarity of the graph's edges and merges them until the
     * specified number of clusters has been reached.
     *
     * @param numClusters the number of clusters to return
     */
    public <E extends Edge> MultiMap<Integer,Integer> cluster(
                      final Graph<E> graph, int numClusters, Properties props) {
        return singleLink(graph, numClusters);
        // throw new Error();
    }

    /**
     * Computes the similarity of the graph's edges and merges them to select
     * the final partitioning that maximizes the overall cluster density.
     */
    public <E extends Edge> MultiMap<Integer,Integer> cluster(
                      final Graph<E> graph, Properties props) {
        
//         Indexer<E> edges = new ObjectIndexer<E>();
//         for (E e : graph.edges())
//             edges.index(e);
//         for (Map.Entry<E,Integer> e : edges)
//             System.out.println(e.getValue() + " " + e.getKey());
//         System.out.print(",");
//         for (int i = 0; i < graph.size(); ++i) {
//             E e = edges.lookup(i);
//             System.out.print(e.from() + "--" + e.to() + ",");
//         }
//         System.out.println();
//         for (int i = 0; i < graph.size(); ++i) {
//             E e = edges.lookup(i);
//             System.out.print(e.from() + "--" + e.to() + ",");
//             for (int j = 0; j < graph.size(); ++j) {
//                 if (i == j)
//                     System.out.print("0");
//                 else
//                     System.out.print(getConnectionSimilarity(graph, edges.lookup(i), edges.lookup(j)));
//                 if (j + 1 < graph.size())
//                     System.out.print(",");
//             }
//             System.out.println();
//         }


        return singleLink(graph);
        /*
        double minSimilarity = 0d;
        String minSimProp = 
            props.getProperty(LinkClustering.MINIMUM_EDGE_SIMILARITY_PROPERTY);
        if (minSimProp != null) {
            minSimilarity = Double.parseDouble(minSimProp);
            verbose(LOGGER, "thresholding edge pairs with similarity below "
                    + minSimilarity);
        }        

        LOGGER.info("Computing edge similarities");
        PriorityQueue<EdgePair> pq = calcuateEdgeSimQueue(graph, minSimilarity);
        
        LOGGER.info("Clustering edges");
        return singleLink(pq, graph);
        */
    }

    /**
     * Performs agglomerative clustering (other than single-linkage) using a
     * priority queue (a min-heap) to decide which edges should be clustered
     * next
     */
    /*
    private MultiMap<Integer,Integer> aggloCluster(PriorityQueue<EdgePair> pq, 
                                                 Graph<? extends Edge> g) {
        int numEdges = g.size();
        // Index the edges so that we can quickly look up which cluster an edge
        // is in
        Indexer<Edge> edgeIndexer = new HashIndexer<Edge>();

        // Keep a simple int->int mapping from each edge's index to the cluster
        // its in.  Each edge is assigned to its own cluster at first
        int[] edgeToCluster = new int[numEdges];

        // Keep track of the vertices in each cluster
        IntIntMultiMap clusterToVertices = new IntIntHashMultiMap();
        IntIntMultiMap densestSolution = new IntIntHashMultiMap();

        // Loop over each edge in the graph and add the vertices for that edge
        // to the initial cluster assignment
        for (Edge e : g.edges()) {
            int initialCluster = edgeIndexer.index(e);
            edgeToCluster[initialCluster] = initialCluster;
            clusterToVertices.put(initialCluster, e.to());
            clusterToVertices.put(initialCluster, e.from());
        }

        // Keep track of the size of each cluster so that we can merge the
        // smaller into the larger.  Each cluster has an initial size of 1
        int[] clusterToNumEdges = new int[numEdges];
        Arrays.fill(clusterToNumEdges, 1);

        // Keep track of how many cluster merges we've performed so that we can
        // terminate early when all of the clusters have been merged
        int numMerges = 0;
        
        // As we cluster the edges, keep track of the density so that once
        // finished, we can recompute the final clustering solution.
        double highestDensity = 0d;
        int mergeStepsWithHighestDensity = 0;

        // While we still have edges to merge and we haven't already merged all
        // the clusters togehter (at most numEdges-1 merges can take place)
        while (!pq.isEmpty() && numMerges < numEdges-1) {
            EdgePair ep = pq.remove();
            int cluster1 = edgeToCluster[edgeIndexer.index(ep.edge1())];
            int cluster2 = edgeToCluster[edgeIndexer.index(ep.edge2())];

            // If the edges are already in the same cluster, we can discard this
            // pair without merging (don't update the counter) and continue
            // looking for a more similar pair
            if (cluster1 == cluster2)
                continue;

            // System.out.printf("%s had the highest similarity%n", ep);

            numMerges++;

            // Figure out which of the clusters is smaller so that we can merge
            // appropriately.
            int smaller = -1, larger = -1;
            if (clusterToNumEdges[cluster1] < clusterToNumEdges[cluster2]) {
                smaller = cluster1;
                larger  = cluster2;
            }
            else {
                smaller = cluster2;
                larger  = cluster1;
            }

            // Merge the smaller into the larger
            for (int k = 0; k < edgeToCluster.length; ++k) {
                if (edgeToCluster[k] == smaller)
                    edgeToCluster[k] = larger;
            }

            // Update the size of the larger cluster.  Note that the smaller
            // cluster still "exists" in the array, but is never referenced
            // again.
            clusterToNumEdges[larger] += clusterToNumEdges[smaller];

            Set<Integer> verticesInSmaller = clusterToVertices.get(smaller);
            System.out.printf("Merged %s into %s%n", verticesInSmaller, clusterToVertices.get(larger));
            clusterToVertices.putMany(larger, verticesInSmaller);

            // Sum the densitites for each partition in this solution.
            double partitionDensitySum = 0d;
            for (Map.Entry<Integer,Set<Integer>> cluster : 
                     clusterToVertices.asMap().entrySet()) {                           
                int numNodesInCluster = cluster.getValue().size();
                int numEdgesInCluster = clusterToNumEdges[cluster.getKey()];
                // Compute this partition's density, adding it to the sum of all
                // partition densities for the current solution
                partitionDensitySum +=
                    computeDensity(numNodesInCluster, numEdgesInCluster);
            }
            
            // Compute the total partition density by averaging the density
            // across all partitions, weighted by the number of edges
            double partitionDensity = 
                (2d / numEdges) * partitionDensitySum;

            System.out.printf("Merge %d/%d had density %f", 
                              numMerges, numEdges-1, partitionDensity);

            veryVerbose(LOGGER, "Merge %d/%d had density %f", 
                        numMerges, numEdges-1, partitionDensity);
            if (numMerges % 1000 == 0)
                verbose(LOGGER, "Merge %d/%d had density %f", 
                            numMerges, numEdges-1, partitionDensity);
            
            if (partitionDensity > highestDensity) {
                highestDensity = partitionDensity;
                mergeStepsWithHighestDensity = numMerges;
                densestSolution.clear();
                densestSolution.putAll(clusterToVertices);
            }
        }

        verbose(LOGGER, "Merge %d had the highest density: %f", 
                mergeStepsWithHighestDensity, highestDensity);

        if (numMerges != numEdges-1) {
            LOGGER.info("Found disconnected components in graph; avoiding full merges");
        }

        return densestSolution;
    }
    */

    /**
     * Performs single-linkage agglomerative clustering on the Graph's edges
     * using a next-best-merge array.  This implementation achieves
     * O(n<sup>2</sup>) run-time complexity and O(n) space, which is a
     * significant savings over running single-linkage with a max-heap.
     */
    private <E extends Edge> MultiMap<Integer,Integer> 
                       singleLink(final Graph<E> g) {


        // Index the edges so that we can quickly look up which cluster an edge
        // is in
        final Indexer<Edge> edgeIndexer = new HashIndexer<Edge>();

        for (Edge e : g.edges()) {
            // Ignore any information on the edges, such as weights or types, by
            // creating our own edges and indexing them
            edgeIndexer.index(new SimpleEdge(e.from(), e.to()));
        }

        final int numEdges = edgeIndexer.size();

        // Keep a simple int->int mapping from each edge's index to (1) the
        // cluster its in, (2) the most similar edge to that edge, (3) the
        // similarity of the most similar edge.  
        int[] edgeToCluster = new int[numEdges];
        final int[] edgeToMostSim = new int[numEdges];
        final double[] edgeToSimOfMostSim = new double[numEdges];

        // Keep track of the vertices in each cluster
        // IntIntMultiMap clusterToVertices = new IntIntHashMultiMap();
        MultiMap<Integer,Integer> clusterToVertices = new HashMultiMap<Integer,Integer>();
        MultiMap<Integer,Integer> densestSolution = new HashMultiMap<Integer,Integer>();

        // Loop over each edge in the graph and add the vertices for that edge
        // to the initial cluster assignment
        for (Edge e : g.edges()) {
            // Ignore any information on the edges, such as weights or types, by
            // creating our own edges.
            int initialCluster = edgeIndexer.index(
                new SimpleEdge(e.from(), e.to()));
            edgeToCluster[initialCluster] = initialCluster;
            clusterToVertices.put(initialCluster, e.to());
            clusterToVertices.put(initialCluster, e.from());
        }

        // Ensure that the reverse lookup table is created in the Indexer ahead
        // of time, since threads will be accessing it concurrently
        edgeIndexer.lookup(0);

        // For each edge, find the most similar cluster updating the relative
        // indices of the rowToMostSimilar arrays with the results.
        Object taskKey = WORK_QUEUE.registerTaskGroup(g.order());
        IntIterator iter1 = g.vertices().iterator();
        while (iter1.hasNext()) {
            final int v1 = iter1.nextInt();
            WORK_QUEUE.add(taskKey, new Runnable() { 
                    public void run() {
                        veryVerbose(LOGGER, "Computing similarities for " + 
                                    "vertex %d", v1);
                        IntSet neighbors = g.getNeighbors(v1);
                        IntIterator it1 = neighbors.iterator();
                        while (it1.hasNext()) {
                            int v2 = it1.nextInt();
                            IntIterator it2 = neighbors.iterator();
                            while (it2.hasNext()) {
                                int v3 = it2.nextInt();
                                if (v2 == v3)
                                    break;
                                double sim = getConnectionSimilarity(
                                    g, v1, v2, v3);

                                int e1index = edgeIndexer
                                    .find(new SimpleEdge(v1, v2));
                                int e2index = edgeIndexer
                                    .find(new SimpleEdge(v1, v3));
                                assert e1index >= 0 : "missing e1";
                                assert e2index >= 0 : "missing e2";
                                assert edgeIndexer.lookup(e1index) != null : "e1 is null";
                                assert edgeIndexer.lookup(e2index) != null : "e2 is null";

                                // Lock on the canonical instance of e1 before
                                // updating its similarity values
                                synchronized(edgeIndexer.lookup(e1index)) {
                                    if (sim > edgeToSimOfMostSim[e1index]) {
                                        edgeToSimOfMostSim[e1index] = sim;
                                        edgeToMostSim[e1index] = e2index;
                                    }
                                }

                                // Lock on the canonical instance of e2 before
                                // updating its similarity values
                                synchronized(edgeIndexer.lookup(e2index)) {
                                    if (sim > edgeToSimOfMostSim[e2index]) {
                                        edgeToSimOfMostSim[e2index] = sim;
                                        edgeToMostSim[e2index] = e1index;
                                    }
                                }
                            }
                        }
                    }
                });
        }
        WORK_QUEUE.await(taskKey);


        // Keep track of the size of each cluster so that we can merge the
        // smaller into the larger.  Each cluster has an initial size of 1
        int[] clusterToNumEdges = new int[numEdges];
        Arrays.fill(clusterToNumEdges, 1);

        // As we cluster the edges, keep track of the density so that once
        // finished, we can recompute the final clustering solution.
        double highestDensity = 0d;
        int mergeStepsWithHighestDensity = 0;

        verbose(LOGGER, "Clustering edges");

        // Perform rows-1 merges to merge all elements
        int mergeIter = 0;
        while (clusterToVertices.size() > 1) {
            if (mergeIter % 1000 == 0) 
                verbose(LOGGER, "Computing dendrogram merge %d/%d",
                        mergeIter+1, numEdges-1);

            //System.out.printf("%n%nStart of merge %d%n", mergeIter);

            // Find the edge that has the highest similarity to another edge
            int edge1index = -1;
            int edge2index = -1; // set during iteration
            double highestSim = -1;
            for (int i = 0; i < edgeToSimOfMostSim.length; ++i) {
//                 System.out.printf("%d is most sim to %d with %f%n", i, 
//                                   edgeToMostSim[i], edgeToSimOfMostSim[i]);
                if (edgeToSimOfMostSim[i] > highestSim) {
                    int c1 = edgeToCluster[i];
                    int mostSim = edgeToMostSim[i];
                    int c2 = edgeToCluster[mostSim];
                    if (c1 != c2) {
                        highestSim = edgeToSimOfMostSim[i];
                        edge1index = i;
                        edge2index = edgeToMostSim[i];
                    }
                }
            }
            
            int cluster1index = -1;
            int cluster2index = -1;

            // No more similar pairs (disconnected graph?) so merge two
            // arbitrary clusters and continue
            if (edge1index == -1) {
                Iterator<Integer> it = clusterToVertices.keySet().iterator();
                cluster1index = it.next();
                cluster2index = it.next(); // by contract, we have > 2 clusters
                
            }
            else {
                cluster1index = edgeToCluster[edge1index];
                cluster2index = edgeToCluster[edge2index];
            }
            
            // When merging clusters each of size > 2, it could be that
            // additional pairs of the two clusters also had higher similarity
            // and were next in the next-best-merge.  However, because these two
            // edges are already in the cluster, we can discard their merge (by
            // setting the similarity to the minimum value) and continue on.
            // Note that we do not need to update the next-most similar to these
            // edges, as that similarity will alread be recorded by the other
            // edges that are not these two.
            if (cluster1index == cluster2index) {
                edgeToSimOfMostSim[edge1index] = -2d;
                // Note that since we are skipping this round, we do not
                // increment the mergeIter counter
                continue;
            }
            // We will merge this iteration
            ++mergeIter; 

//             System.out.printf("Merging c%d (size: %d) with c%d (size: %d)%n",
//                               cluster2index, clusterToVertices.get(cluster2index).size(),
//                               cluster1index, clusterToVertices.get(cluster1index).size());


            Set<Integer> verticesInSmaller = clusterToVertices.get(cluster2index);
//             System.out.printf("Merged %d:%s into %d:%s with simiarity %f%n", 
//                               cluster2index, verticesInSmaller, 
//                               cluster1index, clusterToVertices.get(cluster1index),
//                               highestSim);
            clusterToVertices.putMany(cluster1index, verticesInSmaller);
            clusterToVertices.remove(cluster2index);
            clusterToNumEdges[cluster1index] +=clusterToNumEdges[cluster2index];

            // Short circuit on the last iteration since we don't need to scan
            // through the list of edges again to update their most-similar-edge
            if (clusterToVertices.size() == 1)
                break;

            // Update the similarity for the second edge so that it is no longer
            // merged with another edge.  Even if it is more similar, we maintain
            // the invariant that only the cluster1index is valid after a merge
            // operation
            edgeToSimOfMostSim[edge1index] = -3d;
            edgeToSimOfMostSim[edge2index] = -4d;
            
            // For all the edges not in the current cluster, find the most
            // similar data point to the now-merged cluster.  Note that this
            // process doesn't need to update the nearest neighbors of these
            // nodes, as they should still be valid post-merge.
            int mostSimEdgeToCurCluster = -1;
            highestSim = -5d;
            Edge e1 = edgeIndexer.lookup(edge1index);
            Edge e2 = edgeIndexer.lookup(edge2index);
//             System.out.printf("Cluster assignments halfway into merge %d: %s%n", 
//                               mergeIter, Arrays.toString(edgeToCluster));
            for (int i = 0; i < numEdges; i++) {                
                int cId = edgeToCluster[i];
                if (cId == cluster1index) {
                    // See if the most similar edge is also an edge in cluster1,
                    // in which case we should invalidate its similarity since
                    // the clusters are already merged
//                     int mostSimEdge = edgeToMostSim[i];
//                     if (edgeToCluster[mostSimEdge] == cluster2index)
//                         edgeToSimOfMostSim[i] = Double.MIN_VALUE;
                    continue;
                }
                else if (cId == cluster2index) {
                    edgeToCluster[i] = cluster1index;                   
                    // See if the most similar edge is also an edge in cluster1,
                    // in which case we should invalidate its similarity since
                    // the clusters are already merged
//                     int mostSimEdge = edgeToMostSim[i];
//                     if (edgeToCluster[mostSimEdge] == cluster1index)
//                         edgeToSimOfMostSim[i] = Double.MIN_VALUE;
                    continue;
                }
                                
                Edge e3 = edgeIndexer.lookup(i);
                assert e1 != null : "e1 is null, edge1index: " + edge1index;
                assert e2 != null : "e2 is null, edge2index: " + edge2index;
                assert e3 != null : "e3 is null, edge3index: " + i + ", debug:" + debug(edgeIndexer, numEdges);

                double simToE1 = getConnectionSimilarity(g, e1, e3);
                double simToE2 = getConnectionSimilarity(g, e2, e3);
                
//                 System.out.printf("Comparing %s with %s: %f%n", e1, e3, simToE1);
//                 System.out.printf("Comparing %s with %s: %f%n", e2, e3, simToE2);

                double sim = Math.max(simToE1, simToE2);
                if (sim > highestSim) {
                    highestSim = sim;
                    mostSimEdgeToCurCluster = i;
                }
            }
//             System.out.printf("Most similar edge to edge %d is %d%n",
//                               edge1index, mostSimEdgeToCurCluster);
            edgeToMostSim[edge1index] = mostSimEdgeToCurCluster;
            edgeToSimOfMostSim[edge1index] = highestSim;

            // Sum the densitites for each partition in this solution.
            double partitionDensitySum = 0d;
            int edgeSum = 0, nodeSum = 0;
            for (Map.Entry<Integer,Set<Integer>> cluster : 
                     clusterToVertices.asMap().entrySet()) {                           
                int numNodesInCluster = cluster.getValue().size();
                int numEdgesInCluster = clusterToNumEdges[cluster.getKey()];
                edgeSum += numEdgesInCluster;
//                 System.out.printf("Cluster %d: %d nodes, %d edges -> %f%n",
//                                   cluster.getKey(), numNodesInCluster,
//                                   numEdgesInCluster,
//                                   computeDensity(numNodesInCluster, numEdgesInCluster));
                // Compute this partition's density, adding it to the sum of all
                // partition densities for the current solution
                partitionDensitySum +=
                    computeDensity(numNodesInCluster, numEdgesInCluster);
                // System.out.printf("  %d: %s%n", cluster.getKey(), cluster.getValue());
            }
            assert edgeSum == numEdges : "Adding edges somewhere";

            
            // Compute the total partition density by averaging the density
            // across all partitions, weighted by the number of edges
            double partitionDensity = 
                (2d / numEdges) * partitionDensitySum;


//             System.out.printf("Merge %d/%d had density %f%n", 
//                               mergeIter, numEdges-1, partitionDensity);

            veryVerbose(LOGGER, "Merge %d/%d had density %f", 
                        mergeIter, numEdges-1, partitionDensity);
            if (mergeIter % 1000 == 0)
                verbose(LOGGER, "Merge %d/%d had density %f", 
                        mergeIter, numEdges-1, partitionDensity);
            
            if (partitionDensity > highestDensity) {
                highestDensity = partitionDensity;
                mergeStepsWithHighestDensity = mergeIter;
                densestSolution.clear();
                densestSolution.putAll(clusterToVertices);
            }
        }

//         System.out.printf("Merge %d had the highest density: %f%n", 
//                           mergeStepsWithHighestDensity, highestDensity);

        verbose(LOGGER, "Merge %d had the highest density: %f", 
                mergeStepsWithHighestDensity, highestDensity);

        return densestSolution;
    }

    private static String debug(Indexer<Edge> indexer, int numEdges) {
        for (int i = 0; i < numEdges; ++i)
            System.out.println(i + ": " + indexer.lookup(i));
        return "";
    }

    /**
     * Performs single-linkage agglomerative clustering on the Graph's edges
     * using a next-best-merge array until the specified number of clusters has
     * been reached.  This implementation achieves O(n<sup>2</sup>) run-time
     * complexity and O(n) space, which is a significant savings over running
     * single-linkage with a max-heap.
     *
     * @param numClusters the number of clusters to produce
     */
    private <E extends Edge> MultiMap<Integer,Integer> singleLink(
                       final Graph<E> g, int numClusters) {

        final int numEdges = g.size();
        if (numClusters < 1 || numClusters > numEdges)
            throw new IllegalArgumentException(
                "Invalid range for number of clusters: " + numClusters);       

        // Index the edges so that we can quickly look up which cluster an edge
        // is in
        final Indexer<Edge> edgeIndexer = new HashIndexer<Edge>();

        // Keep a simple int->int mapping from each edge's index to (1) the
        // cluster its in, (2) the most similar edge to that edge, (3) the
        // similarity of the most similar edge.  
        int[] edgeToCluster = new int[numEdges];
        final int[] edgeToMostSim = new int[numEdges];
        final double[] edgeToSimOfMostSim = new double[numEdges];

        // Keep track of the vertices in each cluster
        MultiMap<Integer,Integer> clusterToVertices = new HashMultiMap<Integer,Integer>();

        // Loop over each edge in the graph and add the vertices for that edge
        // to the initial cluster assignment
        for (Edge e : g.edges()) {
            int initialCluster = edgeIndexer.index(
                new SimpleEdge(e.from(), e.to()));
            edgeToCluster[initialCluster] = initialCluster;
            clusterToVertices.put(initialCluster, e.to());
            clusterToVertices.put(initialCluster, e.from());
        }

        // Ensure that the reverse lookup table is created in the Indexer ahead
        // of time, since threads will be accessing it concurrently
        edgeIndexer.lookup(0);

        // For each edge, find the most similar cluster updating the relative
        // indices of the rowToMostSimilar arrays with the results.
        Object taskKey = WORK_QUEUE.registerTaskGroup(g.order());
        IntIterator iter1 = g.vertices().iterator();
        while (iter1.hasNext()) {
            final int v1 = iter1.nextInt();
            WORK_QUEUE.add(taskKey, new Runnable() { 
                    public void run() {
                        veryVerbose(LOGGER, "Computing similarities for " + 
                                    "vertex %d", v1);
                        IntSet neighbors = g.getNeighbors(v1);
                        IntIterator it1 = neighbors.iterator();
                        while (it1.hasNext()) {
                            int v2 = it1.nextInt();
                            IntIterator it2 = neighbors.iterator();
                            while (it2.hasNext()) {
                                int v3 = it2.nextInt();
                                if (v2 == v3)
                                    break;
                                double sim = getConnectionSimilarity(
                                    g, v1, v2, v3);
                                
                                int e1index = edgeIndexer
                                    .index(new SimpleEdge(v1, v2));
                                int e2index = edgeIndexer
                                    .index(new SimpleEdge(v1, v3));

                                // Lock on the canonical instance of e1 before
                                // updating its similarity values
                                synchronized(edgeIndexer.lookup(e1index)) {
                                    if (sim > edgeToSimOfMostSim[e1index]) {
                                        edgeToSimOfMostSim[e1index] = sim;
                                        edgeToMostSim[e1index] = e2index;
                                    }
                                }

                                // Lock on the canonical instance of e2 before
                                // updating its similarity values
                                synchronized(edgeIndexer.lookup(e2index)) {
                                    if (sim > edgeToSimOfMostSim[e2index]) {
                                        edgeToSimOfMostSim[e2index] = sim;
                                        edgeToMostSim[e2index] = e1index;
                                    }
                                }
                            }
                        }
                    }
                });
        }
        WORK_QUEUE.await(taskKey);

        // Keep track of the size of each cluster so that we can merge the
        // smaller into the larger.  Each cluster has an initial size of 1
        int[] clusterToNumEdges = new int[numEdges];
        Arrays.fill(clusterToNumEdges, 1);

        verbose(LOGGER, "Clustering edges");

        // Keep merging until we reach the desired number of clusters
        int mergeIter = 0;
        while (clusterToVertices.size() > numClusters) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(Level.FINE, "Computing dendrogram merge {0}/{1}",
                           new Object[] { mergeIter+1, numEdges-1 });

            // Find the edge that has the highest similarity to another edge
            int edge1index = -1;
            int edge2index = -1; // set during iteration
            double highestSim = -1;
            for (int i = 0; i < edgeToSimOfMostSim.length; ++i) {
                if (edgeToSimOfMostSim[i] > highestSim) {
                    int c1 = edgeToCluster[i];
                    int mostSim = edgeToMostSim[i];
                    int c2 = edgeToCluster[mostSim];
                    if (c1 != c2) {
                        highestSim = edgeToSimOfMostSim[i];
                        edge1index = i;
                        edge2index = edgeToMostSim[i];
                    }
                }
            }
            
            int cluster1index = -1;
            int cluster2index = -1;

            // No more similar pairs (disconnected graph?) so merge two
            // arbitrary clusters and continue
            if (edge1index == -1) {
                Iterator<Integer> it = clusterToVertices.keySet().iterator();
                cluster1index = it.next();
                cluster2index = it.next(); // by contract, we have > 2 clusters
                
            }
            else {
                cluster1index = edgeToCluster[edge1index];
                cluster2index = edgeToCluster[edge2index];
            }
            assert cluster1index != cluster2index : "merging same cluster";

//             System.out.printf("Merging c%d (size: %d) with c%d (size: %d)%n",
//                               cluster2index, clusterToVertices.get(cluster2index).size(),
//                               cluster1index, clusterToVertices.get(cluster1index).size());
            Set<Integer> verticesInSmaller = clusterToVertices.get(cluster2index);
            clusterToVertices.putMany(cluster1index, verticesInSmaller);
            clusterToVertices.remove(cluster2index);
            clusterToNumEdges[cluster1index] +=clusterToNumEdges[cluster2index];

            // Short circuit on the last iteration since we don't need to scan
            // through the list of edges again to update their most-similar-edge
            if (mergeIter == numEdges - 2)
                break;

            // Update the similarity for the second edge so that it is no longer
            // merged with another edge.  Even if it is more similar, we maintain
            // the invariant that only the cluster1index is valid after a merge
            // operation
            edgeToSimOfMostSim[edge1index] = -2d;
            edgeToSimOfMostSim[edge2index] = -3d;
            
            // For all the edges not in the current cluster, find the most
            // similar data point to the now-merged cluster.  Note that this
            // process doesn't need to update the nearest neighbors of these
            // nodes, as they should still be valid post-merge.
            int mostSimEdgeToCurCluster = -1;
            highestSim = -4d;
            Edge e1 = edgeIndexer.lookup(edge1index);
            Edge e2 = edgeIndexer.lookup(edge2index);

            for (int i = 0; i < numEdges; i++) {                
                int cId = edgeToCluster[i];
                if (cId == cluster1index) {
                    // See if the most similar edge is also an edge in cluster1,
                    // in which case we should invalidate its similarity since
                    // the clusters are already merged
//                     int mostSimEdge = edgeToMostSim[i];
//                     if (edgeToCluster[mostSimEdge] == cluster2index)
//                         edgeToSimOfMostSim[i] = Double.MIN_VALUE;
                     continue;
                }
                else if (cId == cluster2index) {
                    edgeToCluster[i] = cluster1index;                   
                    // See if the most similar edge is also an edge in cluster1,
                    // in which case we should invalidate its similarity since
                    // the clusters are already merged
//                     int mostSimEdge = edgeToMostSim[i];
//                     if (edgeToCluster[mostSimEdge] == cluster1index)
//                         edgeToSimOfMostSim[i] = Double.MIN_VALUE;
                    continue;
                }
                                
                
                
                Edge e3 = edgeIndexer.lookup(i);
                double simToE1 = getConnectionSimilarity(g, e1, e3);
                double simToE2 = getConnectionSimilarity(g, e2, e3);

                double sim = Math.max(simToE1, simToE2);
                if (sim > highestSim) {
                    highestSim = sim;
                    mostSimEdgeToCurCluster = i;
                }
            }

            edgeToMostSim[edge1index] = mostSimEdgeToCurCluster;
            edgeToSimOfMostSim[edge1index] = highestSim;
        }

        return clusterToVertices;
    }


    /**
     * Computes the density of the provided partition of edges
     */
    protected double computeDensity(int numNodes, int numEdges) {        
        // Special case when the number of nodes is 2, which has a density of 0    
        if (numNodes == 2)
            return 0;

        return numEdges * (numEdges - numNodes + 1d) 
            / (numNodes - 1d) / (numNodes - 2d);
    }

    /**
     * Calculates the similarity between all pair-wise combinations of edges,
     * returning a max-heap ({@link PriorityQueue}) which has the most similar
     * edges appear at the top.  This method assumes that the only similarities
     * that matter are those that occur between two edges that share a vertex.
     *
     * @param graph a graph whose edges are to be compared 
     * @param minSimilarity an optional parameter for discarding edge pairs
     *        whose similarity is below this value, which can save space held by
     *        low-similairty pairs that are never used in the merging process.
     *        However, setting this value too high results can result in
     *        incomplete or incorrect merge sequences.
     *
     * @return the similarity matrix
     */
    private <E extends Edge> PriorityQueue<EdgePair> calcuateEdgeSimQueue(
                       final Graph<E> graph, final double minSimilarity) {
        
        final int numVertices = graph.order();
        final int numEdges = graph.size();
        double avgDegree = numEdges  / (double)numVertices;
        final int numComparisons = (int)(((avgDegree * (avgDegree+1)) / 2) * numVertices);
//         System.out.printf("size: %d, order: %d, avg. degree: %f, expected num comparisons: %d%n",
//                           numEdges, numVertices, avgDegree, numComparisons);

        final PriorityQueue<EdgePair> pq = 
            new PriorityQueue<EdgePair>(numComparisons);
        Object key = WORK_QUEUE.registerTaskGroup(graph.order());
        
        IntIterator iter1 = graph.vertices().iterator();
        while (iter1.hasNext()) {
            final int v1 = iter1.nextInt();
            WORK_QUEUE.add(key, new Runnable() {
                    public void run() {
                        veryVerbose(LOGGER, "Computing similarities for " + 
                                    "vertex %d", v1);
                        //Set<E> adjList = graph.getAdjacencyList(v1);
                        IntSet neighbors = graph.getNeighbors(v1);
                        // Create a thread-local PriorityQueue that will hold
                        // the edge similarities for this vertex.  Once all the
                        // simialrites have been computed, we can update the
                        // thread-shared queue with minimal locking
                        PriorityQueue<EdgePair> localQ = 
                            new PriorityQueue<EdgePair>(neighbors.size());
                        IntIterator it1 = neighbors.iterator();
                        // for (E e1 : adjList) {
                        while (it1.hasNext()) {
                            // int v2 = (e1.to() == v1) ? e1.from() : e1.to();
                            int v2 = it1.nextInt();
                            
                            IntIterator it2 = neighbors.iterator();
                            // for (Edge e2 : graph.getAdjacencyList(v1)) {
                            while (it2.hasNext()) {
                                int v3 = it2.nextInt();
                                if (v2 == v3)
                                    break;
                                // if (e1.equals(e2))
                                //     break;                               
                                // int v3 = (e2.to() == v1) ? e2.from() : e2.to();
                                float sim = (float)
                                    getConnectionSimilarity(graph, v1, v2, v3);
//                                 System.out.printf("(%d, %d), (%d, %d) : %f%n",
//                                                   Math.min(v1, v2),
//                                                   Math.max(v1, v2), 
//                                                   Math.min(v1, v3), 
//                                                   Math.max(v1, v3), sim);
                                if (sim > minSimilarity)
                                    // localQ.add(new EdgePair(-sim, e1, e2));
                                    localQ.add(new EdgePair(-sim, v1, v2, v3));
                            }
                        }
                        synchronized(pq) {
                            pq.addAll(localQ);
                            int comps = pq.size();
                            veryVerbose(LOGGER, "%d/%d comparisons " +
                                        "completed (%f)", comps, numComparisons,
                                        (double)comps / numComparisons);
                        }
                    }
                });
        }
        WORK_QUEUE.await(key);
        return pq;
    }

    /**
     * Computes the connection similarity for the two edges, first calculating
     * the impost and keystones nodes.  If the edges are not connected, returns
     * 0.
     *
     * @see #getConnectionSimilarity(Graph,int,int,int)
     */
    private <E extends Edge> double getConnectionSimilarity(
            Graph<E> graph, Edge e1, Edge e2) {
        int e1to = e1.to();
        int e1from = e1.from();
        int e2to = e2.to();
        int e2from = e2.from();
        if (e1to == e2to)
            return getConnectionSimilarity(graph, e1to, e1from, e2from);
        else if (e1to == e2from)
            return getConnectionSimilarity(graph, e1to, e1from, e2to);
        else if (e1from == e2to)
            return getConnectionSimilarity(graph, e1from, e1to, e2from);
        else if (e1from == e2from)
            return getConnectionSimilarity(graph, e1from, e1to, e2to);
        else
            return 0;
    }


    /**
     * Computes the similarity of the two edges as the Jaccard index of the
     * neighbors of two impost nodes.  The impost nodes are the two nodes the
     * edges do not have in common.  Subclasses may override this method to
     * define a new method for computing edge similarity.
     *
     * <p><i>Implementation Note</i>: Subclasses that wish to override this
     * behavior should be aware that this method is likely to be called by
     * multiple threads and therefor should make provisions to be thread safe.
     * In addition, this method may be called more than once per edge pair if
     * the similarity matrix is being computed on-the-fly.
     *
     * @param sm a matrix containing the connections between edges.  A non-zero
     *        value in location (i,j) indicates a node <i>i</i> is connected to
     *        node <i>j</i> by an edge.
     * @param e1 an edge to be compared with {@code e2}
     * @param e2 an edge to be compared with {@code e1}
     *
     * @return the similarity of the edges.a
     */
    protected <E extends Edge> double getConnectionSimilarity(
            Graph<E> graph, int keystone, int impost1, int impost2) {
        
        IntSet n1 = graph.getNeighbors(impost1);
        IntSet n2 = graph.getNeighbors(impost2);
        int n1size = n1.size();
        int n2size = n2.size();
        // Swap based on size prior to searching for which vertices are in
        // common
        if (n1size > n2size) {
            IntSet tmp = n2;
            n2 = n1;
            n1 = tmp;
            int t = impost1;
            impost1 = impost2;
            impost2 = t;
        }

        int inCommon = 0;
        IntIterator it = n1.iterator();
        while (it.hasNext()) {
            int v = it.nextInt();
            if (n2.contains(v))
                inCommon++;
        }

        if (n2.contains(impost1)) 
            inCommon++;
        if (n1.contains(impost2)) 
            inCommon++;
        
        // NOTE: it doesn't matter that n1 and n2's sizes might be potentually
        // switched since we're doing a commutative operation
        return (double)inCommon / (n1size + n2size + 2 - inCommon);
    }


    /**
     * A structure for holding the indices of two {@link Edge} instances and
     * their corresponding negative similarity.  The similarity is negated so
     * that when sorted, the edge pair with the highest similarity appears at
     * the front of the {@link PriorityQueue}.
     */
    private static class EdgePair implements Comparable<EdgePair> {
        
        // int e1, e2;
        int keystone;
        int impost1;
        int impost2;

        // double negSim;
        float negSim;
        
        public EdgePair(float negSim, Edge e1, Edge e2) {
//             this.e1 = e1;
//             this.e2 = e2;
//             this.negSim = negSim;
            throw new Error();
        }

        public EdgePair(float negSim, int keystone, int impost1, int impost2) {
            this.keystone = keystone;
            this.impost1 = impost1;
            this.impost2 = impost2;
            this.negSim = negSim;
        }

        public int compareTo(EdgePair ep) {
//             return Double.compare(negSim, ep.negSim);
            return Float.compare(negSim, ep.negSim);
        }

        public Edge edge1() { return new SimpleEdge(keystone, impost1); }
        public Edge edge2() { return new SimpleEdge(keystone, impost2); }

        public boolean equals(Object o) {
            if (o instanceof EdgePair) {
                EdgePair ep = (EdgePair)o;
                 return keystone == ep.keystone
                     && impost1 == ep.impost1
                     && impost2 == ep.impost2
                     && negSim == ep.negSim;

            }
            return false;
        }

        public int hashCode() {
            // return e1.hashCode() ^ e2.hashCode();
            return impost1 ^ impost2 ^ keystone;
        }

        public String toString() {
            int i = Math.min(impost1, impost2);
            int j = Math.max(impost1, impost2);
            return i + "-" + keystone + "-" + j + ": " + (-negSim);
        }
    }
}