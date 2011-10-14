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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SvdlibjDriver;

import java.io.File;
import java.io.IOError;
import java.io.IOException;


public class SvdTool {

    public static void main(String[] args) {
        ArgOptions options = new ArgOptions();
        
        options.addOption('h', "help", "Generates a help message and exits",
                          false, null, "Program Options");

        options.addOption('d', "dimensions", "Desired SVD Triples",
                          true, "INT", "Program Options");

        options.addOption('o', "fileRoot", "Root of files in which to store resulting U,S,V",
                          true, "DIR", "Program Options");
        options.addOption('r', "inputFormat", "Input matrix file format",
                          true, "STRING", "Program Options");
        options.addOption('w', "outputFormat", "Output matrix file format",
                          true, "STRING", "Program Options");

        options.parseOptions(args);

        if (options.numPositionalArgs() == 0 || options.hasOption("help")) {
            usage(options);
            return;
        }

        // Load and sanity check the options prior to computing the SVD
        int dimensions = options.getIntOption('d');
        String matrixFileName = options.getPositionalArg(0);
        File matrixFile = new File(matrixFileName);
        if (!matrixFile.exists())
            throw new IllegalArgumentException(
                "non-existent input matrix file: " + matrixFileName);

        String outputDirName = options.getStringOption('o');
        File outputDir = new File(outputDirName);
        if (!outputDir.exists() || !outputDir.isDirectory())
            throw new IllegalArgumentException(
                "invalid output directory: " + outputDirName);
            

        Format inputFormat = (options.hasOption('r')) 
            ? getFormat(options.getStringOption('r'))
            : Format.SVDLIBC_SPARSE_TEXT;
        Format outputFormat = (options.hasOption('w')) 
            ? getFormat(options.getStringOption('w'))
            : Format.SVDLIBC_DENSE_TEXT;

        try {
            Matrix[] usv = 
                SvdlibjDriver.svd(matrixFile, inputFormat, dimensions);
            File uFile = new File(outputDir, "U.mat");
            File sFile = new File(outputDir, "S.mat");
            File vFile = new File(outputDir, "V.mat");

            MatrixIO.writeMatrix(usv[0], uFile, outputFormat);
            MatrixIO.writeMatrix(usv[1], sFile, outputFormat);
            MatrixIO.writeMatrix(usv[2], vFile, outputFormat);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    private static MatrixIO.Format getFormat(String code) {
        if (code.equals("cd"))
            return MatrixIO.Format.CLUTO_DENSE;
        if (code.equals("cs"))
            return MatrixIO.Format.CLUTO_SPARSE;
        if (code.equals("ms"))
            return MatrixIO.Format.MATLAB_SPARSE;
        if (code.equals("db"))
            return MatrixIO.Format.SVDLIBC_DENSE_BINARY;
        if (code.equals("dt"))
            return MatrixIO.Format.SVDLIBC_DENSE_TEXT;
        if (code.equals("sb"))
            return MatrixIO.Format.SVDLIBC_SPARSE_BINARY;
        if (code.equals("st"))
            return MatrixIO.Format.SVDLIBC_SPARSE_TEXT;
        else
            throw new IllegalArgumentException("unrecognized format: " + code);
    }

    /**
     * Prints the options and supported commands used by this program.
     *
     * @param options the options supported by the system
     */
    private static void usage(ArgOptions options) {
        System.out.println(
            "SVD version 1.0\n" +
            "  Based on SVDLIBJ, written by Adrian Kuhn and David Erni,\n" +
            "  which was adapted from SVDLIBC, written by Doug Rohde,\n" +
            "  which was based on code adapted from SVDPACKC\n\n" +
            "usage: java -jar svd.jar [options] matrix_file\n\n" 
            + options.prettyPrint() +
            "\nValid matrix formats are:\n" +
            "  cd        The sparse text format supported by CLUTO.\n" +
            "  cs        The sparse text format supported by CLUTO.\n" +
            "  ms        The sparse format supported by Matlab.\n" +
            "  db        The dense binary format supported by SVDLIBC.\n" +
            "  dt        The dense text format supported by SVDLIBC. (default output)\n" +
            "  sb        The sparse binary format supported by SVDLIBC.\n" +
            "  st        The sparse text format supported by SVDLIBC. (default input)\n" +
            "\nSee http://tedlab.mit.edu/~dr/SVDLIBC/ for more format details");
    }
}