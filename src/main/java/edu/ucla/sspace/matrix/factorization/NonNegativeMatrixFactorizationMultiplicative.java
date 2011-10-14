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
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.matrix.SvdlibcSparseBinaryMatrixBuilder;

import java.util.Properties;
import java.util.logging.Logger;


/**
 * The most basic Non-Negative Matrix Factorization implementation.  This
 * implementation uses the original multiplicitive update rules for the two
 * decomposed matrices.  If we define our data {@link Matrix} as {@code A}, then
 * A can be approximated by
 * </br>
 *   WH
 * </br>
 * {@code W} and {@code H} are initialized with random uniform values from 0 to
 * 1.  They are both updated iteratively using the following rules:
 * </br>
 *  W <- W .* (AHt) ./ (W(HHt))
 *  H <- H .* (WtA) ./ ((WtW)H)
 * </br>
 * Where {@code t} stands for the transpose.
 *
 * </p>
 * These updates are made by fixing one of the matrices and updating the other
 * for a small number of iterations and then updating the other while keeping
 * the first fixed.  This alternating update is done for a large number of
 * iterations.
 *   
 * @author Keith Stevens
 */
public class NonNegativeMatrixFactorizationMultiplicative
        implements MatrixFactorization {

    private static final Logger LOG = 
        Logger.getLogger(NonNegativeMatrixFactorizationMultiplicative.class.getName());

    /**
     * The base property prefix.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.matrix.factorize.NonNegativeMatrixFactorizationMultiplicative";

    /**
     * The System Property used to define the number outer multiplicative
     * iterations to use when decomposing a matrix.
     */
    public static final String OUTER_ITERATIONS =
        PROPERTY_PREFIX + ".outerIterations";

    /**
     * The System Property used to define the number inner multiplicative
     * iterations to use when decomposing a matrix.
     */
    public static final String INNER_ITERATIONS =
        PROPERTY_PREFIX + ".innerIterations";

    /**
     * By default, use this many outer iterations.
     */
    public static final String DEFAULT_OUTER_ITERATIONS = "30";

    /**
     * By default, use this many inner iterations.
     */
    public static final String DEFAULT_INNER_ITERATIONS = "1";

    /**
     * The data point to latent variable matrix.
     */
    private Matrix W;

    /**
     * The latent variable to feature value matrix.
     */
    private Matrix H;

    /**
     * The number of times either matrix will be udpated while keeping the other
     * fixed.
     */
    private final int innerLoop;

    /**
     * The number of times both matrices will be updated with their inner loops.
     */
    private final int outerLoop;

    /**
     * Creates a new {@link NonNegativeMatrixFactorizationMultiplicative} using
     * the system defined {@link Properties}
     */
    public NonNegativeMatrixFactorizationMultiplicative() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@link NonNegativeMatrixFactorizationMultiplicative} using
     * the provided {@link Properties}
     */
    public NonNegativeMatrixFactorizationMultiplicative(Properties props) {
        outerLoop = Integer.parseInt(props.getProperty(
                    OUTER_ITERATIONS, DEFAULT_OUTER_ITERATIONS));
        innerLoop = Integer.parseInt(props.getProperty(
                    INNER_ITERATIONS, DEFAULT_INNER_ITERATIONS));
    }

    /**
     * Creates a new {@link NonNegativeMatrixFactorizationMultiplicative}.
     *
     * @param innerLoop The number of iterations for updating a single matrix
     *        while keeping the matrix fixed
     * @param outerloop The number of {@code innerLoop} iterations to perform
     */
    public NonNegativeMatrixFactorizationMultiplicative(int innerLoop,
                                                        int outerLoop) {
        this.innerLoop = innerLoop;
        this.outerLoop = outerLoop;
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
    public void factorize(SparseMatrix matrix, int numDimensions) {
        W = new ArrayMatrix(matrix.rows(), numDimensions);
        initialize(W);
        H = new ArrayMatrix(numDimensions, matrix.columns());
        initialize(H);

        // Update W and H by alternatively fixing one matrix and updating the
        // other for several iterations.  The updates for each matrix are:
        //
        //   H <- H .* (WtA) ./ ((WtW)H)
        //
        //   W <- W .* (AHt) ./ (W(HHt))
        for (int i = 0; i < outerLoop; ++i) {
            LOG.info("Updating H matrix");
            // Update the H matrix by holding the W matrix fixed for a few
            // iterations.
            for (int j = 0; j < innerLoop; ++j) {
                // Compute W'A and store it in Hprime as this has the same
                // dimensionality as H.  This is done in a sideways manner so as
                // to take advantage of the sparsity of matrix.  Although this
                // will likely cause cache misses in Hprime, it's likely better
                // than traversing _every_ cell in matrix, which may be in the
                // millions.
                long start = System.currentTimeMillis();
                Matrix Hprime = new ArrayMatrix(H.rows(), H.columns());
                double s = 0;
                for (int k = 0; k < numDimensions; ++k) {
                    for (int n = 0; n < matrix.rows(); ++n) {
                        SparseDoubleVector v = matrix.getRowVector(n);
                        int[] nonZeros = v.getNonZeroIndices();
                        for (int m : nonZeros)
                            Hprime.set(k, m, Hprime.get(k, m) + 
                                             W.get(n,k) * v.get(m));
                    }
                }
                long end = System.currentTimeMillis();
                LOG.info("Step 1: " + (end-start) + "ms");

                // Compute WtW using standard matrix multiplication.
                start = System.currentTimeMillis();
                Matrix WtW = new ArrayMatrix(numDimensions, numDimensions);
                for (int k = 0; k < numDimensions; ++k) {
                    for (int l = 0; l < numDimensions; ++l) {
                        double sum = 0;
                        for (int n = 0; n < W.rows(); ++n) 
                            sum += W.get(n, k) * W.get(n, l);
                        WtW.set(k, l, sum);
                    }
                }
                end = System.currentTimeMillis();
                LOG.info("Step 2: " + (end-start) + "ms");

                // Compute the final update to H which is
                // H <- H .* (WtA)./ (WtWH).
                //
                // Do this by computing each cell of WtWH and then let 
                //   v <- Hprime[k, m]
                //   w <- H[k, m]
                //   sum <- WtWH[k, m]
                //   Hprime[k,m] <- w * v / sum
                // This saves us from every storing WtWH in memory.  We can
                // store the updated values in Hprime because we only access
                // each cell once, but we cannot use H itself since we need to
                // maintain those values until every value of WtWH is computed.
                start = System.currentTimeMillis();
                for (int k = 0; k < numDimensions; ++k) {
                    for (int m = 0; m < H.columns(); ++m) {
                        double sum = 0;
                        for (int l = 0; l < numDimensions; ++l)
                            sum += WtW.get(k, l) * H.get(l, m);
                        double v = Hprime.get(k, m);
                        double w = H.get(k, m);
                        Hprime.set(k, m, w * v / sum);

                    }
                }
                end = System.currentTimeMillis();
                LOG.info("Step 3: " + (end-start) + "ms");

                // Update H with the new value.
                H = Hprime;
            }

            LOG.info("Updating W matrix");
            // Update the H matrix by holding the W matrix fixed for a few
            // iterations.
            for (int j = 0; j < innerLoop; ++j) {
                // Compute Wprime, which is AHt.  Since A is the left matrix, we
                // can take advantage of it's sparsity using the standard matrix
                // multiplication techniques.
                long start = System.currentTimeMillis();
                Matrix Wprime = new ArrayMatrix(W.rows(), W.columns());
                for (int n = 0; n < matrix.rows(); ++ n) {
                    SparseDoubleVector v = matrix.getRowVector(n);
                    int[] nonZeros = v.getNonZeroIndices();
                    for (int k = 0; k < numDimensions; ++k) {
                        double sum = 0;
                        for (int m : nonZeros)
                            sum += v.get(m) * H.get(k, m);
                        Wprime.set(n, k, sum);
                    }
                }
                long end = System.currentTimeMillis();
                LOG.info("Step 4: " + (end-start) + "ms");

                // Compute HHt using standard matrix multiplication.
                start = System.currentTimeMillis();
                Matrix HHt = new ArrayMatrix(numDimensions, numDimensions);
                for (int k = 0; k < numDimensions; ++k) {
                    for (int l = 0; l < numDimensions; ++l) {
                        double sum = 0;
                        for (int m = 0; m < H.columns(); ++m)
                            sum += H.get(k, m) * H.get(l, m);
                        HHt.set(k, l, sum);
                    }
                }
                end = System.currentTimeMillis();
                LOG.info("Step 5: " + (end-start) + "ms");

                // Compute W(HHt) and update Wprime using the following update:
                // W <- W .* (AHt) ./ (W(HHt)).
                //
                // Do this by computing each cell of W(HHt) and then let 
                //   v <- Wprime[n, k]
                //   w <- W[n, k]
                //   sum <- W(HHt)[n, k]
                // This saves us from every storing W(HHt)in memory.  We can
                // store the updated values in Wprime because we only access
                // each cell once, but we cannot use W itself since we need to
                // maintain those values until every value of W(HHt) is
                // computed.
                start = System.currentTimeMillis();
                for (int n = 0; n < W.rows(); ++n) {
                    for (int k = 0; k < W.columns(); ++k) {
                        double sum = 0;
                        for (int l = 0; l < numDimensions; ++l)
                            sum += W.get(n, l) * HHt.get(l, k);
                        double v = Wprime.get(n, k);
                        double w = W.get(n, k);
                        Wprime.set(n, k, w * v / sum);
                    }
                }
                end = System.currentTimeMillis();
                LOG.info("Step 6: " + (end-start) + "ms");

                // Update W with Wprime.
                W = Wprime;
            }

            LOG.info("Finishedo processing outer loop: " + i);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Matrix dataClasses() {
        return W;
    }

    /**
     * {@inheritDoc}
     */
    public Matrix classFeatures() {
        return H;
    }

    /**
     * {@inheritDoc}
     */
    public MatrixBuilder getBuilder() {
        return new SvdlibcSparseBinaryMatrixBuilder();
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
}
