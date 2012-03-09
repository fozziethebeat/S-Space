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

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.Vector;


/**
 * An Interface for any similarity metric between two {@link Vectors}.  Methods
 * should provide efficient support for sparse vectors if an implementation is
 * feasible as these metrics are heavily used in some algorithms.
 * Metrics should note via {@link #isSymmetric} whether or not they are
 * symmetic, whereby {@code method.sim(A,B) == method.sim(B,A)}  always
 * holds true for any {@code A,B}.  If the metric takes in any parameters,
 * such as weights, they can use the {@link #setParams} method, which can take
 * in any number of {@code double} arguments.  Methods should also provide a no
 * argument constructor that has reasonable default values, or loads values from
 * system properties.
 *
 * @author Keith Stevens
 */
public interface SimilarityFunction {

    /**
     * Sets an double parameters, such as weights, for this {@link
     * SimilarityFunction}.
     */
    void setParams(double... arguments);

    /**
     * Returns true if {@code sim(A,B) == sim(B,A)} is true for any {@code A},
     * {@code B}.
     */
    boolean isSymmetric();

    /**
     * Returns the similarity between {@code v1} and {@code v2}.  If {@link
     * #isSymmetric} is false, the ordering <b>does</b> matter.
     */
    double sim(DoubleVector v1, DoubleVector v2);

    /**
     * Returns the similarity between {@code v1} and {@code v2}.  If {@link
     * #isSymmetric} is false, the ordering <b>does</b> matter.
     */
    double sim(IntegerVector v1, IntegerVector v2);

    /**
     * Returns the similarity between {@code v1} and {@code v2}.  If {@link
     * #isSymmetric} is false, the ordering <b>does</b> matter.
     */
    double sim(Vector v1, Vector v2);
}
