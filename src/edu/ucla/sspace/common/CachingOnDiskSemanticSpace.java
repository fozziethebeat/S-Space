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

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A {@link SemanticSpace} where most vector data is kept on disk, but
 * frequently accessed data is kept in memory.  This class is designed for large
 * semantic spaces whose data will not fit in memory and whose usage pattern
 * will frequently access a vector multiple times.<p>
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
 * @see OnDiskSemanticSpace
 */
public class CachingOnDiskSemanticSpace implements SemanticSpace {

    private static final Logger LOGGER = 
        Logger.getLogger(CachingOnDiskSemanticSpace.class.getName());

    /**
     * A mapping for words that have had their vector recently loaded into
     * memory.
     */
    private final Map<String, Vector> wordToVector;

    /**
     * The backing semantic space that reads in the data from disk.
     */
    private final SemanticSpace backingSpace;

    /**
     * Creates a new instance of {@code CachingOnDiskSemanticSpace} from the
     * data in the file with the specified name.
     *
     * @param filename the name of a file containing a semantic space
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the file
     */
    public CachingOnDiskSemanticSpace(String filename) throws IOException {
        this(new File(filename));
    }

    /**
     * Creates a new instance of {@code CachingOnDiskSemanticSpace} from the data in
     * the specified file.
     *
     * @param file a file containing a semantic space
     *
     * @throws IOException if any I/O exception occurs when reading the semantic
     *         space data from the fil
     */
    public CachingOnDiskSemanticSpace(File file) throws IOException {
        backingSpace = new OnDiskSemanticSpace(file);
        wordToVector = new WeakHashMap<String,Vector>();
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return backingSpace.getSpaceName();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
	return backingSpace.getWords();
    }
  
    /**
     * {@inheritDoc} If the word is in the semantic space, its vector will be
     * temporarily loaded into memory so that subsequent calls will not need to
     * go to disk.  As memory pressure increases, the vector will be discarded.
     *
     * @throws IOError if any {@code IOException} occurs when reading the data
     *         from the underlying semantic space file.
     */
    public synchronized Vector getVector(String word) {
        Vector vector = wordToVector.get(word);
        if (vector != null)
            return Vectors.immutable(vector);

        Vector v = backingSpace.getVector(word);
        if (v != null)
            wordToVector.put(word, v);
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return backingSpace.getVectorLength();
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     *
     * @throws an {@link UnsupportedOperationException} if called.
     */
    public void processDocument(BufferedReader document) { 
        throw new UnsupportedOperationException(
            "CachingOnDiskSemanticSpace instances cannot be updated");
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     *
     * @throws an {@link UnsupportedOperationException} if called.
     */
    public void processSpace(Properties props) { 
        throw new UnsupportedOperationException(
            "CachingOnDiskSemanticSpace instances cannot be updated");
    }
}
