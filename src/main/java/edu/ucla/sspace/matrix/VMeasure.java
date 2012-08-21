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


/**
 * This {@link MatrixAggregate} measure implements the <a
 * href="http://scikit-learn.org/0.11/modules/clustering.html#homogeneity-completeness-and-v-measure">V-Measure</a>
 * between two variables.  This measure is often used to compare different
 * clustering solutions over the same dataset.  Values range from {@code 0} to
 * {@code 1.0}.  A V-Measure of {@code 1.0} indicates a perfect match between
 * two solutions while a score of {@code 0.0} indicates no significant overlap
 * between two solutions.
 *
 * </p>
 *
 * This implementation is based on the paper described in:
 *
 * <li>Andrew Rosenburg and Julia Hirschberg, "V-Measure: A Conditional
 * Entropy-Based External Cluster Evaluation Measure," In <i>Proceedings of the
 * 2007 Joint Conference on Empirical Methods in Natural Language Processing and
 * Computational Natural Language Learning (EMNLP-CoNLL)</i>.  Available <a
 * href="http://acl.ldc.upenn.edu/D/D07/D07-1043.pdf">here</a></li>
 *
 * @author Keith Stevens
 */
public class VMeasure implements MatrixAggregate {

    /**
     * {@inheritDoc}
     */
    public double aggregate(Matrix m) {
        // Compute simple summation aggregates of the matrix.
        double[] rowSums = new double[m.rows()];
        double[] colSums = new double[m.columns()];
        double sum = 0;
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c) {
                double v = m.get(r, c);
                rowSums[r] += v;
                colSums[c] += v;
                sum += v;
            }

        // Compute the conditional entropy of the first solution given the
        // second solution.
        double hEntropy = 0;
        for (int k = 0; k < m.columns(); ++k)
            for (int c = 0; c < m.rows(); ++c)
                hEntropy += info(m.get(c,k), sum, colSums[k]);

        // Compute the overal entropy of the first solution.
        double classEntropy = 0;
        for (int c = 0; c < m.rows(); ++c)
            classEntropy += info(rowSums[c], sum, sum);

        // Compute the homogeneity between the two solutions.  
        double homogeneity = (classEntropy == 0d) ? 1 : 1 - (hEntropy/classEntropy);

        // Compute the conditional entropy of the second solution given the
        // first solution.
        double cEntropy = 0;
        for (int c = 0; c < m.rows(); ++c) 
            for (int k = 0; k < m.columns(); ++k)
                cEntropy += info(m.get(c,k), sum, rowSums[c]);

        // Compute the overal entropy of the second solution.
        double clustEntropy = 0;
        for (int k = 0; k < m.columns(); ++k)
            clustEntropy += info(colSums[k], sum, sum);

        // Compute the completeness between the two solutions.
        double completeness = (clustEntropy == 0d) ? 1 : 1 - (cEntropy/clustEntropy);

        // Return the harmonic mean between completeness and homogeneity.
        return 2 * homogeneity * completeness / (homogeneity + completeness);
    }

    /**
     * A helper function to compute the information between two values.
     */
    private static final double info(double value, double d1, double d2) {
        return (value == 0d)
            ? 0.0
            : value / d1 * Math.log(value/d2);
    }
}
