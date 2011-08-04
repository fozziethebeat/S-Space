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
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.WordComparator;

import edu.ucla.sspace.util.BoundedSortedMap;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SortedMultiMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A utility tool for generating lists of most similar words for each word in a
 * {@link SemanticSpace}.
 */
public class SimilarityListGenerator {

    public static final int DEFAULT_SIMILAR_ITEMS = 10;

    private static final Logger LOGGER = 
        Logger.getLogger(SimilarityListGenerator.class.getName());

    private final ArgOptions argOptions;
    
    public SimilarityListGenerator() { 
        argOptions = new ArgOptions();
        addOptions();
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    private void addOptions() {

        argOptions.addOption('p', "printSimilarity",
                             "whether to print the similarity score " +
                             "(default: false)",
                             false, null, "Program Options");
        argOptions.addOption('s', "similarityFunction",
                             "name of a similarity function (default: cosine)",
                             true, "String", "Program Options");
        argOptions.addOption('n', "numSimilar", "the number of similar words " +
                             "to print (default: 10)", true, "String", 
                             "Program Options");
        argOptions.addOption('t', "threads", "the number of threads to use" +
                             " (default: #procesors)", true, "int", 
                             "Program Options");
        argOptions.addOption('w', "overwrite", "specifies whether to " +
                             "overwrite the existing output (default: true)",
                             true, "boolean", "Program Options");
        argOptions.addOption('v', "verbose", "prints verbose output "+ 
                             "(default: false)", false, null, 
                             "Program Options");
    }


    private void usage() {
        System.out.println("usage: java SimilarityListGenerator [options] " +
                           "<sspace-file> <output-dir>\n"  + 
                           argOptions.prettyPrint());
    }

    public static void main(String[] args) {
        try {
            SimilarityListGenerator generator = new SimilarityListGenerator();
            if (args.length == 0) {
                generator.usage();
                return;
            }

            generator.run(args);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }        

    public void run(String[] args) throws Exception {

        // process command line args
        argOptions.parseOptions(args);

        if (argOptions.numPositionalArgs() < 2) {
            throw new IllegalArgumentException("must specify input and output");
        }

        final File sspaceFile = new File(argOptions.getPositionalArg(0));
        final File outputDir = new File(argOptions.getPositionalArg(1));

        if (!outputDir.isDirectory()) {
            throw new IllegalArgumentException(
                "output directory is not a directory: " + outputDir);
        }
                    
        // Load the program-specific options next.
        int numThreads = Runtime.getRuntime().availableProcessors();
        if (argOptions.hasOption("threads")) {
            numThreads = argOptions.getIntOption("threads");
        }

        boolean overwrite = true;
        if (argOptions.hasOption("overwrite")) {
            overwrite = argOptions.getBooleanOption("overwrite");
        }

        if (argOptions.hasOption('v')) {
            // update the loggers to print FINE messages as well as INFO
            // messages
            Logger.getLogger("edu.ucla.sspace").setLevel(Level.FINE);
        }

        // load the behavior options
        final boolean printSimilarity = argOptions.hasOption('p');

        String similarityTypeName = (argOptions.hasOption('s'))
            ? argOptions.getStringOption('s').toUpperCase() : "COSINE";

        SimType similarityType = SimType.valueOf(similarityTypeName);
        
        LOGGER.fine("using similarity measure: " + similarityType);
        
        final int numSimilar = (argOptions.hasOption('n'))
            ? argOptions.getIntOption('n') : 10;

        LOGGER.fine("loading .sspace file: " + sspaceFile.getName());
        
        final SemanticSpace sspace = SemanticSpaceIO.load(sspaceFile);

        File output = (overwrite)
            ? new File(outputDir, sspaceFile.getName() + ".similarityList")
            : File.createTempFile(sspaceFile.getName(), "similarityList",
                                  outputDir);

        final PrintWriter outputWriter = new PrintWriter(output);
            
        final Set<String> words = sspace.getWords();
        WordComparator comparator = new WordComparator(numThreads);


        for (String word : words) {            
            // compute the k most-similar words to this word
            SortedMultiMap<Double,String> mostSimilar =
                comparator.getMostSimilar(word, sspace, numSimilar,
                                          similarityType);
            
            // once processing has finished write the k most-similar words to
            // the output file.
            StringBuilder sb = new StringBuilder(256);
            sb.append(word).append("|");
            for (Map.Entry<Double,String> e : 
                     mostSimilar.entrySet()) {
                String s = e.getValue();
                Double d = e.getKey();

                sb.append(s);
                if (printSimilarity) {
                    sb.append(" ").append(d);
                }
                sb.append("|");
                
            }
            outputWriter.println(sb.toString());
            outputWriter.flush();
        }
    }
}
