/*
 * Copyright 2009 David Jurgens
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
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.OnDiskMatrix;

import edu.ucla.sspace.util.WorkQueue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A utility class for performing <a
 * href="http://en.wikipedia.org/wiki/Cluster_analysis#Agglomerative_hierarchical_clustering">Hierarchical
 * Agglomerative Clustering</a> on matrix data in a file.
 *
 * </p> This class provides static accessors to several variations of
 * agglomerative clustering and conforms to the {@link Clustering} interface,
 * which allows this method to be used in place of other clustering algorithms.
 *
 * <p> In addition to clustering, this implementation also exposes the ability
 * to view the iterative bottom-up merge through the {@link
 * buildDendrogram(Matrix,ClusterLinkage,SimType) buildDendogram} methods.
 * These methods return a series of {@link Merge} operations that can be used to
 * construct a <a href="http://en.wikipedia.org/wiki/Dendrogram">dendrogram</a>
 * and see the partial clustering at any point during the agglomerative merging
 * process.  For example, to view the clustering solution after four steps, the
 * following code might be used:
 *
 *<pre name="code" class="java:nocontrols:nogutter">
 *   Matrix matrix; 
 *   List&lt;Merge&gt; merges = buildDendogram(matrix, ...);
 *   List&lt;Merge&gt; fourMergeSteps = merges.subList(0, 4);
 *   MultiMap&lt;Integer,Integer&gt; clusterToRows =  new HashMultiMap&lt;Integer,Integer&gt;();
 *   for (int i = 0; i &lt; matrix.rows(); ++i)
 *       clusterToElements.put(i, i);
 *
 *   for (Merge m : fourMergeSteps) {
 *       clusterToElements.putMulti(m.remainingCluster(), 
 *           clusterToElements.remove(m.mergedCluster()));
 *   }
 *</pre>
 * 
 * The resulting {@link edu.ucla.sspace.util.MultiMap} {@code clusterToRows}
 * contains the mapping from each cluster to the rows that are a part of it.
 *
 * <p><i>Implementation Note:</i> The current version runs in O(n<sup>3</sup>)
 * worst case time for the number of rows in the matrix.  While O(n<sup>2</sup>
 * * log(n)) methods exist, these require storing similarity comparisons in a
 * priority queue, which has a substantially higher memory overhead.  Therefore,
 * this implementation has opted for a more expensive running time in order to
 * be able to process larger matrices.
 *
 * <p> When using the {@link Clustering#cluster(Matrix,Properties)} interface,
 * this class supports the following properties for controlling the clustering.
 *
 * <dl style="margin-left: 1em">
 * <dt> <i>Property:</i> <code><b>{@value #MIN_CLUSTER_SIMILARITY_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> unset
 *
 * <dd style="padding-top: .5em"> This property specifies the cluster similarity
 *      threshold at which two clusters are merged together.  Merging will
 *      continue until either all clusters have similarities below this
 *      threshold or the number of desired clusters has been reached.  This
 *      property provides an alternative to the num of clusters property for
 *      deciding when to stop agglomeratively merging clusters.  Both properties
 *      cannot be specified at the same time. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #CLUSTER_LINKAGE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_CLUSTER_LINKAGE_PROPERTY}
 *
 * <dd style="padding-top: .5em"> This property specifies the {@link
 *       ClusterLinkage} to use when computing cluster similarity. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #SIMILARITY_FUNCTION_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link SimType#COSINE COSINE}
 *
 * <dd style="padding-top: .5em"> This property specifies the name of {@link
 *       SimType} to use when computing the similarity of two data points. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #NUM_CLUSTERS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> unset
 *
 * <dd style="padding-top: .5em"> This property specifies the number of clusters
 *      to generate from the data.  Clusters are agglomeratively merged until
 *      the specified number of clusters is reached.  This property provides an
 *      alternative to the cluster similarity property for deciding when to stop
 *      agglomeratively merging clusters.  Both properties cannot be specified
 *      at the same time. </p>
 *
 * </dl>
 *
 * @author David Jurgens
 */
public class HierarchicalAgglomerativeClustering implements Clustering {

    /**
     * The method to use when comparing the similarity of two clusters.  See <a
     * href="http://home.dei.polimi.it/matteucc/Clustering/tutorial_html/hierarchical.html">
     * here </a> for an example of how the different linkages operate.
     */
    public enum ClusterLinkage { 
        /**
         * Clusters will be compared based on the most similar link between
         * them.
         */
        SINGLE_LINKAGE, 

        /**
         * Clusters will be compared based on the total similarity of all
         * pair-wise comparisons of the data points in each.
         */
        COMPLETE_LINKAGE, 

        /**
         * Clusters will be compared using the similarity of the computed mean
         * data point (or <i>centroid</i>) for each cluster.  This comparison
         * method is also known as UPGMA.
         */
        MEAN_LINKAGE,
        
        /**
         * Clusters will be compared using the similarity of the computed
         * median data point for each cluster
         */
        MEDIAN_LINKAGE 
    }

    /**
     * A prefix for specifying properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering";

    /**
     * The property for specifying the cluster similarity threshold.
     */
    public static final String MIN_CLUSTER_SIMILARITY_PROPERTY =
        PROPERTY_PREFIX + ".clusterThreshold";

