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

package edu.ucla.sspace.beagle;

import jnt.FFT.ComplexDoubleFFT_Radix2; 

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.fft.FastFourierTransform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.VectorMath;
import edu.ucla.sspace.vector.VectorMath;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.logging.Logger;


/**
 * An implementation of the Beagle Semantic Space model. This implementation is
 * based on <p style="font-family:Garamond, Georgia, serif">Jones, M. N.,
 * Mewhort, D.  J.L. (2007).    Representing Word Meaning and Order Information
 * in a Composite Holographic Lexicon.    <i>Psychological Review</i>
 * <b>114</b>, 1-37.  Available <a
 * href="www.indiana.edu/~clcl/BEAGLE/Jones_Mewhort_PR.pdf">here</a></p>
 *
 * For every word, a unique random index vector is created, where the vector has
 * some large dimension (by default 512), with each entry in the vector being
 * from a random gaussian distribution. The holographic meaning of a word is
 * updated by first adding the sum of index vectors for all the words in a
 * sliding window centered around the target term. Additionally a sum of
 * convolutions of several n-grams is added to the holographic meaning. The
 * main functionality of this class can be found in the {@link IndexBuilder}
 * class.
 *
 * @author Keith Stevens
 */
public class Beagle implements SemanticSpace {

    public enum SemanticType {
        CONTEXT,
        ORDERING,
        COMPOSITE,
    };

    /**
     * The full context size used when scanning the corpus. This is the
     * total number of words considered in the context.
     */
    public static final int CONTEXT_SIZE = 6;

    /**
     * The Semantic Space name for Beagle
     */
    public static final String BEAGLE_SSPACE_NAME = 
        "beagle-semantic-space";

    /**
     * Logger for Beagle. 
     */
    private static final Logger LOGGER =
        Logger.getLogger(Beagle.class.getName());

    /**
     * The class responsible for creating index vectors, and incorporating them
     * into a semantic vector.
     */
    private final Map<String, DoubleVector> vectorMap;

    /**
     * A mapping for terms to their semantic vector representation. A {@code
     * DoubleVector} is used as these representations may be large.
     */
    private final ConcurrentMap<String, DoubleVector> termHolographs;

    /**
     * The size of each index vector, as set when the sspace is created.
     */
    private final int indexVectorSize;

    /**
     * The number of words in the context to save prior to the focus word.
     */
    private int prevSize;

    /**
     * The number of words in the context to save after the focus word.
     */
    private int nextSize;

    /**
     * An empty place holder vector to represent the focus word when computing
     * the circular convolution.
     */
    private DoubleVector placeHolder;

    /**
     * The first permutation ordering for vectors.
     */
    private int[] permute1;

    /**
     * The second permutation ordering for vectors.
     */
    private int[] permute2;

    private final SemanticType semanticType;

    public Beagle(int vectorSize, Map<String, DoubleVector> vectorMap) {
        this(vectorSize, SemanticType.COMPOSITE, vectorMap);
    }

    public Beagle(int vectorSize,
                  SemanticType semanticType,
                  Map<String, DoubleVector> vectorMap) {
        this.indexVectorSize = vectorSize;
        this.vectorMap = vectorMap;
        termHolographs = new ConcurrentHashMap<String, DoubleVector>();
        this.semanticType = semanticType;

        placeHolder = vectorMap.get("");

        // Generate the permutation arrays.
        permute1 = new int[indexVectorSize];
        permute2 = new int[indexVectorSize];
        randomPermute(permute1);
        randomPermute(permute2);

        prevSize = 1;
        nextSize = 5;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return termHolographs.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getVector(String term) {
        return Vectors.immutable(termHolographs.get(term));
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return BEAGLE_SSPACE_NAME + "-" +
               indexVectorSize + "-" +
               semanticType.toString();
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return indexVectorSize;
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        Queue<String> prevWords = new ArrayDeque<String>();
        Queue<String> nextWords = new ArrayDeque<String>();

        Iterator<String> it = IteratorFactory.tokenizeOrdered(document);
        Map<String, DoubleVector> documentVectors =
            new HashMap<String, DoubleVector>();

        // Fill up the words after the context so that when the real processing
        // starts, the context is fully prepared.
        for (int i = 0 ; i < nextSize && it.hasNext(); ++i)
            nextWords.offer(it.next().intern());
        prevWords.offer(IteratorFactory.EMPTY_TOKEN);

        String focusWord = null;
        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();

            if (it.hasNext())
                nextWords.offer(it.next().intern());

            if (!focusWord.equals(IteratorFactory.EMPTY_TOKEN)) {
                // Incorporate the context into the semantic vector for the
                // focus word.  If the focus word has no semantic vector yet,
                // create a new one, as determined by the index builder.
                DoubleVector meaning = termHolographs.get(focusWord);
                if (meaning == null) {
                    meaning = new DenseVector(indexVectorSize);
                    documentVectors.put(focusWord, meaning);
                }
                updateMeaning(meaning, prevWords, nextWords);
            }

            prevWords.offer(focusWord);
            if (prevWords.size() > 1)
                prevWords.remove();
        }

        // Add the local cached semantics to the global term semantics.
        for (Map.Entry<String, DoubleVector> entry :
                documentVectors.entrySet()) {
            synchronized (entry.getKey()) {
                // Get the global semantic representation of each word.  If it
                // does not currently exist, then just put the local copies
                // representation, otherwise add the local copy to the global
                // version.
                DoubleVector existingVector =
                    termHolographs.get(entry.getKey());
                if (existingVector == null)
                    termHolographs.put(entry.getKey(), entry.getValue());
                else
                    VectorMath.add(existingVector, entry.getValue());
            }
        }
    }
    
