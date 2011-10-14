/*
 * Copyright 2010 David Jurgens
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

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;


/**
 * An abstraction of a matrix that is stored on disk.  This class provides
 * convenient accessors to the contents of the data and allows other classes
 * that operate on matrices to more easily handle matrix files as parameters and
 * return arguments.
 */
public class MatrixFile implements Iterable<MatrixEntry> {

    /**
     * The file containing the matrix data.
     */
    private final File matrixFile;

    /**
     * The current format of the matrix file
     */
    private final MatrixIO.Format format;

    /**
     * Constucts a {@code MatrixFile} from the provided {@code File} and format.
     */
    public MatrixFile(File matrixFile, MatrixIO.Format format) {
        // pre-emptive check that the matrix file is non-null
        if (matrixFile == null)
            throw new NullPointerException("matrix file cannot be null");
        this.matrixFile = matrixFile;
        this.format = format;
    }

    public boolean equals(Object o) {
        if (o instanceof MatrixFile) {
            MatrixFile m = (MatrixFile)o;
            return matrixFile.equals(m.matrixFile) 
                && format.equals(m.format);
        }
        return false;
    }

    /**
     * Returns file containing the matrix data 
     */
    public File getFile() {
        return matrixFile;
    }

    /**
     * Returns the format of the matrix
     */
    public MatrixIO.Format getFormat() {
        return format;
    }

    public int hashCode() {
        return matrixFile.hashCode() ^ format.hashCode();
    }

    /**
     * Returns an iterator over all the entries in the matrix.  The order in
     * which entries are returned is format-specific; no guarantee is provided
     * about the ordering.
     */
    public Iterator<MatrixEntry> iterator() {
        try {
            return MatrixIO.getMatrixFileIterator(matrixFile, format);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Loads the matrix from disk and returns a copy of its data.  Note that a
     * new {@code Matrix} is created each time this is called.
     */
    public Matrix load() {
        try {
            return MatrixIO.readMatrix(matrixFile, format);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    public String toString() {
        return "Matrix[" + matrixFile + ":" + format + "]";
    }

}