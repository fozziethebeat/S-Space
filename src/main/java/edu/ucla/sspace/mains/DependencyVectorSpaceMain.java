/*
 * Copyright 2010 Keith Stevens
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

import edu.ucla.sspace.dependency.DependencyExtractor;

import edu.ucla.sspace.dv.DependencyVectorSpace;

import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link DepenencyVectorSpace} from the command
 * line.
 *
 * An invocation will produce one file as output {@code
 * structued-vector-space.sspace}.  If {@code overwrite} was set to {@code
 * true}, this file will be replaced for each new semantic space.  Otherwise, a
 * new output file of the format {@code dependency-vector-space<number>.sspace}
 * will be created, where {@code <number>} is a unique identifier for that
 * program's invocation.  The output file will be placed in the directory
 * specified on the command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see DependencyVectorSpace 
 *
 * @author David Jurgens
 */
public class DependencyVectorSpaceMain extends DependencyGenericMain {

    private DependencyVectorSpaceMain() { }

    /**
     * {@inheritDoc}
     */
    public void addExtraOptions(ArgOptions options) {
        super.addExtraOptions(options);

        options.addOption('a', "pathAcceptor",
                          "the DependencyPathAcceptor to filter relations",
                          true, "CLASSNAME", "Algorithm Options");
        options.addOption('W', "pathWeighter",
                          "the DependencyPathWeight to weight parse tree paths",
                          true, "CLASSNAME", "Algorithm Options");
        options.addOption('b', "basisMapping",
                          "the BasisMapping to decide the dimension " +
                          "representations",
                          true, "CLASSNAME", "Algorithm Options");
        options.addOption('l', "pathLength",
                          "the maximum path length that will be accepted " +
                          "(default: any).",
                          true, "INT", "Algorithm Options");
    }

    public static void main(String[] args) {
        DependencyVectorSpaceMain svs =  new DependencyVectorSpaceMain();
        try {
            svs.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    /**
     * {@inheritDoc}
     */
    protected SemanticSpace getSpace() {
        // Ensure that the configured DependencyExtactor is in place prior to
        // constructing the SVS
        setupDependencyExtractor();        
        return new DependencyVectorSpace(
                System.getProperties(), argOptions.getIntOption('l', 0));
    }

    /**
     * {@inheritDoc}
     */
    protected String getAlgorithmSpecifics() {
        return
            "The --basisMapping specifies how the dependency paths that " +
            "connect two words are\n"+
            "mapped into dimensions.  The default behavior is to use only " +
            "the word at the end\n" +
            "of the path.\n\n" +
            
            "The --pathAcceptor specifies which paths in the corpus are " +
            "treated as valid\n" +
            "contexts.  The default behavior is to use the minimum set of " +
            "paths defined in\n" +
            "Padó and Lapata (2007) paper.\n\n" +

            "The --pathWeighter specifies how to score paths that are " +
            "accepted.  The default\n" + 
            "behavior is not to weight the paths.\n";
    }


    /**
     * {@inheritDoc}
     */
    protected Properties setupProperties() {
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = System.getProperties();

        if (argOptions.hasOption("pathAcceptor"))
            props.setProperty(
                    DependencyVectorSpace.PATH_ACCEPTOR_PROPERTY,
                    argOptions.getStringOption("pathAcceptor"));

        if (argOptions.hasOption("pathWeighter"))
            props.setProperty(
                    DependencyVectorSpace.PATH_WEIGHTING_PROPERTY,
                    argOptions.getStringOption("pathWeighter"));

        if (argOptions.hasOption("basisMapping"))
            props.setProperty(
                    DependencyVectorSpace.BASIS_MAPPING_PROPERTY,
                    argOptions.getStringOption("basisMapping"));

        return props;
    }
}
