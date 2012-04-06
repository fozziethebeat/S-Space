/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

import edu.ucla.sspace.similarity.CosineSimilarity;
import edu.ucla.sspace.similarity.SimilarityFunction;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SymmetricMatrix;
import edu.ucla.sspace.vector.DoubleVector;

import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * A Nearest Neighbor Chain Agglomerative Clustering implementation.  This
 * approach builds large chains starting from a random node of nearest
 * neighbors.  As soon as a recirocal nearest neighbor (RNN) is found, the pair
 * is merged.  This approach find an exact solution and runs in O(n^2) time.  It
 * also uses O(n^2) space to represent an adjacency matrix based on a similar
 * function.
 *
 * </p>
 *
 * Currently, this algorithm only implements the UGPMA agglomerative criteria.
 * However, it does use any symmetric {@link SimilarityFunction}.
 *
 * </p>
 *
 * This implementation is based on the following paper:
 *
 *   <li style="font-family:Garamond, Georgia, serif">Murtagh, Fionn and
 *   Contreras, PedroM. Algorithms for hierarchical clustering: an overview.  In
 *   <i>Wiley Interdisciplinary Reviews: Data Mining and Knowledge Discovery</i>
 *   Available <a
 *   href="http://onlinelibrary.wiley.com/doi/10.1002/widm.53/full">here</a>
 *   </li>
 *
 * @author Keith Stevens
 */
public class NeighborChainAgglomerativeClustering implements Clustering {

    /**
     * The similarity method used when comparing and merging compelte clusters.
     * See <a
     * href="http://home.dei.polimi.it/matteucc/Clustering/tutorial_html/hierarchical.html">this
     * guide</a> for examples of how each link method operates.
     */
    public enum ClusterLink {
        /**
         * Similarity will be based on the most similar item pair connecting
         * two clusters.
         */
        SINGLE_LINK,

        /**
         * Similarity will be based on most dissimilar item pair connecting 
         * two clusters.
         */
        COMPLETE_LINK,

        /**
         * Similarity will be the average similarity of all item pairs
         * connecting two clusters.
         */
        MEAN_LINK,

        /**
         * Similarity will be based on median points for each cluster.
         */
        MEDIAN_LINK,

        /**
         * Similarity will be the unweighted average similarity of all item
         * pairs connecting two clusters.
         */
        MCQUITTY_LINK,
    }

    private final ClusterLink method;

    private final SimilarityFunction simFunc;
    
    /**
     * Creates a {@link NeighborChainAgglomerativeClustering} with no minimum
     * similarity threshold.  When using this constructor, you must specifcy the
     * number of clusters or the results will be undefined.
     */
    public NeighborChainAgglomerativeClustering() {
        this(ClusterLink.MEAN_LINK, new CosineSimilarity());
    }

    /**
     * Creates a {@link NeighborChainAgglomerativeClustering} with the specified
     * minimum similarity threshold.  When clustering without specifying the
     * number of clusters, clustering stops when the most similar clusters have
     * a similarity below this threshold.
     */
    public NeighborChainAgglomerativeClustering(ClusterLink method,
                                                SimilarityFunction simFunc) {
        if (!simFunc.isSymmetric())
            throw new IllegalArgumentException(
                    "Agglomerative Clustering requires a symmetric " +
                    "similarity function");

        this.method = method;
        this.simFunc = simFunc;
    }

    /**
     * Unsupported 
     *
     * @throws UnsupportedOperationException
     */
    public Assignments cluster(Matrix m, Properties props) {
        throw new UnsupportedOperationException(
                "Cannot cluster without specifying the number of clusters.");
    }

    /**
     * Returns the agglomerative clustering result using {@link ClusterLink} and
     * {@code SimilarityFunction} specified from the constructor.  The {@link
     * SimilarityFunction} will be used to build a symmetric adjacency matrix
     * and the {@link ClusterLink} will determine how to udpate similarities
     * between newly nerged clusters.
     */
    public Assignments cluster(Matrix m, int numClusters, Properties props) {
        Matrix adj = new SymmetricMatrix(m.rows(), m.rows());
        for (int r = 0; r < m.rows(); ++r) {
            DoubleVector v = m.getRowVector(r);
            for (int c = r+1; c < m.rows(); ++c)
                adj.set(r,c, simFunc.sim(v, m.getRowVector(c)));
        }
        return clusterAdjacencyMatrix(adj, method, numClusters);
    }

