package edu.ucla.sspace.sim;


/**
 * @author Keith Stevens
 */
public abstract class AbstractSymmetricSimilarityFunction
        implements SimilarityFunction {

    public void setParams(double... arguments) {
    }

    public boolean isSymmetric() {
        return true;
    }
}

