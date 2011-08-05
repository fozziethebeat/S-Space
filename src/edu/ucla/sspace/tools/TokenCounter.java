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

import edu.ucla.sspace.mains.OptionDescriptions;

import edu.ucla.sspace.text.DocumentPreprocessor;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.text.StringUtils;

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
 * @author David Jurgens
 */
public class TokenCounter {

    /**
     * The number of tokens to process before emitting a verbose message about
     * the counting status.
     */
    private static final int UPDATE_INTERVAL = 10000;

    /**
     * The logger used to emit messages for this class
     */
    private static final Logger LOGGER = 
        Logger.getLogger(TokenCounter.class.getName());

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
     * Creates a new token counter
     */
    public TokenCounter() { 
        this(false);
    }

    /**
     * Creates a new token counter that optionally lower cases tokens
     *
     * @param doLowerCasing {@code true} if the token counter should lower case
     *        all tokens before counting
     */
    public TokenCounter(boolean doLowerCasing) { 
        this.doLowerCasing = doLowerCasing;
        tokenToCount = new TrieMap<Integer>();
    }

    /**
     * Returns a mapping from each seen token to the number of times it occurred
     */
    public Map<String,Integer> getTokenCounts() {
        return Collections.unmodifiableMap(tokenToCount);
    }

    /**
     * Counts all of the tokens in the file with specified name
     */
    public void processFile(String fileName) throws IOException {
        process(new BufferedReader(new FileReader(fileName)));
    }

    /**
     * Counts all of the tokens in the file
     */
    public void processFile(File file) throws IOException {
        process(new BufferedReader(new FileReader(file)));
    }

    /**
     * Counts all of the tokens in the reader
     */
    public void process(BufferedReader br) {
        process(IteratorFactory.tokenize(br));
    }
     
    /**
     * Counts all of the tokens in the string
     */
    public void process(String tokens) {
        process(IteratorFactory.tokenize(tokens));
    }

    /**
     * Counts all of the tokens in the iterator
     */
    private void process(Iterator<String> tokens) {
        // NOTE: this method is intentionally private to ensure that the
        // IteratorFactory.tokenize() tokenization scheme is enforced on the
        // input data
        long numTokens = 0;
        while (tokens.hasNext()) {
            String token = tokens.next();
            if (doLowerCasing)
                token = token.toLowerCase();
            if (token.matches("[0-9]+"))
                token = "<NUM>";
            if (token.matches("[^\\w\\s;:\\(\\)\\[\\]'!/&?\",\\.<>]"))
                continue;

            Integer count = tokenToCount.get(token);
            tokenToCount.put(token, (count == null) ? 1 : 1 + count);
            numTokens++;
            if (numTokens % UPDATE_INTERVAL == 0 
                    && LOGGER.isLoggable(Level.FINE))
                LOGGER.fine("Processed " + numTokens + " tokens.  Currently " 
                            + tokenToCount.size() + " unique tokens");
        }
    }

    public static void main(String[] args) {
        ArgOptions options = new ArgOptions();
        options.addOption('Z', "stemmingAlgorithm",
                          "specifices the stemming algorithm to use on " +
                          "tokens while iterating.  (default: none)",
                          true, "CLASSNAME", "Tokenizing Options");
        options.addOption('F', "tokenFilter", "filters to apply to the input " +
                          "token stream", true, "FILTER_SPEC", 
                          "Tokenizing Options");
        options.addOption('C', "compoundWords", "a file where each line is a " +
                          "recognized compound word", true, "FILE", 
                          "Tokenizing Options");
        options.addOption('L', "lowerCase", "lower-cases each token after " +
                          "all other filtering has been applied", false, null, 
                          "Tokenizing Options");
        options.addOption('z', "wordLimit", "Set the maximum number of words " +
                          "an document can return",
                          true, "INT", "Tokenizing Options");
        options.addOption('v', "verbose",
                          "Print verbose output about counting status",
                          false, null, "Optional");
        options.parseOptions(args);
        if (options.numPositionalArgs() < 2) {
            System.out.println(
                "usage: java TokenCounter" 
                + " [options] <output-file> <input-file> [<input-file>]*\n"
                + options.prettyPrint() 
                + "\n" + OptionDescriptions.COMPOUND_WORDS_DESCRIPTION
                + "\n\n" + OptionDescriptions.TOKEN_FILTER_DESCRIPTION);
            return;
        }

        if (options.hasOption("verbose")) 
            LoggerUtil.setLevel(Level.FINE);


        boolean doLowerCasing = options.hasOption("lowerCase");

        Properties props = System.getProperties();
        // Initialize the IteratorFactory to tokenize the documents according to
        // the specified configuration (e.g. filtering, compound words)
        if (options.hasOption("tokenFilter"))
            props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
                              options.getStringOption("tokenFilter"));
        // Set any tokenizing options.
        if (options.hasOption("stemmingAlgorithm"))
            props.setProperty(IteratorFactory.STEMMER_PROPERTY,
                              options.getStringOption("stemmingAlgorithm"));
         
        if (options.hasOption("compoundWords")) 
            props.setProperty(IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
                              options.getStringOption("compoundWords"));
        if (options.hasOption("wordLimit"))
            props.setProperty(IteratorFactory.TOKEN_COUNT_LIMIT_PROPERTY,
                              options.getStringOption("wordLimit"));

        IteratorFactory.setProperties(props);

        try {
            TokenCounter counter = new TokenCounter(doLowerCasing);
            // Process each of the input files
            for (int i = 1; i < options.numPositionalArgs(); ++i)
                counter.processFile(options.getPositionalArg(i));
            // Then write the results to disk
            PrintWriter pw = new PrintWriter(options.getPositionalArg(0));
            for (Map.Entry<String,Integer> e 
                     : counter.tokenToCount.entrySet())
                pw.println(e.getKey() + " " + e.getValue());
            pw.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
