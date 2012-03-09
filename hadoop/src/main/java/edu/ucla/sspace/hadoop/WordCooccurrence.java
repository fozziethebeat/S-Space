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
 * An abstraction of a word co-occurrence as extracted by the Hadoop processing
 * classes.  This class reflects the aggregate result where multiple
 * co-occurrence of two words with the same distance are reprsented by a single
 * {@code WordCooccurrence}
 *
 * @see WordCooccurrenceCountingJob
 */
public interface WordCooccurrence {
    
    /**
     * Returns the word at the center of the co-occurrence window.  All other
     * words in the window are counted at co-occurring with this word.
     */
    public String focusWord();

    /**
     * Returns the word that occurs within the co-occurrence window a specific
     * distance away from the focus word.
     */
    public String relativeWord();

    /**
     * The distance between the relative word and the focus word.  If this value
     * is negative, the relative word occurred before the focus word
     */ 
    public int getDistance();

    /**
     * Returns the number of times the relative word co-occurred with the focus
     * word at the distance reflected in this instance.
     */
    public int getCount();

}