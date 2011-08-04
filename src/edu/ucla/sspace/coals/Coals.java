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

package edu.ucla.sspace.coals;


import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.matrix.CellMaskedSparseMatrix;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.AtomicGrowingSparseMatrix;
import edu.ucla.sspace.matrix.CorrelationTransform;
import edu.ucla.sspace.matrix.MatlabSparseMatrixBuilder;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.Normalize;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.SVD;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import edu.ucla.sspace.text.IteratorFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Logger;


/**
 * An implementation of the COALS Semantic Space model.  This implementation is
 * based on:
 *
 * <p style="font-family:Garamond, Georgia, serif"> Rohde, D. L. T.,
 * Gonnerman, L. M., Plaut, D. C. (2005).  An Improved Model of Semantic
 * Similarity Based on Lexical Co-Occurrence. <i>Cognitive Science</i>
 * <b>(submitted)</b>.  Available <a
 * href="http://www.cnbc.cmu.edu/~plaut/papers/pdf/RohdeGonnermanPlautSUB-CogSci.COALS.pdf">here</a></p>
 *
 * COALS first computes a term by term co-occurrance using a ramped 4-word
 * window. Once all documents have been processed, the co-occurrence matrix will
 * be re ordered such that only the {@code N} most frequent terms have their
 * semantic vectors retained and only the {@code M} most frequent terms are used
 * as co-occurrence features.  These values can be set by the {@value
 *#MAX_WORDS_PROPERTY} and {@value MAX_DIMENSIONS_PROPERTY} properties,
 * resepctively.  After re ordering the semantic vectors and features, {@link
 * CorrelationTransform} is used to rerank all co-occurrence scores.  As part of
 * this transform, all negative correlations are dropped and replaced with a 0.
 * Finally, and optionally, the {@link SVD} is used to reduce the semantic
 * space.  To set the number of retained dimensions via {@link SVD}, set the
 * {@value REDUCE_DIMENSION_PROPERTY} property.
 *
 * @author Keith Stevens
 */
public class Coals implements SemanticSpace {

    /**
     * The property prefix for other settings.
     */
    public static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.coals.Coals";

    /**
     * Specifies whether or not the co-occurance matrix should be reduced.
     */
    public static final String REDUCE_MATRIX_PROPERTY =
        PROPERTY_PREFIX + ".reduce";

    /**
     * Specifies the number of dimensions the co-occurance matrix should be
     * reduced to.
     */
    public static final String REDUCE_DIMENSION_PROPERTY =
        PROPERTY_PREFIX + ".dimension";

    /**
     * Specifies the number of dimensions in the raw co-occurrance matrix to
     * maintain.
     */
    public static final String MAX_DIMENSIONS_PROPERTY = 
        PROPERTY_PREFIX + ".maxDimensions";

    /**
     * Specifies the number of words to build semantics for.
     */
    public static final String MAX_WORDS_PROPERTY = 
        PROPERTY_PREFIX + ".maxWords";

    /**
     * Specifies if Coals should not normalize the co-occurance matrix.
     */
    public static final String DO_NOT_NORMALIZE_PROPERTY = 
        PROPERTY_PREFIX + ".doNotNormalize";

    /**
     * The default number of dimensions to reduce to.
     */
    private static final String DEFAULT_REDUCE_DIMENSIONS = "800";

    /**
     * The default number of dimensions to save in the co-occurrance matrix.
     */
    private static final String DEFAULT_MAX_DIMENSIONS = "14000";

    /**
     * The default number of rows to save in the co-occurrance matrix.
     */
    private static final String DEFAULT_MAX_WORDS = "15000";

    /**
     * The name of this {@code SemanticSpace}
     */
    public static final String COALS_SSPACE_NAME = 
        "coals-semantic-space";

    /**
     * The logger used to record all output
     */
    private static final Logger COALS_LOGGER = 
        Logger.getLogger(Coals.class.getName());

    /**
     * A mapping from each word to the vector the represents its semantics
     */
    private Map<String, SparseDoubleVector> wordToSemantics;

