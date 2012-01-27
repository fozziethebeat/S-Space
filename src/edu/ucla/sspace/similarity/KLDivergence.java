package edu.ucla.sspace.similarity;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.Vector;


/**
 * Returns the KL Divergence between any two probability distributions
 * represented as {@link Vector}s.
 *
 * </p>
 *
 * This metric is <i>not</i> symmetric.  Use the Jensen-Shannon Divergence for a
 * symmetric similarity measure between two probability distributions
 *
 * @author Keith Stevens
 */
public class KLDivergence implements SimilarityFunction {
    

    /**
     * Does nothing
     */
    public void setParams(double... arguments) { }

    /**
     * Returns {@code false} (is asymmetric).
     */    
    public boolean isSymmetric() {
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public double sim(DoubleVector v1, DoubleVector v2) {
        return Similarity.klDivergence(v1, v2);
    }

    /**
     * {@inheritDoc}
     */
    public double sim(IntegerVector v1, IntegerVector v2) {
        return Similarity.klDivergence(v1, v2);
    }

    /**
     * {@inheritDoc}
     */
    public double sim(Vector v1, Vector v2) {
        return Similarity.klDivergence(v1, v2);
    }
}
