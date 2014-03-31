/*
 * Copyright 2014 David Jurgens
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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DoubleVector;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author David Jurgens
 */
public class AssignmentsTest {

    @Test public void testGetCentroidsWithNegativeIds() {
        double[][] values = {
            {1, 1, 2, 4},
            {4, 3, 2, 1},
            {7, 8, 5, 0},
            {3, 8, 0, 0},
            {7, 2, 9, 2},
        };

        Matrix matrix = new ArrayMatrix(values);
        Assignment[] as = new Assignment[] {
            new HardAssignment(0),
            new HardAssignment(0),
            new HardAssignment(1),
            new HardAssignment(1),
            new HardAssignment(-1)
        };
        

        Assignments a = new Assignments(2, as, matrix);
        a.getCentroids();
    }

    @Test public void testGetSparseCentroidsWithNegativeIds() {
        double[][] values = {
            {1, 1, 2, 4},
            {4, 3, 2, 1},
            {7, 8, 5, 0},
            {3, 8, 0, 0},
            {7, 2, 9, 2},
        };

        Matrix matrix = new ArrayMatrix(values);
        Assignment[] as = new Assignment[] {
            new HardAssignment(0),
            new HardAssignment(0),
            new HardAssignment(1),
            new HardAssignment(1),
            new HardAssignment(-1)
        };
        

        Assignments a = new Assignments(2, as, matrix);
        a.getSparseCentroids();
    }

    @Test public void testGetClustersWithNegativeIds() {
        double[][] values = {
            {1, 1, 2, 4},
            {4, 3, 2, 1},
            {7, 8, 5, 0},
            {3, 8, 0, 0},
            {7, 2, 9, 2},
        };

        Matrix matrix = new ArrayMatrix(values);
        Assignment[] as = new Assignment[] {
            new HardAssignment(0),
            new HardAssignment(0),
            new HardAssignment(1),
            new HardAssignment(1),
            new HardAssignment(-1)
        };
        

        Assignments a = new Assignments(2, as, matrix);
        List<Set<Integer>> clusters = a.clusters();
        assertEquals(2, clusters.size());
        Set<Integer> items = new HashSet<Integer>();
        for (Set<Integer> s : clusters)
            items.addAll(s);
        assertEquals(4, items.size());
    }
}
