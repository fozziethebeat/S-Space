/*
 * Copyright 2009 Alex Nau
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

package edu.ucla.sspace.hal;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.AtomicGrowingSparseHashMatrix;
import edu.ucla.sspace.matrix.Matrix;
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
import java.io.IOException;

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
 * A {@code SemanticSpace} implementation of the Hyperspace Analogue to Language
 * (HAL) algorithm described by Lund and Burgess.  This implementation is based
 * on the following paper: <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> K. Lund and C. Burgess
 *     Producing high-dimensional semantic spaces from lexical Co-occurrence
 *     <i>Behavior Research Methods, Instrumentation, and Computers</i>, 28,
 *     pages 203-208, 1996.  Available <a
 *     href="http://locutus.ucr.edu/reprintPDFs/lb96brmic.pdf">here</a>
 *
 *  </ul> See <a href="http://locutus.ucr.edu/Reprints.html">here</a> for
 *  additional papers that use HAL.  <p>
 *
 * HAL is based on recording the co-occurrence of words in a sparse matrix.  HAL
 * also incorporates word order information by treating the co-occurrences of
 * two words <i>x</i> <i>y</i> as being different than <i>y</i> <i>x</i>.  Each
 * word is assigned a unique index in the co-occurrence matrix.  For some word
 * <i>x</i>, when another word <i>x</i> co-occurs before, matrix entry
 * <i>x</i>,<i>y</i> is update.  Similarly, when <i>y</i> co-occurs after, the
 * matrix entry <i>y</i>,<i>x</i> is updated.  Therefore the full semantic
 * vector for any words is its row vector concatenated with its column
 * vector.<p>
 *
 * Typically, the full vectors are used (for an N x N matrix, these are 2*N in
 * length).  However, HAL also offers two posibilities for dimensionality
 * reduction.  Not all columns provide equal amount of information that can be
 * used to distinguish the meanings of the words.  Specifically, the information
 * theoretic <a
 * href="http://en.wikipedia.org/wiki/Information_entropy">entropy</a> of each
 * column can be calculated as a way of ordering the columns by their
 * importance.  Using this ranking, either a fixed number of columns may be
 * retained, or a threshold may be set to filter out low-entropy columns.<p>
 *
 * This class provides four parameters that may be set:
 *
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #WINDOW_SIZE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WINDOW_SIZE}
 *
 * <dd style="padding-top: .5em">This variable sets the number of words before
 *      and after that are counted as co-occurring.  With the default value,
 *      {@value #DEFAULT_WINDOW_SIZE} words are counted before and {@value
 *      #DEFAULT_WINDOW_SIZE} words are counter after.  This class always uses a
 *      symmetric window. <p>


 * <dt> <i>Property:</i> <code><b>{@value #WEIGHTING_FUNCTION_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link LinearWeighting edu.ucla.sspace.hal.LinearWeighting} 
 *
 * <dd style="padding-top: .5em">This property sets the fully-qualified class
 *      name of the {@link WeightingFunction} class that will be used to
 *      determine how to weigh co-occurrences.  HAL traditionally uses a ramped,
 *      linear weighting where those words occurring closets receive more
 *      weight, with a linear decrease based on distance.
 *
 * <dt> <i>Property:</i> <code><b>{@value #RETAIN_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> unset
 *
 * <dd style="padding-top: .5em">This optional property enables dimensionality
 *      reduction by retaining only a fixed number of columns.  The columns with
 *      the high entropy are retrained.  The value should be an integer.  This
 *      property may not be set concurrently with {@value
 *      #ENTROPY_THRESHOLD_PROPERTY}, and will throw an exception if done so.
 *
 * <dt> <i>Property:</i> <code><b>{@value #ENTROPY_THRESHOLD_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> unset
 *
 * <dd style="padding-top: .5em">This optional property enables dimensionality
 *      reduction by retaining only those columns whose entropy is above the
 *      specified threshold.  The value should be a double.  This property may
 *      not be set concurrently with {@value #RETAIN_PROPERTY}, and will throw
 *      an exception if done so.
 *
 * </dl><p>
 *
 * Note that the weight function can also be used to create special cases of the
 * HAL model, For example, an asymmetric window could be created by assigning a
 * weight of {@code 0} to all those co-occurrence on one side.
 *
 * @author Alex Nau
 * @author David Jurgens
 *
 * @see SemanticSpace 
 * @see WeightingFunction
 */
