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

import edu.ucla.sspace.util.Pair;

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
import java.util.Map;

/**
 * Tranforms a matrix according to the <a
 * href="http://en.wikipedia.org/wiki/Tf%E2%80%93idf">Term frequency-Inverse
 * Document Frequency</a> weighting.  The input matrix is assumed to be
 * formatted as rows representing terms and columns representing documents.
 * Each matrix cell indicates the number of times the row's word occurs within
 * the column's document.  For full details see:
 *
 * <ul><li style="font-family:Garamond, Georgia, serif">Spärck Jones, Karen
 *      (1972). "A statistical interpretation of term specificity and its
 *      application in retrieval". <i>Journal of Documentation</i> <b>28</b>
 *      (1): 11–21.</li></ul>
 *
 * @author David Jurgens
 *
 * @see LogEntropyTransform
 */
public class TfIdfTransform implements Transform {

    /**
     * Creates an instance of {@code TfIdfTransform}.
     */
    public TfIdfTransform() { }    
    
    /**
     * Transforms the matrix in the file using the term frequency-inverse
     * document frequency transform and returns a temporary file containing the
     * result.
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
                                          ".tf-idf-transform", ".dat");
        transform(inputMatrixFile, format, output);
        return output;
    }

    /**
     * Transforms the input matrix using the term frequency-inverse document
     * frequency transform and writes the result to the file for the output
     * matrix.
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
                " is not currently supported for tf-idf transform.  Email " +
                "s-space-research-dev@googlegroups.com to have it implemented");
        }

    }

    /**
     * Performs the term frequency-inverse document frequency transform on the
     * matrix file in the {@link MatrixIO.Format.SVDLIBC_SPARSE_BINARY
     * SVDLIBC_SPARSE_BINARY} format.
     *
     * @param inputMatrixFile the matrix file to be transformed
     * @param outputMatrixFile the file that will contain the transformed matrix
     */
    private void svdlibcSparseBinaryTransform(File inputMatrixFile, 
                                              File outputMatrixFile) 
        throws IOException {
        
        // Open the input matrix as a random access file to allow for us to
        // travel backwards as multiple passes are needed
        //RandomAccessFile raf = new RandomAccessFile(inputMatrixFile, "r");
        DataInputStream dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(inputMatrixFile)));

        // Make one pass through the matrix to calculate the global statistics

        int numUniqueWords = dis.readInt();
        int numDocs = dis.readInt(); 
        int numMatrixEntries = dis.readInt();

        // How many words are in each document
        int[] docToTermCount = new int[numDocs];
        // A count of how many different documents a term appeared in
        int[] termToDocCount = new int[numUniqueWords];
        
        // SVDLIBC sparse binary is organized as column data.  Columns are how
        // many times each word (row) as it appears in that columns's document.
        int entriesSeen = 0;
        int docIndex = 0;
        for (; entriesSeen < numMatrixEntries; ++docIndex) {
            int numUniqueWordsInDoc = dis.readInt();

            for (int i = 0; i < numUniqueWordsInDoc; ++i, ++entriesSeen) {
                int termIndex = dis.readInt();
                // Update that the term appeared in another document
                termToDocCount[termIndex]++;
                // occurrence is specified as a float, rather than an int
                int occurrences = (int)(dis.readFloat());
                docToTermCount[docIndex] += occurrences; 
            }
        }

        dis.close();
        dis = new DataInputStream(
            new BufferedInputStream(new FileInputStream(inputMatrixFile)));
        dis.skip(12); // 3 integers

        DataOutputStream dos = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(outputMatrixFile)));
        // Write the matrix header
        dos.writeInt(numUniqueWords);
        dos.writeInt(numDocs);
        dos.writeInt(numMatrixEntries);

        // Calculate the term-frequency
        docIndex = 0;
        entriesSeen = 0;
        for (; entriesSeen < numMatrixEntries; ++docIndex) {
            int numUniqueWordsInDoc = dis.readInt();
            dos.writeInt(numUniqueWordsInDoc); // unchanged in new matrix

            for (int i = 0; i < numUniqueWordsInDoc; ++entriesSeen, ++i) {
                int termIndex = dis.readInt();
                // occurrence is specified as a float, rather than an int
                float occurrences = dis.readFloat();
                
                double termFreq = occurrences / docToTermCount[docIndex];
                double invDocFreq = 
                    Math.log(numDocs / (1d + termToDocCount[termIndex]));

                dos.writeInt(termIndex);
                dos.writeFloat((float)(termFreq * invDocFreq));
            }
        }
        dis.close();
        dos.close();
    }

    /**
     * Performs the term frequency-inverse document frequency transform on the
     * matrix file in the {@link MatrixIO.Format.MATLAB_SPARSE MATLAB_SPARSE}
     * format.
     *
     * @param inputMatrixFile the matrix file to be transformed
     * @param outputMatrixFile the file that will contain the transformed matrix
     */
    private void matlabSparseTransform(File inputMatrixFile, 
                                       File outputMatrixFile) 
        throws IOException {
         
        // for each term, in how many documents did that term appear?
        Map<Integer,Double> termToDocOccurrences = 
            new HashMap<Integer,Double>();
 
        // for each document, how many terms appeared in it
        Map<Integer,Double> docToTermCount = 
            new HashMap<Integer,Double>();
 
        // how many different terms and documents were used in the matrix
        int numTerms = 0;
        int numDocs = 0;         
 
        // calculate all the statistics on the original term-document matrix
        BufferedReader br = new BufferedReader(new FileReader(inputMatrixFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] termDocCount = line.split("\\s+");
     
            Integer term  = Integer.valueOf(termDocCount[0]);
            Integer doc   = Integer.valueOf(termDocCount[1]);
            Double count = Double.valueOf(termDocCount[2]);
     
            // increase the count for the number of documents in which this
            // term was seen
            Double termCount = termToDocOccurrences.get(term);
            termToDocOccurrences.put(term, (termCount == null) 
                                     ? 1 : Double.valueOf(termCount + 1d));
     
            // increase the total count of terms seen in ths document
            Double docTermCount = docToTermCount.get(doc);
            docToTermCount.put(doc, (docTermCount == null)
                               ? count
                               : Double.valueOf(count + docTermCount));
     
        }
        br.close();

        numTerms = termToDocOccurrences.size();
        numDocs = docToTermCount.size();
 
        // the output the new matrix where the count value is replaced by the
        // tf-idf value
        PrintWriter pw = new PrintWriter(outputMatrixFile);
        br = new BufferedReader(new FileReader(inputMatrixFile));
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] termDocCount = line.split("\\s+");
     
            Integer term  = Integer.valueOf(termDocCount[0]);
            Integer doc   = Integer.valueOf(termDocCount[1]);
            Double count = Double.valueOf(termDocCount[2]);

            double tf = count.doubleValue() / docToTermCount.get(doc);
            double idf = 
                Math.log(numDocs / (1d + termToDocOccurrences.get(term)));
            pw.println(term + "\t" + doc + "\t" + (tf * idf));
        }
        br.close();
        pw.close();
    }

    /**
     * Returns the term-frequency inverse document-frequency transformm of the
     * input matrix.
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
        int terms = matrix.rows();
        int docs = matrix.columns();

        // How many terms were in each document
        int[] docToTermCount = new int[docs];
        // A count of how many different documents a term appeared in
        int[] termToDocCount = new int[terms];

        for (int term = 0; term < terms; ++term) {
            DoubleVector termVec = matrix.getRowVector(term);
            
            if (termVec instanceof SparseVector) {
                SparseVector sv = (SparseVector)termVec;
                int[] docsWithWord = sv.getNonZeroIndices();
                termToDocCount[term] = docsWithWord.length;
                for (int doc : docsWithWord) {
                    int count = (int)(termVec.get(doc));
                    docToTermCount[doc] += count;
                }
            }
            else {
                for (int doc = 0; doc < docs; ++doc) {
                    int count = (int)(termVec.get(doc));
                    if (count > 0) {
                        termToDocCount[term]++;
                        docToTermCount[doc] += count;
                    }
                }
            }
        }

        // Once the necessary corpus frequency have been computed, write the
        // transformed matrix using the original values
        for (int term = 0; term < terms; ++term) {
            DoubleVector termVec = matrix.getRowVector(term);
            
            if (termVec instanceof SparseVector) {
                SparseVector sv = (SparseVector)termVec;
                int[] docsWithWord = sv.getNonZeroIndices();

                for (int doc : docsWithWord) {
                    double occurrences = termVec.get(doc);
                    double termFreq =
                        occurrences / docToTermCount[doc];
                    double invDocFreq = 
                        Math.log(docs / (1d + termToDocCount[term]));
                    transformed.set(term, doc, termFreq * invDocFreq);
                }
            }
            else {
                for (int doc = 0; doc < docs; ++doc) {
                    double occurrences = termVec.get(doc);
                    if (occurrences > 0) {
                        double termFreq =
                            occurrences / docToTermCount[doc];
                        double invDocFreq = 
                            Math.log(docs / (1d + termToDocCount[term]));
                        transformed.set(term, doc, termFreq * invDocFreq);
                    }
                }
            }
        }
        return transformed;
    }
    
    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "TF-IDF";
    }
}



