package edu.ucla.sspace.similarity;

import edu.ucla.sspace.vector.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class CosineSimilarityTest {

    public static double rawCosine(DoubleVector v1, DoubleVector v2) {
        if (v1.length() > v2.length()) {
            DoubleVector t = v2;
            v2 = v1;
            v1 = t;
        }

        double sum = 0;
        for (int i = 0; i < v1.length(); ++i)
            sum += v1.get(i) * v2.get(i);
        return sum / (v1.magnitude() * v2.magnitude());
    }

    @Test public void testCosineBothIterableSameLength() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothIterableAShorter() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothIterableAFewerNonZero() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 0, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothIterableBShorter() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothIterableBFewerNonZero() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 0, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseSameLength() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        v2 = new ScaledSparseDoubleVector(v2, 1);

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseAShorter() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        v2 = new ScaledSparseDoubleVector(v2, 1);

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseAFewerNonZero() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 0, 0});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        v2 = new ScaledSparseDoubleVector(v2, 1);

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseBShorter() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        v2 = new ScaledSparseDoubleVector(v2, 1);

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseBFewerNonZero() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineADenseBSparse() {
        SimilarityFunction simFunc = new CosineSimilarity();
        DoubleVector v1 = new DenseVector(new double[] {0, 1, 2, 4, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineASparseBDense() {
        SimilarityFunction simFunc = new CosineSimilarity();
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        DoubleVector v2 = new DenseVector(new double[] {0, 1, 2, 4, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothDenseSameLength() {
        SimilarityFunction simFunc = new CosineSimilarity();
        DoubleVector v1 = new DenseVector(new double[] {0, 1, 2, 4, 0});
        DoubleVector v2 = new DenseVector(new double[] {0, 4, 3, 0, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothDenseAShorter() {
        SimilarityFunction simFunc = new CosineSimilarity();
        DoubleVector v1 = new DenseVector(new double[] {0, 1, 2, 4});
        DoubleVector v2 = new DenseVector(new double[] {0, 4, 3, 0, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }

    @Test public void testCosineBothDenseBShorter() {
        SimilarityFunction simFunc = new CosineSimilarity();
        DoubleVector v1 = new DenseVector(new double[] {0, 1, 2, 4, 0});
        DoubleVector v2 = new DenseVector(new double[] {0, 4, 3, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, simFunc.sim(v1, v2), .0000001);
    }
}
