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

package edu.ucla.sspace.hadoop;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.HadoopResourceFinder;
import edu.ucla.sspace.util.ResourceFinder;

import java.io.IOException;
import java.io.IOError;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*;


/**
 * An common implementation that provides functionality for processing a {@link
 * Text} document and recording word co-occurrences to a {@link Mapper.Context}.
 *
 * <p>This class defines the following configurable properties that may be set
 * using {@link Properties} constructor to {@link HadoopJob}.  Note that
 * setting these properties with the {@link System} properties will have no
 * effect on this class.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #WINDOW_SIZE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WINDOW_SIZE}
 *
 * <dd style="padding-top: .5em">This property sets the number of words before
 *      and after that are counted as co-occurring.  With the default value,
 *      {@value #DEFAULT_WINDOW_SIZE} words are counted before and {@value
 *      #DEFAULT_WINDOW_SIZE} words are counter after.  This class always uses a
 *      symmetric window. <p>
 *
 * </dl>
 */
public class CooccurrenceExtractor {

    /**
     * The property used to configure the {@code Mapper} instances' window size,
     * which counts the number of words to view before and after each word in
     * focus.
     */
    public static final String WINDOW_SIZE_PROPERTY =
        "edu.ucla.sspace.hadoop.CooccurrenceExtractor.windowSize";

    /**
     * The default window size if none is specified.  Note that this value is
     * never used, but is provided as the default value when calling {@link
     * Configuration#getInt(String, int)} to get the actual window size.
     */
    public static final int DEFAULT_WINDOW_SIZE = 2;

    /**
     * The default number of words to view before and after each word in focus,
     * which will be counted as co-occurring.
     */
    private final int windowSize;

    /**
     * The set of terms that should have the co-occurences counted for them.
     * This set acts as an inclusive filter, removing terms from the mapper
     * output if not present in the set.  If the set is empty, all terms are
     * accepted as valid.
     */
    private final Set<String> semanticFilter;        
               
    /**
     * Creates an unconfigured {@code CooccurrenceMapper}.
     */
    public CooccurrenceExtractor(Configuration conf) {
        semanticFilter = new HashSet<String>();
        windowSize = conf.getInt(WINDOW_SIZE_PROPERTY,
                                 DEFAULT_WINDOW_SIZE);
    }
 
    /**
     * Takes the {@code document} and writes a set of tuples mapping a word to
     * the other words it co-occurs with and the relative position of those
     * co-occurrences.
     *
     * @param document the document that will be segmented into tokens and
     *        mapped to cooccurrences
     * @param context the context in which this mapper is executing
     */
    public void processDocument(Text document, 
            Mapper<?,?,Text,TextIntWritable>.Context context) 
            throws IOException, InterruptedException {
 	
        Queue<String> prevWords = new ArrayDeque<String>(windowSize);
        Queue<String> nextWords = new ArrayDeque<String>(windowSize);
        Iterator<String> documentTokens = 
            IteratorFactory.tokenizeOrdered(document.toString());

        String focusWord = null;
        Text focusWordWritable = new Text();

        // Prefetch the first windowSize words 
        for (int i = 0; i < windowSize && documentTokens.hasNext(); ++i)
            nextWords.offer(documentTokens.next());
            
        Map<WordCooccurrenceWritable,Integer> occurrenceToCount = 
            new HashMap<WordCooccurrenceWritable,Integer>();

        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();
            focusWordWritable.set(focusWord);

            // Shift over the window to the next word
            if (documentTokens.hasNext()) {
                String windowEdge = documentTokens.next(); 
                nextWords.offer(windowEdge);
            }    
                
            // If we are filtering the semantic vectors, check whether this word
            // should have its semantics calculated.  In addition, if there is a
            // filter and it would have excluded the word, do not keep its
            // semantics around
            boolean calculateSemantics =
                (semanticFilter.isEmpty() 
                 || semanticFilter.contains(focusWord))
                && !focusWord.equals(IteratorFactory.EMPTY_TOKEN);
                
            if (calculateSemantics) {
                    
                int pos = -prevWords.size();
                for (String word : prevWords) {

                    // Skip the addition of any words that are excluded from the
                    // filter set.  Note that by doing the exclusion here, we
                    // ensure that the token stream maintains its existing
                    // ordering, which is necessary when permutations are taken
                    // into account.
                    if (!word.equals(IteratorFactory.EMPTY_TOKEN)) {
                        context.write(focusWordWritable,
                                      new TextIntWritable(word, pos));
                    }
                    ++pos;
                }
                    
                // Repeat for the words in the forward window.
                pos = 1;
                for (String word : nextWords) {
                    // Skip the addition of any words that are excluded from the
                    // filter set.  Note that by doing the exclusion here, we
                    // ensure that the token stream maintains its existing
                    // ordering, which is necessary when permutations are taken
                    // into account.
                    if (!word.equals(IteratorFactory.EMPTY_TOKEN)) {
                        context.write(focusWordWritable,
                                      new TextIntWritable(word, pos));
                    }
                    ++pos;
                }
            }
                
            // Last put this focus word in the prev words and shift off the
            // front of the previous word window if it now contains more words
            // than the maximum window size
            prevWords.offer(focusWord);
            if (prevWords.size() > windowSize) {
                prevWords.remove();
            }
        }    
    }
}
