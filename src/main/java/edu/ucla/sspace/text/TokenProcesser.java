/*
 * Copyright 2015 David Jurgens
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

import edu.stanford.nlp.util.CoreMap;

import java.util.List;


/**
 * An interface for procedures that convert a token or multi-word expression
 * into the canonical {@code String} representation use by underlying algorithms
 * for determining token equivalence.  Common operations include adding the Part
 * of Speech information, lemmatizing or stemming, and lower casing.
 */
public interface TokenProcesser {
    
    /**
     * Converts a token into the string representation used for measuring token
     * equality.  Common operations include adding the Part of Speech
     * information, lemmatizing or stemming, and lower casing.
     */
    String process(Token t);

    /**
     * Converts multiword expression into the string representation used for
     * measuring token equality.  Common operations include adding the Part of
     * Speech information, lemmatizing or stemming, and lower casing.
     */
    String process(List<Token> multiWordExpression);
    
}
