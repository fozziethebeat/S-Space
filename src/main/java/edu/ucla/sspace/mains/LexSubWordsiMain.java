package edu.ucla.sspace.mains;

import edu.ucla.sspace.basis.BasisMapping;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.common.StaticSemanticSpace;

import edu.ucla.sspace.hal.LinearWeighting;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.CorpusReader;
import edu.ucla.sspace.text.corpora.SemEvalLexSubReader;

import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.NearestNeighborFinder;
import edu.ucla.sspace.util.SerializableUtil;
import edu.ucla.sspace.util.SimpleNearestNeighborFinder;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;

import edu.ucla.sspace.wordsi.ContextExtractor;
import edu.ucla.sspace.wordsi.ContextGenerator;
import edu.ucla.sspace.wordsi.Wordsi;
import edu.ucla.sspace.wordsi.WordOccrrenceContextGenerator;
import edu.ucla.sspace.wordsi.semeval.SemEvalContextExtractor;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Iterator;


/**
 * @author Keith Stevens
 */
public class LexSubWordsiMain {

    public static void main(String[] args) {
        System.err.println("Loading wordsi.");
        Wordsi wordsi = new LexSubWordsi(args[3], args[0]);

        System.err.println("Loading basis mapping and extractor.");
        BasisMapping<String, String> basis = 
            SerializableUtil.load(new File(args[2]));
        basis.setReadOnly(true);
        ContextGenerator generator =
            new WordOccrrenceContextGenerator(basis, new LinearWeighting(), 25);
        ContextExtractor extractor =
            new SemEvalContextExtractor(generator, 25);

        System.out.println("Processing contexts");
        CorpusReader<Document> reader = new SemEvalLexSubReader();
        Iterator<Document> docIter = reader.read(new File(args[1]));
        while (docIter.hasNext())
            extractor.processDocument(docIter.next().reader(), wordsi);
    }

    public static class LexSubWordsi implements Wordsi {
        private final NearestNeighborFinder comparator;

        private final PrintWriter output;

        private final SemanticSpace wordsiSpace;

        public LexSubWordsi(String outFile, String sspaceFile) {
            try {
                output = new PrintWriter(outFile);
                wordsiSpace = new StaticSemanticSpace(sspaceFile);
                comparator = new SimpleNearestNeighborFinder(wordsiSpace);
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        public boolean acceptWord(String focus) {
            return true;
        }

        public void handleContextVector(String focus,
                                        String secondary,
                                        SparseDoubleVector vector) {
            secondary = secondary.replaceAll("_", " ");
            System.err.printf("Processing %s\n", secondary);
            String bestSense = getBaseSense(focus, vector);
            if (bestSense == null)
                return;            
            MultiMap<Double, String> topWords = comparator.getMostSimilar(
                    bestSense, 10);
            output.printf("%s ::", secondary);
            for (String term : topWords.values())
                output.printf(" %s", term);
            output.println();
        }

        public String getBaseSense(String focus, SparseDoubleVector vector) {
            int i = 0;
            String bestSense = null;
            double bestSim = 0;
            while (true) {
                String query = (i == 0) ? focus : focus + "-" + i;
                i++;

                Vector v = wordsiSpace.getVector(query);
                if (v == null)
                    return bestSense;

                double sim = Similarity.cosineSimilarity(v, vector);
                if (sim >= bestSim) {
                    bestSim = sim;
                    bestSense = query;
                }
            }
        }
    }
}
