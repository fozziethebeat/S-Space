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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;

import edu.ucla.sspace.matrix.AbstractMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseHashMatrix;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.SparseSymmetricMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;

import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.WorkQueue;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

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
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import java.util.logging.Level;
import java.util.logging.Logger;

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
public class LinkClustering implements Clustering, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A prefix for specifying properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.LinkClustering";

    /**
     * The property to specify if the edge similarity matrix should be kept in
     * memory during clustering, or if its values should be computed on the fly.
     */
    public static final String KEEP_SIMILARITY_MATRIX_IN_MEMORY_PROPERTY =
        PROPERTY_PREFIX + ".keepSimilarityMatrixInMemory";
    
    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(LinkClustering.class.getName());

    /**
     * The work used by all {@code LinkClustering} instances to perform
     * multi-threaded operations.
     */
    private static final WorkQueue WORK_QUEUE = new WorkQueue();
    
    /**
     * The merges for the prior run of this clustering algorithm
     */
    private List<Merge> mergeOrder;

    /**
     * The list of edges that were last merged.  This list is maintained in the
     * same order as the initial cluster ordering.
     */
    private List<Edge> edgeList;

    /**
     * The number of rows in the input matrix that was last clustered.
     */
    private int numRows;

    /**
     * Instantiates a new {@code LinkClustering} instance.
     */
    public LinkClustering() { 
        mergeOrder = null;
        edgeList = null;
        numRows = 0;
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
    public Assignments cluster(Matrix matrix, 
                               int numClusters,
                               Properties props) {
        LOGGER.warning("Link clustering does not take a specified number of " +
                       "clusters.  Clustering the matrix anyway.");
        return cluster(matrix, props);
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code matrix} is not square, or is
     *         not an instance of {@link SparseMatrix}
     */
    public Assignments cluster(Matrix matrix, Properties props) { 
        if (matrix.rows() != matrix.columns()) 
            throw new IllegalArgumentException("Input matrix is not square. " +
                "Matrix is expected to be a square matrix whose values (i,j) " +
                "denote an edge from row i to row j");
        if (!(matrix instanceof SparseMatrix)) {
            throw new IllegalArgumentException("Input matrix must be a " +
                "sparse matrix.");
        }
        SparseMatrix sm = (SparseMatrix)matrix;

        String inMemProp =
            props.getProperty(KEEP_SIMILARITY_MATRIX_IN_MEMORY_PROPERTY);
        boolean keepSimMatrixInMem = (inMemProp != null) 
            ? Boolean.parseBoolean(inMemProp) : true;

        // IMPLEMENTATION NOTE: Ahn et al. used single-linkage HAC, which can be
        // efficiently implemented in O(n^2) time as a special case of HAC.
        // However, we currently don't optimize for this special case and
        // instead use our HAC class.  Because of the complexity of the edge
        // similarity function, we build our own similarity matrix and then pass
        // it in, rather than passing in the edge matrix directly.

        final int rows = sm.rows();
        numRows = rows;
        LOGGER.fine("Generating link similarity matrix for " + rows + " nodes");

        //  Rather than create an O(row^3) matrix for representing the edges,
        // compress the edge matrix by getting a mapping for each edge to a row
        // in the new matrix.
        final List<Edge> edgeList = new ArrayList<Edge>();
        this.edgeList = edgeList;

        for (int r = 0; r < rows; ++r) {
            SparseDoubleVector row = sm.getRowVector(r);
            int[] edges = row.getNonZeroIndices();
            for (int col : edges) {
                // Always add edges from the upper triangular
                if (r > col)
                    edgeList.add(new Edge(r, col));
                // Otherwise, we only add the edge from the lower triangular if
                // it wasn't present in the upper.  This avoids counting
                // duplicate edges.
                else if (r < col && sm.get(col, r) == 0)
                    edgeList.add(new Edge(r, col));
            }
        }

        final int numEdges = edgeList.size();
        LOGGER.fine("Number of edges to cluster: " + numEdges);
        
        Matrix edgeSimMatrix = 
            getEdgeSimMatrix(edgeList, sm, keepSimMatrixInMem);
        
        LOGGER.fine("Computing single linkage link clustering");

        final List<Merge> mergeOrder = 
            new HierarchicalAgglomerativeClustering().
                buildDendrogram(edgeSimMatrix, ClusterLinkage.SINGLE_LINKAGE);
        this.mergeOrder = mergeOrder;

        LOGGER.fine("Calculating partition densitities");

        // Set up a concurrent map that each thread will update once it has
        // calculated the densitites of each of its partitions.  This map is
        // only written to once per thread.
        final ConcurrentNavigableMap<Double,Integer> partitionDensities 
            = new ConcurrentSkipListMap<Double,Integer>();
        
        // Register a task group for calculating all of the partition
        // densitities
        Object key = WORK_QUEUE.registerTaskGroup(mergeOrder.size());
        for (int p = 0; p < mergeOrder.size(); ++p) {
            final int part = p;
            WORK_QUEUE.add(key, new Runnable() {
                    public void run() {
                        // Get the merges for this particular partitioning of
                        // the links
                        List<Merge> mergeSteps = mergeOrder.subList(0, part);
                        
                        // Convert the merges to a specific cluster labeling
                        MultiMap<Integer,Integer> clusterToElements = 
                            convertMergesToAssignments(mergeSteps, numEdges);

                        // Based on the link partitioning, calculate the
                        // partition density for each cluster
                        double partitionDensitySum = 0d;
                        for (Integer cluster : clusterToElements.keySet()) {
                            Set<Integer> linkPartition = 
                                clusterToElements.get(cluster);
                            int numLinks = linkPartition.size();
                            BitSet nodesInPartition = new BitSet(rows);
                            for (Integer linkIndex : linkPartition) {
                                Edge link = edgeList.get(linkIndex);
                                nodesInPartition.set(link.from);
                                nodesInPartition.set(link.to);
                            }
                            int numNodes = nodesInPartition.cardinality();
                            // This reflects the density of this particular
                            // cluster
                            double partitionDensity =
                                (numLinks - (numNodes - 1d))
                                / (((numNodes * (numNodes - 1d)) / 2d)
                                   - (numLinks - 1));                
                            partitionDensitySum += partitionDensity;
                        }
                        // Compute the density for the total partitioning
                        // solution
                        double partitionDensity = 
                            (2d / numEdges) * partitionDensitySum;
                        LOGGER.log(Level.FINER, "Partition solution {0} had "
                                   + "density {1}",
                                   new Object[] { part, partitionDensity });
                        
                        // Update the thread-shared partition density map with
                        // this task's calculation
                        partitionDensities.put(partitionDensity, part);
                        
                    }
                });
        }

        // Wait for all the partition densities to be calculated
        WORK_QUEUE.await(key);


        Map.Entry<Double,Integer> densest = partitionDensities.lastEntry();
        LOGGER.fine("Partition " + densest.getValue() + 
                    " had the highest density: " + densest.getKey());
        int partitionWithMaxDensity = densest.getValue();

        // Select the solution with the highest partition density and assign
        // nodes accordingly
        MultiMap<Integer,Integer> bestEdgeAssignment =
            convertMergesToAssignments(
                mergeOrder.subList(0, partitionWithMaxDensity), numEdges);

        List<Set<Integer>> nodeClusters = new ArrayList<Set<Integer>>(rows);
        for (int i = 0; i < rows; ++i) 
            nodeClusters.add(new HashSet<Integer>());
        
        // Ignore the original partition labeling, and use our own cluster
        // labeling to ensure that the IDs are contiguous.
        int clusterId = 0;

        // For each of the partitions, add the partion's cluster ID to all the
        // nodes that are connected by one of the partition's edges
        for (Integer cluster : bestEdgeAssignment.keySet()) {
            Set<Integer> edgePartition = bestEdgeAssignment.get(cluster);
            for (Integer edgeId : edgePartition) {
                Edge e = edgeList.get(edgeId);
                nodeClusters.get(e.from).add(clusterId);
                nodeClusters.get(e.to).add(clusterId);
            }
            // Update the cluster id
            clusterId++;
        }

        int numClusters = 0;
        Assignment[] nodeAssignments = new Assignment[rows];
        for (int i = 0; i < nodeAssignments.length; ++i) {
            nodeAssignments[i] = 
                new SoftAssignment(nodeClusters.get(i));
        }
        return new Assignments(numClusters, nodeAssignments, matrix);
    }

    /**
     * Returns the edge similarity matrix for the edges in the provided sparse
     * matrix.
     */
    private Matrix getEdgeSimMatrix(List<Edge> edgeList, SparseMatrix sm,
                                    boolean keepSimilarityMatrixInMemory) {
        return (keepSimilarityMatrixInMemory) 
            ? calculateEdgeSimMatrix(edgeList, sm)
            : new LazySimilarityMatrix(edgeList, sm);            
    }

    /**
     * Calculates the similarity matrix for the edges.  The similarity matrix is
     * symmetric.
     *
     * @param edgeList the list of all edges known to the system
     * @param sm a square matrix whose values denote edges between the rows.
     *
     * @return the similarity matrix
     */
    private Matrix calculateEdgeSimMatrix(
            final List<Edge> edgeList, final SparseMatrix sm) {

        final int numEdges = edgeList.size();
        final Matrix edgeSimMatrix = 
            new SparseSymmetricMatrix(
                new SparseHashMatrix(numEdges, numEdges));

        Object key = WORK_QUEUE.registerTaskGroup(numEdges);
        for (int i = 0; i < numEdges; ++i) {
            final int row = i;
            WORK_QUEUE.add(key, new Runnable() {
                    public void run() {
                        for (int j = row; j < numEdges; ++j) {
                            Edge e1 = edgeList.get(row);
                            Edge e2 = edgeList.get(j);
                            
                            double sim = getEdgeSimilarity(sm, e1, e2);
                
                            if (sim > 0) {
                                // The symmetric matrix handles the (j,i) case
                                edgeSimMatrix.set(row, j, sim);
                            }
                        }
                    }
                });            
        }
        WORK_QUEUE.await(key);
        return edgeSimMatrix;
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
            clusterToElements.putMulti(m.remainingCluster(), 
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
    protected double getEdgeSimilarity(SparseMatrix sm, Edge e1, Edge e2) {
        // Determing the keystone (shared) node by the edges and the other two
        // impost (unshared) nodes.
        int keystone = -1;
        int impost1 = -1;
        int impost2 = -1;
        if (e1.from == e2.from) {
            keystone = e1.from;
            impost1 = e1.to;
            impost2 = e2.to;
        }
        else if (e1.from == e2.to) {
            keystone = e1.from;
            impost1 = e1.to;
            impost2 = e2.from;
        }
        else if (e2.to == e1.from) {
            keystone = e1.from;
            impost1 = e1.to;
            impost2 = e2.from;
        }
        else if (e1.to == e2.to) {
            keystone = e1.to;
            impost1 = e1.from;
            impost2 = e2.from;
        }
        else
            return 0d;

        // Determine the overlap between the neighbors of the impost nodes
        int[] impost1edges = getImpostNeighbors(sm, impost1);
        int[] impost2edges = getImpostNeighbors(sm, impost2);
        double similarity = Similarity.jaccardIndex(impost1edges, impost2edges);
        return similarity;
    }

    /**
     * Returns an array containing the row indices of the neighbors of the
     * impost node and the row index of the impost node itself.
     */
    private static int[] getImpostNeighbors(SparseMatrix sm, int rowIndex) {
        int[] impost1edges = sm.getRowVector(rowIndex).getNonZeroIndices();
        int[] neighbors = Arrays.copyOf(impost1edges, impost1edges.length + 1);
        neighbors[neighbors.length - 1] = rowIndex;
        return neighbors;
    }

    /**
     * Returns the partition density of the clustering solution.
     */
    public double getSolutionDensity(int solutionNum) {
        if (solutionNum < 0 || solutionNum >= mergeOrder.size()) {
            throw new IllegalArgumentException(
                "not a valid solution: " + solutionNum);
        }      
        if (mergeOrder == null || edgeList == null) {
            throw new IllegalStateException(
                "initial clustering solution is not valid yet");
        }
        
        int numEdges = edgeList.size();

        // Get the merges for this particular partitioning of the links
        List<Merge> mergeSteps = 
            mergeOrder.subList(0, solutionNum);
        
        // Convert the merges to a specific cluster labeling
        MultiMap<Integer,Integer> clusterToElements = 
            convertMergesToAssignments(mergeSteps, numEdges);
        
        // Based on the link partitioning, calculate the node partition density
        double partitionDensitySum = 0d;
        for (Integer cluster : clusterToElements.keySet()) {
            Set<Integer> linkPartition = clusterToElements.get(cluster);
            int numLinks = linkPartition.size();
            BitSet nodesInPartition = new BitSet(numRows);
            for (Integer linkIndex : linkPartition) {
                Edge link = edgeList.get(linkIndex);
                nodesInPartition.set(link.from);
                nodesInPartition.set(link.to);
            }
            int numNodes = nodesInPartition.cardinality();
            // This reflects the density of this particular cluster within the
            // total partitioning
            double partitionDensity = (numLinks - (numNodes - 1d))
                / (((numNodes * (numNodes - 1d)) / 2d) - (numLinks - 1));                
            partitionDensitySum += partitionDensity;
        }
        // Compute the density for the total partitioning solution
        double partitionDensity =  (2d / numEdges) * partitionDensitySum;
        return partitionDensity;
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
    public Assignments getSolution(int solutionNum) {
        if (solutionNum < 0 || solutionNum >= mergeOrder.size()) {
            throw new IllegalArgumentException(
                "not a valid solution: " + solutionNum);
        }      
        if (mergeOrder == null || edgeList == null) {
            throw new IllegalStateException(
                "initial clustering solution is not valid yet");
        }

        int numEdges = edgeList.size();

        // Select the solution and all merges necessary to solve it
        MultiMap<Integer,Integer> bestEdgeAssignment =
            convertMergesToAssignments(
                mergeOrder.subList(0, solutionNum), numEdges);

        List<Set<Integer>> nodeClusters = new ArrayList<Set<Integer>>(numRows);
        for (int i = 0; i < numRows; ++i) 
            nodeClusters.add(new HashSet<Integer>());
        
        // Ignore the original partition labeling, and use our own cluster
        // labeling to ensure that the IDs are contiguous.
        int clusterId = 0;

        // For each of the partitions, add the partion's cluster ID to all the
        // nodes that are connected by one of the partition's edges
        for (Integer cluster : bestEdgeAssignment.keySet()) {
            Set<Integer> edgePartition = bestEdgeAssignment.get(cluster);
            for (Integer edgeId : edgePartition) {
                Edge e = edgeList.get(edgeId);
                nodeClusters.get(e.from).add(clusterId);
                nodeClusters.get(e.to).add(clusterId);
            }
            // Update the cluster id
            clusterId++;
        }

        Assignment[] nodeAssignments = new Assignment[numRows];
        for (int i = 0; i < nodeAssignments.length; ++i)
            nodeAssignments[i] = new SoftAssignment(nodeClusters.get(i));
        return new Assignments(clusterId, nodeAssignments);
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
     * A utility data structure for representing a directed edge between two
     * ordinally labeled nodes.
     */
    protected static class Edge {

        public final int from;

        public final int to;

        public Edge(int from, int to) {
            this.from = from;
            this.to = to;
        }

        public boolean equals(Object o) {
            if (o instanceof Edge) {
                Edge e = (Edge)o;
                return e.from == from && e.to == to;
            }
            return false;
        }

        public int hashCode() {
            return from ^ to;
        }

        public String toString() {
            return  "(" + from + "->" + to + ")";
        }
    }

    /**
     * A utility class that represents the edge similarity matrix, where the
     * similarity values are lazily computed on demand, rather than stored
     * internally.  While computationally more expensive, this class provides an
     * enormous benefit for clustering a graph where the similarity matrix
     * cannot fit into memory.
     */
    private class LazySimilarityMatrix extends AbstractMatrix {

        private final List<Edge> edgeList;

        private final SparseMatrix sm;

        public LazySimilarityMatrix(List<Edge> edgeList, SparseMatrix sm) {
            this.edgeList = edgeList;
            this.sm = sm;
        }
        
        public int columns() {
            return edgeList.size();
        }

        public double get(int row, int column) {
            Edge e1 = edgeList.get(row);
            Edge e2 = edgeList.get(column);
            
            double sim = getEdgeSimilarity(sm, e1, e2);
            return sim;
        }
        
        public DoubleVector getRowVector(int row) {
            int cols = columns();
            DoubleVector vec = new DenseVector(cols);
            for (int c = 0; c < cols; ++c) {
                vec.set(c, get(row, c));
            }
            return vec;
        }

        public int rows() {
            return edgeList.size();
        }

        public void set(int row, int columns, double val) {
            throw new UnsupportedOperationException();
        }
    }
}
