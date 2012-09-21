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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * This {@link MatrixAggregate} computes the <a
 * href="http://en.wikipedia.org/wiki/Adjusted_mutual_information">Adjusted
 * Mutual Information</a> between two factors whose interactions are stored
 * within a {@link Matrix}.  The rows of the {@link Matrix} should correspond to
 * factors for one variable while the columns should correspond to factors for a
 * separate variable.  The computed {@link AdjustedMutualInformation} then
 * represents the the amount of overlap between the two variables, as a value
 * between 0 and 1 where 1 is perfect overlap and 0 indicates no overlap.  This
 * measure is best used to compare two distinct clusterings of the same dataset.
 * Under this formulation, an {@link AdjustedMutualInformation} of {@code 1.0}
 * then indicates the two solutions are equivalent while {@code 0.0} indicates
 * that they share no significant groupings.
 *
 * </p>
 *
 * This implementation is based off of the following paper:
 * <li>
 *   Vinh, Nguyen Xuan and Epps, Julien and Bailey, James, "Information
 *   Theoretic Measures for Clusterings Comparison: Variants, Properties,
 *   Normalization and Correction for Chance," in <i>The Journal of Machine
 *   Learning Research</i>.  Available <a
 *   href="http://dl.acm.org/citation.cfm?id=1756006.1953024">here</a>.</li>
 *
 * </p>
 *
 * We make one siginficant deviation from the standard {@link
 * AdjustedMutualInformation} measure.  If both variables have only one factor,
 * i.e. the matrix has one row and one column, we return an AMI of 1.0 to
 * indicate a perfect match between the two variables.  Techinically however,
 * the AMI should be 0.0 as there is no shared information.
 *
 * </p> 
 *
 * Implementation note: that the code for this computation is extremely complicated and
 * poorly documented as it's a translation of Matlab code optimized to avoid
 * computing too many factorials.
 *
 * @author Keith Stevens
 */
public class AdjustedMutualInformation implements MatrixAggregate {

    /**
     * {@inheritDoc}
     */
    public double aggregate(Matrix m) {
        if (m.rows() == 1 && m.columns() == 1)
            return 1.0;

        double[] rowCounts = new double[m.rows()];
        double[] colCounts = new double[m.columns()];
        double sum = 0;
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c) {
                double v = m.get(r, c);
                rowCounts[r] += v;
                colCounts[c] += v;
                sum += v;
            }

        double emi = 0.0;
        double mi = 0.0;
        for (int r = 0; r < m.rows(); ++r) {
            double a = rowCounts[r];
            for (int c = 0; c < m.columns(); ++c) {
                double b = colCounts[c];
                // Update the mi for this class and cluster.
                mi += pmi(a, b, m.get(r,c), sum);

                // Update the expectation of mi.
                double n_ij = Math.max(1, a+b - sum);
                // Determine whether or not this pairing occured more than we'd
                // expect at random.
                double x1 = Math.min(n_ij, sum - a - b + n_ij);
                double x2 = Math.max(n_ij, sum - a - b + n_ij);

                // Compute a range of numbers
                List<Double> numerator = new ArrayList<Double>();
                List<Double> denominator = new ArrayList<Double>();
                for (double x = a-n_ij+1; x <= a; x++)
                    numerator.add(x);
                for (double x = b-n_ij+1; x <= b; x++)
                    numerator.add(x);

                for (double x = sum-a+1; x <= sum; ++x)
                    denominator.add(x);
                for (double x = 1; x <= x1; ++x)
                    denominator.add(x);

                if (sum-b > x2)
                    for (double x = x2+1; x <= sum-b; ++x)
                        numerator.add(x);
                else 
                    for (double x = sum-b+1; x <= x2; ++x)
                        denominator.add(x);

                // Sort the ranges in both num and dom so that we can avoid
                // overflow.
                Collections.sort(numerator);
                Collections.sort(denominator);

                // Compute the product of num / dom.
                double factorialPowers = 1;
                for (int k = 0; k < numerator.size(); ++k)
                    factorialPowers *= (numerator.get(k) / denominator.get(k));

                double factorialSum = pmi(a, b, n_ij, sum) *
                                      factorialPowers;
                factorialPowers *= adjustment(a, b, n_ij, sum);

                for (double x = Math.max(1.0, a+b-sum)+1;
                            x <= Math.min(a, b);
                            ++x) {
                    factorialSum += pmi(a, b, x, sum) *
                                   factorialPowers;
                    factorialPowers *= adjustment(a, b, x, sum);
                }

                emi += factorialSum;
            }
        }

        // Compute the entropy of the labels.
        double ha = entropy(rowCounts, sum);
        double hb = entropy(colCounts, sum);

        // If we would get NaN, return a raw AMI of 0.0
        double rawAmi = (Math.max(ha,hb) - emi == 0) 
            ? 0.0 
            : (mi - emi) / (Math.max(ha, hb) - emi);
        // Range the AMI to be above 0.0.
        return (rawAmi < 0) ? 0.0 : rawAmi;
    }

    /**
     * A helper function for computing a count adjustment.
     */
    private static double adjustment(double a, double b, double n, double sum) {
        return (a-n) * (b-n) / (n+1) / (sum - a - b + n + 1);
    }

    /**
     * A helper function that returns the pmi.
     */
    private static double pmi(double a, double b, double n, double sum) {
        return (n == 0d)
            ? 0.0
            : n/sum * Math.log(sum*n/(a*b + .000000001));
    }

    /**
     * A helper function for computing the entropy of an un-normalized
     * probability distribution.
     */
    private static double entropy(double[] sums, double total) {
        double entropy = 0;
        for (double sum : sums)
            if (sum != 0d)
                entropy += sum / total * Math.log(sum/total);
        return -entropy;
    }
}
