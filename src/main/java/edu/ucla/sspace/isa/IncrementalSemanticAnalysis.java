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

package edu.ucla.sspace.isa;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.index.IntegerVectorGenerator;
import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.index.TernaryPermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.GeneratorMap;
import edu.ucla.sspace.util.SparseDoubleArray;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;

import java.lang.reflect.Constructor;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.logging.Logger;


/**
 * An implementation of Incremental Semantic Analysis (ISA).  This
 * implementation is based on the following paper.  <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif">M. Baroni, A. Lenci and
 *    L. Onnis. 2007. ISA meets Lara: An incremental word space model for
 *    cognitively plausible simulations of semantic learning. Proceedings of the
 *    ACL 2007 Workshop on Cognitive Aspects of Computational Language
 *    Acquisition, East Stroudsburg PA: ACL. 49-56.  Available <a
 *    href="http://clic.cimec.unitn.it/marco/publications/acl2007/coglearningacl07.pdf">here</a>
 *    </li>
 *
 * </ul>
 *
 * <p> ISA is notable in that it builds semantics incrementally using both
 * information from the co-occurrence of a word <i>and</i> the semantics of the
 * co-occurring word.  Similar to <a
 * href="http://code.google.com/p/airhead-research/wiki/RandomIndexing">Random
 * Indexing</a> (RI), ISA uses index vectors to reduce the number of dimensions
 * needed to represent the full co-occurrence matrix.  In contrast, other
 * semantic space algorithms such as RI, HAL and BEAGLE, ISA uses the semantics
 * of the co-occurring words to update the semantics of their neighbors.
 * Formally, the semantics of a word <i>w<sub>i</sub></i> are updated for the
 * co-occurrence of another word <i>w<sub>j</sub></i> as: <div
 * style="font-family:Constantia, Lucidabright, Lucida\ Serif, Lucida, DejaVu\
 * Serif, Bitstream\ Vera\ Serif, Liberation\ Serif, Georgia, serif">
 * sem(<i>w<sub>i</sub></i>) += i &middot (m<sub>c</sub> &middot
 * sem(<i>w<sub>j</sub></i>) + (1 - m<sub>c</sub>) &middot
 * IV(<i>w<sub>j</sub></i>)) </div> <br> where <i>sem</i> is the semantics for a
 * word, and <i>IV</i> is the index vector for a word.  <i>i</i> defines the
 * impact rate, which is how much the co-occurrence affects the semantics.
 * <i>m<sub>c</sub></i> defines the degree to which the semantics affect the
 * co-occurring word's semantics.  This weighting factor is based on the
 * frequency of occurrence; the semantics of frequently occurring words cause
 * less impact.  <i>m<sub>c</sub></i> is formally defined as 1 &divide;
 * <i>e</i><sup>freq(word) &divide; <i>k<sub>m</sub></i></sup>, where
 * <i>k<sub>m</sub></i> is a weighting factor for determing how quickly the
 * semantic of a a word diminish in their affect on co-occurring words.
 *
 * <p> This class defines the following configurable properties that may be set
 * using either the System properties or using the {@link
 * IncrementalSemanticAnalysis#IncrementalSemanticAnalysis(Properties)}
 * constructor.  The two most important properties for configuring ISA are
 * {@value #IMPACT_RATE_PROPERTY} and {@value #HISTORY_DECAY_RATE_PROPERTY}.
 * The values that these properties set have been initialized to the values
 * specified in Baroni et al.
 *
 * <dl style="margin-left: 1em">
 
 * <dt> <i>Property:</i> <code><b>{@value #IMPACT_RATE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_IMPACT_RATE}
 *
 * <dd style="padding-top: .5em">This property specifies the impact rate of
 *      co-occurrence, which specifies to what degree does the co-occurrence of
 *      one word affect the semantics of the other.  This rate affects both the
 *      impact of the index vector for a co-occurring word as well as the impact
 *      of the semantics.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #HISTORY_DECAY_RATE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_HISTORY_DECAY_RATE}
 *
 * <dd style="padding-top: .5em">This property specifies the decay rate at which
 *       the semantics of co-occurring words lessen their impact.  A word's
 *       frequency of occurrence is combined with the history decay rate to
 *       indicate the degree to which the word's semantics will influence
 *       (i.e. be added to) the semantics of a co-occurring word.  High values
 *       will cause the semantics of frequently occurring words to have minimal
 *       impact on other words' semantics.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #WINDOW_SIZE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WINDOW_SIZE}
 *
 * <dd style="padding-top: .5em">This property sets the number of words before
 *      and after that are counted as co-occurring.  With the default value,
 *      {@code 5} words are counted before and {@code 5} words are counter
 *      after.  This class always uses a symmetric window. <p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #VECTOR_LENGTH_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_VECTOR_LENGTH}
 *
 * <dd style="padding-top: .5em">This property sets the number of dimensions to
 *      be used for the index and semantic vectors. <p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #USE_PERMUTATIONS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code false}
 *
 * <dd style="padding-top: .5em">This property specifies whether to enable
 *      permuting the index vectors of co-occurring words.  Enabling this option
 *      will cause the word semantics to include word-ordering information.
 *      However this option is best used with a larger corpus.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #PERMUTATION_FUNCTION_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link edu.ucla.sspace.index.DefaultPermutationFunction 
 *      DefaultPermutationFunction} 
 *
 * <dd style="padding-top: .5em">This property specifies the fully qualified
 *      class name of a {@link PermutationFunction} instance that will be used
 *      to permute index vectors.  If the {@value #USE_PERMUTATIONS_PROPERTY} is
 *      set to {@code false}, the value of this property has no effect.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #USE_SPARSE_SEMANTICS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code false} 
 *
 * <dd style="padding-top: .5em">This property specifies whether to use a sparse
 *       encoding for each word's semantics.  Using a sparse encoding can result
 *       in a large saving in memory, while requiring more time to process each
 *       document.<p>
 *
 * </dl> <p>
 *
 * <p> Due to the incremental nature of ISA, instance of this class are
 * <i>not</i> designed to be multi-threaded.  Documents must be processed
 * sequentially to properly model how the semantics of co-occurring words affect
 * each other.  Multi-threading would induce an ambiguous ordering to
 * co-occurrence.
 * 
 * @author David Jurgens
 */
