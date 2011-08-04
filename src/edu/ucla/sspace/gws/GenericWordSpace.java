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

package edu.ucla.sspace.gws;

import edu.ucla.sspace.basis.BasisMapping;

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.DimensionallyInterpretableSemanticSpace;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseIntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;


/**
 * The most basic co-occurrence model that counts word co-occurrence within a
 * sliding window with no further processing.  This class is meant as a generic
 * model that can be used to measure the efficacy of other {@link SemanticSpace}
 * instances.
 *
 * <p> This class also provides for a slight variation on the basic model by
 * differentiating co-occurrences on the basis of their relative position to the
 * focus word.  In such a case, for example, an occurrence of "red" two before
 * the focus word would be represented by a different position than "red" one
 * position before.  This is reminiscent of the {@link
 * edu.ucla.sspace.ri.RandomIndexing RandomIndexing} model with permutations.
 * However, unlike Random Indexing, this model is not fixed in the number of
 * dimensions it may use, with a possible {@code numWords * windowSize * 2}
 * dimensions.  Such a large number of dimensions can negatively impact the
 * further operations on the semantic space's vectors, e.g., finding the most
 * similar vectors for a word.
 *
 * <p> The dimensions of this space are annotated with a description of what
 * they represent.  In the basic model, this will be the co-occurring word.  In
 * the model that takes into account word order, the description will include
 * the relative position of the word.
 *
 * <p> This class defines the following configurable properties that may be set
 * using either the System properties or using the {@link
 * GenericWordSpace#GenericWordSpace(Properties)} constructor.
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
 * <dt> <i>Property:</i> <code><b>{@value #USE_WORD_ORDER_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code false}
 *
 * <dd style="padding-top: .5em">This property sets whether co-occurrences of
 *      the same word should be distinguished on the basis of their relative
 *      position to the focus word. <p>
 *
 * </dl> 
 *
 * <p> This class implements {@link Filterable}, which allows for fine-grained
 * control of which semantics are retained.  The {@link #setSemanticFilter(Set)}
 * method can be used to speficy which words should have their semantics
 * retained.  Note that the words that are filtered out will still be used in
 * computing the semantics of <i>other</i> words.  This behavior is intended for
 * use with a large corpora where retaining the semantics of all words in memory
 * is infeasible.<p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  At any given point in
  * processing, the {@link #getVector(String) getVector} method may be used
 * to access the current semantics of a word.  This allows callers to track
 * incremental changes to the semantics as the corpus is processed.  <p>
 *
 * The {@link #processSpace(Properties) processSpace} method does nothing for
 * this class and calls to it will not affect the results of {@code
 * getVector}.
 *
 * @author David Jurgens
 */
