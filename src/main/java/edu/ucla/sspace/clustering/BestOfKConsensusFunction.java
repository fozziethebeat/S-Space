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

import java.util.List;


/**
 * An implementation of the Best of K {@link ConsensusFunction} which finds the
 * input {@link Partition} that is most similar to all given {@link Partition}s.
 * Formally, this returns the {@link Partition} that optimizes a {@link
 * PartitionComparison} with respect to all other {@link Partition}s.  If the
 * given {@link PartitionComparison} is a distance, this will minimize the
 * distance, otherwise it will maximize the similarity.  This method is based on
 * <ul>
 *   <li>
 *    <li style="font-family:Garamond, Georgia, serif">Vladimir Filkov and
 *    Steven Skiena.  Integerating Microarray Data by Consensus Clustering. <i>
 *    Proceedings of the 15th IEEE Internation Conference on Tools with
 *    Artificial Intelligence.</i>.  Available <a
 *    href="http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=1250220&tag=1">here</a></li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class BestOfKConsensusFunction implements ConsensusFunction {

    /**
     * The partition function used to compare two {@link Partition}s.
     */
    private final PartitionComparison comp;

    /**
     * Constructs a new {@link BestOfKConsensusFunction} using the {@link
     * RandDistance} comparison.
     */
    public BestOfKConsensusFunction() {
        this(new RandDistance());
    }

    /**
     * Constructs a new {@link BestOfKConsensusFunction} using the given {@link
     * PartitionComparison} function.
     */
    public BestOfKConsensusFunction(PartitionComparison comp) {
        this.comp = comp;
    }

    public Partition consensus(List<Partition> partitions, int numClusters) {
        Partition best = null;
        double bestScore = (comp.isDistance())
            ? Double.MAX_VALUE 
            : -Double.MAX_VALUE;
        for (int i = 0; i < partitions.size(); ++i) {
            Partition curr = partitions.get(i);
            double totalScore = 0;
            for (int j = 0; j < partitions.size(); ++j)
                if (i != j) 
                    totalScore += comp.compare(curr, partitions.get(j));
            if ((comp.isDistance() && totalScore <= bestScore) ||
                (!comp.isDistance() && totalScore >= bestScore)) {
                best = curr;
                bestScore = totalScore;
            }
        }

        return Partition.copyOf(best);
    }
}
