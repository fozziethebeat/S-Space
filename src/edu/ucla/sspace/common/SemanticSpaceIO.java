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

package edu.ucla.sspace.common;

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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import java.util.Set;

import java.util.logging.Logger;


/**
 * A collection of utility methods for reading and writing {@link SemanticSpace}
 * instances.  For a full description of the supported formats, see the <a
 * href="http://code.google.com/p/airhead-research/wiki/FileFormats">file
 * formats</a> wiki page.
 *
 * <p>When loading a semantic space from a file, this class will automatically
 * try to determine whether its data will fit into memory.  If loading the space
 * would exceed the available memory, the space is only partially loaded and its
 * data stays on disk.  This allows users to load several semantic spaces at
 * once.
 *
 * <p>All of the {@code SemanticSpace} instances return by this class are thread
 * safe.  In addition they are all unmodifiable due to the limitations of
 * changing the backing data disk.  Calls to {@code processDocument} and {@code
 * processSpace} will result in an {@code UnsupportedOperationException} being
 * thrown.
 *
 * @see SemanticSpace
 * @see StaticSemanticSpace
 * @see OnDiskSemanticSpace
 */
public class SemanticSpaceIO {

    private static final Logger LOGGER =
        Logger.getLogger(SemanticSpaceIO.class.getName());

    /**
     * The type of formatting to use when writing a semantic space to a file.
     * See <a
     * href="http://code.google.com/p/airhead-research/wiki/FileFormats">here</a>
     * for file format specifications.
     */
    public enum SSpaceFormat 
        { TEXT, BINARY, SPARSE_TEXT, SPARSE_BINARY, SERIALIZE }

    /**
     * Uninstantiable
     */
    private SemanticSpaceIO() { }

    /**
     * Returns the format in which a semantic space is stored in the provided
     * file or {@code null} if the file does not have a recognized format.
     *
     * @param sspaceFile a file containing a semantic space
     *
     * @return the format in which a semantic space is stored in the provided
     *         file or {@code null} if the file does not have a recognized
     *         format.
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    static SSpaceFormat getFormat(File sspaceFile) throws IOException {
        DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(sspaceFile)));
        // read the expected header
        char header = dis.readChar();
        if (header != 's') {
            dis.close();
            return SSpaceFormat.SERIALIZE;
        }
        char encodedFormatCode = dis.readChar();
        int formatCode = encodedFormatCode - '0';
        dis.close();
        return (formatCode < 0 || formatCode > SSpaceFormat.values().length)
            ? SSpaceFormat.SERIALIZE
            : SSpaceFormat.values()[formatCode];                
    }

    /**
     * Returns {@code true} if the semantic space with the specified size and
     * format is estimated to fit in the available heap space if loaded.
     *
     * @param sspaceFileSize the size of a semantic space file in bytes
     * @param format the format in which the semantic space data is stored
     *
     * @return {@code true} if the data is expected to fit in memory
     */
    static boolean fitsInMemory(long sspaceFileSize, SSpaceFormat format) {
        // Determine how much memory is available for the new semantic space.
        // Note that this is a very rough estimate and is not 100% reliable due
        // the various state of the VM's GC cycle.  Moreover, there appears to
        // be some constant overhead for each format type (i.e. structures for
        // the data itself) that isn't taken into account but should be.
        // Nevertheless, this still provides a best-effort attempt.
        MemoryMXBean m = ManagementFactory.getMemoryMXBean();
        MemoryUsage mu = m.getHeapMemoryUsage();
        long available = mu.getMax() - mu.getUsed();
        boolean inMemory = false;

        switch (format) {            
        // For binary formatted matrices, we assume that their size on disk
        // is roughly equivalent to their size in memory.            
        case BINARY: // fallthrough
        case SPARSE_BINARY:
        case SERIALIZE:
            inMemory = sspaceFileSize < available;
            break;
        case TEXT:
            // For TEXT, it looks to be roughly 50% larger, so multiply by 2/3
            // as an estimate of its size in memory
            inMemory = (long)((2d/3) * sspaceFileSize) < available;
            break;
        case SPARSE_TEXT:
            // For SPARSE_TEXT, current estimate is 33% larger so multiply by
            // 3/4 to estimate
            inMemory = (long)(.75 * sspaceFileSize) < available;
            break;
        default:            
            assert false : format;
        }
        return inMemory;
    }

