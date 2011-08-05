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

package edu.ucla.sspace.purandare;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Assignments;
import edu.ucla.sspace.clustering.ClutoClustering;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.AtomicGrowingMatrix;
import edu.ucla.sspace.matrix.AtomicMatrix;
import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.SparseRowMaskedMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;
import edu.ucla.sspace.matrix.SparseOnDiskMatrix;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.SparseArray;
import edu.ucla.sspace.util.SparseHashArray;
import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.VectorMath;
import edu.ucla.sspace.vector.Vectors;

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

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * An implementation of the word sense induction algorithm described by
 * Purandare and Pedersen.  This implementation is based on the following
 * paper: <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Amruta Purandare and Ted
 *   Pedersen.  (2004) Word Sense Discrimination by Clustering Contexts in
 *   Vector and Similarity Spaces.  <i>Proceedings of Conference on
 *   Computational Natural Language Learning (CoNLL)</i>, pp. 41-48, May 6-7,
 *   2004, Boston, MA.</li>
 * 
 * </ul> 
 *
 *
 * This class offers one configurable parameter.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #MAX_CONTEXT_PER_WORD}
 *      </b></code> <br>
 *      <i>Default:</i> {@link Integer.MAX_VALUE}
 *
 * <dd style="padding-top: .5em">This property sets the upper-bound on the
 *      maximum number of contexts to be clustered for a single word.  If there
 *      are fewer of contexts than this value, all of the contexts will be used.
 *      Users should consider setting this value if a large corpus is to be
 *      used, or if the corpus contains many frequently used words after
 *      filtering.<p>
 *
 * </dl></p> 
 *
 * @author David Jurgens
 */
public class PurandareFirstOrder implements SemanticSpace {

    private static final Logger LOGGER = 
        Logger.getLogger(PurandareFirstOrder.class.getName());

    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.purandare.PurandareFirstOrder";

    /**
     * The property to set the upper-bound on the maximum number of contexts to
     * be clustered for a single word.
     */
    public static final String MAX_CONTEXTS_PER_WORD = 
        PROPERTY_PREFIX + ".maxContexts";

    /**
     * Map that pairs the word with its position in the original term-document
     * matrix.
     */
    private final Map<String,Integer> termToIndex;

    /**
     * A mapping from term to the sense-induced semantic vectors.  The first
     * sense of the term will be the token itself, while addition senses will be
     * denoted with a "-" and a sense number appended to the token.
     */
    private final Map<String,DoubleVector> termToVector;

    /**
     * The window size for identifying co-occurence words that have the
     * potential to be features.
     */
    private final int windowSize;

    /**
     * The window size used for generating feature vectors. 
     */ 
    private final int contextWindowSize;

    /**
     * The matrix used for storing weight co-occurrence statistics of those
     * words that occur both before and after.
     */
    private final AtomicMatrix cooccurrenceMatrix;

    /**
     * A count for how many times each term appears in the corpus.
     */
    private final List<AtomicInteger> termCounts;

    /**
     * A compressed version of the corpus that is built as the text version is
     * being processed.  The file contains documents represented as an integer
     * for the number of tokens in that document followed by the indices for all
     * of the tokens in the order that they appeared.
     *
     * @see #processIntDocument(int[],Matrix,int,BitSet[],Set[])
     */
    private File compressedDocuments;

    /**
     * The output stream used to the write the {@link #compressedDocuments} file
     * as the text documents are being processed.
     */
    private DataOutputStream compressedDocumentsWriter;

    /**
     * A counter for the number of documents seen in the corpus.
     */
    private final AtomicInteger documentCounter;

    /**
     * The maximum number of contexts allowable for any word.
     */
    private final int maxContextsPerWord;

    /**
     * The number that keeps track of the index values of words
     */
    private int wordIndexCounter;

    /**
     * Creates a new instance of {@code PurandareFirstOrder} using the system
     * properties for configuration
     */
    public PurandareFirstOrder() {
        this(System.getProperties());
    }

