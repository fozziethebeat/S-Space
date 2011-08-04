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

package edu.ucla.sspace.ri;

import edu.ucla.sspace.vector.TernaryVector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Map;

/**
 * A utility class for loading and saving word-to-{@link TernaryVector}
 * mappings.  This class is intended to provide the ability to preseve the same
 * mapping between corpora, or between algorithms that both use {@code
 * TernaryVectors}s as index vectors.
 */
public class IndexVectorUtil {

    /**
     * Uninstantiable
     */
    private IndexVectorUtil() { }

    /**
     * Saves the mapping from word to {@link TernaryVector} to the specified
     * file.
     */
    public static void save(Map<String,TernaryVector> wordToIndexVector, 
                            File output) {
        try {
            FileOutputStream fos = new FileOutputStream(output);
            ObjectOutputStream outStream = new ObjectOutputStream(fos);
            outStream.writeObject(wordToIndexVector);
            outStream.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Loads a mapping from word to {@link TernaryVector} from the file
     */
    @SuppressWarnings("unchecked")
    public static Map<String,TernaryVector> load(File indexVectorFile) {
        try {
            FileInputStream fis = new FileInputStream(indexVectorFile);
            ObjectInputStream inStream = new ObjectInputStream(fis);
            Map<String, TernaryVector> vectorMap = 
                (Map<String, TernaryVector>) inStream.readObject();
            inStream.close();
            return vectorMap;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new Error(cnfe);
        }
    }
}
