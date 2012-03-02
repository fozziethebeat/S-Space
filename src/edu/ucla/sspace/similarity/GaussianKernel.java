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

package edu.ucla.sspace.similarity;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.Vector;


/**
 * Returns the Gaussing kernel weighting of two vectors using a parameter to
 * weight the distance between the two vectors.  {@link #setParams} takes in one
 * double argument which determines the exponent in the gaussian function.
 *
 * </p>
 *
 * This metric is symmetric.
 *
 * @author Keith Stevens
 */
public class GaussianKernel extends AbstractSymmetricSimilarityFunction {

    /**
     * Determines the exponent in the gaussian kernel.
     */
    private double gaussianKernelParam;

    /**
     * Sets the exponent weight of the gaussian kernel.
     */
    @Override
    public void setParams(double... params) {
        gaussianKernelParam = params[0];
    }

    /**
     * {@inheritDoc}
     */
    public double sim(DoubleVector v1, DoubleVector v2) {
        double dist = Similarity.euclideanDistance(v1, v2);
        return Math.pow(Math.E, -(dist / gaussianKernelParam));
    }

    /**
     * {@inheritDoc}
     */
    public double sim(IntegerVector v1, IntegerVector v2) {
        double dist = Similarity.euclideanDistance(v1, v2);
        return Math.pow(Math.E, -(dist / gaussianKernelParam));
    }

    /**
     * {@inheritDoc}
     */
    public double sim(Vector v1, Vector v2) {
        double dist = Similarity.euclideanDistance(v1, v2);
        return Math.pow(Math.E, -(dist / gaussianKernelParam));
    }
}
