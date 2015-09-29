/*
 * Copyright 2015 David Jurgens
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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.corenlp.CoreNlpProcessedCorpus;

import edu.ucla.sspace.text.Corpus;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.SingleFileCorpusReader;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.LimitedIterator;
import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.ReflectionUtil;
import edu.ucla.sspace.util.WorkQueue;

import edu.ucla.sspace.vector.DoubleVector;

import edu.ucla.sspace.word2vec.Word2Vec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author David Jurgens
 */
public class Word2VecCli {

    private static final Logger LOGGER = 
        Logger.getLogger(Word2VecCli.class.getName());

    /**
     * Whether to emit messages to {@code stdout} when the {@code verbose}
     * methods are used.
     */
    protected boolean verbose;

    private boolean isMultiThreaded;
    
    /**
     * The processed argument options available to the main classes.
     */
    protected final ArgOptions argOptions;

    public Word2VecCli() {
        this(true);
    }

    public Word2VecCli(boolean isMultiThreaded) {
        this.isMultiThreaded = isMultiThreaded;
        argOptions = setupOptions();
        verbose = false;
    }
    
    /**
     * Prints out information on how to run the program to {@code stdout} using
     * the option descriptions for compound words, tokenization, .sspace formats
     * and help.
     */
    protected void usage() {
        System.out.println(
            "usage: java " 
            + this.getClass().getName()
            + " [options] <output-dir>\n"
            + argOptions.prettyPrint());
    }

    /**
     * Returns the {@link SemanticSpaceIO#SSpaceFormat sspace} in which the
     * finished {@code SemanticSpace} should be saved.  Subclasses should
     * override this function if they want to specify a specific format that is
     * most suited for their space, when one is not manually specified by the
     * user.
     *
     * @return the format in which the semantic space will be saved
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.TEXT;
    }

    /**
     * Adds options to the provided {@code ArgOptions} instance, which will be
     * used to parse the command line.  This method allows subclasses the
     * ability to add extra command line options.
     *
     * @param options the ArgOptions object which more main specific options can
     *        be added to.
     *
     * @see #handleExtraOptions()
     */
    protected void addExtraOptions(ArgOptions options) { }

    /**
     * Once the command line has been parsed, allows the subclasses to perform
     * additional steps based on class-specific options.  This method will be
     * called before {@link #getSpace() getSpace}.
     *
     * @see #addExtraOptions(ArgOptions)
     */
    protected void handleExtraOptions() { }

    /**
     * Allows subclasses to interact with the {@code SemanticSpace} after the
     * space has finished processing all of the text.
     */
    protected void postProcessing() { }

    /**
     * Returns the {@code Properties} object that will be used when calling
     * {@link SemanticSpace#processSpace(Properties)}.  Subclasses should
     * override this method if they need to specify additional properties for
     * the space.  This method will be called once before {@link #getSpace()}.
     *
     * @return the {@code Properties} used for processing the semantic space.
     */
    protected Properties setupProperties() {
        Properties props = System.getProperties();
        return props;
    }

    /**
     * Adds the default options for running semantic space algorithms from the
     * command line.  Subclasses should override this method and return a
     * different instance if the default options need to be different.
     */
    protected ArgOptions setupOptions() {
        ArgOptions options = new ArgOptions();

        // Add input file options.
        options.addOption('f', "fileList", "a list of document files", 
                          true, "FILE[,FILE...]", "Required (at least one of)");
        options.addOption('d', "docFile", 
                          "a file where each line is a document", true,
                          "FILE[,FILE...]", "Required (at least one of)");
        options.addOption('R', "corpusReader", 
                          "Specifies a CorpusReader which will " +
                          "automatically parse the document files that are " +
                          "not in the formats expected by -f and -d.",
                          true, "CLASSNAME,FILE[,FILE...]",
                          "Required (at least one of)");

        // Add run time options.
        options.addOption('o', "outputFormat", "the .sspace format to use",
                          true, "FORMAT", 
                          "Program Options");
        if (isMultiThreaded) {
            options.addOption('t', "threads", "the number of threads to use",
                              true, "INT", "Program Options");
        }
        options.addOption('w', "overwrite", "specifies whether to " +
                          "overwrite the existing output", true, "BOOL",
                          "Program Options");
        options.addOption('v', "verbose", "prints verbose output",
                          false, null, "Program Options");

        // Add tokenizing options.
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
        options.addOption('z', "wordLimit", "Set the maximum number of words " +
                          "a document can return",
                          true, "INT", "Tokenizing Options");

        return options;
    }


    public static void main(String[] args) {
        try {
            new Word2VecCli().run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run(String[] args) throws Exception {       
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        argOptions.parseOptions(args);
        
        if (argOptions.numPositionalArgs() == 0) {
            throw new IllegalArgumentException("must specify output path");
        }

        verbose = argOptions.hasOption('v') || argOptions.hasOption("verbose");
        // If verbose output is enabled, update all the loggers in the S-Space
        // package logging tree to output at Level.FINE (normally, it is
        // Level.INFO).  This provides a more detailed view of how the execution
        // flow is proceeding.
        if (verbose) 
            LoggerUtil.setLevel(Level.FINE);

        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        
        Corpus corpus = new CoreNlpProcessedCorpus(new SingleFileCorpusReader(inputFile));
        Word2Vec word2vec = new Word2Vec();
        word2vec.process(corpus);
        word2vec.build(1000, 1e-5, 1, false, 100, 5, 0.025, 1e-2, 0);

        PrintWriter pw = new PrintWriter(outputFile);

        int dims = word2vec.getVectorLength();

        StringBuilder sb = new StringBuilder();
        for (String word : word2vec.getWords()) {
            DoubleVector v = word2vec.getVector(word);
            sb.setLength(0);
            sb.append(word);
            for (int i = 0; i < dims; ++i)
                sb.append(' ').append(v.get(i));
            pw.println(sb);
        }
        pw.close();
    }
    
}
