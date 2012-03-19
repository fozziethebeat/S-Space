/*
 * Copyright 2010 Keith Stevens
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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;

import java.io.File;
import java.io.IOException;


/**
 * A simple command line tool for converting a {@link Matrix} from one format to
 * another.
 *
 * @author Keith Stevens
 */
public class MatrixConverter {

    public static void main(String[] args) throws IOException {
        ArgOptions options = new ArgOptions();
        options.addOption('i', "inputFormat",
                          "the matrix format of the input matrix",
                          true, "STRING", "Required");
        options.addOption('o', "ouputFormat",
                          "the matrix format of the output matrix",
                          true, "STRING", "Required");
        options.parseOptions(args);

        if (options.numPositionalArgs() != 2 ||
            !options.hasOption('i') || !options.hasOption('o')) {
            System.out.println(
               "usage: java MatrixConverter [options] <int.mat> <out.mat>\n" +
               options.prettyPrint());
            System.exit(1);
        }

        File inMatFile = new File(options.getPositionalArg(0));
        File outMatFile = new File(options.getPositionalArg(1));
        Format inMatFormat = Format.valueOf(
                options.getStringOption('i').toUpperCase());
        Format outMatFormat = Format.valueOf(
                options.getStringOption('o').toUpperCase());
        Matrix matrix = MatrixIO.readMatrix(inMatFile, inMatFormat);
        MatrixIO.writeMatrix(matrix, outMatFile, outMatFormat);
    }
}