public class GenericWordSpace 
        implements DimensionallyInterpretableSemanticSpace<String>, Filterable, 
                   Serializable {

    private static final long serialVersionUID = 1L;

    public static final String GWS_SSPACE_NAME =
        "generic-word-space";

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.gws.GenericWordSpace";

    /**
     * The property to specify the number of words to view before and after each
     * word in focus.
     */
    public static final String WINDOW_SIZE_PROPERTY = 
        PROPERTY_PREFIX + ".windowSize";

    /**
     * The property to specify whether the relative positions of a word's
     * co-occurrence should be use distinguished from each other.
     */
    public static final String USE_WORD_ORDER_PROPERTY = 
        PROPERTY_PREFIX + ".useWordOrder";

    /**
     * The default number of words to view before and after each word in focus.
     */
    public static final int DEFAULT_WINDOW_SIZE = 2; // +2/-2
    
    /**
     * A mapping from each word to the vector the represents its semantics
     */
    private final Map<String,SparseIntegerVector> wordToSemantics;

    /**
     * The number of words to view before and after each focus word in a window.
     */
    private final int windowSize;

    /**
     * An optional set of words that restricts the set of semantic vectors that
     * this instance will retain.
     */
    private final Set<String> semanticFilter;

    /**
     * A mapping from a word an position to a specific dimension.  Note that if
     * word ordering is not being used, the dimension information is expected to
     * do nothing.
     */
    private final BasisMapping<Duple<String,Integer>,String> basisMapping;

    /**
     * Creates a new {@code GenericWordSpace} instance using the current {@code
     * System} properties for configuration.
     */
    public GenericWordSpace() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@code GenericWordSpace} instance using the provided
     * properites for configuration.
     */
   public GenericWordSpace(Properties properties) {

        String windowSizeProp = properties.getProperty(WINDOW_SIZE_PROPERTY);
        windowSize = (windowSizeProp != null)
            ? Integer.parseInt(windowSizeProp)
            : DEFAULT_WINDOW_SIZE;

        String useWordOrderProp = 
            properties.getProperty(USE_WORD_ORDER_PROPERTY);
        boolean useWordOrder = (useWordOrderProp != null)
            ? Boolean.parseBoolean(useWordOrderProp)
            : false;

        basisMapping = (useWordOrder)
            ? new WordOrderBasisMapping()
            : new WordBasisMapping();        

        wordToSemantics = new HashMap<String,SparseIntegerVector>(1024, 4f);
        semanticFilter = new HashSet<String>();
    }

    /**
     * Creates a new {@code GenericWordSpace} with the provided window size that
     * ignores word order.
     */
   public GenericWordSpace(int windowSize) {
       this(windowSize, new WordBasisMapping());
    }

    /**
     * Creates a new {@code GenericWordSpace} with the provided window size that
     * optionally includes word order.
     */
   public GenericWordSpace(int windowSize, boolean useWordOrder) {
       this(windowSize, (useWordOrder)
            ? new WordOrderBasisMapping()
            : new WordBasisMapping());
   }

    /**
     * Creates a new {@code GenericWordSpace} with the provided window size that
     * uses the specified basis mapping to map each co-occurrence at a specified
     * position to a dimension.
     *
     * @param basis a basis mapping from a duple that represents a word and its
     *        relative position to a dimension.
     */
   public GenericWordSpace(int windowSize, 
                           BasisMapping<Duple<String,Integer>,String> basis) {
       this.windowSize = windowSize;
       this.basisMapping = basis;
       wordToSemantics = new HashMap<String,SparseIntegerVector>(1024, 4f);
       semanticFilter = new HashSet<String>();
   }

    /**
     * Removes all associations between word and semantics while still retaining
     * the words' basis mapping.  This method can be used to re-use the same
     * instance of a {@code GenericWordSpace} on multiple corpora while keeping
     * the semantics of the dimensions identical.
     */
    public void clearSemantics() {
        wordToSemantics.clear();
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
    private SparseIntegerVector getSemanticVector(String word) {
        SparseIntegerVector v = wordToSemantics.get(word);
        if (v == null) {
            // lock on the word in case multiple threads attempt to add it at
            // once
            synchronized(this) {
                // recheck in case another thread added it while we were waiting
                // for the lock
                v = wordToSemantics.get(word);
                if (v == null) {
                    v = new CompactSparseIntegerVector(Integer.MAX_VALUE);
                    wordToSemantics.put(word, v);
                }
            }
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public String getDimensionDescription(int dimension) {
        return basisMapping.getDimensionDescription(dimension);
    }

   /**
     * {@inheritDoc} Note that because the word space is potentially growing as
     * new documents are processed, the length of the returned vector is equal
     * to the number of dimensions <i>at the time of this call</i> and therefore
     * may be less that the number of dimensions for the same word when obtained
     * at a later time.
     */ 
    public SparseIntegerVector getVector(String word) {
        SparseIntegerVector v = wordToSemantics.get(word);
        if (v == null) {
            return null;
        }
        // Note that because the word space is potentially ever growing, we wrap
        // the return vectors with the size of the semantic space at the time of
        // the call.
        return Vectors.immutable(Vectors.subview(v, 0, getVectorLength()));
    }

    /**
     * {@inheritDoc}
     */ 
    public String getSpaceName() {
        return GWS_SSPACE_NAME + "-w-" + windowSize
            + "-" + basisMapping;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return basisMapping.numDimensions();
    }

    /**
     * {@inheritDoc}
     */ 
    public Set<String> getWords() {
        return Collections.unmodifiableSet(wordToSemantics.keySet());
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
                SparseIntegerVector focusSemantics = 
                    getSemanticVector(focusWord);

                // Keep track of the relative position of the focus word in case
                // word ordering is being used.
                int position = -prevWords.size(); // first word is furthest
                for (String word : prevWords) {
                    // Skip the addition of any words that are excluded from the
                    // filter set.  Note that by doing the exclusion here, we
                    // ensure that the token stream maintains its existing
                    // ordering, which is necessary when word order is taken
                    // into account.
                    if (word.equals(IteratorFactory.EMPTY_TOKEN)) {
                        position++;
                        continue;
                    }
                    
                    int dimension = basisMapping.getDimension(
                        new Duple<String,Integer>(word, position));
                    synchronized(focusSemantics) {
                        focusSemantics.add(dimension, 1);
                    }
                    position++;
                }
            
                // Repeat for the words in the forward window.
                position = 1;
                for (String word : nextWords) {
                    // Skip the addition of any words that are excluded from the
                    // filter set.  Note that by doing the exclusion here, we
                    // ensure that the token stream maintains its existing
                    // ordering, which is necessary when word order is taken
                    // into account.
                    if (word.equals(IteratorFactory.EMPTY_TOKEN)) {
                        ++position;
                        continue;
                    }
                
                    int dimension = basisMapping.getDimension(
                        new Duple<String,Integer>(word, position));
                    synchronized(focusSemantics) {
                        focusSemantics.add(dimension, 1);
                    }
                    position++;
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

}