    /**
     * Clusters the points represented as an adjacency matrix in {@code adj}
     * using the supplied {@code ClusterLink} method into {@code numCluster}. 
     *
     * @param adj A symmetric {@link Matrix} recording similarities between
     *        nodes in a graph.
     * @param method {@link ClusterLink} method that determines how similarities
     *        between clusters will be updated.
     * @param numClusters The desired number of clusters.
     *
     * @return {@link Assignments} from each data point to it's assigned
     *         cluster.
     */
    public static Assignments clusterAdjacencyMatrix(Matrix adj,
                                                     ClusterLink method,
                                                     int numClusters) {
        // A mapping from cluster id's to their point sets.
        Map<Integer, Set<Integer>> clusterMap = 
            new HashMap<Integer, Set<Integer>>();

        // A set of clusters to be considered for merging.
        Set<Integer> remaining = new HashSet<Integer>();

        // Create a cluster for every data point and add it to the cluster map
        // and to the examine set.
        for (int r = 0; r < adj.rows(); ++r) {
            remaining.add(r);
            Set<Integer> points = new HashSet<Integer>();
            points.add(r);
            clusterMap.put(r, points);
        }

        // A stack of the nearest neighbor chains.  The tuple stores the id of the
        // current node and the similarity from this node to it's parent in the
        // chain.
        Deque<Link> chain = new LinkedList<Link>();

        // Initializes the chain.
        initializeChain(chain, remaining);

        while (clusterMap.size() > numClusters) {
            // Get the last link in the chain.
            Link top = chain.peek();

            // Find the nearest neighbor using the clusters not in the chain
            // already.
            Link best = findBest(remaining, adj, top.x);

            // Check the similarity for the best neighbor and compare it to that of
            // the current node in the chain.  If the neighbor sim is larger, then
            // the current node and it's parent aren't RNNs.  Otherwise, the current
            // node is RNNs with it's parent.
            if (best.sim > top.sim) {
                // Not RNNs.  So push the best node and it's similarity to the
                // current node onto the chain and remove it from the examine set.
                chain.push(best);
                remaining.remove(best.x);
            } else {
                // Yes top two on stack are RNNs.
                
                // Pop the current node from the top. 
                chain.pop();

                // Pop the parent of the best node.
                Link parent = chain.pop();

                // Get the current candidates and remove them from the cluster
                // map.
                Set<Integer> c1Points = clusterMap.get(top.x);
                int c1Size = c1Points.size();
                Set<Integer> c2Points = clusterMap.get(parent.x);
                int c2Size = c2Points.size();
                clusterMap.remove(top.x);
                clusterMap.remove(parent.x);

                // Update the distance from the parent id to all other elements
                // in the cluster map.
                for (int o : clusterMap.keySet())
                    adj.set(parent.x, o, 
                            updateSimilarity(method, adj, top.x, c1Size, parent.x, c2Size, 0));

                // Merge the set assignments for the two clusters.
                c1Points.addAll(c2Points);
                // Replace the mapping from parent to now point to the merged cluster.
                clusterMap.put(parent.x, c1Points);

                // Add the id for the new cluster (now using parent's id) into
                // the remaining set.
                remaining.add(parent.x);

                // If the chain is now empty, re-initialize it.
                if (chain.size() == 0)
                    initializeChain(chain, remaining);
            }
        }

        return formAssignments(clusterMap.values(), adj.rows());
    }

    /**
     * Returns the updated similarity between a newly formed cluster containing
     * clusters {@code c1Index} and {@code c2Index} to the cluster {@code
     * otherIndex}.  These similarity updates use the Lance-Williams recurrence
     * relations which hold for both similarity metrics and distance metrics.
     */
    private static double updateSimilarity(ClusterLink method,
                                           Matrix adj,
                                           int c1Index,
                                           int c1Size,
                                           int c2Index,
                                           int c2Size,
                                           int otherIndex) {
        double s1 = adj.get(c1Index, otherIndex);
        double s2 = adj.get(c2Index, otherIndex);
        switch (method) {
            case MEAN_LINK:
                return (c1Size*s1 + c2Size*s2) / (c1Size + c2Size);
            case MEDIAN_LINK:
                return .5*s1 + .5 * s2 - .25*adj.get(c1Index, c2Index);
            case SINGLE_LINK:
                return .5*s1 + .5*s2 + Math.min(s1, s2);
            case COMPLETE_LINK:
                return .5*s1 + .5*s2 + Math.max(s1, s2);
            case MCQUITTY_LINK:
                return .5 * s1 + .5*s2;
            default: throw new IllegalArgumentException(
                             "Unsupported ClusterLink Method");
        }
    }

    /**
     * Initializes the neighbor {@code chain} with the first element in {@code
     * remaining} and removes that elment from the set.
     */
    private static void initializeChain(Deque<Link> chain, 
                                        Set<Integer> remaining) {
        Iterator<Integer> iter = remaining.iterator();
        chain.push(new Link(-Double.MAX_VALUE, iter.next()));
        iter.remove();
    }

    /**
     * Returns a {@link Link} representing the nearest cluster in {@code
     * remaining} to {@code cluster} and the similarity between the two clusters
     * as defined in {@code adj}.
     */
    private static Link findBest(Set<Integer> remaining,
                                 Matrix adj,
                                 int cluster) {
        int best = -1;
        double sim = -Double.MAX_VALUE; 
        for (int i : remaining) {
            double s = adj.get(cluster, i);
            if (s > sim) {
                best = i;
                sim = s;
            }
        }

        return new Link(sim, best);
    }

    /**
     * Returns the {@link Assignments} corresponding to the set of clusters and
     * number of data points.
     */
    public static Assignments formAssignments(Collection<Set<Integer>> clusters,
                                              int numPoints) {
        int cid = 0;
        Assignment[] assignments = new Assignment[numPoints];
        for (Set<Integer> cluster : clusters) {
            for (int point : cluster)
                assignments[point] = new HardAssignment(cid);
            cid++;
        }
        return new Assignments(clusters.size(), assignments, null);
    }

    /**
     * A simple struct for storing links between two clusters.
     */
    public static class Link {

        /**
         * The similarity between the current cluster and it's parent in the
         * chain.
         */
        public double sim;

        /**
         * The id of the current cluster.
         */
        public int x;

        /**
         * Creates a new {@link Link}.
         */
        public Link(double sim, int x) {
            this.sim = sim;
            this.x = x;
        }
    }
}
