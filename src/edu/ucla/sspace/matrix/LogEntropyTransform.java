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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.DoubleVector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import java.util.logging.Logger;

import static edu.ucla.sspace.common.Statistics.log2;
import static edu.ucla.sspace.common.Statistics.log2_1p;


/**
 * Transforms a matrix using log-entropy weighting.  The input matrix is assumed
 * to be formatted as rows representing terms and columns representing
 * documents.  Each matrix cell indicates the number of times the row's word
 * occurs within the column's document.  See the following papers for details
 * and analysis:
 *
 * <ul> 
 *
 * <li style="font-family:Garamond, Georgia, serif"> Landauer, T. K., Foltz,
 *      P. W., and Laham, D. (1998).  Introduction to Latent Semantic
 *      Analysis. <i>Discourse Processes</i>, <b>25</b>, 259-284.</li>
 *
 * <li style="font-family:Garamond, Georgia, serif"> S. Dumais, "Enhancing
 *      performance in latent semantic indexing (LSI) retrieval," Bellcore,
 *      Morristown (now Telcordia Technologies), Tech. Rep. TM-ARH-017527,
 *      1990. </li>
 *
 * <li style="font-family:Garamond, Georgia, serif"> P. Nakov, A. Popova, and
 *      P. Mateev, "Weight functions impact on LSA performance," in
 *      <i>Proceedings of the EuroConference Recent Advances in Natural Language
 *      Processing, (RANLP’01)</i>, 2001, pp. 187–193. </li>
 *
 * </ul>
 * This implementation is based on the description in Dumais (1990).
 *
 * @author David Jurgens
 *
 * @see TfIdfTransform
 */
public class LogEntropyTransform implements Transform {

    private static final Logger LOGGER = 
        Logger.getLogger(LogEntropyTransform.class.getName());

    /**
     * Creates an instance of {@code LogEntropyTransform}.
     */
    public LogEntropyTransform() { }

    /**
     * Transforms the matrix in the file using the log-entropy transform and
     * returns a temporary file containing the result.
     *
     * @param inputMatrixFile a file containing a matrix in the specified format
     * @param format the format of the matrix
     *
     * @return a file with the transformed version of the input.  This file is
     *         marked to be deleted when the JVM exits.
     *
     * @throws IOException if any error occurs while reading the input matrix or
     *         writing the output matrix
     */
    public File transform(File inputMatrixFile, MatrixIO.Format format) 
             throws IOException {
        // create a temp file for the output
        File output = File.createTempFile(inputMatrixFile.getName() + 
                                          ".log-entropy-transform", ".dat");
        transform(inputMatrixFile, format, output);
        return output;
    }

    /**
     * Transforms the input matrix using the log-entropy transform and
     * writes the result to the file for the output matrix.
     *
     * @param inputMatrixFile a file containing a matrix in the specified format
     * @param format the format of the input matrix, and the format in which the
     *        output matrix will be written
     * @param outputMatrixFile the file to which the transformed matrix will be
     *        written
     *
     * @throws IOException if any error occurs while reading the input matrix or
     *         writing the output matrix
     */
    public void transform(File inputMatrixFile, MatrixIO.Format format, 
                          File outputMatrixFile) throws IOException {
        switch (format) {
        case SVDLIBC_SPARSE_BINARY:
            svdlibcSparseBinaryTransform(inputMatrixFile, outputMatrixFile);
            break;
        case MATLAB_SPARSE:
            matlabSparseTransform(inputMatrixFile, outputMatrixFile);
            break;
        default:
            throw new UnsupportedOperationException("Format " + format +
                " is not currently supported for transform.  Email " +
                "s-space-research-dev@googlegroups.com to have it implemented");
        }
    }
    
