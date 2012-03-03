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

import edu.ucla.sspace.rri.ReflectiveRandomIndexing;

import edu.ucla.sspace.vector.TernaryVector;

import java.io.File;
import java.io.IOException;
import java.io.IOError;

import java.util.Map;
import java.util.Properties;

import java.util.logging.Logger;

/**
 * An executable class for running {@link ReflectiveRandomIndexing} from the
 * command line.  <p>
 *
 * An invocation will produce one file as output {@code
 * reflective-random-indexing.sspace}.  If {@code overwrite} was set to {@code
 * true}, this file will be replaced for each new semantic space.  Otherwise, a
 * new output file of the format {@code
 * reflective-random-indexing<number>.sspace} will be created, where {@code
 * <number>} is a unique identifier for that program's invocation.  The output
 * file will be placed in the directory specified on the command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see ReflectiveRandomIndexing
 * 
 * @author David Jurgens
 */
public class ReflectiveRandomIndexingMain extends GenericMain {

    private static final Logger LOGGER 
       = Logger.getLogger(ReflectiveRandomIndexingMain.class.getName());

    private Properties props;    

    /**
     * The {@link ReflectiveRandomIndexing} instance used by this runnable.  This variable
     * is assigned after {@link #getSpace()} is called.
     */
    private ReflectiveRandomIndexing ri;

    private ReflectiveRandomIndexingMain() {
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
            ReflectiveRandomIndexingMain main = new ReflectiveRandomIndexingMain();
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
        if (argOptions.hasOption("vectorLength")) {
            props.setProperty(ReflectiveRandomIndexing.VECTOR_LENGTH_PROPERTY,
                              argOptions.getStringOption("vectorLength"));
        }

        if (argOptions.hasOption("useSparseSemantics")) {
            props.setProperty(ReflectiveRandomIndexing.USE_SPARSE_SEMANTICS_PROPERTY,
                              argOptions.getStringOption("useSparseSemantics"));
        }

        return props;
    }

    /**
     * Returns an instance of {@link ReflectiveRandomIndexing}.  If {@code loadVectors} is
     * specified in the command line options, this method will also initialize
     * the word-to-{@link TernaryVector} mapping.
     */
    protected SemanticSpace getSpace() {
        // Once all the optional properties are known and set, create the
        // ReflectiveRandomIndexing algorithm using them
        ri = new ReflectiveRandomIndexing(props);
        return ri;
    }

    /**
     * Returns the {@likn SSpaceFormat.SPARSE_BINARY sparse binary} format as
     * the default format of a {@code ReflectiveRandomIndexing} space.
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }
}
