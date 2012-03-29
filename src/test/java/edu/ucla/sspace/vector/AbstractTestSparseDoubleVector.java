package edu.ucla.sspace.vector;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public abstract class AbstractTestSparseDoubleVector {

    protected abstract SparseDoubleVector newNoLengthVector();

    protected abstract SparseDoubleVector newLengthVector(int length);

    protected abstract SparseDoubleVector newCopy(SparseDoubleVector other);

    protected abstract SparseDoubleVector newFromArray(double[] values);

    protected abstract SparseDoubleVector newFromValues(int[] nonZeros, 
                                                        double[] values,
                                                        int length);

    private static boolean matchArrays(int[] arr1, int[] arr2) {
        if (arr1.length != arr2.length) {
            System.err.printf("Array length's don't match. arr1: %d arr2: %d\n",
                              arr1.length, arr2.length);
            return false;
        }

        Set<Integer> nz = new HashSet<Integer>();
        for (int i : arr1)
            nz.add(i);
        for (int i : arr2)
            if (!nz.contains(i)) {
                System.err.printf("arr2 contains: %d\n", i);
                return false;
            }
        return true;
    }

    @Test public void testSetLength() {
        SparseDoubleVector sv = newLengthVector(1024);
        assertEquals(1024, sv.length());
    }

    @Test public void testUnsetLength() {
        SparseDoubleVector sv = newNoLengthVector();
        assertEquals(Integer.MAX_VALUE, sv.length());
    }

    @Test public void testFromArray() {
        SparseDoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        assertEquals(7, sv.length());
        assertTrue(matchArrays(new int[]{1,3,5,6}, sv.getNonZeroIndices()));

        assertEquals(1, sv.get(1), .0000001);
        assertEquals(1, sv.get(3), .0000001);
        assertEquals(1, sv.get(5), .0000001);
        assertEquals(10, sv.get(6), .0000001);
        assertEquals(Math.sqrt(103), sv.magnitude(), .000001);
    }

    @Test public void testIsSparseDoubleVector() {
        SparseDoubleVector sv = newNoLengthVector();
        assertTrue(sv instanceof SparseDoubleVector);
    }

    @Test public void testIsSparseVector() {
        SparseDoubleVector sv = newNoLengthVector();
        assertTrue(sv instanceof SparseVector);
    }

    @Test public void testIsIterable() {
        SparseDoubleVector sv = newNoLengthVector();
        assertTrue(sv instanceof Iterable);
    }

    @Test public void testFromSparseVector() {
        SparseDoubleVector first = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});

        SparseDoubleVector sv = newCopy(first);
        assertEquals(7, sv.length());
        assertTrue(matchArrays(new int[]{1,3,5,6}, sv.getNonZeroIndices()));

        assertEquals(1, sv.get(1), .0000001);
        assertEquals(1, sv.get(3), .0000001);
        assertEquals(1, sv.get(5), .0000001);
        assertEquals(10, sv.get(6), .0000001);
        assertEquals(Math.sqrt(103), sv.magnitude(), .000001);

        first.set(1, 10);
        assertEquals(1, sv.get(1), .0000001);
        sv.set(1, .5);
        assertEquals(10, first.get(1), .000001);
    }

    @Test public void testFromValidNonZeros() {
        int[] nonZeros = { 1, 3, 5, 6};
        double[] values = {1, 1, 1, 1};
        SparseDoubleVector sv = newFromValues(nonZeros, values, 7);
        assertEquals(7, sv.length());
        assertTrue(matchArrays(new int[]{1,3,5,6}, sv.getNonZeroIndices()));

        assertEquals(1, sv.get(1), .0000001);
        assertEquals(1, sv.get(3), .0000001);
        assertEquals(1, sv.get(5), .0000001);
        assertEquals(1, sv.get(6), .0000001);
        assertEquals(2, sv.magnitude(), .000001);
    }

    @Test public void testFromLongerLength() {
        int[] nonZeros = {1, 3, 5, 6};
        double[] values = {1, 1, 1, 1};
        SparseDoubleVector sv = newFromValues(nonZeros, values, 8);
        assertEquals(8, sv.length());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFromNonMatchingLengthNonZerosAndValues() {
        int[] nonZeros = { 1, 3, 5, 6};
        double[] values = {1, 1, 1, 0, 1};
        SparseDoubleVector sv = newFromValues(nonZeros, values, 7);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFromShorterLength() {
        int[] nonZeros = {1, 3, 5, 6};
        double[] values = {1, 1, 1, 1};
        SparseDoubleVector sv = newFromValues(nonZeros, values, 6);
    }

    @Test public void testSetToNonZero() {
        SparseDoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        assertTrue(matchArrays(new int[]{1,3,5,6}, sv.getNonZeroIndices()));
        sv.set(1, 2);
        assertEquals(2, sv.get(1), .00000001);
        assertTrue(matchArrays(new int[]{1,3,5,6}, sv.getNonZeroIndices()));
    }

    @Test public void testSetToZero() {
        SparseDoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        assertTrue(matchArrays(new int[]{1,3,5,6}, sv.getNonZeroIndices()));
        sv.set(0, 2);
        assertEquals(2, sv.get(0), .00000001);
        assertTrue(matchArrays(new int[]{0, 1,3,5,6}, sv.getNonZeroIndices()));
    }

    @Test public void testSetNonZeroToZero() {
        SparseDoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        assertTrue(matchArrays(new int[]{1,3,5,6}, sv.getNonZeroIndices()));
        sv.set(1, 0);
        assertEquals(0, sv.get(1), .00000001);
        assertTrue(matchArrays(new int[]{3,5,6}, sv.getNonZeroIndices()));
    }

    @Test public void testAddToNonZero() {
        SparseDoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        assertTrue(matchArrays(new int[]{1, 3,5,6}, sv.getNonZeroIndices()));
        sv.add(1, 1);
        assertEquals(2, sv.get(1), .00000001);
        assertTrue(matchArrays(new int[]{1, 3,5,6}, sv.getNonZeroIndices()));
    }

    @Test public void testAddToZero() {
        SparseDoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        assertTrue(matchArrays(new int[]{1, 3,5,6}, sv.getNonZeroIndices()));
        sv.add(0, 2);
        assertEquals(2, sv.get(0), .00000001);
        assertTrue(matchArrays(new int[]{0, 1, 3,5,6}, sv.getNonZeroIndices()));
    }

    @Test public void testAddNonZeroToZero() {
        SparseDoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        assertTrue(matchArrays(new int[]{1, 3,5,6}, sv.getNonZeroIndices()));
        sv.add(1, -1);
        assertEquals(0, sv.get(1), .00000001);
        assertTrue(matchArrays(new int[]{3,5,6}, sv.getNonZeroIndices()));
    }

    @Test public void testModifyMagnitude() {
        SparseDoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 1});
        assertEquals(2, sv.magnitude(), .00000001);
        sv.set(1, 2);
        assertEquals(Math.sqrt(7), sv.magnitude(), .000001);
        sv.set(3, 0);
        assertEquals(Math.sqrt(6), sv.magnitude(), .000001);
    }
}
