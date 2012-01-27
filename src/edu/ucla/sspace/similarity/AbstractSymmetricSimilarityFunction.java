package edu.ucla.sspace.similarity;


/**
 * @author Keith Stevens
 */
public abstract class AbstractSymmetricSimilarityFunction
        implements SimilarityFunction {

    /**
     * Performs a no-op and sets no parameters
     */
    public void setParams(double... arguments) {
    }

    /**
     * Returns {@code true}.
     */
    public boolean isSymmetric() {
        return true;
    }
}

