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

package edu.ucla.sspace.rri;

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.index.TernaryPermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.DenseIntVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;


import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An implementation of Reflective Random Indexing, which uses a two passes
 * through the corpus to build semantic vectors that better approximate indirect
 * co-occurrence.  This implementation is based on the paper: <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"></li>
 *
 * </ul>
 *
 * <p>
 *
 * This class defines the following configurable properties that may be set
 * using either the System properties or using the {@link
 * ReflectiveRandomIndexing#ReflectiveRandomIndexing(Properties)} constructor.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #VECTOR_LENGTH_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_VECTOR_LENGTH}
 *
 * <dd style="padding-top: .5em">This property sets the number of dimensions to
 *      be used for the index and semantic vectors. <p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #USE_SPARSE_SEMANTICS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code true} 
 *
 * <dd style="padding-top: .5em">This property specifies whether to use a sparse
 *       encoding for each word's semantics.  Using a sparse encoding can result
 *       in a large saving in memory, while requiring more time to process each
 *       document.<p>
 *
 * </dl> <p>
 *
 * This class implements {@link Filterable}, which allows for fine-grained
 * control of which semantics are retained.  The {@link #setSemanticFilter(Set)}
 * method can be used to speficy which words should have their semantics
 * retained.  Note that the words that are filtered out will still be used in
 * computing the semantics of <i>other</i> words.  This behavior is intended for
 * use with a large corpora where retaining the semantics of all words in memory
 * is infeasible.<p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  The {@link
 * #getVector(String) getVector} method will only return valid reflective
 * vectors after the call to {@link #processSpace(Properties) processSpace}. <p>
 *
 * @author David Jurgens
 */
public class ReflectiveRandomIndexing implements SemanticSpace, Filterable {

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.ri.ReflectiveRandomIndexing";

    /**
     * The property to specify the number of dimensions to be used by the index
     * and semantic vectors.
     */
    public static final String VECTOR_LENGTH_PROPERTY = 
        PROPERTY_PREFIX + ".vectorLength";

    /**
     * Specifies whether to use a sparse encoding for each word's semantics,
     * which saves space but requires more computation.
     */
    public static final String USE_SPARSE_SEMANTICS_PROPERTY = 
        PROPERTY_PREFIX + ".sparseSemantics";

    /**
     * The default number of dimensions to be used by the index and semantic
     * vectors.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 4000;

    /**
     * The name returned by {@code getName}.
     */
    private static final String RRI_SSPACE_NAME =
        "reflective-random-indexing";

    /**
     * The internal logger used for tracking processing progress.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(ReflectiveRandomIndexing.class.getName());

    /**
     * A mapping from each word to the vector the represents its the summation
     * of all the co-occurring words' index vectors.
     */
    private final Map<Integer,IntegerVector> docToVector;

    /**
     * A mapping from each word to the vector the represents its semantics after
     * the second pass through the corpus.
     */
    private final Map<String,IntegerVector> termToReflectiveSemantics;

    /**
     * A mapping from each word to the vector the represents its semantics after
     * the second pass through the corpus.
     */
    private final Map<String,TernaryVector> termToIndexVector;

    /**
     * A mapping from a each term to its index
     */
    private final Map<String,Integer> termToIndex;

    /**
     * A counter for the number of documents seen in the corpus.
     */
    private final AtomicInteger documentCounter;

    /**
     * The number of dimensions for the semantic and index vectors.
     */
    private final int vectorLength;

    /**
     * A flag for whether this instance should use {@code SparseIntegerVector}
     * instances for representic a word's semantics, which saves space but
     * requires more computation.
     */
    private final boolean useSparseSemantics;

    /**
     * An optional set of words that restricts the set of semantic vectors that
     * this instance will retain.
     */
    private final Set<String> semanticFilter;

    /**
     * The generator used to create index vectors for each unique term in the
     * corpus.
     */
    private final RandomIndexVectorGenerator indexVectorGenerator;

    /**
     * A compressed version of the corpus that is built as the text version is
     * being processed.  The file contains documents represented as an integer
     * for the number of tokens in that document followed by the indices for all
     * of the tokens in the order that they appeared.
     *
     * @see #processSpace(Properties)
     */
    private File compressedDocuments;

    /**
     * The output stream used to the write the {@link #compressedDocuments} file
     * as the text documents are being processed.
     */
    private DataOutputStream compressedDocumentsWriter;

    /**
     * The number that keeps track of the index values of words.
     */
    private int termIndexCounter;

    /**
     * A mapping from each term's index to the term.  This value is not set
     * until {@link #processSpace(Properties)} is called, at which point the
     * final set of terms has been determined
     */
    private String[] indexToTerm;

    /**
     * Creates a new {@code ReflectiveRandomIndexing} instance using the current
     * {@code System} properties for configuration.
     */
    public ReflectiveRandomIndexing() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@code ReflectiveRandomIndexing} instance using the
     * provided properites for configuration.
     */
   public ReflectiveRandomIndexing(Properties properties) {
        String vectorLengthProp = 
            properties.getProperty(VECTOR_LENGTH_PROPERTY);
        vectorLength = (vectorLengthProp != null)
            ? Integer.parseInt(vectorLengthProp)
            : DEFAULT_VECTOR_LENGTH;
        
        String useSparseProp = 
        properties.getProperty(USE_SPARSE_SEMANTICS_PROPERTY);
        useSparseSemantics = (useSparseProp != null)
            ? Boolean.parseBoolean(useSparseProp)
            : true;

        indexVectorGenerator = 
            new RandomIndexVectorGenerator(vectorLength, properties);

        // The various maps for keeping word and document state during
        // processing
        termToIndexVector = new ConcurrentHashMap<String,TernaryVector>();
        docToVector = new ConcurrentHashMap<Integer,IntegerVector>();
        termToReflectiveSemantics = 
            new ConcurrentHashMap<String,IntegerVector>();
        termToIndex = new ConcurrentHashMap<String,Integer>();

        documentCounter = new AtomicInteger();
        semanticFilter = new HashSet<String>();

        // Last set up the writer that will contain a compressed version of the
        // corpus for use in processSpace()
        try {
            compressedDocuments = 
                File.createTempFile("reflective-ri-documents",".dat");
            compressedDocumentsWriter = new DataOutputStream(
                new BufferedOutputStream(
                    new FileOutputStream(compressedDocuments)));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns a vector for representing word or document semantics whose type
     * is based on whether the used specified to use sparse semantics or not.
     */
    private IntegerVector createVector() {
        return (useSparseSemantics)
            ? new CompactSparseIntegerVector(vectorLength)
            : new DenseIntVector(vectorLength);
    }

    /**
     * Returns the index vector for the term, or if creates one if the term to
     * index vector mapping does not yet exist.
     *
     * @param term a word in the semantic space
     *
     * @return the index for the provide term.
     */
    private TernaryVector getTermIndexVector(String term) {
        TernaryVector iv = termToIndexVector.get(term);
        if (iv == null) {
            // lock in case multiple threads attempt to add it at once
            synchronized(this) {
                // recheck in case another thread added it while we were waiting
                // for the lock
                iv = termToIndexVector.get(term);
                if (iv == null) {
                    // since this is a new term, also map it to its index for
                    // later look-up when the integer documents are processed
                    termToIndex.put(term, termIndexCounter++);
                    // next, map it to its reflective vector which will be
                    // filled in process space
                    termToReflectiveSemantics.put(term, createVector());
                    // last, create an index vector for the term
                    iv = indexVectorGenerator.generate();
                    termToIndexVector.put(term, iv);                    
                }
            }
        }
        return iv;
    }

   /**
     * {@inheritDoc}
     */ 
    public IntegerVector getVector(String word) {
        IntegerVector v = termToReflectiveSemantics.get(word);
        if (v == null) {
            return null;
        }
        return Vectors.immutable(v);
    }

    /**
     * {@inheritDoc}
     */ 
    public String getSpaceName() {
        return RRI_SSPACE_NAME + "-" + vectorLength + "v";
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return vectorLength;
    }

    /**
     * {@inheritDoc}
     */ 
    public Set<String> getWords() {
        return Collections.unmodifiableSet(termToReflectiveSemantics.keySet());
    }
    
    /**
     * Updates the semantic vectors based on the words in the document.
     *
     * @param document {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        int docIndex = documentCounter.getAndIncrement();

        Iterator<String> documentTokens = 
            IteratorFactory.tokenizeOrdered(document);

        // As we read in the document, generate a compressed version of it,
        // which we will use during the process space method to recompute all of
        // the word vectors' semantics
        ByteArrayOutputStream compressedDocument = 
            new ByteArrayOutputStream(4096);
        DataOutputStream dos = new DataOutputStream(compressedDocument);
        int tokens = 0; // count how many are in this document
        int unfilteredTokens = 0; // how many tokens remained after filtering

        IntegerVector docVector = createVector();
        docToVector.put(docIndex, docVector);

        while (documentTokens.hasNext()) {
            tokens++;
            String focusWord = documentTokens.next();

            // If we are filtering the semantic vectors, check whether this word
            // should have its semantics calculated.  In addition, if there is a
            // filter and it would have excluded the word, do not keep its
            // semantics around
            boolean calculateSemantics =
                semanticFilter.isEmpty() || semanticFilter.contains(focusWord)
                && !focusWord.equals(IteratorFactory.EMPTY_TOKEN);

	    // If the filter does not accept this word, skip the semantic
	    // processing, continue with the next word
            if (!calculateSemantics) {
                // Do not write out any removed tokens to save space
		continue;
	    }

            // Update the occurrences of this token
            unfilteredTokens++;
            add(docVector, getTermIndexVector(focusWord));

            // Update the compress version of the document with the token.
            //
            // NOTE: this call to termToIndex *must* come after the
            // getTermIndexVector() call, which is responsible for adding this
            // mapping if it doesn't already exist.
	    int focusIndex = termToIndex.get(focusWord);

            // write the term index into the compressed for the document for
            // later corpus reprocessing
            dos.writeInt(focusIndex);
        }

        document.close();
        
        dos.close();
        byte[] docAsBytes = compressedDocument.toByteArray();

        // Once the document is finished, write the compressed contents to the
        // corpus stream
        synchronized(compressedDocumentsWriter) {
            // Write how many terms were in this document after filtering
            compressedDocumentsWriter.writeInt(unfilteredTokens);
            compressedDocumentsWriter.write(docAsBytes, 0, docAsBytes.length);
        }
    }
    
    /**
     * Computes the reflective semantic vectors for word meanings
     *
     * @param properties {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        try {
            // Wrap the call to avoid having all the code in a try/catch.  This
            // is for improved readability purposes only.
            processSpace();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Computes the reflective semantic vectors for word meanings
     */
    private void processSpace() throws IOException {
        LOGGER.info("generating reflective vectors");
        compressedDocumentsWriter.close();
        int numDocuments = documentCounter.get();
        termToIndexVector.clear();
        indexToTerm = new String[termToIndex.size()];
        for (Map.Entry<String,Integer> e : termToIndex.entrySet())
            indexToTerm[e.getValue()] = e.getKey();

        // Read in the compressed version of the corpus, re-processing each
        // document to build up the document vectors
        DataInputStream corpusReader = new DataInputStream(
            new BufferedInputStream(new FileInputStream(compressedDocuments)));

        // Set up the concurrent data structures so we can reprocess the
        // documents concurrently using a work queue
        final BlockingQueue<Runnable> workQueue =
            new LinkedBlockingQueue<Runnable>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
            Thread t = new WorkerThread(workQueue);
            t.start();
        }
        final Semaphore documentsRerocessed = new Semaphore(0);         

        for (int d = 0; d < numDocuments; ++d) {
            final int docId = d;

            // This value already has any filtered tokens taken into account,
            // i.e. in only counts those tokens that remain after filtering
            int tokensInDoc = corpusReader.readInt();
            // Read in the document
            final int[] doc = new int[tokensInDoc];
            for (int i = 0; i < tokensInDoc; ++i)
                doc[i] = corpusReader.readInt();

            workQueue.offer(new Runnable() {
                    public void run() {
                        // This method creates the document vector and then adds
                        // that document vector with the reflective semantic
                        // vector for each word occurring in the document
                        LOGGER.fine("reprocessing doc #" + docId);
                        processIntDocument(docToVector.get(docId), doc);
                        documentsRerocessed.release();
                    }
                });
        }
        corpusReader.close();

        // Wait until all the documents have been processed
        try {
            documentsRerocessed.acquire(numDocuments);
        } catch (InterruptedException ie) {
            throw new Error("interrupted while waiting for documents to " +
                            "finish reprocessing", ie);
        }        
        LOGGER.fine("finished reprocessing all documents");

    }

    /**
     * Processes the compressed version of a document where each integer
     * indicates that token's index, adding the document's vector to the
     * reflective semantic vector each time a term occurs in the document.
     *
     * @param docVector the vector of the document that is being processed
     * @param document the document to be processed where each {@code int} is a
     *        term index
     *
     * @return the number of contexts present in this document
     */
    private void processIntDocument(IntegerVector docVector, int[] document) {

        // Make one pass through the document to build the document vector.
        for (int termIndex : document) {
            IntegerVector reflectiveVector = 
                termToReflectiveSemantics.get(indexToTerm[termIndex]);
            // Lock on the term's vector to prevent another thread from updating
            // it concurrently
            synchronized(reflectiveVector) {
                VectorMath.add(reflectiveVector, docVector);
            }
        }
    }

    /**
     * {@inheritDoc} Note that all words will still have an index vector
     * assigned to them, which is necessary to properly compute the semantics.
     *
     * @param semanticsToRetain the set of words for which semantics should be
     *        computed.
     */
    public void setSemanticFilter(Set<String> semanticsToRetain) {
        semanticFilter.clear();
        semanticFilter.addAll(semanticsToRetain);
    }

    /**
     * Atomically adds the values of the index vector to the semantic vector.
     * This is a special case addition operation that only iterates over the
     * non-zero values of the index vector.
     */
    private static void add(IntegerVector semantics, TernaryVector index) {
        // Lock on the semantic vector to avoid a race condition with another
        // thread updating its semantics.  Use the vector to avoid a class-level
        // lock, which would limit the concurrency.
        synchronized(semantics) {
            for (int p : index.positiveDimensions())
                semantics.add(p, 1);
            for (int n : index.negativeDimensions())
                semantics.add(n, -1);
        }
    }
}
