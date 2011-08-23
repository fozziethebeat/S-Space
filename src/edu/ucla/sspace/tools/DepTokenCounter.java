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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.mains.OptionDescriptions;

import edu.ucla.sspace.dependency.CoNLLDependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.WaCKyDependencyExtractor;

import edu.ucla.sspace.text.DependencyFileDocumentIterator;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.TokenFilter;
import edu.ucla.sspace.text.Stemmer;

import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.TrieMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class for counting tokens in one or more files.  This class also
 * supports counting compound token instances, as well as counting for only a
 * subset of the unique tokens.  This class is intended for token counting in
 * very large corpora where space-efficiency is important.  The output is
 * equivalent to the command <tt>cat <i>corpus.txt</i> | awk '{ split($0,a); for
 * (i in a) { print a[i]; }}' | uniq -c</tt>.  However, this
 * command is significantly more memory and CPU intensive. 
 *
 * @author Keith Stevens 
 */
public class DepTokenCounter {

    /**
     * The number of tokens to process before emitting a verbose message about
     * the counting status.
     */
    private static final int UPDATE_INTERVAL = 10000;

    /**
     * The logger used to emit messages for this class
     */
    private static final Logger LOGGER = 
        Logger.getLogger(DepTokenCounter.class.getName());

    /**
     * A mapping from token to the number of times it occurred
     */
    private final Map<String,Integer> tokenToCount;

    /**
     * {@code true} if the token counter should lower case all tokens before
     * counting
     */
    private final boolean doLowerCasing;

    /**
     * If true, part of speech tags will be added to each term before being used
     * as a dimension.
     */
    private final boolean doPos;

    /**
     * The {@link DependencyExtractor} used to extract parse trees.
     */
    private final DependencyExtractor extractor;

    /**
     * Creates a new token counter that optionally lower cases tokens
     *
     * @param doLowerCasing {@code true} if the token counter should lower case
     *        all tokens before counting
     */
    public DepTokenCounter(boolean doLowerCasing,
                           boolean doPos,
                           DependencyExtractor extractor) { 
        this.doLowerCasing = doLowerCasing;
        this.doPos = doPos;
        this.extractor = extractor;

        tokenToCount = new TrieMap<Integer>();
    }

    /**
     * Returns a mapping from each seen token to the number of times it occurred
     */
    public Map<String,Integer> getTokenCounts() {
        return Collections.unmodifiableMap(tokenToCount);
    }

    /**
     * Counts all of the tokens in the iterator
     */
    private void process(Iterator<Document> docs) throws IOException {
        long numTokens = 0;
        while (docs.hasNext()) {
            Document doc = docs.next();
            DependencyTreeNode[] nodes = extractor.readNextTree(doc.reader());
            for (DependencyTreeNode node : nodes) {
                String token = node.word();
                if (doLowerCasing)
                    token = token.toLowerCase();
                if (doPos)
                    token = token + "-" + node.pos();

                Integer count = tokenToCount.get(token);
                tokenToCount.put(token, (count == null) ? 1 : 1 + count);
                numTokens++;

                if (numTokens % UPDATE_INTERVAL == 0)
                    LOGGER.fine(
                            "Processed " + numTokens + " tokens.  Currently " +
                            tokenToCount.size() + " unique tokens");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // Setup the argument options.
        ArgOptions options = new ArgOptions();
        options.addOption('Z', "stemmingAlgorithm",
                          "specifices the stemming algorithm to use on " +
                          "tokens while iterating.  (default: none)",
                          true, "CLASSNAME", "Tokenizing Options");
        options.addOption('F', "tokenFilter", "filters to apply to the input " +
                          "token stream", true, "FILTER_SPEC", 
                          "Tokenizing Options");
        options.addOption('L', "lowerCase", "lower-cases each token after " +
                          "all other filtering has been applied", false, null, 
                          "Tokenizing Options");
        options.addOption('P', "partOfSpeech",
                          "use part of speech tags for each token.",
                          false, null, "Tokenizing Options");
        options.addOption('H', "discardHeader",
                          "If true, the first line of each dependency " +
                          "document will be discarded.",
                          false, null, "Tokenizing Options");
        options.addOption('v', "verbose",
                          "Print verbose output about counting status",
                          false, null, "Optional");
        options.addOption('D', "dependencyParseFormat",
                          "the name of the dependency parsed format for " +
                          "the corpus (defalt: CoNLL)",
                          true, "STR", 
                          "Advanced Dependency Parsing");

        // Parse and validate the options.
        options.parseOptions(args);
        if (options.numPositionalArgs() < 2) {
            System.out.println(
                "usage: java DepTokenCounter" 
                + " [options] <output-file> <input-file> [<input-file>]*\n"
                + options.prettyPrint() 
                + "\n\n" + OptionDescriptions.TOKEN_FILTER_DESCRIPTION);
            return;
        }

        // Setup logging.
        if (options.hasOption("verbose")) 
            LoggerUtil.setLevel(Level.FINE);

        // Extract key arguments.
        boolean doLowerCasing = options.hasOption("lowerCase");
        boolean doPos = options.hasOption("partOfSpeech");
        boolean discardHeader = options.hasOption('H');

        TokenFilter filter = (options.hasOption("tokenFilter"))
            ? TokenFilter.loadFromSpecification(options.getStringOption('F'))
            : null;

        Stemmer stemmer = options.getObjectOption("stemmingAlgorithm", null);

        String format = options.getStringOption(
                "dependencyParseFormat", "CoNLL");

        // setup the dependency extractor.
        DependencyExtractor e = null;
        if (format.equals("CoNLL"))
            e = new CoNLLDependencyExtractor(filter, stemmer);
        else if (format.equals("WaCKy"))
            e = new WaCKyDependencyExtractor(filter, stemmer);

        DepTokenCounter counter = new DepTokenCounter(doLowerCasing, doPos, e);

        // Process each of the input files
        for (int i = 1; i < options.numPositionalArgs(); ++i)
            counter.process(new DependencyFileDocumentIterator(
                        options.getPositionalArg(i), discardHeader));

        // Then write the results to disk
        PrintWriter pw = new PrintWriter(options.getPositionalArg(0));
        for (Map.Entry<String,Integer> entry 
                 : counter.tokenToCount.entrySet())
            pw.printf("%s %d\n", entry.getKey(), entry.getValue());
        pw.close();
    }
}
