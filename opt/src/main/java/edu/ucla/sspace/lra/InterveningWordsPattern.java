/*
 * Copyright 2009 Sky Lin
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

package edu.ucla.sspace.lra;

public class InterveningWordsPattern implements Comparable<InterveningWordsPattern> {
    private int occurrences = 0;
    private String pattern = "";
    private boolean reverse_mapping = false;

    public InterveningWordsPattern(String comb_pattern) {
        pattern = comb_pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public boolean getReverse() {
        return reverse_mapping;
    }

    public void setOccurrences(int frequency) {
        occurrences = frequency;
    }
    
    public void setReverse(boolean rev) {
        reverse_mapping = true;
    }

    public int compareTo(InterveningWordsPattern p) {
        int score = occurrences - p.occurrences;
        if (score == 0 && !pattern.equals(p.getPattern())) {
            score = -1;
        }
        return score;
    }

    /*
    @Override public boolean equals (Object p) {
        if (this == p || pattern.equals(((InterveningWordsPattern)p).getPattern()))
            return true;
        else
            return false;
    }
    */

}

