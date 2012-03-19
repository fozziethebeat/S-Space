/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPermutationFunction;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.TernaryVector;

import java.util.Iterator;
import java.util.Map;

/**
 * A {@link DependencyContextGenerator} that forms context vectors using a
 * summation of index vectors.  Each index vector is fixed {@link TernaryVector}
 * corresponding to each term in the corpus.  Only index vectors for terms
 * reachable by a valid {@link DependencyPath} starting from the focus wordwill
 * be applied to a particular context.
 *
 * @author Keith Stevens
 */
public class RandomIndexingDependencyContextGenerator
        implements DependencyContextGenerator {

    /**
     * The {@link DependencyPermutationFunction} that permutes {@link
     * TernaryVector}.
     */
    private final DependencyPermutationFunction<TernaryVector> permFunc;

    /**
     * A mapping from word forms to their index vectors.
     */
    private final Map<String, TernaryVector> indexMap;

    /**
     * The length of each index vector.
     */
    private final int indexVectorLength;

    /**
     * The maximum valid length.
     */
    private final int pathLength;

    /**
     * The filter that accepts only dependency paths that match predefined
     * criteria.
     */
    private final DependencyPathAcceptor acceptor;

    /**
     * Set to true when new words should not be auto assigned an index vector.
     */
    private boolean readOnly;

    /**
     * Creates a new {@link RandomIndexingDependencyContextGenerator}.
     *
     * @param permFunc the {@link DependencyPermutationFunction} responsible for
     *        permuting {@link TernaryVector}, which serve as index vectors.
     * @param acceptor The {@link DependencyPathAcceptor} used to validate
     *        {@link DependencyPath}s stemming from focus word.
     * @param indexMap A mapping from word forms to {@link TernaryVector}s.
     * @param indexVectorLength The length of each index vector.
     * @param pathLength The maximum acceptable length of any {@link
     *        DependencyPath}.
     */
    public RandomIndexingDependencyContextGenerator(
                    DependencyPermutationFunction<TernaryVector> permFunc,
                    DependencyPathAcceptor acceptor,
                    Map<String, TernaryVector> indexMap,
                    int indexVectorLength,
                    int pathLength) {
            this.permFunc = permFunc;
            this.acceptor = acceptor;
            this.indexMap = indexMap;
            this.indexVectorLength = indexVectorLength;
            this.pathLength = pathLength;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector generateContext(DependencyTreeNode[] tree,
                                              int focusIndex) {
        DependencyTreeNode focusNode = tree[focusIndex];

        SparseDoubleVector meaning = new CompactSparseVector(indexVectorLength);

        Iterator<DependencyPath> paths = new FilteredDependencyIterator(
                focusNode, acceptor, pathLength);

        while (paths.hasNext()) {
            DependencyPath path = paths.next();
            if (readOnly && !indexMap.containsKey(path.last().word()))
                continue;

            TernaryVector termVector = indexMap.get(path.last().word());
            if (permFunc != null)
                termVector = permFunc.permute(termVector, path);
            add(meaning, termVector);
        }
        return meaning;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return indexVectorLength;
    }

    /**
     * {@inheritDoc}
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Adds a {@link TernaryVector} to a {@link IntegerVector}
     */
    private void add(SparseDoubleVector dest, TernaryVector src) {
        for (int p : src.positiveDimensions())
            dest.add(p, 1);
        for (int n : src.negativeDimensions())
            dest.add(n, -1);
    }
}
