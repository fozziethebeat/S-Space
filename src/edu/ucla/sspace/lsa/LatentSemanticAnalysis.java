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

package edu.ucla.sspace.lsa;

import edu.ucla.sspace.basis.BasisMapping;

import edu.ucla.sspace.common.GenericTermDocumentVectorSpace;

import edu.ucla.sspace.matrix.LogEntropyTransform;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SVD;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.DoubleVector;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

import java.util.concurrent.ConcurrentMap;;


/**
 * An implementation of Latent Semantic Analysis (LSA).  This implementation is
 * based on two papers.
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Landauer, T. K., Foltz,
 *     P. W., and Laham, D. (1998).  Introduction to Latent Semantic
 *     Analysis. <i>Discourse Processes</i>, <b>25</b>, 259-284.  Available <a
 *     href="http://lsa.colorado.edu/papers/dp1.LSAintro.pdf">here</a> </li>
 * 
 * <li style="font-family:Garamond, Georgia, serif"> Landauer, T. K., and
 *    Dumais, S. T. (1997). A solution to Plato's problem: The Latent Semantic
 *    Analysis theory of the acquisition, induction, and representation of
 *    knowledge.  <i>Psychological Review</i>, <b>104</b>, 211-240.  Available
 *    <a href="http://lsa.colorado.edu/papers/plato/plato.annote.html">here</a>
 *    </li>
 *
 * </ul> See the Wikipedia page on <a
 * href="http://en.wikipedia.org/wiki/Latent_semantic_analysis"> Latent Semantic
 * Analysis </a> for an execuative summary.
 *
 * <p>
 * 
 * LSA first processes documents into a word-document matrix where each unique
 * word is a assigned a row in the matrix, and each column represents a
 * document.  The values of ths matrix correspond to the number of times the
 * row's word occurs in the column's document.  After the matrix has been built,
 * the <a
 * href="http://en.wikipedia.org/wiki/Singular_value_decomposition">Singular
 * Value Decomposition</a> (SVD) is used to reduce the dimensionality of the
 * original word-document matrix, denoted as <span style="font-family:Garamond,
 * Georgia, serif">A</span>. The SVD is a way of factoring any matrix A into
 * three matrices <span style="font-family:Garamond, Georgia, serif">U &Sigma;
 * V<sup>T</sup></span> such that <span style="font-family:Garamond, Georgia,
 * serif"> &Sigma; </span> is a diagonal matrix containing the singular values
 * of <span style="font-family:Garamond, Georgia, serif">A</span>. The singular
 * values of <span style="font-family:Garamond, Georgia, serif"> &Sigma; </span>
 * are ordered according to which causes the most variance in the values of
 * <span style="font-family:Garamond, Georgia, serif">A</span>. The original
 * matrix may be approximated by recomputing the matrix with only <span
 * style="font-family:Garamond, Georgia, serif">k</span> of these singular
 * values and setting the rest to 0. The approximated matrix <span
 * style="font-family:Garamond, Georgia, serif"> &Acirc; = U<sub>k</sub>
 * &Sigma;<sub>k</sub> V<sub>k</sub><sup>T</sup></span> is the least squares
 * best-ﬁt rank-<span style="font-family:Garamond, Georgia, serif">k</span>
 * approximation of <span style="font-family:Garamond, Georgia, serif">A</span>.
 * LSA reduces the dimensions by keeping only the ﬁrst <span
 * style="font-family:Garamond, Georgia, serif">k</span> dimensions from the row
 * vectors of <span style="font-family:Garamond, Georgia, serif">U</span>.
 * These vectors form the <i>semantic space</i> of the words.
 *
 * <p>
 *
 * This class offers configurable preprocessing and dimensionality reduction.
 * through three parameters.  These properties should be specified in the {@code
 * Properties} object passed to the {@link #processSpace(Properties)
 * processSpace} method.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #MATRIX_TRANSFORM_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link LogEntropyTransform}
 *
 * <dd style="padding-top: .5em">This variable sets the preprocessing algorithm
 *      to use on the term-document matrix prior to computing the SVD.  The
 *      property value should be the fully qualified named of a class that
 *      implements {@link Transform}.  The class should be public, not abstract,
 *      and should provide a public no-arg constructor.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value LSA_DIMENSIONS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code 300}
 *
 * <dd style="padding-top: .5em">The number of dimensions to use for the
 *       semantic space.  This value is used as input to the SVD.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value LSA_SVD_ALGORITHM_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link edu.ucla.sspace.matrix.SVD.Algorithm#ANY}
 *
 * <dd style="padding-top: .5em">This property sets the specific SVD algorithm
 *       that LSA will use to reduce the dimensionality of the word-document
 *       matrix.  In general, users should not need to set this property, as the
 *       default behavior will choose the fastest available on the system.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value RETAIN_DOCUMENT_SPACE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code false}
 *
 * <dd style="padding-top: .5em">This property indicate whether the document
 *       space should be retained after {@code processSpace}.  Setting this
 *       property to {@code true} will enable the {@link #getDocumentVector(int)
 *       getDocumentVector} method. <p>
 *
 * </dl> <p>
 *
 * <p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  Once {@link
 * #processSpace(Properties) processSpace} has been called, no further calls to
 * {@code processDocument} should be made.  This implementation does not support
 * access to the semantic vectors until after {@code processSpace} has been
 * called.
 *
 * @see Transform
 * @see SVD
 * 
 * @author David Jurgens
 */
