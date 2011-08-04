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

import ch.akuhn.edu.mit.tedlab.SMat;
import ch.akuhn.edu.mit.tedlab.Svdlib;
import ch.akuhn.edu.mit.tedlab.SVDRec;

import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.Matrix.Type;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A wrapper around SVDLIBJ that allows {@link Matrix} objects and arbitrarily
 * formatted matrix files to use the SVDLIBJ library.  The SVDLIBJ library was
 * written by Adrian Kuhn and David Erni and is a direct Java port of Doug
 * Rohde's <a href="http://tedlab.mit.edu/~dr/SVDLIBC/">SVDLIBC</a> library,
 * which is itself a C port of the FORTRAN SVDPAKC code.
 *
 * @see SVD
 *
 * @author David Jurgens
 */
public class SvdlibjDriver {

    private SvdlibjDriver() { }

    /**
     * Computes the SVD of the matrix.
     *
     * @param m the matrix to be decomposed
     * @param dimensions the number of singular values to return
     *
     * @return the decomposed matrices: U, S, V<sup>T</sup> in that order.
     */
    public static Matrix[] svd(Matrix m, int dimensions) {
        SMat input = covertToSMat(m);
        return svd(input, dimensions);
    }

    /**
     * Computes the SVD of the matrix in the provided file in the specified
     * format.
     *
     * @param matrix the file containing the matrix to be decomposed
     * @param format the format of the data in the file
     * @param dimensions the number of singular values to return
     *
     * @return the decomposed matrices: U, S, V<sup>T</sup> in that order.
     */
    public static Matrix[] svd(File matrix, MatrixIO.Format format, 
                               int dimensions) throws IOException {
        // NOTE: this conversion is inefficient in that the coversion process
        // itself computes all that's needed to covert to the SMat format.
        // However, to avoid having an explosion of duplicated formatting logic,
        // the conversion call is just used instead to simplify the code.
        File formatted = MatrixIO.convertFormat(
            matrix, format, Format.SVDLIBC_SPARSE_BINARY);
        SMat input = readToSMat(formatted);
        return svd(input, dimensions);
    }

