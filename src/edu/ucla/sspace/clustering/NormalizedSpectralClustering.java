package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;

import org.apache.commons.math.linear.EigenDecomposition;
import org.apache.commons.math.linear.EigenDecompositionImpl;
import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;

import java.util.Properties;


/**
 * @author Keith Stevens
 */
public class NormalizedSpectralClustering implements Clustering {

    public Assignments cluster(Matrix m, int k, Properties props) {
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

        // Now create the normalized symmetric graph laplacian:
        RealMatrix L = new Array2DRowRealMatrix(m.rows(), m.columns());
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c)
                L.setEntry(r,c, ((r == c) ? 1 : 0) -
                                degrees[r] * m.get(r,c) * degrees[c]);
        
        // Extract the top k eigen vectors and set them as the columns of our
        // new spectral decomposition matrix.  While doing this, also compute
        // the squared sum of rows.
        EigenDecomposition eigenDecomp = new EigenDecompositionImpl(L, 0);
        Matrix spectral = new ArrayMatrix(m.rows(), k);
        double[] rowNorms = new double[m.rows()];
        for (int c = 0; c < k; ++c) {
            RealVector eigenVec = eigenDecomp.getEigenvector(c);
            for (int r = 0; r < m.rows(); ++r) {
                double value = eigenVec.getEntry(r);
                rowNorms[r] += Math.pow(value, 2);
                spectral.set(r, c, value);
            }
        }

        // Normalize each row by the squared root of the sum of squares.
        for (int r = 0; r < spectral.rows(); ++r) {
            double norm = Math.sqrt(rowNorms[r]);
            for (int c = 0; c < spectral.columns(); ++c)
                spectral.set(r,c, spectral.get(r,c) / norm);
        }

        // Now cluster the data points with K-Means clustering and return the
        // assignments.
        return DirectClustering.cluster(spectral, k, 20);
    }

    public Assignments cluster(Matrix m, Properties props) {
        return null;
    }
}