public class LatentSemanticAnalysis extends GenericTermDocumentVectorSpace {

    /** 
     * The prefix for naming publically accessible properties
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.lsa.LatentSemanticAnalysis";

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
    public static final String LSA_DIMENSIONS_PROPERTY =
        PROPERTY_PREFIX + ".dimensions";

    /**
     * The property to set the specific SVD algorithm used by an instance during
     * {@code processSpace}.  The value should be the name of a {@link
     * edu.ucla.sspace.matrix.SVD.Algorithm}.  If this property is unset, any
     * available algorithm will be used according to the ordering defined in
     * {@link SVD}.
     */
    public static final String LSA_SVD_ALGORITHM_PROPERTY = 
        PROPERTY_PREFIX + ".svd.algorithm";

    /**
     * The property whose boolean value indicate whether the document space
     * should be retained after {@code processSpace}.  Setting this property to
     * {@code true} will enable the {@link #getDocumentVector(int)
     * getDocumentVector} method.
     */
    public static final String RETAIN_DOCUMENT_SPACE_PROPERTY =
        PROPERTY_PREFIX + ".retainDocSpace";

    /**
     * The name prefix used with {@link #getName()}
     */
    private static final String LSA_SSPACE_NAME =
        "lsa-semantic-space";

    /**
     * The document space of the term document based word space If the word
     * space is reduced.  After reduction it is the right factor matrix of the
     * SVD of the word-document matrix.  This matrix is only available after the
     * {@link #processSpace(Transform, SVD.Algorithm, int, boolean)
     * processSpace} method has been called.
     */
    private Matrix documentSpace;
    
    /**
     * Creates a new {@link LatentSemanticAnalysis} instance.
     */
    public LatentSemanticAnalysis() throws IOException {
        super();

        documentSpace = null;
    }

    /**
     * Constructs a new {@code LatentSemanticAnalysis} using the provided
     * objects for processing.
     *
     * @param readHeaderToken If true, the first token of each document will be
     *        read and passed to {@link #handleDocumentHeader(int, String)
     *        handleDocumentHeader}, which discards the header.
     * @param termToIndex The {@link BasisMapping} used to map strings to
     *        indices.
     * @param termDocumentMatrixBuilder The {@link MatrixBuilder} used to write
     *        document vectors to disk which later get processed in {@link
     *        #processSpace(Properties) processSpace}.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public LatentSemanticAnalysis(
            boolean readHeaderToken,
            BasisMapping<String, String> termToIndex,
            MatrixBuilder termDocumentMatrixBuilder) throws IOException {
        super(readHeaderToken, termToIndex, termDocumentMatrixBuilder);
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return LSA_SSPACE_NAME;
    }

    /**
     * Returns the semantics of the document as represented by a numeric vector.
     * Note that document semantics may be represented in an entirely different
     * space, so the corresponding semantic dimensions in the word space will be
     * completely unrelated.  However, document vectors may be compared to find
     * those document with similar content.
     *
     * </p>
     *
     * Similar to {@code getVector}, this method is only to be used after {@code
     * processSpace} has been called.  By default, the document space is not
     * retained unless {@code retainDocumentSpace} is set to true.
     *
     * </p>
     *
     * Implementation note: If a specific document ordering is needed, caution
     * should be used when using this class in a multi-threaded environment.
     * Beacuse the document number is based on what order it was
     * <i>processed</i>, no guarantee is made that this will correspond with the
     * original document ordering as it exists in the corpus files.  However, in
     * a single-threaded environment, the ordering will be preserved.
     *
     * @param documentNumber the number of the document according to when it was
     *        processed
     *
     * @return the semantics of the document in the document space.
     * @throws IllegalArgumentException If the document space was not retained
     *         or the document number is out of range.
     */
    public DoubleVector getDocumentVector(int documentNumber) {
        if (documentSpace == null)
            throw new IllegalArgumentException(
                    "The document space has not been retained or generated.");

        if (documentNumber < 0 || documentNumber >= documentSpace.rows()) {
            throw new IllegalArgumentException(
                    "Document number is not within the bounds of the number of "
                    + "documents: " + documentNumber);
        }
        return documentSpace.getRowVector(documentNumber);
    }

