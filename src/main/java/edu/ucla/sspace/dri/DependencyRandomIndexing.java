/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.dri;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractorManager;
import edu.ucla.sspace.dependency.DependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyRelationAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyPermutationFunction;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FlatPathWeight;
import edu.ucla.sspace.dependency.UniversalRelationAcceptor;

import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Generator;
import edu.ucla.sspace.util.GeneratorMap;
import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseIntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.logging.Logger;


/**
 * A co-occurrence based approach to statistical semantics that uses dependency
 * parse trees and approximates a full co-occurrence matrix by using a
 * randomized projection. This implementation is an extension of {@link
 * edu.ucla.sspace.ri.RandomIndexing}, which is based on three papers: <ul>
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
 * </p>
 *
 * The technique for incorprating dependnecy parse trees is based on the following paper:
 *   <li style="font-family:Garamond, Georgia, serif">S Pado and M. Lapata,
 *   "Dependency-Based Construction of Semantic Space Models," in <i>Association
 *   for Computational Linguistics</i>, 2007</li>
 *
 * <p>
 *
 * Dependency Random Indexing (DRI) extends Random Indexing by restricting a
 * word's context to be set of words with which it has a syntactic relationship.
 * Full word co-occurrence models have shown that this restricted interpretation
 * of a context can improve the semantic representations.  DRI uses the same
 * approximation technique as Random Indexing to project this full co-occurrence
 * space into a significantly smaller dimensional space.  This projection is
 * done through use of index vectors, each of which are sparse and mostly
 * orthogonal to all other index vectors.  The summation of a word's index
 * vectors corresponds directly to that word's occurrence in a context.
 *
 * <p> 
 *
 * While Random Indexing uses permutations of these index vectors to encode
 * lexical position, a shallow form of syntactic structure, DRI extends the
 * notion of permutations to allow for the encoding of dependency relationships.
 * Through this modification, the set of relationships between any two
 * co-occurirng words in a sentence can be encoded, as can the distance between
 * the two words.  Under this model, each possible dependency relationship could
 * have it's own permutation function, as could each possible distance between
 * co-occurring words.
 *
 * </p>
 *
 * This class defines the following configurable properties that may be set
 * using either the System properties or using the {@link
 * DependencyRandomIndexing#DependencyRandomIndexing(
 * DependencyExtractor, DependencyPermutationFunction, Properties)} constructor.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #DEPENDENCY_ACCEPTOR_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link UniversalRelationAcceptor}
 *
 * <dd style="padding-top: .5em">This property sets {@link
 *      DependencyRelationAcceptor} to use for validating dependency paths.  If a
 *      path is rejected it will not influence either the lemma vector or the
 *      selectional preference vectors. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #DEPENDENCY_PATH_LENGTH_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value DEFAULT_DEPENDENCY_PATH_LENGTH}
 *
 * <dd style="padding-top: .5em">This property sets the maximal length a
 *      dependency path can be for it to be accepted.  Paths beyond this length
 *      will not contribute towards either the lemma vectors or selectional
 *      preference vectors. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #VECTOR_LENGTH_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link DEFAULT_VECTOR_LENGTH}
 *
 * <dd style="padding-top: .5em">This property sets the number of dimensions in
 *      the word space.
 *
 * </dl>
 *
 * </p>
 *
 * This class implements {@link Filterable}, which allows for fine-grained
 * control of which semantics are retained.  The {@link #setSemanticFilter(Set)}
 * method can be used to speficy which words should have their semantics
 * retained.  Note that the words that are filtered out will still be used in
 * computing the semantics of <i>other</i> words.  This behavior is intended for
 * use with a large corpora where retaining the semantics of all words in memory
 * is infeasible.
 *
 * </p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  At any given point in
 * processing, the {@link #getVectorFor(String) getVector} method may be used
 * to access the current semantics of a word.  This allows callers to track
 * incremental changes to the semantics as the corpus is processed.
 *
 * </p>
 *
 * The {@link #processSpace(Properties) processSpace} method does nothing for
 * this class and calls to it will not affect the results of {@code
 * getVectorFor}.
 *
 * @see RandomIndexing
 * @see DependencyPermutationFunction
 *
 * @author Keith Stevens
 */
