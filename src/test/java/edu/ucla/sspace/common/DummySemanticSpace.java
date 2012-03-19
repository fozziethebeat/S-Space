package edu.ucla.sspace.common;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.text.*; 
import edu.ucla.sspace.util.*;
import edu.ucla.sspace.vector.*;

import java.io.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A test-only {@link SemanticSpace}, where all the semantic vectors must be
 * manually asssigned.
 */
public class DummySemanticSpace implements SemanticSpace {

    private final Map<String,Vector> wordToVector;

    private int dimensions;
    
    public DummySemanticSpace() {
        wordToVector = new HashMap<String,Vector>();
    }
    
    /**
     * Does nothing
     */
    public void processDocument(BufferedReader document) throws IOException { }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return wordToVector.keySet();
    }

    /**
     * Returns the manually assigned vector for the word
     */
    public Vector getVector(String word) {
        return wordToVector.get(word);
    }

    /**
     * Sets the vector for the word
     */
    public Vector setVector(String word, Vector vector) {
        dimensions = vector.length();
        return wordToVector.put(word, vector);
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return dimensions;
    }

    /**
     * Does nothing
     */
    public void processSpace(Properties properties) { }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return "DummySemanticSpace";
    }

}
