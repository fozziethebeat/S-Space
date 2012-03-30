package edu.ucla.sspace.clustering;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class PartitionOverlapComparisonTest {

    @Test public void testOverlapWithSingleCluster() {
        int[][] a1 = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        };
        int[][] a2 = {
            {0, 1, 2, 3},
            {4, 5, 6, 7},
            {8, 9},
        };
        Partition p1 = PartitionTest.loadFromArray(a1);
        Partition p2 = PartitionTest.loadFromArray(a2);

        PartitionComparison comp = new PartitionOverlapComparison();

        assertEquals(13, comp.compare(p1, p2), .000001);
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
        Partition p1 = PartitionTest.loadFromArray(a1);
        Partition p2 = PartitionTest.loadFromArray(a2);

        PartitionComparison comp = new PartitionOverlapComparison();

        assertEquals(13, comp.compare(p1, p2), .000001);
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
        Partition p1 = PartitionTest.loadFromArray(a1);
        Partition p2 = PartitionTest.loadFromArray(a2);

        PartitionComparison comp = new PartitionOverlapComparison();

        assertEquals(0, comp.compare(p1, p2), .000001);
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
        Partition p1 = PartitionTest.loadFromArray(a1);
        Partition p2 = PartitionTest.loadFromArray(a2);

        PartitionComparison comp = new PartitionOverlapComparison();

        assertEquals(6, comp.compare(p1, p2), .000001);
    }
}
