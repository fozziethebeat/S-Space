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

import edu.ucla.sspace.matrix.AffinityMatrixCreator;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.NearestNeighborAffinityMatrixCreator;

import edu.ucla.sspace.sim.CosineSimilarity;
import edu.ucla.sspace.sim.SimilarityFunction;

import java.util.Properties;


/**
 * @author Keith Stevens
 */
public class DataMatrixLinkClustering implements Clustering {

    private final AffinityMatrixCreator creator;

    private final LinkClustering linkCluster;

    public DataMatrixLinkClustering() {
        this(createDefaultAffinityMatrixCreator());
    }

    public DataMatrixLinkClustering(AffinityMatrixCreator creator) {
        this.creator = creator;
        this.linkCluster = new LinkClustering(); 
    }

    public static AffinityMatrixCreator createDefaultAffinityMatrixCreator() {
        SimilarityFunction simFunc = new CosineSimilarity();
        AffinityMatrixCreator creator =
            new NearestNeighborAffinityMatrixCreator();
        creator.setParams(10);
        creator.setFunctions(simFunc, simFunc);
        return creator;
    }

    public Assignments cluster(Matrix matrix, 
                               int numClusters,
                               Properties props) {
        MatrixFile affinityMatrix = creator.calculate(matrix);
        return linkCluster.cluster(affinityMatrix.load(), numClusters, props);
    }

    public Assignments cluster(Matrix matrix, Properties props) { 
        MatrixFile affinityMatrix = creator.calculate(matrix);
        return linkCluster.cluster(affinityMatrix.load(), props);
    }
}