public class HyperspaceAnalogueToLanguage implements SemanticSpace {

    /**
     * Logger for HAL
     */
    private static final Logger LOGGER = 
        Logger.getLogger(HyperspaceAnalogueToLanguage.class.getName());

    /**
     * The default number of words before and after the focus word to include
     */
    public static final int DEFAULT_WINDOW_SIZE = 5;

    /**
     * A mapping from terms to initial indices in the co-occurrence matrix.
     */
    private final BasisMapping<String, String> termToIndex;

    /**
     * The number of words to consider in one direction to create the symmetric
     * window
     */
    private final int windowSize;
    
    private final double columnThreshold;

    private final int retainColumns;

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
    private AtomicGrowingSparseHashMatrix cooccurrenceMatrix;

    /**
     * The reduced matrix, if columns are to be dropped.
     */
    private Matrix reduced;

    /**
     * Constructs a new instance using the system properties for configuration.
     */
    public HyperspaceAnalogueToLanguage() {
        this(new StringBasisMapping(),
             DEFAULT_WINDOW_SIZE,
             new LinearWeighting(), 
             -1d,
             -1);
    }
    
    /**
     * Constructs a new instance using the provided properties for
     * configuration.
     */
    public HyperspaceAnalogueToLanguage(BasisMapping<String, String> basis,
                                        int windowSize,
                                        WeightingFunction weightFunction,
                                        double columnThreshold,
                                        int retainColumns) {
        this.cooccurrenceMatrix = new AtomicGrowingSparseHashMatrix();
        this.termToIndex = basis;
        this.windowSize = windowSize;
        this.weighting = weightFunction;
        this.columnThreshold = columnThreshold;
        this.retainColumns = retainColumns;

        reduced = null;
        wordIndexCounter = 0;
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
            
            int focusIndex = termToIndex.getDimension(focus);
            
            // Iterate through the words occurring after and add values
            int wordDistance = 1;
            addTokens(nextWords, focusIndex, wordDistance, matrixEntryToCount);

            // in front of the focus word
            wordDistance = -windowSize + (windowSize - prevWords.size());
            addTokens(prevWords, focusIndex, wordDistance, matrixEntryToCount);

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
            cooccurrenceMatrix.addAndGet(p.x, p.y, e.getValue());
        }
    }

    private void addTokens(Queue<String> words,
                           int foucsIndex,
                           int distance,
                           Map<Pair<Integer>, Double> matrixEntryToCount) {
        for (String word : words) {
            // skip adding co-occurence values for words that are not
            // accepted by the filter
            if (!word.equals(IteratorFactory.EMPTY_TOKEN)) {
                int index = termToIndex.getDimension(word);

                // Get the current number of times that the focus word has
                // co-occurred with this word before after it.  Weight the
                // word appropriately baed on distance
                Pair<Integer> p = new Pair<Integer>(index, focusIndex);
                double value = weighting.weight(distance, windowSize);
                Double curCount = matrixEntryToCount.get(p);
                matrixEntryToCount.put(p, (curCount == null)
                                       ? value : value + curCount);
            }
            distance++;
        }
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
        Integer index = termToIndex.getDimension(word);
        if (index == null)
            return null;
        // If the matrix hasn't had columns dropped then the returned vector
        // will be the combination of the word's row and column
        else if (reduced == null) {
            // NOTE: the matrix could be asymmetric if the a word has only
            // appeared on one side of a context (its row or column vector would
            // never have been set).  Therefore, check the index with the matrix
            // size first.
            SparseDoubleVector rowVec = (index < cooccurrenceMatrix.rows())
                ? cooccurrenceMatrix.getRowVectorUnsafe(index)
                : new CompactSparseVector(termToIndex.numDimensions());
            SparseDoubleVector colVec = (index < cooccurrenceMatrix.columns())
                ? cooccurrenceMatrix.getColumnVectorUnsafe(index)
                : new CompactSparseVector(termToIndex.numDimensions());

            return new ConcatenatedSparseDoubleVector(rowVec, colVec);
        }
        // The co-occurrence matrix has had columns dropped so the vector is
        // just the word's row
        else {
            return reduced.getRowVector(index);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        if (cooccurrenceMatrix != null)
            return cooccurrenceMatrix.columns() + cooccurrenceMatrix.rows();
        return reduced.columns();
    }

    private double[] getColumn(int col) {
        int rows = cooccurrenceMatrix.rows();
        double[] column = new double[rows];
        for (int row = 0; row < rows; ++row) {
            column[row] = cooccurrenceMatrix.get(row, col);
        }
        return column;
    }
    
    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        if (cooccurrenceMatrix.get(termToIndex.numDimensions() - 1,
                                   termToIndex.numDimensions() - 1) == 0d)
            cooccurrenceMatrix.set(termToIndex.numDimensions() - 1,
                                   termToIndex.numDimensions() - 1,
                                   0d);

        if (columnThreshold > -1d)
            thresholdColumns(columnThreshold);
        if (retainColumns > 0)
            retainOnly(retainColumns);
    }

    /**
     * Drops all but the specified number of columns, retaining those that have
     * the highest information theoretic entropy.
     *
     * @param columns the number of columns to keep
     */
    private void retainOnly(int columns) {
        int words = termToIndex.numDimensions();
        MultiMap<Double,Integer> entropyToIndex = 
            new BoundedSortedMultiMap<Double,Integer>(columns, false, 
                                  true, true);

        // first check all the columns in the co-occurrence matrix
        for (int col = 0; col < words; ++col) {
            entropyToIndex.put(Statistics.entropy(getColumn(col)), col);
        }

        // Next check the rows.  Note that in the full version, the row's values
        // become a columns with the word's row is appended to the column.
        for (int row = 0; row < words; ++row) {
            double[] rowArr = cooccurrenceMatrix.getRow(row);
            entropyToIndex.put(Statistics.entropy(rowArr), row + words);
        }

        LOGGER.info("reducing to " + columns + " columns");

        // create the next matrix that will contain the fixed number of columns
        reduced = new YaleSparseMatrix(words, columns);

        Set<Integer> indicesToKeep = 
            new HashSet<Integer>(entropyToIndex.values());

        for (int word = 0; word < words; ++word) {
            int newColIndex = 0;
            for (int col = 0; col < words * 2; ++col) {
                if (indicesToKeep.contains(col)) {
                    if (col < words) {
                        reduced.set(word, newColIndex, 
                                    cooccurrenceMatrix.get(word, col));
                    } else {
                        // the column value is really from one of the transposed
                        // rows
                        reduced.set(word, newColIndex, 
                                    cooccurrenceMatrix.get(col - words, word));
                    }
                    newColIndex++;
                }
            }
        }
        
        // replace the co-occurrence matrix with the truncated version
        cooccurrenceMatrix = null;
    }
        
    /**
     * Calculates the entropy of all the columns in the co-occurrence matrix and
     * removes those columns that are below the threshold, setting {@link
     * #reduced} to the remaining columns.
     *
     * @param threshold
     */
    private void thresholdColumns(double threshold) {
        int words = termToIndex.numDimensions();
        BitSet colsToDrop = new BitSet(words * 2);

        // first check all the columns in the co-occurrence matrix
        for (int col = 0; col < words; ++col) {
            double[] column = getColumn(col);
            double entropy = Statistics.entropy(column);

            if (entropy < threshold)
                colsToDrop.set(col);
        }

        // Next check the rows.  Note that in the full version, the row's values
        // become a columns with the word's row is appended to the column.
        for (int row = 0; row < words; ++row) {
            double[] rowArr = cooccurrenceMatrix.getRow(row);
            double entropy = Statistics.entropy(rowArr);

            // add an offset based on the number of words.
            if (entropy < threshold) 
                colsToDrop.set(row + words);
        }

        LOGGER.info("dropping " + colsToDrop.cardinality() + "/" + (words*2) +
                " columns, which were below the threshold of " + threshold);
        
        // create the next matrix that will contain only those columns with
        // enough entropy to pass the threshold
        reduced =
            new YaleSparseMatrix(words, (words*2)-colsToDrop.cardinality());

        for (int word = 0; word < words; ++word) {
            int newColIndex = 0;
            for (int col = 0; col < words * 2; ++col) {
                if (!colsToDrop.get(col)) {
                    if (col < words) {
                        reduced.set(word, newColIndex, 
                                cooccurrenceMatrix.get(word, col));
                    } else {
                        // the column value is really from one of the transposed
                        // rows
                        reduced.set(word, newColIndex, 
                                    cooccurrenceMatrix.get(col - words, word));
                    }
                    newColIndex++;
                }
            }
        }
        
        // replace the co-occurrence matrix with the truncated version
        cooccurrenceMatrix = null;
    }
        
    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return "hal-semantic-space";
    }
}
