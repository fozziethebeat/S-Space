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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An implementation of the Clustering by Committee (CBC) algorithm.  This
 * implementation is based on the Pantel's thesis:
 * <ul>
 *
 *    <li style="font-family:Garamond, Georgia, serif"> Patrick
 *    Pantel. 2003. Clustering by Committee. Ph.D. Dissertation. Department of
 *    Computing Science, University of Alberta, Canada. available <a
 *    href="http://www.patrickpantel.com/cgi-bin/web/tools/getfile.pl?type=paper&id=2003/cbc.pdf">here</a>.
 * </ul>
 *
 * <p>This class offers five parameters for configuring how the clustering
 * occurs
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #AVERGAGE_LINK_MERGE_THRESHOLD_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_AVERGAGE_LINK_MERGE_THRESHOLD}
 *
 * <dd style="padding-top: .5em">The property to specify during the Phase II.1
 *      when to stop the agglomerative clustering of the nearest neighbors.
 *      This property specifies a {@code double} threshold where clusters whose
 *      the average-link similarity falls below the value will not be merged
 *      (i.e. stay two clusters).<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #COMMITTEE_SIMILARITY_THRESHOLD_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_COMMITTEE_SIMILARITY_THRESHOLD}
 *
 * <dd style="padding-top: .5em"> The property to specify during Phase II.3 what
 *      is the maximum similarity between two committees above which a new
 *      committee will not be included.  This property corresponds to θ-1 in the
 *      CBC papers.  This property specifies a {@code double}.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #RESIDUE_SIMILARITY_THRESHOLD_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_RESIDUE_SIMILARITY_THRESHOLD}
 *
 * <dd style="padding-top: .5em"> The property for specifying the similarity
 *      threshold in Phase II.5 where if an element has a similarity less than
 *      this threshold to all existing committees, the element is marked as
 *      "residue" and recursively clustered.  This property corresponds to the
 *      θ-2 parameter in the original papers.  The property is specified as a
 *      {@code double}.  <p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #HARD_CLUSTERING_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code true}
 *
 * <dd style="padding-top: .5em"> Specifies whether CBC should use a hard
 *      (single class) or soft (multi-class) cluster labeling.  The default
 *      behavior is to use hard clustering.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #SOFT_CLUSTERING_SIMILARITY_THRESHOLD_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_SOFT_CLUSTERING_SIMILARITY_THRESHOLD}
 *
 * <dd style="padding-top: .5em"> If soft clustering is enabled, specifies a
 *      {@code double} the threshold used during soft clustering where a point
 *      will not be labeled with the committees who are more similar than this
 *      value.  If hard clustering is enabled the value of this property has no
 *      effect.  See Phrase III of the CBC algorithm for more details.<p>
 *
 * </dl> <p>
 *
 * This class is thread-safe.
 *
 * @author David Jurgens
 */
public class ClusteringByCommittee implements Clustering {

    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.ClusteringByCommittee";

    /**
     * The property to specify during the Phase II.1 when to stop the
     * agglomerative clustering of the nearest neighbors.  This property
     * specifies a {@code double} threshold where clusters whose the
     * average-link similarity falls below the value will not be merged
     * (i.e. stay two clusters).
     */
    public static final String AVERGAGE_LINK_MERGE_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".averageLinkMergeThreshold";

    /**
     * The default value of the {@value #AVERGAGE_LINK_MERGE_THRESHOLD_PROPERTY}
     * property.
     */
    public static final String DEFAULT_AVERGAGE_LINK_MERGE_THRESHOLD = ".25";

    /**
     * The property to specify during Phase II.3 what is the maximum similarity
     * between two committees above which a new committee will not be included.
     * This property corresponds to θ-1 in the CBC papers.  This property
     * specifies a {@code double}.
     */
    public static final String COMMITTEE_SIMILARITY_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".maxCommitteeSimilarity";

    /**
     * The default value of the {@value
     * #COMMITTEE_SIMILARITY_THRESHOLD_PROPERTY} property.
     */
    public static final String DEFAULT_COMMITTEE_SIMILARITY_THRESHOLD = ".35";

