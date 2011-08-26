/*
 * Copyright 2009 Keith Stevens
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

import edu.ucla.sspace.util.SparseArray;

import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import java.util.Arrays;

import java.util.logging.Logger;


/**
 * A {@code MatrixBuilder} for building sparse cluto matrix input files.
 *
 * @author Keith Stevens
 */
public class ClutoSparseMatrixBuilder implements MatrixBuilder {

    /**
     * Logger for the {@code ClutoSparseMatrixBuilder} class
     */
    private static final Logger LOGGER =
        Logger.getLogger(ClutoSparseMatrixBuilder.class.getName());

    /**
     * The file to which the matrix will be written
     */
    private final File matrixFile;

    /**
     * The writer used to add data to the transposed matrix file
     */
    private final PrintWriter writer;

    /**
     * Whether the builder has finished adding data to the matrix array
     */
    private boolean isFinished;

    /**
     * The number of the row that will next be assigned.  Once this matrix
     * has been finished, this value will reflect the total number of rows in
     * the matrix.
     */
    private int curRow;

    /**
     * The total number of columns in the matrix.  This value is continuously
     * updated as new columns are seen and is not valid until the matrix has
     * been finished.
     */
    private int numCols;

    /**
     * The total number of non-zero values in the matrix.  This value is
     * continuously updated as new columns are seen and is not valid until the
     * matrix has been finished.
     */
    private int nonZeroValues;

    /**
     * Creates a new {@code ClutoSparseMatrixBuilder} using a constructed temp
     * file.
     */
    public ClutoSparseMatrixBuilder() {
        this(getTempMatrixFile());
    }

    /**
     * Creates a new {@code ClutoSparseMatrixBuilder} using a the given 
     * file.
     */
    public ClutoSparseMatrixBuilder(File backingFile) {
        this.matrixFile = backingFile;
        curRow = 0;
        numCols = 0;
        nonZeroValues = 0;
        isFinished = false;
        try {
            writer = new PrintWriter(backingFile);
            // Write a temporary header of whitespace using 100 chars.
            char[] whiteSpace = new char[100];
            Arrays.fill(whiteSpace, ' ');
            writer.println(whiteSpace);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns a temporary file that will be deleted on JVM exit.
     *'
     * @return a temporary file used to store a matrix
     */
    private static File getTempMatrixFile() {
        File tmp = null;
        try {
            tmp = File.createTempFile("cluto-sparse-matrix", ".dat");
            tmp.deleteOnExit();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return tmp;
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized int addColumn(double[] row) {
        if (isFinished)
            throw new IllegalStateException(
                "Cannot add columns to a MatrixBuilder that is finished");

        // Update the size of the matrix based on the size of the array
        if (row.length > numCols)
            numCols = row.length;

        // Write the row to file
        int nonZero = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < row.length; ++i) {
            if (row[i] != 0d) {
                sb.append(i+1).append(" ").append(row[i]).append(" ");
                nonZero++;
            }
        }
        writer.println(sb.toString());

        // Update the total number of non-zero values for the entire matrix
        nonZeroValues += nonZero;

        return ++curRow;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int addColumn(SparseArray<? extends Number> row) {
        if (isFinished)
            throw new IllegalStateException(
                "Cannot add columns to a MatrixBuilder that is finished");

        // Update the size of the matrix based on the size of the array
        if (row.length() > numCols)
            numCols = row.length();

        // SparseArray instances can take on the maximum possible array size
        // when the array length isn't specified.  This ruins the matrix size
        // specification since the matrix shouldn't actually be that big.
        // However, because this is an implementation artifact, we can't check
        // for it explicitly with an exception.  Therefore, put in an assert to
        // indicate what is likely going on if asserts are enabled for debugging
        // they symptoms.
        assert row.length() != Integer.MAX_VALUE : "adding a column whose " +
            "length is Integer.MAX_VALUE (was likley left unspecified in the " +
            " constructor).";           

        // Write the row to the file.
        int[] nonZero = row.getElementIndices();
        nonZeroValues += nonZero.length;
        StringBuilder sb = new StringBuilder();
        for (int i : nonZero) {
            sb.append(i+1).append(" ");
            sb.append(row.get(i).floatValue()).append(" ");
        }
        writer.println(sb.toString());
        return ++curRow;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int addColumn(Vector row) {
        if (isFinished)
            throw new IllegalStateException(
                "Cannot add columns to a MatrixBuilder that is finished");

        // Update the size of the matrix based on the size of the array
        if (row.length() > numCols)
            numCols = row.length();

        // Vector instances can take on the maximum possible array size when the
        // vector length isn't specified.  This ruins the matrix size
        // specification since the matrix shouldn't actually be that big.
        // However, because this is an implementation artifact, we can't check
        // for it explicitly with an exception.  Therefore, put in an assert to
        // indicate what is likely going on if asserts are enabled for debugging
        // they symptoms.
        assert row.length() != Integer.MAX_VALUE : "adding a column whose " +
            "length is Integer.MAX_VALUE (was likley left unspecified in the " +
            " constructor).";

        // Special case for sparse Vectors, for which we already know the
        // non-zero indices for the column
        if (row instanceof SparseVector) {
            SparseVector s = (SparseVector)row;
            int[] nonZero = s.getNonZeroIndices();
            nonZeroValues += nonZero.length;
            StringBuilder sb = new StringBuilder();
            for (int i : nonZero) {
                sb.append(i+1).append(" ");
                sb.append(row.getValue(i).doubleValue()).append(" ");
            }
            writer.println(sb.toString());
        }
        // For dense Vectors, find which values are non-zero and write only
        // those
        else {
            int nonZero = 0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < row.length(); ++i) {
                double d = row.getValue(i).doubleValue();
                if (d != 0d) {
                    sb.append(i+1).append(" ").append(d).append(" ");
                    nonZero++;                    
                }
            }
            writer.println(sb.toString());

            // Update the matrix count
            nonZeroValues += nonZero;
        }
        return ++curRow;
    }

    /**
     * {@inheritDoc} Once this method has been called, any subsequent calls will
     * have no effect and will not throw an exception.
    */
    public synchronized void finish() {
        if (!isFinished) {
            isFinished = true;
            try {
                writer.close();
                // Re-open as a random access file so we can overwrite the 3 int
                // header that specifies the number of dimensions and values.
                // Note that the location of the matrix data is dependent on
                // whether the matrix is to be transposed.
                RandomAccessFile matrixRaf =
                    new RandomAccessFile(matrixFile, "rw");

                // Write the header in the first 100 characters.  The header is
                // the number rows, the number of columns, and the number of non
                // zeros on a single line with spaces between them.
                StringBuilder sb = new StringBuilder();
                sb.append(curRow).append(" ");
                sb.append(numCols).append(" ");
                sb.append(nonZeroValues).append(" ");
                matrixRaf.write(sb.toString().getBytes());
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized File getFile() {
        if (!isFinished)
            throw new IllegalStateException(
                "Cannot access matrix file until finished has been called");
        return matrixFile;
    }

    /**
     * Returns {@link MatrixIO#Format.CLUTO_SPARSE.
     * CLUTO_SPARSE}.
     */
    public Format getMatrixFormat() {
        return MatrixIO.Format.CLUTO_SPARSE;
    }

    /**
     * {@inheritDoc}
     */
    public MatrixFile getMatrixFile() {
        return new MatrixFile(getFile(), getMatrixFormat());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean isFinished() {
        return isFinished;
    }
}
