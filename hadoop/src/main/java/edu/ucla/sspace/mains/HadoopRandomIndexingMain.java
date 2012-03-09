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
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;
import edu.ucla.sspace.common.SemanticSpaceWriter;

import edu.ucla.sspace.ri.HadoopRandomIndexing;
import edu.ucla.sspace.ri.IndexVectorUtil;

import edu.ucla.sspace.vector.TernaryVector;

import java.io.File;
import java.io.IOException;
import java.io.IOError;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import java.util.logging.Logger;

public class HadoopRandomIndexingMain extends HadoopGenericMain {

    private static final Logger LOGGER 
        = Logger.getLogger(HadoopRandomIndexingMain.class.getName());

    private Properties props;    

    /**
     * The {@link RandomIndexing} instance used by this runnable.  This variable
     * is assigned after {@link #getSpace()} is called.
     */
    private HadoopRandomIndexing ri;

    private HadoopRandomIndexingMain() {
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
            HadoopRandomIndexingMain main = 
                new HadoopRandomIndexingMain();
            main.run(args);            
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * Executes the {@link HadoopRandomIndexing} algorithm, processing all of
     * the provided input directories and writing the resulting {@link
     * SemanticSpace} to the writer.
     */
    protected void execute(Collection<String> inputDirs, 
                           SemanticSpaceWriter writer) throws Exception {

        HadoopRandomIndexing hri = new HadoopRandomIndexing();

        // Load the index vectors if the user has specified any
        if (argOptions.hasOption("loadVectors")) {
            String fileName = argOptions.getStringOption("loadVectors");
            LOGGER.info("loading index vectors from " + fileName);
            Map<String,TernaryVector> wordToIndexVector = 
                IndexVectorUtil.load(new File(fileName));
            hri.setWordToIndexVector(wordToIndexVector);
        }

        hri.execute(inputDirs, writer);        
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
            props.setProperty(HadoopRandomIndexing.USE_PERMUTATIONS_PROPERTY,
                              argOptions.getStringOption("usePermutations"));
        }

        if (argOptions.hasOption("permutationFunction")) {
            props.setProperty(
                    HadoopRandomIndexing.PERMUTATION_FUNCTION_PROPERTY,
                    argOptions.getStringOption("permutationFunction"));
        }

        if (argOptions.hasOption("windowSize")) {
            props.setProperty(HadoopRandomIndexing.WINDOW_SIZE_PROPERTY,
                              argOptions.getStringOption("windowSize"));
        }

        if (argOptions.hasOption("vectorLength")) {
            props.setProperty(HadoopRandomIndexing.VECTOR_LENGTH_PROPERTY,
                              argOptions.getStringOption("vectorLength"));
        }

        if (argOptions.hasOption("useSparseSemantics")) {
            props.setProperty(HadoopRandomIndexing.USE_SPARSE_SEMANTICS_PROPERTY,
                              argOptions.getStringOption("useSparseSemantics"));
        }

        return props;
    }

    /**
     * Returns the {@likn SSpaceFormat.SPARSE_BINARY sparse binary} format as
     * the default format of a {@code HadoopRandomIndexing} space.
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
