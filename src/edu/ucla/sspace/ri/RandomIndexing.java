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

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.index.IntegerVectorGenerator;
import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.index.TernaryPermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.GeneratorMap;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.DenseIntVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A co-occurrence based approach to statistical semantics that uses a
 * randomized projection of a full co-occurrence matrix to perform
 * dimensionality reduction.  This implementation is based on three papers: <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif">M. Sahlgren, "Vector-based
 *     semantic analysis: Representing word meanings based on random labels," in
 *     <i>Proceedings of the ESSLLI 2001 Workshop on Semantic Knowledge
 *     Acquisition and Categorisation</i>, Helsinki, Finland, 2001.</li>
 *
 *   <li style="font-family:Garamond, Georgia, serif">M. Sahlgren, "An
 *     introduction to random indexing," in <i>Proceedings of the Methods and
 *     Applicatons of Semantic Indexing Workshop at the 7th International
 *     Conference on Terminology and Knowledge Engineering</i>, 2005.</li>
 *
 *   <li style="font-family:Garamond, Georgia, serif">M. Sahlgren, A. Holst, and
 *     P. Kanerva, "Permutations as a means to encode order in word space," in
 *     <i>Proceedings of the 30th Annual Meeting of the Cognitive Science
 *     Society (CogSci’08)</i>, 2008.</li>
 *
 * </ul>
 *
 * <p>
 *
 * Random Indexing (RI) is an efficient way of capturing word co-occurence.  In
 * most co-occurence models, a word-by-word matrix is constructed, where the
 * values denote how many times the columns's word occurred in the context of
 * the row's word.  RI instead represents co-occurrence through index vectors.
 * Each word is assigned a high-dimensional, random vector that is known as its
 * index vector.  These index vectors are very sparse - typically 7 &plusmn; 2
 * non zero bits for a vector of length 2048, which ensures that the the chance
 * of any two arbitrary index vectors having an overlapping meaning (i.e. a
 * cosine similarity that is non-zero) is very low.  Word semantics are
 * calculated for each word by keeping a running sum of all of the index vectors
 * for the words that co-occur.
 *
 * <p>
 *
 * <span style="font-family:Garamond, Georgia, serif">Sahlgren et
 * al. (2008)</span> introduced another variation on RI, where the semantics
 * also capture word order by using a permutation function.  For each occurrence
 * of a word, rather than summing the index vectors of the co-occurring words,
 * the permutation function is used to transform the co-occurring words based on
 * their position.  For example, consider the sentece, "the quick brown fox
 * jumps over the lazy dog."  With a window-size of 2, the semantic vector for
 * "fox" is added with the values &Pi;<sup>-2</sup>(quick<sub>index</sub>) +
 * &Pi;<sup>-1</sup>(brown<sub>index</sub>) +
 * &Pi;<sup>1</sup>(jumps<sub>index</sub>) +
 * &Pi;<sup>2</sup>(over<sub>index</sub>), where &Pi;<sup>{@code k}</sup>
 * denotes the {@code k}<sup>th</sup> permutation of the specified index vector.
 *
 * <p>
 *
 * This class defines the following configurable properties that may be set
 * using either the System properties or using the {@link
 * RandomIndexing#RandomIndexing(Properties)} constructor.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #WINDOW_SIZE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WINDOW_SIZE}
 *
 * <dd style="padding-top: .5em">This property sets the number of words before
 *      and after that are counted as co-occurring.  With the default value,
 *      {@value #DEFAULT_WINDOW_SIZE} words are counted before and {@value
 *      #DEFAULT_WINDOW_SIZE} words are counter after.  This class always uses a
 *      symmetric window. <p>
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
 *      <i>Default:</i> {@code true} 
 *
 * <dd style="padding-top: .5em">This property specifies whether to use a sparse
 *       encoding for each word's semantics.  Using a sparse encoding can result
 *       in a large saving in memory, while requiring more time to process each
 *       document.<p>
 *
 * </dl> <p>
 *
 * This class implements {@link Filterable}, which allows for fine-grained
 * control of which semantics are retained.  The {@link #setSemanticFilter(Set)}
 * method can be used to speficy which words should have their semantics
 * retained.  Note that the words that are filtered out will still be used in
 * computing the semantics of <i>other</i> words.  This behavior is intended for
 * use with a large corpora where retaining the semantics of all words in memory
 * is infeasible.<p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  At any given point in
 * processing, the {@link #getVectorFor(String) getVector} method may be used
 * to access the current semantics of a word.  This allows callers to track
 * incremental changes to the semantics as the corpus is processed.  <p>
 *
 * The {@link #processSpace(Properties) processSpace} method does nothing for
 * this class and calls to it will not affect the results of {@code
 * getVectorFor}.
 *
 * @see PermutationFunction
 * @see IndexVectorGenerator
 * 
 * @author David Jurgens
 */
public class RandomIndexing implements SemanticSpace, Filterable {

    public static final String RI_SSPACE_NAME =
        "random-indexing";

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.ri.RandomIndexing";

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
     * The property to specify whether the index vectors for co-occurrent words
     * should be permuted based on their relative position.
     */
    public static final String USE_PERMUTATIONS_PROPERTY = 
        PROPERTY_PREFIX + ".usePermutations";

    /**
     * The property to specify the fully qualified named of a {@link
     * PermutationFunction} if using permutations is enabled.
     */
    public static final String PERMUTATION_FUNCTION_PROPERTY = 
        PROPERTY_PREFIX + ".permutationFunction";

    /**
     * Specifies whether to use a sparse encoding for each word's semantics,
     * which saves space but requires more computation.
     */
    public static final String USE_SPARSE_SEMANTICS_PROPERTY = 
        PROPERTY_PREFIX + ".sparseSemantics";

    /**
     * The default number of words to view before and after each word in focus.
     */
    public static final int DEFAULT_WINDOW_SIZE = 2; // +2/-2

    /**
     * The default number of dimensions to be used by the index and semantic
     * vectors.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 4000;
    
    /**
     * A private source of randomization used for creating the index vectors.
     */
    // We use our own source rather than Math.random() to ensure reproduceable
    // behavior when a specific seed is set.
    //
    // NOTE: intentionally package-private to allow other RI-related classes to
    // based their randomness on a this class's seed.
    static final Random RANDOM = new Random();

    /**
     * A mapping from each word to its associated index vector
     */
    private final Map<String,TernaryVector> wordToIndexVector;

    /**
     * A mapping from each word to the vector the represents its semantics
     */
    private final Map<String,IntegerVector> wordToMeaning;

    /**
     * The number of dimensions for the semantic and index vectors.
     */
    private final int vectorLength;

    /**
     * The number of words to view before and after each focus word in a window.
     */
    private final int windowSize;

    /**
     * Whether the index vectors for co-occurrent words should be permuted based
     * on their relative position.
     */
    private final boolean usePermutations;

    /**
     * If permutations are enabled, the permutation function to use on the
     * index vectors.
     */
    private final PermutationFunction<TernaryVector> permutationFunc;

    /**
     * A flag for whether this instance should use {@code SparseIntegerVector}
     * instances for representic a word's semantics, which saves space but
     * requires more computation.
     */
    private final boolean useSparseSemantics;

    /**
     * An optional set of words that restricts the set of semantic vectors that
     * this instance will retain.
     */
    private final Set<String> semanticFilter;

    /**
     * Creates a new {@code RandomIndexing} instance using the current {@code
     * System} properties for configuration.
     */
    public RandomIndexing() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@code RandomIndexing} instance using the provided
     * properites for configuration.
     */
   public RandomIndexing(Properties properties) {
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
            : true;

        wordToIndexVector = new GeneratorMap<TernaryVector>(
                indexVectorGenerator);
        wordToMeaning = new ConcurrentHashMap<String,IntegerVector>();
        semanticFilter = new HashSet<String>();
    }

    /**
     * Returns an instance of the the provided class name, that implements
     * {@code PermutationFunction}.
     *
     * @param className the fully qualified name of a class
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
     * same instance of a {@code RandomIndexing} on multiple corpora while
     * keeping the same semantic space.
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
    private IntegerVector getSemanticVector(String word) {
        IntegerVector v = wordToMeaning.get(word);
        if (v == null) {
            // lock on the word in case multiple threads attempt to add it at
            // once
            synchronized(this) {
                // recheck in case another thread added it while we were waiting
                // for the lock
                v = wordToMeaning.get(word);
                if (v == null) {
                    v = (useSparseSemantics) 
                        ? new CompactSparseIntegerVector(vectorLength)
                        : new DenseIntVector(vectorLength);
                    wordToMeaning.put(word, v);
                }
            }
        }
        return v;
    }

   /**
     * {@inheritDoc}
     */ 
    public Vector getVector(String word) {
        IntegerVector v = wordToMeaning.get(word);
        if (v == null) {
            return null;
        }
        return Vectors.immutable(v);
    }

    /**
     * {@inheritDoc}
     */ 
    public String getSpaceName() {
        return RI_SSPACE_NAME + "-" + vectorLength + "v-" + windowSize + "w-" 
            + ((usePermutations) 
                    ? permutationFunc.toString() 
                    : "noPermutations");
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return vectorLength;
    }

    /**
     * {@inheritDoc}
     */ 
    public Set<String> getWords() {
        return Collections.unmodifiableSet(wordToMeaning.keySet());
    }

    /**
     * Returns an unmodifiable view on the token to {@link IntegerVector}
     * mapping used by this instance.  Any further changes made by this instance
     * to its token to {@code IntegerVector} mapping will be reflected in the
     * returned map.
     *
     * @return a mapping from the current set of tokens to the index vector used
     *         to represent them
     */
    public Map<String,TernaryVector> getWordToIndexVector() {
        return Collections.unmodifiableMap(wordToIndexVector);
    }
    
    /**
     * Updates the semantic vectors based on the words in the document.
     *
     * @param document {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        Queue<String> prevWords = new ArrayDeque<String>(windowSize);
        Queue<String> nextWords = new ArrayDeque<String>(windowSize);

        Iterator<String> documentTokens = 
            IteratorFactory.tokenizeOrdered(document);

        String focusWord = null;

        // prefetch the first windowSize words 
        for (int i = 0; i < windowSize && documentTokens.hasNext(); ++i)
            nextWords.offer(documentTokens.next());
        
        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();

            // shift over the window to the next word
            if (documentTokens.hasNext()) {
                String windowEdge = documentTokens.next(); 
                nextWords.offer(windowEdge);
            }    

            // If we are filtering the semantic vectors, check whether this word
            // should have its semantics calculated.  In addition, if there is a
            // filter and it would have excluded the word, do not keep its
            // semantics around
            boolean calculateSemantics =
                semanticFilter.isEmpty() || semanticFilter.contains(focusWord)
                && !focusWord.equals(IteratorFactory.EMPTY_TOKEN);
            
            if (calculateSemantics) {
                IntegerVector focusMeaning = getSemanticVector(focusWord);

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
                    
                    add(focusMeaning, iv);
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

                    add(focusMeaning, iv);
                }
            }

            // Last put this focus word in the prev words and shift off the
            // front of the previous word window if it now contains more words
            // than the maximum window size
            prevWords.offer(focusWord);
            if (prevWords.size() > windowSize) {
                prevWords.remove();
            }
        }    

        document.close();
    }
    
    /**
     * Does nothing.
     *
     * @param properties {@inheritDoc}
     */
    public void processSpace(Properties properties) {
    }

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
     * {@inheritDoc} Note that all words will still have an index vector
     * assigned to them, which is necessary to properly compute the semantics.
     *
     * @param semanticsToRetain the set of words for which semantics should be
     *        computed.
     */
    public void setSemanticFilter(Set<String> semanticsToRetain) {
        semanticFilter.clear();
        semanticFilter.addAll(semanticsToRetain);
    }

    /**
     * Atomically adds the values of the index vector to the semantic vector.
     * This is a special case addition operation that only iterates over the
     * non-zero values of the index vector.
     */
    private static void add(IntegerVector semantics, TernaryVector index) {
        // Lock on the semantic vector to avoid a race condition with another
        // thread updating its semantics.  Use the vector to avoid a class-level
        // lock, which would limit the concurrency.
        synchronized(semantics) {
            for (int p : index.positiveDimensions())
                semantics.add(p, 1);
            for (int n : index.negativeDimensions())
                semantics.add(n, -1);
        }
    }
}
