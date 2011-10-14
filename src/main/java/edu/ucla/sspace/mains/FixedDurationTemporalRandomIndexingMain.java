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
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.common.WordComparator;

import edu.ucla.sspace.ri.IndexVectorUtil;

import edu.ucla.sspace.temporal.TemporalSemanticSpace;

import edu.ucla.sspace.text.FileListTemporalDocumentIterator;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.text.OneLinePerTemporalDocumentIterator;
import edu.ucla.sspace.text.TemporalDocument;

import edu.ucla.sspace.tri.FixedDurationTemporalRandomIndexing;
import edu.ucla.sspace.tri.OrderedTemporalRandomIndexing;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.SortedMultiMap;
import edu.ucla.sspace.util.TimeSpan;
import edu.ucla.sspace.util.TreeMultiMap;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The executable class for running {@link FixedDurationTemporalRandomIndexing}
 * from the command-line.
 *
 * @see TemporalRandomIndexing
 * @see RandomIndexing
 * 
 * @author David Jurgens
 */
public class FixedDurationTemporalRandomIndexingMain {

    /**
     * Extension used for all saved semantic space files.
     */
    private static final String EXT = ".sspace";

    /**
     * The logger used for reporting.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(FixedDurationTemporalRandomIndexingMain.class.getName());

    /**
     * The processed argument options available to the main classes.
     */
    private final ArgOptions argOptions;

    /**
     * A set of words for which the temporal semantics will be calculated.
     */
    private final Set<String> interestingWords;

    /**
     * Whether the nearest neighbors for each interesting word should be
     * compared after processing each partition.
     */
    private boolean compareNeighbors;

    /**
     * The format in which the .sspace should be saved
     */
    private SSpaceFormat format;

    /**
     * How many nearest neightbors of the words in {@code interestingWords} to
     * print for each semantic partition.  If this variable is 0, no neighbors are
     * printed.
     */
    private int interestingWordNeighbors;

    /**
     * The directory in which any serialized .sspace files should be saved.
     */
    private File outputDir;

    /**
     * Whether to overwrite existing .sspace files when serializing
     */
    private boolean overwrite;

    /**
     * Whether to print the semantic shifts and other statistics for the
     * interesting word set for each partition.
     */
    private boolean printInterestingTokenShifts; 

    /**
     * Whether to write the incremental {@code .sspace} files to disk during the
     * processing of each time span.
     */
    private boolean savePartitions;

    /**
     * Whether to print a complete sorted list of all the semantic shifts for
     * each interesting word from the last partition.
     */
    private boolean printShiftRankings;

    /**
     * The word comparator used for computing similarity scores when calculating
     * the semantic shift.
     */
    private WordComparator wordComparator;

    /**
     * A mapping from each word to the vectors that account for its temporal
     * semantics according to the specified time span
     */
    private final Map<String,SortedMap<Long,double[]>> wordToTemporalSemantics;