    /**
     * The property for specifying the similarity threshold in Phase II.5 where
     * if an element has a similarity less than this threshold to all existing
     * committees, the element is marked as "residue" and recursively clustered.
     * This property corresponds to the θ-2 parameter in the original papers.
     * The property is specified as a {@code double}.
     */
    public static final String RESIDUE_SIMILARITY_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".residueSimilarityThreshold";

    /**
     * The default value of the {@value #RESIDUE_SIMILARITY_THRESHOLD_PROPERTY}
     * property.
     */
    public static final String DEFAULT_RESIDUE_SIMILARITY_THRESHOLD = ".25";

    /**
     * The property for specifying a {@code double} the threshold used during
     * soft clustering where a point will not be labeled with the committees who
     * are more similar than this value.  See Phrase III of the CBC algorithm
     * for more details.
     */
    public static final String SOFT_CLUSTERING_SIMILARITY_THRESHOLD_PROPERTY = 
        PROPERTY_PREFIX + ".softClusteringThreshold";

    /**
     * The default value of the {@value
     * #SOFT_CLUSTERING_SIMILARITY_THRESHOLD_PROPERTY} property.
     */
    public static final String 
        DEFAULT_SOFT_CLUSTERING_SIMILARITY_THRESHOLD = ".25";

    /**
     * Specifies whether CBC should use a hard (single class) or soft
     * (multi-class) cluster labeling.  The default is to use hard clustering.
     */
    public static final String HARD_CLUSTERING_PROPERTY =
        PROPERTY_PREFIX + ".useHardClustering";
    
    /**
     * The logger used by this class.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(ClusteringByCommittee.class.getName());

    /**
     * During Phase II.1, the k-nearest neighbors are used to create candidate
     * committees.  This constant is what was used in the Pantel and Lin (2002)
     * paper.
     */
    private static final int K_MOST_SIMILAR_NEIGHBORS = 10;

    /**
     * Creates a new {@code ClusteringByCommittee} instance
     */
    public ClusteringByCommittee() { }

    /**
     * <b>Ignores the provided number of clusters</b> and clusters the rows of
     * the provided matrix using the CBC algorithm.  This method is equivalent
     * to calling {@link #cluster(Matrix,Properties)} without specifying the
     * number of clusters.
     *
     * @throws IllegalArgumentException if {@code m} is not an instance of
     *         {@link SparseMatrix}.
     */
    public Assignments cluster(Matrix m, int numClusters, Properties props) {
        LOGGER.warning("CBC does not take in a specified number of clusters.  "
                    + "Ignoring specification and clustering anyway.");
        return cluster(m, props);
    }

