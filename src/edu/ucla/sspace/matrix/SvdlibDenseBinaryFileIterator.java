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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
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
class SvdlibcDenseBinaryFileIterator implements Iterator<MatrixEntry> {

    /**
     * The {@link DataInputStream} holding the matrix data.
     */
    private final DataInputStream dis;

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
     * The column number of the next value to return
     */
    private int curCol;

    /**
     * The row number of the next value to return
     */
    private int curRow;

    public SvdlibcDenseBinaryFileIterator(File matrixFile) throws IOException {
        dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(matrixFile)));
        rows = dis.readInt();
        cols = dis.readInt();
        curCol = 0;         
        curRow= 0;
        next = advance();
    }

    private MatrixEntry advance() throws IOException {        
        if (curRow >= rows) 
            return null;

        // Check whether we have exhaused all the data points in the current
        // column
        if (curCol >= cols) {
            curCol = 0;
            curRow++;
        }

        // If the last row has been read, return null.
        if (curRow >= rows) {
            dis.close();
            return null;
        }

        // Read the next data point and return it.
        return new SimpleEntry(curRow, curCol++, dis.readFloat());
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
