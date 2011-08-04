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

import edu.ucla.sspace.text.WordIterator;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;

import edu.ucla.sspace.evaluation.NormedWordPrimingReport;
import edu.ucla.sspace.evaluation.NormedWordPrimingTest;
import edu.ucla.sspace.evaluation.WordChoiceEvaluation;
import edu.ucla.sspace.evaluation.WordChoiceEvaluationRunner;
import edu.ucla.sspace.evaluation.WordChoiceReport;
import edu.ucla.sspace.evaluation.WordSimilarityEvaluation;
import edu.ucla.sspace.evaluation.WordSimilarityEvaluationRunner;
import edu.ucla.sspace.evaluation.WordSimilarityReport;
import edu.ucla.sspace.evaluation.WordPrimingReport;
import edu.ucla.sspace.evaluation.WordPrimingTest;

import edu.ucla.sspace.util.LoggerUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintStream;

import java.lang.reflect.Constructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Evaluates the performance of {@link SemanticSpace} instances on provided
 * benchmarks.
 */
public class EvaluatorMain {

    /**
     * The logger used to report verbose output
     */
    private static final Logger LOGGER = 
        Logger.getLogger(EvaluatorMain.class.getName());

    /**
     * The options available to this main
     */
    private final ArgOptions argOptions;
    
    /**
     * The collection of {@link WordChoiceEvaluation} tests that will be run on
     * the {@link SemanticSpace} instances.
     */
    private Collection<WordChoiceEvaluation> wordChoiceTests;

    /**
     * The collection of {@link WordSimilarityEvaluation} tests that will be run
     * on the {@link SemanticSpace} instances.
     */
    private Collection<WordSimilarityEvaluation> wordSimilarityTests;

    /**
     * The collection of {@link WordPrimingEvaluation} tests that will be run
     * on the {@link SemanticSpace} instances.
     */
    private Collection<WordPrimingTest> wordPrimingTests;

    /**
     * The collection of {@link NormedWordPrimingEvaluation} tests that will be
     * run on the {@link SemanticSpace} instances.
     */
    private Collection<NormedWordPrimingTest> normedPrimingTests;

    /**
     * The reporter for emitting the results of each evaluation.
     */
    private ResultReporter reporter;

    /**
     * The writer for emitting similarity and word choice results.
     */
    private PrintStream resultWriter;

    /**
     * Creates the {@code EvaluatorMain}.
     */
    public EvaluatorMain() {
        argOptions = new ArgOptions();
        addOptions();
    }

    /**
     * Adds the options available to this main.
     */
    protected void addOptions() {
        // input options
         argOptions.addOption('c', "wordChoice",
                              "a list of WordChoiceEvaluation " +
                              "class names and their data files", 
                              true, "CLASS=FILE[=FILE2...][,CLASS=FILE...]", 
                              "Required (at least one of)");
         argOptions.addOption('s', "wordSimilarity",
                              "a list of WordSimilarityEvaluation class names", 
                              true, "CLASS=FILE[=FILE2...][,CLASS=FILE...]", 
                              "Required (at least one of)");        
         argOptions.addOption('p', "wordPriming",
                              "a list of WordPrimingTest class names", 
                              true, "CLASS[=FILE][=FILE2...][,CLASS=FILE...]", 
                              "Required (at least one of)");        
         argOptions.addOption('n', "normedPriming",
                              "a list of NormedWordPrimingTest class names", 
                              true, "CLASS[=FILE][=FILE2...][,CLASS=FILE...]", 
                              "Required (at least one of)");        
         argOptions.addOption('g', "testConfiguration",
                              "a file containing a list of test " +
                              "configurations to run", 
                              true, "FILE", "Required (at least one of)");
        
        // program options
        argOptions.addOption('l', "latexOutput",
                             "writes the results to a file in a latex " +
                             "table format",
                             false, null, "Program Options");

        argOptions.addOption('o', "outputFile",
                             "writes the results to this file",
                             true, "FILE", "Program Options");
        argOptions.addOption('t', "threads",
                             "the number of threads to use",
                             true, "INT", "Program Options");
        argOptions.addOption('v', "verbose",
                             "prints verbose output",
                             false, null, "Program Options");
    }

    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        argOptions.parseOptions(args);

        if (argOptions.hasOption("verbose"))
            LoggerUtil.setLevel(Level.FINE);

