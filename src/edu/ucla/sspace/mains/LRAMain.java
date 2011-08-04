/*
 * Copyright 2009 Sky Lin 
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

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;

import edu.ucla.sspace.util.CombinedIterator;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.FileListDocumentIterator;
import edu.ucla.sspace.text.OneLinePerDocumentIterator;

import edu.ucla.sspace.lra.LatentRelationalAnalysis;

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


/**
 * An executable class for running {@link LatentRelationalAnalysis} from the
 * command line.  This class takes in several command line arguments.
 *
 * <ul>
 *
 * <li><u>Required</u>:
 *   <ul>
 *
 *   <li> {@code -c}, {@code --corpusDir=DIR} the top-level directory of the
 *        corpus.  Only .txt files will be used. 
 *   <li> {@code -a}, {@code --analogyFile=FILE} a text file containing a list
 *         of word pairs separated by newlines. 
 *   <li> {@code -t}, {@code --testAnalogies=FILE} a text file containing a list
 *        of analogies (two word pairs) separated by newlines. 
 *   <li> {@code -o}, {@code --outputFile=FILE} a text file to store the results
 *        from evaluating the --testAnalogies file.
 *
 *   </ul>
 * 
 * <li><u>Algorithm Options</u>:
 *   <ul>
 *
 *   <li> {@code --dimensions=<int>} how many dimensions to use for the LRA
 *        vectors.  Default value is 300.
 *
 *   </ul>
 *
 * <li><u>Program Options</u>:
 *   <ul>
 *
 *   <li> {@code -i}, {@code --indexDir=DIR} the directory for storing or
 *         loading the Lucene index.
 *
 *   <li> {@code -s}, {@code --skipIndex=BOOL} specifies whether to skip Lucene
 *        indexing step.  If this option is set, then --indexDir must also be 
 *        set.
 *
 *   <li> {@code -r}, {@code --readMatrixFile=FILE} file containing a reusable
 *         projection matrix.  Must first run program with --writeMatrixFile 
 *         option.
 *
 *   <li> {@code -w}, {@code --writeMatrixFile=FILE} file to store a reusable
 *        projection matrix.  
 *
 *   <li> {@code -v}, {@code --verbose}  specifies whether to print runtime
 *        information to standard out
 *
 *   </ul>
 *
 * </ul>
 *
 *
 * @see LRA 
 *
 * @author Sky Lin 
 */
public class LRAMain extends GenericMain {

    /**
     * Whether to emit messages to {@code stdout} when the {@code verbose}
     * methods are used.
     */
    protected boolean verbose;

    /**
     * The processed argument options available to the main classes.
     */
    protected ArgOptions argOptions;

    private LRAMain() {
        verbose = false;
    }

    /**
     * Prints out information on how to run the program to {@code stdout}.
     */
    public void usage() {
         System.out.println(
             "usage: java LRAMain [options] <corpusDir> <analogyFile> " +
            "<testAnalogies> <outputFile>\n" + 
            argOptions.prettyPrint());
    }

    /**
     * Returns the {@code Properties} object that will be used when calling
     * {@link SemanticSpace#processSpace(Properties)}.  Subclasses should
     * override this method if they need to specify additional properties for
     * the space.  
     *
     * @return the {@code Properties} used for processing the semantic space.
     */
    public Properties setupProperties() {
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = System.getProperties();

        if (argOptions.hasOption("dimensions")) {
            props.setProperty(LatentRelationalAnalysis.LRA_DIMENSIONS_PROPERTY,
                              argOptions.getStringOption("dimensions"));
        }

        if (argOptions.hasOption("indexDir")) {
            props.setProperty(LatentRelationalAnalysis.LRA_INDEX_DIR,
                              argOptions.getStringOption("indexDir"));
        }

        if (argOptions.hasOption("skipIndex")) {
            props.setProperty(LatentRelationalAnalysis.LRA_SKIP_INDEX,
                              "true");
        }

        if (argOptions.hasOption("readMatrixFile")) {
            props.setProperty(LatentRelationalAnalysis.LRA_READ_MATRIX_FILE,
                              argOptions.getStringOption("readMatrixFile"));
        }

        if (argOptions.hasOption("writeMatrixFile")) {
            props.setProperty(LatentRelationalAnalysis.LRA_WRITE_MATRIX_FILE,
                              argOptions.getStringOption("writeMatrixFile"));
        }

        return props;
    }

    /**
     * Adds the default options for running semantic space algorithms from the
     * command line.  Subclasses should override this method and return a
     * different instance if the default options need to be different.
     */
    protected ArgOptions setupOptions() {
        ArgOptions options = new ArgOptions();
        options.addOption('c', "corpusDir", "the directory of the corpus", 
                          true, "DIR", "Required");
        options.addOption('a', "analogyFile",
                          "the file containing list of word pairs", 
                          true, "FILE", "Required");
        options.addOption('t', "testAnalogies",
                           "the file containing list of analogies",
                           true, "FILE", "Required"); 
        options.addOption('o', "outputFile",
                          "the file containing the cosine similarity output " +
                          "for the analogies from testAnalogies",
                          true, "FILE", "Required"); 
        options.addOption('i', "indexDir",
                          "a Directory for storing or loading "
                          + "the Lucene index", true, "DIR", "Required");
        options.addOption('n', "dimensions", 
                          "the number of dimensions in the semantic space",
                          true, "INT"); 
        options.addOption('r', "readMatrixFile",
                          "file containing projection matrix"
                          , true, "FILE");
        options.addOption('s', "skipIndex",
                          "turn indexing off.  Must specify index directory",
                          false , null);
        options.addOption('v', "verbose",
                          "prints verbose output",
                          false, null, "Program Options");
        options.addOption('w', "writeMatrixFile",
                          "file to write projection matrix to"
                          , true, "FILE");
        return options;
    }

