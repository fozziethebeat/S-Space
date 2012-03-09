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

package edu.ucla.sspace.common;

import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorIO;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.IOError;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import java.util.Arrays;
import java.util.Set;

import java.util.logging.Logger;


/**
 * An output utility to write a {@link SemanticSpace} incrementally, one vector
 * at a time.  This class is designed for algorithms that need to write {@code
 * SemanticSpace} instances that are too large to fit into memory but can still
 * be computed.
 *
 * <p> Upon writing the last vector, users of this class <i>must</i> call {@code
 * #close() close} to ensure that the file header is correctly written.
 *
 * @see SemanticSpaceIO
 */
public class SemanticSpaceWriter {

    /**
     * The file to which the semantic space will be written
     */
    private final File outputFile;

    /**
     * The format of the semantic space
     */
    private final SSpaceFormat format;

    /**
     * The data writer for output the space.
     */
    private final DataOutputStream writer;

    /**
     * The number of vector seen so far.
     */
    private int vectorsSeen;

    /**
     * The length of the vectors in the semantic space output
     */
    private int vectorLength;
    
    /**
     * Creates a {@code SemanticSpaceWriter} that will write a {@link
     * SemanticSpace} to the provided file in the specified format.
     *
     * @param sspaceFile the file to which the {@code SemanticSpace} will be
     *        written
     * @param format the format of the semantic space
     */
    public SemanticSpaceWriter(File sspaceFile, SSpaceFormat format) {
        this.outputFile = sspaceFile;
        this.format = format;
        try {
            this.writer = new DataOutputStream(new FileOutputStream(sspaceFile));
            writeEmptyHeader();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Finishes writing the {@link SemanticSpace} and writes all the associated
     * meta data.  Once this method has been called, the backing file is valid
     * to read using {@link SemanticSpaceIO}.
     */
    public void close() throws IOException {
        writer.close();
        //FileOutputStream fos = new FileOutputStream(outputFile);
        RandomAccessFile raf = new RandomAccessFile(outputFile, "rw");
        raf.seek(4); // skip over the header;
        // Then fill in the sspace information 
        switch (format) {
        case TEXT:
        case SPARSE_TEXT:
            //PrintWriter pw = new PrintWriter(fos);
            raf.writeChars(vectorsSeen + " " + vectorLength);
            break;
        case BINARY: 
        case SPARSE_BINARY:
            raf.writeInt(vectorsSeen); // # of vectors
            raf.writeInt(vectorLength); // # of dimensions
            break;
        default:
            assert false : "unhandled s-space format";
        }
        raf.close();
    }

    /**
     * Returns the file containing the {@code SemanticSpace}
     */
    public File getFile() {
        return outputFile;
    }

    /**
     * Writes the provided word and vector to the {@code SemanticSpace} on disk.
     */
    public void write(String word, Vector vector) throws IOException {
        vectorLength = vector.length();
        vectorsSeen++;
        switch (format) {

        case SPARSE_TEXT: {
            PrintWriter pw = new PrintWriter(writer);
            pw.print(word + "|");
            // For each vector, write all the non-zero elements and their indices
            StringBuilder sb = null;
            if (vector instanceof SparseVector) {
                if (vector instanceof DoubleVector) {
                    SparseDoubleVector sdv = (SparseDoubleVector)vector;
                    int[] nz = sdv.getNonZeroIndices();
                    sb = new StringBuilder(nz.length * 4);
                    // special case the first
                    sb.append(0).append(",").append(sdv.get(0));
                    for (int i = 1; i < nz.length; ++i)
                        sb.append(",").append(i).append(",").append(sdv.get(i));
                }
                else {
                    SparseVector sv = (SparseVector)vector;
                    int[] nz = sv.getNonZeroIndices();                    
                    sb = new StringBuilder(nz.length * 4);
                    // special case the first
                    sb.append(0).append(",")
                        .append(sv.getValue(0).doubleValue());
                    for (int i = 1; i < nz.length; ++i)
                        sb.append(",").append(i).append(",").
                            append(sv.getValue(i).doubleValue());
                }
            }
            
            else {
                boolean first = true;
                sb = new StringBuilder(vectorLength / 2);
                for (int i = 0; i < vector.length(); ++i) {
                    double d = vector.getValue(i).doubleValue();
                    if (d != 0d) {
                        if (first) {
                            sb.append(i).append(",").append(d);
                            first = false;
                        }
                        else {
                            sb.append(",").append(i).append(",").append(d);
                        }
                    }
                }
            }
            pw.println(sb.toString());
            break;
        }
            


        case TEXT: {
            PrintWriter pw = new PrintWriter(writer);
            pw.println(word + "|" + VectorIO.toString(vector));
            break;
        }

        case BINARY: {
            writer.writeUTF(word);
            for (int i = 0; i < vector.length(); ++i) {
                writer.writeDouble(vector.getValue(i).doubleValue());
            }            
            break;
        }

        case SPARSE_BINARY: {
            writer.writeUTF(word);
            if (vector instanceof SparseVector) {
                if (vector instanceof DoubleVector) {
                    SparseDoubleVector sdv = (SparseDoubleVector)vector;
                    int[] nz = sdv.getNonZeroIndices();
                    writer.writeInt(nz.length);
                    for (int i : nz) {
                        writer.writeInt(i);
                        writer.writeDouble(sdv.get(i));
                    }
                }
                else {
                    SparseVector sv = (SparseVector)vector;
                    int[] nz = sv.getNonZeroIndices();
                    writer.writeInt(nz.length);
                    for (int i : nz) {
                        writer.writeInt(i);
                        writer.writeDouble(sv.getValue(i).doubleValue());
                    }
                }
            }
            else {
                // count how many are non-zero
                int nonZero = 0;
                for (int i = 0; i < vector.length(); ++i) {
                    if (vector.getValue(i).doubleValue() != 0d)
                        nonZero++;
                }
                writer.writeInt(nonZero);
                for (int i = 0; i < vector.length(); ++i) {
                    double d = vector.getValue(i).doubleValue();
                    if (d != 0d) {
                        writer.writeInt(i);
                        writer.writeDouble(d);
                    }
                }
            }

            break;
        }}
     }

    /**
     * Writes an empty {@code SemanticSpace} header to the output file.
     */
    private void writeEmptyHeader() throws IOException {
        SemanticSpaceIO.writeHeader(writer, format);
        // Write the extra space for the number of vectors and dimensions;
        switch (format) {
        case TEXT:
        case SPARSE_TEXT:
            char[] blanks = new char[128];
            Arrays.fill(blanks, ' ');
            String blankStr = new String(blanks);
            PrintWriter pw = new PrintWriter(writer);
            pw.println(blankStr);
            break;
        case BINARY: 
        case SPARSE_BINARY:
            writer.writeInt(0); // # of vectors
            writer.writeInt(0); // # of dimensions
            break;
        default:
            assert false : "unhandled s-space format";
        }
    }
}