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

package edu.ucla.sspace.ri;

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceWriter;

import edu.ucla.sspace.hadoop.CooccurrenceExtractor;
import edu.ucla.sspace.hadoop.WordCooccurrence;
import edu.ucla.sspace.hadoop.WordCooccurrenceCountingJob;

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
import edu.ucla.sspace.vector.VectorMath;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*;

/**
 * An implementation of Random Indexing that uses Hadoop to perform a
 * distributed co-occurrence counting.  This class is designed to only be used
 * on systems where a Hadoop system is currently installed and running.
 * Furthermore, this class depends on all external resources and corpora being
 * present in the Hadoop distributed file system.
 *
 * <p> This class supports the following properties.
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
 * @see RandomIndexing
 * @see WordCooccurrenceCountingJob
 */
public class HadoopRandomIndexing {

    private static final Logger LOGGER = 
        Logger.getLogger(HadoopRandomIndexing.class.getName());

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.ri.HadoopRandomIndexing";

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
     * A mapping from each word to its associated index vector
     */
    private final Map<String,TernaryVector> wordToIndexVector;

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
     * Creates an instance of {@code HadoopRandomIndexing} using the system
     * properties for setting the configuratons.
     */
    public HadoopRandomIndexing() {
        this(System.getProperties());
    }

    /**
     * Creates an instance of {@code HadoopRandomIndexing} using the provided
     * properties for configuring the instance.
     */
    public HadoopRandomIndexing(Properties properties) {
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
        semanticFilter = new HashSet<String>();
    }


    /**
     * Creates an {@link IntegerVector} of the kind specified by the user.
     */
    private IntegerVector createSemanticVector() {
        IntegerVector v = (useSparseSemantics) 
            ? new CompactSparseIntegerVector(vectorLength)
            : new DenseIntVector(vectorLength);
        return v;
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
     * Computes the co-occurrences present in the documents in the input
     * directory and writes the {@link SemanticSpace} to the provided writer.
     *
     * @param inputDirs one or more a directories in the Hadoop file system that
     *        contain all the documents to process
     * @param writer a writer to which the output {@link SemanticSpace} will be
     *        written upon completion of the map-reduce analysis.
     *
     * @throws Exception if any exception occurs during the Hadoop processing or
     *         when writing the {@link SemanticSpace}
     */
    public void execute(Collection<String> inputDirs, 
                        SemanticSpaceWriter writer) throws Exception {
        // Set the window size property used the the Cooccurrence Mapper
        Properties props = System.getProperties();
        props.setProperty(CooccurrenceExtractor.WINDOW_SIZE_PROPERTY, 
                          String.valueOf(windowSize));
        LOGGER.info("Beginning Hadoop corpus processing");
        // Construct the counting job that will use Hadoop to count the
        // co-occurrences
        WordCooccurrenceCountingJob job = 
            new WordCooccurrenceCountingJob(props);
        Iterator<WordCooccurrence> occurrences = job.execute(inputDirs);
        LOGGER.info("Finished Hadoop corpus processing; calculating sspace");
        int wordCount =  0;
        // Local state variables for updating the current word's vector.
        String curWord = null;
        IntegerVector semantics = null;
        while (occurrences.hasNext()) {
            WordCooccurrence occ = occurrences.next();
            String word = occ.focusWord();
            // Base case for the first word seen
            if (curWord == null) {
                curWord = word;
                semantics = createSemanticVector();
            }
            // If we've seen a new word, write the previous word's vector
            else if (!curWord.equals(word)) {
                writer.write(curWord, semantics);
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format(
                        "processed word #%d: %s%n ", ++wordCount, curWord));
                }
                curWord = word;
                semantics = createSemanticVector();
            }

            // NOTE: because we are using a GeneratorMap, this call will create
            // a new index vector for the word if it didn't exist prior.
            TernaryVector indexVector = 
                wordToIndexVector.get(occ.relativeWord());

            if (usePermutations) {
                indexVector = 
                    permutationFunc.permute(indexVector, occ.getDistance());
            }
            // Scale the index vector by the number of times this occurrence
            // happened
            VectorMath.addWithScalars(
                semantics, 1, indexVector, occ.getCount());
        }
        writer.close();
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
}