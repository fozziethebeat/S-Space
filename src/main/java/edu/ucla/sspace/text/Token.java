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

import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;



/**
 * The representation of a token in text, which may be annotated with one or
 * more pieces of information.
 */
public interface Token {

    public static final Token EMPTY_TOKEN = new EmptyToken();

    public static final String EMPTY_TOKEN_TEXT = ""; 
    
    /**
     * The annotations provided for this token
     */
    CoreMap annotations();
    
    /**
     * A convience method that returns the text of this token
     */
    String text();

    /**
     * Returns the text of the token, identical to what {@link #text()} returns.
     */
    String toString();

    static class EmptyToken implements Token {

        static final CoreMap EMPTY_MAP = new ArrayCoreMap();
        
        @Override public CoreMap annotations() { return EMPTY_MAP; }

        @Override public String text() { return EMPTY_TOKEN_TEXT; }

        @Override public String toString() { return text(); }
    }
}
