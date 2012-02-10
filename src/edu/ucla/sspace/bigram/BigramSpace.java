package edu.ucla.sspace.bigram;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.matrix.AtomicGrowingSparseHashMatrix;
import edu.ucla.sspace.matrix.ChiSquaredTransform;
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
 * @author Keith Stevens
 */
public class BigramSpace implements SemanticSpace {

    private final BasisMapping<String, String> basis;

    private final AtomicGrowingSparseHashMatrix bigramMatrix;

    private final int windowSize;

    public BigramSpace() {
        this(new StringBasisMapping(), 8);
    }

    public BigramSpace(BasisMapping<String, String> basis, int windowSize) {
        this.basis = basis;
        this.windowSize = windowSize;
        bigramMatrix = new AtomicGrowingSparseHashMatrix();
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return bigramMatrix.columns();
    }

    public Set<String> getWords() {
        return basis.keySet();
    }

    public SparseDoubleVector getVector(String word) {
        int index = basis.getDimension(word);
        return (index < 0) ? null : bigramMatrix.getRowVector(index);
    }

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

    public void processSpace(Properties props) {
        Transform pmi = new FilteredTransform(
                new PointWiseMutualInformationTransform(),
                5.0, Double.MAX_VALUE);
                //new ChiSquaredTransform(), 3.841, Double.MAX_VALUE);
        pmi.transform(bigramMatrix, bigramMatrix);
        basis.setReadOnly(true);
    }
}
