/*
 * Copyright 2010 Keith Stevens
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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CellMaskedMatrixTest {


    private static double[][] baseValues = new double[][]{
        { 0, 1, 2, 3, 4, 5, 6},
        { 1, 2, 3, 4, 5, 6, 7},
        { 3, 4, 5, 6, 7, 8, 9},
        { 9, 8, 7, 6, 5, 4, 3}};

    private static int[] rowMap = new int[3];
    private static int[] colMap = new int[4];

    static {
        rowMap[0] = 0;
        rowMap[1] = 3;
        rowMap[2] = 2;

        colMap[0] = 6;
        colMap[1] = 0;
        colMap[2] = 4;
        colMap[3] = 5;
    }

    public CellMaskedMatrixTest() { }

    private double getBaseValue(Matrix baseMatrix, int row, int col) {
        return baseMatrix.get(rowMap[row], colMap[col]);
    }

    @Test
    public void testSize() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        assertEquals(3, maskedMatrix.rows()); 
        assertEquals(4, maskedMatrix.columns()); 
    }

    @Test
    public void testGet() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        for (int r = 0; r < maskedMatrix.rows(); ++r)
            for (int c = 0; c < maskedMatrix.columns(); ++c)
                assertEquals(getBaseValue(baseMatrix, r, c),
                             maskedMatrix.get(r, c), .00001);
    }

    @Test(expected= IndexOutOfBoundsException.class)
    public void testInvalidRowGet() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        maskedMatrix.get(3, 0);
    }

    @Test(expected= IndexOutOfBoundsException.class)
    public void testInvalidColumnGet() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        maskedMatrix.get(0, 6);
    }

    @Test
    public void testGetRow() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        for (int r = 0; r < maskedMatrix.rows(); ++r) {
            double[] row = maskedMatrix.getRow(r);
            assertEquals(4, row.length);
            for (int c = 0; c < 4; ++c)
                assertEquals(getBaseValue(baseMatrix, r, c), row[c], .00001);
        }
    }

    @Test
    public void testGetRowVector() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        for (int r = 0; r < maskedMatrix.rows(); ++r) {
            DoubleVector rowVector = maskedMatrix.getRowVector(r);
            assertEquals(4, rowVector.length());
            for (int c = 0; c < 4; ++c)
                assertEquals(getBaseValue(baseMatrix, r, c),
                             rowVector.get(c), .000001);
        }
    }

    @Test(expected= IndexOutOfBoundsException.class)
    public void testInvalidGetRowVector() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        maskedMatrix.getRowVector(4);
    }

    @Test(expected= IndexOutOfBoundsException.class)
    public void testInvalidGetRow() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        maskedMatrix.getRow(4);
    }

    @Test
    public void testGetColumn() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        for (int r = 0; r < maskedMatrix.columns(); ++r) {
            double[] col = maskedMatrix.getColumn(r);
            assertEquals(3, col.length);
            for (int c = 0; c < 3; ++c)
                assertEquals(getBaseValue(baseMatrix, c, r), col[c], .00001);
        }
    }

    @Test
    public void testGetColumnVector() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        for (int c = 0; c < maskedMatrix.columns(); ++c) {
            DoubleVector colVector = maskedMatrix.getColumnVector(c);
            assertEquals(3, colVector.length());
            for (int r = 0; r < 3; ++r) 
                assertEquals(getBaseValue(baseMatrix, r, c),
                             colVector.get(r), .000002);
        }
    }

    @Test(expected= IndexOutOfBoundsException.class)
    public void testInvalidGetColumnVector() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        maskedMatrix.getColumnVector(5);
    }

    @Test(expected= IndexOutOfBoundsException.class)
    public void testInvalidGetColumn() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        maskedMatrix.getColumn(5);
    }

    @Test
    public void testSetRow() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        double[] row = new double[]{1, 2, 3, 4};
        maskedMatrix.setRow(0, row);
        for (int i = 0; i < 4; i++)
            assertEquals(getBaseValue(baseMatrix, 0, i), row[i], .000001);
    }

    @Test
    public void testSetRowVector() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        DoubleVector row = new DenseVector(new double[]{1, 2, 3, 4});
        maskedMatrix.setRow(0, row);
        for (int i = 0; i < 4; i++)
            assertEquals(getBaseValue(baseMatrix, 0, i), row.get(i), .000001);
    }

    @Test
    public void testSetColumn() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        double[] col = new double[] {1, 2, 3};
        maskedMatrix.setColumn(0, col);
        for (int i = 0; i < 3; i++)
            assertEquals(getBaseValue(baseMatrix, i, 0), col[i], .000001);
    }

    @Test
    public void testSetColumnVector() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        DoubleVector col = new DenseVector(new double[] {1, 2, 3});
        maskedMatrix.setColumn(0, col);
        for (int i = 0; i < 3; i++)
            assertEquals(getBaseValue(baseMatrix, i, 0), col.get(i), .000001);
    }

    @Test
    public void testToDenseArray() {
        Matrix baseMatrix = new ArrayMatrix(baseValues);
        Matrix maskedMatrix = new CellMaskedMatrix(baseMatrix, rowMap, colMap);
        double[][] arr = maskedMatrix.toDenseArray();
        for (int r = 0; r < 3; ++r)
            for (int c = 0; c < 4; ++c)
                assertEquals(getBaseValue(baseMatrix, r, c), arr[r][c], .00001);

    }
}