    /**
     * Creates a new instance of {@code PurandareFirstOrder} using the provided
     * properties for configuration
     */
    public PurandareFirstOrder(Properties props) {
	cooccurrenceMatrix = new AtomicGrowingMatrix();
        termToIndex = new ConcurrentHashMap<String,Integer>();
        termToVector = new ConcurrentHashMap<String,DoubleVector>();
        termCounts = new CopyOnWriteArrayList<AtomicInteger>();
        windowSize = 5;
        contextWindowSize = 20;
        documentCounter = new AtomicInteger(0);        
        String maxContextsProp = props.getProperty(MAX_CONTEXTS_PER_WORD);
        if (maxContextsProp == null)
            maxContextsPerWord = Integer.MAX_VALUE;
        else {
            int i = Integer.parseInt(maxContextsProp);
            if (i <= 0)
                throw new IllegalArgumentException(
                    "The number of contexts must be a positive number");
            maxContextsPerWord = i;
        }
        try {
            compressedDocuments = 
                File.createTempFile("petersen-documents",".dat");
            compressedDocumentsWriter = new DataOutputStream(
                new BufferedOutputStream(
                    new FileOutputStream(compressedDocuments)));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void  processDocument(BufferedReader document) throws IOException {
        documentCounter.getAndIncrement();
	Queue<String> nextWords = new ArrayDeque<String>();
	Queue<String> prevWords = new ArrayDeque<String>();
		
	Iterator<String> documentTokens = 
	    IteratorFactory.tokenizeOrdered(document);
		
	String focus = null;
		
        ByteArrayOutputStream compressedDocument = 
            new ByteArrayOutputStream(4096);
        DataOutputStream dos = new DataOutputStream(compressedDocument);
        int tokens = 0; // count how many are in this document
        int unfilteredTokens = 0; 
	//Load the first windowSize words into the Queue		
	for(int i = 0;  i < windowSize && documentTokens.hasNext(); i++)
	    nextWords.offer(documentTokens.next());
			
	while(!nextWords.isEmpty()) {
            tokens++;

	    // Load the top of the nextWords Queue into the focus word
	    focus = nextWords.remove();

	    // Add the next word to nextWords queue (if possible)
	    if (documentTokens.hasNext()) {		
		String windowEdge = documentTokens.next();
		nextWords.offer(windowEdge);
	    }			

	    // If the filter does not accept this word, skip the semantic
	    // processing, continue with the next word
	    if (focus.equals(IteratorFactory.EMPTY_TOKEN)) {
                // Mark the token as empty using a negative term index in the
                // compressed form of the document
                dos.writeInt(-1);
		// shift the window
		prevWords.offer(focus);
		if (prevWords.size() > windowSize)
		    prevWords.remove();
		continue;
	    }
		
	    int focusIndex = getIndexFor(focus);
            // write the term index into the compressed for the document for
            // later corpus reprocessing
            dos.writeInt(focusIndex);
            // Update the occurrences of this token
            termCounts.get(focusIndex).incrementAndGet();
            unfilteredTokens++;
            
	    // Iterate through the words occurring after and add values
	    for (String after : nextWords) {
		// skip adding co-occurence values for words that are not
		// accepted by the filter
		if (!after.equals(IteratorFactory.EMPTY_TOKEN)) {
		    int index = getIndexFor(after);
                    cooccurrenceMatrix.addAndGet(focusIndex, index, 1);		 
                }
	    }

	    for (String before : prevWords) {
		// skip adding co-occurence values for words that are not
		// accepted by the filter
		if (!before.equals(IteratorFactory.EMPTY_TOKEN)) {
		    int index = getIndexFor(before);
                    cooccurrenceMatrix.addAndGet(focusIndex, index, 1);
                }
	    }
	    		            
	    // last, put this focus word in the prev words and shift off the
	    // front if it is larger than the window
	    prevWords.offer(focus);
	    if (prevWords.size() > windowSize)
		prevWords.remove();
	}

        dos.close();
        byte[] docAsBytes = compressedDocument.toByteArray();

        // Once the document is finished, write the compressed contents to the
        // corpus stream
        synchronized(compressedDocumentsWriter) {
            // Write how many terms were in this document
            compressedDocumentsWriter.writeInt(tokens);
            compressedDocumentsWriter.writeInt(unfilteredTokens);
            compressedDocumentsWriter.write(docAsBytes, 0, docAsBytes.length);
        }
    } 

    /**
     * Returns the index in the co-occurence matrix for this word.  If the word
     * was not previously assigned an index, this method adds one for it and
     * returns that index.
     */
    private final int getIndexFor(String word) {
	Integer index = termToIndex.get(word);
	if (index == null) {	 
	    synchronized(this) {
		// recheck to see if the term was added while blocking
		index = termToIndex.get(word);
		// if another thread has not already added this word while the
		// current thread was blocking waiting on the lock, then add it.
		if (index == null) {
		    int i = wordIndexCounter++;
                    // Add a new counter for this term.  Because the
                    // wordIndexCounter starts at zero, so the next index will
                    // be the last index in the termCounts list.
                    termCounts.add(new AtomicInteger(0));
		    termToIndex.put(word, i);
		    return i; // avoid the auto-boxing to assign i to index
		}
	    }
	}
	return index;
    }

        /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
	// If no documents have been processed, it will be empty		
	return Collections.unmodifiableSet(termToVector.keySet());
    }		

    /**
     * {@inheritDoc}
     */
    public DoubleVector getVector(String word) {        
        return termToVector.get(word);
    }

    /**
     * {@inheritDoc}
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
     * Calculates the first order co-occurrence statics to determine the feature
     * set for each term, then clusters the feature vectors for each terms
     * contexts and finally induces the sense-specific vectors for each term.
     */
    @SuppressWarnings("unchecked")
    private void processSpace() throws IOException {
        compressedDocumentsWriter.close();        

        // Generate the reverse index-to-term mapping.  We will need this for
        // assigning specific senses to each term
        String[] indexToTerm = new String[termToIndex.size()];
        for (Map.Entry<String,Integer> e : termToIndex.entrySet())
            indexToTerm[e.getValue()] = e.getKey();
        
        // Compute how many terms were in the corpus.  We will need this for
        // determining the log-likelihood for all co-occurrences.
        int corpusSize = 0;
        for (AtomicInteger i : termCounts)
            corpusSize += i.get();
        final int uniqueTerms = cooccurrenceMatrix.rows();

        LOGGER.info("calculating term features");
        // Create a set for each term that contains the term indices that are
        // determined to be features for the term, i.e. not all co-occurrences
        // will count as the features.  The feature set for each term is
        // determined by computing the log-likelihood for each co-occurrence and
        // only keeping those terms whos l-l value is above a certain threshold.
        final BitSet[] termFeatures = new BitSet[wordIndexCounter];        
        for (int termIndex = 0; termIndex < uniqueTerms; ++termIndex) {
            String term = indexToTerm[termIndex];            
            termFeatures[termIndex] = calculateTermFeatures(term, corpusSize);
        }

        LOGGER.info("reprocessing corpus to generate feature vectors");
        
        // Set up the concurrent data structures so we can process the documents
        // concurrently
        final BlockingQueue<Runnable> workQueue =
            new LinkedBlockingQueue<Runnable>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
            Thread t = new WorkerThread(workQueue);
            t.start();
        }
        final Semaphore termsProcessed = new Semaphore(0); 
        
        for (int termIndex = 0; termIndex < uniqueTerms; ++termIndex) {
            
            final String term = indexToTerm[termIndex];
            final int i = termIndex;
            workQueue.offer(new Runnable() {
                    public void run() {
                        try {
                        LOGGER.fine(String.format(
                            "processing term %6d/%d: %s", i, uniqueTerms,term));
                        Matrix contexts = getTermContexts(i, termFeatures[i]);
                        senseInduce(term, contexts);
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        } finally {
                            termsProcessed.release();
                        }                       
                    }
                });
        }
        
        // Wait until all the documents have been processed
        try {
            termsProcessed.acquire(uniqueTerms);
        } catch (InterruptedException ie) {
            throw new Error("interrupted while waiting for terms to " +
                            "finish reprocessing", ie);
        }        
        LOGGER.info("finished reprocessing all terms");
    }

