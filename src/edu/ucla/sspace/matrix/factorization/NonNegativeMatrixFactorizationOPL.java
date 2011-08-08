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
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.SvdlibcSparseBinaryMatrixBuilder;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.Properties;


/**
 * A simple implementation of Non-Negative Matrix Factorization.  This is an
 * implementation of the Oblique Projected Landweber Method explained in 
 *
 *   <p style="font-family:Garamond, Georgia, serif"> R Zdunek and A. Cichocki,
 *     (2008), "Fast Nonnegative Matrix Factorization Algorithms Using Projected
 *     Gradient Approaches for Lage-Scale Problems," Intell. Neuroscience, vol.
 *     2008, <a href="http://www.hindawi.com/journals/cin/2008/939567/abs/">here</a>
 *   </p>
 *
 * NOTE: Does not work yet.  Returns 0 matrices.
 *
 * @author Keith Stevens
 */
public class NonNegativeMatrixFactorizationOPL implements MatrixFactorization {

    /**
     * The base property prefix.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.matrix.factorize.NonNegativeMatrixFactorizationOPL";

    /**
     * The System Property used to define the number of gradient iterations used
     * when decomposing a matrix.
     */
    public static final String ITERATIONS =
        PROPERTY_PREFIX + ".numIterations";

    /**
     * By default, use this many iterations.
     */
    public static final String DEFAULT_ITERATIONS = "100";

    /**
     * The data point to latent variable matrix.
     */
    private Matrix A;

    /**
     * The latent variable to feature value matrix.
     */
    private Matrix X;

    /**
     * The number of dimensions in the matrix that is being decomposed.
     */
    private int numDimensions;

    /**
     * The number of iterations to use when decomposing a {@link Matrix}
     */
    private int numIterations;

    /**
     * Instantiates a new {@link NonNegativeMatrixFactorizationOPL} that uses
     * the system default {@link Properties} to determnine the number of
     * iterations to use when decomposing matrices.
     */
    public NonNegativeMatrixFactorizationOPL() {
        this(System.getProperties());
    }

    /**
     * Instantiates a new {@link NonNegativeMatrixFactorizationOPL} that uses
     * the provided {@link Properties} to determnine the number of iterations to
     * use when decomposing matrices.
     */
    public NonNegativeMatrixFactorizationOPL(Properties props) {
        numIterations = Integer.parseInt(props.getProperty(
                    ITERATIONS, DEFAULT_ITERATIONS));
    }

    /**
     * Instantiates a new {@link NonNegativeMatrixFactorizationOPL} instance
     * which will use {@code numIterations} to compute the decomposed matrices.
     */
    public NonNegativeMatrixFactorizationOPL(int numIterations) {
        this.numIterations = numIterations;
    }

    /**
     * {@inheritDoc}
     */
    public void factorize(MatrixFile mFile, int numDimensions) {
        factorize((SparseMatrix) mFile.load(), numDimensions);
    }

    /**
     * {@inheritDoc}
     */
    public void factorize(SparseMatrix m, int numDimensions) {
        if (numDimensions >= m.columns() ||
            numDimensions >= m.rows())
            throw new IllegalArgumentException(
                    "Cannot factorize with more dimensions than there are " +
                    "rows or columns");
        this.numDimensions = numDimensions;
        A = new ArrayMatrix(m.rows(), numDimensions);
        initialize(A);
        X = new ArrayMatrix(numDimensions, m.columns());
        initialize(X);

        for (int i = 0; i < numIterations; ++i) {
            updateX(computeGofX(m), computeLearningRateX());
            updateA(computeGofA(m), computeLearningRateA());
        }
    }

    /**
     * {@inheritDoc}
     */
    public Matrix dataClasses() {
        return A;
    }

    /**
     * {@inheritDoc}
     */
    public Matrix classFeatures() {
        return X;
    }

    /**
     * {@inheritDoc}
     */
    public MatrixBuilder getBuilder() {
        return new SvdlibcSparseBinaryMatrixBuilder();
    }

    /**
     * Updates the values in {@code X} based on the difference found in {@code
     * G} with each value scaled by the {@code learningRate}.
     */
    private void updateX(Matrix G, double[] learningRate) {
        for (int k = 0; k < X.rows(); ++k)
            for (int c = 0; c < X.columns(); ++c)
                X.set(k, c, X.get(k, c) - learningRate[k] * G.get(k, c));
        makeNonZero(X);
    }

    /**
     * Updates the values in {@code A} based on the difference found in {@code
     * G} with each value scaled by the {@code learningRate}.
     */
    private void updateA(Matrix G, double[] learningRate) {
        for (int r = 0; r < A.rows(); ++r)
            for (int k = 0; k < A.columns(); ++k)
                A.set(r, k, A.get(r, k) - learningRate[k] * G.get(r, k));
        makeNonZero(A);
    }

