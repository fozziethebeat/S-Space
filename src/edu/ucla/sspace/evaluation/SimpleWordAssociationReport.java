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

package edu.ucla.sspace.evaluation;


/**
 * An implementation of {@link WordAssociationTest}.
 *
 * @author David Jurgens
 */
public class SimpleWordAssociationReport implements WordAssociationReport {
        
    /**
     * The total number of word pairs
     */
    private final int numWordPairs;
    
    /**
     * The correlation between the {@link SemanticSpace} judgements and the
     * human association judgements
     */
    private final double correlation;

    /**
     * The number of unaswnserable pairs.
     */
    private final int unanswerable;
    
    /**
     * Creates a simple report
     */
    public SimpleWordAssociationReport(int numWordPairs,
                                       double correlation, 
                                       int unanswerable) {
        this.numWordPairs = numWordPairs;
        this.correlation = correlation;
        this.unanswerable = unanswerable;
    }
    
    /**
     * {@inheritDoc}
     */
    public int numberOfWordPairs() {
        return numWordPairs;
    }
    
    /**
     * {@inheritDoc}
     */
    public double correlation() {
        return correlation;
    }
    
    /**
     * {@inheritDoc}
     */
    public int unanswerableQuestions() {
        return unanswerable;
    }
    
    /**
     * Returns a string describing the three values represented by this
     * {@link report}
     */
    public String toString() {
        return String.format("%.4f correlation; %d/%d unanswered",
                             correlation, unanswerable, numWordPairs);
    }
}