    /**
     * Clusters the rows of {@code m} according to the CBC algorithm, using
     * {@code props} to specify the configurable parameters of the algorithm.
     *
     * @throws IllegalArgumentException if {@code m} is not an instance of
     *         {@link SparseMatrix}.
     */
    public Assignments cluster(Matrix m, Properties props) {
        // Set up the parameters for clustering
        double avgLinkMergeThresh = Double.parseDouble(props.getProperty(
            AVERGAGE_LINK_MERGE_THRESHOLD_PROPERTY,
            DEFAULT_AVERGAGE_LINK_MERGE_THRESHOLD));
        double maxCommitteeSimThresh = Double.parseDouble(props.getProperty(
            COMMITTEE_SIMILARITY_THRESHOLD_PROPERTY,
            DEFAULT_COMMITTEE_SIMILARITY_THRESHOLD));
        double residueSimThresh = Double.parseDouble(props.getProperty(
            RESIDUE_SIMILARITY_THRESHOLD_PROPERTY,
            DEFAULT_RESIDUE_SIMILARITY_THRESHOLD));
        double softClusteringThresh = Double.parseDouble(props.getProperty(
            SOFT_CLUSTERING_SIMILARITY_THRESHOLD_PROPERTY,
            DEFAULT_SOFT_CLUSTERING_SIMILARITY_THRESHOLD));
        boolean useHardClustering = Boolean.parseBoolean(
            props.getProperty(HARD_CLUSTERING_PROPERTY, "true"));

        LOGGER.info("Starting Clustering By Committee");

        // Check that the input is a sparse matrix
        if (!(m instanceof SparseMatrix))
            throw new IllegalArgumentException("CBC only accepts sparse matrices");

        SparseMatrix sm = (SparseMatrix)m;
        
        // Create a bit set with the number of bits equal to the number of rows.
        // This serves as input to phase 2 where we indicate that all rows
        // should be considered for clustering at first.
        BitSet allRows = new BitSet(sm.rows());
        allRows.set(0, sm.rows());
        LOGGER.info("CBC begining Phase 2");
        List<Committee> committees = phase2(
            sm, allRows, avgLinkMergeThresh, 
            maxCommitteeSimThresh, residueSimThresh);
        
        LOGGER.info("CBC begining Phase 3");
        // PHASE 3: Assign elements to clusters
        Assignment[] result = new Assignment[m.rows()];
        for (int r = 0; r < m.rows(); ++r) {
            LOGGER.fine("Computing Phase 3 for row " + r);
            SparseDoubleVector row = sm.getRowVector(r);
            // Determine to which committees the row belongs
            List<Integer> committeeIds = phase3(
                row, committees, useHardClustering, softClusteringThresh);
            int[] assignments = new int[committeeIds.size()];
            for (int i = 0; i < committeeIds.size(); ++i) {
                assignments[i] = committeeIds.get(i);
            }
            result[r] = new SoftAssignment(assignments);
        }
        return new Assignments(committees.size(), result, m);
    }

    /**
     * Starts Phase II of the CBC algorithm, returning a list {@link Committee}
     * instances that may cover the elements (rows) of {@code sm}.
     *
     * @param sm the matrix whose rows are to be clustered
     * @param rowsToConsider a bit set where the {@code true} values indicate
     *        which rows of {@code sm} should be evaluated.  This parameter is
     *        important for recursive calls where not all the neighbors will be
     *        considered
     * @param avgLinkMergeThresh specifies when to stop the agglomerative
     *        clustering of the nearest neighbors for clusters whose the
     *        average-link similarity falls below the value will not be merged
     *        (i.e. stay two clusters).
     * @param maxCommitteeSimThresh specifies during what is the maximum
     *        similarity between two committees above which a new committee will
     *        not be included.  This parameter corresponds to θ-1 in the CBC
     *        papers.
     * @param residueSimThresh specifies the similarity threshold in Phase II.5
     *        where if an element has a similarity less than this threshold to
     *        all existing committees, the element is marked as "residue" and
     *        recursively clustered.  This property corresponds to the θ-2
     *        parameter in the original papers.
     *
     * @return a list of identified committies in arbitrary order.
     */
    private static List<Committee> phase2(SparseMatrix sm, 
                                          BitSet rowsToConsider,
                                          double avgLinkMergeThresh,
                                          double maxCommitteeSimThresh,
                                          double residueSimThresh) {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("CBC computing Phase 2 for " + 
                        rowsToConsider.cardinality() + " rows"); 
        }
        
        List<CandidateCommittee> candidateCommittees = 
            new ArrayList<CandidateCommittee>();
       
