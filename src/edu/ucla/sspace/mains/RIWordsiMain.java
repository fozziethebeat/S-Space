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
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.util.Generator;
import edu.ucla.sspace.util.GeneratorMap;
import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.TernaryVector;

import edu.ucla.sspace.wordsi.ContextExtractor;
import edu.ucla.sspace.wordsi.ContextGenerator;
import edu.ucla.sspace.wordsi.GeneralContextExtractor;
import edu.ucla.sspace.wordsi.RandomIndexingContextGenerator;

import java.util.Map;


/** 
 * An executiable class for running {@link Wordsi} with a {@link
 * RandomIndexingContextGenerator}.   
 *
 * </p>
 *
 * The core command line arguments are provided by {@link GenericWordsiMain}.
 * This class takes in the following additional arguments:
 *
 * <ul>
 *
 * <li><u>Required</u>:
 *   <ul>
 *     </li> {@code -l}, {@code --indexVectorLength=INT} Specifies the length of
 *     index vectors
 *   <ul>
 * </li>
 *
 * <li><u>Optional</u>:
 *   <ul>
 *     </li> {@code -p}, {@code --permFunction=CLASSNAME} Specifies the
 *     permutation function to apply on index vectors. (Default: none).
 *   </ul>
 * </li>
 * </ul>
 * 
 * </p>
 *
 * When using the {@code --Save} option, this class will save a mapping from
 * strings to {@link TernaryVector}s.  When using the {@code --Load} option,
 * this class will load a mapping from strings to {@link TernaryVector}s from
 * disk.  This mapping may be a {@link GeneratorMap} or it may be a fixed {@link
 * Map}.  If {@code --Save} is not used, a {@link GeneratorMap} will be used for
 * creating {@link TernaryVector}s.
 *
 * @see GenericWordsiMain
 * @see RandomIndexingContextGenerator 
 * @author Keith Stevens
 */
public class RIWordsiMain extends GenericWordsiMain {

  /**
   * The {@link PermutationFunction} for responsible for permuting {@link
   * TernaryVector}s, or {@code null}.
   */
  private PermutationFunction<TernaryVector> permFunction;

  /**
   * The mapping from strings to {@link TernaryVector}s.  By default, this map
   * will be a {@link GeneratorMap}, which creates {@link TernaryVector}s
   * for any requested strings that are unmapped.
   */
  private Map<String, TernaryVector> indexMap;

  /**
   * The length of each index vector.
   */
  private int indexVectorLength;

  /**
   * {@inheritDoc}.
   */
  protected void addExtraOptions(ArgOptions options) {
    super.addExtraOptions(options);

    options.addOption('p', "permutationFunction",
                      "Specifies the permutation function to apply on " +
                      "index vectors. (Default: none)",
                      true, "CLASSNAME", "Optional");

    options.addOption('l', "indexVectorLength",
                      "Specifies the length of index vectors.",
                      true, "CLASSNAME", "Required");
  }

  /**
   * {@inheritDoc}.
   */
  protected void handleExtraOptions() {
    indexVectorLength = argOptions.getIntOption('l');

    // Load the permutation function if one is specified.
    if (argOptions.hasOption('p'))
      permFunction = ReflectionUtil.getObjectInstance(
          argOptions.getStringOption('p'));

    // Create an index map.
    if (argOptions.hasOption('L'))
      indexMap = loadObject(openLoadFile());
    else 
      indexMap = new GeneratorMap<TernaryVector>(new RandomIndexVectorGenerator(
        indexVectorLength));

    // Load the index map from file if one was specified.
  }

  /**
   * Saves the index map to disk.
   */
  protected void postProcessing() {
    if (argOptions.hasOption('S'))
      saveObject(openSaveFile(), indexMap);
  }

  /**
   * {@inheritDoc}
   */
  protected ContextExtractor getExtractor() {
    // Create the new context generator.
    ContextGenerator generator = new RandomIndexingContextGenerator(
        indexMap, permFunction, indexVectorLength);
    return contextExtractorFromGenerator(generator);
  }

  /**
   * {@inheritDoc}
   */
  protected SSpaceFormat getSpaceFormat() {
    return SSpaceFormat.SPARSE_TEXT;
  }

  public static void main(String[] args) throws Exception {
    RIWordsiMain main = new RIWordsiMain();
    main.run(args);
  }
}
