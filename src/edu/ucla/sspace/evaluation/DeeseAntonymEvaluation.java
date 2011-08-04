/*
 * Copyright 2010 
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

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.Vector;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 *
 * @see WordAssociationTest
 */
public class DeeseAntonymEvaluation extends AbstractWordAssociationTest {

    /**
     * The set of Deese antonyms
     */
    private static final Set<Pair<String>> DEESE_ANTONYMS =
        new LinkedHashSet<Pair<String>>();
    
    static {
        DEESE_ANTONYMS.add(new Pair<String>("active", "passive"));
        DEESE_ANTONYMS.add(new Pair<String>("bad", "good"));
        DEESE_ANTONYMS.add(new Pair<String>("high", "low"));
        DEESE_ANTONYMS.add(new Pair<String>("right", "wrong"));
        DEESE_ANTONYMS.add(new Pair<String>("big", "little"));
        DEESE_ANTONYMS.add(new Pair<String>("empty", "full"));
        DEESE_ANTONYMS.add(new Pair<String>("narrow", "wide"));
        DEESE_ANTONYMS.add(new Pair<String>("strong", "weak"));
        DEESE_ANTONYMS.add(new Pair<String>("cold", "hot"));
        DEESE_ANTONYMS.add(new Pair<String>("heavy", "light"));
        DEESE_ANTONYMS.add(new Pair<String>("pretty", "ugly"));
        DEESE_ANTONYMS.add(new Pair<String>("alone", "together"));
        DEESE_ANTONYMS.add(new Pair<String>("few", "many"));
        DEESE_ANTONYMS.add(new Pair<String>("dark", "light"));
        DEESE_ANTONYMS.add(new Pair<String>("bottom", "top"));
        DEESE_ANTONYMS.add(new Pair<String>("long", "short"));
        DEESE_ANTONYMS.add(new Pair<String>("sour", "sweet"));
        DEESE_ANTONYMS.add(new Pair<String>("clean", "dirty"));
        DEESE_ANTONYMS.add(new Pair<String>("hard", "soft"));
        DEESE_ANTONYMS.add(new Pair<String>("rich", "poor"));
        DEESE_ANTONYMS.add(new Pair<String>("back", "front"));
        DEESE_ANTONYMS.add(new Pair<String>("dry", "wet"));
        DEESE_ANTONYMS.add(new Pair<String>("left", "right"));
        DEESE_ANTONYMS.add(new Pair<String>("short", "tall"));
        DEESE_ANTONYMS.add(new Pair<String>("far", "near"));
        DEESE_ANTONYMS.add(new Pair<String>("easy", "hard"));
        DEESE_ANTONYMS.add(new Pair<String>("happy", "sad"));
        DEESE_ANTONYMS.add(new Pair<String>("old", "young"));
        DEESE_ANTONYMS.add(new Pair<String>("alive", "dead"));
        DEESE_ANTONYMS.add(new Pair<String>("deep", "shallow"));
        DEESE_ANTONYMS.add(new Pair<String>("large", "small"));
        DEESE_ANTONYMS.add(new Pair<String>("rough", "smooth"));
        DEESE_ANTONYMS.add(new Pair<String>("black", "white"));
        DEESE_ANTONYMS.add(new Pair<String>("fast", "slow"));
        DEESE_ANTONYMS.add(new Pair<String>("new", "old"));
        DEESE_ANTONYMS.add(new Pair<String>("thin", "thick"));
        DEESE_ANTONYMS.add(new Pair<String>("first", "last"));
        DEESE_ANTONYMS.add(new Pair<String>("single", "married"));
        DEESE_ANTONYMS.add(new Pair<String>("inside", "outside"));
    }

    public DeeseAntonymEvaluation() {
        super(createMap());
    }
    
    private static Map<Pair<String>,Double> createMap() {
        Map<Pair<String>,Double> antonymToSimilarity = 
            new HashMap<Pair<String>,Double>();
        for (Pair<String> p : DEESE_ANTONYMS)
            antonymToSimilarity.put(p, 1d);
        return antonymToSimilarity;
    }

    /**
     * {@inheritDoc}
     */
    protected double getLowestScore() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    protected double getHighestScore() {
        return 1;
    }

    /**
     * Returns the association of the two words on a scale of 0 to 1.
     *
     * @return the assocation or {@code null} if either {@code word1} or {@code
     *         word2} are not in the semantic space
     */
    protected Double computeAssociation(SemanticSpace sspace, 
                                        String word1, String word2) {
        Vector v1 = sspace.getVector(word1);
        Vector v2 = sspace.getVector(word2);
        if (v1 == null || v2 == null)
            return null;
        
        // Find the ranks of each of the two words to each other
        double rank1 = findRank(sspace, word1, word2);
        double rank2 = findRank(sspace, word2, word1);
        return 2d / (rank1 + rank2);
    }

    /**
     * Returns the average computer generated score on the Deese Antonymy test.
     */
    protected double computeScore(double[] humanScores, double[] compScores) {
        double average = 0;
        for (double score : compScores)
            average += score;
        return average / compScores.length;
    }

    /**
     * Returns the ranking <i>k</i> such that {@code other} is the k<sup>th</th>
     * most similar word to {@code target} in the semantic space.
     */
    private int findRank(SemanticSpace sspace, 
                         String target, String other) {
        Vector v1 = sspace.getVector(target);
        Vector v2 = sspace.getVector(other);
        // Compute the base similarity between the two words
        double baseSim = Similarity.cosineSimilarity(v1, v2);
        int rank = 0;
        // Next, count how many words are more similar to the target than the
        // other word is
        for (String word : sspace.getWords()) {
            Vector v = sspace.getVector(word);
            double sim = Similarity.cosineSimilarity(v1, v);
            if (sim > baseSim)
                rank++;
        }
        return rank;
    }
}