    /**
     * Computes the SVD for the SVDLIBJ formatted data
     *
     * @param m the matrix to be decomposed
     * @param dimensions the number of singular values to return
     */
    static Matrix[] svd(SMat m, int dimensions) {
        SVDRec result = new Svdlib().svdLAS2A(m, dimensions);
        // To avoid having the output matrix in memory in both S-Space Matrix
        // and SMat formats (i.e. double overhead), write the initial SMat
        // output to disk, then clear its memory and load the file back in as an
        // S-Space Matrix

        //File utFile = writeToFile(result.Ut);
        int cols = result.Ut.cols;
        int rows = result.Ut.rows;
        // Create as transposed
        Matrix U = new ArrayMatrix(cols, rows);
        // Transpose the values when filling the array
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                U.set(col, row, result.Ut.value[row][col]);
            }
        }        
        result.Ut = null;
        Matrix S = new DiagonalMatrix(result.S);
        result.S = null;

        rows = result.Vt.rows;
        cols = result.Vt.cols;
        Matrix Vt = new ArrayMatrix(result.Vt.rows, result.Vt.cols);
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                Vt.set(row, col, result.Vt.value[row][col]);
            }
        }
        result.Vt = null;
        return new Matrix[] { U, S, Vt };
    }

    /**
     * Writes the {@link SMat} matrix data to a file formatted as {@link
     * Format.SVDLIBC_SPARSE_BINARY}.
     *
     * @param m the matrix data to be written to file
     * @return a file containing the matrix data
     * @throws IOException if an error occurs while writing to file
     */
    static File writeToFile(SMat m) throws IOException {
        File f = File.createTempFile("svdlibj-output", ".mat");
        f.deleteOnExit();
        DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(f)));
        dos.writeInt(m.rows);
        dos.writeInt(m.cols);
        dos.writeInt(m.vals);
        int n = 0;
        for (int col = 0; col < m.cols; ++col) {
            int colOffset = m.pointr[col];
            // NOTE: m.pointr is cols+1 in length, just for this reason
            int nz = m.pointr[col+1] - m.pointr[col];
            dos.writeInt(nz);
            for (int off = 0; off < colOffset + nz; ++off) {
                int row = m.rowind[off];
                double val = m.value[off];
                dos.writeInt(row);
                // REMDINER: this is unnecessarily losing the precision of the
                //           operation, perhaps it might be worth extending the
                //           format somehow?
                dos.writeFloat((float)val); 
            }
        }
        dos.close();
        return f;
    }

    /**
     * Converts a {@link Matrix} into an {@link SMat} struct for input to
     * SVDLIBJ.
     *
     * @param m the matrix to be converted
     * @return the data in {@code m} represented as an {@code SMat}
     */
    static SMat covertToSMat(Matrix m) {
        SMat output = null;
        if (m instanceof SparseMatrix) {
            SparseMatrix sm = (SparseMatrix)m;
            int rows = m.rows();
            int cols = m.columns();
            int nz = 0;
            // Use the SparseVector functionality to quickly compute the number
            // of non-zero values
            for (int row = 0; row < rows; ++row)
                nz += sm.getRowVector(row).getNonZeroIndices().length;

            output = new SMat(rows, cols, nz);

            // NOTE: because the matrices are currently order according to row,
            // it's more expensive to call getColumnVector() than it is to
            // iterate, n^2 vs n^2*log(n).  This behavior could actually be cut
            // down to just O(n * nz), if the matrix was column ordered and
            // there was an efficient way of determining that.
            nz = 0;
            for (int col = 0; col < cols; col++) {
                output.pointr[col] = nz;
                for (int row = 0; row < rows; row++) {
                    double d = m.get(row, col);
                    if (d != 0) {
                        output.rowind[nz] = row;
                        output.value[nz] = d;
                        nz++;
                    }
                }
            }
            output.pointr[cols] = output.vals;
        }
        else {                                     
            int i = 0, j = 0, nz = 0;
            int rows = m.rows();
            int cols = m.columns();

            // count the number of non-zero elements in the matrix
            for (i = 0; i < rows; i++) {
                for (j = 0; j < cols; j++) {
                    if (m.get(i, j) != 0) 
                        nz++;
                }
            }

            output = new SMat(rows, cols, nz);
            for (j = 0, nz = 0; j < cols; j++) {
                output.pointr[j] = nz;
                for (i = 0; i < rows; i++) {
                    double d = m.get(i, j);
                    if (d != 0) {
                        output.rowind[nz] = i;
                        output.value[nz] = d;
                        nz++;
                    }
                }
            }
            output.pointr[cols] = output.vals;
        }
        return output;
    }

    /**
     * Reads in a file formatted as {@link Format.SVDLIBC_SPARSE_BINARY} and
     * convert the data to an {@link SMat}
     *
     * @throws IOError if any {@link IOException} is thrown as a part of reading
     */
    static SMat readToSMat(File f) throws IOException {
        try {
            DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(f)));
            int rows = dis.readInt();
            int cols = dis.readInt();
            int vals = dis.readInt();
            SMat m = new SMat(rows, cols, vals);
            
            int n = 0; // total non-zero values seen
            for (int col = 0; col < cols; ++col) {
                m.pointr[col] = n;
                int nz = dis.readInt(); // discard
                for (int i = 0; i < nz; i++) {
                    int row = dis.readInt();
                    float val = dis.readFloat();
                    m.rowind[n] = row;
                    m.value[n] = val;
                    n++;
                }
            }
            m.pointr[cols] = vals;
            return m;
        } 
        catch (EOFException eofe) {
            // Wrap to indicate what happened at a more conceptual level
            throw new MatrixIOException("Truncated matrix data file: " + f);
        }
    }
}
