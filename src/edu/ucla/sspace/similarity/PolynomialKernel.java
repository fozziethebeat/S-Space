package edu.ucla.sspace.similarity;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorMath;


/**
 * Returns the dot product of the two vectors raised to a specified power.
 *
 * </p>
 *
 * This metric is symmetric.
 *
 * @author Keith Stevens
 */
public class PolynomialKernel extends AbstractSymmetricSimilarityFunction {

    private double degree;

    /**
     * Sets the first degree of the polynomial.
     */
    public void setParams(double... params) {
        this.degree = params[0];
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
        double dotProduct = VectorMath.dotProduct(v1, v2);
        return Math.pow(dotProduct + 1, degree);
    }

    /**
     * {@inheritDoc}
     */
    public double sim(IntegerVector v1, IntegerVector v2) {
        double dotProduct = VectorMath.dotProduct(v1, v2);
        return Math.pow(dotProduct + 1, degree);
    }

    /**
     * {@inheritDoc}
     */
    public double sim(Vector v1, Vector v2) {
        double dotProduct = VectorMath.dotProduct(v1, v2);
        return Math.pow(dotProduct + 1, degree);
    }
}
