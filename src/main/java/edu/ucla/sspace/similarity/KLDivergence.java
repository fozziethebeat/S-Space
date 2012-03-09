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
 * Returns the KL Divergence between any two probability distributions
 * represented as {@link Vector}s.
 *
 * </p>
 *
 * This metric is <i>not</i> symmetric.  Use the Jensen-Shannon Divergence for a
 * symmetric similarity measure between two probability distributions
 *
 * @author Keith Stevens
 */
public class KLDivergence implements SimilarityFunction {

    /**
     * Does nothing
     */
    public void setParams(double... arguments) { }

    /**
     * Returns {@code false} (is asymmetric).
     */    
    public boolean isSymmetric() {
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public double sim(DoubleVector v1, DoubleVector v2) {
        return Similarity.klDivergence(v1, v2);
    }

    /**
     * {@inheritDoc}
     */
    public double sim(IntegerVector v1, IntegerVector v2) {
        return Similarity.klDivergence(v1, v2);
    }

    /**
     * {@inheritDoc}
     */
    public double sim(Vector v1, Vector v2) {
        return Similarity.klDivergence(v1, v2);
    }
}
