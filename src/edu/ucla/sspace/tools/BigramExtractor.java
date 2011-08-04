/*
 * Copyright 2009 David Jurgens 
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

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.TrieMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A utility class for computing bigram statistics from a corpus.
 */
public class BigramExtractor {
    
    /**
     * The significance tests to use in determing how two tokens are
     * statistically related in their occurrences.
     */
    public enum SignificanceTest {
        CHI_SQUARED,
            FISHERS_EXACT,
            BARNARDS,
            PMI,
            LOG_LIKELIHOOD
    }

    /**
     * The logger used to emit status updates from the extractor
     */ 
    private static final Logger LOGGER =
        Logger.getLogger(BigramExtractor.class.getName());

    /**
     * A mapping from a token to the occurrence statistics for it.
     */
    private final Map<String,TokenStats> tokenCounts;

    /**
     * A mapping from the packed-long (consisting of the two {@code int}
     * token-indices values representing a bigram) to the number of times the
     * bigram occurred in the corpus.
     */
    private final Map<Long,Number> bigramCounts;

    /**
     * A counter for assigning new unique tokens to a unique numeric index
     */
    private int tokenIndexCounter;

    /**
     * A count of how many bigrams were seen in the corpus
     */
    private int numBigramsInCorpus;

    /**
     * Creates a new bigram extractor
     */
    public BigramExtractor() {
        this(1000);
    }

    /**
     * Creates a new bigram extractor that expects approximately the specified
     * number of bigrams
     */
    public BigramExtractor(int expectedNumBigrams) {
        tokenCounts = new TrieMap<TokenStats>();
            //new HashMap<CharSequence,TokenStats>();
        bigramCounts = new HashMap<Long,Number>(expectedNumBigrams);
        tokenIndexCounter = 0;
        numBigramsInCorpus = 0;
    }

    /**
     * Returns {@code true} if the token should not be considered as a part of
     * any bigram.
     */
    private boolean excludeToken(String token) {
        return token.equals(IteratorFactory.EMPTY_TOKEN);
    }

    /**
     * Processes the tokens in the text to gather statistics for any bigrams
     * contained therein
     */
    public void process(String text) {
        // Use tokenize ordered to pick up any filtering that was done by a
        // TokenFilter
        process(IteratorFactory.tokenizeOrdered(text));
    }
    
    /**
     * Processes the tokens in the reader to gather statistics for any bigrams
     * contained therein
     */
    public void process(BufferedReader text) {
        // Use tokenize ordered to pick up any filtering that was done by a
        // TokenFilter
        process(IteratorFactory.tokenizeOrdered(text));
    }

    /**
     * Processes the tokens in the iterator to gather statistics for any bigrams
     * contained therein
     */
    public void process(Iterator<String> text) {
        String nextToken = null, curToken = null;
        // Base case for the next token buffer to ensure we always have two
        // valid tokens present
        if (text.hasNext())
            nextToken = text.next();
        while (text.hasNext()) {
            curToken = nextToken;
            nextToken = text.next();
            // Only process bigrams where the two tokens weren't excluded by the
            // token filter
            if (!(excludeToken(curToken) || excludeToken(nextToken)))
                processBigram(curToken, nextToken);
        }
    }

    /**
     * Updates the statistics for the bigram formed from the provided left and
     * right token.
     *
     * @param left the left token in the bigram
     * @param right the right token in the bigram
     */
    private void processBigram(String left, String right) {
        TokenStats leftStats = getStatsFor(left);
        TokenStats rightStats = getStatsFor(right);
        
        // mark that both appeared
        leftStats.count++;
        rightStats.count++;

        // Mark the respective positions of each
        leftStats.leftCount++;
        rightStats.rightCount++;

        // Increase the number of bigrams seen
        numBigramsInCorpus++;

        // Update the bigram statistics

        // Map the two token's indices into a single long
        long bigram = (((long)leftStats.index) << 32) |  rightStats.index;
        Number curBigramCount = bigramCounts.get(bigram);
        int i = (curBigramCount == null) ? 1 : 1 + curBigramCount.intValue();

        // Compact the count into the smallest numeric type that can represent
        // it.  This hopefully results in some space savings.
        Number val = null;
        if (i < Byte.MAX_VALUE)
            val = Byte.valueOf((byte)i);
        else if (i < Short.MAX_VALUE)
            val = Short.valueOf((short)i);
        else
            val = Integer.valueOf(i);
        
        bigramCounts.put(bigram, val);
    }


