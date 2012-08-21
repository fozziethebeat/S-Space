package edu.ucla.sspace.vector;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public abstract class AbstractTestDenseDoubleVector {

    protected abstract DoubleVector newLengthVector(int length);

    protected abstract DoubleVector newFromArray(double[] values);

    protected abstract DoubleVector newCopy(DoubleVector other);

    @Test public void testSetLength() {
        DoubleVector sv = newLengthVector(1024);
        assertEquals(1024, sv.length());
    }

    @Test public void testFromArray() {
        DoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        assertEquals(7, sv.length());
        assertEquals(1, sv.get(1), .0000001);
        assertEquals(1, sv.get(3), .0000001);
        assertEquals(1, sv.get(5), .0000001);
        assertEquals(10, sv.get(6), .0000001);
        assertEquals(Math.sqrt(103), sv.magnitude(), .000001);
    }

    @Test public void testFromVector() {
        DoubleVector first = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});

        DoubleVector sv = newCopy(first);
        assertEquals(7, sv.length());
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

    @Test public void testSet() {
        DoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        sv.set(1, 2);
        assertEquals(2, sv.get(1), .00000001);
    }

    @Test public void testAdd() {
        DoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 10});
        sv.add(1, 1);
        assertEquals(2, sv.get(1), .00000001);
    }

    @Test public void testModifyMagnitude() {
        DoubleVector sv = newFromArray(new double[]{0, 1, 0, 1, 0, 1, 1});
        assertEquals(2, sv.magnitude(), .00000001);
        sv.set(1, 2);
        assertEquals(Math.sqrt(7), sv.magnitude(), .000001);
        sv.set(3, 0);
        assertEquals(Math.sqrt(6), sv.magnitude(), .000001);
    }
}
