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
import edu.ucla.sspace.util.ResourceFinder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A utility for asserting what tokens are valid and invalid within a stream of
 * tokens.  A filter may be either inclusive or exclusive.<p>
 *
 * An inclusive filter will accept only those tokens with which it was
 * initialized.  For an example, an inclusive filter initialized with all of the
 * words in the english dictionary would exclude all misspellings or foreign
 * words in a token stream.<p>
 *
 * An exclusive filter will aceept only those tokens that are not in set with
 * which it was initialized.  An exclusive filter is often used with a list of
 * common words that should be excluded, which is also known as a "stop
 * list."<p>
 *
 * {@code TokenFilter} instances may be combined into a linear chain of filters.
 * This allows for a highly configurable filter to be made from mulitple rules.
 * Chained filters are created in a linear order and each filter must accept the
 * token for the last filter to return {@code}.  If the any of the earlier
 * filters return {@code false}, then the token is not accepted.<p>
 *
 * This class also provides a static utility function {@link
 * #loadFromSpecification(String) loadFromSpecification} for initializing a
 * chain of filters from a text configuration.  This is intended to facility
 * command-line tools that want to provide easily configurable filters.  An
 * example configuration might look like:
 * <tt>include=top-tokens.txt:test-words.txt,exclude=stop-words.txt</tt>
 *
 * @see FilteredIterator
 */
public class TokenFilter {

    /**
     * The set of tokens used to filter the output
     */
    private final Set<String> tokens;

    /**
     * {@code true} if the returned tokens must not be in the filter set
     */
    private final boolean excludeTokens;

    /**
     * A filter that is to be applied before this filter when determining
     * whether a token should  be accepted
     */
    private TokenFilter parent;

    /**
     * Constructs a filter that accepts only those tokens present in {@code tokens}.
     */
    public TokenFilter(Set<String> tokens) {
	this(tokens, false, null);
    }

    /**
     * Constructs a filter using {@code tokens} that if {@code excludeTokens} is
     * {@code false} will accept those in {@code tokens}, or if {@code
     * excludeTokens} is {@code true}, will accept those that are <i>not</i> in
     * {@code tokens}.
     *
     * @param tokens the set of tokens to use in filtering the output
     * @param excludeTokens {@code true} if tokens in {@code tokens} should be
     *        excluded, {@code false} if only tokens in {@code tokens} should
     *        be included
     */
    public TokenFilter(Set<String> tokens, boolean excludeTokens) {
	this(tokens, excludeTokens, null);
    }

    /**
     * Constructs a chained filter that accepts the subset of what the parent
     * accepts after applying its own filter to any tokens that the parent
     * accepts.  Note that if the parent does not accept a token, then this
     * filter will not either.
     *
     * @param tokens the set of tokens to use in filtering the output
     * @param excludeTokens {@code true} if tokens in {@code tokens} should be
     *        excluded, {@code false} if only tokens in {@code tokens} should
     *        be included
     * @param parent a filter to be applied before determining whether a token
     *        is to be accepted
     */
    public TokenFilter(Set<String> tokens, boolean excludeTokens, 
		       TokenFilter parent) {
	this.tokens = tokens;
	this.excludeTokens = excludeTokens;
	this.parent = parent;
    }

    /**
     * Returns {@code true} if the token is valid according to the configuration
     * of this filter.
     *
     * @param token a token to be considered
     *
     * @return {@code true} if this token is valid
     */
    public boolean accept(String token) {
	return (parent == null || parent.accept(token)) &&
		tokens.contains(token) ^ excludeTokens;
    }

    /**
     * Creates a chained filter by accepting the subset of whatever {@code
     * parent} accepts less what tokens this filter rejects.  
     *
     * @param parent a filter to be applied before determining whether a token
     *        is to be accepted
     *
     * @return the previous parent filter or {@code null} if one had not been
     *         assigned
     */
    public TokenFilter combine(TokenFilter parent) {
	TokenFilter oldParent = parent;
	this.parent = parent;
	return oldParent;
    }
    
    /**
     * Loads a series of chained {@code TokenFilter} instances from the
     * specified configuration string.  This method will assume that all
     * specified resources exist on the local file system.<p>
     * 
     * A configuration lists sets of files that contain tokens to be included or
     * excluded.  The behavior, {@code include} or {@code exclude} is specified
     * first, followed by one or more file names, each separated by colons.
     * Multiple behaviors may be specified one after the other using a {@code ,}
     * character to separate them.  For example, a typicaly configuration may
     * look like: "include=top-tokens.txt,test-words.txt:exclude=stop-words.txt"
     * <b>Note</b> behaviors are applied in the order they are presented on the
     * command-line.
     *
     * @param configuration a token filter configuration
     *
     * @return the chained TokenFilter instance made of all the specification,
     *         or {@code null} if the configuration did not specify any filters
     *
     * @throws IOError if any error occurs when reading the word list files
     */
    public static TokenFilter loadFromSpecification(String configuration) {
        return loadFromSpecification(configuration, new FileResourceFinder());
    }

    /**
     * Loads a series of chained {@code TokenFilter} instances from the
     * specified configuration string using the provided {@link ResourceFinder}
     * to locate the resources.  This method is provided for applications that
     * need to load resources from a custom environment or file system.<p>
     * 
     * A configuration lists sets of files that contain tokens to be included or
     * excluded.  The behavior, {@code include} or {@code exclude} is specified
     * first, followed by one or more file names, each separated by colons.
     * Multiple behaviors may be specified one after the other using a {@code ,}
     * character to separate them.  For example, a typicaly configuration may
     * look like: "include=top-tokens.txt,test-words.txt:exclude=stop-words.txt"
     * <b>Note</b> behaviors are applied in the order they are presented on the
     * command-line.
     *
     * @param configuration a token filter configuration
     * @param finder the {@code ResourceFinder} used to locate the file
     *        resources specified in the configuration string.
     *
     * @return the chained TokenFilter instance made of all the specification,
     *         or {@code null} if the configuration did not specify any filters
     *
     * @throws IOError if any error occurs when reading the word list files
     */    
    public static TokenFilter loadFromSpecification(String configuration,
                                                    ResourceFinder finder) {

	TokenFilter toReturn = null;

	// multiple filter are separated by a ':'
	String[] filters = configuration.split(",");

	for (String s : filters) {
            String[] optionAndFiles = s.split("=");
            if (optionAndFiles.length != 2)
                throw new IllegalArgumentException(
                    "Invalid number of filter parameters: " + s);
            
            String behavior = optionAndFiles[0];
            boolean exclude = behavior.equals("exclude");
            // Sanity check that the behavior was include
            if (!exclude && !behavior.equals("include"))
                throw new IllegalArgumentException(
                    "Invalid filter behavior: " + behavior);
                
            String[] files = optionAndFiles[1].split(":");
            
	    // Load the words in the file(s)
	    Set<String> words = new HashSet<String>();
	    try {
                for (String f : files) {
                    BufferedReader br = finder.open(f);
                    for (String line = null; (line = br.readLine()) != null; ) 
                        words.add(line);
                    br.close();
                }
	    } catch (IOException ioe) {
		// rethrow since filter error is fatal to correct execution
		throw new IOError(ioe);
	    }
	    
            // Chain the filters on top of each other
	    toReturn = new TokenFilter(words, exclude, toReturn);
	}

	return toReturn;
    }
}

