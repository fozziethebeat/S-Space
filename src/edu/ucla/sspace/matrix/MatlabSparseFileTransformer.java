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

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A {@link FileTransformer} for matrix files in the {@link
 * Format#MATLAB_SPARSE} format.
 *
 * @author Keith Stevens
 */
public class MatlabSparseFileTransformer implements FileTransformer {

    /**
     * {@inheritDoc}
     */
    public File transform(File inputFile,
                          File outputFile,
                          GlobalTransform transform) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            PrintWriter writer = new PrintWriter(new BufferedWriter(
                        new FileWriter(outputFile)));

            for (String line = null; (line = br.readLine()) != null; ) {
                String[] rowColEntry = line.split("\\s+");
                int row = Integer.parseInt(rowColEntry[0]);
                int col = Integer.parseInt(rowColEntry[1]);
                double value = Double.parseDouble(rowColEntry[2]);
                writer.printf("%d %d %f\n", row, col, 
                              transform.transform(row-1, col-1, value));
            }
            writer.close();

            return outputFile;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
