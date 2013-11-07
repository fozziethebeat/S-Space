/*
 * Copyright 2012 David Jurgens
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

package edu.ucla.sspace.util;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.ObjectCounter;

import edu.ucla.sspace.util.primitive.IntPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * A (partial) implementation of computing <a
 * href="http://en.wikipedia.org/wiki/Krippendorff%27s_alpha">Krippendorff's
 * alpha</a>, which measures the level of annotator agreement.  Alpha can be
 * computed across arbitrary numbers of annotators (with missing values) as well
 * as with different <a
 * href="http://en.wikipedia.org/wiki/Levels_of_measurement">levels of
 * measurement</a>
 *
 * @since 2.0.3
 */ 
public class KrippendorffsAlpha {

    /**
     * The <a href="http://en.wikipedia.org/wiki/Levels_of_measurement">level of
     * measurement</a> at which the data is annotated.
     */
    public enum LevelOfMeasurement {
            NOMINAL,
            ORDINAL,
            INTERVAL,
            // RATIO,
            // POLAR,
            // CIRCULAR
    }
    
    /**
     * Computes <a
     * href="http://en.wikipedia.org/wiki/Krippendorff%27s_alpha">Krippendorff's
     * alpha</a> for the provuded ratings, where the each coder (annotator) is
     * listed as a separate row in {@code ratings} and each item and its ratings
     * are indicated by a column in {@code ratings}
     *
     * @param ratings a matrix where coders are represented by rows and items by
     *        columns.  The interpretation of the values of {@code ratings}
     *        depends on the level of measurement.  Missing values are encoded
     *        as {@code NaN} values.
     * @param level the <a
     *        href="http://en.wikipedia.org/wiki/Levels_of_measurement">level of
     *        measurement</a> at which the data is annotated.
     *
     * @return the level of agreement for the given annotators
     *
     * @throws {@link IllegalArgumentException} if the {@code ratings} matrix
     *         has zero items with paired ratings
     */
    public double compute(Matrix ratings, LevelOfMeasurement level) {
        
        SortedSet<Rating> values = new TreeSet<Rating>();
        for (int r = 0; r < ratings.rows(); ++r) {
            for (int c = 0; c < ratings.columns(); ++c) {
                double d = ratings.get(r, c);
                if (!Double.isNaN(d))
                    values.add(new Rating(d));
            }
        }

        Indexer<Rating> valueRanks = new ObjectIndexer<Rating>(values);


        double[][] coincidence = new double[values.size()][values.size()];
        int numPairableValues = 0;

        for (int item = 0; item < ratings.columns(); ++item) {

            // This is the number of value pairs for this item 
            int m_u = 0;
            for (int c = 0; c < ratings.rows(); ++c) {
                if (!Double.isNaN(ratings.get(c, item)))
                    m_u++;
            }

            if (m_u > 1)
                numPairableValues += m_u;
            else
                continue;

            for (int coder1 = 0; coder1 < ratings.rows(); ++coder1) {
                double d1 = ratings.get(coder1, item);
                if (Double.isNaN(d1))
                    continue;
                Rating r1 = new Rating(d1);

                for (int coder2 = 0; coder2 < ratings.rows(); ++coder2) {
                    if (coder1 == coder2)
                        continue;
                    double d2 = ratings.get(coder2, item);
                    if (Double.isNaN(d2))
                        continue;
                    Rating r2 = new Rating(d2);
                    coincidence[valueRanks.index(r1)][valueRanks.index(r2)] += 1d / (m_u-1);
                }
            }            
        }

        double[] ratingToSum = new double[valueRanks.size()];
        for (int i = 0; i < coincidence.length; ++i) {
            for (int j = 0; j < coincidence[i].length; ++j) {
                ratingToSum[i] += coincidence[i][j];
            }
        }
               
        double expectedDisagreementSum = 0;
        for (int i = 0; i < coincidence.length; ++i) {
            // We can skip diagonals since they do not disagree (i.e., the
            // values are identical)
            for (int j = i+1; j < coincidence[i].length; ++j) {
                expectedDisagreementSum += 
                    ratingToSum[i] * ratingToSum[j] * diff(i, j, ratingToSum, valueRanks, level);
            }
        }
        
        
        double disagreementSum = 0;
        for (int i = 0; i < coincidence.length; ++i) {
            // We can skip diagonals since they do not disagree (i.e., the
            // values are identical)
            for (int j = i+1; j < coincidence[i].length; ++j) {
                double co = coincidence[i][j];
                if (co > 0) {
                    disagreementSum += co * diff(i, j, ratingToSum, valueRanks, level);
                }
            }
        }

        double expectedDisagreement = expectedDisagreementSum / (numPairableValues - 1);

        return 1 - disagreementSum / expectedDisagreement;
    }

    static double diff(int c, int k, double[] ratingToSum, Indexer<Rating> ranks, 
                       LevelOfMeasurement lom) {
        switch (lom) {
        case ORDINAL: {
            double sum = 0;
            for (int g = c; g <= k; ++g) {
                sum += ratingToSum[g];
            }
            sum -= ((ratingToSum[c] + ratingToSum[k]) / 2d);
            return sum * sum;
        }
        case NOMINAL: {
            double rating1 = ranks.lookup(c).val;
            double rating2 = ranks.lookup(k).val;
            return rating1 == rating2 ? 0 : 1;
        }
        case INTERVAL: {
            double rating1 = ranks.lookup(c).val;
            double rating2 = ranks.lookup(k).val;
            double d = rating1 - rating2;
            return d * d;
        }
        default:
            throw new IllegalArgumentException(
                "Unsupported level of measurement: " + lom);
        }
    }

    static class Rating implements Comparable<Rating> {

        final double val;
        public Rating(double val) {
            this.val = val;
        }

        public int compareTo(Rating r) {
            return Double.compare(val, r.val);
        }

        public boolean equals(Object o) {
            return o instanceof Rating && ((Rating)o).val == val;
        }

        public int hashCode() {
            return (int)val;
        }

        public String toString() {
            return String.valueOf(val);
        }
    }
}
