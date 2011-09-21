package edu.ucla.sspace.sim;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.Vector;


/**
 * Returns the Gaussing kernel weighting of two vectors using a parameter to
 * weight the distance between the two vectors.
 *
 * </p>
 *
 * This metric is symmetric.
 *
 * @author Keith Stevens
 */
public class GaussianKernel extends AbstractSymmetricSimilarityFunction {

    private double gaussianKernelParam;

    public void setParams(double... params) {
        gaussianKernelParam = params[0];
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSymmetric() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public double sim(DoubleVector v1, DoubleVector v2) {
        double dist = Similarity.euclideanDistance(v1, v2);
        return Math.pow(Math.E, -(dist / gaussianKernelParam));
    }

    /**
     * {@inheritDoc}
     */
    public double sim(IntegerVector v1, IntegerVector v2) {
        double dist = Similarity.euclideanDistance(v1, v2);
        return Math.pow(Math.E, -(dist / gaussianKernelParam));
    }

    /**
     * {@inheritDoc}
     */
    public double sim(Vector v1, Vector v2) {
        double dist = Similarity.euclideanDistance(v1, v2);
        return Math.pow(Math.E, -(dist / gaussianKernelParam));
    }
}