    private BitSet calculateTermFeatures(String term, int corpusSize) {
        int termIndex = termToIndex.get(term);
        LOGGER.fine(String.format("Calculating feature set for %6d/%d: %s",
                                  termIndex, cooccurrenceMatrix.rows(), term));
        DoubleVector cooccurrences = cooccurrenceMatrix.getRowVector(termIndex);
        int termCount = termCounts.get(termIndex).get();
        BitSet validFeatures = new BitSet(wordIndexCounter);
        
        // For each of the co-occurring terms, calculate the log-likelikehood
        // value for that term's occurrences.  Only terms whose value is above
        // 3.841 will be counted as features
        for (int co = 0; co < cooccurrences.length(); ++co) {
            // Form the contingency table:
            //  a   b
            //  c   d
            double count = cooccurrences.get(co);
            // Don't include words that never co-occur as features
            if (count == 0)
                continue;
            
            // a = the number of times they both co-occur
            double a = count;
            // b = the number of times co occurs without term
            double b = termCounts.get(co).get() - count;
            // c = the number of times term occurs without co
            double c = termCount - count;
            // d = the number of times neither co-occurrence
            double d = corpusSize - (a +  b + c);
            
            double logLikelihood = logLikelihood(a, b, c, d);
            if (logLikelihood > 3.841)
                validFeatures.set(co);
        }
        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine(term + " had " + validFeatures.cardinality() 
                        + " features");
        return validFeatures;
    }

