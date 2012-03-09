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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An iterator for sequentially accessing the data of a {@link
 * MatrixIO.Format.MATLAB_SPARSE} formatted file.
 */
class MatlabSparseFileIterator implements Iterator<MatrixEntry> {

    private final BufferedReader matrixFileReader;

    private MatrixEntry next;

    /**
     * The line number for the matrix values that have just been read.
     */
    private int lineNo;

    public MatlabSparseFileIterator(File matrixFile) throws IOException {
        matrixFileReader = new BufferedReader(new FileReader(matrixFile));
        lineNo = 0;
        advance();
    }

    private void advance() throws IOException {
        String line = matrixFileReader.readLine();
        lineNo++;
        if (line == null) {
            next = null;
            // If the end of the file has been reached, close the reader
            matrixFileReader.close();
        }
        else {
            String[] rowColVal = line.split("\\s+");
            if (rowColVal.length != 3)
                throw new MatrixIOException(
                    "Incorrect number of values on line: " + lineNo);
            int row = Integer.parseInt(rowColVal[0]) - 1;
            int col = Integer.parseInt(rowColVal[1]) - 1;
            double value = Double.parseDouble(rowColVal[2]);
            next = new SimpleEntry(row, col, value);
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
