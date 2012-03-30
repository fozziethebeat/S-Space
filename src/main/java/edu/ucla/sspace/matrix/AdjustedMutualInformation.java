package edu.ucla.sspace.matrix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class AdjustedMutualInformation implements MatrixAggregate {

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

    private static double adjustment(double a, double b, double n, double sum) {
        return (a-n) * (b-n) / (n+1) / (sum - a - b + n + 1);
    }

    public static double pmi(double a, double b, double n, double sum) {
        return (n == 0d)
            ? 0.0
            : n/sum * Math.log(sum*n/(a*b + .000000001));
    }

    private static double entropy(double[] sums, double total) {
        double entropy = 0;
        for (double sum : sums)
            if (sum != 0d)
                entropy += sum / total * Math.log(sum/total);
        return -entropy;
    }
}
