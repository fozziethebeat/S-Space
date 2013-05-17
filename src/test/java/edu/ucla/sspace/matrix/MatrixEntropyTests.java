package edu.ucla.sspace.matrix;

import edu.ucla.sspace.matrix.MatrixEntropy.EntropyStats;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class MatrixEntropyTests {
    private static final double[][] rawData = {
        {2.0, 5.0, 4.0, 3.0, 6.0, 4.0, 3.0},
        {3.0, 0.0, 5.0, 4.0, 2.0, 1.0, 0.0},
        {4.0, 0.0, 0.0, 5.0, 3.0, 2.0, 1.0},
        {5.0, 0.0, 0.0, 0.0, 4.0, 3.0, 2.0},
        {0.0, 0.0, 0.0, 0.0, 0.0, 5.0, 4.0},
        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0},
        {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}
    };

    private static final double EPSILON = .00001;
    @Test public void testDenseEntropy() {
        Matrix m = new ArrayMatrix(7, 7);
        for (int r = 0; r < rawData.length; ++r)
            for (int c = 0; c < rawData[r].length; ++c)
                m.set(r, c, rawData[r][c]);

        EntropyStats stats = MatrixEntropy.entropy(m);

        // Check the length of each entropy value.
        assertEquals(rawData.length, stats.rowEntropy.length);
        assertEquals(rawData[0].length, stats.colEntropy.length);

        // Validate that the row and column entropies match what we'd expect
        // within some small error margin.
        for (int r = 0; r < stats.rowEntropy.length; ++r)
            assertEquals(rowEntropy(r), stats.rowEntropy[r], EPSILON);
        for (int c = 0; c < stats.colEntropy.length; ++c)
            assertEquals(colEntropy(c), stats.colEntropy[c], EPSILON);
    }


    @Test public void testSparseEntropy() {
        SparseMatrix m = new YaleSparseMatrix(7, 7);
        for (int r = 0; r < rawData.length; ++r)
            for (int c = 0; c < rawData[r].length; ++c)
                if (rawData[r][c] != 0d)
                    m.set(r, c, rawData[r][c]);
        EntropyStats stats = MatrixEntropy.entropy(m);

        // Check the length of each entropy value.
        assertEquals(rawData.length, stats.rowEntropy.length);
        assertEquals(rawData[0].length, stats.colEntropy.length);

        // Validate that the row and column entropies match what we'd expect
        // within some small error margin.
        for (int r = 0; r < stats.rowEntropy.length; ++r)
            assertEquals(rowEntropy(r), stats.rowEntropy[r], EPSILON);
        for (int c = 0; c < stats.colEntropy.length; ++c)
            assertEquals(colEntropy(c), stats.colEntropy[c], EPSILON);
    }

    private double rowEntropy(int row) {
        double sum = 0; 
        for (double x : rawData[row])
            sum += x;
        if (sum == 0d)
            return 0d;
        double entropy = 0;
        for (double x : rawData[row]) {
            double p = x / sum;
            if (p != 0d)
                entropy -= Math.log(p) * p;
        }
        return entropy;
    }

    private double colEntropy(int col) {
        double sum = 0; 
        for (double[] row : rawData)
            sum += row[col];
        if (sum == 0d)
            return 0d;
        double entropy = 0;
        for (double[] row : rawData) {
            double p = row[col] / sum;
            if (p != 0d)
                entropy -= Math.log(p) * p;
        }
        return entropy;
    }
}
