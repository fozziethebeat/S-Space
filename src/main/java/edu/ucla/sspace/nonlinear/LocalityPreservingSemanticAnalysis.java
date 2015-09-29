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

package edu.ucla.sspace.nonlinear;

import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.GenericTermDocumentVectorSpace;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.AffinityMatrixCreator;
import edu.ucla.sspace.matrix.LocalityPreservingProjection;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SvdlibcSparseBinaryMatrixBuilder;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.ReflectionUtil;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

import java.util.concurrent.ConcurrentHashMap;


/**
 * An implementation of Locality Preserving Semantic Analysis (LPSA).  LPSA is a
 * non-linear reduction of the <a
 * href="http://en.wikipedia.org/wiki/Vector_space_model">Vector Space Model</a>
 * (VSM) through use of <a
 * href="http://people.cs.uchicago.edu/~xiaofei/LPP.html">Locality Preserving
 * Projections</a> (LPP).  In this sense, LPSA is related to {@link
 * edu.ucla.sspace.lsa.LatentSemanticAnalysis LSA}, but uses a different
 * reduction of the VSM for the final word representations.  This implementation
 * is based on the following paper.  <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> <i>forthcoming</i></li>
 *
 * </ul>
 *
 * <p>
 *
 * This class offers configurable preprocessing and dimensionality reduction.
 * through two parameters.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #MATRIX_TRANSFORM_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> none.
 *
 * <dd style="padding-top: .5em">This variable sets the preprocessing algorithm
 *      to use on the term-document matrix prior to computing the SVD.  The
 *      property value should be the fully qualified named of a class that
 *      implements {@link Transform}.  The class should be public, not abstract,
 *      and should provide a public no-arg constructor.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value LPSA_DIMENSIONS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code 300}
 *
 * <dd style="padding-top: .5em">The number of dimensions to use for the
 *       semantic space.  This value is used as input to the SVD.<p>
 *
 * </dl> <p>
 *
 * <p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  Once {@link
 * #build(Properties) build} has been called, no further calls to
 * {@code processDocument} should be made.  This implementation does not support
 * access to the semantic vectors until after {@code build} has been
 * called.
 *
 * @see Transform
 * @see LocalityPreservingProjection
 * @see GenericTermDocumentVectorSpace
 * @see LSA
 * 
 * @author David Jurgens
 */
public class LocalityPreservingSemanticAnalysis
        extends GenericTermDocumentVectorSpace {

    /** 
     * The prefix for naming publically accessible properties
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.lpsa.LocalityPreservingSemanticAnalysis";

    /**
     * The property to define the {@link Transform} class to be used
     * when processing the space after all the documents have been seen.
     */
    public static final String MATRIX_TRANSFORM_PROPERTY =
        PROPERTY_PREFIX + ".transform";

    /**
     * The property to set the number of dimension to which the space should be
     * reduced using the SVD
     */
    public static final String LPSA_DIMENSIONS_PROPERTY =
        PROPERTY_PREFIX + ".dimensions";

    /**
     * The {@link AffinityMatrixCreator}.
     */
    private final AffinityMatrixCreator affinityCreator;

    /**
     * The name prefix used with {@link #getName()}
     */
    private static final String LPSA_SSPACE_NAME =
        "lpsa-semantic-space";

    /**
     * Constructs the {@code LocalityPreservingSemanticAnalysis} using the
     * system properties for configuration.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public LocalityPreservingSemanticAnalysis(AffinityMatrixCreator creator) 
            throws IOException {
        super(false, new StringBasisMapping(),
              new SvdlibcSparseBinaryMatrixBuilder(true));
        this.affinityCreator = creator;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return LPSA_SSPACE_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @param properties {@inheritDoc} See this class's {@link
     *        LocalityPreservingSemanticAnalysis javadoc} for the full list of
     *        supported properties.
     */
    public void build(Properties properties) {
        try {
            Transform transform = null;
            // If the user specified a transform, then apply it and update the
            // matrix file
            String transformClass = 
                properties.getProperty(MATRIX_TRANSFORM_PROPERTY);
            if (transformClass != null)
                transform = ReflectionUtil.getObjectInstance(transformClass);

            MatrixFile transformedMatrix = build(transform);

            // Set all of the default properties
            int dimensions = 300; 

            // Then load any of the user-specified properties
            String dimensionsProp = 
                properties.getProperty(LPSA_DIMENSIONS_PROPERTY);
            if (dimensionsProp != null) {
                try {
                    dimensions = Integer.parseInt(dimensionsProp);
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException(
                        LPSA_DIMENSIONS_PROPERTY + " is not an integer: " +
                        dimensionsProp);
                }
            }

            LoggerUtil.verbose(LOG, "reducing to %d dimensions", dimensions);

            Matrix termDocMatrix = MatrixIO.readMatrix(
                transformedMatrix.getFile(), 
                transformedMatrix.getFormat(), 
                Matrix.Type.SPARSE_IN_MEMORY, true);

            // Calculate the affinity matrix for the term-doc matrix
            MatrixFile affinityMatrix = affinityCreator.calculate(
                termDocMatrix);

            // Using the affinity matrix as a guide to locality, project the
            // co-occurrence matrix into the lower dimensional subspace
            wordSpace = LocalityPreservingProjection.project(
                    termDocMatrix, affinityMatrix, dimensions);
        } catch (IOException ioe) {
            //rethrow as Error
            throw new IOError(ioe);
        }
    }
}
