

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseDoubleVector;


/**
 * A utility class for computing the standard Shannon entropy over a matrix
 * representing counts of various events.  This utility computes the total
 * entropy over the joint event space, the entropy for each column, and the
 * entropy for each row.  The entropy statistics will be returned as a simple
 * {@link EntropyStats} object.  All entropy computations use the natural log.
 *
 * @author Keith Stevens
 */
public class MatrixEntropy {

    /**
     * Returns a {@link EntropyStats} covering the entropies of all joint events,
     * rows, and columns in {@code m}.
     */
    public static EntropyStats entropy(Matrix m) {
        if (m instanceof SparseMatrix)
            return entropy((SparseMatrix) m);

        double sum = 0;
        double[] colSums = new double[m.columns()];
        double[] rowSums = new double[m.rows()];
        for (int r = 0; r < m.rows(); ++r) {
            for (int c = 0; c < m.columns(); ++c) {
                double v = m.get(r, c);
                sum += v;
                colSums[c] += v;
                rowSums[r] += v;
            }
        }

        double entropy = 0;
        double[] colEntropy = new double[m.columns()];
        double[] rowEntropy = new double[m.rows()];
        for (int r = 0; r < m.rows(); ++r) {
            for (int c = 0; c < m.columns(); ++c) {
                double v = m.get(r, c);
                if (v != 0d) {
                    entropy -= entropy(v, sum);
                    colEntropy[c] -= entropy(v, colSums[c]);
                    rowEntropy[r] -= entropy(v, rowSums[r]);
                }
            }
        }
        return new EntropyStats(entropy, colEntropy, rowEntropy);
    }

    /**
     * Returns a {@link EntropyStats} covering the entropies of all joint events,
     * rows, and columns in {@code m}.
     */
    public static EntropyStats entropy(SparseMatrix m) {
        double sum = 0;
        double[] colSums = new double[m.columns()];
        double[] rowSums = new double[m.rows()];
        for (int r = 0; r < m.rows(); ++r) {
            SparseDoubleVector sv = m.getRowVector(r);
            for (int c : sv.getNonZeroIndices()) {
                double v = m.get(r, c);
                sum += v;
                colSums[c] += v;
                rowSums[r] += v;
            }
        }

        double entropy = 0;
        double[] colEntropy = new double[m.columns()];
        double[] rowEntropy = new double[m.rows()];
        for (int r = 0; r < m.rows(); ++r) {
            SparseDoubleVector sv = m.getRowVector(r);
            for (int c : sv.getNonZeroIndices()) {
                double v = m.get(r, c);
                entropy -= entropy(v, sum);
                colEntropy[c] -= entropy(v, colSums[c]);
                rowEntropy[r] -= entropy(v, rowSums[r]);
            }
        }
        return new EntropyStats(entropy, colEntropy, rowEntropy);
    }

    /**
     * Computes the entropy of a raw count given a particular summation using
     * the natural log.
     */
    private static double entropy(double count, double sum) {
        double p = count / sum;
        return Math.log(p) * p;
    }

    /**
     * A simple struct containing the total entropy of a matrix, and arrays
     * holding the column and row entropies.
     */
    public static class EntropyStats {
        public double entropy;
        public double[] colEntropy;
        public double[] rowEntropy;

        public EntropyStats(double entropy,
                            double[] colEntropy,
                            double[] rowEntropy) {
            this.entropy = entropy;
            this.colEntropy = colEntropy;
            this.rowEntropy = rowEntropy;
        }
    }
}
