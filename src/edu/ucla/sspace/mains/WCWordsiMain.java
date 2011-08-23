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

import edu.ucla.sspace.wordsi.ContextExtractor;
import edu.ucla.sspace.wordsi.ContextGenerator;
import edu.ucla.sspace.wordsi.GeneralContextExtractor;
import edu.ucla.sspace.wordsi.WordOccrrenceContextGenerator;

import java.util.Map;


/**
 * An executiable class for running {@link Wordsi} with a {@link
 * WordOccrrenceContextGenerator}.  The core command line arguments are provided
 * by {@link GenericWordsiMain}.  This class takes in the following additional
 * arguments:
 *
 * <ul>
 * <li><u>Optional</u>:
 *   <ul>
 *     </li> {@code -W}, {@code --weightingFunction=CLASSNAME} Specifies the
 *     class that will weight co-occurrences based on the window distances.
 *     (Default: {@link LinearWeighting}
 *   </ul>
 * </li>
 * </ul>
 * 
 * When using the {@code --Save} option, this class will save a {@link
 * BasisMapping} from strings to feature indices.  When using the {@code --Load}
 * option, this class will load a mapping from strings to feature indices from
 * disk.  This mapping must be a {@link BasisMapping}.  If {@code --Save} is not
 * used, a new {@link BasisMapping} will be used.
 *
 * @see GenericWordsiMain
 * @see WordOccrrenceContextGenerator
 * @author Keith Stevens
 */
public class WCWordsiMain extends GenericWordsiMain {

    /**
     * The {@link BasisMapping} responsible for creating feature indices for
     * features keyed by strings, with each feature being described by a string.
     */
    private BasisMapping<String, String> basis;

    /**
     * The {@link WeightingFunction} responsible for scoring each word
     * co-occurrence.
     */
    private WeightingFunction weighting;

    /**
     * {@inheritDoc}.
     */
    protected void addExtraOptions(ArgOptions options) {
        super.addExtraOptions(options);

        options.addOption('G', "weightingFunction",
                          "Specifies the class that will weight " +
                          "co-occurrences based on the window distance. " +
                          "(Default: LinearWeighting)",
                          true, "CLASSNAME", "Optional");
    }

    /**
     * {@inheritDoc}
     */
    protected void handleExtraOptions() {
        // Create the weighting function.    If one is specified by the command
        // line, create a new instance of it, otherwise default to
        // LinearWeighting.
        if (argOptions.hasOption('G'))
            weighting = ReflectionUtil.getObjectInstance(
                    argOptions.getStringOption('G'));
        else 
            weighting = new LinearWeighting();

        // If the -L option is given, load the basis mapping from disk.
        if (argOptions.hasOption('L'))
            basis = loadObject(openLoadFile());
        else 
            basis = new StringBasisMapping();
    }

    /**
     * Saves the {@code basis} to disk.
     */
    protected void postProcessing() {
        if (argOptions.hasOption('S'))
            saveObject(openSaveFile(), basis);
    }

    /**
     * {@inheritDoc}
     */
    protected ContextExtractor getExtractor() {
        // Create the new generator.
        ContextGenerator generator = new WordOccrrenceContextGenerator(
                basis, weighting, windowSize());
        return contextExtractorFromGenerator(generator);
    }

    /**
     * {@inheritDoc}
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    public static void main(String[] args) throws Exception {
        WCWordsiMain main = new WCWordsiMain();
        main.run(args);
    }
}