    /**
     * A mapping from word to index number.
     */
    private Map<String, Integer> termToIndex;

    /**
     * A map containg the total frequency counts of each word.
     */
    private ConcurrentMap<String, AtomicInteger> totalWordFreq;

    /**
     * The final reduced matrix.
     */
    Matrix finalCorrelation;

    /**
     * Specifies if the matrix has been reduced by SVD.
     */
    boolean reduceMatrix;

    /**
     * Specifies the number of reduced dimensions if the matrix is reduced by
     * SVD. 
     */
    int reducedDimensions;

    /**
     * A counter for keeping track of the index values of words.
     */
    private int wordIndexCounter;

    /**
     * Creats a {@link Coals} instance.
     */
    public Coals() {
        termToIndex = new HashMap<String, Integer>();
        totalWordFreq = new ConcurrentHashMap<String, AtomicInteger>();
        wordToSemantics = new HashMap<String, SparseDoubleVector>(1024, 4f);
        finalCorrelation = null;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return termToIndex.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(String term) {
        Integer index = termToIndex.get(term);
        if (index == null) 
            return null;
        return Vectors.immutable(
                finalCorrelation.getRowVector(index.intValue()));
    }

    public String getSpaceName() {
        String ret = COALS_SSPACE_NAME;
        if (reduceMatrix)
            ret += "-svd-" + reducedDimensions;
        return ret;
    }

    public int getVectorLength() {
        return finalCorrelation.columns();
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        Map<String, Integer> wordFreq = new HashMap<String, Integer>();
        Map<String, SparseDoubleVector> wordDocSemantics =
            new HashMap<String, SparseDoubleVector>();

        // Setup queues to track the set of previous and next words in a
        // context.
        Queue<String> prevWords = new ArrayDeque<String>();
        Queue<String> nextWords = new ArrayDeque<String>();

        Iterator<String> it = IteratorFactory.tokenizeOrdered(document);

        for (int i = 0; i < 4 && it.hasNext(); ++i)
            nextWords.offer(it.next());

        // Compute the co-occurrance statistics of each focus word in the
        // document.
        while (!nextWords.isEmpty()) {

            // Slide over the context by one word.
            if (it.hasNext())
                nextWords.offer(it.next());

            // Get the focus word
            String focusWord = nextWords.remove();
            if (!focusWord.equals(IteratorFactory.EMPTY_TOKEN)) {
                getIndexFor(focusWord);

                // Update the frequency count of the focus word.
                Integer focusFreq = wordFreq.get(focusWord);
                wordFreq.put(focusWord, (focusFreq == null)
                        ? 1
                        : 1 + focusFreq.intValue());

                // Get the temprorary semantics for the focus word, create a new
                // vector for them if needed.
                SparseDoubleVector focusSemantics = wordDocSemantics.get(
                        focusWord);
                if (focusSemantics == null) {
                    focusSemantics = new SparseHashDoubleVector(
                            Integer.MAX_VALUE);
                    wordDocSemantics.put(focusWord, focusSemantics);
                }

                // Process the previous words.
                int offset = 4 - prevWords.size();
                for (String word : prevWords) {
                    offset++;
                    if (word.equals(IteratorFactory.EMPTY_TOKEN))
                        continue;
                    int index = getIndexFor(word);
                    focusSemantics.add(index, offset);
                }

                // Process the next words.
                offset = 5;
                for (String word : nextWords) {
                    offset--;
                    if (word.equals(IteratorFactory.EMPTY_TOKEN))
                        continue;
                    int index = getIndexFor(word);
                    focusSemantics.add(index, offset);
                }
            }

            prevWords.offer(focusWord);
            if (prevWords.size() > 4)
                prevWords.remove();
        }

        // Add the temporary vectors for each word in this document to the 
        // actual semantic fectors.
        for (Map.Entry<String, SparseDoubleVector> e :
                wordDocSemantics.entrySet()) {
            SparseDoubleVector focusSemantics = getSemanticVector(
                    e.getKey());
            // Get the non zero indices before hand so that they are cached
            // during the synchronized section.
            focusSemantics.getNonZeroIndices();
            synchronized (focusSemantics) {
                VectorMath.add(focusSemantics, e.getValue());
            }
        }

        // Store the total frequency counts of the words seen in this document
        // so far.
        for (Map.Entry<String, Integer> entry : wordFreq.entrySet()) {
            int count = entry.getValue().intValue();
            AtomicInteger freq = totalWordFreq.putIfAbsent(
                    entry.getKey(), new AtomicInteger(count));
            if (freq != null)
                freq.addAndGet(count);
        }
    }

    /**
     * Returns the current semantic vector for the provided word, or if the word
     * is not currently in the semantic space, a vector is added for it and
     * returned.
     *
     * @param word a word
     *
     * @return the {@code SemanticVector} for the provide word.
     */
    private SparseDoubleVector getSemanticVector(String word) {
        SparseDoubleVector v = wordToSemantics.get(word);
        if (v == null) {
            // lock on the word in case multiple threads attempt to add it at
            // once
            synchronized(this) {
                // recheck in case another thread added it while we were waiting
                // for the lock
                v = wordToSemantics.get(word);
                if (v == null) {
                    v = new CompactSparseVector();
                    wordToSemantics.put(word, v);
                }
            }
        }
        return v;
    }

    /**
     * Returns the index in the co-occurence matrix for this word.  If the word
     * was not previously assigned an index, this method adds one for it and
     * returns that index.
     */
    private int getIndexFor(String word) {
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
    public void processSpace(Properties props) {
        reduceMatrix =
            props.getProperty(REDUCE_MATRIX_PROPERTY) != null;
        reducedDimensions = Integer.parseInt(
                props.getProperty(REDUCE_DIMENSION_PROPERTY,
                                  DEFAULT_REDUCE_DIMENSIONS));
        boolean normalize =
            props.getProperty(DO_NOT_NORMALIZE_PROPERTY) == null;
        int maxWords = Integer.parseInt(
                props.getProperty(MAX_WORDS_PROPERTY,
                                  DEFAULT_MAX_WORDS));
        int maxDimensions = Integer.parseInt(
                props.getProperty(MAX_DIMENSIONS_PROPERTY,
                                  DEFAULT_MAX_DIMENSIONS));

        COALS_LOGGER.info("Droppring dimensions from co-occurrance matrix.");
        // Read in the matrix from a file with dimensions dropped.
        finalCorrelation = buildMatrix(maxWords, maxDimensions);
        COALS_LOGGER.info("Done dropping dimensions.");

        if (normalize) {
            COALS_LOGGER.info("Normalizing co-occurrance matrix.");

            // Normalize the matrix using correlation.
            int wordCount = finalCorrelation.rows();
            Transform correlation = new CorrelationTransform();
            finalCorrelation = correlation.transform(finalCorrelation);
            COALS_LOGGER.info("Done normalizing co-occurrance matrix.");
        }

        if (reduceMatrix) {
            COALS_LOGGER.info("Reducing using SVD.");
            try {
                File coalsMatrixFile =
                    File.createTempFile("coals-term-doc-matrix", "dat");
                coalsMatrixFile.deleteOnExit();
                MatrixIO.writeMatrix(finalCorrelation,
                                     coalsMatrixFile,
                                     Format.SVDLIBC_SPARSE_BINARY);
                if (reducedDimensions > finalCorrelation.columns())
                    reducedDimensions = finalCorrelation.columns();

                Matrix[] usv = SVD.svd(coalsMatrixFile,
                                       SVD.Algorithm.ANY,
                                       Format.SVDLIBC_SPARSE_BINARY,
                                       reducedDimensions);
                finalCorrelation = usv[0];
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
            COALS_LOGGER.info("Done reducing using SVD.");
        }
    }

    /**
     * Returns a {@link Matrix} that contains {@code maxWords} and {@code
     * maxDimensions} columns.  If {@code maxWords} is 0, then all words will be
     * returned in the semantic {@link Matrix}.  If {@code maxDimensions} is
     * larger than the number of observed features, then all observed features
     * will be maintained.  The resulting rows and columns are both ordred based
     * on the frequency of each term, in descending order, {@code termToIndex}
     * is modified to account for these changed.
     */
    private Matrix buildMatrix(int maxWords, int maxDimensions) {
        // Convert the vectors in the semantic map to a matrix.
        SparseDoubleVector[] vectorList =
            new SparseDoubleVector[wordToSemantics.size()];
        for (Map.Entry<String, SparseDoubleVector> e :
                wordToSemantics.entrySet())
            vectorList[getIndexFor(e.getKey())] = e.getValue();
        SparseMatrix matrix = Matrices.asSparseMatrix(
                Arrays.asList(vectorList));

        // If maxwords was set to 0, save all words.
        if (maxWords == 0)
            maxWords = wordToSemantics.size();

        COALS_LOGGER.info("Forming the inverse mapping from terms to indices.");
        // Calculate an inverse mapping from index to word since the binary file
        // stores things by index number.
        String[] indexToTerm = new String[termToIndex.size()];
        for (Map.Entry<String, Integer> entry : termToIndex.entrySet())
            indexToTerm[entry.getValue()] = entry.getKey();

        COALS_LOGGER.info("Sorting the terms based on frequency.");
        // Calculate the new indices for each word that will be kept based on
        // the frequency count, where the most frequent word will be first.
        ArrayList<Map.Entry<String, AtomicInteger>> wordCountList =
            new ArrayList<Map.Entry<String, AtomicInteger>>(
                    totalWordFreq.entrySet());
        Collections.sort(wordCountList, new EntryComp());

        // Calculate the new term to index mapping based on the order of the
        // word frequencies.
        COALS_LOGGER.info("Generating the index masks.");

        // Compute the number of dimensions to maintain. 
        int wordCount = (wordCountList.size() > maxDimensions)
            ? maxDimensions 
            : wordCountList.size();

        int[] rowMask = new int[maxWords];
        int[] colMask = new int[wordCount];

        // Create a new vector list to store the word semantics that will be
        // retained.  When this method exits, it will throw away all the other
        // vectors.
        SparseDoubleVector[] newVectorList = new SparseDoubleVector[maxWords];

        // For each of the terms that we have a mapping, add row and column
        // maskings for the indices of the first maxWords terms.  For all other
        // terms, remove the term to index mapping.
        int termCount = 0;
        for (Map.Entry<String, AtomicInteger> entry : wordCountList) {
            Integer oldIndex = termToIndex.get(entry.getKey());

            // Skip any non mapped terms.
            if (oldIndex == null)
                continue;

            // Add a row and/or column mask from the index of this word to it's
            // index in the original matrix.
            if (termCount < maxWords) {
                if (termCount <  wordCount)
                    colMask[termCount] = oldIndex;
                // Add the vector for this reserved word to the new vector list.
                newVectorList[termCount] = vectorList[oldIndex];
                
                // Record the new dimension for this term.
                rowMask[termCount] = termCount;
                termToIndex.put(entry.getKey(), termCount);
                termCount++;
            }
            // Drop all other mappings.
            else
                termToIndex.remove(entry.getKey());
        }

        wordToSemantics = null;
        matrix = Matrices.asSparseMatrix(Arrays.asList(newVectorList));
        // Return a masked version of the original matrix.
        return new CellMaskedSparseMatrix(matrix, rowMask, colMask);
    }

    private class EntryComp
            implements Comparator<Map.Entry<String,AtomicInteger>> {
        public int compare(Map.Entry<String, AtomicInteger> o1,
                           Map.Entry<String, AtomicInteger> o2) {
            int diff = o2.getValue().get() - o1.getValue().get();
            return (diff != 0) ? diff : o2.getKey().compareTo(o1.getKey());
        }
    }
}
