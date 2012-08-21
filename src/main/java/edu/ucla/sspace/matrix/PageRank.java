/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseDoubleVector;


/**
 * This {@link MatrixRank} implements the classic <a
 * href="http://en.wikipedia.org/wiki/PageRank">PageRank</a> algorithm used to
 * weight nodes in an arbitrary graph.  This implementation assumes that the
 * nodes are represented as an <a
 * href="http://en.wikipedia.org/wiki/Adjacency_matrix">Adjacency Matrix</a>.
 * The matrix is assumed to be formatted such that each entry at row {@code i}
 * and column {@code j} encodes the weight of the edge from node {@code j} to
 * node {@code i}.  The input matrix does not need to be symmetric nor fully
 * connected.
 *
 * </p>
 *
 * Hyperparameters to the {@link PageRank} algorithm, such as the weight given
 * to the initial random jump weights for each node and the convergence
 * criteria, can be set through the constructor.  The algorithm can be
 * "personalized" by providing a custom initial ranking of each node when
 * calling {@code rank}.
 *
 * @author Keith Stevens
 */
public class PageRank implements MatrixRank {

    /**
     * The weight given to the link counts in the matrix.
     */
    private final double alpha;

    /**
     * The weight given to the random jump weights.
     */
    private final double beta;

    /**
     * The number of iterations before completing.
     */
    private final int numIterations;

    /**
     * Creates a new {@link PageRank} instance with an {@code alpha} value of
     * {@code 0.85} that will run for {@code 50} iterations.
     */
    public PageRank() {
        this(.85, 50);
    }

    /**
     * Creates a new {@link PageRank} instance with a specified values for
     * {@code alpha} and {@link numIterations}.  {@code beta} will be set as
     * {@code 1 - alpha}.
     *
     * @param alpha The weight given to the adjacency matrix ranking.  Ranges
     *              from 0 to 1.
     * @param numIterations The number of iterations to run for before stopping.
     */
    public PageRank(double alpha, int numIterations) {
        if (alpha < 0 || alpha > 1d)
            throw new IllegalArgumentException(
                    "Alpha must be between 0 and 1.   " + alpha + " was given.");
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
