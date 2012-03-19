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

package edu.ucla.sspace.common;

import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import edu.ucla.sspace.util.IntegerMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An unmodifiable {@link SemanticSpace} whose data is loaded into memory from
 * an {@code .sspace} file.  Instance of this class perform no document
 * processing, and the {@code processDocument} and {@code processSpace} methods
 * throw an {@link UnsupportedOperationException}.  
 *
 * <p> In general, users should call {@link
 * edu.ucla.sspace.common.SemanticSpaceUtils#loadSemanticSpace(File)
 * SemanticSpaceUtils.loadSemanticSpace(File)} rather than create an instance of
 * this class directly.<p>
 *
 * This class is thread-safe
 *
 * @see OnDiskSemanticSpace
 * @see SemanticSpaceUtils
 * @see SemanticSpaceUtils.SSpaceFormat
 */
public class StaticSemanticSpace implements SemanticSpace {

    private static final Logger LOGGER = 
        Logger.getLogger(StaticSemanticSpace.class.getName());

    /**
     * The {@code Matrix} which contains the data read from a finished {@link
     * SemanticSpace}.
     */
    private Matrix wordSpace;

    /**
     * A mapping of terms to row indexes.  Also serves as a quick means of
     * retrieving the words known by this {@link SemanticSpace}.
     */
    private Map<String, Integer> termToIndex ;

    /**
     * The name of this semantic space.
     */
    private String spaceName;    

