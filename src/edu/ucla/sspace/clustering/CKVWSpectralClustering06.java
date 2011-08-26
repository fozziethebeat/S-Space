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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.util.Generator;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;

import java.util.Properties;


/**
 * A spectral clustering implementation based on the following paper:
 *
 * <p style="font-family:Garamond, Georgia, serif"> David Cheng ,  Ravi Kannan ,
 * Santosh Vempala ,  Grant Wang (2003) A Divid-and-Merge Methodology for
 * Clustering. Available <a
 * href="http://people.csail.mit.edu/gjw/papers/divmerge.pdf">here</a>.
 *
 * </p>  This implementation implements a subclass of the {@link
 * BaseSpectralCut} and simply computes the second eigen vector for a data set.
 *
 * @see BaseSpectralCut
 * @see SpectralClustering
 *
  * @author Keith Stevens
 */
public class CKVWSpectralClustering06 implements Clustering {

    /**
     * The proper prefix.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.CKVWSpectralClustering06";

    /**
     * The property used to use K-Means as the objective function.
     */
    public static final String USE_KMEANS =
        PROPERTY_PREFIX + ".useKMeans";

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, Properties props) {
        SpectralClustering cluster = new SpectralClustering(
                .2, new SuperSpectralGenerator());
        return cluster.cluster(matrix);
    }

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix,
                                int numClusters,
                                Properties props) {
        SpectralClustering cluster = new SpectralClustering(
                .2, new SuperSpectralGenerator());
        return cluster.cluster(
                matrix, numClusters, props.getProperty(USE_KMEANS) != null);
    }

    /**
     * An internal spectral cut implementation that is based on the referred to
     * paper.  See paper for details.
     */
    public class SuperSpectralCut extends BaseSpectralCut {

        /**
         * {@inheritDoc}
         */
        protected DoubleVector computeSecondEigenVector(Matrix matrix,
                                                        int vectorLength) {
           // Compute pi, and D.  Pi is the normalized form of rho.  D a
           // diagonal matrix with sqrt(pi) as the values along the diagonal.
           // Also compute pi * D^-1.
            DoubleVector pi = new DenseVector(vectorLength);
            DoubleVector D = new DenseVector(vectorLength);
            DoubleVector piDInverse = new DenseVector(vectorLength);
            for (int i = 0; i < vectorLength; ++i) {
                double piValue = rho.get(i)/pSum;
                pi.set(i, piValue);
                if (piValue > 0d) {
                    D.set(i, Math.sqrt(piValue));
                    piDInverse.set(i, piValue / D.get(i));
                }
            }

            // Create the second largest eigenvector of the a scaled form of the
            // row normalized affinity matrix.  The computation is using the
            // power method such that the affinity matrix is never explicitly
            // computed.
            // piDInverse serves as a vector which is similar to the first eigen
            // vector.  The second eigen vector is assumed to be orthogonal to
            // piDInverse.  This algorithm makes O(log(matrix.rows())) passes
            // through the data matrix.
         
            // Step 1, generate a random vector, v,  that is orthogonal to
            // pi*D-Inverse.
            DoubleVector v = new DenseVector(vectorLength);
            for (int i = 0; i < v.length(); ++i)
                v.set(i, Math.random());

            // Make log(matrix.rows()) passes.
            int log = (int) Statistics.log2(vectorLength);
            for (int k = 0; k < log; ++k) {
                // start the orthonormalizing the eigen vector.
                v = orthonormalize(v, piDInverse);

                // Step 2, repeated, (a) normalize v (b) set v = Q*v, where Q =
                // D * R-Inverse * matrix * matrix-Transpose * D-Inverse.

                // v = Q*v is broken into 4 sub steps that allow for sparse
                // multiplications. 
                // Step 2b-1) v = D-Inverse*v.
                for (int i = 0; i < vectorLength; ++ i)
                    if (D.get(i) != 0d)
                        v.set(i, v.get(i) / D.get(i));

                // Step 2b-2) v = matrix-Transpose * v.
                DoubleVector newV = computeMatrixTransposeV(matrix, v);

                // Step 2b-3) v = matrix * v.
                computeMatrixDotV(matrix, newV, v);

                // Step 2b-4) v = D*R-Inverse * v. Note that R is a diagonal
                // matrix with rho as the values along the diagonal.
                for (int i = 0; i < vectorLength; ++i) {
                    double oldValue = v.get(i);
                    double newValue = oldValue * D.get(i) / rho.get(i);
                    v.set(i, newValue);
                }
            }

            return v;
        }
    }

    public String toString() {
        return "CKVWSpectralClustering06";
    }

    /**
     * A simple generator for creating instances of the {@link SpectralCut}
     * class.
     */
    public class SuperSpectralGenerator implements Generator<EigenCut> {

        /**
         * {@inheritDoc}
         */
        public EigenCut generate() {
            return new SuperSpectralCut();
        }
    }
}
