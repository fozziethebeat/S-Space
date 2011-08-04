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

package edu.ucla.sspace.text;

import edu.ucla.sspace.util.FileResourceFinder;
import edu.ucla.sspace.util.LimitedIterator;
import edu.ucla.sspace.util.ReflectionUtil;
import edu.ucla.sspace.util.ResourceFinder;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * A factory class for generating {@code Iterator<String>} tokenizers for
 * streams of tokens such as {@link BufferedReader} instances.  This class
 * manages all of the internal configurations and properties for how to
 * tokenize.  {@link edu.ucla.sspace.common.SemanticSpace SemanticSpace}
 * instances are encouraged to utilize this class for creating iterators over
 * the tokens in the documents rather than creating the iterators themsevles, as
 * this class may contain additional settings to be applied to which the {@link
 * edu.ucla.sspace.common.SemanticSpace SemanticSpace} instance would not have
 * access.
 *
 * <p>
 *
 * This class offers two configurable parameters for controlling the tokenizing
 * of streams.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #TOKEN_FILTER_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> <i>unset</i>
 *
 * <dd style="padding-top: .5em">This property sets a configuration of a {@link
 *      TokenFilter} that should be applied to all token streams.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #STEMMER_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> <i>unset</i>
 *
 * <dd style="padding-top: .5em">This property sets enables the use of the
 *      {@link Stemmer} on all the tokens returned by iterators of this class.
 *      The property value should be the fully qualified class name of a {@code
 *      Stemmer} class implementation.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #TOKEN_COUNT_LIMIT_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> <i>unset</i>
 *
 * <dd style="padding-top: .5em">This property sets the maximum number of tokens
 *       returned by any iterator returned from this class.  It can be used to
 *       artificially limit the total number of tokens per document.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #COMPOUND_TOKENS_FILE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> <i>unset</i>
 *
 * <dd style="padding-top: .5em">This property sets the name of a file that
 *      Contains all of the recognized compound words (or multi-token tokens)
 *      recognized by any iterators returned by this class.<p>
 *
 * </dl> <p>
 *
 * <p> 
 *
 * Note that tokens will be combined into a compound token prior to filtering.
 * Therefore if filtering is enabled, any compound token should also be
 * permitted by the word filter.<p>
 *
 * Note that this class provides two distinct ways to access the token streams
 * if filtering is enabled.  The {@link #tokenize(BufferedReader) tokenize}
 * method will filter out any tokens without any indication.  This can
 * significantly alter the original ordering of the token stream.  For
 * applications where the original ordering needs to be preserved, the {@link
 * #tokenizeOrdered(BufferedReader) tokenizeOrdered} method should be used
 * instead.  This method will return the {@code IteratorFactor.EMTPY_TOKEN}
 * value to indicate that a token has been removed.  This preserves the original
 * token ordering without requiring applications to do the filtering themselves.
 * Note that If filtering is disabled, the two methods will return the same
 * tokens.<p>
 *
 * This class is thread-safe.
 *
 * @see WordIterator
 * @see TokenFilter
 * @see CompoundWordIterator
 */
public class IteratorFactory {

    /**
     * The signifier that stands in place of a token has been removed from an
     * iterator's token stream by means of a {@link TokenFilter}.  Tokens
     * returned by {@link #tokenizeOrdered(BufferedReader) tokenizeOrdered} may
     * be checked against this value to determine whether a token at that
     * position in the stream would have been returned but was removed.
     */
    public static final String EMPTY_TOKEN = "";

    /** 
     * The prefix for naming publically accessible properties
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.text.TokenizerFactory";

    /**
     * Specifies the {@link TokenFilter} to apply to all iterators generated by
     * this factory
     */
    public static final String TOKEN_FILTER_PROPERTY = 
        PROPERTY_PREFIX + ".tokenFilter";

    /**
     * Specifies the {@link Stemmer} to use on tokens.  If not set, no stemming
     * is done.
     */
    public static final String STEMMER_PROPERTY =
        PROPERTY_PREFIX + ".stemmer";

    /**
     * Specifies the name of a file that contains all the recognized compound
     * tokens
     */
    public static final String COMPOUND_TOKENS_FILE_PROPERTY = 
        PROPERTY_PREFIX + ".compoundTokens";
    
    /**
     * Specifies the name of a file which contains term replacement mappings for
     * a {@code WordReplacementIterator}.
     */
    public static final String TOKEN_REPLACEMENT_FILE_PROPERTY =
        PROPERTY_PREFIX + ".replacementTokens";

    /**
     * Specifices an upper limit on the number of tokens each iterator can
     * return.
     */
    public static final String TOKEN_COUNT_LIMIT_PROPERTY =
        PROPERTY_PREFIX + ".tokenCountLimit";


    /**
     * A list of all the factory properties supported for configuration by the
     * {@link IteratorFactory}.
     */
    public static final Set<String> ITERATOR_FACTORY_PROPERTIES =
        new HashSet<String>();
    
    // Static block for setting the properties
    static {
        ITERATOR_FACTORY_PROPERTIES.add(
                IteratorFactory.TOKEN_FILTER_PROPERTY);
        ITERATOR_FACTORY_PROPERTIES.add(
                IteratorFactory.STEMMER_PROPERTY);
        ITERATOR_FACTORY_PROPERTIES.add(
                IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY);
        ITERATOR_FACTORY_PROPERTIES.add(
                IteratorFactory.TOKEN_REPLACEMENT_FILE_PROPERTY);
        ITERATOR_FACTORY_PROPERTIES.add(
                IteratorFactory.TOKEN_COUNT_LIMIT_PROPERTY);
    }

    /**
     * An optional {@code TokenFilter} to use to remove tokens from document
     */
    private static TokenFilter filter;

    /**
     * The {@link ResourceFinder} used to locate the file-based resources used
     * by the iterator factory.  The default value for this is to read things
     * directly from {@code File} instances.
     */
    private static ResourceFinder resourceFinder = new FileResourceFinder();
    
    /**
     * True if stemming should be done in a word iterator.
     */
    private static Stemmer stemmer;

    /**
     * The maximum number of tokens an iterator may return.
     */
    private static int wordLimit;

    /**
     * An optional {@code Map} used to replace terms returned by iterators.
     */
    private static Map<String, String> replacementMap;

    /**
     * A mapping from a thread that is currently processing tokens to the {@link
     * CompoundWordIterator} doing the tokenizing if compound word support is
     * enabled.  This mapping is required for two reasons.  One to reduce the
     * overhead of creating {@code CompoundWordIterators} by calling {@code
     * reset} on them; and two, to provide a way for any updates to the list of
     * compound words to propagate to the threads that process them.
     */
    private static final Map<Thread,CompoundWordIterator> compoundIterators =
        new HashMap<Thread,CompoundWordIterator>();

    /**
     * The set of compound tokens recognized by the system or {@code null} if
     * none are recognized
     */
    private static Set<String> compoundTokens = null;

    /**
     * Uninstantiable
     */
    private IteratorFactory() { }

    /**
     * Reconfigures the type of iterator returned by this factory based on the
     * specified properties.
     */
    public static synchronized void setProperties(Properties props) {
        wordLimit = Integer.parseInt(
                props.getProperty(TOKEN_COUNT_LIMIT_PROPERTY, "0"));

        String filterProp = 
            props.getProperty(TOKEN_FILTER_PROPERTY);
        filter = (filterProp != null)
            ? TokenFilter.loadFromSpecification(filterProp, resourceFinder)
            : null;
        
        // NOTE: future implementations may interpret the value of this property
        // to decide which stemmer to use
        String stemmerProp = props.getProperty(STEMMER_PROPERTY);
        if (stemmerProp != null)
            stemmer = ReflectionUtil.<Stemmer>getObjectInstance(stemmerProp);

        String compoundTokensProp = 
            props.getProperty(COMPOUND_TOKENS_FILE_PROPERTY);
        if (compoundTokensProp != null) {
            // Load the tokens from file
            compoundTokens = new LinkedHashSet<String>();
            try {
                BufferedReader br = resourceFinder.open(compoundTokensProp);
                for (String line = null; (line = br.readLine()) != null; ) {
                    compoundTokens.add(line);
                }
                // For any currently processing threads, update their mapped
                // iterator with the new set of tokens
                for (Map.Entry<Thread,CompoundWordIterator> e
                     : compoundIterators.entrySet()) {
                    // Create an empy dummy BufferedReader, which will be
                    // discarded upon the next .reset() call to the iterator
                    BufferedReader dummyBuffer =
                        new BufferedReader(new StringReader(""));
                    e.setValue(new CompoundWordIterator(
                                dummyBuffer, compoundTokens));
                }
            } catch (IOException ioe) {
                // rethrow
                throw new IOError(ioe);
            }
        } else {
            // If the user did not specify a set of compound tokens, null out
            // the set, in the event that there was one previously
            compoundTokens = null;
        }

        String replacementProp = 
            props.getProperty(TOKEN_REPLACEMENT_FILE_PROPERTY);
        if (replacementProp != null) {
            try {
                BufferedReader br = resourceFinder.open(replacementProp);
                replacementMap = new HashMap<String, String>();
                String line = null;
                while ((line = br.readLine()) != null) {
                    String[] termReplacement = line.split("\\s+");
                    replacementMap.put(termReplacement[0], termReplacement[1]);
                }
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        } else
            replacementMap = null;
    }

    /**
     * Sets the {@link ResourceFinder} used by the iterator factory to locate
     * its file-based resources when configuring the tokenization.  This method
     * should be set prior to calling {@link #setProperties(Properties)
     * setProperties} to ensure that the resources are accessed correctly.  Most
     * applications will never need to call this method.
     *
     * @param finder the resource finder used to find and open file-based
     *        resources
     */
    public static void setResourceFinder(ResourceFinder finder) {
        resourceFinder = finder;
    }

    /**
     * Tokenizes the contents of the reader according to the system
     * configuration and returns an iterator over all the tokens, excluding
     * those that were removed by any configured {@link TokenFilter}.
     *
     * @param reader a reader whose contents are to be tokenized
     *
     * @return an iterator over all of the optionally-filtered tokens in the
     *         reader
     */
    public static Iterator<String> tokenize(BufferedReader reader) {
        return getBaseIterator(reader, false);
    }

    /**
     * Tokenizes the contents of the string according to the system
     * configuration and returns an iterator over all the tokens, excluding
     * those that were removed by any configured {@link TokenFilter}.
     *
     * @param str a string whose contents are to be tokenized
     *
     * @return an iterator over all of the optionally-filtered tokens in the
     *         string
     */
    public static Iterator<String> tokenize(String str) {
        return tokenize(new BufferedReader(new StringReader(str)));
    }

    /**
     * Tokenizes the contents of the reader according to the system
     * configuration and returns an iterator over all the tokens where any
     * removed tokens have been replaced with the {@code
     * IteratorFactory.EMPTY_TOKEN} value.  Tokens returned by this method may
     * be checked against this value to determine whether a token at that
     * position in the stream would have been returned but was removed.  In
     * doing this, the original order and positioning is retained.
     *
     * @param reader a reader whose contents are to be tokenized
     *
     * @return an iterator over all of the tokens in the reader where any tokens
     *         removed due to filtering have been replaced with the {@code
     *         IteratorFactory.EMPTY_TOKEN} value
     */
    public static Iterator<String> tokenizeOrdered(BufferedReader reader) {
        return getBaseIterator(reader, true);
    }

    /**
     * Tokenizes the contents of the string according to the system
     * configuration and returns an iterator over all the tokens where any
     * removed tokens have been replaced with the {@code
     * IteratorFactory.EMPTY_TOKEN} value.  Tokens returned by this method may
     * be checked against this value to determine whether a token at that
     * position in the stream would have been returned but was removed.  In
     * doing this, the original order and positioning is retained.
     *
     * @param str a string whose contents are to be tokenized
     *
     * @return an iterator over all of the tokens in the string where any tokens
     *         removed due to filtering have been replaced with the {@code
     *         IteratorFactory.EMPTY_TOKEN} value
     */
    public static Iterator<String> tokenizeOrdered(String str) {
        return tokenizeOrdered(new BufferedReader(new StringReader(str)));
    }

    /**
     * Wraps an iterator returned by {@link #tokenizeOrdered(String)
     * tokenizeOrdered} to also include term replacement of tokens.  Terms will
     * be replaced based on a mapping provided through the system configuration.
     *
     * @param reader A reader whose contents are to be tokenized.
     *
     * @return An iterator over all the tokens in the reader where any tokens 
     *         removed due to filtering have been replaced with the {@code
     *         IteratorFactory.EMPTY_TOKEN} value, and tokens may be replaced
     *         based on system configuration.
     */
    public static Iterator<String> tokenizeOrderedWithReplacement(
            BufferedReader reader) {
        Iterator<String> baseIterator = tokenizeOrdered(reader);
        return (replacementMap == null)
            ? baseIterator
            : new WordReplacementIterator(baseIterator, replacementMap);
    }

    /**
     * Returns an iterator for the basic tokenization of the stream before
     * filtering has been applied to the tokens.
     *
     * @param reader a reader whose contents are to be tokenized
     *
     * @return an iterator over the tokens in the stream
     */
    private static Iterator<String> getBaseIterator(BufferedReader reader,
                                                    boolean keepOrdering) {

        // The final iterator is how the stream will be tokenized after all the
        // tokenizing options have been applied.  This value is iteratively set
        // as the options are applied
        Iterator<String> finalIterator = new WordIterator(reader);

        // STEP 1: APPLY TOKEN REPLACEMENT
        if (replacementMap != null)
            finalIterator = 
                new WordReplacementIterator(finalIterator, replacementMap);

        // STEP 2: APPLY COMPOUND TOKENIZING
        if (compoundTokens != null) {
            // Because the initialization step for a CWI has some overhead, use
            // the reset to keep the same tokens.  However, multiple threads may
            // be each using their own CWI, so keep Thread-local storage of what
            // CWI is being used to avoid resetting another thread's iterator.
            CompoundWordIterator cwi = 
                compoundIterators.get(Thread.currentThread());
            if (cwi == null) {
                cwi = new CompoundWordIterator(finalIterator, compoundTokens);
                compoundIterators.put(Thread.currentThread(), cwi);
            } else {
                // NOTE: if the underlying set of valid compound words is ever
                // changed, the iterator returned from the compoundIterators map
                // will have been updated by the setProperties() call, so this
                // method is guaranteed to pick up the latest set of compound
                // words
                cwi.reset(finalIterator);
            }
            finalIterator = cwi;
        } 

        // STEP 3: APPLY TOKEN LIMITING
        if (wordLimit > 0)
            finalIterator = new LimitedIterator<String>(
                    finalIterator, wordLimit);

        // STEP 4: APPLY TOKEN FILTERING
        if (filter != null) {
            finalIterator = (keepOrdering)
                ? new OrderPreservingFilteredIterator(finalIterator, filter)
                : new FilteredIterator(finalIterator, filter);
        }

        // STEP 5: APPLY STEMMING
        if (stemmer != null) 
            finalIterator = new StemmingIterator(finalIterator, stemmer);

        return finalIterator;
    }
}