    private FixedDurationTemporalRandomIndexingMain() {
        argOptions = createOptions();
        interestingWords = new HashSet<String>();
        interestingWordNeighbors = 0;
        compareNeighbors = false;
        wordToTemporalSemantics = 
            new HashMap<String,SortedMap<Long,double[]>>();
        savePartitions = false;
        printShiftRankings = false;
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    protected ArgOptions createOptions() {
        ArgOptions options = new ArgOptions();
        options.addOption('f', "fileList", "a list of document files", 
                          true, "FILE[,FILE...]", "Required (at least one of)");
        options.addOption('d', "docFile", 
                          "a file where each line is a document", true,
                          "FILE[,FILE...]", "Required (at least one of)");

        options.addOption('T', "timespan", "the timespan for each semantic " +
                          "partition", true, "Date String", "Required");

        options.addOption('o', "outputFormat", "the .sspace format to use",
                          true, "{text|binary}", "Program Options");
        options.addOption('t', "threads", "the number of threads to use",
                          true, "INT", "Program Options");
        options.addOption('w', "overwrite", "specifies whether to " +
                          "overwrite the existing output", true, "BOOL",
                          "Program Options");
        options.addOption('v', "verbose", "prints verbose output",
                          false, null, "Program Options");

        // Algorithm Options
        options.addOption('i', "vectorGenerator", "IndexVectorGenerator "
                          + "class to use", true,
                          "CLASSNAME", "Algorithm Options");
        options.addOption('l', "vectorLength", "length of semantic vectors",
                          true, "INT", "Algorithm Options");
        options.addOption('n', "permutationFunction", "permutation function "
                          + "to use", true,
                          "CLASSNAME", "Algorithm Options");
        options.addOption('p', "usePermutations", "whether to permute " +
                          "index vectors based on word order", true,
                          "BOOL", "Algorithm Options");
        options.addOption('r', "useSparseSemantics", "use a sparse encoding of "
                          + "semantics to save memory", true,
                          "BOOL", "Algorithm Options");
        options.addOption('s', "windowSize", "how many words to consider " +
                          "in each direction", true,
                          "INT", "Algorithm Options");
        options.addOption('S', "saveVectors", "save word-to-IndexVector mapping"
                          + " after processing", true,
                          "FILE", "Algorithm Options");
        options.addOption('L', "loadVectors", "load word-to-IndexVector mapping"
                          + " before processing", true,
                          "FILE", "Algorithm Options");

        // Input Options
        options.addOption('F', "tokenFilter", "filters to apply to the input " +
                          "token stream", true, "FILTER_SPEC", 
                          "Tokenizing Options");
        options.addOption('C', "compoundWords", "a file where each line is a " +
                          "recognized compound word", true, "FILE", 
                          "Tokenizing Options");

        options.addOption('W', "semanticFilter", "exclusive list of word",
                          true, "FILE", "Input Options");

        // Output Options
        options.addOption('I', "interestingTokenList", "list of interesting " +
                          "words", true, "FILE", "Output Options");
        options.addOption('K', "printShiftRankings", "print ranked list of " +
                          "semantic shifts for each interesting word", false, 
                          null, "Output Options");
        options.addOption('R', "savePartitions", "write semantic partitions as " +
                          ".sspace files to disk", false, null, 
                          "Output Options");
        options.addOption('P', "printInterestingTokenShifts", "prints the "
                          + "vectors for each interesting word", false, null, 
                          "Output Options");
        options.addOption('N', "printInterestingTokenNeighbors", "prints the "
                          + "nearest neighbors for each interesting word", true,
                          "INT", "Output Options");
        options.addOption('Z', "printInterestingTokenNeighborComparison",
                          "prints the distances between each of the"
                          + "nearest neighbors for each interesting word", 
                          false, null , "Output Options");

        return options;
    }

    public static void main(String[] args) {
        try {
            FixedDurationTemporalRandomIndexingMain main = 
                new FixedDurationTemporalRandomIndexingMain();
            main.run(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        argOptions.parseOptions(args);
        
        if (argOptions.numPositionalArgs() == 0) {
            throw new IllegalArgumentException("must specify output directory");
        }

        outputDir = new File(argOptions.getPositionalArg(0));
        if (!outputDir.isDirectory()){
            throw new IllegalArgumentException(
                "output directory is not a directory: " + outputDir);
        }

        if (!argOptions.hasOption("timespan")) {
            throw new IllegalArgumentException(
                "must specify a timespan duration for the semantic partition");
        }

        // Get the time span that will be used to group the documents
        String timespanStr = argOptions.getStringOption("timespan");
        TimeSpan timeSpan = new TimeSpan(timespanStr);

        if (argOptions.hasOption('v') || argOptions.hasOption("verbose")) {
            // Enable all the logging at the FINE level for the application
            Logger appRooLogger = Logger.getLogger("edu.ucla.sspace");
            Handler verboseHandler = new ConsoleHandler();
            verboseHandler.setLevel(Level.FINE);
            appRooLogger.addHandler(verboseHandler);
            appRooLogger.setLevel(Level.FINE);
            appRooLogger.setUseParentHandlers(false);
        }
        
        // all the documents are listed in one file, with one document per line
        Iterator<TemporalDocument> docIter = null;
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
        Collection<Iterator<TemporalDocument>> docIters = 
            new LinkedList<Iterator<TemporalDocument>>();

        if (fileList != null) {
            String[] fileNames = fileList.split(",");
            // we have a file that contains the list of all document files we
            // are to process
            for (String s : fileNames) {
                docIters.add(new FileListTemporalDocumentIterator(s));
            }
        }
        if (docFile != null) {
            String[] fileNames = docFile.split(",");
            // all the documents are listed in one file, with one document per
            // line
            for (String s : fileNames) {
                docIters.add(new OneLinePerTemporalDocumentIterator(s));
            }
        }

        // combine all of the document iterators into one iterator.
        docIter = new CombinedIterator<TemporalDocument>(docIters);
        
        int numThreads = Runtime.getRuntime().availableProcessors();
        if (argOptions.hasOption("threads")) {
            numThreads = argOptions.getIntOption("threads");
        }

        // initialize the word comparator based on the number of threads
        wordComparator = new WordComparator(numThreads);

        overwrite = true;
        if (argOptions.hasOption("overwrite")) {
            overwrite = argOptions.getBooleanOption("overwrite");
        }

        // If the user specified a list of interesting words, load in the set to
        // filter out which semantics shifts are actually tracked
        if (argOptions.hasOption("interestingTokenList")) {
            String fileName = argOptions.getStringOption("interestingTokenList");
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            for (String line = null; (line = br.readLine()) != null; ) {
                for (String s : line.split("\\s+")) {
                    interestingWords.add(s);
                    wordToTemporalSemantics.put(s,new TreeMap<Long,double[]>());
                }
            }
            LOGGER.info("loaded " + interestingWords.size() + 
                        " interesting words");
        }

        // Check whether the incremental .sspace files should be written to disk
        if (argOptions.hasOption("savePartitions"))
            savePartitions = true;
        
        // Check wether each partition should generate a ranked list of words
        // according to their semantic shift
        if (argOptions.hasOption("printShiftRankings"))
            printShiftRankings = true;

        // if the user did not indicate any interesting words, and the .sspace
        // files are not being written, then the program has no output, which is
        // an error
        else if (interestingWords.isEmpty()) {
            throw new IllegalArgumentException(
                "Must specify some form of output as either a non-empty set" +
                "of interesting words and/or writing the semantic partition .sspace"
                + "files to disk");
        }

        // Check whether any interesting-word-output is enabled
        if (argOptions.hasOption("printInterestingTokenNeighbors")) {
            interestingWordNeighbors = 
                argOptions.getIntOption("printInterestingTokenNeighbors");        
        }
        if (argOptions.hasOption("printInterestingTokenShifts")) {
            printInterestingTokenShifts = true;
                LOGGER.info("Recording interesting token shifts");
        }
        if (argOptions.hasOption("printInterestingTokenNeighborComparison")) {
            compareNeighbors = true;
        }
        
        
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = setupProperties();

        
        FixedDurationTemporalRandomIndexing fdTri = 
            new FixedDurationTemporalRandomIndexing(props); 

        // The user may also specify a limit to the words for which semantics
        // are computed.  If so, set up Random Indexing to not keep semantics
        // for those words.
        if (argOptions.hasOption("semanticFilter")) {
            String fileName = argOptions.getStringOption("semanticFilter");
            BufferedReader br = new BufferedReader(new FileReader(fileName));
            Set<String> wordsToCompute = new HashSet<String>();
            for (String line = null; (line = br.readLine()) != null; ) {
                for (String s : line.split("\\s+")) {
                    wordsToCompute.add(s);
                }
            }
            LOGGER.info("computing semantics for only " + wordsToCompute.size()
                        + " words");

            fdTri.setSemanticFilter(wordsToCompute);
        }

        // Load the word-to-IndexVector mappings if they were specified.
        if (argOptions.hasOption("loadVectors")) {
            String fileName = argOptions.getStringOption("loadVectors");
            LOGGER.info("loading index vectors from " + fileName);
            Map<String,TernaryVector> wordToIndexVector = 
                IndexVectorUtil.load(new File(fileName));
            fdTri.setWordToIndexVector(wordToIndexVector);
        }
        
        String formatName = (argOptions.hasOption("outputFormat"))
            ? argOptions.getStringOption("outputFormat").toUpperCase()
            : "TEXT";
        
        format = SSpaceFormat.valueOf(formatName.toUpperCase());

        parseDocumentsMultiThreaded(fdTri, docIter, timeSpan, numThreads);

        long startTime = System.currentTimeMillis();
        fdTri.processSpace(props);
        long endTime = System.currentTimeMillis();
        LOGGER.info(String.format("processed space in %.3f seconds%n",
                                   ((endTime - startTime) / 1000d)));
        
        // save the word-to-IndexVector mapping if specified to do so
        if (argOptions.hasOption("saveVectors")) {
            String fileName = argOptions.getStringOption("saveVectors");
            LOGGER.info("saving index vectors to " + fileName);
            IndexVectorUtil.save(fdTri.getWordToIndexVector(), 
                                 new File(fileName));
        }
    }

    /**
     * Prints the semantic space to file, inserting the tag into the .sspace
     * file name
     */
    private void printSpace(SemanticSpace sspace, String tag) {
        try {
            String EXT = ".sspace";
            File output = (overwrite)
                ? new File(outputDir, sspace.getSpaceName() + tag + EXT)
                : File.createTempFile(sspace.getSpaceName() + tag, EXT, 
                                      outputDir);
            
            long startTime = System.currentTimeMillis();
            SemanticSpaceIO.save(sspace, output, format);
            long endTime = System.currentTimeMillis();
            verbose("printed space in %.3f seconds%n",
                    ((endTime - startTime) / 1000d));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Adds the temporal semantics for each interesting word using the provided
     * semantic partition.
     *
     * @param currentSemanticPartitionStartTime the start time of the semantic
     *        partition
     */
    private void updateTemporalSemantics(long currentSemanticPartitionStartTime,
                                         SemanticSpace semanticPartition) {
        
        // Pre-allocate the zero vector so that if multiple interesting words
        // are not present in the space, they all point to the same zero
        // semantics
        double[] zeroVector = new double[semanticPartition.getVectorLength()];

        for (String word : interestingWords) {
            // update the vectors
            SortedMap<Long,double[]> temporalSemantics = 
                wordToTemporalSemantics.get(word);
            Vector v = semanticPartition.getVector(word);
            // If the wor was not present in the current partition, then just
            // use the zero vector.  Otherwise, use it distirbution.
            double[] semantics = (v == null) 
                ? zeroVector
                : Vectors.asDouble(v).toArray();

            temporalSemantics.put(currentSemanticPartitionStartTime,
                                  semantics);
        }
    }

    /**
     * Prints the semantic shifts for all the words in the {@link
     * #wordToTemporalSemantics} map, using the {code dateString} for naming the
     * output file with the date of the last semantic partition.
     *
     * @param dateString the date of the last semantic partition.
     */
    private void printSemanticShifts(String dateString) throws IOException {

        LOGGER.fine("Writing semantic shifts for " + dateString);

        // Once we have all the vectors for each word in each sspace,
        // calculate how much the vector has changed.
        for (Map.Entry<String,SortedMap<Long,double[]>> e : 
                 wordToTemporalSemantics.entrySet()) {
            
            String word = e.getKey();
            SortedMap<Long,double[]> timeStampToSemantics = e.getValue();
            Iterator<Map.Entry<Long,double[]>> it = 
                timeStampToSemantics.entrySet().iterator();
            
            PrintWriter pw = new PrintWriter(new File(outputDir,
                word + "." + dateString + ".temporal-changes.txt"));            
            
            // Write the header so we can keep track of what all the columns
            // mean
            pw.println("#time\ttime-delay\tcosineSim\tcosineAngle\tEuclidean"+
                       "\tchange-in-magnitde\tmagnitde\tprev-magnitude");
            Map.Entry<Long,double[]> last = null;
            while (it.hasNext()) {
                Map.Entry<Long,double[]> cur = it.next();
                if (last != null) {
                    long timeDelay = cur.getKey() - last.getKey();
                    double euclideanDist = Similarity.
                            euclideanDistance(cur.getValue(), last.getValue());
                    double cosineSim = Similarity.
                        cosineSimilarity(cur.getValue(), last.getValue());
                    double cosineAngle = Math.acos(cosineSim);
                    
                    double oldMag = getMagnitude(last.getValue());
                    double newMag = getMagnitude(cur.getValue());
                    
                    pw.println(cur.getKey() + "\t" + timeDelay + "\t" + 
                               cosineSim + "\t" + cosineAngle + "\t" + 
                               euclideanDist + "\t" + (newMag - oldMag) + "\t" 
                               + newMag + "\t" + oldMag);
                }
                last = cur;
            }
            pw.close();
        }
    }

    /**
     * Computes the ranking of which words underwent the most dramatic shifts in
     *  the most recent partition and then prints the ranking list of a file.
     *
     * @param dateString the string to use when indiciation which partition is
     *        having its ranking lists printed.  This string becomes a part of
     *        the file name.
     */
    private void printShiftRankings(String dateString, 
                                    long startOfMostRecentPartition,
                                    TimeSpan partitionDuration) 
            throws IOException {

        SortedMultiMap<Double,String> shiftToWord = 
            new TreeMultiMap<Double,String>();

        // Create a second time span than is twice the duration.  We will use
        // this to check whether two partition's vectors were adjacent in the
        // slice by seeing wether the timestamps fall within this duration
        TimeSpan twoPartitions = new TimeSpan(partitionDuration.getYears() * 2,
                                              partitionDuration.getMonths() * 2,
                                              partitionDuration.getWeeks() * 2,
                                              partitionDuration.getDays() * 2,
                                              partitionDuration.getHours() * 2);
        
        // Once we have all the vectors for each word in each sspace,
        // calculate how much the vector has changed.
        for (Map.Entry<String,SortedMap<Long,double[]>> e : 
                 wordToTemporalSemantics.entrySet()) {
            String word = e.getKey();
            SortedMap<Long,double[]> m = e.getValue();
            
            // Skip computing shifts for words without enough partitions
            if (m.size() < 2)
                continue;

            // Get the timestamps as a navigable map so we can identify the last
            // two keys in it more easly.
            NavigableMap<Long,double[]> timestampToVector = 
                (e instanceof NavigableMap) 
                ? (NavigableMap<Long,double[]>)m
                : new TreeMap<Long,double[]>(m);            

            Map.Entry<Long,double[]> mostRecent = timestampToVector.lastEntry();
            // Skip calculating the shift for words who most recent partition
            // was not the same as the most recent partition for TRI
            if (!mostRecent.getKey().equals(startOfMostRecentPartition))
                continue;
            
            Map.Entry<Long,double[]> secondMostRecent = 
                timestampToVector.lowerEntry(mostRecent.getKey());
            // Skip calculating the shift for words where the two most recent
            // partitoins aren't contiguous.  Check for this using the custom
            // time span that covers two partitions
            if (!twoPartitions.insideRange(secondMostRecent.getKey(),
                                           mostRecent.getKey()))
                continue;

            
            // Compute the semantic shift of the two partitions
            shiftToWord.put(Similarity.cosineSimilarity(
                            secondMostRecent.getValue(),
                            mostRecent.getValue()), word);
        }

        PrintWriter pw = new PrintWriter(new File(outputDir,
            "shift-ranks-for." + dateString + ".txt"));
        for (Map.Entry<Double,String> e : shiftToWord.entrySet()) {
            pw.println(e.getKey() + "\t" + e.getValue());
        }        
        pw.close();
    }

    /**
     * Using the {@link wordToTemporalSemantics} set and input parameters,
     * calculates the shift in each word's semantic vector per recorded time
     * period and also prints out the nearest neighbors to each word for each
     * time period.
     *
     * @param dateString the string that encodes the date of the semantic partition.
     *        This will be used as a part of the file name to indicate when the
     *        shifts occurred.
     * @param semanticPartition the current semantic that will be used to identify
     *        the neighbors of each interesting word
     */
    private void printWordNeighbors(String dateString,
                                    SemanticSpace semanticPartition) 
            throws IOException {
        
        LOGGER.info("printing the most similar words for the semantic partition" +
                    " starting at: " + dateString);

        // generate the similarity lists
        for (String toExamine : interestingWords) {
            SortedMultiMap<Double,String> mostSimilar = 
                wordComparator.getMostSimilar(toExamine, semanticPartition,
                                              interestingWordNeighbors,
                                              Similarity.SimType.COSINE);

            if (mostSimilar != null) {
                File neighborFile = 
                    new File(outputDir, toExamine + "-" + dateString + ".txt");
                neighborFile.createNewFile(); // iff it doesn't already exist
                
                File neighborComparisonFile = new File(outputDir,
                    toExamine + "_neighbor-comparisons_" + dateString + ".txt");
                neighborComparisonFile.createNewFile(); // see above comment
                    

                PrintWriter pw = new PrintWriter(neighborFile);
                for (String similar : mostSimilar.values()) {
                    pw.println(similar);
                }
                pw.close();

                if (compareNeighbors) {                                
                    // Print an N x N comparison between all of the most similar
                    // words.  This gives an indication of whether any of the
                    // words might be outliers.
                    writeNeighborComparison(neighborComparisonFile, 
                                            mostSimilar, semanticPartition);
                }
            }
        }    
    }

    /**
     * Write to the {@code neighborFile} the previously computed results of an N
     * x N similarity comparison of all the neighbors for word.
     *
     * @param neighborFile the file to which the results of the comparison
     *        should be written
     * @param mostSimilar a mapping from the similarity value to the neighbors
     *        of an interesting word that have the value.
     * @param sspace the semantic
     */
    private static void 
        writeNeighborComparison(File neighborFile, 
                                MultiMap<Double,String> mostSimilar, 
                                SemanticSpace sspace) throws IOException {        

        PrintWriter pw = new PrintWriter(neighborFile);
        
        // print out the header so we know the comparison order
        StringBuffer sb = new StringBuffer(mostSimilar.size() * 10);
        for (Iterator<String> it = mostSimilar.values().iterator();
             it.hasNext();) {
            sb.append(it.next());
            if (it.hasNext()) 
                sb.append(" ");
        }
        pw.println(sb.toString());
        
        // create an N x N table of how similar all the words are to each
        // other.
        for (String word : mostSimilar.values()) {
            sb = new StringBuffer(mostSimilar.size() * 10);
            sb.append(word).append(" ");
            
            // loop through all of the words
            for (String other : mostSimilar.values()) {
                // determine how similar the two words are
                double similarity = Similarity.cosineSimilarity(
                        sspace.getVector(word),
                        sspace.getVector(other));
                sb.append(similarity).append(" ");
            }
            pw.println(sb.toString());
        }
        
        pw.close();
    }

    /**
     * Returns the {@code Properties} used to set up the semantic space.
     */
    protected Properties setupProperties() {
        Properties props = System.getProperties();

        // Use the command line options to set the desired properites in the
        // constructor.  Use the system properties in case these properties were
        // set using -Dprop=<value>
        if (argOptions.hasOption("usePermutations")) {
            props.setProperty(
                OrderedTemporalRandomIndexing.USE_PERMUTATIONS_PROPERTY,
                              argOptions.getStringOption("usePermutations"));
        }

        if (argOptions.hasOption("permutationFunction")) {
            props.setProperty(
                OrderedTemporalRandomIndexing.PERMUTATION_FUNCTION_PROPERTY,
                             argOptions.getStringOption("permutationFunction"));
        }

        if (argOptions.hasOption("windowSize")) {
            props.setProperty(
                OrderedTemporalRandomIndexing.WINDOW_SIZE_PROPERTY,
                             argOptions.getStringOption("windowSize"));
        }

        if (argOptions.hasOption("vectorLength")) {
            props.setProperty(
                OrderedTemporalRandomIndexing.VECTOR_LENGTH_PROPERTY,
                             argOptions.getStringOption("vectorLength"));
        }
        
        if (argOptions.hasOption("useSparseSemantics")) {
            props.setProperty(
                OrderedTemporalRandomIndexing.USE_SPARSE_SEMANTICS_PROPERTY,
                              argOptions.getStringOption("useSparseSemantics"));
        }

        if (argOptions.hasOption("partitionDuration")) {
            props.setProperty(FixedDurationTemporalRandomIndexing.
                              SEMANTIC_PARTITION_DURATION_PROPERTY,
                              argOptions.getStringOption("partitionDuration"));
        }
        
        // Initialize the IteratorFactory to tokenize the documents according to
        // the specified configuration (e.g. filtering, compound words)
        if (argOptions.hasOption("tokenFilter")) {
            props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
                              argOptions.getStringOption("tokenFilter"));            
        }

        if (argOptions.hasOption("compoundTokens")) {
            props.setProperty(IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
                              argOptions.getStringOption("compoundTokens"));
        }

        return props;
    }


    private static double getMagnitude(double[] arr) {
        double mag = 0d;
        for (double d : arr) {
            if (d != 0)
                mag += d*d;
        }
        return Math.sqrt(mag);
    }

    /**
     * Calls {@link TemporalSemanticSpace#processDocument(BufferedReader,long)
     * processDocument} once for every document in {@code docIter} using a the
     * specified number thread to call {@code processSpace} on the {@code
     * TemporalSemanticSpace} instance.
     *
     * @param sspace the space to build
     * @param docIter an iterator over all the documents to process
     * @param numThreads the number of threads to use
     */
    protected void parseDocumentsMultiThreaded(
        final FixedDurationTemporalRandomIndexing fdTri, 
        final Iterator<TemporalDocument> docIter,
        final TimeSpan timeSpan, int numThreads)
            throws IOException, InterruptedException {

        Collection<Thread> processingThreads = new LinkedList<Thread>();

        final AtomicInteger count = new AtomicInteger(0);

        final AtomicLong curSSpaceStartTime = new AtomicLong();
        final Object calendarLock = new Object();
        final DateFormat df = new SimpleDateFormat("yyyy_MM_ww_dd_hh");

        final AtomicLong lastWriteTime = new AtomicLong();
        
        // barrier for setting up the initial time stamp based on the first
        // document processed
        final AtomicBoolean startBarrier = new AtomicBoolean(false);

        // Before a Thread blocks waiting for s-space serialization, it enqueues
        // the time for its next document (outside the time-span).  These times
        // are used to select the start time for the next s-sspace.
        final Queue<Long> futureStartTimes = new ConcurrentLinkedQueue<Long>();

        // final variables necessary due to the anonymous inner class
        final boolean writeSemanticPartitions = savePartitions;
        final boolean writeSemanticShifts = printInterestingTokenShifts;
        final boolean writeInterestingWordNeighbors = 
            interestingWordNeighbors > 0;
        final boolean writeShiftRankings = printShiftRankings;

        /**
         * A runnable that serializes the current semantic space to disk and
         * annotates it with the time at which the space started.
         */
        Runnable serializeTimeSpan = new Runnable() {
                public void run() {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(curSSpaceStartTime.get());
                    String dateString = df.format(c.getTime());
                                    
                    try {

                        // Save the s-space only when requried to.  This
                        // operation can be very slow due to I/O requirements,
                        // and is not mandatory when computing the shifts
                        if (writeSemanticPartitions) {
                            LOGGER.info("writing semantic partition starting " +
                                        "at: " + dateString);
                            // save the current contets of the semantic space
                            printSpace(fdTri, "-" + dateString);
                        }

                        // Add the semantics from the current semantic partition
                        // for each of the interesting words
                        updateTemporalSemantics(curSSpaceStartTime.get(),
                                                fdTri);
                        if (writeSemanticShifts) 
                            printSemanticShifts(dateString);

                        if (writeShiftRankings) 
                            printShiftRankings(dateString, 
                                               curSSpaceStartTime.get(),
                                               timeSpan);

                        // NOTE: since the FD-TRI implementaiton resets
                        // its semantics after every 
                        if (interestingWordNeighbors > 0) 
                            printWordNeighbors(dateString, fdTri);

                    } catch (IOException ioe) {
                        // rethrow
                        throw new IOError(ioe);
                    }

                    // Pick the earlier start time available as the new starting
                    // time for the s-space
                    assert futureStartTimes.size() > 0;
                    Long ssStart = new TreeSet<Long>(futureStartTimes).first();
                    futureStartTimes.clear();

                    // last update the date with the new time
                    curSSpaceStartTime.set(ssStart);
                }
            };
            
        // barrier for document processing threads.  When their next document is
        // outside of the time range, the immediately increase the release on
        // this semaphore and lock on the an object while the serialization
        // thread writes out the current time span's .sspace
        final CyclicBarrier exceededTimeSpanBarrier =
            new CyclicBarrier(numThreads, serializeTimeSpan);
                    
        
        for (int i = 0; i < numThreads; ++i) {

            Thread processingThread = new Thread() {
                    
                    public void run() {
                        // repeatedly try to process documents while some still
                        // remain
                        while (docIter.hasNext()) {
                            
                            TemporalDocument doc = docIter.next();
                            int docNumber = count.incrementAndGet();
                            long docTime = doc.timeStamp();

                            // special case for first document
                            if (docNumber == 1) {
                                curSSpaceStartTime.set(docTime);
                                startBarrier.set(true);
                            }
                            
                            // Spin until the Thread with the first document
                            // sets the initial starting document time.  Note
                            // that we spin here instead of block, because this
                            // is expected that another thread will immediately
                            // set this and so it will be a quick no-op
                            while (startBarrier.get() == false)
                                ;

                            // Check whether the time for this document would
                            // exceed the maximum time span for any TRI
                            // partition.  Loop to ensure that if this thread
                            // does loop and another thread has an earlier time
                            // that would cause this thread's time span to
                            // exceeds the other thread's time period, then this
                            // thread will block and loop again.
                            while (!timeSpan.insideRange(
                                   curSSpaceStartTime.get(), docTime)) {
                                try {
                                    // notify the barrier that this Thread is
                                    // now processing a document in the next
                                    // time span and so the serialization thread
                                    // should write the .sspace to disk.  In
                                    // addition, enqueue the time for this
                                    // document so the serialization thread can
                                    // reset the correct s-sspace start time
                                    futureStartTimes.offer(docTime);
                                    exceededTimeSpanBarrier.await();
                                } catch (InterruptedException ex) {
                                    return;
                                } catch (BrokenBarrierException ex) {
                                    return;
                                }
                            }

                            try {
                                fdTri.processDocument(doc.reader());
                            } catch (IOException ioe) {
                                // rethrow
                                throw new IOError(ioe);
                            }
                            LOGGER.fine("parsed document #" + docNumber);
                        }
                    }
                };
            processingThreads.add(processingThread);
        }

        long threadStart = System.currentTimeMillis();
        
        // start all the threads processing
        for (Thread t : processingThreads)
            t.start();

        verbose("Beginning processing using %d threads", numThreads);

        // wait until all the documents have been parsed
        for (Thread t : processingThreads)
            t.join();

        verbose("parsed %d document in %.3f total seconds)%n",
                count.get(),
                ((System.currentTimeMillis() - threadStart) / 1000d));
    }
        
    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    protected void usage() {
         System.out.println(
             "usage: java FixedDurationTemporalRandomIndexingMain [options] " +
            "<output-dir>\n\n" + 
            argOptions.prettyPrint() +

            "\nFixed-Duration TRI provides four main output options:\n\n" +

            "  1) Outputting each semantic partition as a separate .sspace file.  "+
            "Each file\n     is named using the yyyy_MM_ww_dd_hh format to " +
            "indicate it start date.\n     This is the most expensive of the " +
            "operations due to I/O overhead.\n\n" +
            
            "  The remaining options require the use of the -I " + 
            "--interestingTokenList option to\n  specify a set of word for use"+
            " in tracking temporal changes.\n\n  2) For each of the interesting"
            + "words, -P, --printInterestingTokenShifts will track\n" +
            "     the semantics" +
            " through time and report the semantic shift along with other\n" +
            "     distance statistics.\n\n"  +
            "  3) For each of the interesting words, -N, " +
            "--printInterestingTokenNeighbors\n     will print the nearest " +
            "neighbor for each in the semantic space.  The\n     number " +
            "of neighbors to print should be specified.\n\n" +

            "  4) For each of the interesting words, generate the list of " + 
            "similar\n     neighbors using the --printInterestingTokenNeighbors"
            + " and then compare\n     those neighbors with each other using " +
            "the\n     --printInterestingTokenNeighborComparison option.  " +
            "This creates a file\n     with the pair-wise cosine similarities "+
            "for all neighbors.  Note that this\n     option requires both " +
            "flags to be specified.\n\n" +

            "Semantic filters limit the set of tokens for which the " +
            "semantics are kept.\nThis limits the potential memory overhead " +
            "for calculating semantics for a\nlarge set of words."

            + "\n\n" + OptionDescriptions.COMPOUND_WORDS_DESCRIPTION
            + "\n\n" + OptionDescriptions.TOKEN_FILTER_DESCRIPTION
            + "\n\n" + OptionDescriptions.FILE_FORMAT_DESCRIPTION
            + "\n\n" + OptionDescriptions.HELP_DESCRIPTION);            
    }

    protected void verbose(String msg) {        
        LOGGER.fine(msg);
    }

    protected void verbose(String format, Object... args) {
        if (LOGGER.isLoggable(Level.FINE)) 
            LOGGER.fine(String.format(format, args));
    }
}