    public static void main(String[] args) {
        LRAMain lra = new LRAMain();
        try {
            lra.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
	}

    public SemanticSpace getSpace() {
        return null;
    }

    /**
     * Runs {@link LRA Latent Relational Analysis} using the configuration
     * properties found in the specified arguments.
     *
     * @param args arguments used to configure this program and the {@code
     *        LRA} instance
     */
    public void run(String[] args) {
        argOptions = setupOptions();
        try {
            if (args.length < 3) {
                usage();
                System.exit(1);
            }
            argOptions.parseOptions(args);
            
            if (argOptions.numPositionalArgs() < 4) {
                throw new IllegalArgumentException(
                        "must include all Required args");
            }

            Properties props = setupProperties();

            String corpusDir = argOptions.getPositionalArg(0);
            String analogyFile = argOptions.getPositionalArg(1);
            String testAnalogies = argOptions.getPositionalArg(2);
            String outputFile = argOptions.getPositionalArg(3);
            String indexDir = corpusDir;
            String userSpecifiedDir = 
                props.getProperty(LatentRelationalAnalysis.LRA_INDEX_DIR);
            if (userSpecifiedDir != null) {
                indexDir = userSpecifiedDir;
            } 

            boolean doIndex = true; //set as option later
            String skipIndexProp = props.getProperty(
                    LatentRelationalAnalysis.LRA_SKIP_INDEX);
            if (skipIndexProp.equals("true")) {
                doIndex = false; //set as option later
            }
            LatentRelationalAnalysis lra =
                new LatentRelationalAnalysis(corpusDir, indexDir, doIndex);

            //Steps 1-2. Load analogy input
            lra.loadAnalogiesFromFile(analogyFile);

            Matrix projection;

            // if we load a projection matrix from file, we can skip all the
            // preprocessing
            String readProjectionFile = props.getProperty(
                    LatentRelationalAnalysis.LRA_READ_MATRIX_FILE);
            if (readProjectionFile != null) {
                File readFile = new File(readProjectionFile);
                if (readFile.exists()) {
                    projection = 
                        MatrixIO.readMatrix(new File(readProjectionFile), 
                                            MatrixIO.Format.SVDLIBC_SPARSE_TEXT,
                                            Matrix.Type.SPARSE_IN_MEMORY);
                } else {
                    throw new IllegalArgumentException(
                        "specified projection file does not exist");
                }
            } else { //do normal LRA preprocessing...


                //Step 3. Get patterns Step 4. Filter top NUM_PATTERNS
                lra.findPatterns();

                //Step 5. Map phrases to rows 
                lra.mapRows();
                //Step 6. Map patterns to columns 
                lra.mapColumns();

                //Step 7. Create sparse matrix 
                Matrix sparse_matrix = lra.createSparseMatrix();

                //Step 8. Calculate entropy
                sparse_matrix = lra.applyEntropyTransformations(sparse_matrix);

                //Step 9. Compute SVD on the pre-processed matrix.
                int dimensions = 300; //TODO: set as option
                String userSpecfiedDims = props.getProperty(
                        LatentRelationalAnalysis.LRA_DIMENSIONS_PROPERTY);
                if (userSpecfiedDims != null) {
                    try {
                        dimensions = Integer.parseInt(userSpecfiedDims);
                    } catch (NumberFormatException nfe) {
                        throw new IllegalArgumentException(
                            LatentRelationalAnalysis.LRA_DIMENSIONS_PROPERTY +
                            " is not an integer: " + userSpecfiedDims);
                    }
                }
                Matrix[] usv = lra.computeSVD(sparse_matrix, dimensions);

                //Step 10. Compute projection matrix from U and S.
                projection = Matrices.multiply(usv[0],usv[1]);
            }

            String writeProjectionFile = props.getProperty(
                    LatentRelationalAnalysis.LRA_WRITE_MATRIX_FILE);
            if(writeProjectionFile != null) {
                MatrixIO.writeMatrix(projection, 
                                     new File(writeProjectionFile),
                                     MatrixIO.Format.SVDLIBC_SPARSE_TEXT);
            }

            //Step 11. Get analogy input and Evaluate Alternatives
            lra.evaluateAnalogies(projection, testAnalogies, outputFile);
        } catch (Throwable t)  {
            t.printStackTrace();
        }
    }

    protected void verbose(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }

    protected void verbose(String format, Object... args) {
        if (verbose) {
            System.out.printf(format, args);
        }
    }
}
