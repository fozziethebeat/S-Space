/*
 * Copyright 2011 Keith Stevens
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

import edu.ucla.sspace.util.*;
import edu.ucla.sspace.vector.*;

import java.io.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class MatlabSparseMatrixBuilderTest {
    
    private static final double[][] values = {{1, 0, 1, 2, 4},
                                              {3, 5, 0, 0, 1},
                                              {1, 1, 1, 0, 0}};

    /**
     * Checks that a single line has the correct values.
     */
    private static void checkLine(int row, int col, double value,
                                  String line) {
        // Split up the [col row value] in the line.
        String[] colRowValue = line.split("\\s+");

        // Matlab indices are off by one, so add 1 to each index.
        assertEquals(row+1, Integer.parseInt(colRowValue[0]));
        assertEquals(col+1, Integer.parseInt(colRowValue[1]));
        assertEquals(value, Double.parseDouble(colRowValue[2]), .000001);
    }

    /**
     * Checks that the lines in {@code mFile} matches the values in {@code
     * values}.
     */
    private static void testFileWithData(double[][] values, File mFile,
                                         boolean transposed) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(mFile));

            for (int col = 0; col < values.length; ++col) {
                for (int row = 0; row < values[col].length; ++row) {
                    if (values[col][row] != 0d) {
                        String line = reader.readLine();
                        if (transposed)
                            checkLine(col, row, values[col][row], line);
                        else
                            checkLine(row, col, values[col][row], line);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    @Test public void testAddSparseVectorColumn() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder();
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new CompactSparseVector(values[col]));

        builder.finish();
        testFileWithData(values, builder.getFile(), false);
    }

    @Test public void testAddSparseVectorColumnTranspose() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder(true);
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new CompactSparseVector(values[col]));

        builder.finish();
        testFileWithData(values, builder.getFile(), true);
    }

    @Test public void testAddDoubleColumn() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder();
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(values[col]);

        builder.finish();
        testFileWithData(values, builder.getFile(), false);
    }

    @Test public void testAddDoubleColumnTranspose() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder();
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(values[col]);

        builder.finish();
        testFileWithData(values, builder.getFile(), false);
    }

    @Test public void testAddDenseVectorColumn() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder(true);
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new DenseVector(values[col]));

        builder.finish();
        testFileWithData(values, builder.getFile(), true);
    }

    @Test public void testAddDenseVectorColumnTranpose() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder(true);
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new DenseVector(values[col]));

        builder.finish();
        testFileWithData(values, builder.getFile(), true);
    }

    @Test public void testAddSparseArrayColumn() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder();
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new SparseDoubleArray(values[col]));

        builder.finish();
        testFileWithData(values, builder.getFile(), false);
    }

    @Test public void testAddSparseArrayColumnTranspose() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder(true);
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new SparseDoubleArray(values[col]));

        builder.finish();
        testFileWithData(values, builder.getFile(), true);
    }
}
