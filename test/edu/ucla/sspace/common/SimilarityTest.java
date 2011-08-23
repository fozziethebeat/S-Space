/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.common;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.text.*; 
import edu.ucla.sspace.util.*;
import edu.ucla.sspace.vector.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimilarityTest {

    @Test public void testEuclideanDistSparseDoubleSame() {
        double[] v = new double[] { 0, 1, 1, 1, 1, 1, 1, 1, 0, 0};
        Vector a = new CompactSparseVector(10);
        Vector b = new CompactSparseVector(10);
        for (int i = 0; i < v.length; ++i) {
            if (v[i] != 0) {
                a.set(i, v[i]);
                b.set(i, v[i]);
            }
        }
        assertEquals(0, Similarity.euclideanDistance(a, b), .00000001);
    }

    @Test public void testEuclideanDistSparseDoubleDiff() {
        double[] v1 = new double[] { 0, 1, 0, 1, 0, 1, 0, 1, 0, 0};
        double[] v2 = new double[] { 0, 0, 1, 1, 1, 1, 1, 1, 0, 0};
        Vector a = new CompactSparseVector(10);
        Vector b = new CompactSparseVector(10);
        for (int i = 0; i < v1.length; ++i) {
            a.set(i, v1[i]);
            b.set(i, v2[i]);
        }
        assertEquals(4, Similarity.euclideanDistance(a, b), .00000001);
    }

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
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothIterableAShorter() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothIterableAFewerNonZero() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 0, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothIterableBShorter() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothIterableBFewerNonZero() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 0, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseSameLength() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        v2 = new ScaledSparseDoubleVector(v2, 1);

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseAShorter() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        v2 = new ScaledSparseDoubleVector(v2, 1);

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseAFewerNonZero() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 0, 0});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        v2 = new ScaledSparseDoubleVector(v2, 1);

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseBShorter() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        v2 = new ScaledSparseDoubleVector(v2, 1);

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothSparseBFewerNonZero() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 1, 2, 4, 0});
        v1 = new ScaledSparseDoubleVector(v1, 1);
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineADenseBSparse() {
        DoubleVector v1 = new DenseVector(new double[] {0, 1, 2, 4, 0});
        SparseDoubleVector v2 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineASparseBDense() {
        SparseDoubleVector v1 = new CompactSparseVector(
                new double[] {0, 4, 3, 1, 0});
        DoubleVector v2 = new DenseVector(new double[] {0, 1, 2, 4, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothDenseSameLength() {
        DoubleVector v1 = new DenseVector(new double[] {0, 1, 2, 4, 0});
        DoubleVector v2 = new DenseVector(new double[] {0, 4, 3, 0, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothDenseAShorter() {
        DoubleVector v1 = new DenseVector(new double[] {0, 1, 2, 4});
        DoubleVector v2 = new DenseVector(new double[] {0, 4, 3, 0, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineBothDenseBShorter() {
        DoubleVector v1 = new DenseVector(new double[] {0, 1, 2, 4, 0});
        DoubleVector v2 = new DenseVector(new double[] {0, 4, 3, 0});

        double sim = rawCosine(v1, v2);
        assertEquals(sim, Similarity.cosineSimilarity(v1, v2), .0000001);
    }

    @Test public void testCosineSimDoubleSame() {
        double[] a = new double[] { 1d, 1d, 1d, 1d };
        double[] b = new double[] { 1d, 1d, 1d, 1d };

        assertEquals(1d, Similarity.cosineSimilarity(a, b), 0);
    }

    @Test public void testCosineSimDoubleOrthogonal() {
        double[] a = new double[] { 1d, 1d, 1d, 1d };
        double[] b = new double[] { 0,  0,  0,  0  };

        assertEquals(0d, Similarity.cosineSimilarity(a, b), 0);
    }

    @Test public void testCosineSimDoubleOpp() {
        double[] a = new double[] { 1d, 1d, 1d, 1d };
        double[] b = new double[] { -1d, -1d, -1d, -1d };

        assertEquals(-1d, Similarity.cosineSimilarity(a, b), 0);
    }

    @Test public void test() {
        double[] a = new double[] {8.19005E-6, -1.44134E-5, -4.3301E-5, 4.30826E-5, -2.64222E-5, -9.86308E-6, -3.03196E-5, 7.03687E-5, -4.87804E-5, 1.10858E-5, 2.83266E-6, -4.01535E-6, -3.51149E-6, -8.00313E-5, -1.47108E-4, 2.35135E-5, 3.78031E-5, -2.28232E-5, -2.77489E-5, -7.99647E-5, 5.65426E-6, 6.04393E-6, 2.52512E-5, -5.83511E-5, -7.96796E-5, 4.27407E-5, 9.19847E-5, -8.5067E-5, -7.2703E-5, -6.46051E-6, -1.9538E-5, -5.78909E-5, -2.83674E-5, -4.06525E-5, 3.84215E-5, 2.79336E-5, -7.69892E-5, 2.36779E-5, -4.35609E-5, -1.94329E-6, 7.72848E-5, -4.30089E-5, -2.08998E-5, -9.33174E-6, -1.2093E-5, 7.48906E-5, -2.69058E-5, 3.29903E-7, 2.26033E-6, 3.7489E-5, 2.59857E-5, -8.39284E-6, 7.53021E-5, 3.77313E-5, 1.63646E-5, -4.79374E-5, -4.1694E-5, -4.92779E-5, 2.57649E-5, 1.56992E-5, 1.2077E-5, 4.24031E-5, -3.09575E-5, -9.07588E-5, 2.12212E-5, 1.89771E-5, -1.18034E-4, -1.44766E-5, 1.92178E-5, 9.57666E-6, -5.5876E-5, 4.89421E-5, -2.80457E-5, -9.01221E-6, 4.61115E-5, 1.22786E-5, -1.90424E-5, -7.27246E-5, -2.15286E-5, 9.2882E-5, 6.58164E-6, 2.69464E-5, 2.40526E-5, -8.20241E-5, -1.29577E-5, 5.59582E-5, 6.50146E-5, 8.59783E-5, -4.8178E-5, 5.4302E-5, -3.52774E-5, -5.27758E-5, 5.50476E-5, -2.52206E-5, -7.95274E-8, 7.42056E-5, -2.86824E-5, -9.20887E-5, -8.69728E-5, 4.16115E-5, -8.48129E-5, -1.7871E-4, -1.29719E-4, -4.09618E-5, 2.25774E-4, -5.23729E-5, -6.61788E-5, -6.30097E-5, -5.65847E-5, -2.42076E-5, 1.50876E-5, -3.27144E-5, -6.21432E-5, -7.74189E-5, 4.45677E-5, -3.31479E-5, -7.41299E-5, 2.02004E-5, -4.82303E-5, 6.02592E-5, -5.09835E-5, 1.05076E-4, 1.73753E-5, 1.73117E-5, 9.93678E-6, -2.41887E-5, 1.08279E-4, -1.3019E-4, -3.09393E-5, -5.13315E-6, 1.84155E-5, -7.09676E-5, 2.32905E-5, 1.47E-5, 2.71342E-5, -9.19811E-6, -9.87307E-5, -5.84401E-6, 5.16592E-5, 1.36645E-4, 2.19905E-5, 4.45868E-5, -2.66875E-5, -3.59186E-5, -6.1684E-6, 6.97521E-5, 2.60127E-5, -5.01494E-5, -4.16584E-5, -2.30615E-5, 4.20723E-5, 3.40843E-5, 4.03148E-5, -6.56817E-5, 1.65275E-4, 4.18116E-5, -2.48442E-5, 4.78178E-5, 7.83753E-5, -1.47407E-5, 4.88307E-5, -5.93782E-5, 5.03502E-5, 2.88539E-5, 1.19331E-4, -2.39112E-5, -8.49992E-5, -4.01541E-5, 1.62923E-5, 1.88547E-5, 8.95842E-5, -7.28299E-5, 1.21445E-5, -5.89378E-5, -6.65967E-5, -8.50644E-5, 2.46782E-5, -1.0299E-4, 9.60735E-8, 2.93387E-6, 2.57378E-5, -1.12325E-5, -1.10034E-5, 1.24761E-4, 7.6044E-5, -1.03977E-4, -3.06481E-6, 7.12917E-5, -7.9224E-5, 1.59117E-4, 2.11879E-5, 3.0842E-5, -7.9322E-5, -1.04462E-4, 3.87261E-5, 4.3244E-6, 9.46437E-5, -8.60174E-5, -3.05064E-5, 5.7438E-5, 3.31488E-5, -3.17478E-6, -8.79997E-5, -4.7839E-5, -5.40552E-5, -7.47663E-5, 9.21402E-5, 1.10518E-4, -4.8445E-5, -8.39209E-5, -1.08664E-6, 1.68847E-5, 3.79695E-6, 4.55555E-5, -9.89627E-7, -8.77606E-5, -1.28227E-4, -2.87216E-5, 5.2299E-5, -1.11951E-4, 2.02437E-5, 3.29525E-5, 1.08223E-4, 3.91065E-5, 3.70774E-5, 4.77546E-5, -5.70924E-5, -4.86682E-5, -6.93965E-5, 8.32165E-5, -6.64209E-5, -1.26831E-4, -1.96279E-4, -1.05842E-4, 1.11053E-4, 1.92816E-6, 1.13789E-4, -1.57837E-4, 1.19083E-5, -8.1302E-5, 3.11232E-5, 4.8594E-5, 8.36189E-5, 1.48566E-4, -5.61234E-5, -6.4129E-5, 2.52695E-5, 1.59706E-4, -6.53347E-5, 1.53161E-4, -1.3499E-4, -5.75281E-6, 1.09625E-5, 5.95813E-6, -4.66603E-5, 7.25739E-6, 1.04223E-4, 1.99208E-4, 1.05687E-4, 5.05025E-5, -5.26635E-5, -1.28142E-4, 4.62843E-6, 5.89622E-5, -7.81203E-5, -1.22613E-4, 7.79161E-5, -2.3893E-5, 5.60786E-5, 4.50843E-5, 8.00563E-5, -4.42426E-5, -9.01087E-5, -1.25973E-4, -3.71886E-6, -4.20646E-5, 1.15534E-4, 1.29791E-4, 1.51436E-5, -1.56128E-4, -1.00892E-4, 7.77265E-5, -6.48648E-5, -8.47049E-5, 8.85242E-5, 2.34376E-5, -2.5317E-5, -1.06402E-4, 2.06177E-4, -1.0973E-5, -8.46892E-6, 6.22562E-5, 9.11549E-5, 9.84701E-5, 6.05338E-5, -3.99993E-6, -7.7409E-5, -1.60585E-4, -1.77891E-5, -2.12131E-4};
        double[] b = new double[] {1.00981E-5, 1.11685E-5, 1.60502E-5, -2.59952E-5, 8.03264E-5, 2.29943E-5, -1.29749E-6, 4.24541E-5, -3.18316E-5, 8.1733E-6, -3.93688E-5, 3.32728E-5, 9.9989E-5, -9.30302E-6, 1.98399E-5, -1.13557E-4, 1.54307E-4, -7.53817E-5, -2.75576E-5, 2.022E-5, 4.53363E-5, -4.4962E-5, 3.53611E-7, 1.10403E-4, -3.26303E-5, 1.04853E-4, -6.07937E-5, -1.49832E-5, 6.69598E-6, -5.2008E-5, 5.52798E-7, -6.8504E-5, -4.286E-6, 1.96727E-5, -2.70103E-5, 1.08071E-4, 9.09873E-5, -1.58805E-4, -1.01143E-4, -2.73059E-5, 2.44786E-5, -5.78045E-5, 5.92241E-5, 1.03822E-4, 3.80285E-5, -1.07878E-5, -3.18042E-5, -1.61565E-5, 3.62269E-5, 6.70518E-5, 1.08011E-4, 5.31756E-5, -3.63183E-7, -3.22668E-5, 8.80509E-5, 2.83258E-5, 1.04982E-4, 2.62087E-5, 9.11093E-5, 5.57983E-5, 3.51724E-5, 1.06398E-6, -2.51774E-5, 4.27631E-5, 1.33901E-4, 6.62948E-5, 1.12234E-5, -3.38213E-5, 7.24845E-5, 2.47791E-5, 6.17921E-5, 8.77473E-5, 2.69209E-6, -3.32704E-5, 6.94755E-5, -6.37214E-5, -9.88855E-5, 8.22564E-5, 2.76285E-5, -1.10709E-4, -8.55563E-5, 1.89175E-5, 3.44617E-5, 1.53762E-4, -1.19729E-5, -5.06515E-5, 3.76008E-5, 1.31956E-4, -1.89257E-4, -9.34057E-5, -7.15389E-5, 3.06375E-5, -1.33741E-5, 8.50051E-5, 4.59844E-5, -1.9626E-5, 4.907E-5, -9.14112E-5, 4.87105E-5, -6.26826E-6, -4.15098E-5, -4.26402E-5, 3.83271E-5, -6.68858E-6, -7.46563E-5, -3.95302E-5, 4.41794E-5, -7.51164E-5, -1.55637E-4, -5.48001E-5, 5.24174E-6, 4.3018E-5, 1.5166E-5, -1.94679E-5, -6.95803E-6, -6.58549E-5, 8.96534E-5, -9.20762E-6, 4.89856E-5, 2.68825E-5, -2.20285E-5, -8.61901E-5, 6.83344E-5, 7.69868E-5, -6.89018E-5, -5.60828E-6, -9.52779E-6, -9.27611E-6, 3.95908E-5, 6.79429E-5, -4.80287E-5, 3.97039E-6, 4.32897E-7, -4.19336E-5, 3.53723E-5, 3.3337E-5, -4.86027E-5, -2.71073E-5, 1.30804E-4, 4.79676E-5, 1.65763E-5, -1.77766E-5, 3.66144E-5, 4.4479E-5, -1.50806E-5, -1.13818E-5, -3.13176E-5, -7.70504E-5, 6.31817E-5, 2.18676E-5, 1.20261E-4, 4.35607E-5, -5.15999E-5, -2.58346E-5, -4.13177E-5, -2.00417E-4, 1.68831E-5, -6.02303E-5, 9.45197E-5, 6.6662E-5, 1.33994E-4, -1.66022E-5, -2.79836E-5, -4.74424E-5, 6.93223E-6, -5.18014E-5, -2.04815E-5, -5.11196E-5, -9.87329E-6, 4.26724E-5, 2.32871E-6, -1.01156E-4, -1.11858E-4, 4.23491E-5, -6.93713E-6, -6.29313E-6, 3.29539E-5, 6.56043E-5, 1.43328E-4, 7.11957E-5, -1.07679E-5, 7.44211E-5, 8.17042E-5, -6.15996E-6, -6.59826E-5, -8.82818E-6, -3.09656E-5, -8.9475E-5, -6.68468E-5, -6.7283E-5, 5.34261E-5, -6.79428E-6, 4.32786E-5, -1.85479E-5, 9.39179E-5, 6.07096E-5, 9.52276E-5, 7.12292E-5, 4.33654E-5, -4.02982E-5, 7.37147E-5, -2.24896E-5, -4.9196E-5, 2.25685E-5, -8.08117E-5, 1.21398E-4, 1.50914E-5, -2.80098E-5, -2.34042E-5, -3.48547E-5, 1.71487E-5, -8.07387E-5, -5.70597E-5, -4.54813E-5, -1.52235E-5, -9.85513E-5, 7.44884E-5, 9.94725E-6, -5.1021E-5, -1.33205E-5, 3.62451E-7, -1.03036E-4, 1.04155E-5, 9.87697E-5, 4.86448E-5, 7.63919E-5, -2.31125E-5, 2.48584E-5, 2.8561E-5, 1.87416E-6, -6.29007E-5, -9.28173E-5, 2.10043E-5, 2.18302E-5, 1.91366E-5, 7.94096E-5, -7.14599E-5, -4.63358E-5, 7.59393E-5, -2.85492E-5, -1.28253E-4, -6.09456E-5, 8.28371E-5, -1.43334E-6, -4.55608E-5, 2.19517E-6, -5.15362E-6, -2.83001E-5, -5.87708E-5, -1.37837E-5, -5.0083E-5, 1.0064E-4, 2.61741E-5, -4.95747E-5, -8.3353E-5, 1.0429E-4, 6.08571E-5, -2.04295E-5, -7.8495E-5, 2.2737E-5, -3.82019E-5, 2.46483E-5, 1.39389E-5, 3.4087E-5, 3.68337E-5, -1.01225E-4, 6.25603E-6, -1.2452E-5, -1.4045E-4, -4.88644E-5, -3.80586E-6, 7.58782E-5, -8.85142E-6, 1.55179E-4, -3.97388E-6, -1.03643E-5, -1.7448E-5, 2.08918E-4, 2.4555E-7, 1.7616E-4, 1.93204E-4, -6.38885E-6, -6.89794E-5, 4.71293E-5, 4.37854E-5, 4.3897E-5, -1.37457E-4, -5.65646E-5, 1.47547E-5, -6.87011E-5, -2.90971E-5, 1.77622E-6, 9.87288E-5, 5.55597E-5, -5.27009E-6, -5.22753E-6, 9.39871E-5, 1.27954E-4, 3.32004E-5, -5.23862E-5};
        assertFalse(Similarity.cosineSimilarity(a, b) == 0d);
    }

    @Test public void testSpearman() {
        double[] a = 
            new double[] { 106, 86, 100, 101, 99, 103, 97, 113, 112, 110 };
        double[] b =
            new double[] { 7,   0,  27,  50,  28, 29,  20, 12,  6,   17};
        double spCorr = Similarity.spearmanRankCorrelationCoefficient(a,b);
        assertEquals(-0.175758, spCorr, 0.01);
    }

    @Test public void testSpearmanInt() {
        int[] a = 
            new int[] { 106, 86, 100, 101, 99, 103, 97, 113, 112, 110 };
        int[] b =
            new int[] { 7,   0,  27,  50,  28, 29,  20, 12,  6,   17};
        double spCorr = Similarity.spearmanRankCorrelationCoefficient(a,b);
        assertEquals(-0.175758, spCorr, 0.01);
    }

}