    /**
     * Performs the log-entropy transform on the matrix file in the {@link
     * MatrixIO.Format.SVDLIBC_SPARSE_BINARY SVDLIBC_SPARSE_BINARY} format.
     *
     * @param inputMatrixFile the matrix file to be transformed
     * @param outputMatrixFile the file that will contain the transformed matrix
     */
    private void svdlibcSparseBinaryTransform(File inputMatrixFile, 
                                              File outputMatrixFile) 
            throws IOException {

        // Open the input matrix as a random access file to allow for us to
        // travel backwards as multiple passes are needed
        DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(inputMatrixFile)));

        // Make one pass through the matrix to calculate the global statistics
        int numUniqueWords = dis.readInt();
        int numDocs = dis.readInt(); // equal to the number of rows
        int numMatrixEntries = dis.readInt();

        // Also keep track of how many times a word was seen throughout the
        // entire corpus (i.e. matrix)
        int[] termToGlobalCount = new int[numUniqueWords];
        int[] docToTermCount = new int[numDocs];
        
        // SVDLIBC sparse binary is organized as column data.  Columns are how
        // many times each word (row) as it appears in that columns's document.
        int entriesSeen = 0;
        int docIndex = 0;
        for (; entriesSeen < numMatrixEntries; ++docIndex) {
            int numUniqueWordsInDoc = dis.readInt();

            for (int i = 0; i < numUniqueWordsInDoc; ++i, ++entriesSeen) {
                int termIndex = dis.readInt();
                // occurrence is specified as a float, rather than an int
                int occurrences = (int)(dis.readFloat());
                termToGlobalCount[termIndex] += occurrences; 
                docToTermCount[docIndex] += occurrences;
            }
        }

        // Seek back to the start of the data for the next pass
        dis.close();
        dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(inputMatrixFile)));
        dis.skip(12); // 3 integers

        double[] termEntropy = new double[numUniqueWords];

        docIndex = 0;
        entriesSeen = 0;
        for (; entriesSeen < numMatrixEntries; ++docIndex) {
            int numUniqueWordsInDoc = dis.readInt();

            for (int i = 0; i < numUniqueWordsInDoc; ++i, ++entriesSeen) {
                int termIndex = dis.readInt();
                // occurrence is specified as a float, rather than an int
                float occurrences = dis.readFloat();
                // tf = term frequency in that document
                double tf = occurrences;
                // gf = global frequency of the term
                double gf = termToGlobalCount[termIndex];
                double p = tf / gf;
                termEntropy[termIndex] += (p * log2(p)) / log2(numDocs);
            }
        }

        dis.close();

        // Last, rewrite the original matrix using the log-entropy values
        DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(outputMatrixFile)));
        // Write the matrix header
        dos.writeInt(numUniqueWords);
        dos.writeInt(numDocs);
        dos.writeInt(numMatrixEntries);

        // Reset the original once more for the last pass
        dis.close();
        dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(inputMatrixFile)));
        dis.skip(12); // 3 integers

        docIndex = 0;
        entriesSeen = 0;
        for (; entriesSeen < numMatrixEntries; ++docIndex) {
            int numUniqueWordsInDoc = dis.readInt();
            dos.writeInt(numUniqueWordsInDoc); // unchanged in new matrix

            for (int i = 0; i < numUniqueWordsInDoc; ++i, ++entriesSeen) {
                int termIndex = dis.readInt();
                // occurrence is specified as a float, rather than an int
                float occurrences = dis.readFloat();
                // tf = term frequency in that document
                double tf = occurrences;
                double log = log2_1p(tf);
                double entropy = 1 + termEntropy[termIndex];
                dos.writeInt(termIndex);
                dos.writeFloat((float)(log * entropy));
            }
        }

        dis.close();
        dos.close();
    }

    /**
     * Performs the log-entropy transform on the matrix file in the {@link
     * MatrixIO.Format.MATLAB_SPARSE MATLAB_SPARSE} format.
     *
     * @param inputMatrixFile the matrix file to be transformed
     * @param outputMatrixFile the file that will contain the transformed matrix
     */
    private void matlabSparseTransform(File inputMatrixFile, 
                                       File outputMatrixFile) 
            throws IOException {

        Map<Integer,Integer> docToNumTerms = new HashMap<Integer,Integer>();
        Map<Integer,Integer> termToGlobalCountMap = 
            new HashMap<Integer,Integer>();
        Set<Integer> docs = new HashSet<Integer>();

        int tokensSeenInCorpus = 0;
        
        // Calculate the global occurrance count for each term from the original
        // term-document matrix.  Since the format is in the MatLab format,
        // everything is 1 based, so decrement each index we will use later on
        // by 1.
        BufferedReader br = new BufferedReader(new FileReader(inputMatrixFile));
        for (String line = null; (line = br.readLine()) != null; ) {

            String[] termDocCount = line.split("\\s+");
            
            Integer term  = Integer.valueOf(termDocCount[0]) - 1;
            Integer doc   = Integer.valueOf(termDocCount[1]) - 1;
            Integer count = Double.valueOf(termDocCount[2]).intValue();
                        
            Integer termGlobalCount = termToGlobalCountMap.get(term);
            termToGlobalCountMap.put(term, (termGlobalCount == null)
                                     ? count  : termGlobalCount + count);
            docs.add(doc);
        }

        br.close();
        
        int numDocs = docs.size();
        int numUniqueWords = termToGlobalCountMap.size();

        // Recalculate the values as arrays to speed up the computation in the
        // next step
        int[] termToGlobalCount = new int[numUniqueWords];
        
        for (Map.Entry<Integer,Integer> e : termToGlobalCountMap.entrySet())
            termToGlobalCount[e.getKey()] = e.getValue();

        double[] termEntropy = new double[numUniqueWords];

        // Calculate the term entropy for each term in the original matrix.
        br = new BufferedReader(new FileReader(inputMatrixFile));
        for (String line = null; (line = br.readLine()) != null; ) {

            String[] termDocCount = line.split("\\s+");
            
            int term  = Integer.valueOf(termDocCount[0]) - 1;
            int doc   = Integer.valueOf(termDocCount[1]) - 1;
            int count = Double.valueOf(termDocCount[2]).intValue();

            // tf = term frequency in that document
            double tf = count;
            // gf = global frequency of the term
            double gf = termToGlobalCount[term];
            double p = tf / gf;
            termEntropy[term] += (p * log2(p)) / log2(numDocs);
        }
        br.close();

        LOGGER.fine("generating new matrix");
                        
        PrintWriter pw = new PrintWriter(outputMatrixFile);

        // Last, rewrite the original matrix using the log-entropy values
        br = new BufferedReader(new FileReader(inputMatrixFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] termDocCount = line.split("\\s+");
            
            int term  = Integer.valueOf(termDocCount[0]) - 1;
            int doc   = Integer.valueOf(termDocCount[1]) - 1;
            int count = Double.valueOf(termDocCount[2]).intValue();
            
            // tf = term frequency in that document
            double tf = count;
            double log = log2_1p(tf);
            // gf = global frequency of the term
            double gf = termToGlobalCount[term];
            double entropy = 1 + termEntropy[term];
            
            // now print out the noralized values.  Add 1 to ensure that the
            // 0-based term and document indices are reconverted to Matlab
            // 1-based indices
            pw.println((term + 1) + "\t" +
                       (doc + 1) + "\t" +
                       (log * entropy));            
        }
        br.close();
        pw.close();
    }      

    /**
     * Returns the log-entropy transformm of the input matrix.
     *
     * @param matrix the matrix to be transformed
     *
     * @return the transformed version of the input matrix
     */
    public Matrix transform(Matrix matrix) {
        // NOTE: as of 0.9.9, there is no good way to create a new matrix of the
        // same type unless you already know the type or use reflection.  In
        // addition, there's no way to access the Matrix.Type for a given
        // instance, further obfuscating what class should be instantiated.
        // Therefore, we just make a guess.  This is definitely a case for
        // concern in the API.  -jurgens
        Matrix transformed = Matrices.create(matrix.rows(), matrix.columns(), 
                                             Matrix.Type.DENSE_IN_MEMORY);
        int rows = matrix.rows();
        int cols = matrix.columns();
        
        // Count how many total words are in each document.  We need this value
        // when calculating the entropy of each word in the next step.
        int[] docToNumWords = new int[cols];
        int[] termToGlobalCount = new int[rows];

        for (int term = 0; term < rows; ++term) {

            // Each row is a word
            DoubleVector rowVec = matrix.getRowVector(term);
            
            // Special case for sparse vectors
            if (rowVec instanceof SparseVector) {
                SparseVector sv = (SparseVector)rowVec;
                int[] docsWithTerm = sv.getNonZeroIndices();                
                for (int doc : docsWithTerm) {
                    int count = (int)rowVec.get(doc);
                    termToGlobalCount[term] += count;
                    docToNumWords[doc] += count;
                }
            }
            else {
                for (int doc = 0; doc < cols; ++doc) {
                    int count = (int)rowVec.get(doc);
                    termToGlobalCount[term] += count;
                    docToNumWords[doc] += count;
                }
            }
        }
         
        // Then calculate the entropy (information gain) for the occurrence
        // of the word in each document
        double[] termEntropy = new double[rows];

        for (int term = 0; term < rows; ++term) {
            DoubleVector rowVec = matrix.getRowVector(term);

            if (rowVec instanceof SparseVector) {
                SparseVector sv = (SparseVector)rowVec;
                for (int doc : sv.getNonZeroIndices()) {                    
                    double occurrences = rowVec.get(doc);                    
                    double tf = occurrences;
                    // gf = global frequency of the term
                    double gf = termToGlobalCount[term];
                    double p = tf / gf;
                    termEntropy[term] += (p * log2(p)) / log2(cols);
                }
            }
            else {
                for (int doc = 0; doc < cols; ++doc) {
                    double occurrences = rowVec.get(doc);
                    double tf = occurrences;
                    // gf = global frequency of the term
                    double gf = termToGlobalCount[term];
                    double p = tf / gf;
                    termEntropy[term] += (p * log2(p)) / log2(cols);
                }
            }
        }

        // Last, rewrite the original matrix using the log-entropy values
        for (int term = 0; term < rows; ++term) {
            DoubleVector rowVec = matrix.getRowVector(term);

            if (rowVec instanceof SparseVector) {
                SparseVector sv = (SparseVector)rowVec;
                for (int doc : sv.getNonZeroIndices()) {                    
                    double occurrences = rowVec.get(doc);
                    double tf = occurrences;
                    double log = log2_1p(tf);
                    double entropy = 1 + termEntropy[term];
                    transformed.set(term, doc, log * entropy);
                }
            }
            else {
                for (int doc = 0; doc < cols; ++doc) {
                    double occurrences = rowVec.get(doc);
                    double tf = occurrences;
                    double log = log2_1p(tf);
                    double entropy = 1 + termEntropy[term];
                    transformed.set(term, doc, log * entropy);
                }
            }
        }

        return transformed;
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "log-entropy";
    }
}
