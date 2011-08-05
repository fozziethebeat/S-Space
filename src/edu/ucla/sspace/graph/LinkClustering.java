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
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;
import edu.ucla.sspace.clustering.Merge;
import edu.ucla.sspace.clustering.SoftAssignment;

import edu.ucla.sspace.matrix.AbstractMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseHashMatrix;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.SparseSymmetricMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;

import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.Indexer;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.ObjectCounter;
import edu.ucla.sspace.util.ObjectIndexer;
import edu.ucla.sspace.util.OpenIntSet;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.WorkQueue;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseIntegerVector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.verbose;


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
     * A prefix for specifying properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.LinkClustering";

    /**
     * FILL IN
     */
    public static final String SMALL_EDGE_INDEX_PROPERTY =
        PROPERTY_PREFIX + ".smallEdgeIndex";
    
    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(LinkClustering.class.getName());

    /**
     * The work used by all {@code LinkClustering} instances to perform
     * multi-threaded operations.
     */
    private static final WorkQueue WORK_QUEUE = new WorkQueue(1);
    
    /**
     * The merges for the prior run of this clustering algorithm
     */
    private List<Merge> mergeOrder;

    /**
     * The list of vertex pairs that were last merged.  This list is maintained
     * in the same order as the initial cluster ordering.
     */
    private Indexer<Edge> edgeIndices;

    /**
     * The number of rows in the input matrix that was last clustered.
     */
    private int numVertices;
    
    /**
     * The densitites of all the partitions 
     */
    private double[] partitionDensities;

    private int densestSolutionIndex;

    /**
     * Instantiates a new {@code LinkClustering} instance.
     */
    public LinkClustering() { 
        mergeOrder = null;
        edgeIndices = null;
        numVertices = -1;
        densestSolutionIndex = -1;
    }

    /**
     * <i>Ignores the specified number of clusters</i> and returns the
     * clustering solution according to the partition density.
     *
     * @param numClusters this parameter is ignored.
     *
     * @throws IllegalArgumentException if {@code matrix} is not square, or is
     *         not an instance of {@link SparseMatrix}
     */
    public <E extends Edge> Assignment[] cluster(
                      final Graph<E> graph, Properties props) {

        final int numVertices = graph.order();
        this.numVertices = numVertices;
        LOGGER.fine("Generating link similarity matrix for " + numVertices + " nodes");

        final int numEdges = graph.size();
        LOGGER.fine("Number of edges to analyze: " + numEdges);
        
        // Map each of the graph's edges to an index, which will use to fill the
        // similarity matrix
        System.out.println("Finding connections");
        boolean performEdgeCompression = 
            Boolean.parseBoolean(props.getProperty(SMALL_EDGE_INDEX_PROPERTY));
        final Indexer<Edge> edgeIndices = 
            findEdgeIndices(graph, performEdgeCompression);
        this.edgeIndices = edgeIndices;
        LOGGER.fine("Number of connections to cluster: " + edgeIndices.size());        
        // System.out.printf("Found %d connections", edgeIndices.size());

        System.out.println("Computing similarity matrix");
        // Matrix edgeSimMatrix = calculateConnectionSimMatrix(edgeIndices, graph);
        PriorityQueue<EdgePair> pq = calcuateEdgeSimQueue(edgeIndices, graph);
        System.out.println("Computed similarity matrix"); 
        
        LOGGER.fine("Computing single linkage link clustering");

        final List<Merge> mergeOrder = 
            singleLink(pq, edgeIndices.size());

        this.mergeOrder = mergeOrder;
        System.out.printf("Number of connections %d, number of merges: %d%n",
                          edgeIndices.size(), mergeOrder.size());
        int maxCluster = mergeOrder.size();

        LOGGER.fine("Calculating partition densitities");       
        this.partitionDensities = new double[mergeOrder.size()];

        // Divide up the series partitionings into multiple groups.  Each group
        // will calculate the partitioning at the start of its group's
        // sequenence and then incrementally update this partitioning for each
        // subsequent Merge in the series.  This avoids having each task
        // completely recompute the total partitioning.
        int partitionDivisions = WORK_QUEUE.availableThreads();
        final int mergesPerDivision = mergeOrder.size() / partitionDivisions;

        // Register a task group for calculating each of the group's partition
        // densitities
        Object key = WORK_QUEUE.registerTaskGroup(partitionDivisions);       

        for (int mergeStart = 0; mergeStart < mergeOrder.size(); 
                 mergeStart += mergesPerDivision) {

            final int divisionStart = mergeStart;
            final int divisionEnd = Math.min(mergeStart + mergesPerDivision,
                                             mergeOrder.size());
            System.out.printf("Task X running merges %d to %d%n", divisionStart, divisionEnd);
            WORK_QUEUE.add(key, new Runnable() {
                    public void run() {
                        // Get the merges up to the start of this particular
                        // partitioning of the connections.  Note that this list
                        // does not include the first merge in the division
                        List<Merge> mergeSteps = 
                            mergeOrder.subList(0, divisionStart);
                        
                        // Convert the merges to a partitions of connections
                        MultiMap<Integer,Integer> clusterToEdges = 
                            computeEdgePartitions(mergeSteps, edgeIndices.size());

                        
                        verbose(LOGGER, "Computing density of partition %d/%d",
                                divisionStart, mergeOrder.size());

                        // For each merge in this division, we'll incrementally
                        // update the clusterToEdges mapping and then compute
                        // its partition density.
                        for (int i = divisionStart; i < divisionEnd; ++i) {
                            // Update the clustering solution for the merge
                            Merge m = mergeOrder.get(i);
                            clusterToEdges.putMany(m.remainingCluster(),
                                clusterToEdges.remove(m.mergedCluster()));

                            if ((i - divisionStart) % 10000 == 0)
                                verbose(LOGGER, "Computing density of partition"
                                        + " %d/%d", divisionStart, 
                                        mergeOrder.size());
                                                        
                            // Sum the densitites for each partition in this
                            // solution.
                            double partitionDensitySum = 0d;
                            for (Integer cId : clusterToEdges.keySet()) {
                                Set<Integer> linkPartition = 
                                    clusterToEdges.get(cId);
                                double density = computeDensity(
                                    linkPartition, edgeIndices);
                                partitionDensitySum += density;
                            }
                           
                            // Compute the total partition density by averaging
                            // the density across all partitions, weighted by
                            // the number of edges
                            double partitionDensity = 
                                (2d / numEdges) * partitionDensitySum;
                        
                            // Update the thread-shared partition density map
                            // with this task's calculation
                            LinkClustering.this.partitionDensities[i]
                                = partitionDensity;
                        }
                    }
                });
        }

        // Wait for all the partition densities to be calculated
        WORK_QUEUE.await(key);

        int partitionWithMaxDensity = - 1;
        double highestDensity = Double.MIN_VALUE;
        for (int i = 0; i < partitionDensities.length; ++i) {
            double d = partitionDensities[i];
            if (d > highestDensity) {
                highestDensity = d;
                partitionWithMaxDensity = i;
            }
        }

        verbose(LOGGER, "Partition %d had the highest density: %f",
                partitionWithMaxDensity, highestDensity);

        // NOTE: we don't explicitly calculate the density of solution 0, which
        // always has density 0.  Therefore, the partition index is really the
        // number of merges that occurred - 1.  To compute the final assigment,
        // we need to increment the partition index by 1. 
        //
        // For proof, consider that to compute the solution with all the merges
        // the index would need to be mergeOrder.size(), which is one more than
        // range of partitionWithMaxDensity on its own.
        //
        // This seems confusing, but it's a subtle "feature" of how we compute
        // the solutions -jurgens
        partitionWithMaxDensity++;
        densestSolutionIndex = partitionWithMaxDensity;
        
        // Select the solution with the highest partition density and assign
        // nodes accordingly
        MultiMap<Integer,Integer> bestEdgeAssignment =
            computeEdgePartitions(
                mergeOrder.subList(0, partitionWithMaxDensity), 
                edgeIndices.size());

        List<Set<Integer>> nodeClusters = 
            new ArrayList<Set<Integer>>(numVertices);
        for (int i = 0; i < numVertices; ++i) 
            nodeClusters.add(new HashSet<Integer>());
        
        // Ignore the original partition labeling, and use our own cluster
        // labeling to ensure that the IDs are contiguous.
        int clusterId = 0;

        // For each of the partitions, add the partion's cluster ID to all the
        // nodes that are connected by one of the partition's edges
        for (Integer cluster : bestEdgeAssignment.keySet()) {
            Set<Integer> linkPartition = bestEdgeAssignment.get(cluster);
            for (Integer edgeIndex : linkPartition) {
                Edge e = edgeIndices.lookup(edgeIndex);
                nodeClusters.get(e.to()).add(clusterId);
                nodeClusters.get(e.from()).add(clusterId);
            }
            // Update the cluster id
            clusterId++;
        }

        Assignment[] nodeAssignments = new Assignment[numVertices];
        for (int i = 0; i < nodeAssignments.length; ++i) {
            nodeAssignments[i] = 
                new SoftAssignment(nodeClusters.get(i));
        }
        return nodeAssignments;
    }

    public MultiMap<Integer,Edge> getEdgeClusters(int solutionNum) {
        MultiMap<Integer,Edge> clusterToEdges = new HashMultiMap<Integer,Edge>();
        // Select the solution with the highest partition density and assign
        // nodes accordingly
        MultiMap<Integer,Integer> bestEdgeAssignment =
            computeEdgePartitions(mergeOrder.subList(0, densestSolutionIndex), 
                                  edgeIndices.size());
        
        // Ignore the original partition labeling, and use our own cluster
        // labeling to ensure that the IDs are contiguous.
        int clusterId = 0;

        // For each of the partitions, add the partion's cluster ID to all the
        // nodes that are connected by one of the partition's edges
        for (Integer cluster : bestEdgeAssignment.keySet()) {
            Set<Integer> linkPartition = bestEdgeAssignment.get(cluster);
            for (Integer edgeIndex : linkPartition) {
                Edge e = edgeIndices.lookup(edgeIndex);
                clusterToEdges.put(clusterId, e);
            }
            // Update the cluster id
            clusterId++;
        }
        return clusterToEdges;
    }

    public int getReturnedSolutionNumber() {
        return densestSolutionIndex;
    }

    private List<Merge> singleLink(PriorityQueue<EdgePair> pq, int numEdges) {
        // Place each edge in its own cluster initially        
        int[] edgeToCluster = new int[numEdges];
        for (int i = 0; i < edgeToCluster.length; ++i)
            edgeToCluster[i] = i;

        // Keep track of the size of each cluster so that we can merge the
        // smaller into the larger.  Each cluster has an initial size of 1
        int[] clusterToSize = new int[numEdges];
        Arrays.fill(clusterToSize, 1);

        // Keep track of how many cluster merges we've performed so that we can
        // terminate early when all of the clusters have been merged
        int numMerges = 0;

        // Create a list to hold all the merge operations
        List<Merge> merges = new ArrayList<Merge>(numEdges - 1);

        // While we still have edges to merge and we haven't already merged all
        // the clusters togehter (at most numEdges-1 merges can take place)
        while (!pq.isEmpty() && numMerges < numEdges-1) {
            EdgePair ep = pq.remove();
            int cluster1 = edgeToCluster[ep.e1];
            int cluster2 = edgeToCluster[ep.e2];

            // If the edges are already in the same cluster, we can discard this
            // pair without merging (don't update the counter) and continue
            // looking for a more similar pair
            if (cluster1 == cluster2)
                continue;

            numMerges++;

            if (numMerges % 1000 == 0)
                verbose(LOGGER, "computing merge %d/%d", numMerges, numEdges-1);

            // Figure out which of the clusters is smaller so that we can merge
            // appropriately.
            int smaller = -1, larger = -1;
            if (clusterToSize[cluster1] < clusterToSize[cluster2]) {
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
            clusterToSize[larger] += clusterToSize[smaller];

            // System.out.printf("%d: merging %d with %d (%f)%n", numMerges-1, smaller, larger, cp.negSim);
            merges.add(new Merge(larger, smaller, ep.invSim));
        }

        if (numMerges != numEdges-1) {
            System.out.println("Found disconnected components in graph; avoiding full merges");
        }

        System.out.printf("Generated %d merges from %d edges (%d remain)%n",
                          numMerges, numEdges - pq.size(), pq.size());
        return merges;
    }

    private static class EdgePair implements Comparable<EdgePair> {
        
        int e1, e2;
        double invSim;
        
        public EdgePair(double invSim, int e1, int e2) {
            this.e1 = e1;
            this.e2 = e2;
            this.invSim = invSim;
        }

        public int compareTo(EdgePair ep) {
            return Double.compare(invSim, ep.invSim);
        }

        public boolean equals(Object o) {
            if (o instanceof EdgePair) {
                EdgePair ep = (EdgePair)o;
                return e1 == ep.e1
                    && e2 == ep.e2
                    && invSim == ep.invSim;
            }
            return false;
        }

        public int hashCode() {
            long v = Double.doubleToLongBits(invSim);
            return (int)(v^(v>>>32));
        }
    }

    /**
     * Computes the density of the provided partition of edges
     * 
     * @param graph the graph
     * @param linkPartition the set of edges contained in the partition
     * @param edgeIndices a mapping from {@link Edge} instances to their numeric
     *        index
     */
    protected double computeDensity(Set<Integer> linkPartition,
                                    Indexer<Edge> edgeIndices) {

        // Special case when the number of nodes is 2, which has a density of 0    
        int numEdges = linkPartition.size();
        if (numEdges == 1)
            return 0;

        // Determine how many nodes and edges are actually in this partition as
        // well as how many edges of each type.
        Set<Integer> nodesInPartition = new HashSet<Integer>();

        for (Integer edgeIndex : linkPartition) {
            Edge e = edgeIndices.lookup(edgeIndex);
            nodesInPartition.add(e.from());
            nodesInPartition.add(e.to());
        }

        int numNodes = nodesInPartition.size();
        assert numNodes > 2 : "self-edge in graph";

        return numEdges * (numEdges - numNodes + 1d) 
                           / (numNodes - 1) / (numNodes - 2);
    }

    /**
     * Returns the pairs of vertices that are connected in the graph, which have
     * been mapped to indices.
     */
    private Indexer<Edge> findEdgeIndices(Graph<? extends Edge> graph,
                                          boolean performCompression) {
        if (performCompression) 
            return new EdgeIndexer(graph);

        Indexer<Edge> edgeIndices = new ObjectIndexer<Edge>();
        for (Edge e : graph.edges()) {
            edgeIndices.index(e);
            if (edgeIndices.size() % 10000 == 0)
                verbose(LOGGER, "indexed %d edges", edgeIndices.size());
        }
        return edgeIndices;
    }

    /**
     * Calculates the similarity matrix for the edges.  The similarity matrix is
     * symmetric.  This method assumes that the
     * only similarities that matter are those that occur between two edges
     * that share a vertex.
     *
     * @param edgeIndices the list of all edges known to the system
     * @param sm a square matrix whose values denote edges between the rows.
     *
     * @return the similarity matrix
     */
    private <E extends Edge> PriorityQueue<EdgePair> calcuateEdgeSimQueue(
                         final Indexer<Edge> edgeIndices, 
                         final Graph<E> graph) {

        int numEdges = edgeIndices.size();
        final int numVertices = graph.order();

        PriorityQueue<EdgePair> pq = new PriorityQueue<EdgePair>(numEdges);

        List<EdgePair> hack = new ArrayList<EdgePair>();
        for (int vertex : graph.vertices()) {
            final int v1 = vertex;
            verbose(LOGGER, "Computing similarities for vertex %d", v1);
            for (Edge e1 : graph.getAdjacencyList(v1)) {
                int e1index = edgeIndices.find(e1);
                if (e1index < 0)
                    throw new IllegalStateException("Umapped pair: " + e1);
                
                int v2 = (e1.to() == v1) ? e1.from() : e1.to();
                
                for (Edge e2 : graph.getAdjacencyList(v1)) {
                    if (e1.equals(e2))
                        break;
                    
                    int v3 = (e2.to() == v1) ? e2.from() : e2.to();
                    int e2index = edgeIndices.find(e2);                                                   
                    double sim = getConnectionSimilarity(graph, v1, v2, v3);
                    pq.add(new EdgePair(-sim, e1index, e2index));
                }
            }
        }
        for (EdgePair ep : hack)
            pq.add(ep);
        return pq;
    }

    /**
     * Calculates the similarity matrix for the edges.  The similarity matrix is
     * symmetric.  This method assumes that the
     * only similarities that matter are those that occur between two edges
     * that share a vertex.
     *
     * @param edgeIndices the list of all edges known to the system
     * @param sm a square matrix whose values denote edges between the rows.
     *
     * @return the similarity matrix
     */
    private <E extends Edge> Matrix calculateConnectionSimMatrix(
                         final Indexer<Edge> edgeIndices, 
                         final Graph<E> graph) {

        int numConnections = edgeIndices.size();
        final int numVertices = graph.order();
        final Matrix connectionSimMatrix = 
            new SparseSymmetricMatrix(
                new SparseHashMatrix(numConnections, numConnections));

        // Create an array of locks for fine-grained locking of the similarity
        // matrix as the connection-pair similarities are being updated
        final Object[] pairLocks = new Object[edgeIndices.size()];
        for (int i = 0; i < pairLocks.length; ++i)
            pairLocks[i] = new Object();

        // For each of the vertices, get its neighbors and then compute the
        // similarity between the pair-wise combinations.  This assumes that the
        // only similarities that matter are those that occur between two edges
        // that share a vertex.

//         Object key = WORK_QUEUE.registerTaskGroup(numVertices);
        for (int vertex : graph.vertices()) {
            final int v1 = vertex;
            //System.out.println("Computing similarities for " + v1);
            //System.out.println("Neighbors for: " + v1);
//              WORK_QUEUE.add(key, new Runnable() {
//                      public void run() {
                        for (Edge e1 : graph.getAdjacencyList(v1)) {
                            int e1index = edgeIndices.find(e1);
                            if (e1index < 0)
                                throw new IllegalStateException("Umapped pair: " + e1);

                            int v2 = (e1.to() == v1) ? e1.from() : e1.to();

                            for (Edge e2 : graph.getAdjacencyList(v1)) {
                                if (e1.equals(e2))
                                    break;

                                int v3 = (e2.to() == v1) ? e2.from() : e2.to();

                                int e2index = edgeIndices.find(e2);                                
                                
                                double sim = 
                                    getConnectionSimilarity(graph, v1, v2, v3);
                                //System.out.printf("Computing similarity of %s, %s: %f%n", e1, e2, sim);
                                
                                // Determine the row and column for the
                                // connections in the similarity matrix.  Note
                                // that the symmetric matrix will update the row
                                // > col, case.  To ensure that we can update
                                // the rows in parallel, we figure out which row
                                // will be updated and then lock internall on
                                // that value.
                                int row = Math.max(e1index, e2index);
                                int col = Math.min(e1index, e2index);
                                
                                //System.out.printf("%s:%d + %s:%d -> %f%n", p1, p1index, p2, p2index, sim);

//                                  synchronized(pairLocks[row]) {
                                     // The symmetric matrix handles the (col,
                                     // row) case
                                connectionSimMatrix.set(row, col, sim);
                            }
                        }
        }
//     }
//                  });            
//          }
//         WORK_QUEUE.await(key);
        return connectionSimMatrix;
    }

    /**
     * Converts a series of merges to edge cluster assignments.  Cluster
     * assignments are assumed to start at 0.
     *
     * @param merges the merge steps, in order
     * @param numOriginalClusters how many clusters are present prior to
     *        merging.  This is typically the number of rows in the matrix being
     *        clustered
     *
     * @returns a mapping from a cluster to all the elements contained within it.
     */
    private static MultiMap<Integer,Integer> computeEdgePartitions(
            List<Merge> merges, int numOriginalClusters) {

        MultiMap<Integer,Integer> clusterToElements = 
            new HashMultiMap<Integer,Integer>();
        for (int i = 0; i < numOriginalClusters; ++i)
            clusterToElements.put(i, i);

        for (Merge m : merges) {
            clusterToElements.putMany(m.remainingCluster(), 
                                      clusterToElements.remove(m.mergedCluster()));
        }           

        return clusterToElements;
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
        
        Set<Integer> n1 = graph.getNeighbors(impost1);
        Set<Integer> n2 = graph.getNeighbors(impost2);

        // REMINDER: swap based on size
        int inCommon = 0;
        for (int v : n1) 
            if (n2.contains(v))
                inCommon++;

        if (n2.contains(impost1)) 
            inCommon++;
        if (n1.contains(impost2)) 
            inCommon++;
        
        return (double)inCommon / (n1.size() + n2.size() + 2 - inCommon);
    }

    /**
     * Returns the partition density of the clustering solution.
     *
     * @throws IllegalArgumentException if the solution number is outside the
     *         range of valid solutions
     * @throws IllegalStateException if the graph clustering has not yet
     *         finished
     */
    public double getSolutionDensity(int solutionNum) {
        if (solutionNum < 0 || solutionNum >= mergeOrder.size()) {
            throw new IllegalArgumentException(
                "not a valid solution: " + solutionNum);
        }      
        if (mergeOrder == null || edgeIndices == null) {
            throw new IllegalStateException(
                "initial clustering solution is not valid yet");
        }
        return partitionDensities[solutionNum];
    }

    /**
     * Returns the clustering solution after the specified number of merge
     * steps.
     *
     * @param solutionNum the number of merge steps to take prior to returning
     *        the clustering solution.
     *
     * @throws IllegalArgumentException if {@code solutionNum} is less than 0 or
     *         is greater than or equal to {@link #numberOfSolutions()}.
     * @throws IllegalStateException if this instance has not yet finished a
     *         clustering solution.
     */
    public Assignment[] getSolution(int solutionNum) {
        if (solutionNum < 0 || solutionNum >= mergeOrder.size()) {
            throw new IllegalArgumentException(
                "not a valid solution: " + solutionNum);
        }      
        if (mergeOrder == null || edgeIndices == null) {
            throw new IllegalStateException(
                "initial clustering solution is not valid yet");
        }

        int numConnections = edgeIndices.size();

        // Select the solution and all merges necessary to solve it
        MultiMap<Integer,Integer> bestEdgeAssignment =
            convertMergesToAssignments(
                mergeOrder.subList(0, solutionNum), numConnections);

        List<Set<Integer>> nodeClusters = new ArrayList<Set<Integer>>(numVertices);
        for (int i = 0; i < numVertices; ++i) 
            nodeClusters.add(new HashSet<Integer>());
        
        // Ignore the original partition labeling, and use our own cluster
        // labeling to ensure that the IDs are contiguous.
        int clusterId = 0;

        // For each of the partitions, add the partion's cluster ID to all the
        // nodes that are connected by one of the partition's edges
        for (Integer cluster : bestEdgeAssignment.keySet()) {
            Set<Integer> linkPartition = bestEdgeAssignment.get(cluster);
            for (Integer linkIndex : linkPartition) {
                Edge e = edgeIndices.lookup(linkIndex);
                nodeClusters.get(e.from()).add(clusterId);
                nodeClusters.get(e.to()).add(clusterId);
            }
            // Update the cluster id
            clusterId++;
        }

        Assignment[] nodeAssignments = new Assignment[numVertices];
        for (int i = 0; i < nodeAssignments.length; ++i) {
            nodeAssignments[i] = 
                new SoftAssignment(nodeClusters.get(i));
        }
        return nodeAssignments;        
    }

    /**
     * Returns the number of clustering solutions found by this instances for
     * the prior clustering run.
     *
     * @returns the number of solutions, or {@code 0} if no solutions are
     *          available.
     */
    public int numberOfSolutions() {
        return (mergeOrder == null) ? 0 : mergeOrder.size();
    }

    /**
     * Converts a series of merges to cluster assignments.  Cluster assignments
     * are assumed to start at 0.
     *
     * @param merges the merge steps, in order
     * @param numOriginalClusters how many clusters are present prior to
     *        merging.  This is typically the number of rows in the matrix being
     *        clustered
     *
     * @returns a mapping from a cluster to all the elements contained within it.
     */
    private static MultiMap<Integer,Integer> convertMergesToAssignments(
            List<Merge> merges, int numOriginalClusters) {

        MultiMap<Integer,Integer> clusterToElements = 
            new HashMultiMap<Integer,Integer>();
        for (int i = 0; i < numOriginalClusters; ++i)
            clusterToElements.put(i, i);

        for (Merge m : merges) {
            clusterToElements.putMany(m.remainingCluster(), 
                clusterToElements.remove(m.mergedCluster()));
        }           

        return clusterToElements;
    }

//     /**
//      * A utility class that represents the edge similarity matrix, where the
//      * similarity values are lazily computed on demand, rather than stored
//      * internally.  While computationally more expensive, this class provides an
//      * enormous benefit for clustering a graph where the similarity matrix
//      * cannot fit into memory.
//      */
//     private class LazySimilarityMatrix extends AbstractMatrix {

//         private final List<Edge> edgeList;

//         private final SparseMatrix sm;

//         public LazySimilarityMatrix(List<Edge> edgeList, SparseMatrix sm) {
//             this.edgeList = edgeList;
//             this.sm = sm;
//         }
        
//         public int columns() {
//             return edgeList.size();
//         }

//         public double get(int row, int column) {
//             Edge e1 = edgeList.get(row);
//             Edge e2 = edgeList.get(column);
            
//             double sim = getEdgeSimilarity(sm, e1, e2);
//             return sim;
//         }
        
//         public DoubleVector getRowVector(int row) {
//             int cols = columns();
//             DoubleVector vec = new DenseVector(cols);
//             for (int c = 0; c < cols; ++c) {
//                 vec.set(c, get(row, c));
//             }
//             return vec;
//         }

//         public int rows() {
//             return edgeList.size();
//         }

//         public void set(int row, int columns, double val) {
//             throw new UnsupportedOperationException();
//         }
//     }
  

}