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

package edu.ucla.sspace.tri;

import edu.ucla.sspace.common.Filterable;

import edu.ucla.sspace.ri.RandomIndexing;

import edu.ucla.sspace.temporal.TemporalSemanticSpace;

import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

import java.util.logging.Logger;


/**
 * A simplified version of {@link TemporalRandomIndexing} that imposes
 * restrictions on the document input ordering to improve efficiency at the cost
 * of functionality.  Specifically, this class assumes: <ol>
 *
 *  <li> Documents will be processed in an on-line manner such that all
 *  documents that comprise a semantic slice will be contiguous.
 *
 *  <li> After a semantic slice has been built and processed, it does not need
 *  to be referenced any longer may be discarded.
 *
 * </ol>
 *
 * The first property requires that the intial data be sorted according to some
 * predetermined ordering.  The second property limits the semantics that are
 * retained at any given time period. <p>
 *
 * Because each slice is calculated and then discarded, this class provides a
 * way for users to be notified when a semantic slice has been completed.  Users
 * may add a {@link Runnable} via the {@link #addPartitionHook(Runnable)} method.
 * When the input stream of documents partitions the current semantic slice from
 * the next (i.e. the slice is complete), each runnable will be invoked.  This
 * allows users to perform any operations on the slice as necessary, such as
 * save it to disk or compute various statistics.<p>
 *
 * This class implements {@link Filterable}, which allows for fine-grained
 * control of which semantics are retained.  The {@link #setSemanticFilter(Set)}
 * method can be used to speficy which words should have their semantics
 * retained.  Note that the words that are filtered out will still be used in
 * computing the semantics of <i>other</i> words.  This behavior is intended for
 * use with a large corpora where retaining the semantics of all words in memory
 * is infeasible.<p>
 *
 * This base class defines the following configurable properties:
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #WINDOW_SIZE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WINDOW_SIZE}
 *
 * <dd style="padding-top: .5em">This variable sets the number of words before
 *      and after that are counted as co-occurring.  With the default value,
 *      {@code 5} words are counted before and {@code 5} words are counter
 *      after.  This class always uses a symmetric window. <p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #VECTOR_LENGTH_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_VECTOR_LENGTH}
 *
 * <dd style="padding-top: .5em">This variable sets the number of dimensions to
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
 * Due to the ordered nature of its processing, great care must be used when
 * invoking {@code processDocument} from multiple threads.  Multiple threads may
 * order the documents such that the time stamps at semantic slice boundaries
 * overlap.  This may causes the {@link shouldPartitionSpace(long)} method to
 * return true for slices with only a single document.  Subclasses must make it
 * clear whether any such multithreading behavior is permissable and how to
 * correctly invoke it to avoid triggering semantic slice boundary edge cases.<p>
 *
 * In its base behavior, instances of this class do <i>not</i> support the
 * optional {@code getTimeSteps}, {@code getVectorAfter}, {@code
 * getVectorBefore} and {@code getVectorBetween} methods.  However, subclasses
 * may add this functionality.<p>
 *
 * @see RandomIndexing
 * @see TemporalRandomIndexing
 * @see TemporalSemanticSpace
 *
 * @author David Jurgens
 */
