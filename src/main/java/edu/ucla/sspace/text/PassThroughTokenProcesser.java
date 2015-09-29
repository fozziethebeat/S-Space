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

import java.util.ArrayList;
import java.util.List;


/**
 * An implementation of {@link TokenProcessor} that does nothing to the text.
 */
public class PassThroughTokenProcesser implements TokenProcesser {
    
    /**
     * Returns the base-form of the token, without any processing
     */
    public String process(Token t) {
        return t.text();
    }
    
    /**
     * Returns the base forms of the tokens in the multi-word expression, joined
     * by underscore characters.
     */
    public String process(List<Token> multiWordExpression) {
        List<String> tokens = new ArrayList<String>();
        for (Token t : multiWordExpression)
            tokens.add(t.text());
        return String.join("_", tokens);
    }
   
}
