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

import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.IntegerMap;
import edu.ucla.sspace.util.ObjectCounter;


/**
 * A collection of static methods for statistical analysis and basic numeric
 * computation.
 *
 * <p>Unless otherwise noted all methods will throw a {@link
 * NullPointerException} if passed a {@code null} object.
 */
public class Statistics {

    /**
     * Uninstantiable
     */
    private Statistics() { }

    /**
     * Returns the entropy of an array representing a (potentially) unormalized
     * probability distribution).  Note that this method uses the natural log
     * when computing the entropy.
     */
    public static double entropy(int[] a) {
        double sum = sum(a);
        double entropy = 0;
        for (int x : a) {
            double p = x / sum;
            if (p != 0d)
                entropy += Math.log(p) * p;
        }
        return -entropy;
    }

    /**
     * Returns the entropy of an array representing a (potentially) unormalized
     * probability distribution).  Note that this method uses the natural log
     * when computing the entropy.
     */
    public static double entropy(double[] a) {
        double sum = sum(a);
        double entropy = 0;
        for (double x : a) {
            double p = x / sum;
            if (p != 0d)
                entropy += Math.log(p) * p;
        }
        return -entropy; 
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
     * Returns the mode value of the collection of numbers
     */
    public static <T extends Number> T mode(Collection<T> values) {
        if (values.isEmpty())
            throw new IllegalArgumentException(
                "No mode in an empty collection");
        Counter<T> c = new ObjectCounter<T>();
        for (T n : values)
            c.count(n);
        return c.max();
    }

    /**
     * Returns the mode value of the array of ints
     */
    public static int mode(int[] values) {
        if (values.length == 0)
            throw new IllegalArgumentException("No mode in an empty array");        
        Counter<Integer> c = new ObjectCounter<Integer>();
        for (int i : values)
            c.count(i);
        return c.max();
    }

    /**
     * Returns the mode value of the array of doubles
     */
    public static double mode(double[] values) {
        if (values.length == 0)
            throw new IllegalArgumentException("No mode in an empty array");        
        Counter<Double> c = new ObjectCounter<Double>();
        for (double d : values)
            c.count(d);
        return c.max();
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
}
