/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.esa;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.GenericTermDocumentVectorSpace;

import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SvdlibcSparseBinaryMatrixBuilder;
import edu.ucla.sspace.matrix.TfIdfTransform;

import edu.ucla.sspace.util.GrowableArrayList;
import edu.ucla.sspace.util.SparseArray;
import edu.ucla.sspace.util.SparseHashArray;

import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * An implementation of Explicit Semanic Analysis proposed by Evgeniy
 * Gabrilovich and Shaul Markovitch.    For full details see:
 *
 * <ul>
 *
 *     <li style="font-family:Garamond, Georgia, serif"> Evgeniy Gabrilovich and
 *         Shaul Markovitch. (2007). "Computing Semantic Relatedness using
 *         Wikipedia-based Explicit Semantic Analysis," Proceedings of The 20th
 *         International Joint Conference on Artificial Intelligence (IJCAI),
 *         Hyderabad, India, January 2007. </li>
 *
 * </ul>
 *
 * @author Keith Stevens 
 */
public class ExplicitSemanticAnalysis extends GenericTermDocumentVectorSpace {

    public static final String ESA_SSPACE_NAME =
        "esa-semantic-space";

    /**
     * A mapping from document indices to document labels.  This {@link List}
     * must both be thread safe and able to dynamically grow it's current length
     * since multiple calls to {@link List#set} can be made in parallel, and the
     * final number of documents is unknown.
     */
    private final List<String> documentLabels;

    /**
     * Constructs a new {@link ExplicitSemanticAnalysis} instance.
     */
    public ExplicitSemanticAnalysis() throws IOException {
        super(true, new StringBasisMapping(), 
              new SvdlibcSparseBinaryMatrixBuilder());

        // We use a synchronized and growable array list in order to save space.
        // Since the GrowableArrayList does the growing whenever set is called,
        // the number of synchronization calls are minimized.
        documentLabels = Collections.synchronizedList(
                new GrowableArrayList<String>());
    }

    /**
     * Constructs a new {@code ExplicitSemanticAnalysis} using the provided
     * objects for processing.
     *
     * @param termToIndex The {@link BasisMapping} used to map strings to
     *        indices.
     * @param termDocumentMatrixBuilder The {@link MatrixBuilder} used to write
     *        document vectors to disk which later get processed in {@link
     *        #processSpace(Properties) processSpace}.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public ExplicitSemanticAnalysis(
            BasisMapping<String, String> termToIndex,
            MatrixBuilder termDocumentMatrixBuilder) throws IOException {
        super(true, termToIndex, termDocumentMatrixBuilder);

        // We use a synchronized and growable array list in order to save space.
        // Since the GrowableArrayList does the growing whenever set is called,
        // the number of synchronization calls are minimized.
        documentLabels = Collections.synchronizedList(
                new GrowableArrayList<String>());
    }

    /**
     * Stores {@link header} at index {@link docIndex}.
     */
    protected void handleDocumentHeader(int docIndex, String header) {
        documentLabels.set(docIndex, header);
    }

    /**
     * Returns a {@link SparseArray} containing document labels for any non zero
     * value in the given {@link Vector}.  The given {@link Vector}s are
     * expected to have the same dimensionality as this {@link
     * ExplicitSemanticAnalysis} word space.  Under ESA, these returned document
     * labels can be considered the wikipedia articles that best describe the
     * vector created by combining each of the term vectors in a fragment of
     * text.
     */
    public SparseArray<String> getDocumentDescriptors(Vector documentVector) {
        if (documentVector.length() != getVectorLength())
            throw new IllegalArgumentException(
                    "An documentVector with an invalid length cannot be " +
                    "interpreted by ESA.");

        SparseArray<String> docLabels = new SparseHashArray<String>();

        // Extract the indices which are non zero and add the corresponding
        // document label to docLabels.
        if (documentVector instanceof SparseVector) {
            int[] nonZeros = ((SparseVector) documentVector).getNonZeroIndices();
            for (int index : nonZeros)
                docLabels.set(index, documentLabels.get(index));
        } else {
            for (int index = 0; index < documentVector.length(); index++)
                if (documentVector.getValue(index).doubleValue() != 0d)
                    docLabels.set(index, documentLabels.get(index));
        }

        return docLabels;
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        try {
            MatrixFile processedSpace = processSpace(
                    new TfIdfTransform());
            wordSpace = MatrixIO.readMatrix(
                    processedSpace.getFile(), processedSpace.getFormat());
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return ESA_SSPACE_NAME;
    }
}
