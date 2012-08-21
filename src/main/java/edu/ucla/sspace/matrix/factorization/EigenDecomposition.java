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

package edu.ucla.sspace.matrix.factorization;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.MatlabSparseMatrixBuilder;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.SparseMatrix;

import org.netlib.lapack.LAPACK;
import org.netlib.util.intW;


/**
 * This {@link MatrixFactorization} implements the <a
 * href="http://en.wikipedia.org/wiki/Eigendecomposition_of_a_matrix">Eigendecomposition</a>
 * of a {@link Matrix}.  This factorization will permit saving the top n eigen
 * vectors, which each correspond to the n largest eigen values found during
 * decomposition.  
 *
 * </p>
 *
 * Implementation note: this implementation currently does not gain any savings
 * from a {@link SparseMatrix} and will likely break when applied to really
 * large {@link Matrix} instances.
 *
 * @author Keith Stevens
 */
public class EigenDecomposition implements MatrixFactorization {

    /**
     * Constants used for linking into LAPACK.
     */
    private static final String VECTOR_AND_VALUE = "V";
    private static final String VALUE_RANGE = "I";
    private static final String UPPER_TRIANGULAR = "U";

    /**
     * The top n eigen values.
     */
    private Matrix dataClasses;

    /**
     * The top n eigen vectors, with each vector as a row.
     */
    private Matrix classFeatures;

    /**
     * {@inheritDoc}
     */
    public void factorize(MatrixFile m, int numDimensions) {
        factorize((SparseMatrix) m.load(), numDimensions);
    }

    /**
     * {@inheritDoc}
     */
    public void factorize(SparseMatrix sm, int numDimensions) {
        Matrix m = sm;
        factorize(m, numDimensions);
    }

    /**
     * {@inheritDoc}
     */
    public void factorize(Matrix m, int numDimensions) {
        LAPACK l = LAPACK.getInstance();
        // Create the data matrix to pass to LAPACK.  This must be a single one
        // dimensional matrix with all values.
        int n = m.rows();
        double[] data = new double[n*n];
        for (int r = 0; r < n; ++r)
            for (int c = 0; c < n; ++c)
                data[r*n+c] = m.get(r,c);

        // Create output integer objects.
        intW M = new intW(0);
        intW info = new intW(0);

        // Create the output space for the eigen values and the eigen vectors.
        double[] eigenValues = new double[n];
        double[] eigenVectors = new double[n*numDimensions];

        // Create a workspace for LAPACK.
        int lwork = 8*n;
        double[] work = new double[lwork];
        int[] iwork = new int[5*n];
        int[] ifail = new int[n];

        // Create the lower and upper bounds on the eigen value/vectors we want
        // to compute.  The nth value is the largest value and the 1st is the
        // smallest value.
        int lower = n-numDimensions+1;
        int upper = n;
        l.dsyevx(
                VECTOR_AND_VALUE, VALUE_RANGE, UPPER_TRIANGULAR, // Parameters
                n, data, n, // Input data and size
                lower, upper, lower, upper, // ranges
                0, M, // error parameters and output
                eigenValues, eigenVectors, n, // Vector/Value output
                work, lwork, iwork, ifail, info); // Extra stuff
        dataClasses = new ArrayMatrix(1, n, eigenValues);
        classFeatures = new ArrayMatrix(numDimensions, n, eigenVectors);
    }

    /**
     * {@inheritDoc}
     */
    public MatrixBuilder getBuilder() {
        return new MatlabSparseMatrixBuilder();
    }

    /**
     * {@inheritDoc}
     */
    public Matrix dataClasses() {
        return dataClasses;
    }

    /**
     * {@inheritDoc}
     */
    public Matrix classFeatures() { 
        return classFeatures;
    }
}