        // STEP 1
        // For each element e in E (for each row in m)
        for (int r = rowsToConsider.nextSetBit(0); r >= 0; 
                 r = rowsToConsider.nextSetBit(r + 1)) {

            // 1.1) Cluster the top similar elements of e from S using
            //      average-link clustering.           
            MultiMap<Double,Integer> mostSimilarElements = new
                BoundedSortedMultiMap<Double,Integer>(K_MOST_SIMILAR_NEIGHBORS);

            for (int r2 = rowsToConsider.nextSetBit(0); r2 >= 0; 
                     r2 = rowsToConsider.nextSetBit(r2 + 1)) {
                                
                if (r == r2) 
                    continue ;
                               
                double sim = Similarity.cosineSimilarity(sm.getRowVector(r),
                                                         sm.getRowVector(r2));
                mostSimilarElements.put(sim, r2);
            }

            // If there were no similar elements to the current row, skip it.
            if (mostSimilarElements.size() == 0)
                continue;

            // 1.2) For each cluster discovered c compute the following score:
            //      |c| × avgsim(c), where |c| is the number of elements in c
            //      and avgsim(c) is the average pairwise similarity between
            //      elements in c.
            List<CandidateCommittee> commsForRow = buildCommitteesForRow(
                mostSimilarElements.values(), sm, avgLinkMergeThresh);
            Collections.sort(commsForRow);
        
            // 1.3) Store the highest-scoring cluster in a list L.
            candidateCommittees.add(commsForRow.get(0));
        }

        // STEP 2
        // Sort the clusters in L in descending order of their scores.
        Collections.sort(candidateCommittees);

        // STEP 3 
        // Let C be a list of committees, initially empty.
        List<Committee> committees = new ArrayList<Committee>();

        // For each cluster c in L in sorted order 
        for (CandidateCommittee cc : candidateCommittees) {
            // 3.1) Compute the centroid of c by averaging the frequency vectors
            //      of its elements and computing the mutual information vector
            //      of the centroid in the same way as we did for individual
            //      elements.
            

            // 3.2) If c's similarity to the centroid of each committee
            //      previously added to C is below a threshold, add c to C.
            boolean isDissimilar = true;
            for (Committee c : committees) {
                if (Similarity.cosineSimilarity(cc.centroid(), c.centroid()) >=
                        maxCommitteeSimThresh) {
                    isDissimilar = false;
                }
            }

            if (isDissimilar) {
                 committees.add(new Committee(cc));
            }
        }

        LOGGER.log(Level.FINE,
                "Found {0} committees.", new Object[] {committees.size()});

        // STEP 4
        // If C is empty, we are done and return C. 
        if (committees.isEmpty())
            return committees;

        Set<Integer> residues = new HashSet<Integer>();

        // STEP 5 
        // For each element e in E
        for (int r = rowsToConsider.nextSetBit(0); r >= 0; 
                 r = rowsToConsider.nextSetBit(r + 1)) {

            // 5.1) If e's similarity to every committee in C is below
            //      threshold2, add e to a list of residues R.
            boolean isResidue = true;
            SparseDoubleVector row = sm.getRowVector(r);
            for (Committee c : committees) {
                if (Similarity.cosineSimilarity(c.centroid(), row) >=
                        residueSimThresh) {
                    isResidue = false;
                }
            }
            if (isResidue)
                residues.add(r);            
        }

        if (LOGGER.isLoggable(Level.FINER) && !residues.isEmpty()) {
            LOGGER.finer("Found residual elements: " + residues);
        }

        // STEP 6
        // 6.1) If R is empty, we are done and return C.
        if (residues.isEmpty()) {
            return committees;
        }
        // Edge case: if only a single row is passed in to evaluate, the return
        // the existing set of committees, since we can't form any new
        // committees from a word with zero neighbors.
        else if (residues.size() == 1)
            return committees;

        BitSet b = new BitSet(sm.rows());
        for (Integer i : residues)
            b.set(i);
    
        // 6.2) Otherwise, return the union of C and the output of a recursive
        //      call to Phase II using the same input except replacing E with R.
        committees.addAll(phase2(sm, b, avgLinkMergeThresh, 
                                 maxCommitteeSimThresh, residueSimThresh));

