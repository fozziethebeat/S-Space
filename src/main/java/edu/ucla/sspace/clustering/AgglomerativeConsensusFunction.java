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

import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering.ClusterLink;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SymmetricMatrix;
import edu.ucla.sspace.matrix.SymmetricIntMatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * An implementation of the agglomerative consensus function that builds an
 * adjacency matrix recording the aggrement rates among a series of partitions
 * and then clusters that matrix using agglomerative clustering to create a
 * final consensus partition.
 *
 * This paper is based on the consensus matrix approach described in
 *
 * <ul>
 *    <li style="font-family:Garamond, Georgia, serif">Stefano Monti, Pablo
 *    Tamayo, Jill Mesirov, and Todd Golub.  Consensus Clustering.  <i>Machine
 *    Learning</i>.  Avaiable <a
 *    href="http://www.broadinstitute.org/mpr/publications/projects/Bioinformatics/consensus4pdflatex.pdf">here</a>
 *    </li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class AgglomerativeConsensusFunction implements ConsensusFunction {

    /**
     * {@inheritDoc}
     */
    public Partition consensus(List<Partition> partitions, int numClusters) {
        int numPoints = partitions.get(0).numPoints();
        // Create the consensus matrix and an indicator matrix tracking the
        // number of times each data pairing was observed.
        Matrix consensusMatrix = new SymmetricMatrix(numPoints, numPoints);
        //Matrix indicatorMatrix = new SymmetricIntMatrix(numPoints, numPoints);
        for (Partition partition : partitions) {
            // Throw an error if any partition reports a different number of
            // data points.  Note that not all partitions must report
            // assignments for every point, but they must all agree on the total
            // number of points.
            if (partition.numPoints() != numPoints)
                throw new IllegalArgumentException(
                        "All partitions must have the same number of total " +
                        "elements.  Given " + numPoints + " and " + 
                        partition.numPoints());

            // Create a list to keep track of all points observed by this
            // partition.
            List<Integer> allPoints = new ArrayList<Integer>();
            // Iterate over each cluster and update the consensus matrix with
            // the found data pairings.
            for (Set<Integer> cluster : partition.clusters()) {
                List<Integer> points = new ArrayList<Integer>(cluster);
                for (int i = 0; i < points.size(); ++i)
                    for (int j = i+1; j < points.size(); ++j)
                        consensusMatrix.add(points.get(i), points.get(j), 1.0);
                allPoints.addAll(cluster);
            }

            // Update the indicator matrix to note all possible pairings in the
            // partition.
            //for (int i = 0; i < allPoints.size(); ++i)
            //    for (int j = i+1; j < allPoints.size(); ++j)
            //        indicatorMatrix.add(allPoints.get(i), allPoints.get(j), 1);
        }

        // Normalize the consensus matrix such that every value is between 0 and
        // 1.
        for (int r = 0; r < consensusMatrix.rows(); ++r)
            for (int c = 0; c < r; ++c)
                consensusMatrix.set(
                        r, c, consensusMatrix.get(r, c) / partitions.size()); //indicatorMatrix.get(r, c));

        // Cluster the consensus matrix using agglomerative clustering and
        // return the result as a partition.
        return Partition.fromAssignments(
                NeighborChainAgglomerativeClustering.clusterAdjacencyMatrix(
                    consensusMatrix, ClusterLink.MEAN_LINK, numClusters));
    }
}
