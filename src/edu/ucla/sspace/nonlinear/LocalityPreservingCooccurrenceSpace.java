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

package edu.ucla.sspace.nonlinear;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.hal.EvenWeighting;
import edu.ucla.sspace.hal.WeightingFunction;

import edu.ucla.sspace.matrix.AffinityMatrixCreator;
import edu.ucla.sspace.matrix.AffinityMatrixCreator.EdgeType;
import edu.ucla.sspace.matrix.AffinityMatrixCreator.EdgeWeighting;
import edu.ucla.sspace.matrix.AtomicMatrix;
import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.matrix.LocalityPreservingProjection;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.logging.Logger;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author David Jurgens
 *
 * @see SemanticSpace 
 * @see LocalityPreservingSemanticAnalysis
 * @see AffinityMatrixCreator
 * @see LocalityPreservingProjection
 */
public class LocalityPreservingCooccurrenceSpace implements SemanticSpace {

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.lpsa.LocalityPreservingCooccurrenceSpace";
    
    /**
     * The property to specify the minimum entropy theshold a word should have
     * to be included in the vector space after processing.  The specified value
     * of this property should be a double
     */
    public static final String ENTROPY_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".threshold";

    /**
     * The property to specify the number of words to view before and after each
     * word in focus.
     */
    public static final String WINDOW_SIZE_PROPERTY =
        PROPERTY_PREFIX + ".windowSize";

    /**
     * The property to set the {@link WeightingFunction} to be used with
     * weighting the co-occurrence of neighboring words based on their distance.
     */
    public static final String WEIGHTING_FUNCTION_PROPERTY =
        PROPERTY_PREFIX + ".weighting";

    /**
     * The property to set the number of dimension to which the space should be
     * reduced using the SVD
     */
    public static final String LPCS_DIMENSIONS_PROPERTY =
        PROPERTY_PREFIX + ".dimensions";

    public static final String LPCS_AFFINITY_EDGE_PROPERTY =
        PROPERTY_PREFIX + ".affinityEdgeType";

    public static final String LPCS_AFFINITY_EDGE_PARAM_PROPERTY =
        PROPERTY_PREFIX + ".affinityEdgeTypeParam";

    public static final String LPCS_AFFINITY_EDGE_WEIGHTING_PROPERTY =
        PROPERTY_PREFIX + ".affinityEdgeWeighting";

    public static final String LPCS_AFFINITY_EDGE_WEIGHTING_PARAM_PROPERTY =
        PROPERTY_PREFIX + ".affinityEdgeWeightingParam";
    
    /**
     * The default number of words before and after the focus word to include
     */
    public static final int DEFAULT_WINDOW_SIZE = 5;

    /**
     * The default {@code WeightingFunction} to use.
     */        
    public static final WeightingFunction DEFAULT_WEIGHTING = 
        new EvenWeighting();

    /**
     * Logger for HAL
     */
    private static final Logger LOGGER = 
        Logger.getLogger(LocalityPreservingCooccurrenceSpace.class.getName());

    /**
     * Map that pairs the word with it's position in the matrix
     */
    private final Map<String,Integer> termToIndex;       

    /**
     * The number of words to consider in one direction to create the symmetric
     * window
     */
    private final int windowSize;
    
    /**
     * The type of weight to apply to a the co-occurrence word based on its
     * relative location
     */
    private final WeightingFunction weighting;

    /**
     * The number that keeps track of the index values of words
     */
    private int wordIndexCounter;

    /**
     * The matrix used for storing weight co-occurrence statistics of those
     * words that occur both before and after.
     */
    private SparseMatrix cooccurrenceMatrix;

    /**
     * An atomic wrapper around the {@link #cooccurrenceMatrix} instance to
     * provide atomic updates during document processing.
     */
    private AtomicMatrix atomicMatrix;    

    /**
     * The reduced matrix
     */
    private Matrix reduced;

    /**
     * Constructs a new instance using the system properties for configuration.
     */
    public LocalityPreservingCooccurrenceSpace() {
        this(System.getProperties());
    }
    
