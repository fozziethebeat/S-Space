package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.FilteredStringBasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.matrix.CellMaskedSparseMatrix;
import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SparseSymmetricMatrix;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.text.DependencyFileDocumentIterator;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.util.SerializableUtil;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Properties;


/**
 * @author Keith Stevens
 */
public class GraphWordsi implements SemanticSpace {

    private final BasisMapping<String, String> basis;

    private final DependencyExtractor extractor;

    private final Map<String, Double> referenceLikilhood;

    private final SparseMatrix termGraph;

    private final String matrixFileName;
    private final String basisFileName;

    public GraphWordsi(BasisMapping<String, String> basis,
                       DependencyExtractor extractor,
                       Map<String, Double> referenceLikilhood,
                       String outName) {
        this.basis = basis;
        this.extractor = extractor;
        this.referenceLikilhood = referenceLikilhood;
        this.termGraph = new SparseSymmetricMatrix(new GrowingSparseMatrix());
        this.matrixFileName = outName + ".mat";
        this.basisFileName = outName + ".basis";
    }

    public void processDocument(BufferedReader reader) throws IOException {
        // Handle the context header, if one exists.  Context headers are
        // assumed to be the first line in a document.
        String header = reader.readLine();

        // Iterate over all of the parseable dependency parsed sentences in
        // the document.
        DependencyTreeNode[] nodes = extractor.readNextTree(reader);

        // Skip empty documents.
        if (nodes.length == 0)
            return;

        // Examine the paths for each word in the sentence.
        for (int wordIndex = 0; wordIndex < nodes.length; ++wordIndex) {
            DependencyTreeNode focusNode = nodes[wordIndex];

            // Get the focus word, i.e., the primary key, and the
            // secondary key.  These steps are made as protected methods
            // so that the SenseEvalDependencyContextExtractor
            // PseudoWordDependencyContextExtractor can manage only the
            // keys, instead of the document traversal.
            String focusWord = focusNode.word();
            String secondarykey = focusNode.lemma();

            // Ignore any focus words that are unaccepted by Wordsi.
            if (!secondarykey.equals(header))
                continue;

            for (int i = 0; i < nodes.length; ++i) {
                if (i == wordIndex ||
                    !nodes[i].pos().startsWith("N"))
                    continue;

                int index1 = basis.getDimension(nodes[i].word());
                if (index1 < 0)
                    continue;
                for (int j = i+1; j < nodes.length; ++j) {
                    if (i == wordIndex ||
                        !nodes[j].pos().startsWith("N"))
                        continue;

                    int index2 = basis.getDimension(nodes[j].word());
                    if (index2 < 0)
                        continue;

                    termGraph.add(index1, index2, 1);
                }
            }
        }
    }

    public void processSpace(Properties props) {
        try {
            StringBasisMapping finalBasis = new StringBasisMapping();

            double[] termFrequency = new double[termGraph.rows()];
            double total = 0;
            for (int r = 0; r < termGraph.rows(); ++r)
                for (int c = r+1; c < termGraph.columns(); ++c) {
                    termFrequency[r] += termGraph.get(r,c);
                    total += termGraph.get(r,c);
                }

            int savedRows = 0;
            List<Integer> rowMapList = new ArrayList<Integer>();
            for (int r = 0; r < termGraph.rows(); ++r) {
                String word = basis.getDimensionDescription(r);
                Double l = referenceLikilhood.get(word);
                double reference = (l == null) ? .0000000001 : l;
                double logLikelihood = -2 * Math.log(reference/(termFrequency[r] / total));
                if (termFrequency[r] >= 10 && logLikelihood >= 0) {
                    rowMapList.add(r);
                    finalBasis.getDimension(word);
                }
            }

            int[] rowMap = new int[rowMapList.size()];
            for (int i = 0; i < rowMap.length; ++i)
                rowMap[i] = rowMapList.get(i);

            Matrix maskedMatrix = new CellMaskedSparseMatrix(
                    termGraph, rowMap, rowMap);

            MatrixIO.writeMatrix(maskedMatrix,
                                 new File(matrixFileName),
                                 Format.SVDLIBC_SPARSE_TEXT);
            SerializableUtil.save(finalBasis, basisFileName);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public String getSpaceName() {
        return null;
    }

    public int getVectorLength() {
        return 0;
    }

    public Vector getVector(String word) {
        return null;
    }

    public Set<String> getWords() {
        return null;
    }
}
