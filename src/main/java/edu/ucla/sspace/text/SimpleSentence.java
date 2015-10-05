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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;


/**
 * 
 */
public class SimpleSentence implements Sentence {

    private final CoreMap annotations;

    private final Iterable<String> tokens;   

    public SimpleSentence(String sentence) {
        this(Arrays.asList(sentence.split("\\s+")));
    }

    public SimpleSentence(Iterable<String> tokens) {
        this.tokens = tokens;
        this.annotations = new ArrayCoreMap();
    }
   
    /**
     * {@inheritDoc}
     */
    public CoreMap annotations() {
        return annotations;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Token> iterator() {
        return new TokenIter(tokens.iterator());
    }

    /**
     * {@inheritDoc}
     */
    public String text() {
        return String.join(" ", tokens);
    }

    static class TokenIter implements Iterator<Token> {

        private final Iterator<String> iter;

        public TokenIter(Iterator<String> iter) {
            this.iter = iter;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Token next() {
            return new SimpleToken(iter.next());
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
}
