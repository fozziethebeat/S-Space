/*
 * Copyright 2009 David Jurgens 
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

package edu.ucla.sspace.common;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucla.sspace.util.IntegerMap;


/**
 * A collection of static methods for statistical analysis and basic numeric
 * computation.
 */
public class Statistics {

    /**
     * Uninstantiable
     */
    private Statistics() { }

    /**
     * Returns the entropy of the array.
     */
    public static double entropy(int[] a) {
        Map<Integer,Integer> symbolFreq = new IntegerMap<Integer>();
        for (int i : a) {
            Integer freq = symbolFreq.get(i);
            symbolFreq.put(i, (freq == null) ? 1 : 1 + freq);
        }
        
        double entropy = 0;
        double symbols = a.length;
        for (Integer freq : symbolFreq.values()) {
            double symbolProbability = freq / symbols;
            entropy -= symbolProbability * log2(symbolProbability);
        }

        return entropy;
    }

    /**
     * Returns the entropy of the array.
     */
    public static double entropy(double[] a) {
        Map<Double,Integer> symbolFreq = new HashMap<Double,Integer>();
        for (double d : a) {
            Integer freq = symbolFreq.get(d);
            symbolFreq.put(d, (freq == null) ? 1 : 1 + freq);
        }
        
        double entropy = 0;
        double symbols = a.length;
        for (Integer freq : symbolFreq.values()) {
            double symbolProbability = freq / symbols;
            entropy -= symbolProbability * log2(symbolProbability);
        }

        return entropy;
    }

    /**
     * Returns the base-2 logarithm of {@code d}.
     */
    public static double log2(double d) {
        return Math.log(d) / Math.log(2);
    }

    /**
     * Returns the base-2 logarithm of {@code d + 1}.
     * 
     * @see Math#log1p(double)
     */
    public static double log2_1p(double d) {
        return Math.log1p(d) / Math.log(2);
    }

    /**
     * Returns the mean value of the collection of numbers
     */
    public static double mean(Collection<? extends Number> values) {
        double sum = 0d;
        for (Number n : values)
            sum += n.doubleValue();
        return sum / values.size();
    }

    /**
     * Returns the mean value of the array of ints
     */
    public static double mean(int[] values) {
        double sum = 0d;
        for (int i : values)
            sum += i;
        return sum / values.length;
    }

    /**
     * Returns the mean value of the array of doubles
     */
    public static double mean(double[] values) {
        double sum = 0d;
        for (double d : values)
            sum += d;
        return sum / values.length;
    } 