public abstract class OrderedTemporalRandomIndexing 
        implements TemporalSemanticSpace, Filterable {

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
    "edu.ucla.sspace.tri.OrderedTemporalRandomIndexing";

    /**
     * The property to specify the fully qualified named of a {@link
     * edu.ucla.sspace.ri.PermutationFunction} if using permutations is enabled.
     */
    public static final String PERMUTATION_FUNCTION_PROPERTY = 
    PROPERTY_PREFIX + ".permutationFunction";

    /**
     * The property to specify whether the index vectors for co-occurrent words
     * should be permuted based on their relative position.
     */
    public static final String USE_PERMUTATIONS_PROPERTY = 
    PROPERTY_PREFIX + ".usePermutations";

    /**
     * Specifies whether to use a sparse encoding for each word's semantics,
     * which saves space but requires more computation.
     */
    public static final String USE_SPARSE_SEMANTICS_PROPERTY = 
    PROPERTY_PREFIX + ".sparseSemantics";

    /**
     * The property to specify the number of dimensions to be used by the index
     * and semantic vectors.
     */
    public static final String VECTOR_LENGTH_PROPERTY = 
    PROPERTY_PREFIX + ".vectorLength";

    /**
     * The property to specify the number of words to view before and after each
     * word in focus.
     */
    public static final String WINDOW_SIZE_PROPERTY = 
    PROPERTY_PREFIX + ".windowSize";

    /**
     * The default number of dimensions to be used by the index and semantic
     * vectors.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 10000;

    /**
     * The default number of words to view before and after each word in focus.
     */
    public static final int DEFAULT_WINDOW_SIZE = 4; // +4/-4

    /**
     * The logger used for instances of this class
     */
    private static final Logger LOGGER =
    Logger.getLogger(OrderedTemporalRandomIndexing.class.getName());
    
    /**
     * The collection of hooks that are to be run prior to every time this
     * instances partitions its semantic space.
     */
    protected final Collection<Runnable> partitionHooks;

    /**
     * The current semantic slice, which is updated as new documents are
     * processed and has its semantics cleared when {@link
     * #shouldPartitionSpace(long)} returns {@code true}.
     */
    protected final RandomIndexing currentSlice;

    /**
     * The most recent time stamp seen during the current semantic slice 
     */
    protected Long endTime;

    /**
     * The least recent time stamp seen during the current semantic slice
     */
    protected Long startTime;
    
    /**
     * Creates an instance of {@code OrderedTemporalRandomIndexing} using
     * the system properties to configure the behavior.
     */
    public OrderedTemporalRandomIndexing() {
        this(System.getProperties());
    }

    /**
     * Creates an instance of {@code OrderedTemporalRandomIndexing} using
     * the system properties to configure the behavior.
     *
     * @param props the properties used to configure this instance
     */
    public OrderedTemporalRandomIndexing(Properties props) {

        partitionHooks = new ArrayList<Runnable>();
        
        // Translate the On-line TRI properties into RI properties
        Properties riProps = new Properties();

        // Conditionally assign any of the specified Ordered TRI properties to
        // the RI instance if they were set
        String prop = null;
        if ((prop = props.getProperty(VECTOR_LENGTH_PROPERTY)) != null)
            riProps.put(RandomIndexing.VECTOR_LENGTH_PROPERTY, prop);

        if ((prop = props.getProperty(WINDOW_SIZE_PROPERTY)) != null)
            riProps.put(RandomIndexing.WINDOW_SIZE_PROPERTY, prop);

        if ((prop = props.getProperty(USE_SPARSE_SEMANTICS_PROPERTY)) != null)
            riProps.put(RandomIndexing.USE_SPARSE_SEMANTICS_PROPERTY, prop);

        currentSlice = new RandomIndexing(riProps);
    }

    /**
     * Adds the provided {@code Runnable} to the list of hooks that will be
     * invoked immediately <i>prior</i> to the partitioning of this space.  This
     * method provides a mechanism for users to perform additional processing on
     * the current semantic slice of this space before it is discarded.
     *
     * @param hook a runnable to be invoked.
     */
    public void addPartitionHook(Runnable hook) {
        partitionHooks.add(hook);
    }

    /**
     * Clears the semantic content of this space as a part of the partitioning
     * processing.
     */
    protected void clear() {
        // Reset the current semantic slice
        currentSlice.clearSemantics();

        // Clear the start and end times, which will be reset after the next
        // document is processed following the clear() operation
        startTime = null;
        endTime = null;
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        processDocument(document, System.currentTimeMillis());
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document, long timeStamp) 
        throws IOException {

        if (startTime != null && shouldPartitionSpace(timeStamp)) {
            for (Iterator<Runnable> it = partitionHooks.iterator(); 
                 it.hasNext(); ) {
                Runnable r = it.next();
                // If one of the hooks has errors, remove it from processing but
                // don't stop processing.
                try {
                    r.run();
                } catch (Throwable t) {
                    LOGGER.warning("Partition hook " + r + " caused the " + 
                           "following exception during its operations" +
                           t + " and is being removed");
                    it.remove();
                }
            }
            clear();
        }
        
        // Update the semantic slice ranges as necessary
        if (startTime == null) {
            startTime = timeStamp;
            endTime = timeStamp;
        }
        else if (endTime < timeStamp)
            timeStamp = endTime;

        currentSlice.processDocument(document);
    }

    /**
     * Sets a filter such that only words that are in the set have their
     * semantics retained by this instance.  Note that all words will still have
     * an index vector assigned to them, which is necessary to properly compute
     * the semantics.
     *
     * @param semanticsToRetain the set of words for which semantics should be
     *        computed.
     */
    public void setSemanticFilter(Set<String> semanticsToRetain) {
        currentSlice.setSemanticFilter(semanticsToRetain);
    }

    /**
     * Returns {@code true} if the current contents of this semantic space
     * should be partitioned and discarded <i>prior</i> to processing the next
     * document with the specified time stamp.  Subclasses should use this
     * method to specify the conditions under which the temporal semantics are
     * to be divided.
     *
     * @param nextTimeStamp the time stamp of the next document that has yet to
     *        be processed
     *
     * @return {@code true} if the current contents of this space should be
     *         partitioned and discarded before processing the next document
     */
    protected abstract boolean shouldPartitionSpace(long nextTimeStamp);

    /**
     * {@inheritDoc}
     */
    public Long startTime() {
        return startTime;
    }

    /**
     * {@inheritDoc}
     */
    public Long endTime() {
        return endTime;
    }

    /**
     * {@inheritDoc}
     */ 
    public abstract String getSpaceName();

    /**
     * <i>Not supported</i>
     *
     * @param word {@inheritDoc}
     *
     * @throws UnsupportedOperationException if called
     */ 
    public SortedSet<Long> getTimeSteps(String word) {
        throw new UnsupportedOperationException(
            "getTimeSteps is not supported");
    }
    
    /**
     * <i>Not supported</i>
     *
     * @param word {@inheritDoc}
     * @param startTime {@inheritDoc}
     *
     * @throws UnsupportedOperationException if called
     */ 
    public Vector getVectorAfter(String word, long startTime) {
        throw new UnsupportedOperationException(
            "getVectorAfter is not supported");
    }

    /**
     * <i>Not supported</i>
     *
     * @param word {@inheritDoc}
     * @param endTime {@inheritDoc}
     *
     * @throws UnsupportedOperationException if called
     */ 
    public Vector getVectorBefore(String word, long endTime) {
        throw new UnsupportedOperationException(
            "getVectorBefore is not supported");
    }

    /**
     * <i>Not supported</i>
     *
     * @param word {@inheritDoc}
     * @param startTime {@inheritDoc}
     * @param endTime {@inheritDoc}
     *
     * @throws UnsupportedOperationException if called
     */ 
    public Vector getVectorBetween(String word, long startTime, 
                     long endTime) {
        throw new UnsupportedOperationException(
            "getVectorBetween is not supported");
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(String word) {
        return currentSlice.getVector(word);
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return currentSlice.getVectorLength();
    }


    /**
     * {@inheritDoc} Note that this set only includes the words that are present
     * in the current semantic slice, which may be a subset of the all the words
     * seen in all semantic slices.
     */
    public Set<String> getWords() {
        return currentSlice.getWords();
    }

    /**
     * Returns an unmodifiable view on the token to {@link TernaryVector}
     * mapping used by this instance.  Any further changes made by this instance
     * to its token to {@code TernaryVector} mapping will be reflected in the
     * return map.
     *
     * @return a mapping from the current set of tokens to the index vector used
     *         to represent them
     */
    public Map<String,TernaryVector> getWordToIndexVector() {
        return currentSlice.getWordToIndexVector();
    }

    /**
     * Does nothing.
     *
     * @param props {@inheritDoc}
     */
    public void processSpace(Properties props) { }

    /**
     * Assigns the token to {@link TernaryVector} mapping to be used by this
     * instance.  The contents of the map are copied, so any additions of new
     * index words by this instance will not be reflected in the parameter's
     * mapping.
     *
     * @param m a mapping from token to the {@code TernaryVector} that should be
     *        used represent it when calculating other word's semantics
     */
    public void setWordToIndexVector(Map<String,TernaryVector> m) {
        currentSlice.setWordToIndexVector(m);
    }

}