        // Return: a list of committees.
        return committees;
    }

    /**
     * Computes Phase 3 of the CBC algorithm for the provided row
     *
     * @param row the row for which the final committee (clustering) assignment
     *        should be performed
     * @param committees the final list of committees to use in labeling the row
     * @param useHardClustering {@code true} if the row should be assigned one
     *        committee, {@code false} if the row could be assigned more than
     *        one
     * @param softClusteringThresh if soft-clustering is enabled, specifies the
     *        threshold used during soft clustering where a point will not be
     *        labeled with the committees who are more similar than this value.
     *
     * @return a list of committee assignments for the given row
     */
    private static List<Integer> phase3(SparseDoubleVector row, 
                                        List<Committee> committees,
                                        boolean useHardClustering,
                                        double softClusteringThresh) {

        if (useHardClustering) {
            int mostSimilarCommittee = -1;
            double highestSim = -1d;
            for (int i = 0; i < committees.size(); ++i) {
                Committee c = committees.get(i);
                double sim = Similarity.cosineSimilarity(row, c.centroid());
                if (sim > highestSim) {
                    highestSim = sim;
                    mostSimilarCommittee = i;
                }                
            }
            return Collections.singletonList(mostSimilarCommittee);
        }
        else {
            // Make a copy of the row because will be changing the vector as we
            // assign it to more committees
            SparseDoubleVector copy = new CompactSparseVector(row);
            
            // let C be a list of clusters initially empty
            List<Integer> assignedClusters = new ArrayList<Integer>();
            
            // let S be the top-200 similar clusters to e
            MultiMap<Double,Duple<Committee,Integer>> mostSimilarCommittees =
                new BoundedSortedMultiMap<Double,Duple<Committee,Integer>>(200);
            // for (Committee c : committees) 
            for (int i = 0; i < committees.size(); ++i) {
                Committee c = committees.get(i);
                mostSimilarCommittees.put(
                Similarity.cosineSimilarity(row, c.centroid()), 
                new Duple<Committee,Integer>(c, i));
            }
            
            //         System.out.println("Most similar committees: " + 
            //                            mostSimilarCommittees);
            
            // while S is not empty {
            // let c be the most similar cluster to e
            for (Duple<Committee,Integer> p : mostSimilarCommittees.values()) {
                Committee c = p.x;
                Integer comId = p.y;
                
                SparseDoubleVector centroid = c.centroid();
                
                // if the similarity(e, c) < SIGMA, exit the loop
                if (Similarity.cosineSimilarity(copy, centroid) < 0) {
                    // NOTE: we intentionally don't exit the loop 
                    continue;
                }
                
                // if c is not similar to any cluster in C {
                boolean isSimilar = false;
                for (Integer committeeId : assignedClusters) {
                    Committee c2 = committees.get(committeeId);
                    if (Similarity.cosineSimilarity(c2.centroid(), centroid) 
                            >= softClusteringThresh) {
                        isSimilar = true;
                        break;
                    }
                }
                if (!isSimilar) {
                    // assign e to c
                    assignedClusters.add(comId);          
                    
                    // remove from e its features that overlap with the features of
                    // c; remove c from S
                    for (int i : centroid.getNonZeroIndices()) {
                        copy.set(i, 0);
                    }
                }
            }
            return assignedClusters;
        }
    }

    /**
     * Builds a set of candidate committees from the clusters formed by the
     * average-link clustering of the provided rows.
     *
     * @param avgLinkMergThresh the parameter used by HAC to determine when to
     *        stop merging clusters on the basis of their dissimilarity
     */
    public static List<CandidateCommittee> buildCommitteesForRow(
            Collection<Integer> rows, SparseMatrix sm, 
            double avgLinkMergeThresh) {
        // If there are no candidate rows, just return early.
        if (rows.size() == 0)
            return new ArrayList<CandidateCommittee>();

        double AVG_LINK_MERGE_THRESHOLD = .25; // ????

        // Convert the nearest neighbors to a matrix and cluster them using HAC
        // wth the mean link critera.
        List<SparseDoubleVector> v = new ArrayList<SparseDoubleVector>();
        for (Integer neighbor : rows)
            v.add(sm.getRowVector(neighbor));

        int[] assignments = HierarchicalAgglomerativeClustering.clusterRows(
            Matrices.asSparseMatrix(v), AVG_LINK_MERGE_THRESHOLD, 
            ClusterLinkage.MEAN_LINKAGE, SimType.COSINE);
        
        // Form clusters for all the rows
        Map<Integer,Set<Integer>> clusters = 
            new HashMap<Integer,Set<Integer>>();              

        int i = 0;
        for (Integer row : rows) {
            int clusterId = assignments[i];
            Set<Integer> cluster = clusters.get(clusterId);
            if (cluster == null) {
                cluster = new HashSet<Integer>();
                clusters.put(clusterId, cluster);
            }
            cluster.add(row);
            i++;
        }

        // Create the set of candidate committees from the clusters
        List<CandidateCommittee> candidates = 
            new ArrayList<CandidateCommittee>();

        for (Set<Integer> cluster : clusters.values()) 
            candidates.add(new CandidateCommittee(cluster, sm));
        
        return candidates;
    }

    /**
     * A decorator for indicating that a {@link CandidateCommittee} has been
     * reified as an actual committee and will be used for the final cluster
     * assignment.
     */
    private static class Committee {

        private final CandidateCommittee cc;

        public Committee(CandidateCommittee cc) {
            this.cc = cc;
        }
        
        /**
         * Returns the centroid.
         */
        public SparseDoubleVector centroid() {
            return cc.centroid();
        }

        public boolean equals(Object o) {
            return (o instanceof Committee) 
                && ((Committee)o).cc.equals(cc);
        }

        public int hashCode() {
            return cc.hashCode();
        }

        public String toString() {
            return cc.toString();
        }
    }
    

    /**
     * A simple struct for representing a proposed committee that has not been
     * finalized for clustering.  This class implements {@link Comparable} so
     * that an ordered, descending list of Committees can be made based on the
     * score (highest score first).
     */
    private static class CandidateCommittee 
            implements Comparable<CandidateCommittee> {

        /**
         * The set of rows that forms this cluster
         */
        private final Set<Integer> rows;

        /**
         * The centroid for this committee.
         */
        private final SparseDoubleVector centroid;

        /**
         * The score for this committee.
         */
        private final double score;

        /**
         * Constructs a new {@link CandidateCommittee}.
         */
        public CandidateCommittee(Set<Integer> rows,
                                  SparseMatrix sm) {
            this.rows = rows;
            // Compute the centroid
            centroid = new CompactSparseVector(sm.columns());
            double simSum = 0d;
            for (int r : rows) {
                SparseDoubleVector row = sm.getRowVector(r);
                VectorMath.add(centroid, row);

                for (int r2 : rows) {
                    if (r == r2)
                        continue;
                    simSum += Similarity.
                        cosineSimilarity(row, sm.getRowVector(r2));
                }
            }
            double denom = 1d / rows.size();
            for (int nz : centroid.getNonZeroIndices()) {
                centroid.set(nz, centroid.get(nz) / denom);
            }

            // From Phase 2
            //
            // 1.2) For each cluster discovered c compute the following score:
            //      |c| × avgsim(c), where |c| is the number of elements in c
            //      and avgsim(c) is the average pairwise similarity between
            //      elements in c.
            double avgSim = (rows.size() == 1) ? 0 :
                simSum / ((rows.size() * rows.size()) - rows.size());
            score = rows.size() * avgSim;
        }

        /**
         * Returns the centroid.
         */
        public SparseDoubleVector centroid() {
            return centroid;
        }

        /**
         * Returns the difference between another Committee's score and this
         * Committee's score.
         */
        public int compareTo(CandidateCommittee c) {             
            return -Double.compare(score, c.score);
        }

        public boolean equals(Object o) {
            if (o instanceof CandidateCommittee) {
                CandidateCommittee cc = (CandidateCommittee)o;
                return cc.rows.equals(rows);
            }
            return false;
        }

        public int hashCode() {
            return rows.hashCode();
        }

        /**
         * Returns the score for this candidate committee according to the
         * criteria defined in Phase 2, Part 1.
         */
        public double score() {
            return score;
        }

        public String toString() {
            return "Committee {rows=" + rows + ", score=" + score
                + ", centroid=" + centroid + "}";
        }
    }
}