    /**
     * Loads and returns the {@link SemanticSpace} from the file with the
     * specified name.
     *
     * @param sspaceFileName the name of a file containing a {@link
     *        SemanticSpace} that has been written to disk
     *
     * @throws IllegalArgumentException if the file does not contain an internal
     *         format specification
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    public static SemanticSpace load(String sspaceFileName) throws IOException {
        return load(new File(sspaceFileName));
    }

    /**
     * Loads and returns the {@link SemanticSpace} stored at the file in the
     * specified format.
     *
     * @param sspaceFileName the name of a file containing a {@link
     *        SemanticSpace} that has been written to disk
     * @param format the format of the {@link SemanticSpace} in the file
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    @Deprecated public static SemanticSpace load(String sspaceFileName, 
                                                 SSpaceFormat format) 
            throws IOException {
        return load(new File(sspaceFileName), format);
    }

    /**
     * Loads and returns the {@link SemanticSpace} stored in the specified
     * file.
     *
     * @param sspaceFile a file containing a {@link SemanticSpace} that has
     *        been written to disk
     *
     * @throws IllegalArgumentException if the file does not contain an internal
     *         format specification
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    public static SemanticSpace load(File sspaceFile) throws IOException {
        // Peek at the file in order to determine how big it will be if unpacked
        SSpaceFormat format = getFormat(sspaceFile);
        if (format == null)
            throw new IllegalArgumentException(
                "The file " + sspaceFile.getName() + " does not contain any " +
                "internal format specification.");
        return loadInternal(sspaceFile, format, false);
    }
    
    /**
     * Loads and returns the {@link SemanticSpace} stored at the file in the
     * specified format.
     *
     * @param sspaceFile a file containing a {@link SemanticSpace} that has
     *        been written to disk
     * @param format the format of the {@link SemanticSpace} in the file
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    @Deprecated public static SemanticSpace load(File sspaceFile, 
                                                 SSpaceFormat format) 
            throws IOException {
        return loadInternal(sspaceFile, format, true);
    }

    /**
     * Loads the semantic space from the file using the format as a guide to its
     * internal layout.  The format is either manually provided by the caller,
     * or was specified within the file itself by the format header.  This
     * method provides a common way for wrapping the internal logic for deciding
     * whether the semantic space in the file will fit into memory if loaded
     * based on its formatting.
     *
     * @param sspaceFile the file from which the semantic space will be loaded
     * @param format the format of the semantic space data within the file
     * @param manuallySpecifiedFormat {@true} if the format of the file was
     *        manually specified by the caller and the file contains no
     *        formatting information
     *
     * @return the semantic space in the file 
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    private static SemanticSpace loadInternal(File sspaceFile, 
                                              SSpaceFormat format,
                                              boolean manuallySpecifiedFormat) 
            throws IOException {

        if (format.equals(SemanticSpaceIO.SSpaceFormat.SERIALIZE)) {
            LOGGER.fine("Loading serialized SemanticSpace from " + sspaceFile);
            ObjectInputStream ois = 
                new ObjectInputStream(new FileInputStream(sspaceFile));           
            SemanticSpace sspace = null;
            try {
                sspace = (SemanticSpace)(ois.readObject());
            } catch (ClassNotFoundException cnfe) {
                throw new IOException(cnfe);
            }
            return sspace;
        }
        // For SemanticSpace instances that have not been serialized, decide
        // whether they fit into memory before determing how to represent their
        // data
        else {
            if (fitsInMemory(sspaceFile.length(), format)) {
                LOGGER.fine(format + "-formatted .sspace file will fit into "
                            + "memory; creating StaticSemanticSpace");
                if (manuallySpecifiedFormat) {
                    @SuppressWarnings("deprecation")
                        SemanticSpace s = 
                        new StaticSemanticSpace(sspaceFile, format);
                    return s;
                }
                else
                    return new StaticSemanticSpace(sspaceFile);
            }
            else {
                LOGGER.fine(format + "-formatted .sspace file will not fit into"
                            + "memory; creating OnDiskSemanticSpace");
                if (manuallySpecifiedFormat) {
                    @SuppressWarnings("deprecation")
                        SemanticSpace s = 
                        new OnDiskSemanticSpace(sspaceFile, format);
                    return s;
                }
                else 
                    return new OnDiskSemanticSpace(sspaceFile);
            }
        }
    }

    /**
     * Writes the data contained in the {@link SemanticSpace} to the file with
     * the provided name using the {@link SSpaceFormat#TEXT} format.  See <a
     * href="#format">here</a> for file format specifications.
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    public static void save(SemanticSpace sspace, String outputFileName) 
            throws IOException {
        save(sspace, new File(outputFileName), SSpaceFormat.TEXT);
    }

    /**
     * Writes the data contained in the {@link SemanticSpace} to the provided
     * file using the {@link SSpaceFormat#TEXT} format.  See <a
     * href="#format">here</a> for file format specifications.
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    public static void save(SemanticSpace sspace, File output) 
            throws IOException {
        save(sspace, output, SSpaceFormat.TEXT);
    }

    /**
     * Writes the data contained in the {@link SemanticSpace} to the provided
     * file and format.  See <a href="#format">here</a> for file format
     * specifications.
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    public static void save(SemanticSpace sspace, File output, 
                            SSpaceFormat format) throws IOException {
        switch (format) {
        case TEXT:
            writeText(sspace, output);
            break;
        case BINARY:
            writeBinary(sspace, output);
            break;
        case SPARSE_TEXT:
            writeSparseText(sspace, output);
            break;
        case SPARSE_BINARY:
            writeSparseBinary(sspace, output);
            break;
        case SERIALIZE: 
            LOGGER.fine("Saving " + sspace + " to disk as serialized object");
            ObjectOutputStream oos = 
                new ObjectOutputStream(new FileOutputStream(output));
            oos.writeObject(sspace);
            oos.close();
            break;
        default:
            assert false : format;
        }
    }

    /**
     * Writes the .sspace format header to the output stream, indicating which
     * format the data will be saved in.  The header constists of a two byte
     * character for '{@code s}' and then a two byte character denoting the
     * specific format code.
     *
     * @param os the output stream into which a semantic space is to be saved
     * @param format the format of the data that will be written after the
     *        header
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    static void writeHeader(OutputStream os, SSpaceFormat format) 
            throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeChar('s');
        dos.writeChar('0' + format.ordinal());
    }

    /**
     * Writes the semantic space to the file using the {@code TEXT} format.
     *
     * @param sspace the semantic space to be written
     * @param output the file into which the space will be written
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    private static void writeText(SemanticSpace sspace, File output) 
            throws IOException {
        
        OutputStream os = new FileOutputStream(output);
        PrintWriter pw = new PrintWriter(os);
        Set<String> words = sspace.getWords();
        // determine how many dimensions are used by the vectors
        int dimensions = 0;
        if (words.size() > 0) {
            dimensions = sspace.getVectorLength();
        }
        writeHeader(os, SSpaceFormat.TEXT);
        // write out how many vectors there are and the number of dimensions
        pw.println(words.size() + " " + dimensions);
        LOGGER.fine("saving text S-Space with " + words.size() + 
                    " words with " + dimensions + "-dimensional vectors");

        for (String word : words) {
            pw.println(word + "|" + VectorIO.toString(sspace.getVector(word)));
        }
        pw.close();
    }

    /**
     * Writes the semantic space to the file using the {@code BINARY} format.
     *
     * @param sspace the semantic space to be written
     * @param output the file into which the space will be written
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    private static void writeBinary(SemanticSpace sspace, File output) 
            throws IOException {

        DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(output)));
        Set<String> words = sspace.getWords();
        // determine how many dimensions are used by the vectors
        int dimensions = 0;
        if (words.size() > 0) {
            dimensions = sspace.getVectorLength();
        }
        writeHeader(dos, SSpaceFormat.BINARY);
        // write out how many vectors there are and the number of dimensions
        dos.writeInt(words.size());
        dos.writeInt(dimensions);
        LOGGER.fine("saving binary S-Space with " + words.size() + 
                    " words with " + dimensions + "-dimensional vectors");

        for (String word : words) {
            dos.writeUTF(word);
        Vector v = sspace.getVector(word);
        for (int i = 0; i < v.length(); ++i) {
            dos.writeDouble(v.getValue(i).doubleValue());
            }
        }
        dos.close();
    }

    /**
     * Writes the semantic space to the file using the {@code SPARSE_TEXT}
     * format.
     *
     * @param sspace the semantic space to be written
     * @param output the file into which the space will be written
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    private static void writeSparseText(SemanticSpace sspace, File output) 
            throws IOException {
        OutputStream os = new FileOutputStream(output);
        PrintWriter pw = new PrintWriter(os);
        Set<String> words = sspace.getWords();
        // determine how many dimensions are used by the vectors
        int dimensions = 0;
        if (words.size() > 0) {
            dimensions = sspace.getVectorLength();
        }

        writeHeader(os, SSpaceFormat.SPARSE_TEXT);
        // print out how many vectors there are and the number of dimensions
        pw.println(words.size() + " " + dimensions);

        LOGGER.fine("saving sparse-text S-Space with " + words.size() + 
                    " words with " + dimensions + "-dimensional vectors");

        for (String word : words) {
            pw.print(word + "|");
            // for each vector, write all the non-zero elements and their indices
            Vector vector = sspace.getVector(word);
            StringBuilder sb = null;
            if (vector instanceof SparseVector) {
                if (vector instanceof DoubleVector) {
                    SparseDoubleVector sdv = (SparseDoubleVector)vector;
                    int[] nz = sdv.getNonZeroIndices();
                    sb = new StringBuilder(nz.length * 4);
                    // special case the first
                    sb.append(nz[0]).append(",").append(sdv.get(nz[0]));
                    for (int i = 1; i < nz.length; ++i)
                        sb.append(",").append(nz[i]).append(",").
                            append(sdv.getValue(nz[i]).doubleValue());
                }
                else {
                    SparseVector sv = (SparseVector)vector;
                    int[] nz = sv.getNonZeroIndices();                    
                    sb = new StringBuilder(nz.length * 4);
                    // special case the first
                    sb.append(nz[0]).append(",")
                        .append(sv.getValue(nz[0]).doubleValue());
                    for (int i = 1; i < nz.length; ++i)
                        sb.append(",").append(nz[i]).append(",").
                            append(sv.getValue(nz[i]).doubleValue());
                }
            }
            
            else {
                boolean first = true;
                sb = new StringBuilder(dimensions / 2);
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
        }
        pw.flush();
        pw.close();
    }

    /**
     * Writes the semantic space to the file using the {@code SPARSE_BINARY}
     * format.
     *
     * @param sspace the semantic space to be written
     * @param output the file into which the space will be written
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    private static void writeSparseBinary(SemanticSpace sspace, File output) 
            throws IOException {

        DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(output)));
        Set<String> words = sspace.getWords();
        // determine how many dimensions are used by the vectors
        int dimensions = 0;
        if (words.size() > 0) {
            dimensions = sspace.getVectorLength();
        }

        writeHeader(dos, SSpaceFormat.SPARSE_BINARY);
        // print out how many vectors there are and the number of dimensions
        dos.writeInt(words.size());
        dos.writeInt(dimensions);

        LOGGER.fine("saving sparse-binary S-Space with " + words.size() + 
                    " words with " + dimensions + "-dimensional vectors");

        for (String word : words) {
            dos.writeUTF(word);
            Vector vector = sspace.getVector(word);
            if (vector instanceof SparseVector) {
                if (vector instanceof DoubleVector) {
                    SparseDoubleVector sdv = (SparseDoubleVector)vector;
                    int[] nz = sdv.getNonZeroIndices();
                    dos.writeInt(nz.length);
                    for (int i : nz) {
                        dos.writeInt(i);
                        dos.writeDouble(sdv.get(i));
                    }
                }
                else {
                    SparseVector sv = (SparseVector)vector;
                    int[] nz = sv.getNonZeroIndices();
                    dos.writeInt(nz.length);
                    for (int i : nz) {
                        dos.writeInt(i);
                        dos.writeDouble(sv.getValue(i).doubleValue());
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
                dos.writeInt(nonZero);
                for (int i = 0; i < vector.length(); ++i) {
                    double d = vector.getValue(i).doubleValue();
                    if (d != 0d) {
                        dos.writeInt(i);
                        dos.writeDouble(d);
                    }
                }
            }
        }
        dos.close();
    }
}