    /**
     * {@inheritDoc}
     *
     * @param properties {@inheritDoc} See this class's {@link
     *        LatentSemanticAnalysis javadoc} for the full list of supported
     *        properties.
     */
    public void processSpace(Properties properties) {
        Transform transform = new LogEntropyTransform();

        String transformClass = properties.getProperty(
                MATRIX_TRANSFORM_PROPERTY);
        if (transformClass != null) {
            transform = ReflectionUtil.getObjectInstance(
                    transformClass);
        }

        int dimensions = 300; // default
        String userSpecfiedDims = 
            properties.getProperty(LSA_DIMENSIONS_PROPERTY);
        if (userSpecfiedDims != null) {
            try {
                dimensions = Integer.parseInt(userSpecfiedDims);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                        LSA_DIMENSIONS_PROPERTY + " is not an integer: " +
                        userSpecfiedDims);
            }
        }

        // Check whether the user has indicated that the document space
        // should be retained.
        boolean retainDocumentSpace = false;
        String retDocSpaceProp = 
            properties.getProperty(RETAIN_DOCUMENT_SPACE_PROPERTY);
        if (retDocSpaceProp != null)
            retainDocumentSpace = Boolean.parseBoolean(retDocSpaceProp);

        String svdProp = properties.getProperty(LSA_SVD_ALGORITHM_PROPERTY);
        SVD.Algorithm alg = (svdProp == null)
            ? SVD.Algorithm.ANY
            : SVD.Algorithm.valueOf(svdProp);


        try {
            MatrixFile processedSpace = processSpace(transform);

            LoggerUtil.info(LOG, "reducing to %d dimensions", dimensions);

            // Compute SVD on the pre-processed matrix.
            Matrix[] usv = SVD.svd(
                    processedSpace.getFile(), alg,
                    processedSpace.getFormat(), dimensions);
            
            // Load the left factor matrix, which is the word semantic space
            wordSpace = usv[0];

            // Weight the values in the word space by the singular values.
            Matrix singularValues = usv[1];
            for (int r = 0; r < wordSpace.rows(); ++r) {
                for (int c = 0; c < wordSpace.columns(); ++c) {
                    wordSpace.set(r, c, wordSpace.get(r, c) * 
                                        singularValues.get(c, c));
                }
            }

            // Save the reduced document space if requested.
            if (retainDocumentSpace) {
                LoggerUtil.verbose(LOG, "loading in document space");
                // We transpose the document space to provide easier access to
                // the document vectors, which in the un-transposed version are
                // the columns.
                //
                documentSpace = Matrices.transpose(usv[2]);
                // Weight the values in the document space by the singular
                // values.
                //
                // REMINDER: when the RowScaledMatrix class is merged in with
                // the trunk, this code should be replaced.
                for (int r = 0; r < documentSpace.rows(); ++r) {
                    for (int c = 0; c < documentSpace.columns(); ++c) {
                        documentSpace.set(r, c, documentSpace.get(r, c) * 
                                                singularValues.get(c, c));
                    }
                }
            }
        } catch (IOException ioe) {
            //rethrow as Error
            throw new IOError(ioe);
        }
    }
}
