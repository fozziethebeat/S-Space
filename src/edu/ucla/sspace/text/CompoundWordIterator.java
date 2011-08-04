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

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.Duple;


/**
 * An iterator over all the tokens in a stream, which supports tokenizing
 * predetermined n-grams as single tokens.  Compound tokenizing is performed
 * greedily so the longest possible match from any tokenizing will be returned.
 *
 * <p>
 *
 * Note that unlike other iterators, the {@link #next() next} method is {@code
 * O(n)} in complexity where {@code n} is the number of unique words starting
 * the set of compound tokens recognized by this iterator.  This may result in a
 * noticeable performance penalty when a large set of compound words is used.
 *
 * <p>
 *
 * This class also provides a {@link #reset(BufferedReader) reset} method to
 * allow resetting the token stream backing this iterator.  If the recognized
 * set of compound words does not change, then this method is prefered over
 * creating a new {@code CompoundWordIterator} as it avoids the initializationg
 * overhead of building the underlying compound recognizer.
 *
 * @author David Jurgens
 */
public class CompoundWordIterator implements Iterator<String> {

    /**
     * A mapping from the first token in a compound n-gram to a list of all the
     * possible following tokens that would result in a grouping.  For example,
     * "white" might be mapped to both "house" and "castle".
     */
    private final Map<String,CompoundTokens> compoundTokens;

    /**
     * The underlying tokenizer that is used for look-ahead when finding
     * compounds
     */
    private BufferedIterator tokenizer;

    public CompoundWordIterator(String str, Set<String> compoundWords) {
	this(new BufferedReader(new StringReader(str)), compoundWords);
    }

    public CompoundWordIterator(BufferedReader br, Set<String> compoundWords) {
	tokenizer = new BufferedIterator(br);
	compoundTokens = new LinkedHashMap<String,CompoundTokens>();
	initializeMapping(compoundWords);
    }


    public CompoundWordIterator(Iterator<String> tokens, 
                                Set<String> compoundWords) {
	tokenizer = new BufferedIterator(tokens);
	compoundTokens = new LinkedHashMap<String,CompoundTokens>();
	initializeMapping(compoundWords);
    }

    /**
     * Fills the {@link #compoundTokens} mapping based on the set of compound
     * words.
     */
    private void initializeMapping(Set<String> compoundWords) {
	for (String s : compoundWords) {
	    String[] tokens  = s.split("\\s+");
	    // skip any compound tokens that are actually just single tokens
	    if (tokens.length == 1)
		continue;

	    CompoundTokens compounds = compoundTokens.get(tokens[0]);
	    if (compounds == null) {
		// use the linked version since we will be iterating over
		// this very frequently
		compounds = new CompoundTokens();
		compoundTokens.put(tokens[0], compounds);
	    }

	    // Copy the remaining tokens in the compound into a a List and then 
	    // add that to the set of valid tokens to create the compound
	    List<String> tokenList = 
		Arrays.asList(Arrays.copyOfRange(tokens, 1, tokens.length));
	    compounds.addCompound(tokenList, s);
	}
    }

    /**
     * Returns {@code true} if there is another token to return.
     */
    public boolean hasNext() {
	return tokenizer.hasNext();
    }


    /**
     * Returns the next token in the stream.  Note that unlike other iterators,
     * this method is {@code O(n)} in complexity where {@code n} is the number
     * of unique words starting the set of compound tokens recognized by this
     * iterator.
     */
    public String next() {
	if (!hasNext()) {
	    throw new NoSuchElementException();
	}

	String token = tokenizer.next();

	// Determine whether the next token to return could be the start of a
	// recognized compound token
	CompoundTokens compounds = compoundTokens.get(token);
	
	// Determine how many tokens are needed at most and see if any grouping
	// exists
	if (compounds != null) {
	    List<String> nextTokens = tokenizer.peek(compounds.maxTokens());
	    Duple<Integer,String> compound = compounds.findMatch(nextTokens);
	    if (compound != null) {
		// shift off the number of extra tokens in the compound
		for (int i = 0; i < compound.x; ++i) 
		    tokenizer.next();
		return compound.y;
	    }
	    else return token;
	}
	
	// If there was no possibility of grouping this token, then return it by
	// itself
	return token;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
	throw new UnsupportedOperationException("remove is not supported");
    }

    /**
     * Resets the underlying token stream to point to the contents of the
     * provided {@code BufferedReader}.  This does not change the set of
     * accepted compound words.
     */
    public void reset(BufferedReader br) {
	tokenizer = new BufferedIterator(br);
    }

    /**
     * Resets the underlying token stream to point to the tokens in the provided
     * iterator.  This does not change the set of accepted compound words.
     */
    public void reset(Iterator<String> tokens) {
	tokenizer = new BufferedIterator(tokens);
    }

    /**
     * A utility class for mapping the remaining part of a multi-token compound.
     * This class internall wraps all of the possibility of token groups from a
     * head token and keeps track of the original string that makes up the
     * compound.  This avoid the need to rebuild the compound should a match be
     * found.
     */
    private static class CompoundTokens {
	
	/**
	 * A mapping from a list of tokens making up a compound to the combined
	 * string form of the compound word.
	 */
	private final Map<List<String>,String> compounds;

	/**
	 * The maximum number of tokens used by any compound in this instance
	 */
	private int maxTokens;

	public CompoundTokens() {
	    maxTokens = 0;
	    compounds = new LinkedHashMap<List<String>,String>();
	}

	/**
	 * Adds the remaining tokens from a compound (less the first token) and
	 * the combined compound word from which all the tokens are derived.
	 */
	public void addCompound(List<String> tokens, String compoundToken) {
	    compounds.put(tokens, compoundToken);
	    if (tokens.size() > maxTokens)
		maxTokens = tokens.size();
	}

	/**
	 * Returns the maximum number of tokens used by this portion of the
	 * compound word.
	 */
	public int maxTokens() {
	    return maxTokens;
	}

	/**
	 * Finds a match for the provided tokens in the remaining part of the
	 * this compound word and returns the entire compound word if a match is
	 * found.
	 */
	public Duple<Integer,String> findMatch(List<String> tokens) {
	    String match = null;
	    int num = -1;
	    for (Map.Entry<List<String>,String> e : compounds.entrySet()) {
		if (e.getKey().equals(tokens)) {
		    // Perform a greedy match for the longest possible compound
		    if (match == null || match.length() < e.getValue().length()) {
			match = e.getValue();
			num = e.getKey().size();
		    }
		}
	    }
	    return (match == null) 
		? null : new Duple<Integer,String>(num, match);
	}
    }
    
}
