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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

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
 * A base class for running {@link SemanticSpace} algorithms.  All derived main
 * classes must implement the abstract functions.  Derived classes have the
 * option of adding more command line options, which can then be handled
 * independently by the derived class to build the SemanticSpace correctly, or
 * produce the Properties object required for processing the space.
 *
 * All mains which inherit from this class will automatically have the ability
 * to process the documents in parallel, and from a variety of file sources.
 * The provided command line arguments are as follows:
 *
 * <ul>
 * <li> <u>Document sources (must provide one)</u>
 *   <ul>
 *
 *   <li> {@code -d}, {@code --docFile=FILE[,FILE...]}  a file containing a list
 *        of file names, each of which is treated as a separate document.
 *
 *   <li> {@code -f}, {@code --fileList=FILE[,FILE...]} a file where each line
 *        is treated as a separate document.  This is the preferred option when
 *        working with large corpora due to reduced I/O demands for multiple
 *        files.
 *
 *   </ul>
 *
 * <li> <u>Program Options</u>
 *
 *   <ul> 
 *
 *   <li> {@code -o}, {@code --outputFormat=}<tt>text|binary}</tt> Specifies the
 *        output formatting to use when generating the semantic space ({@code
 *        .sspace}) file.  See {@link SemanticSpaceIO} for format details.
 *
 *   <li> {@code -t}, {@code --threads=INT} how many threads to use when
 *        processing the documents.  The default is one per core.
 * 
 *   <li> {@code -w}, {@code --overwrite=BOOL} specifies whether to overwrite
 *        the existing output files.  The default is {@code true}.  If set to
 *        {@code false}, a unique integer is inserted into the file name.
 *
 *   <li> {@code -v}, {@code --verbose}  specifies whether to print runtime
 *        information to standard out
 *
 *   </ul>
 * </ul>
 *
 * @author David Jurgens
 */
public abstract class GenericMain {

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

    /**
     * Whether the {@link SemanticSpace} class is capable of running with
     * multiple threads.
     */
    protected final boolean isMultiThreaded;

    public GenericMain() {
        this(true);
    }

    public GenericMain(boolean isMultiThreaded) {
        this.isMultiThreaded = isMultiThreaded;
        argOptions = setupOptions();
        verbose = false;
    }

    /**
     * Returns the {@link SemanticSpace} that will be used for processing.  This
     * method is guaranteed to be called after the command line arguments have
     * been parsed, so the contents of {@link #argOptions} are valid.
     */
    abstract protected SemanticSpace getSpace();

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
            + " [options] <output-dir>\n"
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

