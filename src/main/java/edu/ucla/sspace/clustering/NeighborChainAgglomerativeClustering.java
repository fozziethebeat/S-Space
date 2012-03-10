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

    private final double minSim;

    private final SimilarityFunction simFunc;
    
    /**
     * Creates a {@link NeighborChainAgglomerativeClustering} with no minimum
     * similarity threshold.  When using this constructor, you must specifcy the
     * number of clusters or the results will be undefined.
     */
    public NeighborChainAgglomerativeClustering() {
        this(-Double.MAX_VALUE, new CosineSimilarity());
    }

    /**
     * Creates a {@link NeighborChainAgglomerativeClustering} with the specified
     * minimum similarity threshold.  When clustering without specifying the
     * number of clusters, clustering stops when the most similar clusters have
     * a similarity below this threshold.
     */
    public NeighborChainAgglomerativeClustering(double minSim,
                                                SimilarityFunction simFunc) {
        if (!simFunc.isSymmetric())
            throw new IllegalArgumentException(
                    "Agglomerative Clustering requires a symmetric " +
                    "similarity function");

        this.minSim = minSim;
        this.simFunc = simFunc;
    }

    /**
     * Returns the agglomerative clustering result using the mean link criteria
     * and the cosine similarity metric.  Clustering stops when the {@link
     * #minSim} similarity threshold is larger than the two most similar
     * clusters found.  This threshold should be set by the constructor.
     */
    public Assignments cluster(Matrix m, Properties props) {
        return cluster(m, 0, props);
    }

    /**
     * Returns the agglomerative clustering result using the mean link criteria
     * and the cosine similarity metric for the specified number of clusters.
     */
    public Assignments cluster(Matrix m, int numClusters, Properties props) {
        Matrix adj = new SymmetricMatrix(m.rows(), m.rows());
        for (int r = 0; r < m.rows(); ++r) {
            DoubleVector v = m.getRowVector(r);
            for (int c = r+1; c < m.rows(); ++c)
                adj.set(r,c, simFunc.sim(v, m.getRowVector(c)));
        }
        return clusterAdjacencyMatrix(adj, simFunc, numClusters);
    }

    public static Assignments clusterAdjacencyMatrix(Matrix adj,
                                                     SimilarityFunction simFunc,
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
                for (int o : clusterMap.keySet()) {
                    double d1 = adj.get(top.x, o);
                    double d2 = adj.get(parent.x, o);
                    double newDist = (c1Size*d1 + c2Size*d2) / 
                                     (c1Size + c2Size);
                    adj.set(parent.x, o, newDist);
                }

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

    private static void initializeChain(Deque<Link> chain, 
                                        Set<Integer> remaining) {
        Iterator<Integer> iter = remaining.iterator();
        chain.push(new Link(-Double.MAX_VALUE, iter.next()));
        iter.remove();
    }

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
        int[] assignments = new int[numPoints];
        for (Set<Integer> cluster : clusters) {
            for (int point : cluster)
                assignments[point] = cid;
            cid++;
        }
        return new Assignments(clusters.size(), assignments, null);
    }

    /**
     * A simple struct for storing and comparing links between two clusters.
     * Links with a higher similarity should be given higher priority.
     */
    public static class Link implements Comparable<Link> {
        /**
         * The similarity between the current cluster and it's parent in the
         * chain.
         */
        public double sim;

        /**
         * The id of the current cluster.
         */
        public int x;

        public Link(double sim, int x) {
            this.sim = sim;
            this.x = x;
        }

        public int compareTo(Link o) {
            double diff = this.sim - o.sim;
            if (diff < 0)
                return -1;
            return (diff == 0d) ? 0 : 1;
        }
    }
}
