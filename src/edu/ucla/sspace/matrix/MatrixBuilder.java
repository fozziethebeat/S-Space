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

import edu.ucla.sspace.matrix.MatrixIO.Format;

import edu.ucla.sspace.util.SparseArray;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vector;

import java.io.File;

/**
 * An interface for utilities that support incrementally building matrices on
 * disk, one column at a time.  Instances of these class are designed to
 * abstract away the format-specific details of writing the matrix to disk. 
 *
 * <p> Columns may be added via the {@code
 * addColumn} method until the matrix has the desired data.  The underlying
 * matrix structure will expand to match the dimensions of the input data.
 * Great care should be taken when using {@link SparseArray} and {@link
 * DoubleVector} instances to ensure that the values returned by their
 * respective {@code length} methods are fixed to the actual data structure
 * size, rather than the a possible maximum size of the struture.  This is
 * necessary to determine this matrix's dimension; passing in an array or vector
 * with a maximum size bound will greatly distort the matrix's size.
 *
 * <p> After all the data has been added, the {@code finish} method should be
 * invoked.  Instances should use this as a signal to finish any last remaining
 * writes or perform any necessary post processing for the matrix.  Once this
 * method returns the {@code getMatrixFile} method will allow access to the
 * finished data in its correct format.
 * 
 *
 * <p> Implementations may cache all, part, or none of the data prior to
 * finishing the matrix data.  However, all implementations are expected to be
 * have a low memory overhead, especially when building large matrices.
 *
 * <p> All implementations are expected to be thread-safe.
 *
 * @author David Jurgens
 */
public interface MatrixBuilder {

    /**
     * Adds the column values to the underlying matrix, updating the dimensions as
     * necessary and returning the index at which the column was added.
     *
     * @param column the values of the column in the matrix.
     *
     * @return the index at which the column is present.
     *
     * @throws IllegalStateException if {@code finish} has been called, thereby
     *         signaling that no further data will be added to the matrix
     */
    int addColumn(double[] column);

    /**
     * Adds the {@code double} values in the array to the underlying matrix,
     * updating the dimensions as necessary and returning the index at which the
     * column was added.
     *
     * @param column the values of the column in the matrix.
     *
     * @return the index at which the column is present.
     *
     * @throws IllegalStateException if {@code finish} has been called, thereby
     *         signaling that no further data will be added to the matrix
     */
    int addColumn(SparseArray<? extends Number> column);
    
    /**
     * Adds the vector values to the underlying matrix, updating the dimensions
     * as necessary and returning the index at which the column was added.
     *
     * @param column the values of the column in the matrix.
     *
     * @return the index at which the column is present.
     *
     * @throws IllegalStateException if {@code finish} has been called, thereby
     *         signaling that no further data will be added to the matrix
     */
    int addColumn(Vector column);

    /**
     * Indicates that no further data will be added to the matrix and any
     * remaining matrix meta-data should be written and flushed to disk.  Once
     * this method has been called {@code addColumn} will thcolumn a {@code
     * IllegalStateException} if called.  
     */
    void finish();

    /**
     * Returns the file containing the matrix data after all data has finished
     * being written.  The {@code finish} method must be called prior to
     * accessing the underlying file to ensure that all necessary matrix
     * meta-data has been written to disk.
     *
     * @return the file containing the matrix data
     *
     * @throws IllegalStateException if {@code finish} has not yet been called,
     *         which indicates that the file would be in an inconsistent state
     */
    File getFile();
    
    /**
     * Returns the data format for the matrix file.
     *
     * @return the format of the matrix's data
     */
    Format getMatrixFormat();

    /**
     * Returns {@code true} if no further data should be added to the matrix and
     * the file containing the data is available for us.  This will only return
     * {@code true} if {@code finish} has been called first.
     *
     * @return {@code true} if no further data should be added to the matrix
     */
    boolean isFinished();
    
}
