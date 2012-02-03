package edu.ucla.sspace.bigram;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.matrix.AtomicGrowingSparseHashMatrix;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class BigramSpace implements SemanticSpace {

    private final BasisMapping<String, String> basis;

    private final AtomicGrowingSparseHashMatrix bigramMatrix;

    private final int windowSize;

    public BigramSpace() {
        this(new StringBasisMapping(), 8);
    }

    public BigramSpace(BasisMapping<String, String> basis, int windowSize) {
        this.basis = basis;
        this.windowSize = windowSize;
        bigramMatrix = new AtomicGrowingSparseHashMatrix();
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return bigramMatrix.columns();
    }

    public Set<String> getWords() {
        return basis.keySet();
    }

    public SparseDoubleVector getVector(String word) {
        int index = basis.getDimension(word);
        return (index < 0) ? null : bigramMatrix.getRowVector(index);
    }

    public String getSpaceName() {
        return "BigramSpace";
    }

    /**
     * {@inheritDoc}
     */
    public void  processDocument(BufferedReader document) throws IOException {
        Queue<String> bigramWindow = new ArrayDeque<String>();

        Iterator<String> documentTokens = IteratorFactory.tokenize(document);

        for (int i = 0; i < windowSize; ++i) {
            String word = documentTokens.next();
            int index = basis.getDimension(word);
            if (index >= 0)
                bigramWindow.offer(word);
        }

        while (!bigramWindow.isEmpty()) {
            if (documentTokens.hasNext()) {
                String word = documentTokens.next();
                int index = basis.getDimension(word);
                if (index >= 0)
                    bigramWindow.offer(word);
            }

            String term = bigramWindow.remove();
            int index1 = basis.getDimension(term);
            if (index1 < 0)
                continue;

            for (String other : bigramWindow) {
                int index2 = basis.getDimension(other);
                if (index2 < 0)
                    continue;
                bigramMatrix.add(index1, index2, 1.0);
            }
        }
    }

    public void processSpace(Properties props) {
        double[] rowSums = new double[bigramMatrix.rows()];
        double[] columnSums = new double[bigramMatrix.columns()];
        double total = 0.0;

        for (int r = 0; r < bigramMatrix.rows(); ++r) {
            SparseDoubleVector rowVec = bigramMatrix.getRowVector(r);
            for (int c : rowVec.getNonZeroIndices()) {
                double v = rowVec.get(c);
                if (v <= 5.0d)
                    rowVec.set(c,0.0);
                rowSums[r] += v;
                columnSums[c] += v;
                total += v;
            }
        }

        for (int r = 0; r < bigramMatrix.rows(); ++r) {
            SparseDoubleVector rowVec = bigramMatrix.getRowVector(r);
            for (int c : rowVec.getNonZeroIndices()) {
                double both = rowVec.get(c);
                double justA = rowSums[r] - both;
                double justB = columnSums[c] - both;
                double neither = total - justA - justB - both;
                double chiSqr = score(both, justA, justB, neither);
                if (chiSqr < 3.841)
                    rowVec.set(c,0d);
            }
        }
        basis.setReadOnly(true);
    }

    /**
     * Returns the Pearson Chi-Squared test of significance using one degree of
     * freedom.  To identify events with a significance value above .95, or p
     * &ample; .05, reject any chi-squared values less than 3.841.
     */
    public double score(double both, double justA, double justB, double neither) {
        // Think of the table as
        //      B       !B
        //   A: both    justA   : row1Sum
        //  !A: justB   neither : row2Sum
        //  ---------------------------
        //      col1Sum col2Sum : sum
        double col1sum = both + justB; 
        double col2sum = justA + neither;
        double row1sum = both + justA; 
        double row2sum = justB + neither;
        double sum = row1sum + row2sum;
        
        // Calculate the expected values for a, b, c, d
        // The expected value is coliSum * rowjSum / sum 
        double aExp = (row1sum / sum) * col1sum;
        double bExp = (row1sum / sum) * col2sum;
        double cExp = (row2sum / sum) * col1sum;
        double dExp = (row2sum / sum) * col2sum;

        // Chi-squared is (Observed - Expected)^2 / Expected
        return (Math.pow(both-aExp, 2) / aExp) +
               (Math.pow(justA-bExp, 2) / bExp) +
               (Math.pow(justB-cExp, 2) / cExp) +
               (Math.pow(neither-dExp, 2) / dExp);
    }
}
