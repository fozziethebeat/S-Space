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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * A {@link FileTransformer} for matrix files in the {@link
 * Format#SVDLIBC_SPARSE_TEXT} format.
 *
 * @author Keith Stevens
 */
class SvdlibcSparseTextFileTransformer implements FileTransformer {

    /**
     * {@inheritDoc}
     */
    public File transform(File inputFile,
                          File outFile,
                          GlobalTransform transform) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            PrintWriter writer = new PrintWriter(new BufferedWriter(
                        new FileWriter(outFile)));

            // Read in the header for matrix file.
            String line = br.readLine();
            String[] rowColNonZeroCount = line.split("\\s+");
            int numRows = Integer.parseInt(rowColNonZeroCount[0]);
            int numCols = Integer.parseInt(rowColNonZeroCount[1]);
            int numTotalNonZeros = Integer.parseInt(rowColNonZeroCount[2]);

            // Write out the header for the new matrix file.
            writer.printf("%d %d %d\n", numRows, numCols, numTotalNonZeros);

            line = br.readLine();
            // Traverse each column in the matrix.
            for (int col = 0; line != null && col < numCols; ++col) {
                int numNonZeros = Integer.parseInt(line);
                writer.printf("%d\n", numNonZeros);

                // Transform each of the non zero values for the new matrix
                // file.
                for (int index = 0; (line = br.readLine()) != null &&
                                    index < numNonZeros; ++index) {
                    String[] rowValue = line.split("\\s+");
                    int row = Integer.parseInt(rowValue[0]);
                    double value = Double.parseDouble(rowValue[1]);
                    writer.printf("%d %f\n", row,
                                  transform.transform(row, col, value));
                }
            }

            writer.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        return outFile;
    }
}
