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

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SymmetricIntMatrix;

import java.util.List;


/**
 * This {@link ConsensusFunction} optimizes the total {@link RandDistance} {@link 
 * PartitionComparison} between a consensus partition and all given {@link
 * Partition}s by iteratively improving an initial solution.  This method is
 * based on 
 * <ul>
 *   <li>
 *    <li style="font-family:Garamond, Georgia, serif">Vladimir Filkov and
 *    Steven Skiena.  Integerating Microarray Data by Consensus Clustering. <i>
 *    Proceedings of the 15th IEEE Internation Conference on Tools with
 *    Artificial Intelligence.</i>.  Available <a
 *    href="http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=1250220&tag=1">here</a></li>
 * </ul>
 *
 * </p>
 *
 * This method uses another, given {@link ConsensusFunction} to estimate an
 * initial consensus partition and then updates that partition such that it
 * becomes more similar to the other partitions.
 *
 * @author Keith Stevens
 */
public class BestOneElementMoveConsensusFunction implements ConsensusFunction {

    /**
     * The internal seed method.
     */
    private final ConsensusFunction seedMethod;

    /**
     * The maximum number of partitions to update for.
     */
    private final int numIterations;

    /**
     * Creates a new {@link BestOneElementMoveConsensusFunction} that uses the
     * {@link AgglomerativeConsensusFunction} as the seed method and 200000
     * maximum iterations.
     */
    public BestOneElementMoveConsensusFunction() {
        this(new AgglomerativeConsensusFunction(), 200000);
    }
    
    /**
     * Creates a new {@link BestOneElementMoveConsensusFunction} that uses the
     * given {@link ConsensusFunction} as the seed method and given maximum
     * iterations.
     */
    public BestOneElementMoveConsensusFunction(ConsensusFunction seedMethod, 
                                               int numIterations) {
        this.seedMethod = seedMethod;
        this.numIterations = numIterations;
    }

    /**
     * {@inheritDoc}
     */
    public Partition consensus(List<Partition> partitions, int numClusters) {
        Partition best = seedMethod.consensus(partitions, numClusters);
        int numPoints = best.numPoints();

        // Compute the cost of co-clustered points.  
        Matrix coClusterCost = new SymmetricIntMatrix(numPoints, numPoints);
        for (int r = 0; r < numPoints; ++r)
            for (int c = r+1; c < numPoints; ++c)
                coClusterCost.add(r, c, -2);

        // Create the array that tracks the best one element move for each data
        // point.  
        Move[] bestMoves = new Move[numPoints];

        // Compute the cost of moving each data point to a new cluster.
        Matrix moveCost = new ArrayMatrix(numPoints, numClusters);
        for (int r = 0; r < numPoints; ++r) {
            double bestCost = Double.MAX_VALUE;
            int bestMove = 0;
            for (int c = 0; c < numClusters; ++c) {
                double cost = 0;
                for (int point : best.clusters().get(c))
                    if (point != r)
                        cost += coClusterCost.get(r, point);
                if (cost < bestCost) {
                    bestCost = cost;
                    bestMove = c;
                }
                moveCost.set(r, c, cost);
            }
            bestMoves[r] = new Move(bestCost, bestMove);
        }

        for (int i = 0; i < numIterations; ++i) {
            // Get the best one element move by finding the element with the
            // largest difference from it's current cluster to it's best next
            // cluster.
            double bestDelta = 0;
            int bestPoint = 0;
            for (int r = 0; r < numPoints; ++r) {
                double delta = moveCost.get(r, best.assignments()[r]) - bestMoves[r].cost;
                if (delta > bestDelta) {
                    bestDelta = delta;
                    bestPoint = r;
                }
            }

            // Extract the difference and the point for convenience.
            int oldCluster = best.assignments()[bestPoint];
            int newCluster = bestMoves[bestPoint].cluster;

            // Check that this move makes a difference.  If it doesn't, we can
            // exit early.
            if (bestDelta <= 0 || oldCluster == newCluster)
                return best;

            // For each element, modify the cost of the move and update the best
            // move costs.
            for (int r = 0; r < numPoints; ++r) {
                // Update the cost for the old cluster.
                moveCost.add(r, oldCluster, -coClusterCost.get(r, bestPoint));
                // Update the cost for the new cluster.
                moveCost.add(r, newCluster, coClusterCost.get(r, bestPoint));

                // Update the best move cost for this element if needed.
                if (moveCost.get(r, oldCluster) < bestMoves[r].cost)
                    bestMoves[r] = new Move(moveCost.get(r, oldCluster), oldCluster);
                if (moveCost.get(r, newCluster) < bestMoves[r].cost)
                    bestMoves[r] = new Move(moveCost.get(r, newCluster), newCluster);
            }
        }
        return best;
    }

    /**
     * A private struct recording the cost and new cluster id of the best one
     * element move available.
     */
    private static class Move {
        public double cost;
        public int cluster;

        public Move(double cost, int cluster) {
            this.cost = cost;
            this.cluster = cluster;
        }
    }
}