        // load in the arguments specificing which tests that will be run 
        String wcTests = (argOptions.hasOption("wordChoice"))
            ? argOptions.getStringOption("wordChoice")
            : null;

        String wsTests = (argOptions.hasOption("wordSimilarity"))
            ? argOptions.getStringOption("wordSimilarity")
            : null;

        String wpTests = (argOptions.hasOption("wordPriming"))
            ? argOptions.getStringOption("wordPriming")
            : null;

        String npTests = (argOptions.hasOption("normedPriming"))
            ? argOptions.getStringOption("normedPriming")
            : null;

        String configFile = (argOptions.hasOption("testConfiguration"))
            ? argOptions.getStringOption("testConfiguration")
            : null;

        // check that the user provided some input
        if (wcTests == null && wsTests == null &&
            wpTests == null && configFile == null) {
            usage();
            System.out.println("no tests specified");
            System.exit(1);
        }

        // Load the word choice tests.
        wordChoiceTests = (wcTests == null)
            ? new LinkedList<WordChoiceEvaluation>()
            : loadWordChoiceEvaluations(wcTests);

        // Load the word similarity tests.
        wordSimilarityTests = (wsTests == null)
            ? new LinkedList<WordSimilarityEvaluation>()
            : loadWordSimilarityEvaluations(wsTests);

        resultWriter = (argOptions.hasOption("outputFile"))
            ? new PrintStream(argOptions.getStringOption("outputFile"))
            : System.out;

        reporter = (argOptions.hasOption('l'))
            ? new LatexReporter()
            : new DefaultReporter();

        // Load the word similarity tests.
        wordPrimingTests = (wpTests == null)
            ? new LinkedList<WordPrimingTest>()
            : loadWordPrimingTests(wpTests);

        // Load the word similarity tests.
        normedPrimingTests = (npTests == null)
            ? new LinkedList<NormedWordPrimingTest>()
            : loadNormedPrimingTests(npTests);

        // Load any Parse the config file for test types.  The configuration
        // file formatted as pairs of evaluations paired with data
        // files with everything separated by spaces.
        if (configFile != null) {
            WordIterator it = new WordIterator(new BufferedReader(
                                                   new FileReader(configFile)));
            while (it.hasNext()) {
                String className = it.next();
                if (!it.hasNext()) {
                    throw new Error("test is not matched with data file: " + 
                                    className);
                }
                String[] dataFiles = it.next().split(",");
                // Base the number of constructor arguments on the number of
                // String parameters specified
                Class<?> clazz = Class.forName(className);
                Class[] constructorArgs = new Class[dataFiles.length];
                for (int i = 0; i < constructorArgs.length; ++i)
                    constructorArgs[i] = String.class;
                Constructor<?> c = clazz.getConstructor(constructorArgs);
                System.out.println(className);
                Object o = c.newInstance((Object[])dataFiles);
                
                // once the test has been created, determine what kind it is
                if (o instanceof WordChoiceEvaluation) {
                    wordChoiceTests.add((WordChoiceEvaluation)o);
                    verbose("Loaded word choice test " + className);
                }
                else if (o instanceof WordSimilarityEvaluation) {
                    wordSimilarityTests.add((WordSimilarityEvaluation)o);
                    verbose("Loaded word similarity test " + className);
                }
                else if (o instanceof WordPrimingTest) {
                    wordPrimingTests.add((WordPrimingTest)o);
                    verbose("Loaded word priming test " + className);
                }
                else if (o instanceof NormedWordPrimingTest) {
                    normedPrimingTests.add((NormedWordPrimingTest)o);
                    verbose("Loaded normed word priming test " + className);
                }
                else {
                    throw new IllegalStateException(
                        "provided class is not an known Evaluation class type: "
                        + className);
                }
            }
        }

