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

package edu.ucla.sspace.matrix.factorization;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.DiagonalMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.SparseMatrix;


/**
 * An abstract base class for any Singular Value Decomposition implementation.
 * This base class simply provides protected {@link Matrix} data members
 * corresponding to the U,S,and V matrices resulting from the SVD.  It also
 * automatically computes U*S when calling {@link dataClasses} and S*Vt when
 * calling {@link classFeatures}, both of which are done lazily and then cached
 * so that the multiplication is done only once.
 *
 * </p>
 *
 * Subclasses need only implement {@link factorize(MatrixFile, int)} and {@link
 * factorization(SparseMatrix, int)}, and {@link getBuilder()}.
 *
 * @author Keith Stevens
 */
public abstract class AbstractSvd implements SingularValueDecomposition {

    /**
     * The class by feature type matrix.
     */
    protected Matrix classFeatures;

    /**
     * The left vectors of the SVD decomposition
     */
    protected Matrix U;

    /**
     * The right vectors of the SVD decomposition
     */
    protected Matrix V;

    /**
     * Set to true when {@code classFeatures} has been accessed the first time
     * to mark that the singular values have been applied to each value in the
     * matrix.
     */
    protected boolean scaledClassFeatures;

    /**
     * The data point by class matrix.
     */
    protected Matrix dataClasses;

    /**
     * Set to true when {@code dataClasses} has been accessed the first time to
     * mark that the singular values have been applied to each value in the
     * matrix.
     */
    protected boolean scaledDataClasses;

    /**
     * The singular values computed during factorization.
     */
    protected double[] singularValues;

    /**
     * {@inheritDoc}
     */
    public Matrix dataClasses() {
        if (!scaledDataClasses) {
            scaledDataClasses = true;
            dataClasses = new ArrayMatrix(U.rows(), U.columns());
            // Weight the values in the data point space by the singular
            // values.
            //
            // REMINDER: when the RowScaledMatrix class is merged in with
            // the trunk, this code should be replaced.
            for (int r = 0; r < dataClasses.rows(); ++r)
                for (int c = 0; c < dataClasses.columns(); ++c)
                    dataClasses.set(r, c, U.get(r, c) * 
                                          singularValues[c]);
        }

        return dataClasses;
    }

    /**
     * {@inheritDoc}
     */
    public Matrix classFeatures() {
        if (!scaledClassFeatures) {
            scaledClassFeatures = true;
            classFeatures = new ArrayMatrix(V.rows(), V.columns());
            // Weight the values in the document space by the singular
            // values.
            //
            // REMINDER: when the RowScaledMatrix class is merged in with
            // the trunk, this code should be replaced.
            for (int r = 0; r < classFeatures.rows(); ++r)
                for (int c = 0; c < classFeatures.columns(); ++c)
                    classFeatures.set(r, c, V.get(r, c) * 
                                            singularValues[r]);
        }

        return classFeatures;
    }


    /**
     * {@inheritDoc}
     */
    public Matrix getLeftVectors() {
        if (U == null)
            throw new IllegalStateException(
                "The matrix has not been factorized yet");
        // NOTE: make this read-only?
        return U;
    }

    /**
     * {@inheritDoc}
     */
    public Matrix getRightVectors() {
        if (V == null)
            throw new IllegalStateException(
                "The matrix has not been factorized yet");
        // NOTE: make this read-only?
        return V;
    }

    /**
     * {@inheritDoc}
     */
    public Matrix getSingularValues() {
        if (singularValues == null)
            throw new IllegalStateException(
                "The matrix has not been factorized yet");
        // NOTE: make this read-only?
        return new DiagonalMatrix(singularValues);
    }

    /**
     * Returns a double array of the singular values.
     */
    public double[] singularValues() {
        return singularValues;
    }
}
