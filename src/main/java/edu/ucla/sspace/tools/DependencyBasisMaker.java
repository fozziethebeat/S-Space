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


package edu.ucla.sspace.tools;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractorManager;
import edu.ucla.sspace.dependency.DependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.dependency.FlatPathWeight;
import edu.ucla.sspace.dependency.UniversalPathAcceptor ;

import edu.ucla.sspace.mains.DependencyGenericMain;

import edu.ucla.sspace.matrix.AtomicGrowingSparseHashMatrix;
import edu.ucla.sspace.matrix.NoTransform;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.BoundedSortedMap;
import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * This main creates a {@link BasisMapping} based on the unique terms found in a
 * document set and serializes it to disk.
 *
 * @author Keith Stevens
 */
public class DependencyBasisMaker extends DependencyGenericMain {

    /**
     * {@inheritDoc}
     */
    public void addExtraOptions(ArgOptions options) { 
        options.addOption('b', "basisSize",
                          "Specifies the total desired size of the basis " +
                          "(Default: 10000)",
                          true, "INT", "Optional");
        options.addOption('a', "pathAcceptor",
                          "Specifies the dependency path acceptor to use. " +
                          "(Default:    UnivseralPathAcceptor)",
                          true, "CLASSNAME", "Optional");
        options.addOption('w', "pathWeighter",
                          "Specifies the dependency path weighter to use. " +
                          "(Default:    FlatPathWeight)",
                          true, "CLASSNAME", "Optional");
        options.addOption('l', "pathLength",
                          "Specifies the maximum dependency path length. " +
                          "(Default:    5)",
                          true, "INT", "Optional");
    }

    /**
     * {@inheritDoc}
     */
    protected SemanticSpace getSpace() {
        setupDependencyExtractor();

        int bound = argOptions.getIntOption('b', 10000);
        Transform transform = argOptions.getObjectOption(
                'T', new NoTransform());
        DependencyPathAcceptor acceptor = argOptions.getObjectOption(
                'a', new UniversalPathAcceptor());
        DependencyPathWeight weighter = argOptions.getObjectOption(
                'w', new FlatPathWeight());
        int pathLength = argOptions.getIntOption('l', 5);
        return new OccurrenceCounter(
                transform, bound, acceptor, weighter, pathLength);
    }

    /**
     * Saves the {@link BasisMapping} created from the {@link
     * OccurrenceCounter}.
     */
    protected void saveSSpace(SemanticSpace sspace, File outputFile)
            throws IOException{
        BasisMapping<String, String> savedTerms = new StringBasisMapping();
        for (String term : sspace.getWords())
            savedTerms.getDimension(term);

        ObjectOutputStream ouStream = new ObjectOutputStream(
                new FileOutputStream(outputFile));
        ouStream.writeObject(savedTerms);
        ouStream.close();
    }

    /**
     * A simple term {@link SemanticSpace} implementation that counts word
     * co-occurrences, performs a transform, and then scores each recorded basis
     * dimension based on the row summed scores for each word.
     */
    public class OccurrenceCounter implements SemanticSpace {

        /**
         * The matrix used for storing weight co-occurrence statistics of those
         * words that occur both before and after.
         */
        private final AtomicGrowingSparseHashMatrix cooccurrenceMatrix;

        /**
         * The {@link BasisMapping} used to record dimensions.
         */
        private final BasisMapping<String, String> basis;

        /**
         * The final scores for each word in the {@code basis}.
         */
        private final Map<String, Double> wordScores;

        /**
         * The {@link Transform} class used to rescore each word.
         */
        private final Transform transform;

        /**
         * The {@link DependencyPathAcceptor} used to accept or reject
         * dependency paths.
         */
        private final DependencyPathAcceptor acceptor;

        /**
         * The {@link DependencyPathWeight} used to score dependency paths.
         */
        private final DependencyPathWeight weighter;

        /**
         * The maximum path length that is acceptable.
         */
        private final int pathLength;