        // Load the semantic spaces one by one, evaluating each one as it's
        // loaded.
        Set<String> loadedSSpaces = new HashSet<String>();
        int spaces = argOptions.numPositionalArgs();
        for (int i = 0; i < spaces; ++i) {
            SemanticSpace sspace = null;
            String[] sspaceConfig = argOptions.getPositionalArg(i).split(",");
            String sspaceFileName = sspaceConfig[0];
            SimType comparisonFunction = SimType.COSINE;
            if (sspaceConfig.length > 1) {
                for (int j = 1; j < sspaceConfig.length; ++j) {
                    String setting = sspaceConfig[j];
                    if (j > 2) {
                        throw new IllegalStateException(
                            "too many .sspace file arguments:" + 
                            argOptions.getPositionalArg(i));
                    }
                    else if (setting.startsWith("function")) {
                        comparisonFunction = 
                            SimType.valueOf(setting.substring(10));
                    }
                    else {
                        throw new IllegalArgumentException(
                            "unknown sspace parameter: " + setting);
                    }
                }
            }
            
            // Load and evaluate the .sspace file if it hasn't been loaded
            // already
            if (!loadedSSpaces.contains(sspace)) {
                verbose("Loading semantic space: " + sspaceFileName);
                sspace = SemanticSpaceIO.load(sspaceFileName);
                loadedSSpaces.add(sspaceFileName);
                verbose("Done loading.");

                verbose("Evaluating semantic space: " + sspaceFileName);
                evaluateSemanticSpace(sspace, comparisonFunction);
                verbose("Done evaluating.");
            }
        }
        reporter.printResults();
    }
    
    /**
     * Runs the loaded evaluations on the given {@link SemanticSpace} using the
     * provided {@code SimType}.  Results are printed to the standard out.
     */
    private void evaluateSemanticSpace(SemanticSpace sspace,
                                       SimType similarity) {
        String[] results = new String[wordChoiceTests.size() +
                                      wordSimilarityTests.size() +
                                      wordPrimingTests.size() +
                                      normedPrimingTests.size()];
        int resultIndex = 0;

        // Run the word choice tests.
        for (WordChoiceEvaluation wordChoice : wordChoiceTests) {
            WordChoiceReport report = WordChoiceEvaluationRunner.evaluate(
                        sspace, wordChoice, similarity);
            verbose("Results for %s:%n%s%n", wordChoice, report);
            results[resultIndex++] = String.format("%4.3f", report.score());
        }

        // Run the word similarity tests.
        for (WordSimilarityEvaluation wordSimilarity : 
                 wordSimilarityTests) {
            WordSimilarityReport report =
                WordSimilarityEvaluationRunner.evaluate(
                        sspace, wordSimilarity, similarity);
            verbose("Results for %s:%n%s%n", wordSimilarity, report);
            results[resultIndex++] = String.format(
                    "%4.3f", report.correlation());
        }

        // Run the word priming tests.
        for (WordPrimingTest wordPrimingTest : wordPrimingTests) {
            WordPrimingReport report = wordPrimingTest.evaluate(sspace);
            verbose("Results for %s:%n%s%n", wordPrimingTest , report);
            results[resultIndex++] = String.format("%4.3f & %4.3f & %4.3f", 
                                                   report.relatedPriming(),
                                                   report.unrelatedPriming(),
                                                   report.effect());
        }

        // Run the word priming tests.
        for (NormedWordPrimingTest normedPrimingTest : normedPrimingTests) {
            NormedWordPrimingReport report = normedPrimingTest.evaluate(sspace);
            verbose("Results for %s:%n%s%n", normedPrimingTest, report);
            results[resultIndex++] = String.format(
                    "%4.3f", report.averageCorrelation());
        }

        reporter.addResults(sspace.getSpaceName(),
                            similarity.toString(), results);
    }

    /**
     * Prints verbose strings.
     */
    protected void verbose(String msg) {
        LOGGER.fine(msg);
    }

    /**
     * Prints verbose strings with formatting.
     */
    protected void verbose(String format, Object... args) {
        LOGGER.fine(String.format(format, args));
    }

    /**
     * Prints out the usage for the {@code EvaluatorMain}
     */
    public void usage() {
        System.out.println(
            "java EvaluatorMain " +
            argOptions.prettyPrint() +
            "<sspace-file>[,format=SSpaceFormat[,function=SimType]] " +
            "[<sspace-file>...]\n\n" +
            "The .sspace file arguments may have option specifications that " +
            "indicate\n what vector " + 
            "comparison method\n" + 
            "should be used (default COSINE).  Users should specify the name " +
            "of a\n" +
            "Similarity.SimType.  A single .sspace can be evaluated with " + 
            "multiple\n" +
            "comparison functions by specifying the file multiple times on " + 
            "the command\n" +
            "line.  The .sspace file will be loaded only once.\n\n" +
            "A test configuration file is a series of fully qualified class " +
            "names of evaluations\nthat should be run followed by the data" +
            "files that contains\nthe test information, comma separated");
    }

    /**
     * Starts up the evaluation.
     */
    public static void main(String[] args) {
        try {
            new EvaluatorMain().run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dynamically loads the set of specified {@link WordChoiceEvaluation}s
     * and returns them as a {@link Collection}.
     */
    private Collection<WordChoiceEvaluation> loadWordChoiceEvaluations(
            String wcTests) {
        String[] testsAndFiles = wcTests.split(",");
        Collection<WordChoiceEvaluation> wordChoiceTests = 
            new LinkedList<WordChoiceEvaluation>();
        try {
            for (String s : testsAndFiles) {
                String[] testAndFile = s.split("=");
                Class<?> clazz = Class.forName(testAndFile[0]);
                
                // Base the number of constructor arguments on the number of
                // String parameters specified
                Class[] constructorArgs = new Class[testAndFile.length - 1];
                for (int i = 0; i < constructorArgs.length; ++i)
                    constructorArgs[i] = String.class;
                Constructor<?> c = clazz.getConstructor(constructorArgs);                
                Object[] args = new String[testAndFile.length - 1];
                for (int i = 1; i < testAndFile.length; ++i)
                    args[i - 1] = testAndFile[i];
                WordChoiceEvaluation eval = 
                    (WordChoiceEvaluation)(c.newInstance(args));

                verbose("Loaded word choice test " + testAndFile[0]);
                wordChoiceTests.add(eval);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wordChoiceTests;
    }

    /**
     * Dynamically loads the set of specified {@link WordSimilarityEvaluation}s
     * and returns them as a {@link Collection}.
     */
    private Collection<WordSimilarityEvaluation> loadWordSimilarityEvaluations(
            String wcTests) {
        String[] testsAndFiles = wcTests.split(",");
        Collection<WordSimilarityEvaluation> wordSimTests = 
            new LinkedList<WordSimilarityEvaluation>();
        try { 
            for (String s : testsAndFiles) {
                String[] testAndFile = s.split("=");
                Class<?> clazz = Class.forName(testAndFile[0]);
                // Base the number of constructor arguments on the number of
                // String parameters specified
                Class[] constructorArgs = new Class[testAndFile.length - 1];
                for (int i = 0; i < constructorArgs.length; ++i)
                    constructorArgs[i] = String.class;
                Constructor<?> c = clazz.getConstructor(constructorArgs);                
                Object[] args = new String[testAndFile.length - 1];
                for (int i = 1; i < testAndFile.length; ++i)
                    args[i - 1] = testAndFile[i];
                WordSimilarityEvaluation eval =
                     (WordSimilarityEvaluation)(c.newInstance(args));
                verbose("Loaded word similarity test " + testAndFile[0]);
                wordSimTests.add(eval);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wordSimTests;
    }

    /**
     * A simple interface for reporting the word choice and word similarity
     * tests.
     */
    private interface ResultReporter {

        /**
         * Adds a set of results for a particular semantic space.
         */
        void addResults(String sspaceName, String simType, String[] results);

        /**
         * Prints the set of results.
         */
        void printResults();
    }

    /**
     * A default {@link ResultReporter} that simply reports the semantic space
     * name, the evaluation name, and the result.
     */
    private class DefaultReporter implements ResultReporter {

        /**
         * {@inheritDoc}
         */
        public void addResults(String sspaceName,
                               String simType,
                               String[] results) {
            int index = 0;
            for (WordChoiceEvaluation choice : wordChoiceTests)
                resultWriter.printf(
                        "Result for sspace %s-%s on synonymy test %s: %s\n",
                        sspaceName, simType, choice, results[index++]);

            for (WordSimilarityEvaluation similarity : wordSimilarityTests)
                resultWriter.printf(
                        "Result for sspace %s-%s on similarity test %s: %s\n",
                        sspaceName, simType, similarity, results[index++]);

            for (WordPrimingTest priming : wordPrimingTests)
                resultWriter.printf(
                        "Result for sspace %s-%s on priming test %s: %s\n",
                        sspaceName, simType, priming , results[index++]);

            for (NormedWordPrimingTest priming : normedPrimingTests)
                resultWriter.printf(
                        "Result for sspace %s-%s on priming test %s: %s\n",
                        sspaceName, simType, priming , results[index++]);
        }

        /**
         * {@inheritDoc}
         */
        public void printResults() {
            resultWriter.close();
        }
    }

    /**
     * A {@link ResultReporter} that emits results for a simple latex table.
     */
    private class LatexReporter implements ResultReporter {

        /**
         * The list of semantic space titles as their results are reported.
         */
        private List<String> titleList;

        /**
         * The list of results as they are reported.
         */
        private List<String[]> resultList;

        /**
         * Creates a new {@link LatexReporter}.
         */
        public LatexReporter() {
            titleList = new ArrayList<String>();
            resultList = new ArrayList<String[]>();
        }

        /**
         * {@inheritDoc}
         */
        public void addResults(String sspaceName, 
                               String simType,
                               String[] results) {
            titleList.add(sspaceName + "-" + simType);
            resultList.add(results);
        }

        /**
         * {@inheritDoc}
         */
        public void printResults() {
            // Print out the title.
            StringBuilder sb = new StringBuilder();
            sb.append("SSpace Name  ");
            for (WordChoiceEvaluation choice : wordChoiceTests)
                sb.append("  &  ").append(choice.toString());
            for (WordSimilarityEvaluation similarity : wordSimilarityTests)
                sb.append("  &  ").append(similarity.toString());
            for (WordPrimingTest priming : wordPrimingTests)
                sb.append("  &  ").append(priming.toString());
            for (NormedWordPrimingTest priming : normedPrimingTests)
                sb.append("  &  ").append(priming.toString());
            sb.append("  \\");
            resultWriter.println(sb.toString());

            // Print out the results for each semantic space algorithm tested.
            for (int i = 0; i < titleList.size(); ++i) {
                resultWriter.printf("%s ", titleList.get(i));
                String[] results = resultList.get(i);
                for (String result : results)
                    resultWriter.printf("  &  %s", result);
                resultWriter.printf("  \\\\\n");
            }
            resultWriter.close();
        }
    }

    /**
     * Dynamically loads the set of specified {@link WordPrimingTest}s
     * and returns them as a {@link Collection}.
     */
    private Collection<WordPrimingTest> loadWordPrimingTests(String wpTests) {
        String[] testsAndFiles = wpTests.split(",");
        Collection<WordPrimingTest> wordPrimingTests = 
            new LinkedList<WordPrimingTest>();
        try { 
            for (String s : testsAndFiles) {
                String[] testAndFile = s.split("=");
                Class<?> clazz = Class.forName(testAndFile[0]);
                // Base the number of constructor arguments on the number of
                // String parameters specified
                Class[] constructorArgs = new Class[testAndFile.length - 1];
                for (int i = 0; i < constructorArgs.length; ++i)
                    constructorArgs[i] = String.class;
                Constructor<?> c = clazz.getConstructor(constructorArgs);                
                Object[] args = new String[testAndFile.length - 1];
                for (int i = 1; i < testAndFile.length; ++i)
                    args[i - 1] = testAndFile[i];
                WordPrimingTest eval = (WordPrimingTest)(c.newInstance(args));
                verbose("Loaded word priming test " + testAndFile[0]);
                wordPrimingTests.add(eval);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wordPrimingTests;
    }

    /**
     * Dynamically loads the set of specified {@link NormedWordPrimingTest}s
     * and returns them as a {@link Collection}.
     */
    private Collection<NormedWordPrimingTest> loadNormedPrimingTests(
            String wpTests) {
        String[] testsAndFiles = wpTests.split(",");
        Collection<NormedWordPrimingTest> wordPrimingTests = 
            new LinkedList<NormedWordPrimingTest>();
        try { 
            for (String s : testsAndFiles) {
                String[] testAndFile = s.split("=");
                Class<?> clazz = Class.forName(testAndFile[0]);
                // Base the number of constructor arguments on the number of
                // String parameters specified
                Class[] constructorArgs = new Class[testAndFile.length - 1];
                for (int i = 0; i < constructorArgs.length; ++i)
                    constructorArgs[i] = String.class;
                Constructor<?> c = clazz.getConstructor(constructorArgs);                
                Object[] args = new String[testAndFile.length - 1];
                for (int i = 1; i < testAndFile.length; ++i)
                    args[i - 1] = testAndFile[i];
                NormedWordPrimingTest eval =
                    (NormedWordPrimingTest)(c.newInstance(args));
                verbose("Loaded normed word priming test " + testAndFile[0]);
                wordPrimingTests.add(eval);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wordPrimingTests;
    }
}
