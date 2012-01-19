/*
 * Copyright 2011 David Jurgens
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
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;

import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.NearestNeighborFinder;
import edu.ucla.sspace.util.PartitioningNearestNeighborFinder;
import edu.ucla.sspace.util.SerializableUtil;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Map;

import java.util.logging.Level;


/**
 * The tool for running the {@link NearestNeighborFinder} from command line.
 * This class allows both creating as well using an existing {@code
 * NearestNeighborFinder}.
 */
public class NearestNeighborFinderTool {

    /**
     * Runs the program
     */
    public static void main(String[] args) {
        ArgOptions options = new ArgOptions();
        
        options.addOption('h', "help", "Generates a help message and exits",
                          false, null, "Program Options");
        options.addOption('v', "verbose", "Enables verbose reporting",
                          false, null, "Program Options");


        options.addOption('C', "createFinder", "Creates a nearest " +
                          "neighbor finder from the provided .sspace file",
                          true, "FILE", "Program Options");
        options.addOption('L', "loadFinder", "Loads the finder from " +
                          "file", true, "FILE", "Program Options");
        options.addOption('S', "saveFinder", "Saves the loaded or created " +
                          "finder to file", true, "FILE", "Program Options");

        options.addOption('p', "principleVectors", "Specifies the number " +
                          "of principle vectors to create",
                          true, "INT", "Creation Options");

        options.parseOptions(args);

        if (options.hasOption("help") || 
                (!options.hasOption('C') && !options.hasOption('L'))) {
            usage(options);
            return;
        }
        
        if (options.hasOption("verbose")) 
            LoggerUtil.setLevel(Level.FINE);

        if (options.hasOption('C') && options.hasOption('L')) {
            System.out.println("Cannot load and create a finder concurrently");
            System.exit(1);
        }
        
        NearestNeighborFinder nnf = null;
        if (options.hasOption('C')) {
            try {
                SemanticSpace sspace = 
                    SemanticSpaceIO.load(options.getStringOption('C'));
                int numWords = sspace.getWords().size();
                // See how many principle vectors to create
                int numPrincipleVectors = -1;
                if  (options.hasOption('p')) {
                    numPrincipleVectors = options.getIntOption('p');
                    if (numPrincipleVectors > numWords) {
                        throw new IllegalArgumentException(
                            "Cannot have more principle vectors than " +
                            "word vectors: " + numPrincipleVectors);
                    }
                    else if (numPrincipleVectors < 1) {
                        throw new IllegalArgumentException(
                            "Must have at least one principle vector");
                    }

                }
                else {
                    numPrincipleVectors = 
                        Math.min((int)(Math.ceil(Math.log(numWords))), 1000);
                    System.err.printf("Choosing a heuristically selected %d " +
                                      "principle vectors%n", 
                                      numPrincipleVectors);
                }
                nnf = new PartitioningNearestNeighborFinder(
                    sspace, numPrincipleVectors);
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }
        else if (options.hasOption('L')) {
            nnf = SerializableUtil.<NearestNeighborFinder>load(
                new File(options.getStringOption('L')));
        }
        else {
            throw new IllegalArgumentException(
                "Must either create or load a NearestNeighborFinder");
        }

        if (options.hasOption('S')) {
            SerializableUtil.save(nnf, new File(options.getStringOption('S')));
        }

        int numWords = options.numPositionalArgs();
        for (int i = 0; i < numWords; ++i) {
            String term = options.getPositionalArg(i);
            long start = System.currentTimeMillis();            
            MultiMap<Double,String> m = nnf.getMostSimilar(term, 10);
            if (m == null) {
                System.out.println(term + " is not in the semantic " +
                                   "space; no neighbors found.");
            }
            else {
                long time = System.currentTimeMillis() - start;
                //             System.err.printf("Found the neighbors of %s in %dms%n", 
                //                               term, time / 1000);                              
                System.out.println(term);
                for (Map.Entry<Double,String> e : m.entrySet())
                    System.out.println(e.getValue() + "\t" + e.getKey());            
            }
        }
    }

    /**
     * Prints the options and supported commands used by this program.
     *
     * @param options the options supported by the system
     */
    private static void usage(ArgOptions options) {
        System.out.println(
            "NearestNeighborFinder Tool version 1.0\n" +
            "usage: java -jar nnf.jar [options] [word1 word2...]\n\n" 
            + options.prettyPrint() + 
            "The primary purpose of this tool is the build " + 
            "instances of the\n" +
            "NearestNeighborFinder class from an existing .sspace " + 
            "file.  An example command\n" +
            "line would look like:\n" +
            "\n" +
            "java -jar nnf.jar --createFinder my.sspace " +
            "--saveFinder my.nnf.ser --principleVectors 1000\n" +
            "\n" +
            "However, it may also be used with an existing " + 
            "serialized NearestNeighborFinder\n" +
            "instance to search for the nearest neighbors words, " +
            "which are reported to stdout:\n" +
            "\n" +
            "java -jar tools/nnf.jar --loadFinder my.nnf.ser " + 
            "word1 word2 word3");
    }
            
}