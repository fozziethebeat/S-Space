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

package edu.ucla.sspace.vsm;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.GenericTermDocumentVectorSpace;

import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.util.ReflectionUtil;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

import java.util.concurrent.ConcurrentMap;;


/**
 * An implementation of the <a
 * href="http://en.wikipedia.org/wiki/Vector_space_model">Vector Space Model</a>
 * (VSM).  This model was first based on the paper <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> G. Salton, A. Wong, and
 *     C. S. Yang (1975), "A Vector Space Model for Automatic Indexing,"
 *     Communications of the ACM, vol. 18, nr. 11, pages 613–620.  Available <a
 *     href="http://doi.acm.org/10.1145/361219.361220">here</a> </li>
 *
 * </ul>
 *
 * <p>
 * 
 * The VSM first processes documents into a word-document matrix where each
 * unique word is a assigned a row in the matrix, and each column represents a
 * document.  The values of ths matrix correspond to the number of times the
 * row's word occurs in the column's document.  Optionally, after the matrix has
 * been completely, its values may be transformed.  This is frequently done
 * using the {@link edu.ucla.sspace.matrix.TfIdfTransform Tf-Idf Transform}.
 *
 * <p>
 *
 * This class offers one configurable parameter.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #MATRIX_TRANSFORM_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> none
 *
 * <dd style="padding-top: .5em">This variable sets the preprocessing algorithm
 *      to use on the term-document matrix.  The property value should be the
 *      fully qualified named of a class that implements {@link Transform}.  The
 *      class should be public, not abstract, and should provide a public no-arg
 *      constructor.<p>
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
 * 
 * @author David Jurgens
 */
public class VectorSpaceModel extends GenericTermDocumentVectorSpace {

    /** 
     * The prefix for naming publically accessible properties
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.vsm.VectorSpaceModel";

    /**
     * The property to define the {@link Transform} class to be used
     * when processing the space after all the documents have been seen.
     */
    public static final String MATRIX_TRANSFORM_PROPERTY =
        PROPERTY_PREFIX + ".transform";

    /**
     * The name prefix used with {@link #getName()}
     */
    private static final String VSM_SSPACE_NAME =
        "vector-space-model";

    /**
     * Constructs the {@code VectorSpaceModel} using the system properties
     * for configuration.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public VectorSpaceModel() throws IOException {
        super();
    }

    /**
     * Constructs a new {@code VectorSpaceModel} using the provided
     * objects for processing.
     *
     * @param readHeaderToken If true, the first token of each document will be
     *        read and passed to {@link #handleDocumentHeader(int, String)
     *        handleDocumentHeader}, which discards the header.
     * @param termToIndex The {@link ConcurrentMap} used to map strings to
     *        indices.
     * @param termDocumentMatrixBuilder The {@link MatrixBuilder} used to write
     *        document vectors to disk which later get processed in {@link
     *        #processSpace(Properties) processSpace}.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public VectorSpaceModel(
            boolean readHeaderToken,
            ConcurrentMap<String, Integer> termToIndex,
            MatrixBuilder termDocumentMatrixBuilder) throws IOException {
        super(readHeaderToken, termToIndex, termDocumentMatrixBuilder);
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return VSM_SSPACE_NAME;
    }

    /**
     * {@inheritDoc}
     *
     * @param properties {@inheritDoc} See this class's {@link VectorSpaceModel
     *        javadoc} for the full list of supported properties.
     */
    public void processSpace(Properties properties) {
        try {
            Transform transform = null;

            // Load any optionally specifie transform class
            String transformClass = 
                properties.getProperty(MATRIX_TRANSFORM_PROPERTY);
            if (transformClass != null)
                transform = ReflectionUtil.getObjectInstance(
                        transformClass);
            MatrixFile processedSpace = processSpace(transform);
            wordSpace = MatrixIO.readMatrix(processedSpace.getFile(),
                                            processedSpace.getFormat());
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
