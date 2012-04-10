package edu.ucla.sspace.clustering;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class PartitionTest {

    public static Partition loadFromArray(int[][] assignments) {
        List<Set<Integer>> clusters = new ArrayList<Set<Integer>>();
        for (int[] points : assignments) {
            Set<Integer> cluster = new HashSet<Integer>();
            for (int point : points)
                cluster.add(point);
            clusters.add(cluster);
        }
        return new Partition(clusters);
    }

    public static void validate(Partition partition, int[][] assignments) {
        assertEquals(assignments.length, partition.clusters().size());
        int size = 0;
        int numPairs = 0;
        for (int[] points : assignments) {
            for (int point : points)
                size = Math.max(point, size);
            numPairs += points.length * (points.length - 1) / 2;
        }
        assertEquals(size+1, partition.assignments().length);
        assertEquals(numPairs, partition.numPairs());

        for (int[] points : assignments)
            for (int i = 0; i < points.length; ++i)
                for (int j = 0; j < points.length; ++j)
                    assertTrue(partition.coClustered(points[i], points[j]));

        for (int l = 0; l < assignments.length; l++)
            for (int k = l+1; k < assignments.length; k++)
                for (int i = 0; i < assignments[l].length; ++i)
                    for (int j = 0; j < assignments[k].length; ++j) {
                        assertFalse(partition.coClustered(assignments[l][i],
                                                          assignments[k][j]));
                        assertFalse(partition.coClustered(assignments[k][j],
                                                          assignments[l][i]));
                    }
    }

    @Test public void testCreateNewFullPartition() {
        int[][] assignments = {
            {1, 2, 3, 4},
            {0, 5, 10, 8},
            {6, 7, 9},
        };
        Partition partition = loadFromArray(assignments);
        validate(partition, assignments);
    }

    @Test public void testCreateNewPartialPartition() {
        int[][] assignments = {
            {1, 2, 3, 4},
            {0, 5, 20, 8},
            {6, 7, 9},
        };
        Partition partition = loadFromArray(assignments);
        validate(partition, assignments);
        for (int i = 0; i < 10; ++i)
            assertTrue(-1 != partition.assignments()[i]);
        for (int i = 10; i < 20; ++i)
            assertEquals(-1, partition.assignments()[i]);
    }

    @Test public void testMove() {
        int[][] assignments = {
            {1, 2, 3, 4},
            {0, 5, 10, 8},
            {6, 7, 9},
        };
        Partition partition = loadFromArray(assignments);

        assertFalse(partition.move(0, 3));
        assertFalse(partition.move(0, -1));

        assertFalse(partition.move(0, 1));

        assertTrue(partition.move(0, 2));
    }

    @Test public void testAssignments() {
        int[][] assignments = {
            {1, 2, 3, 4},
            {0, 5, 10, 8},
            {6, 7, 9},
        };
        Partition partition = loadFromArray(assignments);
        for (int cid = 0; cid < assignments.length; ++cid)
            for (int point : assignments[cid])
                assertEquals(cid, partition.assignments()[point]);
    }

    @Test public void testReadMatrixFromFile() throws Exception {
        int[][] assignments = {
            {1, 2, 3, 4},
            {0, 5, 10, 8},
            {6, 7, 9},
        };
        File f = File.createTempFile("partitionTest", "rca");
        f.deleteOnExit();
        PrintWriter pw = new PrintWriter(f);
        pw.println("11 3");
        pw.println("1 2 3 4");
        pw.println("0 5 10 8");
        pw.println("6 7 9");
        pw.close();

        Partition partition = Partition.read(f.toString());
        validate(partition, assignments);
    }

    @Test public void testReadMatrixFromFileWithBlank() throws Exception {
        int[][] assignments = {
            {1, 2, 3, 4},
            {0, 5, 10, 8},
            {6, 7, 9},
        };
        File f = File.createTempFile("partitionTest", "rca");
        f.deleteOnExit();
        PrintWriter pw = new PrintWriter(f);
        pw.println("11 4");
        pw.println("1 2 3 4");
        pw.println();
        pw.println("0 5 10 8");
        pw.println("6 7 9");
        pw.close();

        Partition partition = Partition.read(f.toString());
        validate(partition, assignments);
    }

    @Test public void testCopy() {
        int[][] assignments = {
            {1, 2, 3, 4},
            {0, 5, 10, 8},
            {6, 7, 9},
        };
        Partition p1 = loadFromArray(assignments);
        Partition p2 = Partition.copyOf(p1);

        p1.move(0, 0);
        assertEquals(1, p2.assignments()[0]);

        p2.move(6, 1);
        assertEquals(2, p1.assignments()[6]);
    }
}
