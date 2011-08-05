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
 * This class is thread-safe
 *
 * @author Keith Stevens
 */
public class VectorMapSemanticSpace<T extends Vector> implements SemanticSpace {

    private static final Logger LOGGER = 
        Logger.getLogger(VectorMapSemanticSpace.class.getName());

    /**
     * The mapping from words to their semantic vectors
     */
    private final Map<String, T> wordSpace;

    /**
     * The total number of dimensions in this word space.
     */
    private final int dimensions;

    /**
     * The name of this semantic space.
     */
    private String spaceName;    

    public VectorMapSemanticSpace(Map<String, T> wordSpace,
                                  String spaceName,
                                  int dimensions) {
      this.wordSpace = wordSpace;
      this.dimensions = dimensions;
      this.spaceName = spaceName;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(wordSpace.keySet());
    }
  
    /**
     * {@inheritDoc}
     */
    public T getVector(String term) {
        return wordSpace.get(term);
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
     * A no-op
     */
    public void processDocument(BufferedReader document) { 
    }

    /**
     * A no-op
     */
    public void processSpace(Properties props) { 
    }
}
