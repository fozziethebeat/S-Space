/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
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

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.Properties;


/**
 * An implementation of the Chinese Whispers graph clustering algorithm.  This
 * is a simplified model of the Markov Clustering process, which tracks the flow
 * through a graph and assigns clusters to small highly connected components.  
 * This algorithm automaticall determines the number of clusters.  It requires
 * only one parameter: the number of iterations spent traversing the network.
 * By default, this value is set to 100.
 *
 * </p>
 *
 * For theoretical details see the following paper:
 *
 *   <li> Chris Biemann.  Chinese Whispers - an Efficient Clustering Algotirhm
 *   and its Application to Natural Language Processing Problems.  Workshop on
 *   TextGraphs, pages 73-80 (June 2006).  Available online <a
 *   href="http://wortschatz.uni-leipzig.de/~cbiemann/pub/2006/BiemannTextGraph06.pdf">here</a>.
 *   </li>
 *
 * 
 * @author Keith Stevens
 */
public class ChineseWhispers extends AbstractGraphClustering {

    /**
     * The number of iterations for procesing the network flow.
     */
    private final int numIterations;
    
    /**
     * Creates a new ChineseWhispers object with the given number of iterations.
     *
     * @param numIterations The number of iterations to use for traversing the
     *        network.
     */
    public ChineseWhispers(int numIterations) {
        this.numIterations = numIterations;
    }

    /**
     * Creates a new ChineseWhispers object with 100 iterations.
     */
    public ChineseWhispers() {
        this(100);
    }

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix adjacency, Properties props) {
        int[] assignments = new int[adjacency.rows()];
        for (int i = 0; i < assignments.length; ++i)
            assignments[i] = i;
        for (int i = 0; i < numIterations; ++i) {
            int[] newAssignments = new int[adjacency.rows()];
            for (int c = 0; c < newAssignments.length; ++c)
                newAssignments[c] = maxIndex(
                        adjacency.getRowVector(assignments[c]));
            assignments = newAssignments;
        }

        int numClusters = 0;
        int[] finalAssignments = new int[adjacency.rows()];
        int[] clusterMap = new int[adjacency.rows()];
        for (int i = 0; i < assignments.length; ++i) {
            int rawAssignment = assignments[i];
            if (clusterMap[rawAssignment] == 0)
                clusterMap[rawAssignment] = ++numClusters;
            finalAssignments[i] = clusterMap[rawAssignment]-1;
        }

        return new Assignments(numClusters, finalAssignments, null);
    }

    /**
     * Returns the index of the feature with the highest value in the given
     * {@link DoubleVector}.
     */
    private static int maxIndex(DoubleVector row) {
        int bestIndex = 0;
        double bestValue = 0.0;

        if (row instanceof SparseDoubleVector) {
            SparseDoubleVector sv = (SparseDoubleVector) row;
            for (int index : sv.getNonZeroIndices()) {
                double value = sv.get(index);
                if (value >= bestValue) {
                    bestValue = value;
                    bestIndex = index;
                }
            }
        } else {
            for (int index = 0; index < row.length(); ++index) {
                double value = row.get(index);
                if (value >= bestValue) {
                    bestValue = value;
                    bestIndex = index;
                }
            }
        }

        return bestIndex;
    }
}