public class DependencyRandomIndexing implements SemanticSpace {

    /**
     * The base prefix for all {@code DependencyRandomIndexing}
     * properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.dri.DependencyRandomIndexing";

    /**
     * The property for setting the number of dimensions in the word space.
     */
    public static final String VECTOR_LENGTH_PROPERTY =
        PROPERTY_PREFIX + ".indexVectorLength";

    /**
     * The property for setting the {@link DependencyRelationAcceptor}.
     */
    public static final String DEPENDENCY_ACCEPTOR_PROPERTY =
        PROPERTY_PREFIX + ".dependencyAcceptor";

    /**
     * The property for setting the maximal length of any {@link
     * DependencyPath}.
     */
    public static final String DEPENDENCY_PATH_LENGTH_PROPERTY =
        PROPERTY_PREFIX + ".dependencyPathLength";

    /**
     * The default vector length.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 50000;

    /**
     * The default legnth a dependency path may have.
     */
    public static final int DEFAULT_DEPENDENCY_PATH_LENGTH = Integer.MAX_VALUE;

    /**
     * The Semantic Space name for {@link DependencyRandomIndexing}
     */
    public static final String SSPACE_NAME = 
        "dependency-random-indexing";

    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER =
        Logger.getLogger(DependencyRandomIndexing.class.getName());

    /**
     * A mapping from strings to {@code IntegerVector}s which represent an index
     * vector.
     */
    private Map<String, TernaryVector> indexMap;

    /**
     * The {@code PermutationFunction} to use for co-occurrances.
     */
    private final DependencyPermutationFunction<TernaryVector> permFunc;

    /**
     * A map that represents the word space by mapping raw strings to vectors.
     */
    private ConcurrentMap<String, IntegerVector> wordSpace;

    /**
     * The size of each index vector, as set when the sspace is created.
     */
    private final int vectorLength;

    /**
     * The {@link DependencyExtractor} being used for parsing corpora.
     */
    private final DependencyExtractor parser;

    /**
     * The {@link DependencyRelationAcceptor} to use for validating paths.
     */
    private final DependencyRelationAcceptor acceptor;

    /**
     * The maximum number of relations any path may have.
     */
    private final int pathLength;

    /**
     * An optional set of words that restricts the set of semantic vectors that
     * this instance will retain.
     */
    private Set<String> semanticFilter;

    /**
     * Creates a new instance of {@code DependencyRandomIndexing} that takes
     * ownership of a {@link DependencyExtractor} and uses the System provided
     * properties to specify other class objects.
     */
    public DependencyRandomIndexing(
            DependencyPermutationFunction<TernaryVector> permFunc) {
        this(permFunc, System.getProperties());
    }

