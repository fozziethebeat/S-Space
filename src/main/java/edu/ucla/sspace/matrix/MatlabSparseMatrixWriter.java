/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.OutputStream;
import java.io.PrintStream;


/**
 * This {@link MatrixWriter} serializes a {@link Matrix} into the sparse matlab
 * format to an arbitrary {@link OutputStream}.  The format looks like:
 *
 * <pre>
 *   row col value
 *   row col value
 * </pre>
 *
 * Where both row and column indices start at one.  
 *
 * </p>
 *
 * When writing a {@link SparseMatrix}, the element at the bottom right of the
 * matrix, i.e. the element with the largest row and column indices, will be
 * written to the {@link OutputStream} first, even if it is zero, in order to
 * provide the bounds of the matrix.
 *
 * @author Keith Stevens
 */
public class MatlabSparseMatrixWriter implements MatrixWriter {

    /**
     * Writes the dense {@link Matrix} to the file using a particular format.
     */
    public void writeMatrix(Matrix m, OutputStream s) {
        // If the matrix is actually sparse, let the sparse method handle it.
        if (m instanceof SparseMatrix) {
            writeMatrix((SparseMatrix) m, s);
            return;
        }

        PrintStream p = new PrintStream(s);

        // Print the row, col, value entrie for each element in the matrix.
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c)
                p.printf("%d %d %f\n", r+1, c+1, m.get(r,c));

        p.flush();
        p.close();
    }

    /**
     * Writes the {@link SparseMatrix} to the file using a particular format.
     */
    public void writeMatrix(SparseMatrix m, OutputStream s) {
        PrintStream p = new PrintStream(s);

        // Check to see if the last element in the matrix is non zero.  If it
        // has a zero value, print out a single dummy value to bound the total
        // size of the matrix.
        if (m.get(m.rows()-1, m.columns()-1) == 0d)
            p.printf("%d %d %f\n", m.rows(), m.columns(), 0.0);

        // Print the row, col, value entrie for each element in the matrix.
        for (int r = 0; r < m.rows(); ++r) {
            SparseDoubleVector v = m.getRowVector(r);
            for (int c : v.getNonZeroIndices())
                p.printf("%d %d %f\n", r+1, c+1, m.get(r,c));
        }

        p.flush();
        p.close();
    }
}
