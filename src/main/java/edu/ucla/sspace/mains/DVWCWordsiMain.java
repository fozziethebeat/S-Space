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

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.hal.LinearWeighting;
import edu.ucla.sspace.hal.WeightingFunction;

import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.wordsi.DependencyContextGenerator;
import edu.ucla.sspace.wordsi.OccurrenceDependencyContextGenerator;
import edu.ucla.sspace.wordsi.OrderingDependencyContextGenerator;
import edu.ucla.sspace.wordsi.PartOfSpeechDependencyContextGenerator;

import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;


/**
 * A dependency based executable class for running {@link Wordsi}.  {@link
 * GenericWordsiMain} provides the core command line arguments and
 * functionality.  This class provides the following additional arguments:
 *
 * <ul>
 *   <li><u>Optional</u>
 *     <ul>
 *       </li> {@code -p}, {@code --pathAcceptor=CLASSNAME} Specifies the {@link
 *       DependencyPathAcceptor} to use when validating paths as features.
 *       (Default: {@link UniversalPathAcceptor})
 *       
 *       </li> {@code -W}, {@code --weightingFunction=CLASSNAME} Specifies the
 *       class that will weight dependency paths.
 *       
 *       </li> {@code -b}, {@code --basisMapping=CLASSNAME} Specifies the class
 *       that deterine what aspect of a {@link DependencyPath} will as a feature
 *       in the word space. (Default: {@link WordBasedBasisMapping})
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class DVWCWordsiMain extends DVWordsiMain {

    /**
     * The {@link BasisMapping} responsible for creating feature indices for
     * features keyed by strings, with each feature being described by a string.
     */
    private BasisMapping<String, String> basis;

    public static void main(String[] args) throws Exception {
        DVWCWordsiMain main = new DVWCWordsiMain();
        main.run(args);
    }

    /**
     * {@inheritDoc}
     */
    protected void addExtraOptions(ArgOptions options) {
        super.addExtraOptions(options);

        options.addOption('H', "usePartsOfSpeech",
                          "If provided, parts of speech will be used as part " +
                          "of the word occurrence features.", 
                          false, null, "Optional");
        options.addOption('O', "useWordOrdering",
                          "If provided, parts of speech will be used as part " +
                          "of the word occurrence features.", 
                          false, null, "Optional");
    }

    /**
     * {@inheritDoc}
     */
    protected void handleExtraOptions() {
        // If the -L option is given, load the basis mapping from disk.
        if (argOptions.hasOption('L'))
            basis = loadObject(openLoadFile());
        else 
            basis = new StringBasisMapping();
    }

    /**
     * {@inheritDoc}
     */
    protected void postProcessing() {
        if (argOptions.hasOption('S'))
            saveObject(openSaveFile(), basis);
    }

    /**
     * {@inheritDoc}
     */
    protected DependencyContextGenerator getContextGenerator() {
        if (argOptions.hasOption('H'))
            return new PartOfSpeechDependencyContextGenerator(
                    basis, windowSize());
        if (argOptions.hasOption('O'))
            return new OrderingDependencyContextGenerator(
                    basis, windowSize());
        return new OccurrenceDependencyContextGenerator(basis, windowSize());
    }
}
