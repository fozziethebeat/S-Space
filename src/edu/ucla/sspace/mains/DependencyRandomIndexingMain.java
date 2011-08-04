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

import edu.ucla.sspace.dependency.DefaultDependencyPermutationFunction;
import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyPermutationFunction;

import edu.ucla.sspace.dri.DependencyRandomIndexing;

import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.TernaryPermutationFunction;

import edu.ucla.sspace.util.GeneratorMap;
import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.vector.TernaryVector;

import java.io.File;

import java.lang.reflect.Constructor;


/**
 *
 * <li><u>Process Properties</u>
 *   <ul>
 *
 *   </li> {@code -l}, {@code --vectorLength=INT} The size of the vectors
 *
 *   </li> {@code -s}, {@code --windowSize=INT,INT} The number of words before,
 *         and after the focus term to inspect
 *
 *   </li> {@code -P}, {@code --userPermutations} Set if permutations should be
 *         used
 *
 *   </li> {@code -p}, {@code --permutationFunction} Set the {@link
 *   DependencyPermutationFunction} that will be used.
 *   </ul>
 *
 * <li><u>Post Processing</u>
 *
 *   <ul>
 *
 *   </li> {@code -S}, {@code --saveIndexes=FILE} Save index vectors and
 *         permutation function to a binary file
 *
 *   </ul>
 *
 * </li>
 *
 * <li><u>Pre Processing</u>
 *
 *   <ul>
 *
 *   </li> {@code -L}, {@code --loadIndexes=FILE} Load index vectors and
 *         permutation function from binary files
 *
 *   <ul>
 *
 * </li>
 *
 * </ul>
 *
 * <p>
 *
 * @see DependencyRandomIndexing
 *
 * @author Keith Stevens 
 */
public class DependencyRandomIndexingMain extends DependencyGenericMain {

    private DependencyRandomIndexing dri;

    /**
     * Uninstantiable.
     */
    public DependencyRandomIndexingMain() {
    }

    /**
     * {@inheritDoc}
     */
    public void addExtraOptions(ArgOptions options) {
        super.addExtraOptions(options);

        // Add process property arguements such as the size of index vectors,
        // the generator class to use, the user class to use, the window sizes
        // that should be inspected and the set of terms to replace during
        // processing.
        options.addOption('l', "vectorLength",
                          "The size of the vectors",
                          true, "INT", "Process Properties");
        options.addOption('s', "windowSize",
                          "The maximum number of link in a dependency path " +
                          "to accept",
                          true, "INT", "Process Properties");
        options.addOption('P', "usePermutations",
                          "Set if permutations should be used",
                          false, null, "Process Properties");
        options.addOption('p', "permutationFunction",
                          "The DependencyPermutationFunction to use.",
                          true, "CLASSNAME", "Process Properties");
        options.addOption('a', "pathAcceptor",
                          "The DependencyPathAcceptor to use",
                          true, "CLASSNAME", "Optional");
        options.addOption('W', "pathWeighter",
                          "The DependencyPathWeight to use",
                          true, "CLASSNAME", "Optional");
        
        // Additional processing steps.
        options.addOption('S', "saveIndexes",
                          "Save index vectors and permutation function to a " +
                          "binary file",
                          true, "FILE", "Post Processing");
        options.addOption('L', "loadIndexes",
                          "Load index vectors and permutation function from " +
                          "binary files",
                          true, "FILE", "Pre Processing");
    }

    /**
     * Returns a new instance of a {@link DependencyPermutationFunction} based
     * on the provided command line arguments.
     */
    @SuppressWarnings("unchecked")
    private DependencyPermutationFunction getPermutationFunction() {
        try {
            if (!argOptions.hasOption('P'))
                return null;

            if (!argOptions.hasOption('p'))
                return new DefaultDependencyPermutationFunction<TernaryVector>(
                        new TernaryPermutationFunction());
            Class clazz = Class.forName(argOptions.getStringOption('p'));
            Constructor<?> c = clazz.getConstructor(PermutationFunction.class);                
            return (DependencyPermutationFunction<TernaryVector>)
                c.newInstance(new TernaryPermutationFunction());
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    protected void handleExtraOptions() {
        if (argOptions.hasOption("vectorLength"))
            System.setProperty(DependencyRandomIndexing.VECTOR_LENGTH_PROPERTY,
                               argOptions.getStringOption("vectorLength"));

        if (argOptions.hasOption("windowSize"))
            System.setProperty(
                    DependencyRandomIndexing.DEPENDENCY_PATH_LENGTH_PROPERTY,
                    argOptions.getStringOption("windowSize"));

        if (argOptions.hasOption("pathAcceptor"))
            System.setProperty(
                    DependencyRandomIndexing.DEPENDENCY_ACCEPTOR_PROPERTY,
                    argOptions.getStringOption("pathAcceptor"));

        DependencyPermutationFunction<TernaryVector> permFunction = null;
        // Setup the PermutationFunction.
        if (argOptions.hasOption("loadIndexes") &&
            argOptions.hasOption("usePermutations"))
            permFunction = (DependencyPermutationFunction<TernaryVector>) 
                SerializableUtil.load(
                        new File(argOptions.getStringOption("loadIndexes") +
                                 ".permutation"));
        else 
            permFunction = getPermutationFunction();

        // Ensure that the configured DependencyExtactor is in place prior to
        // constructing the DRI instance
        setupDependencyExtractor();     
   
        dri = new DependencyRandomIndexing(permFunction,System.getProperties());
        if (argOptions.hasOption("loadIndexes")) {
            String savedIndexName = argOptions.getStringOption("loadIndexes");
            dri.setWordToVectorMap((GeneratorMap<TernaryVector>)
                SerializableUtil.load(new File(savedIndexName + ".index"),
                                      GeneratorMap.class));
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void postProcessing() {
        if (argOptions.hasOption("saveIndexes")) {
            String filename = argOptions.getStringOption("saveIndexes");
            SerializableUtil.save(dri.getWordToVectorMap(),
                                  new File(filename + ".index"));
            SerializableUtil.save(dri.getPermutations(),
                                  new File(filename + ".permutation"));
        }
    }

    /**
     * {@inheritDoc}
     */
    public SemanticSpace getSpace() {
        return dri;
    }

    /**
     * Begin processing with {@code FlyingHermit}.
     */
    public static void main(String[] args) {
        DependencyRandomIndexingMain drim = new DependencyRandomIndexingMain();
        try {
            drim.run(args);
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
}
