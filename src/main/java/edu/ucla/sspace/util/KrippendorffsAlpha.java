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
 * as with ifferent <a
 * href="http://en.wikipedia.org/wiki/Levels_of_measurement">levels of
 * measurement</a>
 *
 * <p><b>NOTE: this class is under heavy construction</b>
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
            RATIO,
            POLAR,
            CIRCULAR
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
        
        switch (level) {
         case NOMINAL:
             return compute(ratings, new NominalDifference());
             // 
             
        // NOTE: Supported later when we figure out why the unit test isn't
        // passing...

        // case ORDINAL:
        //     return compute(ratings, new OrdinalDifference());

        case INTERVAL:
            return compute(ratings, new IntervalDifference());
        }
        throw new IllegalArgumentException("unssuported LevelOfMeasurement: " + level);
    }

    /**
     * Computes alpha using the provided {@link DifferenceFunction} to compute
     * the distance between divergent annotations.
     */
    private double compute(Matrix ratings, DifferenceFunction diffFunc) {

        //  Keep track of all the unique values used
        NavigableSet<Double> ratingValues = new TreeSet<Double>();
        List<Integer> itemsToInclude = new ArrayList<Integer>();

        int numRatings = 0;

        Counter<Duple<Integer,Double>> itemAndRatingCounts =
            new ObjectCounter<Duple<Integer,Double>>();

        Counter<Double> ratingCounts = new ObjectCounter<Double>();
        Map<Integer,Integer> itemCounts = new HashMap<Integer,Integer>();
        
        for (int i = 0; i < ratings.columns(); ++i) {
            List<Double> itemRatings = new ArrayList<Double>();
            for (int c = 0; c < ratings.rows(); ++c) {
                double rating = ratings.get(c, i);
                if (!Double.isNaN(rating)) 
                    itemRatings.add(rating);                
            }
            // We only include items that have two or more ratings
            if (itemRatings.size() > 1) {
                ratingValues.addAll(itemRatings);
                itemsToInclude.add(i);
                numRatings += itemRatings.size();
                itemCounts.put(i, itemRatings.size());

                for (int c = 0; c < ratings.rows(); ++c) {
                    double rating = ratings.get(c, i);
                    if (!Double.isNaN(rating)) {
                        ratingCounts.count(rating);
                        itemAndRatingCounts.count(
                            new Duple<Integer,Double>(i, rating));
                    }
                }
            }
        }

        if (numRatings == 0) {
            throw new IllegalArgumentException("Input agreement matrix has " +
                "zero items with paired ratings");
        }

        diffFunc.setValues(ratingValues);
        
        double avgNumRatingsPerItem = 
            ((double)numRatings) / itemsToInclude.size();

        //System.out.println("rBar = " + avgNumRatingsPerItem);

        // Calculate the probability of each rating
        Map<Double,Double> ratingToProb = 
            new HashMap<Double,Double>();
        for (Map.Entry<Double,Integer> e : ratingCounts) {
            Double rating = e.getKey();
            int count = e.getValue();
            double prob = count / (avgNumRatingsPerItem * itemsToInclude.size());
            //System.out.printf("Prob of %f: %f%n", rating, prob);
            ratingToProb.put(rating, prob);
        }



        double numerator = 0;

        for (Integer u :  itemsToInclude) {
            int n_u = itemCounts.get(u); 
            double itemAgreement = 0;
            for (Double c : ratingValues) {
                for (Double k : ratingValues.tailSet(c)) {
                    if (c.equals(k))
                        continue;
                    int n_uc = itemAndRatingCounts.getCount(new Duple<Integer,Double>(u, c));
                    int n_uk = itemAndRatingCounts.getCount(new Duple<Integer,Double>(u, k));
//                     System.out.printf("(c=%f, k=%f) %d * %d * %f%n",c, k,
//                                       n_uc, n_uk, diffFunc.getDifference(c, k));
                    itemAgreement += n_uc * n_uk * diffFunc.getDifference(c, k);
                }
            }
            // System.out.printf("Agreement for item %d: %f / (%d - 1)%n", u, itemAgreement, n_u);
            numerator += itemAgreement / (n_u - 1);
        }

        double denominator = 0;
        for (Double c : ratingValues) {
            for (Double k : ratingValues.tailSet(c)) {
                if (c.equals(k))
                    continue;
                int n_c = 0;
                int n_k = 0;

                for (Integer u :  itemsToInclude) {
                    n_c += itemAndRatingCounts.getCount(new Duple<Integer,Double>(u, c));
                    n_k += itemAndRatingCounts.getCount(new Duple<Integer,Double>(u, k));
                }
                denominator += n_c * n_k * diffFunc.getDifference(c, k);
            }
        }
        

        double alpha = (denominator == 0)
            ? 1 
            : 1 - (numRatings - 1) * (numerator / denominator);
        if (Double.isNaN(alpha)) {
            System.out.printf("1 - (%d - 1) * (%f / %f)%n", 
                              numRatings, numerator, denominator);
            System.out.println("ratingValues: " + ratingValues);
            System.out.println("itemAndRatingCounts: " + itemAndRatingCounts);
            throw new IllegalStateException("Alpha is NaN");
        }
        
        return alpha;
        /*
        // Compute the level of agreement 
        for (Integer item : itemsToInclude) {
            double agreementSumForItem = 0;
            int totalRatingsForItem = itemCounts.get(item);
            for (Double rating : ratingValues) {
                int count = itemAndRatingCounts.getCount(
                    new Duple<Integer,Double>(item, rating));

                // For the given rating on this item, sum the total agrement
                // when computed using the difference function that measures
                // partial agreement between this rating and all other ratings.
                double weightedAgreementSum = 0;
                for (Double rating2 : ratingValues) {
                    int r2count = itemAndRatingCounts.getCount(
                        new Duple<Integer,Double>(item, rating2));
                    weightedAgreementSum += diffFunc
                        .getDifference(rating, rating2) * r2count;
                }

                agreementSumForItem += (count * (weightedAgreementSum - 1)) 
                    / (avgNumRatingsPerItem * (totalRatingsForItem - 1));
            }
            agreement += agreementSumForItem;
        }

        // Normalize by the number of items
        agreement /= itemsToInclude.size();
        // Calculate the expected level of agreement given the current
        // distribution of ratings across the items
        double expectedAgreement = 0;
        for (Double rating1 : ratingValues) {
            double prob1 = ratingToProb.get(rating1);
            for (Double rating2 : ratingValues) {
                if (rating1.equals(rating2))
                    break;
                double prob2 = ratingToProb.get(rating2);
                expectedAgreement += diffFunc
                    .getDifference(rating1, rating2) * prob1 * prob2;
            }
        }
        System.out.printf("(%f - %f) / (1 - %f)%n",
                          agreement, expectedAgreement, expectedAgreement);
        
        double alpha = (agreement - expectedAgreement) / (1 - expectedAgreement);
        
        return alpha;
        */
    }




    private static interface DifferenceFunction {

        void setValues(SortedSet<Double> ratingValues);

        double getDifference(Double rating1, Double rating2);

    }

    private static class NominalDifference implements DifferenceFunction {

        public void setValues(SortedSet<Double> ratingValues) { }

        public double getDifference(Double rating1, Double rating2) {
            return (rating1.equals(rating2)) ? 0 : 1;
        }

    }

    private static class OrdinalDifference implements DifferenceFunction {
        
        private final NavigableMap<Double,Integer> ranks;

        public OrdinalDifference() {
            ranks = new TreeMap<Double,Integer>();
        }

        public void setValues(SortedSet<Double> ratingValues) { 
            for (Double d : ratingValues) 
                ranks.put(d, ranks.size());
        }

        public double getDifference(Double rating1, Double rating2) {
            int rank1 = ranks.get(rating1);
            int rank2 = ranks.get(rating2);
            double avgRank = (rank1 + rank2) / 2d;
            Double start = (rank1 < rank2) ? rating1 : rating2;
            Double end = (rank1 < rank2) ? rating2 : rating1;
            
            int sum = 0;
            Double cur = start;
            while (true) {
                int r = ranks.get(cur);
                sum += r - avgRank;
                if (cur.equals(end))
                    break;
                cur = ranks.higherKey(cur);
                if (cur == null)
                    throw new IllegalStateException();
            }
            return sum * sum;
        }

    }

    private static class IntervalDifference implements DifferenceFunction {

        public void setValues(SortedSet<Double> ratingValues) { }

        public double getDifference(Double rating1, Double rating2) {
            double d = rating1 - rating2;
            return d * d;
        }

    }
















    private double computeOrdinal(Matrix ratings) {
        Set<Double> values = new TreeSet<Double>();
        int numCoders = ratings.rows();
        int numItems = ratings.columns();
        int numObservedRatings = 0;
        for (int item = 0; item < numItems; ++item) {
            int itemRatings = 0;
            for (int coder = 0; coder < numCoders; ++coder) {
                double rating = ratings.get(coder, item);
                if (!Double.isNaN(rating)) {
                    values.add(rating);
                    itemRatings++;
                }
            }
            if (itemRatings > 1)
                numObservedRatings += itemRatings;
        }

        Matrix coincidenceMatrix = 
            new ArrayMatrix(values.size(), values.size());

        double[] valuesArr = new double[values.size()];
        double[] valueCounts = new double[values.size()];
        int i = 0;
        for (Double d : values)
            valuesArr[i++] = d;

        for (i = 0; i < valuesArr.length; ++i) {
            for (int j = i; j < valuesArr.length; ++j) {

                double rating1 = valuesArr[i];
                double rating2 = valuesArr[j];

                // For each rating pair, find out how many times this pair
                // occurred together across all of the annotated items
                for (int item = 0; item < numItems; ++item) {
                    
                    // How many coders answered this item
                    int numResponses = 0;

                    // How many pairs were found in this item that matched the
                    // target
                    int numPairsFound = 0;

                    for (int coder = 0; coder < numCoders; ++coder) {
                        double response = ratings.get(coder, item);
                        if (Double.isNaN(response))
                            continue;
                        numResponses++;

                        // Re-iterate through the responses again and find out
                        // how many pairs (i=rating1,j) can be constructed where
                        // j=rating2.
                        if (rating1 == response) {
                            for (int c = 0; c < numCoders; ++c) {
                                if (c == coder)
                                    continue;
                                double r2 = ratings.get(c, item);
                                if (Double.isNaN(r2))
                                    continue;
                                if (r2 == rating2) {
                                    numPairsFound++;
                                }
                            }
                        }
                    }
                                       
                    double coincidence =  (numResponses == 1)
                        ? 0 
                        : numPairsFound / (numResponses - 1d);
                    double curCoincidence = coincidenceMatrix.get(i, j);

                    coincidenceMatrix.set(i, j, curCoincidence + coincidence);
                    coincidenceMatrix.set(j, i, curCoincidence + coincidence);
                }
            }
        }

        for (i = 0; i < valuesArr.length; ++i) {
            for (int j = 0; j < valuesArr.length; ++j) {
                valueCounts[i] += coincidenceMatrix.get(i, j);
            }
        }
        
        Matrix differenceFunctionMatrix = 
            new ArrayMatrix(valuesArr.length, valuesArr.length); 
        for (i = 0; i < valuesArr.length; ++i) {
            for (int j = i+1; j < valuesArr.length; ++j) {
         
                double sum = 0;
                double ijSum = valueCounts[i] + valueCounts[j];
                
                for (int k = i; k <= j; ++k) {
                    sum += valueCounts[k];
                }
       
                sum -= (ijSum / 2d);
                sum = sum * sum;

                differenceFunctionMatrix.set(i, j, sum);
                differenceFunctionMatrix.set(j, i, sum);
            }
        }

        double numerator = 0;
        double denominator = 0;

         for (i = 0; i < valuesArr.length; ++i) {
             for (int j = i+1; j < valuesArr.length; ++j) {
                 double coincidence = coincidenceMatrix.get(i, j);
                 double difference = differenceFunctionMatrix.get(i, j);
                 numerator += coincidence * difference;
                 denominator += valueCounts[i] * valueCounts[j] * difference;
             }
         }

         return 1d - (numObservedRatings - 1) * (numerator / denominator);
    }


    private double computeInterval(Matrix ratings) {
        Set<Double> values = new TreeSet<Double>();
        int numCoders = ratings.rows();
        int numItems = ratings.columns();
        int numObservedRatings = 0;
        for (int item = 0; item < numItems; ++item) {
            int itemRatings = 0;
            for (int coder = 0; coder < numCoders; ++coder) {
                double rating = ratings.get(coder, item);
                if (!Double.isNaN(rating)) {
                    values.add(rating);
                    itemRatings++;
                }
            }
            if (itemRatings > 1)
                numObservedRatings += itemRatings;
        }

        Matrix coincidenceMatrix = 
            new ArrayMatrix(values.size(), values.size());

        double[] valuesArr = new double[values.size()];
        double[] valueCounts = new double[values.size()];
        int i = 0;
        for (Double d : values)
            valuesArr[i++] = d;

        for (i = 0; i < valuesArr.length; ++i) {
            for (int j = i; j < valuesArr.length; ++j) {

                double rating1 = valuesArr[i];
                double rating2 = valuesArr[j];

                // For each rating pair, find out how many times this pair
                // occurred together across all of the annotated items
                for (int item = 0; item < numItems; ++item) {
                    
                    // How many coders answered this item
                    int numResponses = 0;

                    // How many pairs were found in this item that matched the
                    // target
                    int numPairsFound = 0;

                    for (int coder = 0; coder < numCoders; ++coder) {
                        double response = ratings.get(coder, item);
                        if (Double.isNaN(response))
                            continue;
                        numResponses++;

                        // Re-iterate through the responses again and find out
                        // how many pairs (i=rating1,j) can be constructed where
                        // j=rating2.
                        if (rating1 == response) {
                            for (int c = 0; c < numCoders; ++c) {
                                if (c == coder)
                                    continue;
                                double r2 = ratings.get(c, item);
                                if (Double.isNaN(r2))
                                    continue;
                                if (r2 == rating2) {
                                    numPairsFound++;
                                }
                            }
                        }
                    }
                                       
                    double coincidence =  (numResponses == 1)
                        ? 0 
                        : numPairsFound / (numResponses - 1d);
                    double curCoincidence = coincidenceMatrix.get(i, j);

                    coincidenceMatrix.set(i, j, curCoincidence + coincidence);
                    coincidenceMatrix.set(j, i, curCoincidence + coincidence);
                }
            }
        }

        for (i = 0; i < valuesArr.length; ++i) {
            for (int j = 0; j < valuesArr.length; ++j) {
                valueCounts[i] += coincidenceMatrix.get(i, j);
            }
        }
        
        Matrix differenceFunctionMatrix = 
            new ArrayMatrix(valuesArr.length, valuesArr.length); 
        for (i = 0; i < valuesArr.length; ++i) {
            for (int j = i+1; j < valuesArr.length; ++j) {
         
                double v1 = valuesArr[i];
                double v2 = valuesArr[j];
                double diff = (v1 - v2) * (v1 - v2);

                differenceFunctionMatrix.set(i, j, diff);
                differenceFunctionMatrix.set(j, i, diff);
            }
        }
              
        double numerator = 0;
        double denominator = 0;

         for (i = 0; i < valuesArr.length; ++i) {
             for (int j = i+1; j < valuesArr.length; ++j) {
                 double coincidence = coincidenceMatrix.get(i, j);
                 double difference = differenceFunctionMatrix.get(i, j);
                 numerator += coincidence * difference;
                 denominator += valueCounts[i] * valueCounts[j] * difference;
             }
         }

         return 1d - (numObservedRatings - 1) * (numerator / denominator);
    }

    // NOTE: marked private until further testing can tell whether this actually
    // is working... -jurgens
    private double[] computeConfidenceIntervals(
            Matrix ratings, LevelOfMeasurement level, 
            int numResamples, double[] confidenceIntervals) {

        for (int i = 0; i < confidenceIntervals.length; ++i) {
            double confidence = confidenceIntervals[i];
            if (confidence < 0 || confidence > 1)
                throw new IllegalArgumentException(
                    "confidence out of range [0,1]: " + confidence);
        }

        Set<Double> values = new TreeSet<Double>();
        int numCoders = ratings.rows();
        int numItems = ratings.columns();
        int numObservedRatings = 0;
        for (int item = 0; item < numItems; ++item) {
            int itemRatings = 0;
            for (int coder = 0; coder < numCoders; ++coder) {
                double rating = ratings.get(coder, item);
                if (!Double.isNaN(rating)) {
                    values.add(rating);
                    itemRatings++;
                }
            }
            if (itemRatings > 1)
                numObservedRatings += itemRatings;
        }

        // Create a list of all the values used in this observation
        double[] valueList = new double[numObservedRatings];
        int idx = 0;
        for (int item = 0; item < numItems; ++item) {
            int itemRatings = 0;
            for (int coder = 0; coder < numCoders; ++coder) {
                double rating = ratings.get(coder, item);
                if (!Double.isNaN(rating)) {
                    itemRatings++;
                }
            }
            if (itemRatings > 1) {
                for (int coder = 0; coder < numCoders; ++coder) {
                    double rating = ratings.get(coder, item);
                    if (!Double.isNaN(rating)) {
                        valueList[idx++] = rating;
                    }
                }
            }
        }

        double[] resamples = new double[numResamples];
        
   
      for (int resample = 0; resample < numResamples; ++resample) {

            Matrix sampledAssignments = getRandomSample(ratings);            
            double sampleAlpha = compute(sampledAssignments, level);
            resamples[resample] = sampleAlpha;
        }

        Arrays.sort(resamples);
        double[] alphasAtConfidence = new double[confidenceIntervals.length];
        for (int i = 0; i < confidenceIntervals.length; ++i) {
            double confidence = confidenceIntervals[i];
            int index = Math.min(resamples.length-1,
                                 (int)(resamples.length * confidence));
            System.out.printf("resamples[%d] = %f%n", index, resamples[index]);
            alphasAtConfidence[i] = resamples[index];
        }
        
        return alphasAtConfidence;
    }    


    static Matrix getRandomSample(Matrix m) {
        int items = m.columns();
        int coders = m.rows();
        Matrix sampled = new ArrayMatrix(coders, items);

        for (int item = 0; item < items; ++item) {
            // Pick an item from the original matrix and add that as the new
            // rating
            int randomItem = (int)(Math.random() * items);
            for (int coder = 0; coder < coders; ++coder) {
                sampled.set(coder, item, m.get(coder, randomItem));
            }
        }
        return sampled;
    }

    static Matrix getRandomSample(int coders, int items, int samples,
                                  double[] values) {
        Matrix sample = new ArrayMatrix(coders, items);


        double[] valuesToAssign = Arrays.copyOf(values, values.length);

        // REMINDER: special case this if there are no missing values
        List<IntPair> toAssign = new ArrayList<IntPair>(samples);
        for (int c = 0; c < coders; ++c) {
            for (int i = 0; i < items; ++i) 
                toAssign.add(new IntPair(c, i));
        }
        Collections.shuffle(toAssign);
        
        for (int i = 0; i < samples; ++i) {
            IntPair assignment = toAssign.get(i);
            sample.set(assignment.x, assignment.y, values[i]);
        }
        
        return sample;
    }
}
