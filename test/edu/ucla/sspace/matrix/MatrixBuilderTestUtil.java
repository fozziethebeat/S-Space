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

import edu.ucla.sspace.matrix.MatrixIO.Format;

import edu.ucla.sspace.util.SparseDoubleArray;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class MatrixBuilderTestUtil {

    public static void testMatrixBuild(MatrixBuilder builder, Matrix m) {
        testMatrixBuild(builder, m, false);
    }

    public static void testMatrixBuild(MatrixBuilder builder,
                                       Matrix m,
                                       boolean transposed) {
            // Write the matrix to the file using the given builder.
            for (int r = 0; r < m.rows(); ++r)
                builder.addColumn(m.getRowVector(r));
            builder.finish();
            
            if (transposed)
                checkMatrixTransposed(builder, m);
            else
                checkMatrix(builder, m);
    }

    public static void testArrayBuild(MatrixBuilder builder, Matrix m) {
        testArrayBuild(builder, m, false);
    }

    public static void testArrayBuild(MatrixBuilder builder,
                                      Matrix m, 
                                      boolean transposed) {
            // Write the matrix to the file using the given builder.
            for (int r = 0; r < m.rows(); ++r)
                builder.addColumn(m.getRow(r));
            builder.finish();

            if (transposed)
                checkMatrixTransposed(builder, m);
            else
                checkMatrix(builder, m);
    }

    public static void testSparseArrayBuild(MatrixBuilder builder, Matrix m) {
        testSparseArrayBuild(builder, m, false);
    }

    public static void testSparseArrayBuild(MatrixBuilder builder,
                                            Matrix m,
                                            boolean transposed) {
            // Write the matrix to the file using the given builder.
            for (int r = 0; r < m.rows(); ++r)
                builder.addColumn(new SparseDoubleArray(m.getRow(r)));
            builder.finish();

            if (transposed)
                checkMatrixTransposed(builder, m);
            else
                checkMatrix(builder, m);
    }

    public static void checkMatrix(MatrixBuilder builder, Matrix m) {
        try {
            // Read the matrix back in using the standard matrix io tools.
            File mFile = builder.getFile();
            Format format = builder.getMatrixFormat();
            Matrix out = MatrixIO.readMatrix(mFile, format);
            MatrixFile matrixFile = builder.getMatrixFile();
            assertEquals(mFile, matrixFile.getFile());
            assertEquals(format, matrixFile.getFormat());

            // Check created value against the expected.
            assertEquals(out.rows(), m.rows());
            assertEquals(out.columns(), m.columns());
            for (int r = 0; r < out.rows(); ++r)
                for (int c = 0; c < out.columns(); ++c)
                    assertEquals(m.get(r,c), out.get(r,c), .00001);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    public static void checkMatrixTransposed(MatrixBuilder builder, Matrix m) {
        try {
            // Read the matrix back in using the standard matrix io tools.
            File mFile = builder.getFile();
            Format format = builder.getMatrixFormat();
            MatrixFile matrixFile = builder.getMatrixFile();
            assertEquals(mFile, matrixFile.getFile());
            assertEquals(format, matrixFile.getFormat());

            Matrix out = MatrixIO.readMatrix(mFile, format);

            // Check created value against the expected.
            assertEquals(out.rows(), m.columns());
            assertEquals(out.columns(), m.rows());
            for (int r = 0; r < out.rows(); ++r)
                for (int c = 0; c < out.columns(); ++c)
                    assertEquals(m.get(c,r), out.get(r,c), .00001);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
