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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.factorization.EigenDecomposition;

import java.util.Properties;
import java.util.logging.Logger;


/**
 * An implementation of Normalized Spectral Clustering, a stable method for
 * computing the minimum conductance over a graph by using the normalized graph
 * laplacian.  This implementation is based on 
 *
 * <ul>
 *   <li>
 *    <li style="font-family:Garamond, Georgia, serif">Andrew Ng, Michael
 *    Jordan, and Yair Weiss.  On Spectral Clustering: Analysis and an
 *    Algorithm. <i>Advances in Neural Information Processing Systems</i>.
 *    Available <a
 *    href="http://citeseerx.ist.psu.edu/viewdoc/summary?doi=10.1.1.19.8100">here</a>
 *    </li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class NormalizedSpectralClustering implements Clustering {

    private static final Logger LOG =
        Logger.getLogger(NormalizedSpectralClustering.class.getName());

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix m, int k, Properties props) {
        assert m.rows() == m.columns();

        LOG.fine("Computing Degree of Adjacency Matrix");
        // Assume that the matrix m is symmetric.  Now compute the degrees:
        double[] degrees = new double[m.rows()];
        for (int r = 0; r < m.rows(); ++r) {
            double degree = 0;
            for (int c = 0; c < m.columns(); ++c)
                degree += m.get(r,c);
            degrees[r] = degree;
        }

        // Compute the inverse square of the degrees for the normalized
        // Laplacian.
        for (int r = 0; r < m.rows(); ++r)
            degrees[r] = Math.pow(degrees[r], -.5);

        LOG.fine("Computing Graph Laplacian");
        // Now create the normalized symmetric graph laplacian:
        Matrix L = new ArrayMatrix(m.rows(), m.columns());
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c)
                L.set(r,c, ((r == c) ? 1 : 0) -
                           degrees[r] * m.get(r,c) * degrees[c]);
        
        LOG.fine("Computing Eigenvector Decomposition");
        // Extract the top k eigen vectors and set them as the columns of our
        // new spectral decomposition matrix.  While doing this, also compute
        // the squared sum of rows.
        EigenDecomposition eigenDecomp = new EigenDecompositionImpl(L, 0);

        LOG.fine("Extracting Normalized Spectral Representation");
        EigenDecomposition eigen = new EigenDecomposition();
        eigen.factorize(L, k);
        Matrix eigenValues = eigen.dataClasses();

        Matrix eigenVectors = eigen.classFeatures();
        Matrix spectral = new ArrayMatrix(m.rows(), k);
        double[] rowNorms = new double[m.rows()];
        for (int c = 0; c < k; ++c) {
            for (int r = 0; r < m.rows(); ++r) {
                double value = eigenVectors.get(c, k);
                rowNorms[r] += Math.pow(value, 2);
                spectral.set(r, c, value);
            }
        }

        // Normalize each row by the squared root of the sum of squares.
        for (int r = 0; r < spectral.rows(); ++r) {
            double norm = Math.sqrt(rowNorms[r]);
            if (norm != 0d)
                for (int c = 0; c < spectral.columns(); ++c)
                    spectral.set(r,c, spectral.get(r,c) / norm);
        }

        LOG.fine("Clustering with K-Means");
        // Now cluster the data points with K-Means clustering and return the
        // assignments.
        return DirectClustering.cluster(spectral, k, 20);
    }

    /**
     * Unsupported.
     */
    public Assignments cluster(Matrix m, Properties props) {
        throw new UnsupportedOperationException(
                "Cannot cluster without a fixed number of clusters");
    }
}