    /**
     * Sets any negative values of {@code m} to zero.
     */
    public static void makeNonZero(Matrix m) {
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c)
                if (m.get(r,c) < 0d)
                    m.set(r,c,0);
    }

    /**
     * Initializes every value in {@code m} to be a random value between 0 and
     * 1, inclusive.
     */
    public static void initialize(Matrix m) {
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c)
                m.set(r,c,Math.random());
    }

    /**
     * Computes the following Matrix difference:
     * </br>
     * A' (AX - Y)
     * </br>
     * Without ever completely computing (AX-Y).  This is done by computing
     * (AX-Y) on a row by row basis and then multiplying the row of (AX-Y) by
     * the relevant parts of A' to update the final matrix.
     */
    private Matrix computeGofX(SparseMatrix Y) {
        Matrix G = new ArrayMatrix(numDimensions, X.columns());
        // Iterate through each row of Y, and thux AX-Y.  For each row, compute
        // the difference between the two matrics.  Then multiply the values in
        // the correspond row of A against this difference matrix.  Instead of
        // computing the inner product of A' and (AX-Y), we update each k,m
        // value in G by iterating through the columns of A.
        for (int r = 0; r < Y.rows(); ++r) {
            // Get the row vector of Y.
            SparseDoubleVector v = Y.getRowVector(r);
            double[] vDiff = new double[v.length()];
            // Compute the difference between the row vector of AX and the row
            // vector of Y.  This is the straightforward dot product between A_i
            // and X'_i.
            for (int c = 0; c < X.columns(); c++) {
                double sum = 0;
                for (int k = 0; k < A.columns(); ++k)
                    sum += A.get(r, k) * X.get(k, c);
                vDiff[c] = sum - v.get(c);
            }

            // Now get the row vector A_i and for each column k, multiply it
            // against each column c in vDiff to get the difference in the
            // gradient G_{k, c}.
            for (int k = 0; k < A.columns(); ++k)
                for (int c = 0; c < X.columns(); ++c)
                    G.set(k, c, G.get(k, c) + A.get(r, k) * vDiff[c]);
        }
        return G;
    }

    /**
     * Computes the following gradient:
     * </br>
     * (AX-Y)X'
     * </br>
     * Without every completely computing (AX-Y).  This computation is
     * straightforward as the rows of (AX-Y) can be fully utilized in the naive
     * matrix multiplication against X' without any inverted traversals.
     */
    private Matrix computeGofA(SparseMatrix Y) {
        Matrix G = new ArrayMatrix(A.rows(), numDimensions);
        // Iterate through each row of Y, and thux AX-Y.  For each row, compute
        // the difference between the two matrics.  Then multiply the values in
        // the correspond column of X' against this difference matrix.  
        for (int r = 0; r < Y.rows(); ++r) {
            // Get the row vector of Y.
            SparseDoubleVector v = Y.getRowVector(r);
            double[] vDiff = new double[v.length()];
            // Compute the difference between the row vector of AX and the row
            // vector of Y.  This is the straightforward dot product between A_i
            // and X'_i.
            for (int c = 0; c < X.columns(); c++) {
                double sum = 0;
                for (int k = 0; k < A.columns(); ++k)
                    sum += A.get(r, k) * X.get(k, c);
                vDiff[c] = sum - v.get(c);
            }

            for (int k = 0; k < X.rows(); ++k) {
                double sum = 0;
                for (int c = 0; c < X.columns(); ++c)
                    sum += vDiff[c] * X.get(k, c);
                G.set(r, k, sum);
            }
        }
        return G;
    }

    /**
     * Computes the learning rate for updating the X matrix.  This is simply the
     * diagonal of
     * </br>
     * 2/(A'A)
     * </br>
     */
    private double[] computeLearningRateX() {
        double[] learningRateX = new double[numDimensions];
        for (int r = 0; r < A.rows(); ++r)
            for (int c = 0; c < A.columns(); ++c)
                learningRateX[c] += Math.pow(A.get(r, c), 2);
        for (int k = 0; k < numDimensions; ++k) {
            System.out.printf("%f ", learningRateX[k]);
            learningRateX[k] = 2.0 / (learningRateX[k] + .000000001);
        }
        System.out.println();

        return learningRateX;
    }

    /**
     * Computes the learning rate for updating the A matrix.  This is simply the
     * diagonal of
     * </br>
     * 2/(XX')
     * </br>
     */
    private double[] computeLearningRateA() {
        double[] learningRateA = new double[numDimensions];
        System.out.println("computeLearningRateA");
        for (int r = 0; r < X.rows(); ++r) {
            double sum = 0;
            for (int c = 0; c < X.columns(); ++c) {
                System.out.printf("%f ", Math.pow(X.get(r,c), 2));
                sum += Math.pow(X.get(r, c), 2);
            }
            System.out.println();
            learningRateA[r] = sum + .000000001;
        }

        for (int k = 0; k < numDimensions; ++k)
            learningRateA[k] = 2.0 / learningRateA[k];
        return learningRateA;
    }
}

