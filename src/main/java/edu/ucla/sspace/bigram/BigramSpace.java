/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

package edu.ucla.sspace.bigram;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.matrix.BaseTransform;
import edu.ucla.sspace.matrix.FilteredTransform;
import edu.ucla.sspace.matrix.PointWiseMutualInformationTransform;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.Transform;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;


/**
 * A {@link BigramSpace} creates a vector for every word in a corpus that
 * represents the set of words that come <b>after</b> the row's word within some
 * fixed length window.  If a {@link Transform} is not provided, then the values
 * in each word's vector wil represent raw occurrance counts, otherwise the
 * given {@link Transform} will modify the strength of each bigram occurance.
 *
 * <p>
 *
 * The behavior of resulting model can be significantly modified by specifying a
 * particular {@link BasisMapping} to accept or reject particular words, the
 * window size, and the {@link Transform} applied to the initial bigram
 * co-occurrance counts.  Futhermore, by setting a threshold, particular bigrams
 * can be automatically dropped.
 *
 * @author Keith Stevens
 */
public class BigramSpace implements SemanticSpace {

    /**
     * The {@link BasisMapping} which decides the dimension given to each token.
     */
    private final BasisMapping<String, String> basis;

    /**
     * The occurance strength between discovered bigrams.
     */
    private final SparseMatrix bigramMatrix;

    /**
     * The maximum number of words that can separate a valid bigram.
     */
    private final int windowSize;

    /**
     * A {@link Transform} that alters the co-occurrence weight between two
     * words in a bigram.
     */
    private final Transform filter;

    /**
     * Creates a new default {@link BigramSpace} using a standard {@link
     * StringBasisMapping} and a {@link PointWiseMutualInformationTransform}.
     */
    public BigramSpace() {
        this(new StringBasisMapping(), 8,
             new PointWiseMutualInformationTransform(), 5);
    }

    /**
     * Creates a fully configured {@link BigramSpace}.
     *
     * @param basis A {@link BasisMapping} to decided the token to dimension
     *              mapping.
     * @param windowSize The maximum number of words that can go between a valid
     *                   bigram.
     * @param base A {@link BaseTransform} to alter the bigram weights.
     * @param minValue The minimum valid value for a bigram <b>after</b>
     *                 transforming the weights by the {@code base} transform.
     */
    public BigramSpace(BasisMapping<String, String> basis, 
                       int windowSize,
                       BaseTransform base,
                       double minValue) {
        this.basis = basis;
        this.windowSize = windowSize;
        this.filter = new FilteredTransform(base, minValue);
        this.bigramMatrix = new GrowingSparseMatrix();
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return bigramMatrix.columns();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return basis.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getVector(String word) {
        int index = basis.getDimension(word);
        return (index < 0) ? null : bigramMatrix.getRowVector(index);
    }

    /**
     * Returns "BigramSpace".
     */
    public String getSpaceName() {
        return "BigramSpace";
    }

    /**
     * {@inheritDoc}
     */
    public void  processDocument(BufferedReader document) throws IOException {
        Queue<String> bigramWindow = new ArrayDeque<String>();

        Iterator<String> documentTokens = IteratorFactory.tokenize(document);

        for (int i = 0; i < windowSize && documentTokens.hasNext(); ++i) {
            String word = documentTokens.next();
            int index = basis.getDimension(word);
            if (index >= 0)
                bigramWindow.offer(word);
        }

        while (!bigramWindow.isEmpty()) {
            if (documentTokens.hasNext()) {
                String word = documentTokens.next();
                int index = basis.getDimension(word);
                if (index >= 0)
                    bigramWindow.offer(word);
            }

            String term = bigramWindow.remove();
            int index1 = basis.getDimension(term);
            if (index1 < 0)
                continue;

            for (String other : bigramWindow) {
                int index2 = basis.getDimension(other);
                if (index2 < 0)
                    continue;
                bigramMatrix.add(index1, index2, 1.0);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties props) {
        filter.transform(bigramMatrix, bigramMatrix);
        basis.setReadOnly(true);
    }
}