    /**
     * For the specified term, reprocesses the entire corpus using the term's
     * features to construct a matrix of all the contexts in which the term
     * appears.  If the term occurs in more contexts than is allowed in the
     * {@link #maxContextsPerWord}, the random subset of the contexts is
     * returned.
     *
     * @param termIndex the index of the term for which the context matrix
     *        should be generated
     * @param termFeatures the set of term indices that are valid features when
     *        in the context of {@code termIndex}.
     *
     * @return a {@code Matrix} where each row is a different context for the
     *         term in the corpus
     */
    private Matrix getTermContexts(int termIndex, BitSet termFeatures) 
            throws IOException {
        // Reprocess the corpus in binary format to generate the set of context
        // with the appropriate feature vectors
        DataInputStream corpusReader = new DataInputStream(
            new BufferedInputStream(new FileInputStream(compressedDocuments)));

        int documents = documentCounter.get();
        // Use the number of times the term occurred in the corpus to determine
        // how many rows (contexts) in the matrix.
        SparseMatrix contextsForCurTerm = new YaleSparseMatrix(
            termCounts.get(termIndex).get(), termToIndex.size());
        int contextsSeen = 0;
        for (int d = 0; d < documents; ++d) {
            final int docId = d;

            int tokensInDoc = corpusReader.readInt();
            int unfilteredTokens = corpusReader.readInt();
            // Read in the document
            int[] doc = new int[tokensInDoc];
            for (int i = 0; i < tokensInDoc; ++i)
                doc[i] = corpusReader.readInt();

            int contextsInDoc = 
                processIntDocument(termIndex, doc, contextsForCurTerm,
                                   contextsSeen, termFeatures);
            contextsSeen += contextsInDoc;
        }
        corpusReader.close();

        // If the term is to be processed using fewer than all of its contexts,
        // then randomly select the maximum allowable contexts from the matrix
        if (maxContextsPerWord < Integer.MAX_VALUE &&
                contextsForCurTerm.rows() > maxContextsPerWord) {
            BitSet randomContexts = Statistics.randomDistribution(
                maxContextsPerWord, contextsForCurTerm.rows());
            contextsForCurTerm = 
                new SparseRowMaskedMatrix(contextsForCurTerm, randomContexts);
        }
        
        return contextsForCurTerm;
    }

    /**
     * Given a matrix for the term where each row is a different context,
     * clusters the rows to identify how many senses the word has.
     *
     * @param term the term for which the senses will be discovered
     * @param contexts a matrix containing all the contexts in which {@code
     *        term} appear, with one context per row
     */
    private void senseInduce(String term, Matrix contexts) throws IOException {
        LOGGER.fine("Clustering " + contexts.rows() + " contexts for " + term);

        // For terms with fewer than seven contexts, set the number of potential
        // clusters lower
        int numClusters = Math.min(7, contexts.rows());
        
        // Cluster each of the rows into seven groups.  Note that if the term is
        // not purely alphabetic (i.e. contains number of other symbols), don't
        // bother clustering it.  This is done to reduce the computation time,
        // and to avoid clustering non-meaningful terms such as '.' or '''
        if (!(term.matches("[a-zA-z]+") && numClusters > 6)) { // special case            
            SparseDoubleVector meanSenseVector = 
                new CompactSparseVector(termToIndex.size());            
            int rows = contexts.rows();
            for (int row = 0; row < rows; ++row) 
                VectorMath.add(meanSenseVector, contexts.getRowVector(row));
            termToVector.put(term, meanSenseVector);
            return;
        }

        Assignments clusterAssignment = 
            new ClutoClustering().cluster(contexts, numClusters, 
                                          ClutoClustering.Method.AGGLOMERATIVE,
                                          ClutoClustering.Criterion.UPGMA);
        
        LOGGER.fine("Generative sense vectors for " + term);
        
        // For each of the clusters, compute the mean sense vector
        int[] clusterSize = new int[numClusters];

        // Use CompactSparseVector to conserve memory given the potentially
        // large number of sense vectors
        SparseDoubleVector[] meanSenseVectors =
            new CompactSparseVector[numClusters];

        for (int i = 0; i < meanSenseVectors.length; ++i)
            meanSenseVectors[i] = new CompactSparseVector(termToIndex.size());

        // For each of the contexts, determine which cluster it was in and sum
        // it value with the other contexts
        for (int row = 0; row < clusterAssignment.length(); ++row) {
            // Check whether this row was assigned a cluster
            if (clusterAssignment.get(row).assignments().length == 0)
                continue;                
            int assignment = clusterAssignment.get(row).assignments()[0];                
            clusterSize[assignment]++;

            DoubleVector contextVector = contexts.getRowVector(row);
            VectorMath.add(meanSenseVectors[assignment], contextVector);
        }
        
        // For each of the clusters with more than 2% of the contexts, generage
        // an average sense vectors.  For those clusters with less than that
        // amount, discard them.
        int senseCounter = 0;
        for (int i = 0; i < numClusters; ++i) {
            int size = clusterSize[i];
            if (size / (double)(contexts.rows()) > 0.02) {
                String termWithSense = (senseCounter == 0)
                    ? term : term + "-" + senseCounter;
                senseCounter++;
                termToVector.put(termWithSense,meanSenseVectors[i]);
            }
        }
        LOGGER.fine("Discovered " + senseCounter + " senses for " + term);
    }

