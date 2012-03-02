/*
 * Copyright 2011 David Jurgens 
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

import edu.ucla.sspace.common.statistics.SignificanceTest;

import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.ObjectCounter;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.PairCounter;

import java.io.Serializable;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 *
 */
public class TermAssociationFinder 
        implements Iterable<Map.Entry<Pair<String>,Double>>, Serializable {

    private static final long serialVersionUID = 1L;

    private final Counter<String> termCounts;

    private final Counter<Pair<String>> bigramCounts;

    private int contexts;

    private SignificanceTest test;

    private final Set<String> leftTerms;

    /**
     * The set of terms that may appear with of the focus term
     */
    private final Set<String> rightTerms;

    /**
     * Creates a new {@code TermAssociationFinder} using the specified test,
     * which will only record associations between all terms that appear
     * together in the same context.
     */
    public TermAssociationFinder(SignificanceTest test) {
        this(test, null, null);
    }

    /**
     * Creates a new {@code TermAssociationFinder} using the specified test,
     * which will only record associations between two terms t1 and t2 if {@code
     * leftTerms.contains(t1)} and {@code rightTerms.contains(t2)} or the
     * commutative equivalent.
     */
    public TermAssociationFinder(SignificanceTest test, 
                                Set<String> leftTerms, Set<String> rightTerms) {
        this.test = test;
        this.leftTerms = leftTerms;
        this.rightTerms = rightTerms;
        termCounts = new ObjectCounter<String>();
        bigramCounts = new ObjectCounter<Pair<String>>();
        contexts = 0;
    }

    public void addContext(Set<String> tokens) {
        contexts++;
        for (String t1 : tokens) {
            termCounts.count(t1);
            for (String t2 : tokens) {
                if (t1.equals(t2))
                    break;
                // We need to ensure a canonical ordering between the two terms,
                // so use the lexicographical comparison.  Otherwise we can end
                // up recording counts for (a,b) and (b,a) separately depending
                // on the iteration ordering of tokens.
                String s1, s2;
                if (t1.compareTo(t2) > 0) {
                    s1 = t1;
                    s2 = t2;
                }
                else {
                    s1 = t2;
                    s2 = t1;
                }
                // If the two terms span the filters we have (or we have no
                // filters and are therefore recording all associations), update
                // the counts for these pairs
                if (leftTerms == null
                       || (rightTerms.contains(s1) && leftTerms.contains(s2))
                       || (rightTerms.contains(s2) && leftTerms.contains(s1))) {
                    bigramCounts.count(new Pair<String>(s1, s2));
                }
            }
        }
    }

    public double getAssociationScore(String t1, String t2) {
        int t1Count = termCounts.getCount(t1);
        int t2Count = termCounts.getCount(t2);
        if (t1Count == 0 || t2Count == 0)
            return 0;
        int bothAppeared = bigramCounts.getCount(new Pair<String>(t1, t2));
        int t1butNotT2 = t1Count - bothAppeared;
        int t2butNotT1 = t2Count - bothAppeared;
        int neitherAppeared = contexts - ((t1Count + t2Count) - bothAppeared);
        return test.score(bothAppeared, t1butNotT2, 
                          t2butNotT1, neitherAppeared);
    }

    /**
     * Returns an iterator over all the bigram pairs with their corresponding
     * association scores
     */
    public Iterator<Map.Entry<Pair<String>,Double>> iterator() {
        return new ScoreIterator();
    }

    public SignificanceTest getTest() {
        return test;
    }

    public void setTest(SignificanceTest test) {
        if (test == null)
            throw new NullPointerException("test cannot be null");
        this.test = test;
    }

    private class ScoreIterator 
            implements Iterator<Map.Entry<Pair<String>,Double>> {

        private final Iterator<Map.Entry<Pair<String>,Integer>> bigramIter;

        public ScoreIterator() {
            bigramIter = bigramCounts.iterator();
        }

        public boolean hasNext() {
            return bigramIter.hasNext();
        }

        public Map.Entry<Pair<String>,Double> next() {
            Map.Entry<Pair<String>,Integer> e = bigramIter.next();
            Pair<String> p = e.getKey();
            String t1 = p.x;
            String t2 = p.y;

            int t1Count = termCounts.getCount(t1);
            int t2Count = termCounts.getCount(t2);
            int bothAppeared = e.getValue();
            
            int t1butNotT2 = t1Count - bothAppeared;
            int t2butNotT1 = t2Count - bothAppeared;
            int neitherAppeared = 
                contexts - ((t1Count + t2Count) - bothAppeared);
            double score = test.score(bothAppeared, t1butNotT2, 
                                      t2butNotT1, neitherAppeared);
            return new AbstractMap.SimpleEntry<Pair<String>,Double>(p, score);
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }    
}