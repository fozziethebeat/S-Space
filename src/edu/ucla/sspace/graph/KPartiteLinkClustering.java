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
public class KPartiteLinkClustering extends LinkClustering 
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A prefix for specifying properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.KPartiteLinkClustering";

    private int numPartitions;

    private Map<Integer,Integer> vertexToPartition;

    public KPartiteLinkClustering() {
        
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
    @Override public <E extends Edge> Assignment[] cluster(
                                Graph<E> graph, Properties props) {
        throw new Error();
    }

    public <E extends Edge> Assignment[] cluster(
                      final Graph<E> graph, Properties props, 
                      Map<Integer,Integer> vertexToPartition) {
        // Save the partitioning scheme for use in computing the partition density
        this.vertexToPartition = vertexToPartition;
        // Count how many partitions the user has provided
        numPartitions = new OpenIntSet(vertexToPartition.values()).size();
        // Then custer the result.  The base class will call our overrides,
        // which should correctly take into account the k-partiteness of the
        // graph
        return super.cluster(graph, props);
    }

    /**
     * Computes the density of the provided partition of edges
     * 
     * @param graph the graph
     * @param linkPartition the set of edges contained in the partition
     * @param edgeIndices a mapping from {@link Edge} instances to their numeric
     *        index
     */
    @Override protected double computeDensity(Set<Integer> linkPartition,
                                              Indexer<Edge> edgeIndices) {

        // Special case when the number of nodes is 2, which has a density of 0    
        int numEdges = linkPartition.size();
        if (numEdges == 1)
            return 0;

        // Determine how many nodes and edges are actually in this partition as
        // well as how many edges of each type.
        Set<Integer> nodesInPartition = new HashSet<Integer>();

        // Since the graph is k-partite, keep track of how many nodes showed up
        // in each of the partitions (note that these partitoins are different
        // from the "partition" for which the density is being calculated);
        Counter<Integer> partitionCounts = new ObjectCounter<Integer>();

        for (Integer edgeIndex : linkPartition) {
            Edge e = edgeIndices.lookup(edgeIndex);
            nodesInPartition.add(e.from());
            nodesInPartition.add(e.to());
            partitionCounts.count(vertexToPartition.get(e.from()));
            partitionCounts.count(vertexToPartition.get(e.to()));
        }

        int numNodes = nodesInPartition.size();
        assert numNodes > 2 : "self-edge in graph";

        // Not sure how to describe this quantity, so naming it 'x'...
        int x = 0;
        for (Map.Entry<Integer,Integer> e : partitionCounts) {
            int count = e.getKey();
            // n^k_c * sum_{k' != k}( n^k'_c ), essentially the number of nodes
            // with one type multiple by the number of nodes with a different
            // type
            x += count * (numNodes - count);
        }

        return numEdges * (numEdges + 1 - numNodes) 
            / (x - 2 * (numNodes - 1));
    }

    /**
     * Computes the similarity of the two edges as the Jaccard index of the
     * neighbors of two impost nodes (which exlcudes the impost nodes
     * themselves).
     *
     * @param sm a matrix containing the connections between edges.  A non-zero
     *        value in location (i,j) indicates a node <i>i</i> is connected to
     *        node <i>j</i> by an edge.
     * @param e1 an edge to be compared with {@code e2}
     * @param e2 an edge to be compared with {@code e1}
     *
     * @return the similarity of the edges.
     */
    @Override protected <E extends Edge> double getConnectionSimilarity(
            Graph<E> graph, int keystone, int impost1, int impost2) {
        
        Set<Integer> n1 = graph.getNeighbors(impost1);
        Set<Integer> n2 = graph.getNeighbors(impost2);

        // REMINDER: swap based on size
        int inCommon = 0;
        for (int v : n1) 
            if (n2.contains(v))
                inCommon++;        

        return (double)inCommon / ((n1.size() + n2.size()) - inCommon);
    }

}