    /**
     * Processes the compressed version of a document where each integer
     * indicates that token's index and identifies all the contexts for the
     * target word, adding them as new rows to the context matrix.
     *
     * @param termIndex the term whose contexts should be extracted
     * @param document the document to be processed where each {@code int} is a
     *        term index
     * @param contextMatrix the matrix that will contain all the contexts for
     *        the term with {@code termIndex} when this method returns
     * @param rowStart the next row index in the matrix where a new context can
     *        be added
     * @param featuresForTerm a mapping from term index to the set of other term
     *        indices that are valid feature for that term
     *
     * @return the number of contexts present in this document
     */
    private int processIntDocument(int termIndex, int[] document, 
                                   Matrix contextMatrix, 
                                   int rowStart, 
                                   BitSet featuresForTerm) {
        int contexts = 0;
        for (int i = 0; i < document.length; ++i) {

            int curToken = document[i];
            // Skip processing tokens that are not the current focus
            if (curToken != termIndex)
                continue;

            // Buffer the count of how many times each feature appeared in the
            // context.
            SparseArray<Integer> contextCounts = new SparseHashArray<Integer>();
        
            // Process all the tokes to the left (prior) to the current token;
            for (int left = Math.max(i - contextWindowSize, 0); 
                     left < i; ++left) {
                // NOTE: this token value could be -1 if the token's original
                // text was filtered out from the corpus, i.e. was EMPTY_TOKEN
                int token = document[left];
                // Only count co-occurrences that are valid features for the
                // current token
                if (token >= 0 && featuresForTerm.get(token)) {
                    Integer count = contextCounts.get(token);
                    contextCounts.set(token, (count == null) ? 1 : count + 1);
                }
            }

            // Process all the tokes to the right (after) to the current token;
            int end = Math.min(i + contextWindowSize, document.length);
            for (int right = i + 1; right < end; ++right) {
                int token = document[right];
                // Only count co-occurrences that are valid features for the
                // current token
                if (token >= 0 && featuresForTerm.get(token)) {
                    Integer count = contextCounts.get(token);
                    contextCounts.set(token, (count == null) ? 1 : count + 1);
                }
            }
            
            // Each word in the document represents a new context, so the
            // specific context instance can be determined from the current word
            // and the number of previously process words
            int curContext = rowStart + contexts;
            for (int feat : contextCounts.getElementIndices()) {
                //System.out.println("setting row: " + curContext + ", col: " + feat);
                contextMatrix.set(curContext, feat, contextCounts.get(feat));
            }
            // If the current token wasn't skipped, indicate that another
            // context was seen
            contexts++;
        }
        return contexts;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        // The vector length is dependent upon the total number of features seen
        return termToIndex.size();
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
	return "purandare-petersen";
    }

    /**
     * Returns the log-likelihood of the contingency table made up of the four
     * values.
     */
    private static double logLikelihood(double a, double b, 
                                        double c, double d) {
        // Table set up as:
        //  a   b
        //  c   d
        double col1sum = a + c;
        double col2sum = b + d;
        double row1sum = a + b;
        double row2sum = c + d;
        double sum = row1sum + row2sum;
        
        // Calculate the expected values for a, b, c, d
        double aExp = (row1sum / sum) * col1sum;
        double bExp = (row1sum / sum) * col2sum;
        double cExp = (row2sum / sum) * col1sum;
        double dExp = (row2sum / sum) * col2sum;

        // log(0) = Infinity, which messes up the calcuation.  Therefore, check
        // whether the value is zero before calculating its contribution.
        double aVal = (a == 0) ? 0 : a * Math.log(a / aExp);
        double bVal = (b == 0) ? 0 : b * Math.log(b / bExp);
        double cVal = (c == 0) ? 0 : c * Math.log(c / cExp);
        double dVal = (d == 0) ? 0 : d * Math.log(d / dExp);

        return 2 * (aVal + bVal + cVal + dVal);            
    }
}