    /**
     * No processing is performed on the holographs.
     */
    public void processSpace(Properties properties) {
    }

    /**
     * Adds a holograph encoding the co-occurance information, and the
     * ordering information of the given context.  {@code termVector} will be
     * added to the result {@code DoubleVector}, and then the convolution of any
     * prior convoluted n-grams will be convoluted with the given {@code
     * termVector} and added to the result.  When {@code focusVector} changes to
     * be a different term, new n-gram convolutions are generated which use a
     * placeholder in place of {@code focusVector}.
     */
    private void updateMeaning(DoubleVector meaning,
                               Queue<String> prevWords,
                               Queue<String> nextWords) {
        // Generate the semantics of the context using summation of index
        // vectors.
        if (semanticType == SemanticType.COMPOSITE ||
            semanticType == SemanticType.CONTEXT) {
            DoubleVector context = new DenseVector(indexVectorSize);

            // Sum the words prior to the focus word, skipping filtered tokens.
            for (String term: prevWords) {
                if (term.equals(IteratorFactory.EMPTY_TOKEN))
                    continue;
                VectorMath.add(context, vectorMap.get(term));
            }

            // Sum the words after the focus word, skipping filtered tokens.
            for (String term: nextWords) {
                if (term.equals(IteratorFactory.EMPTY_TOKEN))
                    continue;
                VectorMath.add(context, vectorMap.get(term));
            }

            // Normalize the context vector and add it to the meaning.
            normalize(context);
            VectorMath.add(meaning, context);
        }

        // Generate the semantics of the ordering using circular convolution of
        // n-grams.
        if (semanticType == SemanticType.COMPOSITE ||
            semanticType == SemanticType.ORDERING) {
            DoubleVector order = groupConvolution(prevWords, nextWords);

            // Normalize the order vector and add it to the meaning.
            normalize(order);
            VectorMath.add(meaning, order);
        }
    }

    /**
     * Performs l2-normalization on the vector in place.  If the magnitude of
     * the vector is 0, the values are left unchanged.
     */
    private void normalize(DoubleVector v) {
        double magnitude = 0;
        for (int i = 0; i < v.length(); ++i)
            magnitude += Math.pow(v.get(i), 2);
        if (magnitude == 0)
            return;

        magnitude = Math.sqrt(magnitude);
        for (int i = 0; i < v.length(); ++i)
            v.set(i, v.get(i)/magnitude);
    }

    /**
     * Generate the circular convoltion of n-grams composed of words in the
     * given context. The result of this convolution is returned as a
     * DoubleVector.
     *
     * @param prevWords The words prior to the focus word in the context.
     * @param nextWords The Words after the focus word in the context.
     * 
     * @return The semantic vector generated from the circular convolution.
     */
    private DoubleVector groupConvolution(Queue<String> prevWords,
                                          Queue<String> nextWords) {
        // Generate an empty DoubleVector to hold the convolution.
        DoubleVector result = new DenseVector(indexVectorSize);

        // Do the convolutions starting at index 0.
        String prevWord = prevWords.peek();
        DoubleVector tempConvolution;
        if (!prevWord.equals(IteratorFactory.EMPTY_TOKEN)) {
            tempConvolution =
                convolute(vectorMap.get(prevWords.peek()), placeHolder);
            VectorMath.add(result, tempConvolution);
        } else
            tempConvolution = placeHolder;


        for (String term : nextWords) {
            if (term.equals(IteratorFactory.EMPTY_TOKEN))
                continue;

            tempConvolution = convolute(tempConvolution, vectorMap.get(term));
            VectorMath.add(result, tempConvolution);
        }

        tempConvolution = placeHolder;

        // Do the convolutions starting at index 1.
        for (String term : nextWords) {
            if (term.equals(IteratorFactory.EMPTY_TOKEN))
                continue;

            tempConvolution = convolute(tempConvolution, vectorMap.get(term));
            VectorMath.add(result, tempConvolution);
        }
        return result;
    }

    /**
     * Populates the given array with values 0 to {@code indexVectorSize}, and
     * then shuffly the values randomly.
     */
    private void randomPermute(int[] permute) {
        for (int i = 0; i < indexVectorSize; i++)
            permute[i] = i;
        for (int i = indexVectorSize - 1; i > 0; i--) {
            int w = (int) Math.floor(Math.random() * (i+1));
            int temp = permute[w];
            permute[w] = permute[i];
            permute[i] = permute[w];
        }
    }

    private DoubleVector convolute(DoubleVector left, DoubleVector right) {
        // Permute both vectors.
        left = changeVector(left, permute1);
        right = changeVector(right, permute2);

        // Use the Fast Fourier Transform on each vector.
        FastFourierTransform.transform(left);
        FastFourierTransform.transform(right);

        // Multiply the two together.
        DoubleVector result = VectorMath.multiply(left, right);

        // The inverse transform completes the convolution.
        FastFourierTransform.backtransform(result);
        return result;
    }

    /**
     * Shuffle the given vector based on the ordering information given in
     * {@code orderVector}.
     *
     * @param data The vector to be shuffled.
     * @param orderVector The ordering of values to be used.
     * 
     * @return The shuffled version of {@code data}.
     */
    private DoubleVector changeVector(DoubleVector data, int[] orderVector) {
        DoubleVector result = new DenseVector(indexVectorSize);
        for (int i = 0; i < indexVectorSize; i++)
            result.set(i, data.get(orderVector[i]));
        return result;
    }
}
