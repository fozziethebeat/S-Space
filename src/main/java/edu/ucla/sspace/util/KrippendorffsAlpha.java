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

import edu.ucla.sspace.util.primitive.IntPair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 *
 *
 *
 * @since 2.0
 */ 
public class KrippendorffsAlpha {

    public enum DifferenceFunction {
            NOMINAL,
            ORDINAL,
            INTERVAL,
            RATIO,
            POLAR,
            CIRCULAR
    }

    public double compute(Matrix ratings, DifferenceFunction diffFunc) {
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
//                                     System.out.printf(" (coder-%d,coder-%d)%n",
//                                                       coder, c);
                                    numPairsFound++;
                                }
                            }
                        }
                    }
                                       
                    double coincidence =  (numResponses == 1)
                        ? 0 
                        : numPairsFound / (numResponses - 1d);
                    double curCoincidence = coincidenceMatrix.get(i, j);

//                     System.out.printf("Found %d pairs of (%f, %f) in item " +
//                                       "%d (with %d responses), co-incidence " +
//                                       "is now: %f%n", numPairsFound, 
//                                       rating1, rating2, item, numResponses,
//                                       curCoincidence + coincidence);

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

//         System.out.print(" ");
//         for (i = 0; i < valuesArr.length; ++i) 
//             System.out.print("\t" + valuesArr[i]);
//         System.out.println();

//         for (i = 0; i < valuesArr.length; ++i) {
//             System.out.print(valuesArr[i]);
//             for (int j = 0; j < valuesArr.length; ++j) {
//                 System.out.printf("\t%.2f", coincidenceMatrix.get(i, j));
//             }
//             System.out.println();
//         }

//         System.out.println(java.util.Arrays.toString(valueCounts));
        
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
                
//         System.out.print(" ");
//         for (i = 0; i < valuesArr.length; ++i) 
//             System.out.print("\t" + valuesArr[i]);
//         System.out.println();

//         for (i = 0; i < valuesArr.length; ++i) {
//             System.out.print(valuesArr[i]);
//             for (int j = 0; j < valuesArr.length; ++j) {
//                 System.out.printf("\t%.2f", differenceFunctionMatrix.get(i, j));
//             }
//             System.out.println();
//         }
        
        

        double numerator = 0;
        double denominator = 0;

//         System.out.println("n: " + numObservedRatings);

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
    
//     static double compute(DifferenceFunction d, double value1, double value2) {
//         switch (d) {
//         case ORDINAL:
//             return 
//     }

    public double[] computeConfidenceIntervals(
            Matrix ratings, DifferenceFunction diffFunc, 
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
        /*

        double origAlpha = compute(ratings, diffFunc);

        // int[] bins = new int[2001];


        for (int resample = 0; resample < numResamples; ++resample) {

            Matrix sampledAssignments = getRandomSample(ratings);
                                                        
//                     numCoders, numItems, numObservedRatings, valueList);
            
            double sampleAlpha = compute(sampledAssignments, diffFunc);

            if (sampleAlpha > 1)
                sampleAlpha = 1;

            // int sampleAlphaIndex = (int)(sampleAlpha * 1000) + 1000;
            // bins[sampleAlphaIndex]++;
            samples[resample] = sampleAlpha;
        }



        int origAlphaIndex = (int)(origAlpha * 1000) + 1000;

        double[] alphasAtConfidence = new double[confidenceIntervals.length];

        double x = numResamples;

         for (int i = 0; i < bins.length; ++i) {
             System.err.println( ((i - 1000d) / 1000) + " " + bins[i]);
         }

        for (int i = 0; i < confidenceIntervals.length; ++i) {
            double confidence = confidenceIntervals[i];
            
            double lowerBound = Double.NaN;
            double upperBound = Double.NaN;

            double probSum = 0;
            for (int j = origAlphaIndex; j >= 0; j--) {
                double prob = bins[j] / x;
                probSum += prob;
                if (probSum >= confidence/2) {
                    lowerBound = (j - 1000d) / 1000;
                    break;
                }
            }
            probSum = 0;
            for (int j = origAlphaIndex; j < bins.length; ++j) {
                double prob = bins[j] / x;
                probSum += prob;
                if (probSum >= 1 - (confidence/2)) {
                    upperBound = (j - 1000d) / 1000;
                    break;
                }

            }
            
            System.out.printf("Confidence at %f for alpha %f: (%f, %f)%n",
                              confidence, origAlpha, lowerBound, upperBound);
        }
        
        return alphasAtConfidence;



    }
    */
        
   
      for (int resample = 0; resample < numResamples; ++resample) {

            Matrix sampledAssignments = getRandomSample(ratings);
//                 getRandomSample(
//                 numCoders, numItems, numObservedRatings, values);
            
            double sampleAlpha = compute(sampledAssignments, diffFunc);
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
        
//         // Dump the Set of values to an array for faster random access
//         double[] valueArr = new double[values.size()];
//         int x = 0;
//         for (Double d : values)
//             valueArr[x++] = d;

        for (int i = 0; i < samples; ++i) {
            IntPair assignment = toAssign.get(i);
            sample.set(assignment.x, assignment.y, values[i]);
//                       valueArr[(int)(Math.random() * valueArr.length)]);
        }
        
        return sample;
    }
}