        /**
         * The {@link DependencyExtractor} used to extract parse trees from each
         * document.
         */
        private final DependencyExtractor extractor;

        /**
         * Creates a new {@link OccurrenceCounter}.
         */
        public OccurrenceCounter(Transform transform,
                                 int bound, 
                                 DependencyPathAcceptor acceptor,
                                 DependencyPathWeight weighter,
                                 int pathLength) {
            cooccurrenceMatrix = new AtomicGrowingSparseHashMatrix();
            basis = new StringBasisMapping();
            wordScores = new BoundedSortedMap<String, Double>(bound);
            extractor = DependencyExtractorManager.getDefaultExtractor();

            this.transform = transform;
            this.acceptor = acceptor;
            this.weighter = weighter;
            this.pathLength = pathLength;
        }

        /**
         * {@inheritDoc}
         */
        public void processDocument(BufferedReader document)
                throws IOException {
            // Rather than updating the matrix every time an occurrence is
            // seen, keep a thread-local count of what needs to be modified
            // in the matrix and update after the document has been
            // processed.  This saves potential contention from concurrent
            // writes.
            Map<Pair<Integer>,Double> matrixEntryToCount = 
                    new HashMap<Pair<Integer>,Double>();

            // Iterate over all of the parseable dependency parsed sentences in
            // the document.
            for (DependencyTreeNode[] nodes = null; 
                    (nodes = extractor.readNextTree(document)) != null; ) {

                // Skip empty documents.
                if (nodes.length == 0)
                    continue;                        

                // Examine the paths for each word in the sentence.
                for (int wordIndex = 0; wordIndex < nodes.length; ++wordIndex) {
                    String focusWord = nodes[wordIndex].word();                            
                    int focusIndex = basis.getDimension(focusWord);

                    // Get all the valid paths starting from this word.    The
                    // acceptor will filter out any paths that don't contain the
                    // semantic connections we're looking for.
                    Iterator<DependencyPath> paths =
                        new FilteredDependencyIterator(
                                nodes[wordIndex], acceptor, pathLength);
                            
                    // For each of the paths rooted at the focus word, update
                    // the co-occurrences of the focus word in the dimension
                    // that the BasisFunction states.
                    while (paths.hasNext()) {
                        DependencyPath path = paths.next();

                        String occurrence = path.last().word();
                        int featureIndex = basis.getDimension(occurrence);

                        double score = weighter.scorePath(path);
                        matrixEntryToCount.put(new Pair<Integer>(
                                    focusIndex, featureIndex), score);
                    }
                }
            }

            // Once the document has been processed, update the co-occurrence
            // matrix accordingly.
            for (Map.Entry<Pair<Integer>,Double> e :
                    matrixEntryToCount.entrySet()){
                    Pair<Integer> p = e.getKey();
                    cooccurrenceMatrix.addAndGet(p.x, p.y, e.getValue());
            }
        }

        /**
         * {@inheritDoc}
         */
        public Set<String> getWords() {
            return Collections.unmodifiableSet(wordScores.keySet());
        }

        /**
         * {@inheritDoc}
         */
        public DoubleVector getVector(String word) {
            Double score = wordScores.get(word);
            return (score == null)
                ? new DenseVector(new double[] {0})
                : new DenseVector(new double[] {score});
        }

        /**
         * {@inheritDoc}
         */
        public int getVectorLength() {
            return 1;
        }

        /**
         * {@inheritDoc}
         */
        public void processSpace(Properties properties) {
            SparseMatrix cleanedMatrix = (SparseMatrix) transform.transform(
                    cooccurrenceMatrix);
            for (String term : basis.keySet()) {
                int index = basis.getDimension(term);
                SparseDoubleVector sdv = cleanedMatrix.getRowVector(index);

                double score = 0;
                for (int i : sdv.getNonZeroIndices())
                    score += sdv.get(i);

                wordScores.put(term, score);
            }
        }

        /**
         * {@inheritDoc}
         */
        public String getSpaceName() {
            return "BasisMaker";
        }
    }
}
