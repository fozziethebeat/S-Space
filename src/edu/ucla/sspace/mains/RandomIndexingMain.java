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
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.ri.IndexVectorUtil;
import edu.ucla.sspace.ri.RandomIndexing;

import edu.ucla.sspace.vector.TernaryVector;

import java.io.File;
import java.io.IOException;
import java.io.IOError;

import java.util.Map;
import java.util.Properties;

import java.util.logging.Logger;

/**
 * An executable class for running {@link RandomIndexing} from the command line.
 * This class provides several options:
 *
 * <ul>
 *
 * <li><u>Required (at least one of)</u>:
 *   <ul>
 *
 *   <li> {@code -d}, {@code --docFile=FILE[,FILE...]} a file where each line is
 *        a document
 *
 *   <li> {@code -f}, {@code --fileList=FILE[,FILE...]} a list of document files
 *   </ul>
 *
 * <li><u>Algorithm Options</u>:
 *   <ul>
 *
 *   <li> {@code -l}, {@code --vectorLength=INT} length of semantic vectors
 *
 *   <li> {@code -p}, {@code --usePermutations=BOOL} whether to permute index
 *        vectors based on word order
 *
 *   <li> {@code -s}, {@code --windowSize=INT} how many words to consider in
 *        each direction
 * 
 *   <li> {@code -L}, {@code --loadVectors=FILE} specifies a file containing
 *         word-to-index vector mappings that should be used by the {@code
 *         RandomIndxing} instance.  This allows multiple invocations of this
 *         program to reuse the same semantic space.
 *
 *   <li> {@code -S}, {@code --saveVectors=FILE} specifies a file in which the
 *         word-to-index vector mappings will be saved after the {@code
 *         RandomIndxing} instance has finished processing all the documents.
 *         When used in conjunction with the {@code --loadVectors} option, this
 *         allows later invocations of this program to reuse the this
 *         invocation's semantic space.
 *
 *   <li> {@code -F}, {@code --tokenFilter=FILE[include|exclude][,FILE...]}
 *        specifies a list of one or more files to use for {@link
 *        edu.ucla.sspace.text.TokenFilter filtering} the documents.  An option
 *        flag may be added to each file to specify how the words in the filter
 *        filter should be used: {@code include} if only the words in the filter
 *        file should be retained in the document; {@code exclude} if only the
 *        words <i>not</i> in the filter file should be retained in the
 *        document.
 *
 *   </ul>
 *
 * <li><u>Advanced Algorithm Options</u>:
 *   <ul>
 *
 *   <li> {@code -n}, {@code --permutationFunction=CLASSNAME} the {@link
 *        edu.ucla.sspace.index.PermutationFunction PermutationFunction} class
 *        to use for permuting {@code TernaryVector}s, if permutation is
 *        enabled.
 *
 *   </ul>
 *
 * <li><u>Program Options</u>:
 *   <ul>
 *
 *   <li> {@code -o}, {@code --outputFormat=}<tt>text|binary}</tt> Specifies the
 *        output formatting to use when generating the semantic space ({@code
 *        .sspace}) file.  See {@link edu.ucla.sspace.common.SemanticSpaceUtils
 *        SemanticSpaceUtils} for format details.
 *
 *   <li> {@code -t}, {@code --threads=INT} the number of threads to use
 *
 *   <li> {@code -v}, {@code --verbose} prints verbose output
 *
 *   <li> {@code -w}, {@code --overwrite=BOOL} specifies whether to overwrite
 *        the existing output
 *   </ul>
 *
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output {@code random-indexing.sspace}.
 * If {@code overwrite} was set to {@code true}, this file will be replaced for
 * each new semantic space.  Otherwise, a new output file of the format {@code
 * random-indexing<number>.sspace} will be created, where {@code
 * <number>} is a unique identifier for that program's invocation.  The output
 * file will be placed in the directory specified on the command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see RandomIndexing
 * 
 * @author David Jurgens
 */
public class RandomIndexingMain extends GenericMain {

    private static final Logger LOGGER 
       = Logger.getLogger(RandomIndexingMain.class.getName());

    private Properties props;    

    /**
     * The {@link RandomIndexing} instance used by this runnable.  This variable
     * is assigned after {@link #getSpace()} is called.
     */
    private RandomIndexing ri;

    private RandomIndexingMain() {
        ri = null;
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    protected void addExtraOptions(ArgOptions options) {
        options.addOption('l', "vectorLength", "length of semantic vectors",
                          true, "INT", "Algorithm Options");
        options.addOption('n', "permutationFunction",
                          "permutation function to use.  This should be " +
                          "genric for TernaryVectors",
                          true, "CLASSNAME", "Advanced Algorithm Options");
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
    }

    public static void main(String[] args) {
        try {
            RandomIndexingMain main = new RandomIndexingMain();
            main.run(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Properties setupProperties() {
        props = System.getProperties();
        // Use the command line options to set the desired properites in the
        // constructor.  Use the system properties in case these properties were
        // set using -Dprop=<value>
        if (argOptions.hasOption("usePermutations")) {
            props.setProperty(RandomIndexing.USE_PERMUTATIONS_PROPERTY,
                              argOptions.getStringOption("usePermutations"));
        }

        if (argOptions.hasOption("permutationFunction")) {
            props.setProperty(
                    RandomIndexing.PERMUTATION_FUNCTION_PROPERTY,
                    argOptions.getStringOption("permutationFunction"));
        }

        if (argOptions.hasOption("windowSize")) {
            props.setProperty(RandomIndexing.WINDOW_SIZE_PROPERTY,
                              argOptions.getStringOption("windowSize"));
        }

        if (argOptions.hasOption("vectorLength")) {
            props.setProperty(RandomIndexing.VECTOR_LENGTH_PROPERTY,
                              argOptions.getStringOption("vectorLength"));
        }

        if (argOptions.hasOption("useSparseSemantics")) {
            props.setProperty(RandomIndexing.USE_SPARSE_SEMANTICS_PROPERTY,
                              argOptions.getStringOption("useSparseSemantics"));
        }

        return props;
    }

    /**
     * Returns an instance of {@link RandomIndexing}.  If {@code loadVectors} is
     * specified in the command line options, this method will also initialize
     * the word-to-{@link TernaryVector} mapping.
     */
    protected SemanticSpace getSpace() {
        // Once all the optional properties are known and set, create the
        // RandomIndexing algorithm using them
        ri = new RandomIndexing(props);

        // note that getSpace() is called after the arg options have been
        // parsed, so this call is safe.
        if (argOptions.hasOption("loadVectors")) {
            String fileName = argOptions.getStringOption("loadVectors");
            LOGGER.info("loading index vectors from " + fileName);
            Map<String,TernaryVector> wordToIndexVector = 
                IndexVectorUtil.load(new File(fileName));
            ri.setWordToIndexVector(wordToIndexVector);
        }

        return ri;
    }

    /**
     * Returns the {@likn SSpaceFormat.SPARSE_BINARY sparse binary} format as
     * the default format of a {@code RandomIndexing} space.
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    /**
     * If {@code --saveVectors} was specified, write the accumulated
     * word-to-index vector mapping to file.
     */
    @Override protected void postProcessing() {
        if (argOptions.hasOption("saveVectors")) {
            String fileName = argOptions.getStringOption("saveVectors");
            LOGGER.info("saving index vectors to " + fileName);
            IndexVectorUtil.save(ri.getWordToIndexVector(), 
                                 new File(fileName));
        }
    }
}