    /**
     * The property for specifying the cluster linkage to use.
     */
    public static final String CLUSTER_LINKAGE_PROPERTY =
        PROPERTY_PREFIX + ".clusterLinkage";

    /**
     * The property for specifying the similarity function to use.
     */
    public static final String SIMILARITY_FUNCTION_PROPERTY =
        PROPERTY_PREFIX + ".simFunc";

    /**
     * The property for specifying the similarity function to use.
     */
    public static final String NUM_CLUSTERS_PROPERTY =
        PROPERTY_PREFIX + ".numClusters";

    /**
     * The default similarity threshold to use.
     */
    private static final String DEFAULT_MIN_CLUSTER_SIMILARITY_PROPERTY = "-1";

    /**
     * The default linkage method to use.
     */
    public static final String DEFAULT_CLUSTER_LINKAGE_PROPERTY =
        "COMPLETE_LINKAGE";

    /**
     * The default similarity function to use.
     */
    private static final String DEFAULT_SIMILARITY_FUNCTION_PROPERTY =
        "COSINE";

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(HierarchicalAgglomerativeClustering.class.getName());

    /**
     * The work used by all HAC instances to perform multi-threaded operations.
     */
    private static final WorkQueue WORK_QUEUE = new WorkQueue();

    /**
     * {@inheritDoc}
     */
    public Assignment[] cluster(Matrix matrix, Properties props) {
        String minSimProp = props.getProperty(MIN_CLUSTER_SIMILARITY_PROPERTY);
        String numClustProp = props.getProperty(NUM_CLUSTERS_PROPERTY);
        String simFuncProp = props.getProperty(SIMILARITY_FUNCTION_PROPERTY);
        String linkageProp = props.getProperty(CLUSTER_LINKAGE_PROPERTY);

        SimType simFunc = SimType.valueOf((simFuncProp == null)
                                          ? DEFAULT_SIMILARITY_FUNCTION_PROPERTY
                                          : simFuncProp);
        ClusterLinkage linkage = ClusterLinkage.valueOf((linkageProp == null)
            ? DEFAULT_CLUSTER_LINKAGE_PROPERTY
            : linkageProp);


        if (minSimProp == null && numClustProp == null)
            throw new IllegalArgumentException(
                "This class requires either a specified number of clusters or "+
                "a minimum cluster similarity threshold in order to partition "+
                "throw rows of the input.  Either needs to be provided as a " +
                "property");
        else if (minSimProp != null && numClustProp != null)
            throw new IllegalArgumentException(
                "Cannot specify both a fixed number of clusters AND a minimum "+
                "cluster similarity as input properties");
        else if (minSimProp != null) {
            try {
                double clusterSimThresh = Double.parseDouble(minSimProp);
                return toAssignments(cluster(matrix, clusterSimThresh, 
                                             linkage, simFunc, -1));                
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                    "Cluster similarity threshold was not a valid double: " +
                    minSimProp);
            }
        }
        else {
            return cluster(matrix, -1, props);
        }
    }

    /**
     * {@inheritDoc} The value of the {@code numClusters} parameter will
     * override the {@value #NUM_CLUSTERS_PROPERTY} if it was specified.
     */
    public Assignment[] cluster(Matrix m,
                                int numClusters,
                                Properties props) {
        double clusterSimilarityThreshold =
            Double.parseDouble(props.getProperty(
                        MIN_CLUSTER_SIMILARITY_PROPERTY,
                        DEFAULT_MIN_CLUSTER_SIMILARITY_PROPERTY));

        ClusterLinkage linkage = ClusterLinkage.valueOf(props.getProperty(
                    CLUSTER_LINKAGE_PROPERTY,
                    DEFAULT_CLUSTER_LINKAGE_PROPERTY));

        SimType similarityFunction = SimType.valueOf(props.getProperty(
                    SIMILARITY_FUNCTION_PROPERTY,
                    DEFAULT_SIMILARITY_FUNCTION_PROPERTY));
        return toAssignments(cluster(m, clusterSimilarityThreshold, linkage,
                                     similarityFunction, numClusters));
    }

    /**
     * Clusters all rows in the matrix using the specified cluster similarity
     * measure for comparison and stopping when the number of clusters is equal
     * to the specified number.
     *
     * @param m a matrix whose rows are to be clustered
     * @param numClusters the number of clusters into which the matrix should
     *        divided
     * @param linkage the method to use for computing the similarity of two
     *        clusters
     *
     * @return an array where each element corresponds to a row and the value is
     *         the cluster number to which that row was assigned.  Cluster
     *         numbers will start at 0 and increase.
     */
    static int[] partitionRows(Matrix m, int numClusters,
                                      ClusterLinkage linkage,
                                      SimType similarityFunction) {
        return cluster(m, -1, linkage, similarityFunction, numClusters);
    }

    /**
     * Clusters all rows in the matrix using the specified cluster similarity
     * measure for comparison and threshold for when to stop clustering.
     * Clusters will be repeatedly merged until the highest cluster similarity
     * is below the threshold.
     *
     * @param m a matrix whose rows are to be clustered
     * @param clusterSimilarityThreshold the threshold to use when deciding
     *        whether two clusters should be merged.  If the similarity of the
     *        clusters is below this threshold, they will not be merged and the
     *        clustering process will be stopped.
     * @param linkage the method to use for computing the similarity of two
     *        clusters
     *
     * @return an array where each element corresponds to a row and the value is
     *         the cluster number to which that row was assigned.  Cluster
     *         numbers will start at 0 and increase.
     */
    @SuppressWarnings("unchecked")
    static int[] clusterRows(Matrix m, double clusterSimilarityThreshold,
                                    ClusterLinkage linkage,
                                    SimType similarityFunction) {
        return cluster(m, clusterSimilarityThreshold, linkage, 
                       similarityFunction, -1);
    }

    /**
     * 
     *
     * @param m a matrix whose rows are to be clustered
     * @param clusterSimilarityThreshold the optional parameter for specifying
     *        the minimum inter-cluster similarity to use when deciding whether
     *        two clusters should be merged.  If {@code maxNumberOfClusters} is
     *        positive, this value is discarded in order to cluster to a fixed
     *        number.  Otherwise all clusters will be merged until the minimum
     *        distance is less than this threshold.
     * @param linkage the method to use for computing the similarity of two
     *        clusters
     * @param maxNumberOfClusters an optional parameter to specify the maximum
     *        number of clusters to have.  If this value is non-positive,
     *        clusters will be merged until the inter-cluster similarity is
     *        below the threshold, otherwise; if the value is positive, clusters
     *        are merged until the desired number of clusters has been reached.
     *
     * @return an array where each element corresponds to a row and the value is
     *         the cluster number to which that row was assigned.  Cluster
     *         numbers will start at 0 and increase.
     */
    private static int[] cluster(Matrix m, double clusterSimilarityThreshold,
                                 ClusterLinkage linkage, 
                                 SimType similarityFunction,
                                 int maxNumberOfClusters) {
        int rows = m.rows();
        LOGGER.info("Generating similarity matrix for " + rows+ " data points");
        Matrix similarityMatrix = 
            computeSimilarityMatrix(m, similarityFunction);

        // Create the initial set of clusters where each row is originally in
        // its own cluster
        Map<Integer,Set<Integer>> clusterAssignment = 
            generateInitialAssignment(rows);

        LOGGER.info("Calculating initial inter-cluster similarity using " 
                    + linkage);
        // Generate the initial set of cluster pairings based on the highest
        // similarity.  This mapping will be update as the number of clusters
        // are reduced, where merging a cluster will causes all the pairings
        // pointing to it constinuents recalculated.
        Map<Integer,Pairing> clusterSimilarities =
            new HashMap<Integer,Pairing>();
        for (Integer clusterId : clusterAssignment.keySet()) {
            clusterSimilarities.put(clusterId,
                 findMostSimilar(clusterAssignment, clusterId, 
                                 linkage, similarityMatrix));
        }

        LOGGER.info("Assigning clusters");

        // Keep track of which ID is available for the new, merged cluster
        int nextClusterId = rows;

        // While we still have more clusters than the maximum number loop.  Note
        // that if the maximum was set to negative, this condition will always
        // be true and the inner loop check for inter-cluster similarity will
        // break out of this loop
        while (clusterAssignment.size() > maxNumberOfClusters) {
            // Find a row that has yet to be clustered by searching for the pair
            // that is most similar
            int cluster1index = 0;
            int cluster2index = 0;
            double highestSimilarity = -1;

            // Find the row with the highest similarity to another            
            for (Map.Entry<Integer,Pairing> e : 
                     clusterSimilarities.entrySet()) {

                Pairing p = e.getValue();
                Integer i = e.getKey();
                Integer j = p.pairedIndex;
                if (p.similarity > highestSimilarity) {
                    cluster1index = i;
                    cluster2index = j;
                    highestSimilarity = p.similarity;
                }
            }            
            
            // If the similarity of the two most similar clusters falls below
            // the threshold, then the final set of clusters has been
            // determined.
            if (maxNumberOfClusters < 1 &&
                highestSimilarity < clusterSimilarityThreshold)
                break;
            
            // Assign the merged cluster a new ID, which lets us track any
            // pairings to the original clusters that may need to be
            // recalculated
            int newClusterId = nextClusterId++;

            Set<Integer> cluster1 = clusterAssignment.get(cluster1index);
            Set<Integer> cluster2 = clusterAssignment.get(cluster2index);

            LOGGER.log(Level.FINE, "Merged cluster {0} with {1}",
                       new Object[] { cluster1, cluster2 });

            // Update the cluster assignments, adding in the new cluster and
            // remove all references to the two merged clusters.
            cluster1.addAll(cluster2);
            clusterAssignment.put(newClusterId, cluster1);
            clusterAssignment.remove(cluster1index);
            clusterAssignment.remove(cluster2index);
            clusterSimilarities.remove(cluster1index);
            clusterSimilarities.remove(cluster2index);

            // Local state variables to use while recalculating the similarities
            double mostSimilarToMerged = -1;
            Integer mostSimilarToMergedId = null;

            // Check whether we have just merged the last two clusters, in which
            // case the similarity recalculation is unnecessary
            if (clusterSimilarities.isEmpty())
                break;

            // Recalculate the inter-cluster similarity of a cluster that was
            // paired with either of these two (i.e. was most similar to one of
            // them before the merge).  At the same time, calculate the
            // most-similar to the newly merged cluster
            for (Map.Entry<Integer,Pairing> e : 
                     clusterSimilarities.entrySet()) {

                Integer clusterId = e.getKey();

                // First, calculate the similarity between this cluster and the
                // newly merged cluster
                double simToNewCluster = 
                    getSimilarity(similarityMatrix, cluster1,
                                  clusterAssignment.get(clusterId), linkage);
                if (simToNewCluster > mostSimilarToMerged) {
                    mostSimilarToMerged = simToNewCluster;
                    mostSimilarToMergedId = clusterId;
                }

                // Second, if the pair was previously paired with one of the
                // merged clusters, recompute what its most similar is
                Pairing p = e.getValue();
                if (p.pairedIndex == cluster1index 
                        || p.pairedIndex == cluster2index) {
                    // Reassign with the new most-similar
                    e.setValue(findMostSimilar(clusterAssignment, clusterId, 
                                               linkage, similarityMatrix));
                }
            }
            
            // Update the new most similar to the newly-merged cluster
            clusterSimilarities.put(newClusterId, 
                                    new Pairing(mostSimilarToMerged, 
                                                mostSimilarToMergedId));
        }

        return toAssignArray(clusterAssignment, rows);
    }

    /**
     * Builds a dendrogram of the rows of similarity matrix by iteratelyve
     * linking each row according to the linkage policy in a bottom up manner.
     * The dendrogram is represented as a series of merge steps for the rows of
     * the similarity matrix, where each row is initially assigned to its own
     * cluster.  By following a sequence of merge operations, a particular
     * partitioning of the rows of {@code m} can be determined.  For example, to
     * find the partitioning after 4 merge operations, one might do the
     * following:
     *
     *<pre>
     *   Matrix matrix; 
     *   List<Merge> merges = buildDendogram(matrix, ...);
     *   List<Merge> fourMergeSteps = merges.subList(0, 4);
     *   MultiMap<Integer,Integer> clusterToRows =  new HashMultiMap<Integer,Integer>();
     *   for (int i = 0; i < matrix.rows(); ++i)
     *       clusterToElements.put(i, i);
     *
     *   for (Merge m : fourMergeSteps) {
     *       clusterToElements.putMulti(m.remainingCluster(), 
     *           clusterToElements.remove(m.mergedCluster()));
     *   }
     *</pre>
     * 
     * The resulting {@link edu.ucla.sspace.util.MultiMap} {@code clusterToRows}
     * contains the mapping from each cluster to the rows that are a part of it.
     *
     * @param m a matrix whose rows are to be compared and agglomeratively
     *        merged into clusters
     * @param linkage how two clusters should be compared for similarity when
     *        deciding which clusters to merge together
     * @param similarityFunction how to compare two rows of a matrix for
     *        similarity
     *
     * @return a dendrogram corresponding to the merge steps for each cluster,
     *         where each row is initially assigned to its own cluster whose id
     *         is the same as its row's index
     */
    public List<Merge> buildDendogram(
            Matrix m, ClusterLinkage linkage, SimType similarityFunction) {

        int rows = m.rows();
        LOGGER.finer("Generating similarity matrix for " + rows+ " data points");
        Matrix similarityMatrix = 
            computeSimilarityMatrix(m, similarityFunction);
        return buildDendrogram(similarityMatrix, linkage);
    }

    /**
     * Builds a dendrogram of the rows of similarity matrix by iteratively
     * linking each row according to the linkage policy in a bottom up manner.
     * The dendrogram is represented as a series of merge steps for the rows of
     * the similarity matrix, where each row is initially assigned to its own
     * cluster.
     *
     * @param similarityMatrix a square matrix whose (i, j) values denote the
     *        similarity of row i to row j.
     *
     * @return a dendrogram corresponding to the merge steps for each cluster,
     *         where each row is initially assigned to its own cluster whose id
     *         is the same as its row's index
     *
     * @throws IllegalArgumentException if {@code similarityMatrix} is not a
     *         square matrix
     */
    public List<Merge> buildDendrogram(Matrix similarityMatrix, 
                                       ClusterLinkage linkage) {

        if (similarityMatrix.rows() != similarityMatrix.columns())
            throw new IllegalArgumentException(
                "Similarity matrix must be square");

//         if (!(similarityMatrix instanceof OnDiskMatrix)) {
//             LOGGER.fine("Similarity matrix supports fast multi-threaded " +
//                         "access; switching to multi-threaded clustering");
//             return buildDendogramMultithreaded(similarityMatrix, linkage);
//         }

        int rows = similarityMatrix.rows();

        // Create the initial set of clusters where each row is originally in
        // its own cluster
        final Map<Integer,Set<Integer>> clusterAssignment = 
            generateInitialAssignment(rows);

        LOGGER.finer("Calculating initial inter-cluster similarity using " 
                    + linkage);
        // Generate the initial set of cluster pairings based on the highest
        // similarity.  This mapping will be update as the number of clusters
        // are reduced, where merging a cluster will causes all the pairings
        // pointing to it constinuents recalculated.
        final Map<Integer,Pairing> clusterSimilarities =
            new HashMap<Integer,Pairing>();

        // For each cluster, find the most similar cluster
        for (Integer clusterId : clusterAssignment.keySet()) {
            clusterSimilarities.put(clusterId,
                findMostSimilar(clusterAssignment, clusterId, 
                                linkage, similarityMatrix));
        }

        LOGGER.finer("Assigning clusters");
        List<Merge> merges = new ArrayList<Merge>(rows - 1);       

        // Perform rows-1 merges to merge all elements
        for (int mergeIter = 0; mergeIter < rows - 1; ++mergeIter) {
            LOGGER.finer("Computing dendogram merge" 
                         + mergeIter + "/" + (rows-1));

            // Find the two clusters that have the highest similarity
            int cluster1index = 0;
            int cluster2index = 0;
            double highestSimilarity = -1;

            // For each cluster, look at the cluster with the highest
            // similarity, and select the two with the global max
            for (Map.Entry<Integer,Pairing> e : 
                     clusterSimilarities.entrySet()) {

                Pairing p = e.getValue();
                Integer i = e.getKey();
                Integer j = p.pairedIndex;
                if (p.similarity > highestSimilarity) {
                    cluster1index = i;
                    cluster2index = j;
                    highestSimilarity = p.similarity;
                }
            }            

            // Order the indices so that the smaller index is first
            if (cluster1index > cluster2index) {
                int tmp = cluster2index;
                cluster2index = cluster1index;
                cluster1index = tmp;
            }

            // Track that the two clusters will be merged.  Always use the lower
            // of the two values as the new cluster assignment.
            Merge merge = 
                new Merge(cluster1index, cluster2index, highestSimilarity);
            merges.add(merge);

            Set<Integer> cluster1 = clusterAssignment.get(cluster1index);
            Set<Integer> cluster2 = clusterAssignment.get(cluster2index);

            LOGGER.log(Level.FINER, 
                       "Merged cluster {0} with {1}, similarity {2}",
                       new Object[] { cluster1index, cluster2index, 
                                      highestSimilarity });

            // Update the cluster assignments, adding in elements from the
            // second cluster and remove all references to the second merged-in
            // cluster.
            cluster1.addAll(cluster2);
            clusterAssignment.remove(cluster2index);
            clusterSimilarities.remove(cluster2index);

            // When down to just one cluster, stop the iteration
            if (clusterAssignment.size() == 1)
                break;

            // Local state variables to use while recalculating the similarities
            double mostSimilarToMerged = -1;
            Integer mostSimilarToMergedId = null;            

            // Recalculate the inter-cluster similarity of a cluster in two cases:
            // 
            // 1) a cluster that paired with either of these two (i.e. was most
            // similar to one of them before the merge).  
            //
            // 2) the most similar cluster to the newly merged cluster
            for (Map.Entry<Integer,Pairing> e :
                     clusterSimilarities.entrySet()) {                

                Integer clusterId = e.getKey();

                // Skip self comparisons for the merged clustering
                if (clusterId == cluster1index)
                    continue;

                // First, calculate the similarity between this cluster and the
                // newly merged cluster
                double simToNewCluster = 
                    getSimilarity(similarityMatrix, cluster1,
                                  clusterAssignment.get(clusterId), linkage);

                // If this cluster is now the most similar to the newly-merged
                // cluster update its mapping
                if (simToNewCluster > mostSimilarToMerged) {
                    mostSimilarToMerged = simToNewCluster;
                    mostSimilarToMergedId = clusterId;
                }

                // Second, if the pair was previously paired with one of the
                // merged clusters, recompute what its most similar is
                Pairing p = e.getValue();
                if (p.pairedIndex == cluster1index 
                        || p.pairedIndex == cluster2index) {
                    // Reassign with the new most-similar
                    e.setValue(findMostSimilar(clusterAssignment, clusterId, 
                                               linkage, similarityMatrix));
                }
            }
            
            // Update the new most similar to the newly-merged cluster
            clusterSimilarities.put(cluster1index, 
                                    new Pairing(mostSimilarToMerged, 
                                                mostSimilarToMergedId));
        }

        return merges;

    }

    private List<Merge> buildDendogramMultithreaded(
            final Matrix similarityMatrix, final ClusterLinkage linkage) {

        int rows = similarityMatrix.rows();

        // Create the initial set of clusters where each row is originally in
        // its own cluster
        final Map<Integer,Set<Integer>> clusterAssignment = 
            generateInitialAssignment(rows);

        LOGGER.finer("Calculating initial inter-cluster similarity using " 
                    + linkage);
        // Generate the initial set of cluster pairings based on the highest
        // similarity.  This mapping will be update as the number of clusters
        // are reduced, where merging a cluster will causes all the pairings
        // pointing to it constinuents recalculated.
        final Map<Integer,Pairing> clusterSimilarities =
            new ConcurrentHashMap<Integer,Pairing>(clusterAssignment.size());

        // For each cluster, find the most similar cluster.  Use the current
        // thread as the task key so any other thread executing this method
        // won't conflict.
        Object taskKey = 
            WORK_QUEUE.registerTaskGroup(clusterAssignment.size());
        for (Integer clusterId : clusterAssignment.keySet()) {
            final Integer clustId = clusterId;
            WORK_QUEUE.add(taskKey, new Runnable() {
                    public void run() {
                          clusterSimilarities.put(clustId,
                              findMostSimilar(clusterAssignment, clustId, 
                                              linkage, similarityMatrix));
                    }
                });
        }        
        WORK_QUEUE.await(taskKey);

        LOGGER.finer("Assigning clusters");
        List<Merge> merges = new ArrayList<Merge>(rows - 1);
        

        // Perform rows-1 merges to merge all elements
        for (int mergeIter = 0; mergeIter < rows - 1; ++mergeIter) {
            LOGGER.finer("Computing dendogram merge " + mergeIter);
//             System.out.println("Computing dendogram merge " 
//                                + mergeIter + "/" + (rows-1));


            // Find the two clusters that have the highest similarity
            int cluster1index = 0;
            int cluster2index = 0;
            double highestSimilarity = -1;

            // For each cluster, look at the cluster with the highest
            // similarity, and select the two with the global max
            for (Map.Entry<Integer,Pairing> e : 
                     clusterSimilarities.entrySet()) {

                Pairing p = e.getValue();
                Integer i = e.getKey();
                Integer j = p.pairedIndex;
                if (p.similarity > highestSimilarity) {
                    cluster1index = i;
                    cluster2index = j;
                    highestSimilarity = p.similarity;
                }
            }            

            // Order the indices so that the smaller index is first
            if (cluster1index > cluster2index) {
                int tmp = cluster2index;
                cluster2index = cluster1index;
                cluster1index = tmp;
            }

            // Track that the two clusters will be merged.  Always use the lower
            // of the two values as the new cluster assignment.
            Merge merge = 
                new Merge(cluster1index, cluster2index, highestSimilarity);
            merges.add(merge);

            final Set<Integer> cluster1 = clusterAssignment.get(cluster1index);
            Set<Integer> cluster2 = clusterAssignment.get(cluster2index);

            LOGGER.log(Level.FINER, 
                       "Merged cluster {0} with {1}, similarity {2}",
                       new Object[] { cluster1index, cluster2index, 
                                      highestSimilarity });

            // Update the cluster assignments, adding in elements from the
            // second cluster and remove all references to the second merged-in
            // cluster.
            cluster1.addAll(cluster2);
            clusterAssignment.remove(cluster2index);
            clusterSimilarities.remove(cluster2index);

            // When down to just one cluster, stop the iteration
            if (clusterAssignment.size() == 1)
                break;

            // Recalculate the inter-cluster similarity of a cluster in two cases:
            // 
            // 1) a cluster that paired with either of these two (i.e. was most
            // similar to one of them before the merge).  
            //
            // 2) the most similar cluster to the newly merged cluster
            final ConcurrentNavigableMap<Double,Integer> mostSimilarMap 
                = new ConcurrentSkipListMap<Double,Integer>();
            // Use size()-1 as the number of tasks because we skip adding a task
            // for computing the new cluster's similarity to itself
            taskKey = 
                WORK_QUEUE.registerTaskGroup(clusterSimilarities.size() - 1);

            for (Map.Entry<Integer,Pairing> entry :
                     clusterSimilarities.entrySet()) {
                
                // Thread-local state variables
                final Map.Entry<Integer,Pairing> e = entry;
                final Integer clusterId = e.getKey();
                final Pairing p = e.getValue();
                final int c1index = cluster1index;
                final int c2index = cluster2index;

                // Skip self comparisons for the merged clustering
                if (clusterId == c1index)
                    continue;
                
                WORK_QUEUE.add(taskKey, new Runnable() {
                        public void run() {                            
                            // Task-local state variables to use while
                            // recalculating the similarities
                            double mostSimilarToMerged = -1;
                            Integer mostSimilarToMergedId = null;            

                            // First, calculate the similarity between this
                            // cluster and the newly merged cluster
                            double simToNewCluster = 
                                getSimilarity(similarityMatrix, cluster1,
                                              clusterAssignment.get(clusterId), 
                                              linkage);

                            // If this cluster is now the most similar to
                            // the newly-merged cluster update its mapping
                            if (simToNewCluster > mostSimilarToMerged) {
                                mostSimilarToMerged = simToNewCluster;
                                mostSimilarToMergedId = clusterId;
                            }

                            // Second, if the pair was previously paired with
                            // one of the merged clusters, recompute what its
                            // most similar is
                            if (p.pairedIndex == c1index 
                                    || p.pairedIndex == c2index) {
                                // Reassign with the new most-similar
                                e.setValue(findMostSimilar(clusterAssignment,
                                         clusterId, linkage, similarityMatrix));
                            }
                      
                            // Once all of the cluster for this thread has been
                            // processed, update the similarity map.
                            mostSimilarMap.put(mostSimilarToMerged,
                                               mostSimilarToMergedId);
                        }
                    });
            }
            
            // Run each thread's comparisons
            WORK_QUEUE.await(taskKey);

            // Collect the results from the similarity map.  The highest
            // similarity should be the largest key in the map, with the
            // clustering as the value.  Note that if there were ties in the
            // highest similarity, the cluster is selected by the last thread,
            // which is still arbitrarily fair.
            Map.Entry<Double,Integer> highest = mostSimilarMap.lastEntry();
        
            // Update the new most similar to the newly-merged cluster
            clusterSimilarities.put(cluster1index, 
                                    new Pairing(highest.getKey(), 
                                                highest.getValue()));
        }

        return merges;
    }

    /*
            // Recalculate the inter-cluster similarity of a cluster in two cases:
            // 
            // 1) a cluster that paired with either of these two (i.e. was most
            // similar to one of them before the merge).  
            //
            // 2) the most similar cluster to the newly merged cluster
            Collection<Runnable> similarityTasks = new ArrayList<Runnable>();
            
            // Dump the map's entries into a list so we can partition them among
            // different processing threads.  Although it's a linear operation,
            // this avoids two potential issues: (1) Having to create a new
            // Runnable for each comparison, and (2) Having a large number of
            // concurrent writes trying to update the most-similar value
            // (high-write contention).
            List<Map.Entry<Integer,Pairing>> toPartition = 
                new ArrayList<Map.Entry<Integer,Pairing>>(
                    clusterSimilarities.entrySet());

            int numThreads = WORK_QUEUE.numThreads();
            int comparisonsPerThread = toPartition.size() / numThreads;

            final ConcurrentNavigableMap<Double,Integer> mostSimilarMap 
                = new ConcurrentSkipListMap<Double,Integer>();

            final int c1index = cluster1index;
            final int c2index = cluster2index;

            for (int th = 0; th < numThreads; ++th) {
                int start = th * comparisonsPerThread;
                int end = Math.min((th + 1) * comparisonsPerThread, 
                                   toPartition.size());
                final List<Map.Entry<Integer,Pairing>> clustersToUpdate =
                    toPartition.subList(start, end);                               
                
                similarityTasks.add(new Runnable() {
                        public void run() {
                            
                            // Thread-local state variables to use while
                            // recalculating the similarities
                            double mostSimilarToMerged = -1;
                            Integer mostSimilarToMergedId = null;            

                            for (Map.Entry<Integer,Pairing> e :
                                     clustersToUpdate) {

                                Integer clusterId = e.getKey();
                                Pairing p = e.getValue();

                                // Skip self comparisons for the merged
                                // clustering
                                if (clusterId == c1index)
                                    continue;

                                // First, calculate the similarity between this
                                // cluster and the newly merged cluster
                                double simToNewCluster = 
                                    getSimilarity(similarityMatrix, cluster1,
                                              clusterAssignment.get(clusterId), 
                                              linkage);

                                // If this cluster is now the most similar to
                                // the newly-merged cluster update its mapping
                                if (simToNewCluster > mostSimilarToMerged) {
                                    mostSimilarToMerged = simToNewCluster;
                                    mostSimilarToMergedId = clusterId;
                                }

                                // Second, if the pair was previously paired with
                                // one of the merged clusters, recompute what its
                                // most similar is
                                if (p.pairedIndex == c1index 
                                        || p.pairedIndex == c2index) {
                                    // Reassign with the new most-similar
                                    e.setValue(findMostSimilar(
                                                  clusterAssignment,
                                                  clusterId, linkage, 
                                                  similarityMatrix));
                                }
                            }

                            // Once all of the clusters for this thread have
                            // been processed, update the similarit map.  We do
                            // this last to minimize the contention on the map
                            mostSimilarMap.put(mostSimilarToMerged,
                                               mostSimilarToMergedId);
                        }
                    });
            }
            
            // Run each thread's comparisons
            WORK_QUEUE.run(similarityTasks);

            // Collect the results from the similarity map.  The highest
            // similarity should be the largest key in the map, with the
            // clustering as the value.  Note that if there were ties in the
            // highest similarity, the cluster is selected by the last thread,
            // which is still arbitrarily fair.
            Map.Entry<Double,Integer> highest = mostSimilarMap.lastEntry();
        
            // Update the new most similar to the newly-merged cluster
            clusterSimilarities.put(cluster1index, 
                                    new Pairing(highest.getKey(), 
                                                highest.getValue()));
        }

        return merges;
    }

     */


    /**
     * For the current cluster, finds the most similar cluster using the
     * provided linkage method and returns the pairing for it.
     */
    private static Pairing findMostSimilar(
            Map<Integer,Set<Integer>> curAssignment, int curCluster, 
            ClusterLinkage linkage,  Matrix similarityMatrix) {
        // Start with with the most similar being set to the newly merged
        // cluster, as this value has already been computed
        double mostSimilar = -1;
        Integer paired = -1;
        for (Map.Entry<Integer,Set<Integer>> otherIdAndCluster 
                 : curAssignment.entrySet()) {
            Integer otherId = otherIdAndCluster.getKey();
            if (otherId.equals(curCluster))
                continue;
            double similarity = getSimilarity(
                similarityMatrix, curAssignment.get(curCluster),
                otherIdAndCluster.getValue(), linkage);
            if (similarity > mostSimilar) {
                mostSimilar = similarity;
                paired = otherId;
            }
        }
        return new Pairing(mostSimilar, paired);
    }

    /**
     * Returns the final mapping of data points as an array where each row is
     * assigned to a single cluster value from 0 to <i>n</u>, the number of
     * clusters.
     *
     * @param assignment a mapping from cluster number to the data points (rows)
     *        that are contained in it
     * @param p the number of initial data points
     *
     * @return the cluster assignment
     */
    private static int[] toAssignArray(Map<Integer,Set<Integer>> assignment, 
                                       int numDataPoints) {
        int[] clusters = new int[numDataPoints];
        for (int i = 0; i < numDataPoints; ++i)
            clusters[i] = -1;
        int clusterIndex = 0;
        for (Set<Integer> cluster : assignment.values()) {
            // Decide whether this cluster has already been assigned by picking
            // out the first element in the cluster and seeing if it has the
            // dummy cluster value (-1)
            int r = cluster.iterator().next();
            if (clusters[r] != -1)
                continue;
            // Otherwise the row this cluster needs to be assigned a cluster
            // index
            for (int row : cluster) 
                clusters[row] = clusterIndex;
            // Increment the cluster index for the next cluster
            clusterIndex++;
        }
        LOGGER.info("total number of clusters: " + clusterIndex);
        return clusters;
    }

    /**
     * Coverts an array containing each row's clustering assignment into an
     * array of {@link HardAssignment} instances.
     */
    private static Assignment[] toAssignments(int[] rowAssignments) {
        Assignment[] assignments = new Assignment[rowAssignments.length];
        for (int i = 0; i < rowAssignments.length; ++i)
            assignments[i] = new HardAssignment(rowAssignments[i]);
        return assignments;
    }

    /**
     *
     * @param numDataPoints the number of initial data points
     */
    private static Map<Integer,Set<Integer>> generateInitialAssignment(
        int numDataPoints) {
        Map<Integer,Set<Integer>> clusterAssignment = 
            new HashMap<Integer,Set<Integer>>(numDataPoints);
        for (int i = 0; i < numDataPoints; ++i) {
            HashSet<Integer> cluster=  new HashSet<Integer>();
            cluster.add(i);
            clusterAssignment.put(i, cluster);
        }
        return clusterAssignment;
    }

    /**
     * Computes and returns the similarity matrix for {@code m} using the
     * specified similarity function
     */
    private static Matrix computeSimilarityMatrix(Matrix m, 
                                                  SimType similarityFunction) {
        Matrix similarityMatrix = 
            Matrices.create(m.rows(), m.rows(), Matrix.Type.DENSE_ON_DISK);
        for (int i = 0; i < m.rows(); ++i) {
            for (int j = i + 1; j < m.rows(); ++j) {
                double similarity = 
                    Similarity.getSimilarity(similarityFunction,
                                             m.getRowVector(i),
                                             m.getRowVector(j));
                similarityMatrix.set(i, j, similarity);
                similarityMatrix.set(j, i, similarity);
            }
        }
        return similarityMatrix;
    }

    /**
     * Returns the similarity of two clusters according the specified linkage
     * function.
     * 
     * @param similarityMatrix a matrix containing pair-wise similarity of each
     *        data point in the entire set
     * @param cluster the first cluster to be considered
     * @param toAdd the second cluster to be considered
     * @param linkage the method by which the similarity of the two clusters
     *        should be computed
     *
     * @return the similarity of the two clusters
     */
    private static double getSimilarity(Matrix similarityMatrix,
                                        Set<Integer> cluster, 
                                        Set<Integer> toAdd,
                                        ClusterLinkage linkage) {
        double similarity = -1;
        switch (linkage) {
        case SINGLE_LINKAGE: {
            double highestSimilarity = -1;
            for (int i : cluster) {
                for (int j : toAdd) {
                    double s = similarityMatrix.get(i, j);
                    if (s > highestSimilarity)
                        highestSimilarity = s;
                }
            }
            similarity = highestSimilarity;
            break;
        }

        case COMPLETE_LINKAGE: {
            double lowestSimilarity = 1;
            for (int i : cluster) {
                for (int j : toAdd) {
                    double s = similarityMatrix.get(i, j);
                    if (s < lowestSimilarity)
                        lowestSimilarity = s;
                }
            }
            similarity = lowestSimilarity;
            break;
        }

        case MEAN_LINKAGE: {
            double similaritySum = 0;
            for (int i : cluster) {
                for (int j : toAdd) {
                    similaritySum += similarityMatrix.get(i, j);
                }
            }
            similarity = similaritySum / (cluster.size() * toAdd.size());
            break;
        }

        case MEDIAN_LINKAGE: {
            double[] similarities = new double[cluster.size() * toAdd.size()];
            int index = 0;
            for (int i : cluster) {
                for (int j : toAdd) {
                    similarities[index++] = similarityMatrix.get(i, j);
                }
            }
            Arrays.sort(similarities);
            similarity = similarities[similarities.length / 2];
            break;
        }
        
        default:
            assert false : "unknown linkage method";
        }
        return similarity;
    }

    /**
     * A utility structure for holding the assignment of a cluster to another
     * cluster by means of a high similarity.
     */
    private static class Pairing implements Comparable<Pairing> {
        
        /**
         * The similarity of the other cluster to the cluster indicated by
         * {@code pairedIndex}
         */
        public final double similarity;

        /**
         * The index of the cluster that is paired
         */
        public final int pairedIndex;

        public Pairing(double similarity, int pairedIndex) {
            this.similarity = similarity;
            this.pairedIndex = pairedIndex;
        }

        public int compareTo(Pairing p) {
            return (int)((p.similarity - similarity) * Integer.MAX_VALUE);
        }

        public boolean equals(Object o) {
            return (o instanceof Pairing)
                && ((Pairing)o).pairedIndex == pairedIndex;
        }
        
        public int hashCode() {
            return pairedIndex;
        }        
    }
}