    private TokenStats getStatsFor(String token) {
        TokenStats stats = tokenCounts.get(token);
        if (stats == null) {
            stats = new TokenStats(tokenIndexCounter++);
            tokenCounts.put(token, stats);
        }
        return stats;
    }

//     /**
//      *@return a mapping from the significance test score to the list of bigrams
//      *that had that score
//      */
//     public NavigableMap<Double,Set<List<String>>> getBigrams(
//              SignificanceTest test) {
//         return getBigrams(test, 1);
//     }

//     /**
//      *
//      * @param minOccurrencePerToken the minimum number of times each token in a
//      *        bigram must occur for the bigram's score to be reported
//      *
//      * @return a mapping from the significance test score to the list of bigrams
//      *         that had that score
//      */
//     public NavigableMap<Double,Set<List<String>>> getBigrams(
//              SignificanceTest test, int minOccurrencePerToken) {

//         NavigableMap<Double,Set<List<String>>> scoreToBigram = new
//             TreeMap<Double,Set<List<String>>>();
        
//         for (Bigram b : bigramCounts.keySet()) {
            
//             // Skip processing any bigram whose tokens occur less than the
//             // minimum required
//             if (tokenCounts.get(b.firstTokenIndex).count < minOccurrencePerToken 
//                 || tokenCounts.get(b.secondTokenIndex).count < minOccurrencePerToken)
//                 continue;

//             int[] contingencyTable = getContingencyTable(b.firstTokenIndex,
//                                                          b.secondTokenIndex);
//             double score = -1;
//             switch (test) {
//                 case PMI:
//                     score = pmi(contingencyTable);
//                     break;
//                 case CHI_SQUARED:
//                     score = chiSq(contingencyTable);
//                     break;
//                 case LOG_LIKELIHOOD:
//                     score = logLikelihood(contingencyTable);
//                     break;
//                 default:
//                     throw new Error(test + " not implemented yet");
//             }
//             Set<List<String>> bigramsWithScore = scoreToBigram.get(score);
//             if (bigramsWithScore == null) {
//                 bigramsWithScore = new HashSet<List<String>>();
//                 scoreToBigram.put(score, bigramsWithScore);
//             }
//             //bigramsWithScore.add(bigramToStrings(b));
//         }
//         return scoreToBigram.descendingMap();
//     }

    /**
     * Prints all of the known bigrams, where each token in the
     * bigram must occur at least the number of specified time.
     *
     * @param output the writer where all the bigrams should be printed
     * @param test the significant test to use in rating the statistical
     *        correlation of two tokens
     * @param minOccurrencePerToken the minimum number of times each token in a
     *        bigram must occur for the bigram's score to be reported
     */
    public void printBigrams(PrintWriter output, 
                             SignificanceTest test, int minOccurrencePerToken) {
        
        String[] indexToToken = new String[tokenCounts.size()];
        for (Map.Entry<String,TokenStats> e : tokenCounts.entrySet()) 
            indexToToken[e.getValue().index] = e.getKey().toString();

        LOGGER.info("Number of bigrams: " + bigramCounts.size());
        
        for (Map.Entry<Long,Number> e : bigramCounts.entrySet()) {
            
            long bigram = e.getKey().longValue();
            int firstTokenIndex = (int)(bigram >>> 32);
            int secondTokenIndex = (int)(bigram & 0xFFFFFFFFL);
            int bigramCount = e.getValue().intValue();

            // Skip processing any bigram whose tokens occur less than the
            // minimum required
            TokenStats t1 = tokenCounts.get(indexToToken[firstTokenIndex]);
            TokenStats t2 = tokenCounts.get(indexToToken[secondTokenIndex]);
            
            //System.err.printf("t1: %s, t2: %s%n", t1, t2);

            if (t1.count < minOccurrencePerToken 
                    || t2.count < minOccurrencePerToken)
                continue;

            int[] contingencyTable = getContingencyTable(t1, t2, bigramCount);
            double score = getScore(contingencyTable, test);
            
            output.println(score + " " + indexToToken[firstTokenIndex]
                           + " " + indexToToken[secondTokenIndex]);
        }
    }

