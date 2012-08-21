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

import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering;
import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering.ClusterLink;
import edu.ucla.sspace.matrix.Matrix;

import java.util.Properties;


/**
 * A simple wrapper around {@code NeighborChainAgglomerativeClustering} that
 * treats the input matrix as an adjacency matrix connecting data points within
 * a graph.  This method defaults to using the {@link
 * NeighborChainAgglomerativeClustering#MEAN_LINK} {@link
 * NeighborChainAgglomerativeClustering#ClusterLink} method.
 *
 * </p>
 *
 * This method will run in O(N^2) time.
 *
 * @author Keith Stevens
 */
public class GraphHac implements Clustering {

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix adj, int k, Properties props) {
        return NeighborChainAgglomerativeClustering.clusterAdjacencyMatrix(
                adj, ClusterLink.MEAN_LINK, k);
    }

    /**
     * Unsupported operation.
     *
     * @throws new UnsupportedOperationException
     */
    public Assignments cluster(Matrix adj, Properties props) {
        throw new UnsupportedOperationException(
                "Cannot cluster without setting the number of clusters");
    }
}
