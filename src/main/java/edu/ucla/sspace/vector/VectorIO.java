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

package edu.ucla.sspace.vector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.PrintStream;
import java.io.Reader;

import java.util.ArrayList;
import java.util.List;


/**
 * A shared utility for printing {@code Vector}s and arrays to files in a
 * uniform manner.
 *
 * @author David Jurgens
 */
public class VectorIO {
    
    /**
     * Uninstantiable
     */
    private VectorIO() { }

    /**
     * Returns a {@link List} of {@link SparseDoubleVector} read from an
     * artbitrary {@link Reader}.  Data read from the reader is assumed to be in
     * a the following text format:
     *
     * <pre>
     *     numRows numColumns
     *     (colId:value)+
     *     (colId:value)+
     *     ...
     * </pre>
     *
     * Which includes a header listing the number of rows and number of
     * dimensions, then one row for each vector to be read.  Each vector row has
     * a series of white space delimited column id an value pairs, which are
     * connected via ":".
     */
    public static List<SparseDoubleVector> readSparseVectors(
            Reader reader) throws IOException {
        // Turn the reader into a buffered reader to read off each line.
        BufferedReader br = new BufferedReader(reader);

        // Read off the header from the file.
        String line = br.readLine();
        String[] numRowColumns = line.split("\\s+");
        int numRows = Integer.parseInt(numRowColumns[0]);
        int numCols = Integer.parseInt(numRowColumns[1]);

        // Parse each row and create a new vector.
        List<SparseDoubleVector> vectors =
            new ArrayList<SparseDoubleVector>(numRows);
        for (line = null; (line = br.readLine()) != null; ) {
            // Create and store the vector for this row.
            SparseDoubleVector sv = new CompactSparseVector(numCols);
            vectors.add(sv);
            for (String entry : line.trim().split("\\s+")) {
                // Skip the row if it is empty.
                if (entry.equals(""))
                    continue;
                // Update the column value.
                String[] colValue = entry.split(":");
                int col = Integer.parseInt(colValue[0]);
                double val = Double.parseDouble(colValue[1]);
                sv.set(col, val);
            }
        }

        return vectors;
    }

    /**
     * Writes the {@link SparseDoubleVector}s to a file specified by {@code
     * outputFile}.
     */
    public static void writeVectors(List<SparseDoubleVector> vectors,
                                    String outputFile) throws IOException {
        PrintStream ps = new PrintStream(outputFile);
        writeVectors(vectors, ps);
        ps.close();
    }

    /**
     * Writes the {@link SparseDoubleVector}s to a {@link File} specified by {@code
     * outputFile}.
     */
    public static void writeVectors(List<SparseDoubleVector> vectors,
                                    File outputFile) throws IOException {
        PrintStream ps = new PrintStream(outputFile);
        writeVectors(vectors, ps);
        ps.close();
    }

    /**
     * Writes the {@link SparseDoubleVector}s to an arbitrary {@link PrintStream} destination.
     * The output format will match that of {@link readSparseVectors}.
     */
    public static void writeVectors(List<SparseDoubleVector> vectors,
                                    PrintStream stream) {
        int numData = vectors.size();
        int numFeatures = vectors.get(0).length();

        stream.printf("%d %d\n", numData, numFeatures);
        for (SparseDoubleVector v : vectors) {
            if (v.length() != numFeatures)
                throw new IllegalArgumentException(
                        "All vectors in the list must be of the same size");
            writeVector(v, stream);
        }
    }

    /**
     * Writes a single {@link SparseDoubleVector} to a file specified by {@code
     * outputFile}.
     */
    public static void writeVector(SparseDoubleVector vector, 
                                   String outputFile) throws IOException {
        PrintStream ps = new PrintStream(outputFile);
        writeVector(vector, ps);
        ps.close();
    }

    /**
     * Writes a single {@link SparseDoubleVector} to a {@link File} specified by
     * {@code outputFile}.
     */
    public static void writeVector(SparseDoubleVector vector, 
                                   File outputFile) throws IOException {
        PrintStream ps = new PrintStream(outputFile);
        writeVector(vector, ps);
        ps.close();
    }

