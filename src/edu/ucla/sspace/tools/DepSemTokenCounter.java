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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.Collections;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Keith Stevens 
 */
public class DepSemTokenCounter {

    /**
     * A mapping from token to the number of times it occurred
     */
    private final Set<String> foundTokens;

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
    public DepSemTokenCounter(DependencyExtractor extractor) { 
        this.extractor = extractor;
        foundTokens = new HashSet<String>();
    }

    public Set<String> getTokens() {
        return foundTokens;
    }

    /**
     * Counts all of the tokens in the iterator
     */
    private void process(Iterator<Document> docs) throws IOException {
        long numTokens = 0;
        while (docs.hasNext()) {
            Document doc = docs.next();
            BufferedReader br = doc.reader();
            String header = br.readLine();

            DependencyTreeNode[] nodes = extractor.readNextTree(br);
            int index;
            for (index = 0; index < nodes.length; ++index)
                if (nodes[index].lemma().equals(header))
                    break;

            foundTokens.add(nodes[index].word().toLowerCase());
            for (int i = index+1; i < index+10 && i < nodes.length; ++i)
                foundTokens.add(nodes[i].word().toLowerCase());
            for (int i = Math.max(0, index-10); i < index; ++i)
                foundTokens.add(nodes[i].word().toLowerCase());
        }
    }

    public static void main(String[] args) throws Exception {
        // Setup the argument options.
        ArgOptions options = new ArgOptions();
        options.addOption('F', "tokenFilter", "filters to apply to the input " +
                          "token stream", true, "FILTER_SPEC", 
                          "Tokenizing Options");
        options.addOption('v', "verbose",
                          "Print verbose output about counting status",
                          false, null, "Optional");

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

        TokenFilter filter = (options.hasOption("tokenFilter"))
            ? TokenFilter.loadFromSpecification(options.getStringOption('F'))
            : null;

        // setup the dependency extractor.
        DependencyExtractor e = new CoNLLDependencyExtractor(filter, null);
        DepSemTokenCounter counter = new DepSemTokenCounter(e);

        // Process each of the input files
        for (int i = 1; i < options.numPositionalArgs(); ++i)
            counter.process(new DependencyFileDocumentIterator(
                        options.getPositionalArg(i)));

        // Then write the results to disk
        PrintWriter pw = new PrintWriter(options.getPositionalArg(0));
        for (String term : counter.getTokens()) 
            pw.println(term);
        pw.close();
    }
}