public class IncrementalSemanticAnalysis implements SemanticSpace {

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.isa.IncrementalSemanticAnalysis";

    /**
     * The property to specify the decay rate for determing how much the history
     * (semantics) of a word will affect the semantics of co-occurring words.
     */
    public static final String HISTORY_DECAY_RATE_PROPERTY =
        PROPERTY_PREFIX + ".historyDecayRate";

    /**
     * The property to specify the impact rate of word co-occurrence.
     */
    public static final String IMPACT_RATE_PROPERTY =
        PROPERTY_PREFIX + ".impactRate";

    /**
     * The property to specify the fully qualified named of a {@link
     * PermutationFunction} if using permutations is enabled.
     */
    public static final String PERMUTATION_FUNCTION_PROPERTY = 
        PROPERTY_PREFIX + ".permutationFunction";

    /**
     * The property to specify whether the index vectors for co-occurrent words
     * should be permuted based on their relative position.
     */
    public static final String USE_PERMUTATIONS_PROPERTY = 
        PROPERTY_PREFIX + ".usePermutations";

    /**
     * The property to specify the number of dimensions to be used by the index
     * and semantic vectors.
     */
    public static final String VECTOR_LENGTH_PROPERTY = 
        PROPERTY_PREFIX + ".vectorLength";

    /**
     * The property to specify the number of words to view before and after each
     * word in focus.
     */
    public static final String WINDOW_SIZE_PROPERTY = 
        PROPERTY_PREFIX + ".windowSize";

    /**
     * Specifies whether to use a sparse encoding for each word's semantics,
     * which saves space when words do not co-occur with many unique tokens, but
     * requires more computation.
     */
    public static final String USE_SPARSE_SEMANTICS_PROPERTY = 
        PROPERTY_PREFIX + ".sparseSemantics";

    /**
     * The default rate at which the history (semantics) decays when affecting
     * other co-occurring word's semantics.
     */
    public static final double DEFAULT_HISTORY_DECAY_RATE = 100;

    /**
     * The default rate at which the co-occurrence of a word affects the
     * semantics.
     */
    public static final double DEFAULT_IMPACT_RATE = 0.003;

    /**
     * The default number of dimensions to be used by the index and semantic
     * vectors.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 1800;

    /**
     * The default number of words to view before and after each word in focus.
     */
    public static final int DEFAULT_WINDOW_SIZE = 5; // +5/-5

    /**
     * The rate at which the increased frequency of a word decreases its effect
     * on the semantics of words with which it co-occurs.
     */
    private final double historyDecayRate;

