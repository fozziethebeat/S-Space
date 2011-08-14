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

package edu.ucla.sspace.dv;

import edu.ucla.sspace.common.DimensionallyInterpretableSemanticSpace;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractorManager;
import edu.ucla.sspace.dependency.DependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.dependency.FlatPathWeight;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.logging.Logger;

/**
 * An implementation of the Dependency Vector Space word space model.  This
 * model was described in two papers.
 *
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Sebastian Padó and
 *   Mirella Lapata. Dependency-based Construction of Semantic Space
 *   Models. Computational Linguistics 33(2), 161-199.  Available <a
 *   href="http://www.nlpado.de/~sebastian/pub/papers/cl07_pado.pdf">here</a>.
 *
 *   <li style="font-family:Garamond, Georgia, serif">Sebastian Pado and Mirella
 *   Lapata. Constructing Semantic Space Models from Parsed Corpora. Proceedings
 *   of ACL-03, Sapporo.  Available <a
 *   href="http://www.nlpado.de/~sebastian/pub/papers/acl03_pado.pdf">here</a>.
 *
 * </ul>
 *
 * This algorithm operates on dependency-parsed corpora.  Each sentence is
 * represented as a parse tree.  When two words are connected by a path in these
 * trees, the path is analyzed to see if it contains semantic information,
 * e.g. the "sbj" path connecting the sentence subject to a verb would be
 * informative.  Then the words connected by the path are updated as
 * co-occurring.  The algorith has three main points of variation:
 *
 * <ul>
 *
 *   <li> A {@link BasisFunction} that maps the the co-occurrence of a word at
 *        the end of a path to a specific dimension.  For example, a basis
 *        function may map each occurrence of a word to a single dimension, or
 *        the function might map each occurrence to a different dimension
 *        specific to how the work was related.
 *
 *   <li> A {@link PathWeight} for specifying how to value the co-occurrence.
 *        For example, each occurrence may have the same value, or the weight
 *        could be based on how long is the path that connects them.
 *
 *   <li> A {@link DependencyPathAcceptor} that determines which paths are to be
 *        used in counting co-occurrences.  Padó and Lapata provide three
 *        templates to match against: {@link MinimumTemplateAcceptor}, {@link
 *        MediumTemplateAcceptor}, and {@link MaximumTemplateAcceptor}.  Each
 *        acceptor matches the next smaller's set of paths and additional paths.
 *        See Padó and Lapata (2007) for details.
 *
 * </ul>
 *
 * </p>
 *
 * This class offers the following three parameter options for configuration.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #PATH_ACCEPTOR_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link MinimalTemplateAcceptor}
 *
 * <dd style="padding-top: .5em">This property sets {@link
 *      DependencyPathAcceptor} to use for validating dependency paths.  If a
 *      path is rejected it will not count towards co-occurrences. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #DependencyPathWeight}
 *      </b></code> <br>
 *      <i>Default:</i> {@link FlatPathWeight}
 *
 * <dd style="padding-top: .5em">This property sets the method by which
 *      co-occurrences are scored.  Each valid path is scored by this method.
 *      By default all paths are treated the same regardless of length.</p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #BASIS_MAPPING_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link WordBasedBasisMapping}
 *
 * <dd style="padding-top: .5em">This property determine the way in which a path
 *      is mapped to a specific dimension in the vector space.  By default, only
 *      words are used as dimensions; the occurrence of a word at the end of a
 *      path is treated the same regardless of the relation connect it or length
 *      of the path.</p>
 *
 * </dl>
 *
 *
 * </p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  At any given point in
 * processing, the {@link #getVectorFor(String) getVector} method may be used
 * to access the current semantics of a word.  This allows callers to track
 * incremental changes to the semantics as the corpus is processed. 
 *
 * </p>
 *
 * The {@link #processSpace(Properties) processSpace} method for this class does
 * nothing.
 *
 * @see edu.ucla.sspace.svs.StructuredVectorSpace
 * @see BasisFunction
 * @see PathWeight
 * @see DependencyPathAcceptor
 * 
 * @author David Jurgens
 */