    /**
     * Returns the median value of the collection of numbers
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number & Comparable> T median(Collection<T> values) {
        if (values.isEmpty())
            throw new IllegalArgumentException(
                "No median in an empty collection");
        List<T> sorted = new ArrayList<T>(values);
        
        Collections.sort(sorted);
        return sorted.get(sorted.size() / 2);
    }

    /**
     * Returns the median value of the array of ints
     */
    public static double median(int[] values) {
        if (values.length == 0)
            throw new IllegalArgumentException("No median in an empty array");        
        int[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        return sorted[sorted.length/2];
    }

    /**
     * Returns the median value of the array of doubles
     */
    public static double median(double[] values) {
        if (values.length == 0)
            throw new IllegalArgumentException("No median in an empty array");        
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        return sorted[sorted.length/2];
    }

    /**
     * Randomly sets {@code valuesToSet} values to {@code true} for a sequence
     * from [0:{@code range}).
     *
     * @param valuesToSet the number of values that are to be set to {@code
     *        true} in the distribution
     * @param range the total number of values in the sequence.
     */
    public static BitSet randomDistribution(int valuesToSet, int range) {
        if (valuesToSet < 0 || range <= 0) 
            throw new IllegalArgumentException("must specificy a positive " +
                "range and non-negative number of values to set.");       
        if (valuesToSet > range)
            throw new IllegalArgumentException("too many values (" + valuesToSet
                + ") for range " + range);
        BitSet values = new BitSet(range);
        // We will be setting fewer than half of the values, so set everything
        // to false, and mark true until the desired number is reached
        if (valuesToSet < (range / 2)) {
            int set = 0;
            while (set < valuesToSet) {
                int i = (int)(Math.random() * range);
                if (!values.get(i)) {
                    values.set(i, true);
                    set++;
                }
            }
        }
        // We will be setting more than half of the values, so set everything to
        // true, and mark false until the desired number is reached
        else {
            values.set(0, range, true);
            int set = range;
            while (set > valuesToSet) {
                int i = (int)(Math.random() * range);
                if (values.get(i)) {
                    values.set(i, false);
                    set--;
                }
            }
        }
        return values;
    }

    /**
     * Returns the standard deviation of the collection of numbers
     */
    public static double stddev(Collection<? extends Number> values) {
        double mean = mean(values);
        double sum = 0d;
        for (Number n : values) {
            double d = n.doubleValue() - mean;
            sum += d*d;
        }
        return Math.sqrt(sum / values.size());
    }

    /**
     * Returns the standard deviation of the values in the int array
     */
    public static double stddev(int[] values) {
        double mean = mean(values);
        double sum = 0d;
        for (int i : values) {
            double d = i - mean;
            sum += d*d;
        }
        return Math.sqrt(sum / values.length);
    }

    /**
     * Returns the standard deviation of the values in the double array
     */
    public static double stddev(double[] values) {
        double mean = mean(values);
        double sum = 0d;
        for (double d : values) {
            double d2 = d - mean;
            sum += d2 * d2;
        }
        return Math.sqrt(sum / values.length);
    }

    /**
     * Returns the sum of the collection of numbers
     */
    public static double sum(Collection<? extends Number> values) {
        double sum = 0d;
        for (Number n : values)
            sum += n.doubleValue();
        return sum;
    }

    /**
     * Returns the sum of the values in the int array
     */
    public static int sum(int[] values) {
        int sum = 0;
        for (int i : values) 
            sum += i;        
        return sum;
    }

    /**
     * Returns the sum of the values in the double array
     */
    public static double sum(double[] values) {
        double sum = 0d;
        for (double d : values) 
            sum += d;
        return sum;
    }

    // The following implementations of the bessel functions are copied, with
    // slight modifications, from a StackOverflow post:
    //   http://stackoverflow.com/questions/8797722/modified-bessel-functions-of-order-n
    // and are based on the algorithm in  "Numerical Recipes", http://www.nr.com/

    public static final double ACC = 4.0; 
    public static final double BIGNO = 1.0e10;
    public static final double BIGNI = 1.0e-10;

    public static final double bessi0(double x) {
        double answer;
        double ax = Math.abs(x);
        if (ax < 3.75) { // polynomial fit
            double y = x / 3.75;
            y *= y;
            answer = 1.0 + y * (3.5156229 + y * (3.0899424 + y * (1.2067492 + y * (0.2659732 + y * (0.360768e-1 + y * 0.45813e-2)))));
        } else {
            double y = 3.75 / ax;
            answer = 0.39894228 + y * (0.1328592e-1 + y * (0.225319e-2 + y * (-0.157565e-2 + y * (0.916281e-2 + y * (-0.2057706e-1 + y * (0.2635537e-1 + y * (-0.1647633e-1 + y * 0.392377e-2)))))));
            answer *= (Math.exp(ax) / Math.sqrt(ax));
        }
        return answer;
    }

    public static final double bessi1(double x) {
        double answer;
        double ax = Math.abs(x);
        if (ax < 3.75) { // polynomial fit
            double y = x / 3.75;
            y *= y;
            answer = ax * (0.5 + y * (0.87890594 + y * (0.51498869 + y * (0.15084934 + y * (0.2658733e-1 + y * (0.301532e-2 + y * 0.32411e-3))))));
        } else {
            double y = 3.75 / ax;
            answer = 0.2282967e-1 + y * (-0.2895312e-1 + y * (0.1787654e-1 - y * 0.420059e-2));
            answer = 0.39894228 + y * (-0.3988024e-1 + y * (-0.362018e-2 + y * (0.163801e-2 + y * (-0.1031555e-1 + y * answer))));
            answer *= (Math.exp(ax) / Math.sqrt(ax));
        }
        return answer;
    }

    public static final double bessi(int n, double x) {
        if (n == 0)
            return bessi0(x);
        if (n == 1)
            return bessi1(x);
        if (x == 0.0)
            return 0.0;

        double tox = 2.0/Math.abs(x);
        double ans = 0.0;
        double bip = 0.0;
        double bi  = 1.0;
        for (int j = 2*(n + (int)Math.sqrt(ACC*n)); j > 0; --j) {
            double bim = bip + j*tox*bi;
            bip = bi;
            bi = bim;
            if (Math.abs(bi) > BIGNO) {
                ans *= BIGNI;
                bi *= BIGNI;
                bip *= BIGNI;
            }
            if (j == n) {
                ans = bip;
            }
        }
        ans *= bessi0(x)/bi;
        return (((x < 0.0) && ((n % 2) == 0)) ? -ans : ans);
    }
}