    /**
     * Returns the score of the contingency table using the specified
     * significance test
     *
     * @param contingencyTable a contingency table specified as four {@code int}
     *        values
     * @param test the significance test to use in evaluating the table
     */
    private double getScore(int[] contingencyTable, SignificanceTest test) {
        switch (test) {
        case PMI:
            return pmi(contingencyTable);
        case CHI_SQUARED:
            return chiSq(contingencyTable);
        case LOG_LIKELIHOOD:
            return logLikelihood(contingencyTable);
        default:
            throw new Error(test + " not implemented yet");
        }
    }

    /**
     * Returns the point-wise mutual information (PMI) score of the contingency
     * table
     */
    private double pmi(int[] contingencyTable) {
        // Rename for short-hand convenience
        int[] t = contingencyTable;

        double probOfBigram = t[0] / (double)numBigramsInCorpus;
        double probOfFirstTok = (t[0] + t[2]) / (double)numBigramsInCorpus;
        double probOfSecondTok = (t[0] + t[1]) / (double)numBigramsInCorpus;

        return probOfBigram / (probOfFirstTok * probOfSecondTok);
    }

    /**
     * Returns the &Chi;<sup>2<sup> score of the contingency table
     */
    private double chiSq(int[] contingencyTable) {
        // Rename for short-hand convenience
        int[] t = contingencyTable;
        int col1sum = t[0] + t[2];
        int col2sum = t[1] + t[3];
        int row1sum = t[0] + t[1];
        int row2sum = t[2] + t[3];
        double sum = row1sum + row2sum;
        
        // Calculate the expected values for a, b, c, d
        double aExp = (row1sum / sum) * col1sum;
        double bExp = (row1sum / sum) * col2sum;
        double cExp = (row2sum / sum) * col1sum;
        double dExp = (row2sum / sum) * col2sum;

        // Chi-squared is (Observed - Expected)^2 / Expected
        return 
            ((t[0] - aExp) * (t[0] - aExp) / aExp) +
            ((t[1] - bExp) * (t[1] - bExp) / bExp) +
            ((t[2] - cExp) * (t[2] - cExp) / cExp) +
            ((t[3] - dExp) * (t[3] - dExp) / dExp);
    }
    
    /**
     * Returns the log-likelihood score of the contingency table
     */
    private double logLikelihood(int[] contingencyTable) {
        // Rename for short-hand convenience
        int[] t = contingencyTable;
        int col1sum = t[0] + t[2];
        int col2sum = t[1] + t[3];
        int row1sum = t[0] + t[1];
        int row2sum = t[2] + t[3];
        double sum = row1sum + row2sum;
        
        // Calculate the expected values for a, b, c, d
        double aExp = (row1sum / sum) * col1sum;
        double bExp = (row1sum / sum) * col2sum;
        double cExp = (row2sum / sum) * col1sum;
        double dExp = (row2sum / sum) * col2sum;

        return 2 *
            ((t[0] * Math.log(t[0] - aExp))  +
             (t[1] * Math.log(t[1] - bExp))  +
             (t[2] * Math.log(t[2] - cExp))  +
             (t[3] * Math.log(t[3] - dExp)));
    }

