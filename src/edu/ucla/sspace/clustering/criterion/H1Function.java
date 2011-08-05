/*
 * Copyright 2011 Keith Stevens 
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

package edu.ucla.sspace.clustering.criterion;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.ArrayList;
import java.util.List;


/**
 * This {@link HybridBaseFunction} uses the {@link E1Function} and the {@link
 * I1Function}.
 *
 * @author Keith Stevens
 */
public class H1Function extends HybridBaseFunction {

    /**
     * {@inheritDoc}
     */
    protected BaseFunction getInternalFunction() {
        return new I1Function(matrix, centroids, i1Costs, 
                              assignments, clusterSizes);
    }

    /**
     * {@inheritDoc}
     */
    protected BaseFunction getExternalFunction() {
        return new E1Function(matrix, centroids, e1Costs,
                              assignments, clusterSizes,
                              completeCentroid, simToComplete);
    }

    /**
     * {@inheritDoc}
     */
    public boolean maximize() {
        return true;
    }
}
