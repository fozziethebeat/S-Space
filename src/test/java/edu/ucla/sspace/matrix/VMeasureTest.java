package edu.ucla.sspace.matrix;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class VMeasureTest {

    @Test public void testOneClassOneCluster() {
        Matrix m = new ArrayMatrix(1, 1);
        m.set(0, 0, 24);
        VMeasure ami = new VMeasure();
        assertEquals(1.0, ami.aggregate(m), .0000001);
    }

    @Test public void testOneClassMultiCluster() {
        Matrix m = new ArrayMatrix(1, 2);
        m.set(0, 0, 24);
        m.set(0, 1, 4);
        VMeasure ami = new VMeasure();
        assertEquals(0, ami.aggregate(m), .0000001);
    }

    @Test public void testMultiClassOneCluster() {
        Matrix m = new ArrayMatrix(2, 1);
        m.set(0, 0, 24);
        m.set(1, 0, 2);
        VMeasure ami = new VMeasure();
        assertEquals(0, ami.aggregate(m), .0000001);
    }

    @Test public void testPerfectMatch() {
        Matrix m = new ArrayMatrix(4, 4);
        m.set(0,0, 5);
        m.set(1,1, 6);
        m.set(2,2, 2);
        m.set(3,3, 15);

        VMeasure ami = new VMeasure();
        assertEquals(1.0, ami.aggregate(m), .0000001);
    }

    @Test public void testEmptyRow() {
        double[][] a = {{4d, 0d, 0d, 1d},
                        {1d, 3d, 2d, 1d},
                        {0d, 0d, 0d, 0d},
                        {6d, 0d, 0d, 6d}};

        VMeasure ami = new VMeasure();
        assertEquals(0.3305, ami.aggregate(new ArrayMatrix(a)), .0001);
    }

    @Test public void testEmptyColumn() {
        double[][] a = {{4d, 0d, 0d, 1d},
                        {1d, 3d, 0d, 1d},
                        {0d, 2d, 0d, 0d},
                        {6d, 0d, 0d, 6d}};

        VMeasure ami = new VMeasure();
        assertEquals(0.3558, ami.aggregate(new ArrayMatrix(a)), .0001);
    }
}
