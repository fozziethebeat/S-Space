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
import edu.ucla.sspace.matrix.AtomicGrowingSparseHashMatrix;
import edu.ucla.sspace.matrix.BaseTransform;
import edu.ucla.sspace.matrix.FilteredTransform;
import edu.ucla.sspace.matrix.PointWiseMutualInformationTransform;
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
 * @author Keith Stevens
 */
public class BigramSpace implements SemanticSpace {

    private final BasisMapping<String, String> basis;

    private final AtomicGrowingSparseHashMatrix bigramMatrix;

    private final int windowSize;

    private final Transform filter;

    public BigramSpace() {
        this(new StringBasisMapping(), 8,
             new PointWiseMutualInformationTransform(), 5);
    }

    public BigramSpace(BasisMapping<String, String> basis, 
                       int windowSize,
                       BaseTransform base,
                       double minValue) {
        this.basis = basis;
        this.windowSize = windowSize;
        this.filter = new FilteredTransform(base, minValue);
        this.bigramMatrix = new AtomicGrowingSparseHashMatrix();
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

        for (int i = 0; i < windowSize; ++i) {
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
