/*
 * Copyright 2012 David Jurgens
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

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.common.statistics.*;

import edu.ucla.sspace.clustering.Assignment;

import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.Graphs;
import edu.ucla.sspace.graph.LinkClustering;
import edu.ucla.sspace.graph.WeightedEdge;
import edu.ucla.sspace.graph.WeightedGraph;
import edu.ucla.sspace.graph.SparseWeightedGraph;
import edu.ucla.sspace.graph.SimpleWeightedEdge;

import edu.ucla.sspace.text.Corpus;
import edu.ucla.sspace.text.StringUtils;
import edu.ucla.sspace.text.WordIterator;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.text.Sentence;
import edu.ucla.sspace.text.Token;

import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.HashIndexer;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.Indexer;
import edu.ucla.sspace.util.LineReader;
import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.ObjectCounter;
import edu.ucla.sspace.util.ObjectIndexer;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SortedMultiMap;
import edu.ucla.sspace.util.TreeMultiMap;
import edu.ucla.sspace.util.WorkQueue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import static edu.ucla.sspace.util.LoggerUtil.info;
import static edu.ucla.sspace.util.LoggerUtil.verbose;
import static edu.ucla.sspace.util.LoggerUtil.veryVerbose;

public class IterativeBigramExtractor {

    private static final Logger LOGGER = 
        Logger.getLogger(IterativeBigramExtractor.class.getName());


    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();

        options.addOption('f', "fileList", "a list of document files", 
                          true, "FILE[,FILE...]", "Required (at least one of)");
        options.addOption('d', "docFile", 
                          "a file where each line is a document", true,
                          "FILE[,FILE...]", "Required (at least one of)");

        options.addOption('s', "stopWords", "A file containing a list of stop "+
                          "words that should be encluded from bigrams",
                          true, "FILE", "Program Options");


        options.addOption('h', "help", "Generates a help message and exits",
                          false, null, "Program Options");
        options.addOption('v', "verbose", "Turns on verbose output",
                          false, null, "Program Options");
        options.addOption('V', "veryVerbose", "Turns on *very* verbose output",
                          false, null, "Program Options");

        options.addOption('n', "numberOfTermsPerIteration", "Specifies the " +
                          "number of terms to compute the association between "+
                          "per iteration (default: all)",
                          true, "INT", "Runtime Options");        
        options.addOption('F', "filterAssociationBelow", "Specifies the " +
                          "an association score below which the pair will " +
                          "not be reported",
                          true, "DOUBLE", "Runtime Options");        

        options.parseOptions(args);
        
        // Set the verbosity
        if (options.hasOption('v'))
            LoggerUtil.setLevel(Level.FINE);
        if (options.hasOption('V'))
            LoggerUtil.setLevel(Level.FINER);
        if (options.numPositionalArgs() < 3 || options.hasOption("help")) {
            usage(options);
            return;
        }

        File termsFile = new File(options.getPositionalArg(0));
        String outputPrefix = options.getPositionalArg(1);

        Set<String> terms = StringUtils.loadFileAsSet(termsFile);

        Set<String> stopWords = null;
        if (options.hasOption('s')) {
            stopWords = StringUtils.loadFileAsSet(
                new File(options.getStringOption('s')));
        }

        // A mapping to the minimum weight for a test, or null if all the test's
        // scores should be reported
        Map<SignificanceTest,Double> tests = 
            new HashMap<SignificanceTest,Double>();
        Map<SignificanceTest,PrintWriter> testWriters = 
            new HashMap<SignificanceTest,PrintWriter>();

        int numArgs = options.numPositionalArgs();
        for (int i = 2; i < numArgs; ++i) {
            String testName = options.getPositionalArg(i);
            SignificanceTest test = getTest(testName);
            Double minWeight = null;
            if (i+1 < numArgs) {
                // This might be a test name
                String weightStr = options.getPositionalArg(i+1);

                try {
                    minWeight = Double.parseDouble(weightStr);
                } catch (NumberFormatException nfe) { }
                i++;
            }
            tests.put(test, minWeight);
            PrintWriter pw = new PrintWriter(outputPrefix + testName + ".txt");
            testWriters.put(test, pw);
        }

        int termsToUsePerIteration = (options.hasOption('n'))
            ? options.getIntOption('n')
            : terms.size();               

        Queue<String> termsToAssociate = new ArrayDeque<String>(terms);
        int round = 0;
        while (termsToAssociate.size() > 0) {
            round++;

            Counter<String> termCounts = new ObjectCounter<String>();
            Counter<Pair<String>> bigramCounts = 
                new ObjectCounter<Pair<String>>();
            int allBigramCounts = 0;

            // Load the next set of terms to test for being bigrams
            Set<String> curTerms = new HashSet<String>();
            while (curTerms.size() < termsToUsePerIteration 
                   && !termsToAssociate.isEmpty()) {
                curTerms.add(termsToAssociate.poll());
            }

            info(LOGGER, "Finding associations between all %d terms and a %d " +
                 "term subset (%d remain)", terms.size(), curTerms.size(),
                 termsToAssociate.size());
       
            int docNum = 0;
            long startTime = System.currentTimeMillis();
            Iterator<Corpus> iter = getDocuments(options);
            while (iter.hasNext()) {
                Corpus c = iter.next();
                for (Document doc : c) {
                    for (Sentence sent : doc) {
                        Iterator<Token> tokens = sent.iterator();
                        String t1 = null;
                        if (tokens.hasNext())
                            t1 = tokens.next().text();

                        while (tokens.hasNext()) {
                            String t2 = tokens.next().text();

                            // Count the occurrence of this token if we're
                            // supposed to record it
                            if (terms.contains(t2)
                                && (stopWords == null || !stopWords.contains(t2))) {

                                termCounts.count(t2);
                                if (t1 != null) {
                                    allBigramCounts++;
                                    // See if we are supposed to record this bigram
                                    if (curTerms.contains(t1))
                                        bigramCounts.count(new Pair<String>(t1, t2));
                                }
                            }
                            t1 = t2;
                        }
                    }
                

                    // Just some reporting
                    if (++docNum % 1000 == 0) {
                        double now = System.currentTimeMillis();
                        double docsSec = docNum / ((now - startTime) / 1000);
                        verbose(LOGGER, "Processed document %d in round %d, " +
                                "docs/sec: %f", docNum, round, docsSec);
                    }
                }
            }

            for (Map.Entry<SignificanceTest,Double> e : tests.entrySet()) {
                SignificanceTest test = e.getKey();
                double minWeight = (e.getValue() == null) ? 0 : e.getValue();
                PrintWriter pw = testWriters.get(test);
                
                for (Map.Entry<Pair<String>,Integer> e2 : bigramCounts) {
                    Pair<String> bigram = e2.getKey();
                    int bigramCount = e2.getValue();
                    String t1 = bigram.x;
                    String t2 = bigram.y;
                    int t1Count = termCounts.getCount(t1);
                    int t2Count = termCounts.getCount(t2);
                    int t1butNotT2 = t1Count - bigramCount;
                    int t2butNotT1 = t2Count - bigramCount;
                    int neitherAppeared = 
                        allBigramCounts - ((t1Count + t2Count) - bigramCount);

                    double score = test.score(bigramCount, t1butNotT2, 
                                              t2butNotT1, neitherAppeared);
                    if (score > minWeight && !Double.isNaN(score) 
                            && !Double.isInfinite(score))
                        pw.println(t1 + "\t" + t2 + "\t" + score);
                }
            }

            for (PrintWriter pw : testWriters.values())
                pw.flush();
        }            

        for (PrintWriter pw : testWriters.values())
            pw.close();
    }


    private static Iterator<Corpus> getDocuments(ArgOptions argOptions) 
            throws IOException {

        throw new AssertionError();
        
        /*
        Collection<Iterator<Document>> docIters = 
            new LinkedList<Iterator<Document>>();

        if (argOptions.hasOption('f')) {
            for (String s : argOptions.getStringOption('f').split(","))
                docIters.add(new BufferedFileListDocumentIterator(s));
        }
        if (argOptions.hasOption('d')) {
            for (String s : argOptions.getStringOption('d').split(","))
                docIters.add(new OneLinePerDocumentIterator(s));
        }

         if (docIters.size() == 0)
             throw new IllegalStateException(
                 "Must specify at least one document source");

        // combine all of the document iterators into one iterator.
        Iterator<Document> docIter = new CombinedIterator<Document>(docIters);
        
        return docIter;
        */
    }

    /**
     * Removes all tokens in the document that are not member of either set,
     * returning the cleaned version, or if the sets contain no elements
     * (indicating no filtering is to be done), the original string is returned.
     */
    private static Set<String> clean(String document, 
                                     Set<String> validContext) {
        Set<String> tokens = new HashSet<String>();
        String[] arr = document.split("\\s+");
        // If either set does not include tokens, this indicates that the
        // context should include all the possible tokens.  Therefore, just
        // return the string in its original form.
        if (validContext.isEmpty()) {
            tokens.addAll(Arrays.asList(arr));
            return tokens;
        }

        for (String token : arr) {
            if (validContext.contains(token))
                tokens.add(token);
        }
        return tokens;
    }

    private static SignificanceTest getTest(String testName) {
        if (testName.equals("g-test"))
            return new GTest();
        else if (testName.equals("chi-squared"))
            return new ChiSquaredTest();
        else if (testName.equals("pmi"))
            return new PointwiseMutualInformationTest();
        else
            throw new IllegalArgumentException(
                "No such significance test: " + testName);
    }

    /**
     * Prints the options and supported commands used by this program.
     *
     * @param options the options supported by the system
     */
    private static void usage(ArgOptions options) {
        System.out.println(
            "IterativeBigramExtractor version 1.0\n" +
            "usage: java IterativeBigramExtractor [options] " +
            "terms.txt output-prefix test-name [min-weight] " + 
            "[test2-name [min-weight]]\n\n"
            + options.prettyPrint());
    }
}
