package edu.ucla.sspace.sim;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.Vector;


/**
 * @author Keith Stevens
 */
public interface SimilarityFunction {

    void setParams(double... arguments);

    boolean isSymmetric();

    double sim(DoubleVector v1, DoubleVector v2);

    double sim(IntegerVector v1, IntegerVector v2);

    double sim(Vector v1, Vector v2);
}
