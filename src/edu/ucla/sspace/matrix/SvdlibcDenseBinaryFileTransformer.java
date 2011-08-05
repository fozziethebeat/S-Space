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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;


/**
 * A {@link FileTransformer} for matrix files in the {@link
 * Format#SVDLIBC_DENSE_BINARY} format.
 *
 * @author Keith Stevens
 */
class SvdlibcDenseBinaryFileTransformer implements FileTransformer {

    /**
     * {@inheritDoc}
     */
    public File transform(File inputFile,
                          File outFile,
                          GlobalTransform transform) {
        try {
            DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(inputFile)));
            // Read in the header for the matrix.
            int rows = dis.readInt();
            int cols = dis.readInt();

            DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(outFile)));
            // Writeout the header for the new matrix.
            dos.writeInt(rows);
            dos.writeInt(cols);

            // Traverse each row and column value, transforming each entry.
            for (int row = 0; row < rows; ++row) {
                for (int col = 0; col < cols; ++col) {
                    double val = dis.readFloat();
                    dos.writeFloat((float) transform.transform(row, col, val));
                }
            }

            dos.close();
            return outFile;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
