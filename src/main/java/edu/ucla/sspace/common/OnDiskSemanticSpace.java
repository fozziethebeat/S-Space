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

import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A {@link SemanticSpace} where all vector data is kept on disk.  This class is
 * designed for large semantic spaces whose data, even in sparse format, will
 * not fit into memory.<p>
 *
 * The performance of this class is dependent on the format of the backing
 * vector data; {@code .sspace} files in {@link SSpaceFormat#BINARY binary} or
 * {@link SSpaceFormat#SPARSE_BINARY sparse binary} format will likely be faster
 * for accessing the data due to it being in its native format.<p>
 *
 * The {@code getWords} method will return words in the order they are stored on
 * disk.  Accessing the words in this order will have to a significant
 * performance improve over random access.  Furtherore, random access to {@link
 * SSpaceFormat#TEXT text} and {@link SSpaceFormat#SPARSE_TEXT sparse text}
 * formatted matrices will have particularly poor performance for large semantic
 * spaces, as the internal cursor to the data will have to restart from the
 * beginning of the file.<p>
 *
 * This class is thread-safe.
 *
 * @see SemanticSpaceIO
 * @see StaticSemanticSpace
 */
public class OnDiskSemanticSpace implements SemanticSpace {

    private static final Logger LOGGER = 
        Logger.getLogger(OnDiskSemanticSpace.class.getName());

    /**
     * A mapping of terms to offsets in the file where the word will be found.
     * If the {@code .sspace} is in binary, this will be a byte offset;
     * otherwise it is a line number in the text file.
     */
    private Map<String,Long> termToOffset;
    
    /**
     * Whether the underlying semantic space file contains a 4-byte header
     * indicating its format.  Before version 1.0 this was not required, so this
     * flag enables older .sspace files to be manually loaded with a specific
     * format, without breaking the binary compatibility of the rest of the
     * file.
     */
    private final boolean containsHeader;
    
    /**
     * The number of dimensions used in this semantic space.  This value is set
     * when the {@code termToOffset} map is populated and is used for error
     * checking in the files.
     */
    private int dimensions;

    /**
     * The name of this semantic space.
     */
    private String spaceName;

    /**
     * The reader for accessing a text-based {@code .sspace} file, or {@code
     * null} if the {@code .sspace} file is in binary format.
     */
    private RandomAccessBufferedReader textSSpace;

    /**
     * Byte access for a binary format {@code .sspace} file, or {@code null} if
     * the {@code .sspace} file is in text format.
     */
    private RandomAccessFile binarySSpace;

    /**
     * The format of the file that backs this space.
     */
    private SSpaceFormat format;

    /**
     * Creates the {@link OnDiskSemanticSpace} from the file.
     *
     * @param filename the name of a semantic space file
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     * @throws Error if the 4-byte header for the file contains an unrecognized
     *         semantic space format
     */
    public OnDiskSemanticSpace(String filename) throws IOException {
        this(new File(filename));
    }

    /**
     * Creates the {@link OnDiskSemanticSpace} from the provided file.
     *
     * @param file a file containing a store semantic space
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     * @throws Error if the 4-byte header for the file contains an unrecognized
     *         semantic space format
     */
    public OnDiskSemanticSpace(File file) throws IOException {
        containsHeader = true;
        SSpaceFormat format = SemanticSpaceIO.getFormat(file);
        if (format == null)
            throw new Error("Unrecognzied format in " +
                            "file: " + file.getName());
        loadOffsetsFromFormat(file, format);
    }

    /**
     * Creates the {@link OnDiskSemanticSpace} from the provided file in the
     * specified format.  This constructor should only be used for loading
     * semantic space files that do not have the 4-byte header indicating their
     * format.
     *
     * @param file a file containing a semantic space
     * @param format the format of the semanti space.
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    @Deprecated
    public OnDiskSemanticSpace(File file, SSpaceFormat format)
        throws IOException {
        containsHeader = false;
        loadOffsetsFromFormat(file, format);
    }
    
    /**
     * Loads the words and offets for each word's vector in the semantic space
     * file using the format as a guide to how the semantic space data is stored
     * in the file.
     * 
     * @param file a file containing semantic space data
     * @param format the format of the data in the file
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    private void loadOffsetsFromFormat(File file, SSpaceFormat format) 
            throws IOException {
        this.format = format;
        spaceName = file.getName();

        // NOTE: Use a LinkedHashMap here because this will ensure that the
        // words are returned in the same row-order as the matrix.  This
        // generates better disk I/O behavior for accessing the matrix since
        // each word is directly after the previous on disk.
        termToOffset = new LinkedHashMap<String,Long>();
        long start = System.currentTimeMillis();
        int dims = -1;
        RandomAccessFile raf = null;
        RandomAccessBufferedReader lnr = null;
        switch (format) {
            case TEXT:
                lnr = new RandomAccessBufferedReader(file);
                dims = loadTextOffsets(lnr);
                break;
            case BINARY:
                raf = new RandomAccessFile(file, "r");
                dims = loadBinaryOffsets(raf);
                break;
            case SPARSE_TEXT:
                lnr = new RandomAccessBufferedReader(file);
                dims = loadSparseTextOffsets(lnr);
                break;
            case SPARSE_BINARY:
                raf = new RandomAccessFile(file, "r");
                dims = loadSparseBinaryOffsets(raf);
                break;
            default:
            assert false : format;
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("loaded " + format + " .sspace file in " +
                (System.currentTimeMillis() - start) + "ms");
        }
    
        this.dimensions = dims;
        this.binarySSpace = raf;
        this.textSSpace = lnr;
    }

    /**
     * Loads the {@link SemanticSpace} from the {@code TEXT} formatted file,
     * adding its words to {@link #termToOffset} and returning the number of
     * dimensions for each vector
     *
     * @param textSSpace a file in {@link SSpaceFormat#TEXT text} format
     *
     * @return the number of dimensions for vectors in the loaded semantic space
     */
    private int loadTextOffsets(RandomAccessBufferedReader textSSpace) 
            throws IOException {
        String line = textSSpace.readLine();
        if (line == null)
            throw new IOError(new Throwable(
                        "An empty file has been passed in"));

        // Strip off the 4-byte (2 char) header
        if (containsHeader)
            line = line.substring(4);
        String[] dimensionStrs = line.split("\\s");
        int dimensions = Integer.parseInt(dimensionStrs[1]);

        int row = 1;    
        while ((line = textSSpace.readLine()) != null) {
            String[] termVectorPair = line.split("\\|");
            termToOffset.put(termVectorPair[0], Long.valueOf(row));
            row++;
        }

        return dimensions;
    }

    /**
     * Loads a vector from the backing semantic space file in {@code TEXT}
     * format using the predetermined offet for the word.
     *
     * @param word a word in the semantic space
     * @return the vector for the word or {@code null} if the word does not
     *         exist in the semantic space
     */
    private double[] loadTextVector(String word) throws IOException {
        Long lineNumber = termToOffset.get(word);
        if (lineNumber == null)
            return null;
        
        // skip to the line where the word's vector is found
        textSSpace.moveToLine(lineNumber.intValue());
        String line = textSSpace.readLine();
        
        double[] row = new double[dimensions];
        String[] termVectorPair = line.split("\\|");
        String[] values = termVectorPair[1].split("\\s");
        
        if (values.length != dimensions) {
            throw new IOError(new Throwable(
                        "improperly formated semantic space file"));
        }
        for (int c = 0; c < dimensions; ++c) {
            double d = Double.parseDouble(values[c]);
            row[c] = d;
        }
        return row;
    }

    /**
     * Loads the {@link SemanticSpace} from the text formatted file, adding its
     * words to {@link #termToOffset} and returning the {@code Matrix}
     * containing
     * the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#TEXT text} format
     */
    private int loadSparseTextOffsets(RandomAccessBufferedReader textSSpace) 
            throws IOException {
        String line = textSSpace.readLine();
        if (line == null)
            throw new IOError(new Throwable(
                        "An empty file has been passed in"));

        // Strip off the 4-byte (2 char) header
        if (containsHeader) {
            line = line.substring(4);
            System.out.println(line);
        }        

        String[] dimensions = line.split("\\s");
        int columns = Integer.parseInt(dimensions[1]);
        int rows = Integer.parseInt(dimensions[0]);
        int row = 1;
        
        while ((line = textSSpace.readLine()) != null) {
            String[] termVectorPair = line.split("\\|");
            termToOffset.put(termVectorPair[0], Long.valueOf(row));
            row++;
        }
        if ((row - 1) != rows)
            throw new IOException(String.format(
                "Different number of rows than specified (%d): %d", rows, row));
        return columns;    
    }

    /**
     * Loads a vector from the backing semantic space file in {@code
     * SPARSE_TEXT} format using the predetermined offet for the word.
     *
     * @param word a word in the semantic space
     * @return the vector for the word or {@code null} if the word does not
     *         exist in the semantic space
     */
    private double[] loadSparseTextVector(String word) throws IOException {
        Long lineNumber = termToOffset.get(word);
        if (lineNumber == null)
            return null;

        // skip to the line where the word's vector is found
        textSSpace.moveToLine(lineNumber.intValue());
        String line = textSSpace.readLine();
        if (line == null)
            System.out.printf("%s -> null row %d%n", word, lineNumber);
        double[] row = new double[dimensions];
            
        String[] termVectorPair = line.split("\\|");
        String[] values = termVectorPair[1].split(",");
        
        // even indicies are columns, odd are the values
        for (int i = 0; i < values.length; i +=2 ) {
            int col = Integer.parseInt(values[i]);
            double val = Double.parseDouble(values[i+1]);
            row[col] = val;
        }
        return row;
    }

    /**
     * Loads the {@link SemanticSpace} from the binary formatted file, adding
     * its words to {@link #termToOffset} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private int loadBinaryOffsets(RandomAccessFile binarySSpace) 
            throws IOException {

        // Reader off the 4-byte header if it exists
        if (containsHeader)
            binarySSpace.readInt();
    
        int rows = binarySSpace.readInt();
        int cols = binarySSpace.readInt();

        for (int row = 0; row < rows; ++row) {
            String word = binarySSpace.readUTF();
            termToOffset.put(word, binarySSpace.getFilePointer());
            // read and discard the rest of the vector
            for (int col = 0; col < cols; ++col) {
                binarySSpace.readDouble();
            }
        }
        return cols;
    }

    /**
     * Loads a vector from the backing semantic space file in {@code BINARY}
     * format using the predetermined offet for the word.
     *
     * @param word a word in the semantic space
     * @return the vector for the word or {@code null} if the word does not
     *         exist in the semantic space
     */
    private double[] loadBinaryVector(String word) throws IOException {
        Long byteOffset = termToOffset.get(word);
        if (byteOffset == null)
            return null;

        binarySSpace.seek(byteOffset);

        double[] vector = new double[dimensions];
        
        for (int col = 0; col < dimensions; ++col) {
            vector[col] = binarySSpace.readDouble();
        }

        return vector;
    }

    /**
     * Loads the {@link SemanticSpace} from the binary formatted file, adding
     * its words to {@link #termToOffset} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private int loadSparseBinaryOffsets(RandomAccessFile binarySSpace) 
            throws IOException {
        // Reader off the 4-byte header if it exists
        if (containsHeader) {
             int header = binarySSpace.readInt();
        }
        int rows = binarySSpace.readInt();
        int cols = binarySSpace.readInt();

        for (long row = 0; row < rows; ++row) {
            String word = binarySSpace.readUTF();
            termToOffset.put(word, binarySSpace.getFilePointer());
            
            // read and discard the rest of the vector
            int nonZero = binarySSpace.readInt();
            for (int i = 0; i < nonZero; ++i) {
                binarySSpace.readInt();
                binarySSpace.readDouble();
            }
        }
        return cols;
    }

    /**
     * Loads a vector from the backing semantic space file in {@code
     * SPARSE_BINARY} format using the predetermined offet for the word.
     *
     * @param word a word in the semantic space
     * @return the vector for the word or {@code null} if the word does not
     *         exist in the semantic space
     */
    private double[] loadSparseBinaryVector(String word) throws IOException {
        Long byteOffset = termToOffset.get(word);
        if (byteOffset == null)
            return null;

        binarySSpace.seek(byteOffset);
                
        int nonZero = binarySSpace.readInt();
        double[] vector = new double[dimensions];
        for (int i = 0; i < nonZero; ++i) {
            int col = binarySSpace.readInt();
            double val = binarySSpace.readDouble();
            vector[col] = val;
        }

        return vector;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(termToOffset.keySet());
    }
  
    /**
     * {@inheritDoc}
     *
     * @throws IOError if any {@code IOException} occurs when reading the data
     *         from the underlying semantic space file.
     */
    public synchronized Vector getVector(String word) {
        try {
            switch (format) {
            case TEXT:
                return new DenseVector(loadTextVector(word));
            case BINARY:
                return new DenseVector(loadBinaryVector(word));
            case SPARSE_TEXT:
                return new CompactSparseVector(loadSparseTextVector(word));
            case SPARSE_BINARY:
                return new CompactSparseVector(loadSparseBinaryVector(word));
            }
        } catch (IOException ioe) {
            // rethrow as something catastrophic must have happened to the
            // underlying .sspace file
            throw new IOError(ioe);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return spaceName;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return dimensions;
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     */
    public void processDocument(BufferedReader document) { 
        throw new UnsupportedOperationException(
            "OnDiskSemanticSpace instances cannot be updated");
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     */
    public void processSpace(Properties props) { 
        throw new UnsupportedOperationException(
            "OnDiskSemanticSpace instances cannot be updated");
    }

    /**
     * A utility class for randomly seeking in in a text file.  The current
     * implementation is only able to see in one direction internally, so calls
     * to seek to a previous location cause the entire file to be re-read up to
     * the desire position.  Accordingly, calls to sequential positions will
     * operate much faster.
     */
    private static class RandomAccessBufferedReader {

        /**
         * The file from which the data is being read
         */
        private final File backingFile;

        /**
         * The reader into the contents of the file
         */
        private BufferedReader current;

        /**
         * The number for the line that will be returned next by {@code
         * readLine}
         */
        private int currentLineNumber;

        /**
         * Creates a random access reader for the file and initializes its
         * position at the first line.
         *
         * @param f the file to be accessed
         */
        public RandomAccessBufferedReader(File f) throws IOException {
            backingFile = f;
            reset();
        }

        /**
         * Returns the number of the line that will next be returned by {@link
         * #nextLine()}.
         *
         * @return the line number of the next line that will be returned.
         */
        public int getLineNumber() {
            return currentLineNumber;
        }

        /**
         * Move the reader to the specified line number.  The next call to
         * {@code readLine} will return the line at that number.
         *
         * @param lineNum the number of the line that should next be returned
         */
        public void moveToLine(int lineNum) throws IOException {
            // If we are trying to go backward in the stream, close it and
            // restart from the beginning
            if (lineNum < currentLineNumber) {
                reset(); 
            }
            for (int i = currentLineNumber; i < lineNum; ++i) {
                current.readLine();
            }

            // Update to the new line number
            currentLineNumber = lineNum;
        }
        
        /**
         * Returns the line in the file at the current position and advances the
         * current position to the next line.
         *
         * @return the line at the current position
         */
        public String readLine() throws IOException {
            currentLineNumber++;
            return current.readLine();
        }

        /**
         * Resets the position of this reader to the very first line in the
         * file.
         */
        private void reset() throws IOException {
            current = new BufferedReader(new FileReader(backingFile));
            currentLineNumber = 0;
        }
    }

}
