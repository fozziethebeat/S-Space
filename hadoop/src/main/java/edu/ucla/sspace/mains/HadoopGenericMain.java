/*
 * Copyright 2010 David Jurgens
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
import edu.ucla.sspace.common.SemanticSpaceWriter;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.FileListDocumentIterator;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.text.OneLinePerDocumentIterator;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.LimitedIterator;
import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.ReflectionUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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
 * An abstract base class for algorithms that use Hadoop for corpus processing.
 * This class is the equivalent of {@link GenericMain}.
 *
 * @author David Jurgens
 */
public abstract class HadoopGenericMain {

    /**
     * The property for setting a unique corpus reader.  This corpus reader must
     * have a no argument constructor and implement {@code Iterator<Document>}.
     * Since this is expected to be a rare use case, this is done as a property
     * instead of a standard command line argument to keep the argument space
     * from being poluted.
     */
    public static final String CORPUS_READER_PROPERTY =
        "edu.ucla.sspace.mains.GenericMain.corpusReader";

    /**
     * Extension used for all saved semantic space files.
     */
    public static final String EXT = ".sspace";    

    private static final Logger LOGGER = 
        Logger.getLogger(GenericMain.class.getName());

    /**
     * Whether to emit messages to {@code stdout} when the {@code verbose}
     * methods are used.
     */
    protected boolean verbose;

    /**
     * The processed argument options available to the main classes.
     */
    protected final ArgOptions argOptions;

    public HadoopGenericMain() {
        argOptions = setupOptions();
    }

    /**
     * Returns a string describing algorithm-specific options and behaviods.
     * This string will be printed before the default option details
     */
    protected String getAlgorithmSpecifics() {
        return "";
    }
    
    /**
     * Prints out information on how to run the program to {@code stdout} using
     * the option descriptions for compound words, tokenization, .sspace formats
     * and help.
     */
    protected void usage() {
        String specifics = getAlgorithmSpecifics();
        System.out.println(
            "usage: java " 
            + this.getClass().getName()
            + " [options] input-dir [input-dir2 ...] output-sspace\n"
            + argOptions.prettyPrint() 
            + ((specifics.length() == 0) ? "" : "\n" + specifics)
            + "\n" + OptionDescriptions.COMPOUND_WORDS_DESCRIPTION
            + "\n\n" + OptionDescriptions.TOKEN_FILTER_DESCRIPTION
            + "\n\n" + OptionDescriptions.TOKEN_STEMMING_DESCRIPTION
            + "\n\n" + OptionDescriptions.FILE_FORMAT_DESCRIPTION
            + "\n\n" + OptionDescriptions.HELP_DESCRIPTION);
    }

    /**
     * Returns the {@link SemanticSpaceIO.SSpaceFormat format} in which the
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

        // Add run time options.
        options.addOption('o', "outputFormat", "the .sspace format to use",
                          true, "FORMAT", 
                          "Program Options");
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
                          "an document can return",
                          true, "INT", "Tokenizing Options");        

        addExtraOptions(options);
        return options;
    }

    /**
     * Processes the arguments and begins processing the documents using the
     * {@link SemanticSpace} returned by {@link #getSpace() getSpace}.
     *
     * @param args arguments used to configure this program and the {@code
     *        SemanticSpace}
     */
    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        argOptions.parseOptions(args);
        
        int numArgs = argOptions.numPositionalArgs();
        if (numArgs < 2) {
            throw new IllegalArgumentException("must specify output path");
        }

        verbose = argOptions.hasOption('v') || argOptions.hasOption("verbose");
        // If verbose output is enabled, update all the loggers in the S-Space
        // package logging tree to output at Level.FINE (normally, it is
        // Level.INFO).  This provides a more detailed view of how the execution
        // flow is proceeding.
        if (verbose) 
            LoggerUtil.setLevel(Level.FINE);

        boolean overwrite = true;
        if (argOptions.hasOption("overwrite")) {
            overwrite = argOptions.getBooleanOption("overwrite");
        }
        
        handleExtraOptions();

        Properties props = setupProperties();

