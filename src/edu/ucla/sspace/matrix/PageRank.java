package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseDoubleVector;


/**
 * @author Keith Stevens
 */
public class PageRank implements MatrixRank {

    private final double alpha;

    private final double beta;

    private final int numIterations;

    public PageRank() {
        this(.85, 50);
    }

    public PageRank(double alpha, int numIterations) {
        this.alpha = alpha;
        this.beta = 1-alpha;
        this.numIterations = numIterations;
    }

    /**
     * {@inheritDoc}
     */
    public double[] rank(Matrix adj) {
        return rank(adj, initialRanks(adj.rows()));
    }

    /**
     * {@inheritDoc}
     */
    public double[] rank(SparseMatrix adj) {
        return rank(adj, initialRanks(adj.rows()));
    }

    /**
     * {@inheritDoc}
     */
    public double[] rank(Matrix adj, double[] initialRanks) {
        if (adj instanceof SparseMatrix)
            return rank((SparseMatrix) adj, initialRanks);

        double[] ranks = initialRanks;
        double[] columnSums =
            TransformStatistics.extractStatistics(adj).columnSums;

        for (int i = 0; i < numIterations; i++) {
            double[] newRanks = new double[adj.rows()];
            for (int r = 0; r < adj.rows(); ++r)
                for (int c = 0; c < adj.columns(); ++c)
                    newRanks[r] += adj.get(r, c) / columnSums[c] * ranks[r];
            for (int r = 0; r < adj.rows(); ++r)
                newRanks[r] = alpha * newRanks[r] + beta * initialRanks[r];
            ranks = newRanks;
        }
        return ranks;
    }

    /**
     * {@inheritDoc}
     */
    public double[] rank(SparseMatrix adj, double[] initialRanks) {
        double[] ranks = initialRanks;
        double[] columnSums =
            TransformStatistics.extractStatistics(adj).columnSums;

        for (int i = 0; i < numIterations; i++) {
            double[] newRanks = new double[adj.rows()];
            for (int r = 0; r < adj.rows(); ++r)
                for (int c : adj.getRowVector(r).getNonZeroIndices())
                    newRanks[r] += adj.get(r, c) / columnSums[c] * ranks[r];

            for (int r = 0; r < adj.rows(); ++r)
                newRanks[r] = alpha * newRanks[r] + beta * initialRanks[r];
            ranks = newRanks;
        }
        return ranks;
    }

    /**
     * {@inheritDoc}
     */
    public double[] initialRanks(int numRows) {
        double[] evenRanks = new double[numRows];
        for (int n = 0; n < numRows; ++n)
            evenRanks[n] = 1.0/n;
        return evenRanks;
    }
}