    /**
     * Create a new instance of {@code DependencyRandomIndexing} which
     * takes ownership
     */
    public DependencyRandomIndexing(
            DependencyPermutationFunction<TernaryVector> permFunc,
            Properties properties) {
        this.permFunc = permFunc;
        this.parser = DependencyExtractorManager.getDefaultExtractor();

        // Load the vector length.
        String vectorLengthProp = 
            properties.getProperty(VECTOR_LENGTH_PROPERTY);
        vectorLength = (vectorLengthProp != null)
            ? Integer.parseInt(vectorLengthProp)
            : DEFAULT_VECTOR_LENGTH;

        // Load the maximum dependency path length.
        String pathLengthProp =
            properties.getProperty(DEPENDENCY_PATH_LENGTH_PROPERTY);
        pathLength = (pathLengthProp != null)
            ? Integer.parseInt(pathLengthProp)
            : DEFAULT_DEPENDENCY_PATH_LENGTH;

        // Load the path acceptor.
        String acceptorProp = 
            properties.getProperty(DEPENDENCY_ACCEPTOR_PROPERTY);
        acceptor = (acceptorProp != null)
            ? (DependencyRelationAcceptor) 
                ReflectionUtil.getObjectInstance(acceptorProp)
            : new UniversalRelationAcceptor();

        // Set up the generator vector maps.
        RandomIndexVectorGenerator indexVectorGenerator = 
            new RandomIndexVectorGenerator(vectorLength, properties);
        indexMap = new GeneratorMap<TernaryVector>(indexVectorGenerator);
        wordSpace = new ConcurrentHashMap<String,IntegerVector>();
        semanticFilter = new HashSet<String>();
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
    public Vector getVector(String term) {
        return Vectors.immutable(wordSpace.get(term));
    }

    public DependencyPermutationFunction<TernaryVector> getPermutations() {
        return permFunc;
    }

    public Map<String, TernaryVector> getWordToVectorMap() {
        return indexMap;
    }

    public void setWordToVectorMap(Map<String, TernaryVector> vectorMap) {
        indexMap = vectorMap;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return SSPACE_NAME + "-" + vectorLength;
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
    public void processDocument(BufferedReader document) throws IOException {
        // Iterate over all of the parseable dependency parsed sentences in the
        // document.
        for (DependencyTreeNode[] nodes = null;
                (nodes = parser.readNextTree(document)) != null; ) {

            // Skip empty documents.
            if (nodes.length == 0)
                continue;

            // Examine the paths for each word in the sentence.
            for (int i = 0; i < nodes.length; ++i) {
                String focusWord = nodes[i].word();

                // Skip words that are rejected by the semantic filter.
                if (!acceptWord(focusWord))
                    continue;

                // Acquire the semantic vector for the focus word.
                IntegerVector focusMeaning = getSemanticVector(focusWord);

                // Create the path iterator for all acceptable paths rooted at
                // the focus word in the sentence.
                Iterator<DependencyPath> pathIter = 
                    new DependencyIterator(nodes[i], acceptor, pathLength);

                // For every path, obtain the index vector of the last word in
                // the path and add it to the semantic vector for the focus
                // word.  The index vector is permuted if a permutation
                // function has been provided based on the contents of the path.
                while (pathIter.hasNext()) {
                    DependencyPath path = pathIter.next();
                    TernaryVector termVector = indexMap.get(path.last().word());
                    if (permFunc != null)
                        termVector = permFunc.permute(termVector, path);
                    add(focusMeaning, termVector);
                }
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
     * Assigns the word to {@link IntegerVector} mapping to be used by this
     * instance.  This instance takes ownership of the passed in map.
     *
     * @param m a mapping from token to the {@code IntegerVector} that should be
     *        used represent it when calculating other word's semantics
     */
    public void setWordToIndexVector(Map<String,TernaryVector> m) {
        indexMap = m;
    }

    /**
     * {@inheritDoc}.
     *
     * </p> Note that all words will still have an index vector assigned to
     * them, which is necessary to properly compute the semantics.
     *
     * @param semanticsToRetain the set of words for which semantics should be
     *        computed.
     */
    public void setSemanticFilter(Set<String> semanticsToRetain) {
        semanticFilter.clear();
        semanticFilter.addAll(semanticsToRetain);
    }

    /**
     * Returns true if there is no semantic filter list or the word is in the
     * filter list.
     */
    private boolean acceptWord(String word) {
        return semanticFilter.isEmpty() || semanticFilter.contains(word);
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

     /**
     * Returns the current semantic vector for the provided word.  If the word
     * is not currently in the semantic space, a vector is added for it and
     * returned.
     *
     * @param word a word that requires a semantic vector
     *
     * @return the {@code SemanticVector} representing {@code word}
     */
    private IntegerVector getSemanticVector(String word) {
        IntegerVector v = wordSpace.get(word);
        if (v == null) {
            // lock on the word in case multiple threads attempt to add it at
            // once
            synchronized(this) {
                // recheck in case another thread added it while we were waiting
                // for the lock
                v = wordSpace.get(word);
                if (v == null) {
                    v = new CompactSparseIntegerVector(vectorLength);
                    wordSpace.put(word, v);
                }
            }
        }
        return v;
    }
}