        // Add input file options.
        options.addOption('f', "fileList", "a list of document files", 
                          true, "FILE[,FILE...]", "Required (at least one of)");
        options.addOption('d', "docFile", 
                          "a file where each line is a document", true,
                          "FILE[,FILE...]", "Required (at least one of)");
         options.addOption('X', "docLimit",
                           "The maximum number of documents from the corpus " + 
                           "to use (default: infinit)",
                           true, "INT", "Program Options");

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
                          "an document can return",
                          true, "INT", "Tokenizing Options");

        addExtraOptions(options);
        return options;
    }

    /**
     * Returns the iterator for all of the documents specified on the command
     * line or throws an {@code Error} if no documents are specified.
     * Subclasses should override this method if they provide document input by
     * some other manner than a file list or document list.
     *
     * @throws Error if no document source is specified
     */
    protected Iterator<Document> getDocumentIterator() throws IOException {
        // Check to see if a corpus reader was specified as a property.  If it
        // was, this reader overrides any command line arguments.
        String readerProperty = 
            System.getProperties().getProperty(CORPUS_READER_PROPERTY);
        if (readerProperty != null)
            return ReflectionUtil.getObjectInstance(readerProperty);

        Iterator<Document> docIter = null;

        String fileList = (argOptions.hasOption("fileList"))
            ? argOptions.getStringOption("fileList")
            : null;

        String docFile = (argOptions.hasOption("docFile"))
            ? argOptions.getStringOption("docFile")
            : null;
        if (fileList == null && docFile == null) {
            throw new Error("must specify document sources");
        }

        // Second, determine where the document input sources will be coming
        // from.
        Collection<Iterator<Document>> docIters = 
            new LinkedList<Iterator<Document>>();

        if (fileList != null) {
            String[] fileNames = fileList.split(",");
            // we have a file that contains the list of all document files we
            // are to process
            for (String s : fileNames) {
                docIters.add(new FileListDocumentIterator(s));
            }
        }
        if (docFile != null) {
            String[] fileNames = docFile.split(",");
            // all the documents are listed in one file, with one document per
            // line
            for (String s : fileNames) {
                docIters.add(new OneLinePerDocumentIterator(s));
            }
        }

        // combine all of the document iterators into one iterator.
        docIter = new CombinedIterator<Document>(docIters);

        // Return a limited iterator if requested.
        if (argOptions.hasOption("docLimit"))
            return new LimitedIterator<Document>(
                    docIter, argOptions.getIntOption("docLimit"));

        // Otherwise return the standard iterator.
        return docIter;
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

        // all the documents are listed in one file, with one document per line
        Iterator<Document> docIter = getDocumentIterator();
        
        // Check whether this class supports mutlithreading when deciding how
        // many threads to use by default
        int numThreads = (isMultiThreaded)
            ? Runtime.getRuntime().availableProcessors()
            : 1;
        if (argOptions.hasOption("threads")) {
            numThreads = argOptions.getIntOption("threads");
        }

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
            props.setProperty(IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
                              argOptions.getStringOption("compoundWords"));
        }
        if (argOptions.hasOption("wordLimit"))
            props.setProperty(IteratorFactory.TOKEN_COUNT_LIMIT_PROPERTY,
                              argOptions.getStringOption("wordLimit"));

        IteratorFactory.setProperties(props);

        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.

        SemanticSpace space = getSpace(); 
        
        processDocumentsAndSpace(space, docIter, numThreads, props);

        File outputPath = new File(argOptions.getPositionalArg(0));
        File outputFile = null;
        // If the path is a directory, generate the .sspace file name based on
        // the space's name, taking into account any duplicates
        if (outputPath.isDirectory()) {
            outputFile = (overwrite)
                ? new File(outputPath, space.getSpaceName() + EXT)
                : File.createTempFile(space.getSpaceName(), EXT, outputPath);

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

        long startTime = System.currentTimeMillis();
        SemanticSpaceIO.save(space, outputFile, format);
        long endTime = System.currentTimeMillis();

        verbose("printed space in %.3f seconds",
                ((endTime - startTime) / 1000d));

        postProcessing();
    }

    /**
     * Processes all the documents held by the iterator and process the space.
     */
    protected void processDocumentsAndSpace(SemanticSpace space,
                                            Iterator<Document> docIter,
                                            int numThreads,
                                            Properties props) throws Exception {
        parseDocumentsMultiThreaded(space, docIter, numThreads);

        long startTime = System.currentTimeMillis();
        space.processSpace(props);
        long endTime = System.currentTimeMillis();
        verbose("processed space in %.3f seconds",
                ((endTime - startTime) / 1000d));
    }
        
    /**
     * Calls {@link SemanticSpace#processDocument(BufferedReader)
     * processDocument} once for every document in {@code docIter} using a
     * single thread to interact with the {@code SemanticSpace} instance.
     *
     * @param sspace the space to build
     * @param docIter an iterator over all the documents to process
     */
    protected void parseDocumentsSingleThreaded(SemanticSpace sspace,
                                                Iterator<Document> docIter)
        throws IOException {

        long processStart = System.currentTimeMillis();
        int count = 0;

        while (docIter.hasNext()) {
            long startTime = System.currentTimeMillis();
            Document doc = docIter.next();
            int docNumber = ++count;
            int terms = 0;
            sspace.processDocument(doc.reader());
            long endTime = System.currentTimeMillis();
            verbose("processed document #%d in %.3f seconds",
                    docNumber, ((endTime - startTime) / 1000d));
        }

        verbose("Processed all %d documents in %.3f total seconds",
                count,
                ((System.currentTimeMillis() - processStart) / 1000d));            
    }

    /**
     * Calls {@link SemanticSpace#processDocument(BufferedReader)
     * processDocument} once for every document in {@code docIter} using a the
     * specified number thread to call {@code processSpace} on the {@code
     * SemanticSpace} instance.
     *
     * @param sspace the space to build
     * @param docIter an iterator over all the documents to process
     * @param numThreads the number of threads to use
     */
    protected void parseDocumentsMultiThreaded(final SemanticSpace sspace,
                                               final Iterator<Document> docIter,
                                               int numThreads)        
        throws IOException, InterruptedException {

        Collection<Thread> threads = new LinkedList<Thread>();

        final AtomicInteger count = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; ++i) {
            Thread t = new Thread() {
                public void run() {
                    // repeatedly try to process documents while some still
                    // remain
                    while (docIter.hasNext()) {
                        long startTime = System.currentTimeMillis();
                        Document doc = docIter.next();
                        int docNumber = count.incrementAndGet();
                        int terms = 0;
                        try {
                            sspace.processDocument(doc.reader());
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        long endTime = System.currentTimeMillis();
                        verbose("parsed document #%d in %.3f seconds",
                                docNumber, ((endTime - startTime) / 1000d));
                    }
                }
            };
            threads.add(t);
        }

        long processStart = System.currentTimeMillis();
        
        // start all the threads processing
        for (Thread t : threads)
            t.start();

        verbose("Beginning processing using %d threads", numThreads);

        // wait until all the documents have been parsed
        for (Thread t : threads)
            t.join();

        verbose("Processed all %d documents in %.3f total seconds",
                count.get(),
                ((System.currentTimeMillis() - processStart) / 1000d));            
    }

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
