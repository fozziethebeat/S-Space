package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;


/**
 * A simple O(N^2 log N) implementation of Agglomerative Clustering using the
 * Mean link criteria and the cosine similarity measure.  We acheive O(N^2 log
 * n) by using a priority heap to order the possible clusters to be merged.
 *
 * @author Keith Stevens
 */
public class AverageLinkAgglomerativeClustering implements Clustering {

    private final double minSim;
    
    /**
     * Creates a {@link AverageLinkAgglomerativeClustering} with no minimum
     * similarity threshold.  When using this constructor, you must specifcy the
     * number of clusters or the results will be undefined.
     */
    public AverageLinkAgglomerativeClustering() {
        this(-Double.MAX_VALUE);
    }

    /**
     * Creates a {@link AverageLinkAgglomerativeClustering} with the specified
     * minimum similarity threshold.  When clustering without specifying the
     * number of clusters, clustering stops when the most similar clusters have
     * a similarity below this threshold.
     */
    public AverageLinkAgglomerativeClustering(double minSim) {
        this.minSim = minSim;
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
        // Create a counter for cluster identifiers.
        int nextClusterId = m.rows();

        // Store the possible clusters mergings according to their similarity
        // value in a priority queue.  The pairing with the highest similarity
        // will be returned first.
        Queue<Link> linkPriority = new PriorityQueue<Link>(
                m.rows() * m.rows() / 2);

        // Store a mapping from cluster ids to their composite vector and assigned
        // data points.  
        Map<Integer, Candidate> clusters = new HashMap<Integer, Candidate>();

        // Compute the pairwise similarity between every data point.  This operation
        // is fundamentally N^2 in complexity.  Right now, assume that the
        // similarity metric is symmetric and only store half of the similarity
        // values.
        for (int r = 0; r < m.rows(); ++r) {
            DoubleVector vec = m.getRowVector(r);
            // Create a cluster for each data point.
            clusters.put(r, new Candidate(vec, r));
            // Add the link similarities between each data point.
            for (int c = r+1; c < m.rows(); ++c)
                linkPriority.offer(new Link(
                            Similarity.cosineSimilarity(vec, m.getRowVector(c)),
                            r, c));
        }

        while (clusters.size() > numClusters) {
            // Get the best link in the queue that connects two existing
            // clusters.  Other links can be ignored since one of the clusters
            // has been merged already.  This loop has complexity N^2, but most
            // of those entries will be discareded.  Only N entries will be
            // retained and passed along for further processing (which itself is
            // N log N in complexity).
            Link link = null;
            do {
                link = linkPriority.poll();
            } while (link != null && 
                     !(clusters.containsKey(link.x) && 
                       clusters.containsKey(link.y)));

            // If we run out of links to consider or we've found a link that is
            // less similar than our threshold, return the final assignments.
            if (link == null || link.sim < minSim)
                return formAssignments(clusters.values(), m.rows());

            // Get the new cluster id and update the nextClusterId.
            int newId = nextClusterId;
            nextClusterId++;

            // Get the new candidate cluster by merging together the two nearest
            // clusters.  Then remove those clusters from the cluster map.
            Candidate newCandidate = clusters.get(link.x).merge(
                    clusters.get(link.y));
            clusters.remove(link.x);
            clusters.remove(link.y);

            // Iterate through the existing cluster ids and add the new
            // similarity between the two clusters.  Since we're using UPGMA, we
            // can do this by just comparing the centers (we don't even need to
            // scale since the cosine similarity is invariant to scalar
            // differences).  
            //
            // Note: This operation is N log N in complexity since we are
            // entering N items into a priority queue (log N complexity for
            // offer).
            for (Map.Entry<Integer, Candidate> e : clusters.entrySet()) {
                int id = e.getKey();
                Candidate c = e.getValue();
                linkPriority.offer(new Link(
                            Similarity.cosineSimilarity(newCandidate.center, c.center),
                            id, newId));
            }

            // Add the new cluster to the cluster map.  We make sure to do this
            // after adding similarities so we don't add a self similarity
            // value.
            clusters.put(newId, newCandidate);
        }
        return formAssignments(clusters.values(), m.rows());
    }

    /**
     * Returns the {@link Assignments} corresponding to the set of {@link
     * Candidate} clusters and number of data points.
     */
    public static Assignments formAssignments(Collection<Candidate> clusters,
                                              int numPoints) {
        int cid = 0;
        Assignments assignments = new Assignments(clusters.size(), numPoints);
        for (Candidate candidate : clusters) {
            for (int point : candidate.points)
                assignments.set(point, cid);
            cid++;
        }
        return assignments;
    }

    /**
     * A simple struct for storing and comparing links between two clusters.
     * Links with a higher similarity should be given higher priority.
     */
    public static class Link implements Comparable<Link> {
        /**
         * The similarity between clusters {@code x} and {@code y}.
         */
        public double sim;

        /**
         * The cluster id for the first cluster.
         */
        public int x;

        /**
         * The cluster id for the second cluster.
         */
        public int y;

        public Link(double sim, int x, int y) {
            this.sim = sim;
            this.x = x;
            this.y = y;
        }

        public int compareTo(Link o) {
            double diff = this.sim - o.sim;
            if (diff < 0)
                return 1;
            return (diff == 0d) ? 0 : -1;
        }
    }

    /**
     * A simple struct for storing cluster centers and their assigned points.
     * {@link Candidate}s can be merged.
     */
    public static class Candidate {

        /**
         * The centroid vector for a cluster.
         */
        public DoubleVector center;

        /**
         * The points assigned to this cluster.
         */
        public Set<Integer> points;

        /**
         * Creates a new {@link Candidate}.  A copy of the center is made so
         * that any changes to it do not affect the matrix which the vector came
         * from.
         */
        public Candidate(DoubleVector center, int point) {
            this.center = Vectors.copyOf(center);
            this.points = new HashSet<Integer>();
            points.add(point);
        }

        /**
         * Adds the data from another cluster into this cluster and returns a
         * reference to this cluster.
         */
        public Candidate merge(Candidate other) {
            VectorMath.add(this.center, other.center);
            points.addAll(other.points);
            return this;
        }
    }
}