public class DependencyVectorSpace 
        implements DimensionallyInterpretableSemanticSpace<String> {

    /**
     * The base prefix for all {@code DependencyVectorSpace} properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.dri.DependencyVectorSpace";

    /**
     * The property for setting the {@link DependencyPathAcceptor}.
     */
    public static final String PATH_ACCEPTOR_PROPERTY =
        PROPERTY_PREFIX + ".pathAcceptor";

    /**
     * The property for setting the {@link DependencyPathWeight}.
     */
    public static final String PATH_WEIGHTING_PROPERTY =
        PROPERTY_PREFIX + ".pathWeighting";

    /**
     * The property for setting the maximal length of any {@link
     * DependencyPath}.
     */
    public static final String BASIS_MAPPING_PROPERTY =
        PROPERTY_PREFIX + ".basisMapping";

    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER =
        Logger.getLogger(DependencyVectorSpace.class.getName());

    /**
     * A mapping from each term to the vector the represents its distribution
     */
    private Map<String,SparseDoubleVector> termToVector;

    /**
     * The {@link DependencyExtractor} used to extract parse trees from the
     * already parsed documents
     */
    private final DependencyExtractor extractor;

    /**
     * A basis mapping from dependency paths to the the dimensions that
     * represent the content of those paths.
     */
    private final DependencyPathBasisMapping basisMapping;

    /**
     * A function that weights {@link DependencyPath} instances according to
     * some criteria.
     */
    private final DependencyPathWeight weighter;

    /**
     * The filter that accepts only dependency paths that match predefined
     * criteria.
     */
    private final DependencyPathAcceptor acceptor;

    private final int pathLength;

    /**
     * Creates and configures this {@code DependencyVectorSpace} with the
     * default set of parameters.  The default values are:<ul>
     *   <li> a {@link WordBasedBasisMapping} is used for dimensions;
     *   <li> a {@link FlatPathWeight} is used to weight accepted paths;
     *   <li> and a {@link MinimumTemplateAcceptor} is used to filter the paths
     *        in a sentence.
     * </ul>
     */
    public DependencyVectorSpace() {
        this(System.getProperties(), 0);
    }
   
    /**
     * Creates and configures this {@code DependencyVectorSpace} with the
     * default set of parameters.  The default values are:<ul>
     *   <li> a {@link WordBasedBasisMapping} is used for dimensions;
     *   <li> a {@link FlatPathWeight} is used to weight accepted paths;
     *   <li> and a {@link MinimumTemplateAcceptor} is used to filter the paths
     *        in a sentence.
     * </ul>
     */
    public DependencyVectorSpace(Properties properties) {
        this(properties, 0);
    }

    /**
    /**
     * Creates and configures this {@code DependencyVectorSpace} with the
     * default set of parameters.  The default values are:<ul>
     *   <li> a {@link WordBasedBasisMapping} is used for dimensions;
     *   <li> a {@link FlatPathWeight} is used to weight accepted paths;
     *   <li> and a {@link MinimumTemplateAcceptor} is used to filter the paths
     *        in a sentence.
     * </ul>
     *
     * @param properties The {@link Properties} setting the above options
     * @param pathLength The maximum valid path length.  Must be non-negative.
     *        If zero, an the maximum path length used by the {@link
     *        DependencyPathAcceptor} will be used.
     */
    public DependencyVectorSpace(Properties properties, int pathLength) {
        if (pathLength < 0)
            throw new IllegalArgumentException(
                    "path length must be non-negative");

        termToVector = new HashMap<String,SparseDoubleVector>();
        
        String basisMappingProp = 
            properties.getProperty(BASIS_MAPPING_PROPERTY);
        basisMapping = (basisMappingProp == null)
            ? new WordBasedBasisMapping()
            : ReflectionUtil.<DependencyPathBasisMapping>
                getObjectInstance(basisMappingProp);

        String pathWeightProp = 
            properties.getProperty(PATH_WEIGHTING_PROPERTY);
        weighter = (pathWeightProp == null)
            ? new FlatPathWeight()
            : ReflectionUtil.<DependencyPathWeight>
                getObjectInstance(pathWeightProp);

        String acceptorProp = 
            properties.getProperty(PATH_ACCEPTOR_PROPERTY);
        acceptor = (acceptorProp == null)
            ? new MinimumPennTemplateAcceptor()
            : ReflectionUtil.<DependencyPathAcceptor>
                getObjectInstance(acceptorProp);

        this.pathLength = (pathLength == 0)
            ? acceptor.maxPathLength()
            : pathLength;

        extractor = DependencyExtractorManager.getDefaultExtractor();
    }

    /**
     * Returns a description of the dependency path feature to which the
     * provided dimension is mapped.
     *
     * @param dimension {@inheritDoc}
     * @return {@inheritDoc}
     */
    public String getDimensionDescription(int dimension) {
        if (dimension < 0 || dimension >= basisMapping.numDimensions())
            throw new IllegalArgumentException(
                "Invalid dimension: " + dimension);
        return basisMapping.getDimensionDescription(dimension);
    }

    /**
     * Returns the current semantic vector for the provided word.  If the word
     * is not currently in the semantic space, a vector is added for it and
     * returned.
     *
     * @param word a word that requires a semantic vector
     *
     * @return the {@code SemanticVector} representing {@code word}
     */
    private SparseDoubleVector getSemanticVector(String word) {
        SparseDoubleVector v = termToVector.get(word);
        if (v == null) {
            // lock on the word in case multiple threads attempt to add it at
            // once
            synchronized(this) {
                // recheck in case another thread added it while we were waiting
                // for the lock
                v = termToVector.get(word);
                if (v == null) {
                    v = new CompactSparseVector();
                    termToVector.put(word, v);
                }
            }
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(termToVector.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(String term) {
        SparseDoubleVector v = termToVector.get(term);
        return (v == null) ? null : Vectors.immutable(
            Vectors.subview(v, 0, basisMapping.numDimensions()));
    }

    /**
     * Returns "{@code DependencyVectorSpace}" plus this instance's
     * configuration of a basis mapping, path weighting and path acceptor.
     */
    public String getSpaceName() {
        return "DependencyVectorSpace_" + basisMapping + "_"
            + weighter + "_" + acceptor;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return basisMapping.numDimensions();
    }

    /**
     * Extracts all the parsed sentences in the document and then updates the
     * co-occurrence values for those paths matching the loaded set of
     * templates, according to this instance's {@link BasisFunction}.  Path
     * occurrences are weighted using this instance's {@link PathWeight}.
     */
    public void processDocument(BufferedReader document) throws IOException {
        
        // Iterate over all of the parseable dependency parsed sentences in the
        // document.
        for (DependencyTreeNode[] nodes = null; 
                 (nodes = extractor.readNextTree(document)) != null; ) {

            // Skip empty documents.
            if (nodes.length == 0)
                continue;            

            // Examine the paths for each word in the sentence.
            for (int wordIndex = 0; wordIndex < nodes.length; ++wordIndex) {

                String focusWord = nodes[wordIndex].word();              

                // Acquire the semantic vector for the focus word.
                SparseDoubleVector focusMeaning = getSemanticVector(focusWord);

                // Get all the valid paths starting from this word.  The
                // acceptor will filter out any paths that don't contain the
                // semantic connections we're looking for.
                Iterator<DependencyPath> paths = new FilteredDependencyIterator(
                            nodes[wordIndex], acceptor, pathLength);
                
                // For each of the paths rooted at the focus word, update the
                // co-occurrences of the focus word in the dimension that the
                // BasisFunction states.
                while (paths.hasNext()) {
                    DependencyPath path = paths.next();

                    // Get the dimension associated with the relation and/or
                    // words in the path from the basis function.  The basis
                    // function creates a specific dimension for the syntactic
                    // context in order to meaningfully comparable vectors.
                    int dimension = basisMapping.getDimension(path);

                    // Then calculate the weight for the feature presence in the
                    // dimension.  For example, the weighter might score paths
                    // inversely proportional to their length.
                    double weight = weighter.scorePath(path);

                    // Last, update the focus word's semantic vector based on
                    // the dimension and weight
                    synchronized(focusMeaning) {
                        focusMeaning.add(dimension, weight);                    
                    }
                }
            }
        }
        document.close();
    }
        
    /**
     * Does nothing.
     *
     * @param properties {@inheritDoc}
     */
    public void processSpace(Properties properties) {
    }
}
