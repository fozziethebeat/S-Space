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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An iterator for sequentially accessing the data of a {@link
 * MatrixIO.Format.SVDLIBC_DENSE_BINARY} formatted file.
 *
 * @author Keith Stevens
 */
class SvdlibcSparseTextFileIterator implements Iterator<MatrixEntry> {

    /**
     * The {@link DataInputStream} holding the matrix data.
     */
    private final BufferedReader reader;

    /**
     * The next {@link MatrixEntry} to be returned.
     */
    private MatrixEntry next;
    
    /**
     * The number of rows in the matrix.
     */
    private int rows;

    /**
     * The number of columns in the matrix.
     */
    private int cols;

    /**
     * The number of nonZeros on this line.
     */
    private int curNonZeros;

    /**
     * The row number of the next value to return
     */
    private int curCol;

    /**
     * Creates a new {@link SvdlibcSparseTextFileIterator} for {@code
     * matrixFile}.
     */
    public SvdlibcSparseTextFileIterator(File matrixFile) throws IOException {
        reader = new BufferedReader(new FileReader(matrixFile));
        String[] numRowCol = reader.readLine().split("\\s");
        rows = Integer.parseInt(numRowCol[0]);
        cols = Integer.parseInt(numRowCol[1]);

        curCol= 0;
        curNonZeros = Integer.parseInt(reader.readLine());
        next = advance();
    }

    private MatrixEntry advance() throws IOException {        
        if (curCol >= cols) 
            return null;

        // Check that there are still non zero values to read for the current
        // row.  If not, advance to the next row.
        if (curNonZeros == 0) {
            curCol++;

            // If the last row has been read, return null.
            if (curCol >= cols) {
                reader.close();
                return null;
            }

            // Read the next number of non zeros.
            curNonZeros = Integer.parseInt(reader.readLine());
        }

        // Read a line and decrement the number of nonZeros that we expect to
        // process.  Then return the created MatrixEntry.
        String[] rowValue = reader.readLine().split("\\s+");
        curNonZeros--;
        return new SimpleEntry(Integer.parseInt(rowValue[0]),
                               curCol, 
                               Double.parseDouble(rowValue[1]));
    }

    public boolean hasNext() {
        return next != null;
    }

    public MatrixEntry next() {
        if (next == null) 
            throw new NoSuchElementException("No futher entries");
        MatrixEntry me = next;
        try {
            next = advance();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return me;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove from file");
    }
}
