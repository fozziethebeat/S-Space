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
 * MatrixIO.Format.SVDLIBC_SPARSE_BINARY} formatted file.
 */
class SvdlibcSparseBinaryFileIterator implements Iterator<MatrixEntry> {

    private final DataInputStream dis;

    private MatrixEntry next;
    
    /**
     * The entry number that will next be returned from the matrix
     */
    private int entry;

    /**
     * The total number of non-zero entries in the matrix
     */
    private int nzEntriesInMatrix;

    /**
     * The index of the current column
     */
    private int curCol;

    /**
     * The number of non-zero entries for the current column of the matrix
     */
    private int nzInCurCol;

    /**
     * The entry number of the next to return
     */
    private int curColEntry;

    public SvdlibcSparseBinaryFileIterator(File matrixFile) throws IOException {
        dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(matrixFile)));
        int rows = dis.readInt();
        int cols = dis.readInt();
        nzEntriesInMatrix = dis.readInt();
        nzInCurCol = dis.readInt();         
        entry = 0;
        curCol = 0;         
        curColEntry = 0;
        advance();
    }

    private void advance() throws IOException {        
        if (entry >= nzEntriesInMatrix) {
            next = null;
            // If the end of the file has been reached, close the reader
            dis.close();
        }
        else {
            try {
                // Check whether we have exhaused all the data points in the
                // current column
                while (curColEntry == nzInCurCol) {
                    curColEntry = 0;
                    nzInCurCol = dis.readInt();
                    curCol++;
                }
                int row = dis.readInt();
                double value = dis.readFloat();
                next = new SimpleEntry(row, curCol, value);
                curColEntry++;
                entry++;
            } catch (IOException ioe) {
                // Rethrow wrapped exception
                throw new MatrixIOException(
                    "Missing data when reading.  Truncated file?", ioe);
            }
        }
    }

    public boolean hasNext() {
        return next != null;
    }

    public MatrixEntry next() {
        if (next == null) 
            throw new NoSuchElementException("No futher entries");
        MatrixEntry me = next;
        try {
            advance();
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