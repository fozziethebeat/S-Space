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

import edu.ucla.sspace.nonlinear.LocalityPreservingSemanticAnalysis;

import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link LocalityPreservingSemanticAnalysis}
 * (LPSA) from the command line.
 *
 * <p>
 *
 * An invocation will produce one file as output {@code
 * lpsa-semantic-space.sspace}.  If {@code overwrite} was set to {@code true},
 * this file will be replaced for each new semantic space.  Otherwise, a new
 * output file of the format {@code lpsa-semantic-space<number>.sspace} will be
 * created, where {@code <number>} is a unique identifier for that program's
 * invocation.  The output file will be placed in the directory specified on the
 * command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see LocalityPreservingSemanticAnalysis
 * @see edu.ucla.sspace.matrix.Transform Transform
 *
 * @author David Jurgens
 */
public class LpsaMain extends GenericMain {

    private LpsaMain() { }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    protected void addExtraOptions(ArgOptions options) {
        options.addOption('n', "dimensions", 
                          "the number of dimensions in the semantic space",
                          true, "INT", "Algorithm Options"); 
        options.addOption('p', "preprocess", "a MatrixTransform class to "
                          + "use for preprocessing", true, "CLASSNAME",
                          "Algorithm Options");
        options.addOption('e', "edgeType", 
                          "the method to used for adding edge to the " +
                          "affinity matrix",
                          true, "EdgeType", "Algorithm Options"); 
        options.addOption('E', "edgeTypeParam", 
                          "a parameter that the EdgeType selection process " +
                          "may use",
                          true, "DOUBLE", "Algorithm Options"); 
        options.addOption('W', "edgeWeighting", 
                          "the method for weighting edges in the affinity " +
                          "matrix",
                          true, "EdgeWeighting", "Algorithm Options"); 
        options.addOption('G', "edgeWeightingParam", 
                          "a parameter for the edge weighting process",
                          true, "DOUBLE", "Algorithm Options");         
    }

    public static void main(String[] args) {
        LpsaMain lpsa = new LpsaMain();
        try {
            lpsa.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    protected SemanticSpace getSpace() {
        try {
            return new LocalityPreservingSemanticAnalysis();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns the {@likn SSpaceFormat.BINARY binary} format as the default
     * format of a {@code LocalityPreservingSemanticAnalysis} space.
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.BINARY;
    }

    protected Properties setupProperties() {
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = System.getProperties();

        if (argOptions.hasOption("dimensions")) {
            props.setProperty(LocalityPreservingSemanticAnalysis.LPSA_DIMENSIONS_PROPERTY,
                              argOptions.getStringOption("dimensions"));
        }
        if (argOptions.hasOption("preprocess")) {
            props.setProperty(LocalityPreservingSemanticAnalysis.MATRIX_TRANSFORM_PROPERTY,
                              argOptions.getStringOption("preprocess"));
        }
        if (argOptions.hasOption("edgeType")) {
            props.setProperty(
                LocalityPreservingSemanticAnalysis.LPSA_AFFINITY_EDGE_PROPERTY,
                argOptions.getStringOption("edgeType"));
        }
        if (argOptions.hasOption("edgeTypeParam")) {
            props.setProperty(
                LocalityPreservingSemanticAnalysis.LPSA_AFFINITY_EDGE_PARAM_PROPERTY,
                argOptions.getStringOption("edgeTypeParam"));
        }
        if (argOptions.hasOption("edgeWeighting")) {
            props.setProperty(
                LocalityPreservingSemanticAnalysis.LPSA_AFFINITY_EDGE_WEIGHTING_PROPERTY,
                argOptions.getStringOption("edgeWeighting"));
        }
        if (argOptions.hasOption("edgeWeightingParam")) {
            props.setProperty(
                LocalityPreservingSemanticAnalysis.LPSA_AFFINITY_EDGE_WEIGHTING_PARAM_PROPERTY,
                argOptions.getStringOption("edgeWeightingParam"));
        }
        return props;
    }

    /**
     * {@inheritDoc}
     */
    protected String getAlgorithmSpecifics() {
        return "The --edgeType option specifies the method by which words are "+
            "connected in\n" +
            "the affinity matrix.  Two options are provided: " +
            "NEAREST_NEIGHBORS and\n" +
            "MIN_SIMILARITY.  Each takes a parameter specified by the " + 
            "--edgeTypeParam\n" +
            "option.  For NEAREST_NEIGHBORS, the parameter specifies how " + 
            "many words will\n" +
            "be counted as edges in the affinity matrix.  For MIN_SIMILARITY, "+
            "the parameter\n" +
            "specifies the minimum similarity for two words to have an " +
            "edge.\n\n" +
            
            "The --edgeWeighting option specifies how edges in the affinity " +
            "matrix should\n" +
            "be weighted.  Valid options are: BINARY, GAUSSIAN_KERNEL, " + 
            "POLYNOMIAL_KERNEL,\n" +
            "DOT_PRODUCT, COSINE_SIMILARITY.  The Gaussian and polynomial " +
            "kernels take an\n" +
            "optional parameter specified by --edgeWeightingParam that " +
            "weights the kernel\n" +
            "function.  The other options ignore the value of the " +
            "parameter.\n\n" +

            "The default behavior is the use NEAREST_NEIGHBORS with a value " +
            "of 20 and\n" +
            "COSINE_SIMILARITY edge weighting.";
    }
}