        // Initialize the IteratorFactory to tokenize the documents according to
        // the specified configuration (e.g. filtering, compound words)
        if (argOptions.hasOption("tokenFilter")) {
            props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
                              argOptions.getStringOption("tokenFilter"));            
        }

        // Set any tokenizing options.
        if (argOptions.hasOption("stemmingAlgorithm"))
            props.setProperty(IteratorFactory.STEMMER_PROPERTY,
                              argOptions.getStringOption("stemmingAlgorithm"));

        if (argOptions.hasOption("compoundWords")) {
            props.setProperty(
                IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
                              argOptions.getStringOption("compoundWords"));
        }
        if (argOptions.hasOption("wordLimit"))
            props.setProperty(IteratorFactory.TOKEN_COUNT_LIMIT_PROPERTY,
                              argOptions.getStringOption("wordLimit"));

        
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.

        File outputPath = new File(argOptions.getPositionalArg(numArgs - 1));
        File outputFile = null;
        // If the path is a directory, generate the .sspace file name based on
        // the space's name, taking into account any duplicates
        if (outputPath.isDirectory()) {
            outputFile = (overwrite)
                ? new File(outputPath, "temp-fixme-" + EXT)
                : File.createTempFile("temp-fixme-", EXT, outputPath);

        }
        // Otherwise the user has specified a file name directly, which should
        // be used.
        else {
            if (outputPath.exists() && !overwrite) {
                // Find the file's base name and extension in order to generate
                // a unique file name with the same structure
                String name = outputPath.getName();
                int dotIndex = name.lastIndexOf(".");
                String extension = (dotIndex < 0 && dotIndex+1 < name.length())
                    ? "" : name.substring(dotIndex);
                String baseName = name.substring(0, dotIndex);
                // createTempFile has a restriction that the filename must be at
                // least 3 characters.  If it is less, then we need to pad it
                // with random numbers outselves.
                if (baseName.length() < 3)
                    baseName += Math.abs((Math.random() * Short.MAX_VALUE) *10);
                File outputDir = outputPath.getParentFile();
                // If the parent was null, then the file must be being created
                // in the directory from which this class was invoked.  
                if (outputDir == null)
                    outputDir = new File("");
                System.out.println("base dir: " + outputDir);
                outputFile = File.createTempFile(baseName, extension, outputDir);
            }
            else
                outputFile = outputPath;
        }

        System.out.println("output File: " + outputFile);

        SSpaceFormat format = (argOptions.hasOption("outputFormat"))
            ? SSpaceFormat.valueOf(
                argOptions.getStringOption("outputFormat").toUpperCase())
            : getSpaceFormat();

        SemanticSpaceWriter writer = 
            new SemanticSpaceWriter(outputFile, format);

        Collection<String> inputFiles = new LinkedList<String>();
        for (int arg = 0; arg < numArgs - 1; ++arg)
            inputFiles.add(argOptions.getPositionalArg(arg));

        long startTime = System.currentTimeMillis();
        execute(inputFiles, writer);            
        long endTime = System.currentTimeMillis();

        verbose("Computed space in %.3f seconds",
                ((endTime - startTime) / 1000d));

        postProcessing();
    }

    /**
     *
     *
     * @param inputDirs one or more directories on the Hadoop file system which
     *        contain files to be processed
     * @param writer the writer to which the resulting {@link SemanticSpace}
     *        should be written
     *
     * @throws Exception if any error occurs either in Hadoop or the I/O during
     *         the execution of this algorithm
     */
    protected abstract void execute(Collection<String> inputDirs,
                                   SemanticSpaceWriter writer) throws Exception;

    /**
     * Returns a set of terms based on the contents of the provided file.  Each
     * word is expected to be on its own line.
     */
    protected static Set<String> loadValidTermSet(String validTermsFileName) 
        throws IOException {

        Set<String> validTerms = new HashSet<String>();
        BufferedReader br = new BufferedReader(
            new FileReader(validTermsFileName));
        
        for (String line = null; (line = br.readLine()) != null; ) {
            validTerms.add(line);
        }
         
        br.close();

        return validTerms;
    }

    protected void verbose(String msg) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.logp(Level.FINE, getClass().getName(), "verbose", msg);
    }

    protected void verbose(String format, Object... args) {
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.logp(Level.FINE, getClass().getName(), "verbose", 
                        String.format(format, args));
    }
}
