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
public class SvdlibcSparseBinaryMatrixBuilderTests {
    
    private static final double[][] values = {{1, 0, 1, 2, 4},
                                              {3, 5, 0, 0, 1},
                                              {1, 1, 1, 0, 0}};

    /**
     * Checks that the lines in a transposed {@code mFile} matches the values in
     * {@code values}.
     */
    private static void testFileWithData(double[][] values, File mFile) {
        try {
            DataInputStream dos = new DataInputStream(new FileInputStream(
                        mFile));
            assertEquals(values[0].length, dos.readInt());
            assertEquals(values.length, dos.readInt());
            dos.readInt();

            for (int col = 0; col < values.length; ++col) {
                int nonZeros = dos.readInt();
                for (int row = 0; row < values[col].length; ++row) {
                    if (values[col][row] != 0d) {
                        int c = dos.readInt();
                        assertEquals(c, row);

                        double v = dos.readFloat();
                        assertEquals(values[col][row], v, .0001);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Checks that the lines in a transposed {@code mFile} matches the values in
     * {@code values}.
     */
    private static void testFileWithDataTransposed(double[][] values,
                                                   File mFile) {
        try {
            DataInputStream dos = new DataInputStream(new FileInputStream(
                        mFile));
            assertEquals(values.length, dos.readInt());
            assertEquals(values[0].length, dos.readInt());
            dos.readInt();

            for (int row = 0; row < values[0].length; ++row) {
                int nonZeros = dos.readInt();
                for (int col = 0; col < values.length; ++col) {
                    if (values[col][row] != 0d) {
                        int c = dos.readInt();
                        assertEquals(c, col);

                        double v = dos.readFloat();
                        assertEquals(values[col][row], v, .0001);
                    }
                }
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }


    @Test public void testAddSparseVectorColumn() {
        MatrixBuilder builder = new SvdlibcSparseBinaryMatrixBuilder();
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new CompactSparseVector(values[col]));

        builder.finish();
        testFileWithData(values, builder.getFile());
    }

    @Test public void testAddSparseVectorColumnTranspose() {
        MatrixBuilder builder = new SvdlibcSparseBinaryMatrixBuilder(true);
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new CompactSparseVector(values[col]));

        builder.finish();
        testFileWithDataTransposed(values, builder.getFile());
    }

    @Test public void testAddDoubleColumn() {
        MatrixBuilder builder = new SvdlibcSparseBinaryMatrixBuilder();
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(values[col]);

        builder.finish();
        testFileWithData(values, builder.getFile());
    }

    @Test public void testAddDoubleColumnTraspose() {
        MatrixBuilder builder = new SvdlibcSparseBinaryMatrixBuilder(true);
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(values[col]);

        builder.finish();
        testFileWithDataTransposed(values, builder.getFile());
    }

    @Test public void testAddDenseVectorColumn() {
        MatrixBuilder builder = new SvdlibcSparseBinaryMatrixBuilder();
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new DenseVector(values[col]));

        builder.finish();
        testFileWithData(values, builder.getFile());
    }

    @Test public void testAddDenseVectorColumnTranspose() {
        MatrixBuilder builder = new SvdlibcSparseBinaryMatrixBuilder(true);
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new DenseVector(values[col]));

        builder.finish();
        testFileWithDataTransposed(values, builder.getFile());
    }

    @Test public void testAddSparseArrayColumn() {
        MatrixBuilder builder = new SvdlibcSparseBinaryMatrixBuilder();
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new SparseDoubleArray(values[col]));

        builder.finish();
        testFileWithData(values, builder.getFile());
    }

    @Test public void testAddSparseArrayColumnTranpose() {
        MatrixBuilder builder = new SvdlibcSparseBinaryMatrixBuilder(true);
        for (int col = 0; col < values.length; ++col)
            builder.addColumn(new SparseDoubleArray(values[col]));

        builder.finish();
        testFileWithDataTransposed(values, builder.getFile());
    }
}