    /**
     * Generates a contingency table from the occurrence statistics of the two
     * tokens.  The table is formatted as an array where the array values {@code
     * [a, b, c, d]} correspond to
     * <table><tr><td>a</td><td>b</td></tr><tr><td>c</td><td>d</td></tr></table>
     * in the table
     *
     * @param bigramCount the number of times the two tokens appeared together
     *        as a bigram
     *
     * @return the contingency table as an array
     */
    private int[] getContingencyTable(TokenStats leftTokenStats, 
                                      TokenStats rightTokenStats,
                                      int bigramCount) {
        int leftTokenOnLeftInAnyBigram = leftTokenStats.leftCount;
        int rightTokenOnRightInAnyBigram = rightTokenStats.rightCount;
        // The nubmer of bigrams in which both tokens appeared
        int a = bigramCount;
        // The number of times the left token appeared as the left token in some
        // other bigram without the current right token
        int b = rightTokenOnRightInAnyBigram - a;
        // The number of times the left token appeared as the left token in some
        // other bigram without the current right token
        int c = leftTokenOnLeftInAnyBigram - a;
        // The total number of bigrams in which neither the current left or
        // right token appeared
        int d = (numBigramsInCorpus - (b + c + a));
        return new int[] { a, b, c, d };
    }


    public static void main(String[] args) {
        ArgOptions options = new ArgOptions();
        options.addOption('F', "tokenFilter", "filters to apply to the input " +
                          "token stream", true, "FILTER_SPEC", 
                          "Tokenizing Options");
        options.addOption('M', "minFreq", "minimum frequency of the reported " +
                          "bigrams" , true, "INT", 
                          "Bigram Options");
        options.addOption('v', "verbose",
                          "Print verbose output about counting status",
                          false, null, "Program Options");
        options.parseOptions(args);

        if (options.numPositionalArgs() < 3) {
            System.out.println("usage: java BigramExtractor [options] " +
                               "<OutputFile> " +
                               "<SignificanceTest> " +
                               "<InputFile> [<InputFile>...]\n" +
                               " significance test options: " + 
                               SignificanceTest.values() + "\n" +
                               options.prettyPrint());
            return;
        }
        
        if (options.hasOption("verbose")) 
            LoggerUtil.setLevel(Level.FINE);


        Properties props = System.getProperties();
        // Initialize the IteratorFactory to tokenize the documents according to
        // the specified configuration (e.g. filtering, compound words)
        if (options.hasOption("tokenFilter"))
            props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
                              options.getStringOption("tokenFilter"));
        IteratorFactory.setProperties(props);
        
        try {
            BigramExtractor be = new BigramExtractor(1000000); // 1M
            String testStr = options.getPositionalArg(1).toUpperCase();
            SignificanceTest test = SignificanceTest.valueOf(testStr);
            PrintWriter output = new PrintWriter(options.getPositionalArg(0));
            int numArgs = options.numPositionalArgs();
            // Process each of the input files
            for (int i = 2; i < numArgs; ++i) {
                String inputFile = options.getPositionalArg(i);
                BufferedReader br = new BufferedReader(
                    new FileReader(inputFile));
            
                int lineNo = 0;
                for (String line = null; (line = br.readLine()) != null; ) {
                    be.process(line);
                    if (++lineNo % 10000 == 0)
                        LOGGER.fine(inputFile + 
                                    ": processed document " + lineNo);
                }
                br.close();
            }
            // Write out the bigrams to file
            int minFreq = (options.hasOption("minFreq"))
                ? options.getIntOption("minFreq")
                : 0;
            be.printBigrams(output, test, minFreq);
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * A utility class for keeping track of how many times a token has appeared
     * in different positions.
     */
    private static class TokenStats {

        /**
         * The numeric index associated with the token's string value
         */
        public int index;

        /**
         * The number of times the token occurred in the corpus
         */
        public int count;

        /**
         * The number of times the token appeared on the left-hand side of any
         * bigram
         */
        public int leftCount;

        /**
         * The number of times the token appeared on the right-hand side of any
         * bigram
         */
        public int rightCount;
        
        /**
         * Creates an instance for storing statistics for a token
         *
         * @param index the index of the token for which these statistics are
         *        being kept
         */
        public TokenStats(int index) {
            this.index = index;
            count = 0;
            leftCount = 0;
            rightCount = 0;
        }

    }
}
