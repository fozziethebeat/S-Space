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


/**
 * A straigh-forward implementation of {@link WordCooccurrence}.
 */
public class SimpleWordCooccurrence implements WordCooccurrence {
        
    private final String focusWord;
        
    private final String relativeWord;

    private final int distance;

    private final int count;

    public SimpleWordCooccurrence(String focusWord, String relativeWord, 
                                  int distance, int count) {
        this.focusWord = focusWord;
        this.relativeWord = relativeWord;
        this.distance = distance;
        this.count = count;
    }

    /**
     * {@inheritDoc}
     */
    public String focusWord() {
        return focusWord;
    }

    /**
     * {@inheritDoc}
     */
    public String relativeWord() {
        return relativeWord;
    }
        
    /**
     * {@inheritDoc}
     */
    public int getDistance() {
        return distance;
    }

    /**
     * {@inheritDoc}
     */
    public int getCount() {
        return count;
    }
}