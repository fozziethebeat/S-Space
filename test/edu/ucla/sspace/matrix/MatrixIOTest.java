/*
 * Copyright 2009 David Jurgens
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

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.text.*; 
import edu.ucla.sspace.util.*;

import java.io.*;
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class MatrixIOTest {

    public static double[][] testMatrix = {
        {2.3, 0, 4.2},
        {0, 1.3, 2.2},
        {3.8, 0, 0.5}};

    public static File getMatlabFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        PrintWriter pw = new PrintWriter(f);
        pw.println("1 1 2.3");
        pw.println("1 3 4.2");
        pw.println("2 2 1.3");
        pw.println("2 3 2.2");
        pw.println("3 1 3.8");
        pw.println("3 3 0.5"); 
        pw.close();
        return f;
    }

    public static File getDenseTextFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        PrintWriter pw = new PrintWriter(f);
        pw.println("2.3 0 4.2");
        pw.println("0 1.3 2.2");
        pw.println("3.8 0 0.5");
        pw.close();
        return f;
    }

    public static File getClutoSparseFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        PrintWriter pw = new PrintWriter(f);

        pw.println("3 3 6");

        pw.println("1 2.3 3 4.2");
        pw.println("2 1.3 3 2.2");
        pw.println("1 3.8 3 0.5");
        pw.close();
        return f;
    }

    public static File getSVDLIBCDenseBinaryFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
        dos.writeInt(testMatrix.length);
        dos.writeInt(testMatrix[0].length);
        for (int i = 0; i < testMatrix.length; ++i)
            for (int j = 0; j < testMatrix[0].length; ++j)
                dos.writeFloat((float) testMatrix[i][j]);
        dos.close();
        return f;
    }

    public static File getSVDLIBCDenseTextFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        PrintWriter pw = new PrintWriter(f);
        pw.println("3 3");
        pw.println("2.3 0 4.2");
        pw.println("0 1.3 2.2");
        pw.println("3.8 0 0.5");
        pw.close();
        return f;
    }

    public static File getSparseSVDLIBCFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        PrintWriter pw = new PrintWriter(f);
        pw.println("3 3 6");

        pw.println("2");
        pw.println("0 2.3");
        pw.println("2 3.8");

        pw.println("1");
        pw.println("1 1.3");

        pw.println("3");
        pw.println("0 4.2");
        pw.println("1 2.2");
        pw.println("2 0.5");
        pw.close();
        return f;
    }

    public static File getSparseBinarySVDLIBCFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        DataOutputStream pw = new DataOutputStream(new FileOutputStream(f));
        pw.writeInt(3);
        pw.writeInt(3);
        pw.writeInt(6);

        pw.writeInt(2);
        pw.writeInt(0);
        pw.writeFloat(2.3f);
        pw.writeInt(2);
        pw.writeFloat(3.8f);

        pw.writeInt(1);
        pw.writeInt(1);
        pw.writeFloat(1.3f);

        pw.writeInt(3);
        pw.writeInt(0);
        pw.writeFloat(4.2f);
        pw.writeInt(1);
        pw.writeFloat(2.2f);
        pw.writeInt(2);
        pw.writeFloat(0.5f);

        pw.close();
        return f;
    }

    @Test public void matlabToSVDLIBCsparse() throws Exception {
        
        File matlab = getMatlabFile();
        File svdlibc = MatrixIO.convertFormat(matlab,
            MatrixIO.Format.MATLAB_SPARSE, MatrixIO.Format.SVDLIBC_SPARSE_TEXT);
        
        WordIterator it = new WordIterator(
            new BufferedReader(new FileReader(svdlibc)));

        // size
        assertEquals("3", it.next());
        assertEquals("3", it.next());
        assertEquals("6", it.next());

        // col 1
        assertEquals("2", it.next());
        assertEquals("0", it.next());
        assertEquals("2.3", it.next());
        assertEquals("2", it.next());
        assertEquals("3.8", it.next());

        // col 2
        assertEquals("1", it.next());
        assertEquals("1", it.next());
        assertEquals("1.3", it.next());

        // col 3
        assertEquals("3", it.next());
        assertEquals("0", it.next());
        assertEquals("4.2", it.next());
        assertEquals("1", it.next());
        assertEquals("2.2", it.next());
        assertEquals("2", it.next());
        assertEquals("0.5", it.next());
    }

    /**
     * Tests that a result {@link Matrix} contains the same data values as in
     * {@code testMatrix}.
     */
    public static void testReadMatrix(Matrix resultMatrix) {
        assertEquals(testMatrix.length, resultMatrix.rows()); 
        assertEquals(testMatrix[0].length, resultMatrix.columns());
        int rows = resultMatrix.rows();
        int cols = resultMatrix.columns();
        for (int i = 0; i < rows; ++i)
            for (int j = 0; j < cols; ++j) 
                assertEquals(testMatrix[i][j], resultMatrix.get(i, j), .01);
    }

    /**
     * Tests that a result {@link Matrix} contains the same data values as in
     * {@code testMatrix}.
     */
    public static void testReadMatrixTransposed(Matrix resultMatrix) {
        assertEquals(testMatrix[0].length, resultMatrix.columns());
        assertEquals(testMatrix.length, resultMatrix.rows()); 
        int rows = resultMatrix.rows();
        int cols = resultMatrix.columns();
        for (int i = 0; i < rows; ++i)
            for (int j = 0; j < cols; ++j) 
                assertEquals(testMatrix[i][j], resultMatrix.get(j, i), .01);
    }

    /**
     * Tests that a result double array contains the same data values as in
     * {@code testMatrix}.
     */
    @SuppressWarnings("deprecation")
    public static void testReadDoubleArray(double[][] resultMatrix) {
        assertEquals(testMatrix.length, resultMatrix.length); 
        assertEquals(testMatrix[0].length, resultMatrix[0].length); 
        int rows = testMatrix.length;
        int cols = testMatrix[0].length;
        for (int i = 0; i < rows; ++i) {
            for (int j = 0; j < cols; ++j) {
                assertEquals(testMatrix[i][j], resultMatrix[i][j], 0.0001);
            }
        }
    }

    /**
     * Tests that all of the values returned by a {@link Iterator} over {@link
     * MatrixEntry}s matches the expected values in {@code testMatrixIterator}.
     */
    public static void testMatrixIterator(Iterator<MatrixEntry> matrixIter) {
        while (matrixIter.hasNext()) {
            MatrixEntry entry = matrixIter.next();
            int col = entry.column();
            int row = entry.row();
            double value = entry.value();
            assertEquals(testMatrix[row][col], value, .0001);
        }
    }

    /**
     * Test the reading of tranposed {@link Matrix}s from matrix files.
     */

    @Test public void readTransposedMatrixFromMatlabSparse() throws Exception {
        File matlab = getMatlabFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                matlab, MatrixIO.Format.MATLAB_SPARSE,
                Matrix.Type.SPARSE_IN_MEMORY, true);
        assertTrue(resultMatrix instanceof SparseMatrix);
        testReadMatrixTransposed(resultMatrix);
    }

    @Test public void readTransposedMatrixFromSVDLIBCSparseText() throws Exception {
        File svdlibcSparseText = getSparseSVDLIBCFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                svdlibcSparseText, MatrixIO.Format.SVDLIBC_SPARSE_TEXT,
                Matrix.Type.SPARSE_IN_MEMORY, true);
        assertTrue(resultMatrix instanceof SparseMatrix);
        testReadMatrixTransposed(resultMatrix);
    }

    @Test public void readTransposedMatrixFromSVDLIBCSparseBinary() throws Exception {
        File svdlibcSparseText = getSparseBinarySVDLIBCFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                svdlibcSparseText, MatrixIO.Format.SVDLIBC_SPARSE_BINARY,
                Matrix.Type.SPARSE_IN_MEMORY, true);
        assertTrue(resultMatrix instanceof SparseMatrix);
        testReadMatrixTransposed(resultMatrix);
    }

    @Test public void readTransposedMatrixFromSVDLIBCDenseText() throws Exception {
        File mFile = getSVDLIBCDenseTextFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_TEXT,
                Matrix.Type.DENSE_IN_MEMORY, true);
        testReadMatrixTransposed(resultMatrix);
    }

    @Test public void readTransposedMatrixFromSVDLIBCDenseBinary() throws Exception {
        File mFile = getSVDLIBCDenseBinaryFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_BINARY,
                Matrix.Type.DENSE_IN_MEMORY, true);
        testReadMatrixTransposed(resultMatrix);
    }

    @Test public void readTransposedMatrixFromClutoSparse() throws Exception {
        // Note that CLUTO Dense and SVDLIBC Dense Text are the same format.
        File mFile = getClutoSparseFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.CLUTO_SPARSE,
                Matrix.Type.SPARSE_IN_MEMORY, true);
        assertTrue(resultMatrix instanceof SparseMatrix);
        testReadMatrixTransposed(resultMatrix);
    }

    @Test public void readTransposedMatrixFromClutoDense() throws Exception {
        // Note that CLUTO Dense and SVDLIBC Dense Text are the same format.
        File mFile = getSVDLIBCDenseTextFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.CLUTO_DENSE,
                Matrix.Type.DENSE_IN_MEMORY, true);
        testReadMatrixTransposed(resultMatrix);
    }

    @Test public void readTransposedMatrixFromDenseText() throws Exception {
        File mFile = getDenseTextFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.DENSE_TEXT,
                Matrix.Type.DENSE_IN_MEMORY, true);
        testReadMatrixTransposed(resultMatrix);
    }

    /**
     * Test the reading of a {@link Matrix} from matrix files.
     */

    @Test public void readMatrixFromMatlabSparse() throws Exception {
        File matlab = getMatlabFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                matlab, MatrixIO.Format.MATLAB_SPARSE);
        assertTrue(resultMatrix instanceof SparseMatrix);
        testReadMatrix(resultMatrix);
    }

    @Test public void readMatrixFromSVDLIBCSparseText() throws Exception {
        File svdlibcSparseText = getSparseSVDLIBCFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                svdlibcSparseText, MatrixIO.Format.SVDLIBC_SPARSE_TEXT);
        assertTrue(resultMatrix instanceof SparseMatrix);
        testReadMatrix(resultMatrix);
    }

    @Test public void readMatrixFromSVDLIBCSparseBinary() throws Exception {
        File svdlibcSparseText = getSparseBinarySVDLIBCFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                svdlibcSparseText, MatrixIO.Format.SVDLIBC_SPARSE_BINARY);
        assertTrue(resultMatrix instanceof SparseMatrix);
        testReadMatrix(resultMatrix);
    }

    @Test public void readMatrixFromSVDLIBCDenseText() throws Exception {
        File mFile = getSVDLIBCDenseTextFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_TEXT);
        testReadMatrix(resultMatrix);
    }

    @Test public void readMatrixFromSVDLIBCDenseBinary() throws Exception {
        File mFile = getSVDLIBCDenseBinaryFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_BINARY);
        testReadMatrix(resultMatrix);
    }

    @Test public void readMatrixFromClutoSparse() throws Exception {
        // Note that CLUTO Dense and SVDLIBC Dense Text are the same format.
        File mFile = getClutoSparseFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.CLUTO_SPARSE);
        assertTrue(resultMatrix instanceof SparseMatrix);
        testReadMatrix(resultMatrix);
    }

    @Test public void readMatrixFromClutoDense() throws Exception {
        // Note that CLUTO Dense and SVDLIBC Dense Text are the same format.
        File mFile = getSVDLIBCDenseTextFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.CLUTO_DENSE);
        testReadMatrix(resultMatrix);
    }

    @Test public void readMatrixFromDenseText() throws Exception {
        File mFile = getDenseTextFile();
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.DENSE_TEXT);
        testReadMatrix(resultMatrix);
    }

    /**
     * Test the writing of {@link Matrix} objects to matrix files. Note that
     * these tests assume that reading works properly.  This isn't the best
     * testing practice, but it's makes writing the tests significantly less
     * error prone.
     */

    @Test public void writeDenseTextMatrixFile() throws Exception {
        // Create a test matrix and write it out the the desired format.
        Matrix m = new ArrayMatrix(testMatrix);
        File mFile = File.createTempFile("test-matrix", "dat");
        MatrixIO.writeMatrix(m, mFile, MatrixIO.Format.DENSE_TEXT);

        // Then read it back in and ensure that it contains the right values.
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.DENSE_TEXT);
        testReadMatrix(resultMatrix);
    }

    @Test public void writeClutoSparseMatrixFile() throws Exception {
        // Create a test matrix and write it out the the desired format.
        Matrix m = new ArrayMatrix(testMatrix);
        File mFile = File.createTempFile("test-matrix", "dat");
        MatrixIO.writeMatrix(m, mFile, MatrixIO.Format.CLUTO_SPARSE);

        // Then read it back in and ensure that it contains the right values.
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.CLUTO_SPARSE);
        testReadMatrix(resultMatrix);
    }

    @Test public void writeClutoDenseMatrixFile() throws Exception {
        // Create a test matrix and write it out the the desired format.
        Matrix m = new ArrayMatrix(testMatrix);
        File mFile = File.createTempFile("test-matrix", "dat");
        MatrixIO.writeMatrix(m, mFile, MatrixIO.Format.CLUTO_DENSE);

        // Then read it back in and ensure that it contains the right values.
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.CLUTO_DENSE);
        testReadMatrix(resultMatrix);
    }

    @Test public void writeSvdlibcDenseTextMatrixFile() throws Exception {
        // Create a test matrix and write it out the the desired format.
        Matrix m = new ArrayMatrix(testMatrix);
        File mFile = File.createTempFile("test-matrix", "dat");
        MatrixIO.writeMatrix(m, mFile, MatrixIO.Format.SVDLIBC_DENSE_TEXT);

        // Then read it back in and ensure that it contains the right values.
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_TEXT);
        testReadMatrix(resultMatrix);
    }

    @Test public void writeSvdlibcDenseBinaryMatrixFile() throws Exception {
        // Create a test matrix and write it out the the desired format.
        Matrix m = new ArrayMatrix(testMatrix);
        File mFile = File.createTempFile("test-matrix", "dat");
        MatrixIO.writeMatrix(m, mFile, MatrixIO.Format.SVDLIBC_DENSE_BINARY);

        // Then read it back in and ensure that it contains the right values.
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_BINARY);
        testReadMatrix(resultMatrix);
    }

    @Test public void writeSvdlibcSparseBinaryMatrixFile() throws Exception {
        // Create a test matrix and write it out the the desired format.
        Matrix m = new ArrayMatrix(testMatrix);
        File mFile = File.createTempFile("test-matrix", "dat");
        MatrixIO.writeMatrix(m, mFile, MatrixIO.Format.SVDLIBC_SPARSE_BINARY);

        // Then read it back in and ensure that it contains the right values.
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.SVDLIBC_SPARSE_BINARY);
        testReadMatrix(resultMatrix);
    }

    @Test public void writeSvdlibcSparseTextMatrixFile() throws Exception {
        // Create a test matrix and write it out the the desired format.
        Matrix m = new ArrayMatrix(testMatrix);
        File mFile = File.createTempFile("test-matrix", "dat");
        MatrixIO.writeMatrix(m, mFile, MatrixIO.Format.SVDLIBC_SPARSE_TEXT);

        // Then read it back in and ensure that it contains the right values.
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.SVDLIBC_SPARSE_TEXT);
        testReadMatrix(resultMatrix);
    }

    @Test public void writeMatlabSparseMatrixFile() throws Exception {
        // Create a test matrix and write it out the the desired format.
        Matrix m = new ArrayMatrix(testMatrix);
        File mFile = File.createTempFile("test-matrix", "dat");
        MatrixIO.writeMatrix(m, mFile, MatrixIO.Format.MATLAB_SPARSE);

        // Then read it back in and ensure that it contains the right values.
        Matrix resultMatrix = MatrixIO.readMatrix(
                mFile, MatrixIO.Format.MATLAB_SPARSE);
        testReadMatrix(resultMatrix);
    }

    /**
     * Test the reading of double arrays from matrix files.
     */

    @SuppressWarnings("deprecation")
    @Test public void readMatrixArrayFromMatlabSparse() throws Exception {
        File matlab = getMatlabFile();
        double[][] resultMatrix =
            MatrixIO.readMatrixArray(matlab, MatrixIO.Format.MATLAB_SPARSE);
        testReadDoubleArray(resultMatrix);
    }

    @SuppressWarnings("deprecation")
    @Test public void readMatrixArrayFromSVDLIBCSparseText() throws Exception {
        File svdlibcSparseText = getSparseSVDLIBCFile();
        double[][] resultMatrix =
            MatrixIO.readMatrixArray(svdlibcSparseText,
                                     MatrixIO.Format.SVDLIBC_SPARSE_TEXT);
        testReadDoubleArray(resultMatrix);
    }

    @SuppressWarnings("deprecation")
    @Test public void readMatrixArrayFromSVDLIBCSparseBinary() throws Exception {
        File svdlibcSparseText = getSparseBinarySVDLIBCFile();
        double[][] resultMatrix =
            MatrixIO.readMatrixArray(svdlibcSparseText,
                                     MatrixIO.Format.SVDLIBC_SPARSE_BINARY);
        testReadDoubleArray(resultMatrix);
    }

    @SuppressWarnings("deprecation")
    @Test public void readMatrixArrayFromSVDLIBCDenseText() throws Exception {
        File mFile = getSVDLIBCDenseTextFile();
        double[][] resultMatrix = MatrixIO.readMatrixArray(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_TEXT);
        testReadDoubleArray(resultMatrix);
    }

    @SuppressWarnings("deprecation")
    @Test public void readMatrixArrayFromSVDLIBCDenseBinary() throws Exception {
        File mFile = getSVDLIBCDenseBinaryFile();
        double[][] resultMatrix = MatrixIO.readMatrixArray(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_BINARY);
        testReadDoubleArray(resultMatrix);
    }

    @SuppressWarnings("deprecation")
    @Test public void readMatrixArrayFromClutoSparse() throws Exception {
        // Note that CLUTO Dense and SVDLIBC Dense Text are the same format.
        File mFile = getClutoSparseFile();
        double[][] resultMatrix = MatrixIO.readMatrixArray(
                mFile, MatrixIO.Format.CLUTO_SPARSE);
        testReadDoubleArray(resultMatrix);
    }

    @SuppressWarnings("deprecation")
    @Test public void readMatrixArrayFromClutoDense() throws Exception {
        // Note that CLUTO Dense and SVDLIBC Dense Text are the same format.
        File mFile = getSVDLIBCDenseTextFile();
        double[][] resultMatrix = MatrixIO.readMatrixArray(
                mFile, MatrixIO.Format.CLUTO_DENSE);
        testReadDoubleArray(resultMatrix);
    }

    @SuppressWarnings("deprecation")
    @Test public void readMatrixArrayFromDenseText() throws Exception {
        File mFile = getDenseTextFile();
        double[][] resultMatrix = MatrixIO.readMatrixArray(
                mFile, MatrixIO.Format.DENSE_TEXT);
        testReadDoubleArray(resultMatrix);
    }

    /**
     * Test the matrix file iterators.
     */

    @Test public void readDenseTextIterator() throws Exception {
        File mFile = getDenseTextFile();
        Iterator<MatrixEntry> matrixIter = MatrixIO.getMatrixFileIterator(
                mFile, MatrixIO.Format.DENSE_TEXT);
        testMatrixIterator(matrixIter);
    }

    @Test public void readMatlabSparseIterator() throws Exception {
        File mFile = getMatlabFile();
        Iterator<MatrixEntry> matrixIter = MatrixIO.getMatrixFileIterator(
                mFile, MatrixIO.Format.MATLAB_SPARSE);
        testMatrixIterator(matrixIter);
    }

    @Test public void readClutoSparseIterator() throws Exception {
        File mFile = getClutoSparseFile();
        Iterator<MatrixEntry> matrixIter = MatrixIO.getMatrixFileIterator(
                mFile, MatrixIO.Format.CLUTO_SPARSE);
        testMatrixIterator(matrixIter);
    }

    @Test public void readClutoDenseIterator() throws Exception {
        File mFile = getSVDLIBCDenseTextFile();
        Iterator<MatrixEntry> matrixIter = MatrixIO.getMatrixFileIterator(
                mFile, MatrixIO.Format.CLUTO_DENSE);
        testMatrixIterator(matrixIter);
    }

    @Test public void readSVDLIBCDenseBinaryIterator() throws Exception {
        File mFile = getSVDLIBCDenseBinaryFile();
        Iterator<MatrixEntry> matrixIter = MatrixIO.getMatrixFileIterator(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_BINARY);
        testMatrixIterator(matrixIter);
    }

    @Test public void readSVDLIBCDenseTextIterator() throws Exception {
        File mFile = getSVDLIBCDenseTextFile();
        Iterator<MatrixEntry> matrixIter = MatrixIO.getMatrixFileIterator(
                mFile, MatrixIO.Format.SVDLIBC_DENSE_TEXT);
        testMatrixIterator(matrixIter);
    }

    @Test public void readSVDLIBCSparseBinaryIterator() throws Exception {
        File mFile = getSparseBinarySVDLIBCFile();
        Iterator<MatrixEntry> matrixIter = MatrixIO.getMatrixFileIterator(
                mFile, MatrixIO.Format.SVDLIBC_SPARSE_BINARY);
        testMatrixIterator(matrixIter);
    }

    @Test public void readSVDLIBCSparseTextIterator() throws Exception {
        File mFile = getSparseSVDLIBCFile();
        Iterator<MatrixEntry> matrixIter = MatrixIO.getMatrixFileIterator(
                mFile, MatrixIO.Format.SVDLIBC_SPARSE_TEXT);
        testMatrixIterator(matrixIter);
    }
}