    /**
     * Constructs a new instance using the provided properties for
     * configuration.
     */
    public LocalityPreservingCooccurrenceSpace(Properties properties) {
        cooccurrenceMatrix = new GrowingSparseMatrix();
        atomicMatrix = Matrices.synchronizedMatrix(cooccurrenceMatrix);
        reduced = null;
        termToIndex = new ConcurrentHashMap<String,Integer>();
        
        wordIndexCounter = 0;

        String windowSizeProp = properties.getProperty(WINDOW_SIZE_PROPERTY);
        windowSize = (windowSizeProp != null)
            ? Integer.parseInt(windowSizeProp)
            : DEFAULT_WINDOW_SIZE;

        String weightFuncProp = 
        properties.getProperty(WEIGHTING_FUNCTION_PROPERTY);
        weighting = (weightFuncProp == null) 
            ? DEFAULT_WEIGHTING
            : loadWeightingFunction(weightFuncProp);
    }

    /**
     * Creates an instance of {@link WeightingFunction} based on the provide
     * class name.
     */
    private static WeightingFunction loadWeightingFunction(String classname) {
        try {
            @SuppressWarnings("unchecked")
            Class<WeightingFunction> clazz = 
            (Class<WeightingFunction>)Class.forName(classname);
            WeightingFunction wf = clazz.newInstance();
            return wf;
        } catch (Exception e) {
            // rethrow based on any reflection errors
            throw new Error(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void  processDocument(BufferedReader document) throws IOException {
        Queue<String> nextWords = new ArrayDeque<String>();
        Queue<String> prevWords = new ArrayDeque<String>();
            
        Iterator<String> documentTokens = 
            IteratorFactory.tokenizeOrdered(document);
            
        String focus = null;

        // Rather than updating the matrix every time an occurrence is seen,
        // keep a thread-local count of what needs to be modified in the matrix
        // and update after the document has been processed.  This saves
        // potential contention from concurrent writes.
        Map<Pair<Integer>,Double> matrixEntryToCount = 
            new HashMap<Pair<Integer>,Double>();
            
        //Load the first windowSize words into the Queue        
        for(int i = 0;  i < windowSize && documentTokens.hasNext(); i++)
            nextWords.offer(documentTokens.next());
            
        while(!nextWords.isEmpty()) {
            
            // Load the top of the nextWords Queue into the focus word
            focus = nextWords.remove();

            // Add the next word to nextWords queue (if possible)
            if (documentTokens.hasNext()) {        
                String windowEdge = documentTokens.next();
                nextWords.offer(windowEdge);
            }            

            // If the filter does not accept this word, skip the semantic
            // processing, continue with the next word
            if (focus.equals(IteratorFactory.EMPTY_TOKEN)) {
            // shift the window
                prevWords.offer(focus);
                if (prevWords.size() > windowSize)
                    prevWords.remove();
                continue;
            }
            
            int focusIndex = getIndexFor(focus);
            
            // Iterate through the words occurring after and add values
            int wordDistance = 1;
            for (String after : nextWords) {
                // skip adding co-occurence values for words that are not
                // accepted by the filter
                if (!after.equals(IteratorFactory.EMPTY_TOKEN)) {
                    int index = getIndexFor(after);
                    
                    // Get the current number of times that the focus word has
                    // co-occurred with this word appearing after it.  Weightb
                    // the word appropriately baed on distance
                    Pair<Integer> p = new Pair<Integer>(focusIndex, index);
                    double value = weighting.weight(wordDistance, windowSize);
                    Double curCount = matrixEntryToCount.get(p);
                    matrixEntryToCount.put(p, (curCount == null)
                                           ? value : value + curCount);
                }
             
                wordDistance++;        
            }

            wordDistance = -1; // in front of the focus word
            for (String before : prevWords) {
                // skip adding co-occurence values for words that are not
                // accepted by the filter
                if (!before.equals(IteratorFactory.EMPTY_TOKEN)) {
                    int index = getIndexFor(before);

                    // Get the current number of times that the focus word has
                    // co-occurred with this word before after it.  Weight the
                    // word appropriately baed on distance
                    Pair<Integer> p = new Pair<Integer>(index, focusIndex);
                    double value = weighting.weight(wordDistance, windowSize);
                    Double curCount = matrixEntryToCount.get(p);
                    matrixEntryToCount.put(p, (curCount == null)
                                           ? value : value + curCount);
                }
                wordDistance--;
            }
                    
            // last, put this focus word in the prev words and shift off the
            // front if it is larger than the window
            prevWords.offer(focus);
            if (prevWords.size() > windowSize)
                prevWords.remove();
        }

        // Once the document has been processed, update the co-occurrence matrix
        // accordingly.
        for (Map.Entry<Pair<Integer>,Double> e : matrixEntryToCount.entrySet()){
            Pair<Integer> p = e.getKey();
            atomicMatrix.addAndGet(p.x, p.y, e.getValue());
        }                    
    }

    /**
     * Returns the index in the co-occurence matrix for this word.  If the word
     * was not previously assigned an index, this method adds one for it and
     * returns that index.
     */
    private final int getIndexFor(String word) {
        Integer index = termToIndex.get(word);
        if (index == null) {     
            synchronized(this) {
                // recheck to see if the term was added while blocking
                index = termToIndex.get(word);
                // if another thread has not already added this word while the
                // current thread was blocking waiting on the lock, then add it.
                if (index == null) {
                    int i = wordIndexCounter++;
                    termToIndex.put(word, i);
                    return i; // avoid the auto-boxing to assign i to index
                }
            }
        }
        return index;
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        // If no documents have been processed, it will be empty        
        return Collections.unmodifiableSet(termToIndex.keySet());            
    }        

    /**
     * {@inheritDoc}
     */
    public Vector getVector(String word) {
        Integer index = termToIndex.get(word);
        if (index == null)
            return null;
        // If the matrix hasn't had columns dropped then the returned vector
        // will be the combination of the word's row and column
        else 
            return reduced.getRowVector(index);
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return reduced.columns();
    }
    
    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {

        // Set all of the default properties
        int dimensions = 300; 
        EdgeType edgeType = EdgeType.NEAREST_NEIGHBORS;
        double edgeTypeParam = 20;
        EdgeWeighting weighting = EdgeWeighting.COSINE_SIMILARITY;
        double edgeWeightParam = 0; // unused with default weighting
        
        // Then load any of the user-specified properties
        String dimensionsProp = 
            properties.getProperty(LPCS_DIMENSIONS_PROPERTY);
        if (dimensionsProp != null) {
            try {
                dimensions = Integer.parseInt(dimensionsProp);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                    LPCS_DIMENSIONS_PROPERTY + " is not an integer: " +
                    dimensionsProp);
            }
        }
        
        String edgeTypeProp = 
            properties.getProperty(LPCS_AFFINITY_EDGE_PROPERTY);
        if (edgeTypeProp != null) 
            edgeType = EdgeType.valueOf(edgeTypeProp.toUpperCase());
        String edgeTypeParamProp = 
            properties.getProperty(LPCS_AFFINITY_EDGE_PARAM_PROPERTY);
        if (edgeTypeParamProp != null) {
            try {
                edgeTypeParam = Double.parseDouble(edgeTypeParamProp);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                    LPCS_AFFINITY_EDGE_PARAM_PROPERTY + 
                    " is not an double: " + edgeTypeParamProp);
            }
        }
        
        String edgeWeightingProp = 
            properties.getProperty(LPCS_AFFINITY_EDGE_WEIGHTING_PROPERTY);
        if (edgeWeightingProp != null) 
            weighting = EdgeWeighting.valueOf(
                edgeWeightingProp.toUpperCase());
        String edgeWeightingParamProp = properties.getProperty(
            LPCS_AFFINITY_EDGE_WEIGHTING_PARAM_PROPERTY);
        if (edgeWeightingParamProp != null) {
            try {
                edgeWeightParam = Double.parseDouble(edgeWeightingParamProp);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                    LPCS_AFFINITY_EDGE_WEIGHTING_PARAM_PROPERTY + 
                    " is not an double: " + edgeWeightingParamProp);
            }
        }
        
        try {
            LOGGER.info("reducing to " + dimensions + " dimensions");
            File tiMap = new File("lpcs-term-index." + Math.random() + ".map");
            PrintWriter pw = new PrintWriter(tiMap);
            for (Map.Entry<String,Integer> e : termToIndex.entrySet())
                pw.println(e.getKey() + "\t" + e.getValue());
            pw.close();
            LOGGER.info("wrote term-index map to " + tiMap);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Calculate the affinity matrix for the cooccurrence matrix
        MatrixFile affinityMatrix = AffinityMatrixCreator.calculate(
            cooccurrenceMatrix, Similarity.SimType.COSINE, 
            edgeType, edgeTypeParam, weighting, edgeWeightParam);
        
        // Using the affinity matrix as a guide to locality, project the
        // co-occurrence matrix into the lower dimensional subspace
        reduced = LocalityPreservingProjection.project(
            cooccurrenceMatrix, affinityMatrix, dimensions);
    }
        
    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return "nws-semantic-space";
    }
}
