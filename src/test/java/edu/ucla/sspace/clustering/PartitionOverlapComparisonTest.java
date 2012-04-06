package edu.ucla.sspace.clustering;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class PartitionOverlapComparisonTest {

    public static void testComparison(int[][] a1, int[][] a2, 
                                      PartitionComparison comp,
                                      double expected) {
        Partition p1 = PartitionTest.loadFromArray(a1);
        Partition p2 = PartitionTest.loadFromArray(a2);
        assertEquals(expected, comp.compare(p1, p2), .000001);
    }

    @Test public void testOverlapWithSingleCluster() {
        int[][] a1 = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        };
        int[][] a2 = {
            {0, 1, 2, 3},
            {4, 5, 6, 7},
            {8, 9},
        };
        testComparison(a1, a2, new PartitionOverlapComparison(), 13);
    }

    @Test public void testPerfectOverlap() {
        int[][] a1 = {
            {0, 1, 2, 3},
            {4, 5, 6, 7},
            {8, 9},
        };
        int[][] a2 = {
            {0, 1, 2, 3},
            {4, 5, 6, 7},
            {8, 9},
        };
        testComparison(a1, a2, new PartitionOverlapComparison(), 13);
    }

    @Test public void testNoOverlap() {
        int[][] a1 = {
            {0, 1, 2, 3},
            {4, 5, 6, 7},
            {8, 9},
        };
        int[][] a2 = {
            {0, 4, 8},
            {1, 5, 9},
            {2, 6},
            {3, 7},
        };
        testComparison(a1, a2, new PartitionOverlapComparison(), 0);
    }

    @Test public void testPartialOverlap() {
        int[][] a1 = {
            {0, 1, 2, 3},
            {4, 5, 6, 7},
            {8, 9},
        };
        int[][] a2 = {
            {0, 4, 2},
            {7, 5, 6},
            {8, 9},
            {3, 1},
        };
        testComparison(a1, a2, new PartitionOverlapComparison(), 6);
    }

    @Test public void testOverlapWithSingleClusterPartial() {
        int[][] a1 = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 19},
        };
        int[][] a2 = {
            {0, 1, 2, 3},
            {4, 5, 6, 7},
            {8, 19},
        };
        testComparison(a1, a2, new PartitionOverlapComparison(), 13);
    }

    @Test public void testPerfectOverlapPartial() {
        int[][] a1 = {
            {0, 1, 2, 3},
            {4, 5, 6, 17},
            {8, 9},
        };
        int[][] a2 = {
            {0, 1, 2, 3},
            {4, 5, 6, 17},
            {8, 9},
        };
        testComparison(a1, a2, new PartitionOverlapComparison(), 13);
    }

    @Test public void testNoOverlapPartial() {
        int[][] a1 = {
            {0, 1, 2, 3},
            {4, 5, 6, 17},
            {8, 9},
        };
        int[][] a2 = {
            {0, 4, 8},
            {1, 5, 9},
            {2, 6},
            {3, 17},
        };
        testComparison(a1, a2, new PartitionOverlapComparison(), 0);
    }

    @Test public void testPartialOverlapPartial() {
        int[][] a1 = {
            {0, 1, 2, 3},
            {4, 5, 6, 17},
            {8, 9},
        };
        int[][] a2 = {
            {0, 4, 2},
            {17, 5, 6},
            {8, 9},
            {3, 1},
        };
        testComparison(a1, a2, new PartitionOverlapComparison(), 6);
    }
}
