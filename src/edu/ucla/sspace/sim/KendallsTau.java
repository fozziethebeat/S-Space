package edu.ucla.sspace.sim;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.Vector;


/**
 * A functional class for computing <a
 * href="http://en.wikipedia.org/wiki/Kendall%27s_tau">Kendall's tau</a> of the
 * values in the two vectors.  This method uses tau-b, which is suitable for
 * vectors with duplicate values.
 *
 * @author Keith Stevens
 */
public class KendallsTau extends AbstractSymmetricSimilarityFunction {

    /**
     * {@inheritDoc}
     */
    public double sim(DoubleVector v1, DoubleVector v2) {
        return Similarity.kendallsTau(v1, v2);
    }

    /**
     * {@inheritDoc}
     */
    public double sim(IntegerVector v1, IntegerVector v2) {
        return Similarity.kendallsTau(v1, v2);
    }

    /**
     * {@inheritDoc}
     */
    public double sim(Vector v1, Vector v2) {
        return Similarity.kendallsTau(v1, v2);
    }
}
