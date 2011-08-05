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

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Assignments;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.ClusterUtil;

import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.Matrices;

import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.logging.Logger;



/**
 * A {@link Wordsi} implementation that performs batch clustering.  Each context
 * vector is stored and later clustered using a {@link Clustering} algorithm.
 *
 * @author Keith Stevens
 */
public class WaitingWordsi extends BaseWordsi {

    /**
     * A logger for recording the process of the {@link Wordsi} processing.
     */
    private static final Logger LOG = Logger.getLogger(
            WaitingWordsi.class.getName());

    /**
     * The {@link Clustering} implementation to use when all data points have
     * been observed.
     */
    private final Clustering clustering;

    /**
     * A mapping from strings to the set of context vectors associated with that
     * token.
     */
    private final Map<String, List<SparseDoubleVector>> dataVectors;

    /**
     * The final word space, which maps from strings to the semantic
     * representation.
     */
    private final Map<String, SparseDoubleVector> wordSpace;

    /**
     * The number of clusters.  This may be used as a theoretical upper bound as
     * opposed to a strict number of clusters.
     */
    private final int numClusters;

    /**
     * The {@link AssignmentReporter} to use for reporting clustering
     * assignments.
     */
    private final AssignmentReporter reporter;

    /**
     * Creates a new {@link WaitingWordsi}.  The number of clusters is left
     * unset, which requires that the {@link Clustering} algorithm be able to
     * decide on an appropriate number of clusters.
     *
     * @param acceptedWords The set of words that {@link Wordsi} should
     *        represent.  This may be {@code null} or empty}.
     * @param extractor The {@link ContextExtractor} used to parse documents.
     * @param trackSecondaryKeys If true, cluster assignments and secondary keys
     *        will be tracked.  If this is false, the {@link AssignmentReporter}
     *        will not be used.
     * @param clustering The {@link Clustering} algorithm to use on each data
     *        set.
     * @param reporter The {@link AssignmentReporter} responsible for generating
     *        a report that details the cluster assignments.    This may be
     *        {@link null}.    If {@code trackSecondaryKeys} is false, this is
     *        not used.
     */
    public WaitingWordsi(Set<String> acceptedWords,
                         ContextExtractor extractor,
                         Clustering clustering,
                         AssignmentReporter reporter) {
        this(acceptedWords, extractor, clustering, reporter, 0);
    }

    /**
     * Creates a new {@link WaitingWordsi}.    
     *
     * @param acceptedWords The set of words that {@link Wordsi} should
     *        represent.  This may be {@code null} or empty}.
     * @param extractor The {@link ContextExtractor} used to parse documents.
     * @param clustering The {@link Clustering} algorithm to use on each data
     *        set.
     * @param reporter The {@link AssignmentReporter} responsible for generating
     *        a report that details the cluster assignments.  This may be {@link
     *        null}.  If {@code trackSecondaryKeys} is false, this is not used.
     * @param numClusters Specifies the number of clusters to generate for each
     *        term.
     */
    public WaitingWordsi(Set<String> acceptedWords,
                         ContextExtractor extractor,
                         Clustering clustering,
                         AssignmentReporter reporter,
                         int numClusters) {
        super(acceptedWords, extractor);

        this.clustering = clustering;
        this.reporter = reporter;
        this.numClusters = numClusters;

        dataVectors = new HashMap<String, List<SparseDoubleVector>>();
        wordSpace = new ConcurrentHashMap<String, SparseDoubleVector>();
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
     * Adds the context vector to the end of the list of context vectors
     * associated with {@code focusKey}.
     */
    public void handleContextVector(String focusKey,
                                    String secondaryKey,
                                    SparseDoubleVector context) {
        // Get the list of context vectors for the focus key.
        List<SparseDoubleVector> termContexts = dataVectors.get(focusKey);
        if (termContexts == null) {
            synchronized (this) {
                termContexts = dataVectors.get(focusKey);
                if (termContexts == null) {
                    termContexts = new ArrayList<SparseDoubleVector>();
                    dataVectors.put(focusKey, termContexts);
                }
            }
        }

        // Add the new context vector.
        int contextId = 0;
        synchronized (termContexts) {
            contextId = termContexts.size();
            termContexts.add(context);
        }

        // Record the association.
        if (reporter != null)
            reporter.assignContextToKey(focusKey, secondaryKey, contextId);
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(final Properties props) {
        // Set up the concurrent data structures so we can process the documents
        // concurrently
        final BlockingQueue<Runnable> workQueue = 
            new LinkedBlockingQueue<Runnable>();
        int numThreads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < numThreads; ++i) {
            Thread t = new WorkerThread(workQueue);
            t.start();
        }

        final Semaphore termsProcessed = new Semaphore(0); 
        final int numTerms = dataVectors.size();

        // Process each word's context set in a worker thread.
        for (Map.Entry<String, List<SparseDoubleVector>> entry :
                dataVectors.entrySet()) {
            // Get the root word being discriminated and list of observed
            // contexts.
            final String senseName = entry.getKey();

            List<SparseDoubleVector> contextsWithNoLength = entry.getValue();
            final List<SparseDoubleVector> contextSet = 
                new ArrayList<SparseDoubleVector>(contextsWithNoLength.size());
            for (SparseDoubleVector v : contextsWithNoLength)
                contextSet.add(Vectors.subview(v, 0, getVectorLength()));
            
            workQueue.offer(new Runnable() {
                public void run() {
                    try {
                        clusterTerm(senseName, contextSet, props);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        termsProcessed.release();
                    }
                }
            });
        }
        try {
            termsProcessed.acquire(numTerms);
            if (reporter != null)
                reporter.finalizeReport();
        } catch (InterruptedException ie) {
            throw new Error(ie);
        }
        LOG.info("Finished processing all terms");
    }

    /**
     * Clusters the context vectors associated with {@link senseName}.
     */
    private void clusterTerm(String senseName,
                             List<SparseDoubleVector> contextSet,
                             Properties props) {
        // Convert the data points to a sparse matrix.
        SparseMatrix contexts = Matrices.asSparseMatrix(contextSet);

        // Cluster the context set.
        LOG.info("Clustering term: " + senseName);
        Assignments assignments = (numClusters > 0) 
            ? clustering.cluster(contexts, numClusters, props)
            : clustering.cluster(contexts, props);
        LOG.info("Finished clustering term: " + senseName);

        SparseDoubleVector[] centroids = assignments.getCentroids(contexts);

        // Add the centroids to the splitSenses map.
        for (int index = 0; index < centroids.length; ++index) {
            String sense = (index > 0)
                    ? senseName + "-" + index
                    : senseName;
            wordSpace.put(sense, centroids[index]);
        }

        LOG.info("Finished creating centroids for term: " + senseName);

        // If the reporter is null, avoid making any report.
        if (reporter == null)
            return;

        // Generate the secondary context labels for each data point.
        String[] contextLabels = reporter.contextLabels(senseName);
        if (contextLabels.length == 0)
            return;

        LOG.info("Making assignment report: " + senseName);
        // Report the assignments for each clustered data point.  Note that some
        // data points might not have been clustered (Cluto based clustering
        // does this on occasion) so we must check for the number of assignments
        // first.
        for (int i = 0; i < assignments.length(); ++i)
            if (assignments.get(i).assignments().length > 0)
                reporter.updateAssignment(senseName, contextLabels[i],
                                          assignments.get(i).assignments()[0]);
        LOG.info("Finished making assignment report: " + senseName);
    }
}