    /**
     * The degree to which the co-occurrence of a word affects the semantics of
     * a second word.
     */
    private final double impactRate;

    /**
     * If permutations are enabled, what permutation function to use on the
     * index vectors.
     */
    private final PermutationFunction<TernaryVector> permutationFunc;

    /**
     * Whether the index vectors for co-occurrent words should be permuted based
     * on their relative position.
     */
    private final boolean usePermutations;

    /**
     * A flag for whether this instance should use {@code SparseDoubleVector}
     * instances for representic a word's semantics, these save space but
     * requires more computation.
     */
    private final boolean useSparseSemantics;

    /**
     * The number of dimensions for the semantic and index vectors.
     */
    private final int vectorLength;

    /**
     * The number of words to view before and after each word the focus word in
     * a window.
     */
    private final int windowSize;

    /**
     * A mapping from each word to its associated index vector
     */
    private final Map<String,TernaryVector> wordToIndexVector;

    /**
     * A mapping from each word to the vector the represents its semantics
     */
    private final Map<String,SemanticVector> wordToMeaning;

    /**
     * A mapping from each word to the number of times it has occurred in the
     * corpus at the time of processing.  This mapping is incrementally updated
     * as documents are processed.
     */
    private final Map<String,Integer> wordToOccurrences;

    /**
     * Creates a new {@code IncrementalSemanticAnalysis} instance using the
     * current {@code System} properties for configuration.
     */
    public IncrementalSemanticAnalysis() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@code IncrementalSemanticAnalysis} instance using the
     * provided properties for configuration.
     *
     * @param properties the properties that specify the configuration for this
     *        instance
     */
    public IncrementalSemanticAnalysis(Properties properties) {
        String vectorLengthProp = 
            properties.getProperty(VECTOR_LENGTH_PROPERTY);
        vectorLength = (vectorLengthProp != null)
            ? Integer.parseInt(vectorLengthProp)
            : DEFAULT_VECTOR_LENGTH;

        String windowSizeProp = properties.getProperty(WINDOW_SIZE_PROPERTY);
        windowSize = (windowSizeProp != null)
            ? Integer.parseInt(windowSizeProp)
            : DEFAULT_WINDOW_SIZE;

        String usePermutationsProp = 
            properties.getProperty(USE_PERMUTATIONS_PROPERTY);
        usePermutations = (usePermutationsProp != null)
            ? Boolean.parseBoolean(usePermutationsProp)
            : false;

        String permutationFuncProp =
            properties.getProperty(PERMUTATION_FUNCTION_PROPERTY);
        permutationFunc = (permutationFuncProp != null)
            ? loadPermutationFunction(permutationFuncProp)
            : new TernaryPermutationFunction();

        RandomIndexVectorGenerator indexVectorGenerator = 
            new RandomIndexVectorGenerator(vectorLength, properties);

        String useSparseProp = 
            properties.getProperty(USE_SPARSE_SEMANTICS_PROPERTY);
        useSparseSemantics = (useSparseProp != null)
            ? Boolean.parseBoolean(useSparseProp)
            : false;

        String decayRateProp = 
            properties.getProperty(HISTORY_DECAY_RATE_PROPERTY);
        historyDecayRate = (decayRateProp != null)
            ? Double.parseDouble(decayRateProp)
            : DEFAULT_HISTORY_DECAY_RATE;

        String impactRateProp =
            properties.getProperty(IMPACT_RATE_PROPERTY);
        impactRate = (impactRateProp != null)
            ? Double.parseDouble(impactRateProp)
            : DEFAULT_IMPACT_RATE;
            
        wordToIndexVector = 
            new GeneratorMap<TernaryVector>(indexVectorGenerator);
        wordToMeaning = new HashMap<String,SemanticVector>();
        wordToOccurrences = new HashMap<String,Integer>();
    }


    /**
     * Returns an instance of the the provided class name, that implements
     * {@code PermutationFunction}.
     *
     * @param className the fully qualified name of a class
     *
     * @return a permutation function of the specified class name
     */ 
    @SuppressWarnings("unchecked")
    private static PermutationFunction<TernaryVector> loadPermutationFunction(
            String className) {
        try {
            Class clazz = Class.forName(className);
            return (PermutationFunction<TernaryVector>)(clazz.newInstance());
        } catch (Exception e) {
            // catch all of the exception and rethrow them as an error
            throw new Error(e);
        }
    }

    /**
     * Removes all associations between word and semantics while still retaining
     * the word to index vector mapping.  This method can be used to re-use the
     * same mapping on multiple corpora while keeping the same semantic space.
     */
    public void clearSemantics() {
        wordToMeaning.clear();
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
    private SemanticVector getSemanticVector(String word) {
        SemanticVector v = wordToMeaning.get(word);
        if (v == null) {
            v = (useSparseSemantics) 
                ? new SparseSemanticVector(vectorLength)
                : new DenseSemanticVector(vectorLength);
            wordToMeaning.put(word, v);
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return "IncrementSemanticAnalysis-"
            + "-" + vectorLength + "v-" + windowSize + "w-" 
            + ((usePermutations) 
               ? permutationFunc.toString() 
               : "noPermutations");
    }
 
    /**
     * {@inheritDoc}
     */ 
    public Vector getVector(String word) {
        SemanticVector v = wordToMeaning.get(word);
        if (v == null) {
            return null;
        }
        return Vectors.immutable(v);
    }

    /**
     * {@inheritDoc}
     */ 
    public int getVectorLength() {
        return vectorLength;
    }

    /**
     * Returns an unmodifiable view on the token to {@link IntegerVector}
     * mapping used by this instance.  Any further changes made by this instance
     * to its token to {@code IntegerVector} mapping will be reflected in the
     * return map.
     *
     * @return a mapping from the current set of tokens to the index vector used
     *         to represent them
     */
    public Map<String,TernaryVector> getWordToIndexVector() {
        return Collections.unmodifiableMap(wordToIndexVector);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(wordToMeaning.keySet());
    }
    
    /**
     * {@inheritDoc}  Note that this method is <i>not</i> thread safe.
     */
    public void processDocument(BufferedReader document) throws IOException {
        Queue<String> prevWords = new ArrayDeque<String>(windowSize);
        Queue<String> nextWords = new ArrayDeque<String>(windowSize);

        Iterator<String> documentTokens = 
            IteratorFactory.tokenizeOrdered(document);

        String focusWord = null;

        // Prefetch the first windowSize words.  As soon as a word enters the
        // nextWords buffer increase its occurrence count.
        for (int i = 0; i < windowSize && documentTokens.hasNext(); ++i) 
            nextWords.offer(documentTokens.next());        
        
        while (!nextWords.isEmpty()) {
            
            focusWord = nextWords.remove();

            // shift over the window to the next word
            if (documentTokens.hasNext()) {
                String windowEdge = documentTokens.next(); 
                nextWords.offer(windowEdge);
            }    
                
            // Don't bother calculating the semantics for empty tokens
            // (i.e. words that were filtered out)
            if (!focusWord.equals(IteratorFactory.EMPTY_TOKEN)) {
                SemanticVector focusMeaning = getSemanticVector(focusWord);

                // Sum up the index vector for all the surrounding words.  If
                // permutations are enabled, permute the index vector based on
                // its relative position to the focus word.
                int permutations = -(prevWords.size());        
                for (String word : prevWords) {
                    // Skip the addition of any words that are excluded from the
                    // filter set.  Note that by doing the exclusion here, we
                    // ensure that the token stream maintains its existing
                    // ordering, which is necessary when permutations are taken
                    // into account.
                    if (word.equals(IteratorFactory.EMPTY_TOKEN)) {
                        ++permutations;
                        continue;
                    }
                    
                    TernaryVector iv = wordToIndexVector.get(word);
                    if (usePermutations) {
                        iv = permutationFunc.permute(iv, permutations);
                        ++permutations;
                    }
                            
                    updateSemantics(focusMeaning, word, iv);
                }
                
                // Repeat for the words in the forward window.
                permutations = 1;
                for (String word : nextWords) {
                    // Skip the addition of any words that are excluded from the
                    // filter set.  Note that by doing the exclusion here, we
                    // ensure that the token stream maintains its existing
                    // ordering, which is necessary when permutations are taken
                    // into account.
                    if (word.equals(IteratorFactory.EMPTY_TOKEN)) {
                        ++permutations;
                        continue;
                    }
                    
                    TernaryVector iv = wordToIndexVector.get(word);
                    if (usePermutations) {
                        iv = permutationFunc.permute(iv, permutations);
                        ++permutations;
                    }
                    
                    updateSemantics(focusMeaning, word, iv);
                }
            }

            // Last put this focus word in the prev words and shift off the
            // front of the previous word window if it now contains more words
            // than the maximum window size
            prevWords.offer(focusWord);

            // Increment the frequency count for the word now that it has been
            // seen and processed.
            Integer count = wordToOccurrences.get(focusWord);
            wordToOccurrences.put(focusWord, (count == null) ? 1 : count + 1);

            if (prevWords.size() > windowSize) {
                prevWords.remove();
            }
        }    

        document.close();
    }
        
    /**
     * Does nothing, as ISA in an incremental algorithm and no final processing
     * needs to be performed on the space.
     *
     * @properties {@inheritDoc}
     */
    public void processSpace(Properties properties) { }

    /**
     * Assigns the token to {@link IntegerVector} mapping to be used by this
     * instance.  The contents of the map are copied, so any additions of new
     * index words by this instance will not be reflected in the parameter's
     * mapping.
     *
     * @param m a mapping from token to the {@code IntegerVector} that should be
     *        used represent it when calculating other word's semantics
     */
    public void setWordToIndexVector(Map<String,TernaryVector> m) {
        wordToIndexVector.clear();
        wordToIndexVector.putAll(m);
    }

    /**
     * Update the semantics using the weighed combination of the semantics of
     * the co-occurring word and the provided index vector.  Note that the index
     * vector is provided so that the caller can permute it as necessary.
     *
     * @param toUpdate the semantics to be updated
     * @param cooccurringWord the word that is co-occurring 
     * @param iv the index vector for the co-occurring word, which has be
     *        permuted as necessary
     */    
    @SuppressWarnings("unchecked")
    private void updateSemantics(SemanticVector toUpdate,
                                 String cooccurringWord,
                                 TernaryVector iv) {
        SemanticVector prevWordSemantics = getSemanticVector(cooccurringWord);
        
        Integer occurrences = wordToOccurrences.get(cooccurringWord);
        if (occurrences == null)
            occurrences = 0;
        double semanticWeight = 
            1d / (Math.exp(occurrences / historyDecayRate));
                    
        // The meaning is updated as a combination of the index vector and the
        // semantics, which is weighted by how many times the co-occurring word
        // has been seen.  The semantics of frequently co-occurring words
        // receive less weight, i.e. the index vector is weighted more.
        add(toUpdate, iv, impactRate * (1 - semanticWeight));
        toUpdate.addVector(prevWordSemantics, impactRate * semanticWeight);
    }

    /**
     * Adds the index vector to the semantic vector using the percentage to
     * specify how much of each dimesion is added.
     *
     * @param semantics the semantic vector whose values will be modified by the
     *        index vector
     * @param index the index vector that will be added to the semantic vector
     * @param the percentage of the index vector's values that will be added to
     *        the semantic vector
     */
    private static void add(DoubleVector semantics,
                            TernaryVector index,
                            double percentage) {
        for (int p : index.positiveDimensions())
            semantics.add(p, percentage);
        for (int n : index.negativeDimensions())
            semantics.add(n, -percentage);
    }

    /**
     * A utility extension of {@link DoubleVector} that supports adding entire
     * vectors of the same type using a percentage of their values.  Instances
     * of this class are used rather than {@link VectorMath#add(Vector, Vector)}
     * with a {@link edu.ucla.sspace.vector.ScaledVector ScaledVector} to
     * improve the addition performance since the type of vector is known and
     * can be better optimized.
     */
    private interface SemanticVector<T extends DoubleVector>
            extends DoubleVector {
        public void addVector(T v, double percentage);
    }

    private class DenseSemanticVector extends DenseVector
            implements SemanticVector<DenseVector> {

        private static final long serialVersionUID = 1L;

        public DenseSemanticVector(int vectorLength) {
            super(vectorLength);
        }

        public void addVector(DenseVector v, double percentage) {
            int length = v.length();
            for (int i = 0; i < length; ++i) 
                add(i, percentage * v.get(i));
        }
    }
   
    private class SparseSemanticVector extends SparseHashDoubleVector
            implements SemanticVector<SparseDoubleVector> {

        private static final long serialVersionUID = 1L;

        public SparseSemanticVector(int vectorLength) {
            super(vectorLength);
        }

        public void addVector(SparseDoubleVector v, double percentage) {
            for (int n : v.getNonZeroIndices())
                add(n, percentage * v.get(n));
        }
    }
}