    /**
     * Writes a single {@link SparseDoubleVector} to an arbitrary output {@link
     * PrintStream} matching the format of the row format described by {@link
     * readSparseVectors}.
     */
    public static void writeVector(SparseDoubleVector vector, 
                                   PrintStream stream) {
        for (int i : vector.getNonZeroIndices())
            stream.printf("%d:%f ", i, vector.get(i));
        stream.println();
    }

    /**
     * Read a double array from the specified file.  {@f} is interpreted as a
     * plain text file with array values separated by whitespace.
     *
     * @param f A whitespace separated file of double values.
     *
     * @return A double array of values in {@code f}.
     */
    public static double[] readDoubleArray(File f) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        String[] valueStrs = (br.readLine()).trim().split("\\s+");

        double[] values = new double[valueStrs.length];
        for (int i = 0; i < valueStrs.length; ++i) {
            values[i] = Double.parseDouble(valueStrs[i]);
        }

        br.close();
        return values;
    }

    /**
     * Convert a {@code Vector} to a {@code String} where values are separated
     * by spaces.
     *
     * @param vector An {@code Vector} of values to convert.
     *
     * @return A {@code String} representing {@code vector}.
     */
    public static String toString(Vector vector) {
        StringBuilder sb = new StringBuilder(vector.length() * 5);
        if (vector instanceof SparseDoubleVector) {
            SparseDoubleVector sdv = (SparseDoubleVector) vector;
            int[] nz = sdv.getNonZeroIndices();
            sb.append(nz[0]).append(",").append(sdv.get(nz[0]));
            for (int i = 1; i < nz.length; ++i)
              sb.append(";").append(nz[i]).append(",").append(sdv.get(nz[i]));
        } else {
            for (int i = 0; i < vector.length() - 1; ++i)
                sb.append(vector.getValue(i).doubleValue()).append(" ");
            sb.append(vector.getValue(vector.length() - 1).doubleValue());
        }
        
        return sb.toString();
    }

    /**
     * Convert a double array to a {@code String} where values are separated by
     * spaces.
     *
     * @param vector An array of values to convert.
     *
     * @return A {@code String} representing {@code vector}.
     */
    public static String toString(double[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 5);
        
        for (int i = 0; i < vector.length - 1; ++i)
            sb.append(vector[i]).append(" ");
        sb.append(vector[vector.length-1]);
        
        return sb.toString();
    }

    /**
     * Convert a integer array to a {@code String} where values are separated by
     * spaces.
     *
     * @param vector An array of values to convert.
     *
     * @return A {@code String} representing {@code vector}.
     */
    public static String toString(int[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 3);
        
        for (int i = 0; i < vector.length - 1; ++i)
            sb.append(vector[i]).append(" ");
        sb.append(vector[vector.length-1]);
        
        return sb.toString();
    }

    /**
     * Write the values of an integer array to the destination stored by a
     * {@code PrintWriter}.
     *
     * @param vector The integer array to write out.
     * @param pw A {@code PrintWriter} to write values to.
     */
    public static void writeVector(int[] vector, PrintWriter pw)
        throws IOException {
        pw.println(toString(vector));
        pw.close();
    }

    /**
     * Write the values of an integer array to the the specified {@code File}.
     *
     * @param vector The integer array to write out.
     * @param f A {@code PrintWriter} to write values to.
     */
    public static void writeVector(int[] vector, File f) throws IOException {
        writeVector(vector, new PrintWriter(f));
    }
    
    /**
     * Creates a file using the provided word name in the given output
     * directory.  All "/" characters are replaced with <tt>-SLASH-</tt>.
     *
     * @param vector The integer array to write out.
     * @param word The name of the file to write values to, with .vector as the
     *             extension type.
     * @param outputDir The directory where {@word}.vector will be created.
     */
    public static void writeVector(int[] vector, String word, File outputDir)
        throws IOException {
        if (!outputDir.isDirectory()) {
            throw new IllegalArgumentException("provided output directory file "
                                               + "is not a directory: " + 
                                               outputDir);
        }

        word = word.replaceAll("/","-SLASH-");
        File output = new File(outputDir, word + ".vector");
        writeVector(vector, output);
    }
}
