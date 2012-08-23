/*
 * Copyright 2012 David Jurgens
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

package edu.ucla.sspace.matrix.factorization;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFactorization;


/**
 * A refinement of the {@link MatrixFactorization} interface for algorithms that
 * compute the Singular Value Decomposition.  This interface exposes the three
 * matrices that are generated as a result of the decomposition.
 *
 * @author David Jurgens
 */
public interface SingularValueDecomposition extends MatrixFactorization {

    /**
     * Returns the left factor matrix, <i>U</i>.
     *
     * @throws IllegalStateException if {@link #factorize(SparseMatrix,int)} has
     *         not been called prior.
     */
    Matrix getLeftVectors();

    /**
     * Returns the left factor matrix, <i>V</i>.
     *
     * @throws IllegalStateException if {@link #factorize(SparseMatrix,int)} has
     *         not been called prior.
     */
    Matrix getRightVectors();

    /**
     * Returns the diagonal matrix, &Sigma;, consisting of the singular values
     *
     * @throws IllegalStateException if {@link #factorize(SparseMatrix,int)} has
     *         not been called prior.
     */
    Matrix getSingularValues();
}

