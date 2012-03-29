package edu.ucla.sspace.matrix;


/**
 * @author Keith Stevens
 */
public class VMeasure implements MatrixAggregate {

    public double aggregate(Matrix m) {
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

        double hEntropy = 0;
        for (int k = 0; k < m.columns(); ++k)
            for (int c = 0; c < m.rows(); ++c)
                hEntropy += info(m.get(c,k), sum, colSums[k]);

        double classEntropy = 0;
        for (int c = 0; c < m.rows(); ++c)
            classEntropy += info(rowSums[c], sum, sum);

        double homogeneity = (classEntropy == 0d) ? 1 : 1 - (hEntropy/classEntropy);

        double cEntropy = 0;
        for (int c = 0; c < m.rows(); ++c) 
            for (int k = 0; k < m.columns(); ++k)
                cEntropy += info(m.get(c,k), sum, rowSums[c]);

        double clustEntropy = 0;
        for (int k = 0; k < m.columns(); ++k)
            clustEntropy += info(colSums[k], sum, sum);

        double completeness = (clustEntropy == 0d) ? 1 : 1 - (cEntropy/clustEntropy);

        return 2 * homogeneity * completeness / (homogeneity + completeness);
    }

    private static final double info(double value, double d1, double d2) {
        return (value == 0d)
            ? 0.0
            : value / d1 * Math.log(value/d2);
    }
}