    /**
     * Creates the {@link StaticSemanticSpace} from the file.
     *
     * @param filename the name of a file containing {@code SemanticSpace} data.
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    public StaticSemanticSpace(String filename) throws IOException {
        this(new File(filename));
    }

    /**
     * Creates the {@link StaticSemanticSpace} from the provided file.
     *
     * @param file a file containing the data of a {@link
     *        edu.ucla.sspace.common.SemanticSpace}.
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    public StaticSemanticSpace(File file) throws IOException {
        spaceName = file.getName();
        SSpaceFormat format = SemanticSpaceIO.getFormat(file);
        if (format == null)
            throw new Error("Unrecognzied format in " +
                            "file: " + file.getName());
        DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(file)));
        // Read off the four byte header from the stream so the loading methods
        // do not see the data.  This is necessary to support older formats that
        // did not include the header.
        dis.readInt();
        loadFromFormat(dis, format);
    }

    /**
     * Creates the {@link StaticSemanticSpace} from the provided file in the
     * specified format.  This method is only to be used in accessing {@code
     * SemanticSpace} files that do not include the format in their file
     * contents.
     *
     * @param file a file containing the data of a {@link
     *        edu.ucla.sspace.common.SemanticSpace}.
     * @param format the format of the semantic space
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    @Deprecated public StaticSemanticSpace(File file, SSpaceFormat format) 
            throws IOException {
        loadFromFormat(new BufferedInputStream(
                           new FileInputStream(file)), format);
        spaceName = file.getName();
    }

    /**
     * Loads the semantic space data from the specified stream, using the format
     * to determine how the data is layed out internally within the stream.
     *
     * @param is the input stream from which the semantic space will be read
     * @param format the internal data formatting of the semantic space
     */
    private void loadFromFormat(InputStream is, SSpaceFormat format)
            throws IOException {
        // NOTE: Use a LinkedHashMap here because this will ensure that the
        // words are returned in the same row-order as the matrix.  This
        // generates better disk I/O behavior for accessing the matrix since
        // each word is directly after the previous on disk.
        termToIndex = new LinkedHashMap<String, Integer>();
        Matrix m = null;
        long start = System.currentTimeMillis();

            switch (format) {
            case TEXT:
                m = Matrices.synchronizedMatrix(loadText(is));
                break;
            case BINARY:
                m = Matrices.synchronizedMatrix(loadBinary(is));
                break;
            
            // REMINDER: we don't use synchronized here because the current
            // sparse matrix implementations are thread-safe.  We really should
            // be aware of this for when the file-based sparse matrix gets
            // implemented.  -jurgens 05/29/09
            case SPARSE_TEXT:
                m = loadSparseText(is);
                break;
            case SPARSE_BINARY:
                m = loadSparseBinary(is);
                break;
        }
                    
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("loaded " + format + " .sspace file in " +
                (System.currentTimeMillis() - start) + "ms");
        }	
        wordSpace = m;
    }

    /**
     * Loads the {@link SemanticSpace} from the text formatted file, adding its
     * words to {@link #termToIndex} and returning the {@code Matrix} containing
     * the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#TEXT text} format
     */
    private Matrix loadText(InputStream fileStream) throws IOException {
        Matrix matrix = null;

        BufferedReader br = 
                new BufferedReader(new InputStreamReader(fileStream));
        String line = br.readLine();
        if (line == null)
            throw new IOException("Empty .sspace file");
            // Strip off the 4-byte (2 char) header
        String[] dimensions = line.split("\\s");
        int rows = Integer.parseInt(dimensions[0]);
        int columns = Integer.parseInt(dimensions[1]);
        int index = 0;
        
        // reusable array for writing rows into the matrix
        double[] row = new double[columns];
        
        matrix = new ArrayMatrix(rows, columns);

        while ((line = br.readLine()) != null) {
            if (index >= rows)
                throw new IOException("More rows than specified");
            String[] termVectorPair = line.split("\\|");
            String[] values = termVectorPair[1].split("\\s");
            termToIndex.put(termVectorPair[0], index);
            if (values.length != columns) {
                throw new IOException(
                            "improperly formated semantic space file");
            }
            for (int c = 0; c < columns; ++c) {
                double d = Double.parseDouble(values[c]);
                row[c] = d;
                // matrix.set(index, c, d);
            }
            matrix.setRow(index, row);
            index++;
        }
        if (index != rows)
            throw new IOException(String.format(
                "Expected %d rows; saw %d", rows, index));
        return matrix;    
    }

    /**
     * Loads the {@link SemanticSpace} from the text formatted file, adding its
     * words to {@link #termToIndex} and returning the {@code Matrix} containing
     * the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#TEXT text} format
     */
    private Matrix loadSparseText(InputStream fileStream) throws IOException {
        Matrix matrix = null;

        BufferedReader br = 
                new BufferedReader(new InputStreamReader(fileStream));
        String line = br.readLine();
        if (line == null)
            throw new IOError(new Throwable(
                        "An empty file has been passed in"));
        String[] dimensions = line.split("\\s");
        int rows = Integer.parseInt(dimensions[0]);
        int columns = Integer.parseInt(dimensions[1]);

        int row = 0;
        
        // create a sparse matrix
        matrix = Matrices.create(rows, columns, false);
        while ((line = br.readLine()) != null) {
            String[] termVectorPair = line.split("\\|");
            String[] values = termVectorPair[1].split(",");
            termToIndex.put(termVectorPair[0], row);

            // even indicies are columns, odd are the values
            for (int i = 0; i < values.length; i +=2 ) {
                int col = Integer.parseInt(values[i]);
                double val = Double.parseDouble(values[i+1]);
                matrix.set(row, col, val);
            }
            row++;
        }
        return matrix;    
    }

    /**
     * Loads the {@link SemanticSpace} from the binary formatted file, adding
     * its words to {@link #termToIndex} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private Matrix loadBinary(InputStream fileStream) throws IOException {
        DataInputStream dis = new DataInputStream(fileStream);
        int rows = dis.readInt();
        int cols = dis.readInt();

        // create a dense matrix
        Matrix m = new ArrayMatrix(rows, cols);
        double[] d = new double[cols];
        for (int row = 0; row < rows; ++row) {
            String word = dis.readUTF();
            termToIndex.put(word, row);

            for (int col = 0; col < cols; ++col) {
                d[col] = dis.readDouble();
            }
            m.setRow(row, d);
        }
        return m;
    }

    /**
     * Loads the {@link SemanticSpace} from the binary formatted file, adding
     * its words to {@link #termToIndex} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private Matrix loadSparseBinary(InputStream fileStream) throws IOException {
        DataInputStream dis = new DataInputStream(fileStream);
        int rows = dis.readInt();
        int cols = dis.readInt();
        // Create the sparse matrix as individual rows since we can fully
        // allocate the indices values at once, rather than pay the log(n)
        // overhead of sorting them
        CompactSparseVector[] rowVectors = new CompactSparseVector[rows];

        for (int row = 0; row < rows; ++row) {
            String word = dis.readUTF();
            termToIndex.put(word, row);
            
            int nonZero = dis.readInt();
            int[] indices = new int[nonZero];
            double[] values = new double[nonZero];
            for (int i = 0; i < nonZero; ++i) {
                int nz = dis.readInt();
                double val = dis.readDouble();
                indices[i] = nz;
                values[i] = val;
            }
            rowVectors[row] = new CompactSparseVector(indices, values, cols);
        }
        return Matrices.asSparseMatrix(Arrays.asList(rowVectors));
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(termToIndex.keySet());
    }
  
    /**
     * {@inheritDoc}
     */
    public Vector getVector(String term) {
        Integer index = termToIndex.get(term);
        return (index == null)
            ? null
            : wordSpace.getRowVector(index.intValue());
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
        return wordSpace.columns();
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     *
     * @throws an {@link UnsupportedOperationException} if called
     */
    public void processDocument(BufferedReader document) { 
        throw new UnsupportedOperationException(
            "StaticSemanticSpace instances cannot be updated");
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     *
     * @throws an {@link UnsupportedOperationException} if called
     */
    public void processSpace(Properties props) { 
        throw new UnsupportedOperationException(
            "StaticSemanticSpace instances cannot be updated");
    }
}
