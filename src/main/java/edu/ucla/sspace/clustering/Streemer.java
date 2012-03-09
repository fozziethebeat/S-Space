/*
 * Copyright 2012 David Jurgens
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

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.similarity.SimilarityFunction;

import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.TroveIntSet;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.VectorMath;
import edu.ucla.sspace.vector.Vectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


/**
 * An implementation of the Streemer (<b>Stre</b>aming <b>EM</b>) algorithm for
 * foreground/background clustering.  The full algorithm is described in 
 *
 * <ul> <li> V. Kandylas, S. P. Upham, and L. Ungar.  Finding cohesive clusters
 *   for analyzing knowledge communities.  In <i>Proceedings of the Seventh IEEE
 *   Conference on Data Mining (ICDM)</i>.  IEEE Computer Society.  Omaha, NE.
 *   Available online <a
 *   href="http://citeseerx.ist.psu.edu/viewdoc/download;?doi=10.1.1.146.2954&rep=rep1&type=pdf">here</a>
 *   </li> </ul>
 *
 * </p>
 *
 *
 * @author David Jurgens
 */
public class Streemer implements Clustering {

    /**
     * Clusters the set of rows in the given {@code Matrix} without a specified
     * number of clusters 
     *
     * @param matrix {@inheritDoc}
     * @param props {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, Properties props) {
        throw new Error();
    }

    /**
     * Clusters the set of rows in the given {@code Matrix} into the specified
     * number of clusters.  The set of cluster assignments are returned for each
     * row in the matrix.
     *
     * @param matrix {@inheritDoc}
     * @param numClusters {@inheritDoc}
     * @param props {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, int numClusters, Properties props) {
        throw new Error();
    }

    public Assignments cluster(Matrix matrix, int numClusters, 
                               double backgroundClusterPerc, double similarityThreshold,
                               int minClusterSize, SimilarityFunction simFunc) {

        /*
         * FIRST PASS: generate a list of candidate clusters
         */        
        int rows = matrix.rows();
        List<CandidateCluster> candidateClusters = 
            new ArrayList<CandidateCluster>();

        // Base case: the first row gets its own cluster
        CandidateCluster first = new CandidateCluster();
        first.add(0, matrix.getRowVector(0));
        candidateClusters.add(first);

        // Loop through all remaining rows, either assigning them to the most
        // similar cluster, or splitting them off into their own cluster
        for (int r = 1; r < rows; ++r) {
            DoubleVector row = matrix.getRowVector(r);
            CandidateCluster mostSim = null;
            double highestSim = -1d;
            for (CandidateCluster cc : candidateClusters) {
                double sim = simFunc.sim(cc.centerOfMass(), row);
                if (sim > highestSim) {
                    mostSim = cc;
                    highestSim = sim;
                }                    
            }
            
            if (highestSim < similarityThreshold) {
                CandidateCluster cc = new CandidateCluster();
                cc.add(r, row);
                candidateClusters.add(cc);
            }
            else {
                mostSim.add(r, row);
            }
        }        

        /*
         * Generate the list of final clusters
         */
        List<CandidateCluster> finalClusters = 
            new ArrayList<CandidateCluster>();

        for (CandidateCluster cc : candidateClusters) {
            if (cc.size() < minClusterSize) 
                continue;
            
            double maxSim = -1;
            for (CandidateCluster cc2 : candidateClusters) {
                if (cc == cc2)
                    continue;
                double sim = simFunc.sim(cc.centerOfMass(), cc2.centerOfMass());
                if (sim > maxSim)
                    maxSim = sim;
            }
            if (maxSim < similarityThreshold)
                finalClusters.add(cc);
            // Compute the cluster cohesiveness for all clusters with sim >
            // threshold, adding the cluster with the highest to the final set
            else {
                CandidateCluster mostCohesive = null;
                double maxCohesiveness = -1;
                for (CandidateCluster cc2 : candidateClusters) {
                    if (cc == cc2)
                        continue;
                    double sim = simFunc.sim(cc.centerOfMass(), cc2.centerOfMass());
                    if (sim < similarityThreshold) 
                        continue;

                    IntIterator iter = cc2.indices().iterator();
                    double similaritySum = 0;
                    while (iter.hasNext()) {
                        DoubleVector v = matrix.getRowVector(iter.next());
                        similaritySum += simFunc.sim(cc2.centerOfMass(), v);
                    }
                    double avgSim = similaritySum / cc2.size();
                    
                    if (avgSim > maxCohesiveness) {
                        maxCohesiveness = avgSim;
                        mostCohesive = cc2;
                    }
                }
                finalClusters.add(mostCohesive);
            }
        }

        
        /*
         * OPTIONAL STEP: if we're inducing the number of clusters, keep the set
         * of final clusters as is; otherwise, ensure that the size of the set
         * is equal to the requested number of clusters
         */
        // TODO!

        int foundClusters = finalClusters.size();

        /*
         * THIRD PASS: compute the similarity distribution
         */
        double[] similarities = new double[rows];
        int[] clusterAssignments = new int[rows];
        for (int r = 0; r < rows; ++r) {
            DoubleVector v = matrix.getRowVector(r);
            double highestSim = -1;
            int mostSim = -1;
            for (int j = 0; j < foundClusters; ++j) {
                CandidateCluster cc = finalClusters.get(j);
                double sim = simFunc.sim(v, cc.centerOfMass());
                if (sim > highestSim) {
                    mostSim = j;
                    highestSim = sim;
                }
            }
            similarities[r] = highestSim;
            clusterAssignments[r] = mostSim;
        }

        // Create a copy of the similarities, which we'll sort and then use to
        // determine what the cutoff bound is for the background cluster
        double[] copy = Arrays.copyOf(similarities, similarities.length);
        Arrays.sort(copy);
        double cutoffSim = copy[(int)(copy.length * backgroundClusterPerc)];

        // Any data point whose similarity is less than the cutoff is relegated
        // to the background cluster.  (The next cluster id is the number of
        // clusters)
        int backgroundClusterId = foundClusters;

        Assignment[] assignments = new Assignment[rows];
        for (int i = 0; i < similarities.length; ++i) {
            if (similarities[i] < cutoffSim)
                clusterAssignments[i] = backgroundClusterId;
            assignments[i] = new HardAssignment(clusterAssignments[i]);
        }

        return new Assignments(foundClusters + 1, assignments, matrix);        
    }
}
