/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.clustering.Cluster;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;
import edu.ucla.sspace.clustering.OnlineClustering;

import edu.ucla.sspace.matrix.Matrices;

import edu.ucla.sspace.vector.SparseDoubleVector;

import edu.ucla.sspace.util.Generator;
import edu.ucla.sspace.util.GeneratorMap;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * A {@link Wordsi} implementation that utilizes streaming, or online,
 * clustering algorithms.  This model will immediate assign a context vector to
 * one of the clusters generated for a particular focus word, or create a new
 * cluster if needed.  After processing is compelete, the {@link
 * AssignmentReporter} will be informed of all the data point assignments made
 * by the clustering algorithm for each word.
 *
 * @author Keith Stevens
 */
public class StreamingWordsi extends BaseWordsi {

    /**
     * The type of clustering used for {@code StreamingWordsi}.  This specifies
     * how {@link Wordsi} will merge it's context vectors into different senses.
     */
    private GeneratorMap<OnlineClustering<SparseDoubleVector>> clusterMap;

    /**
     * The final word space generated, with one vector for each induced word
     * sense.  Word senses are stored as "focusTerm[-senseNum]".  A sense number
     * is only provided if it is larger than 0.
     */
    private final Map<String, SparseDoubleVector> wordSpace;

    /**
     * The {@link AssignmentReporter} responsible for generating an assignment
     * report based on the clustering assignments.    This may be {@code null}.
     */
    private final AssignmentReporter reporter;

    /**
     * The maximum number of clusters permitted.
     */
    private final int numClusters;

    /**
     * Creates a new {@link StreamingWordsi}.
     *
     * @param acceptedWords The set of words that {@link Wordsi} should
     *         represent.  This may be {@code null} or empty}.
     * @param extractor The {@link ContextExtractor} used to parse documents
     * @param trackSecondaryKeys If true, cluster assignments and secondary keys
     *        will be tracked. If this is false, the {@link AssignmentReporter}
     *        will not be used.
     * @param clusterGenerator A {@link Generator} responsible for creating new
     *        instances of a {@link OnlineClustering} algorithm.
     * @param reporter The {@link AssignmentReporter} responsible for generating
     *        a report that details the cluster assignments. This may be {@link
     *        null}. If {@code trackSecondaryKeys} is false, this is not used.
     */
    public StreamingWordsi(
            Set<String> acceptedWords,
            ContextExtractor extractor,
            Generator<OnlineClustering<SparseDoubleVector>> clusterGenerator,
            AssignmentReporter reporter,
            int numClusters) {
        super(acceptedWords, extractor); 
        clusterMap = new GeneratorMap<OnlineClustering<SparseDoubleVector>>(
                        clusterGenerator);
        this.reporter = reporter;
        this.numClusters = numClusters;

        this.wordSpace = new HashMap<String, SparseDoubleVector>();
    }
 
    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return wordSpace.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getVector(String term) {
        return wordSpace.get(term);
    }

    /**
     * {@inheritDoc}
     */
    public void handleContextVector(String focusKey,
                                    String secondaryKey,
                                    SparseDoubleVector context) {
        OnlineClustering<SparseDoubleVector> clustering = 
            clusterMap.get(focusKey);
        int contextId = clustering.addVector(context);
        if (reporter != null)
            reporter.assignContextToKey(focusKey, secondaryKey, contextId);
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties props) {
        double mergeThreshold = .15;

        // Iterate through all of the clusters and perform an agglomerative
        // cluster over the learned word senses.  If there is a reporter, the
        // cluster assignments are reported.
        for (Map.Entry<String, OnlineClustering<SparseDoubleVector>> entry :
                 clusterMap.entrySet()) {

            // First forcefully condense everything down to the required number
            // of clusters.
            List<Cluster<SparseDoubleVector>> newClusters = clusterStream(
                entry.getValue().getClusters(), numClusters, 0.0);

            // Then try to merge these new centroids based on the similarity
            // threshold.
            newClusters = clusterStream(entry.getValue().getClusters(),
                                        0, mergeThreshold);

            // Store a mapping for each word sense to it's induced word sense,
            // i.e., the centroid.
            String primaryKey = entry.getKey();
            wordSpace.put(primaryKey, newClusters.get(0).centroid());
            for (int i = 1; i < newClusters.size(); ++i)
                wordSpace.put(primaryKey+"-"+i, newClusters.get(i).centroid());

            // If there is no reporter, skip any post processing.
            if (reporter == null)
                continue;

            // Get the set of context labels for each data point for the given
            // word.
            String[] contextLabels = reporter.contextLabels(primaryKey);
            if (contextLabels.length == 0)
                continue;

            // Output the assignments for a single clustering.
            int clusterId = 0;
            for (Cluster<SparseDoubleVector> cluster : newClusters) {
                BitSet contextIds = cluster.dataPointIds();
                for (int contextId = contextIds.nextSetBit(0); contextId >= 0;
                         contextId = contextIds.nextSetBit(contextId + 1)) {
                    reporter.updateAssignment(
                            primaryKey, contextLabels[contextId], clusterId); 
                }
                clusterId++;
            }
        }

        // Null out the cluster map so that the garbage collector can reclaim it
        // and any data associated with the Clusters.
        clusterMap = null;

        if (reporter != null)
            reporter.finalizeReport();
    }

    private List<Cluster<SparseDoubleVector>> clusterStream(
            List<Cluster<SparseDoubleVector>> clusters,
            int numClusters,
            double threshold) {
        // Form a list of centroid vectors, which will be clustered by HAC.
        List<SparseDoubleVector> centroids =
            new ArrayList<SparseDoubleVector>();

        for (Cluster<SparseDoubleVector> cluster : clusters)
            centroids.add(cluster.centroid());

        // Cluster the centroids with a threshold.
        int[] assignments;
        if (numClusters != 0) {
            assignments = HierarchicalAgglomerativeClustering.partitionRows(
                Matrices.asSparseMatrix(centroids), 
                numClusters,
                ClusterLinkage.MEAN_LINKAGE,
                SimType.COSINE);
        } else {
            assignments = HierarchicalAgglomerativeClustering.clusterRows(
                Matrices.asSparseMatrix(centroids), 
                threshold,
                ClusterLinkage.MEAN_LINKAGE,
                SimType.COSINE);
        }

        // Combine clusters determined to be merged by HAC.
        List<Cluster<SparseDoubleVector>> newClusters =
            new ArrayList<Cluster<SparseDoubleVector>>();

        // When a new assignment index is encountered, create an empty
        // cluster.  The first cluster assigned to that index becomes the
        // initial value.  Later clusters assigned to that index are merged
        // into the primary cluster.
        for (int i = 0; i < assignments.length; ++i) {
            int assignment = assignments[i];
            while (assignment >= newClusters.size())
                newClusters.add(null);
            Cluster<SparseDoubleVector> cluster = newClusters.get(assignment);
            if (cluster == null)
                newClusters.set(assignment, clusters.get(i));
            else
                cluster.merge(clusters.get(i));
        }

        return newClusters;
    }
}
