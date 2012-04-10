package edu.ucla.sspace.clustering;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class CosinePartitionSimilarityTest {

    @Test public void testZeroDistance() {
        int[][] a1 = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        };
        int[][] a2 = {
            {0, 1, 2, 3, 4, 5, 6, 7, 8, 9},
        };
        PartitionOverlapComparisonTest.testComparison(
                a1, a2, new CosinePartitionSimilarity(), 1);
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
        PartitionOverlapComparisonTest.testComparison(
                a1, a2, new CosinePartitionSimilarity(), 0);
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
        PartitionOverlapComparisonTest.testComparison(
                a1, a2, new CosinePartitionSimilarity(),
                6 / Math.sqrt(13*8));
    }

    @Test public void testPartialPartition() {
        int[][] a1 = {
            {0, 1, 2, 3, 14, 5, 6, 7, 18, 9},
        };
        int[][] a2 = {
            {0, 1, 2, 3, 14, 5, 6, 7, 18, 9},
        };
        PartitionOverlapComparisonTest.testComparison(
                a1, a2, new CosinePartitionSimilarity(), 1);
    }

    @Test public void testNoOverlapPartialPartition() {
        int[][] a1 = {
            {0, 1, 12, 3},
            {4, 5, 6, 7},
            {18, 9},
        };
        int[][] a2 = {
            {0, 4, 18},
            {1, 5, 9},
            {12, 6},
            {3, 7},
        };
        PartitionOverlapComparisonTest.testComparison(
                a1, a2, new CosinePartitionSimilarity(), 0);
    }

    @Test public void testPartialOverlapPartialPartition() {
        int[][] a1 = {
            {0, 11, 2, 3},
            {4, 15, 6, 7},
            {8, 9},
        };
        int[][] a2 = {
            {0, 4, 2},
            {7, 15, 6},
            {8, 9},
            {3, 11},
        };
        PartitionOverlapComparisonTest.testComparison(
                a1, a2, new CosinePartitionSimilarity(), 
                6 / Math.sqrt(13*8));
    }
}
