/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.vector;


/**
 * A collection of static arithmetic operations on {@code Vector} instances. <p>
 *
 * The methods of this class all throw a {@code NullPointerException} if the
 * {@link Vector} objects provided to them are {@code null}.
 *
 * @author Keith Stevens
 */
public class VectorMath {

    /**
     * A private constructor to make this class uninstantiable.
     */
    private VectorMath() { }

    /**
     * Adds the second {@code Vector} to the first {@code Vector} and returns 
     * the result.
     *
     * @param vector1 The destination vector to be summed onto.
     * @param vector2 The source vector to sum from.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static Vector add(Vector vector1, Vector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        if (vector2 instanceof IntegerVector &&
            vector1 instanceof DoubleVector)
            return add(vector1, Vectors.asDouble(vector2));
        if (vector2 instanceof SparseVector)
            addSparseValues(vector1, vector2);
        else {
            int length = vector2.length();
            for (int i = 0; i < length; ++i) {
                double value = vector2.getValue(i).doubleValue() +
                               vector1.getValue(i).doubleValue();
                vector1.set(i, value);
            }
        }

        return vector1;
    }

    /**
     * Adds the second {@code DoubleVector} to the first {@code DoubleVector}
     * and returns the result.
     *
     * @param vector1 The destination vector to be summed onto.
     * @param vector2 The source vector to sum from.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static DoubleVector add(DoubleVector vector1,
                                   DoubleVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added.  " +
                    "Lengths are: vector1: " + vector1.length() +
                    ", vector2: " + vector2.length());
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector)
            addSparseValues(vector1, vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            int length = vector2.length();
            for (int i = 0; i < length; ++i) {
                double value = vector2.get(i);
                // In the case that vector1 is sparse, only add non zero values.
                if (value != 0d)
                    vector1.add(i, value);
            }
        }
        return vector1;
    }

    /**
     * Adds the second {@code IntegerVector} to the first {@code IntegerVector}
     * and returns the result.
     *
     * @param vector1 The destination vector to be summed onto.
     * @param vector2 The source vector to sum from.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static IntegerVector add(IntegerVector vector1,
                                    IntegerVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector) 
            addSparseValues(vector1, vector2);
        else if (vector2 instanceof TernaryVector)
            addTernaryValues(vector1, (TernaryVector)vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            int length = vector2.length();
            for (int i = 0; i < length; ++i) {
                int value = vector2.get(i);
                // In the case that vector1 is sparse, only add non zero values.
                if (value != 0d)
                    vector1.add(i, value);
            }
        }
        return vector1;
    }

    /**
     * Returns a new {@code Vector} which is the summation of {@code vector2}
     * and {@code vector1}.
     *
     * @param vector1 The first vector to used in a summation.
     * @param vector2 The second vector to be used in a summation.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static Vector addUnmodified(Vector vector1, Vector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        return addUnmodified(Vectors.asDouble(vector1),
                             Vectors.asDouble(vector2));
    }

    /**
     * Returns a new {@code DoubleVector} which is the summation of {@code
     * vector2} and {@code vector1}.
     *
     * @param vector1 The first vector to used in a summation.
     * @param vector2 The second vector to be used in a summation.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static DoubleVector addUnmodified(DoubleVector vector1,
                                             DoubleVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        DoubleVector finalVector = Vectors.copyOf(vector1);
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector)
            addSparseValues(finalVector, vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            int length = vector2.length();
            for (int i = 0; i < length; ++i) {
                double value = vector2.get(i);
                finalVector.add(i, value);
            }
        }
        return finalVector;
    }

    /**
     * Returns a new {@code IntegerVector} which is the summation of {@code
     * vector2} and {@code vector1}.
     *
     * @param vector1 The first vector to used in a summation.
     * @param vector2 The second vector to be used in a summation.
     * @return The summation of {@code vector1} and {@code vector2}.
     */
    public static IntegerVector addUnmodified(IntegerVector vector1,
                                              IntegerVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        IntegerVector finalVector = Vectors.copyOf(vector1);
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector)
            addSparseValues(finalVector, vector2);
        else if (vector2 instanceof TernaryVector)
            addTernaryValues(finalVector, (TernaryVector)vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            int length = vector2.length();
            for (int i = 0; i < length; ++i) {
                int value = vector2.get(i);
                finalVector.add(i, value);
            }
        }
        return finalVector;
    }

    /**
     * Adds two {@code Vector}s with some scalar weight for each {@code Vector}.
     *
     * @param vector1 The vector values should be added to.
     * @param weight1 The weight of values in {@code vector1}
     * @param vector2 The vector values that should be added to {@code vector1}
     * @param weight2 The weight of values in {@code vector2}
     *
     * @param {@code vector1}
     */
    public static Vector addWithScalars(Vector vector1, double weight1,
                                        Vector vector2, double weight2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        int length = vector2.length();
        for (int i = 0; i < length; ++i) {
            double value = vector1.getValue(i).doubleValue() * weight1 +
                           vector2.getValue(i).doubleValue()  * weight2;
            vector1.set(i, value);
        }
        return vector1;
    }

    /**
     * Adds two {@code DoubleVector}s with some scalar weight for each {@code
     * DoubleVector}.
     *
     * @param vector1 The vector values should be added to.
     * @param weight1 The weight of values in {@code vector1}
     * @param vector2 The vector values that should be added to {@code vector1}
     * @param weight2 The weight of values in {@code vector2}
     *
     * @param {@code vector1}
     */
    public static Vector addWithScalars(DoubleVector vector1, double weight1,
                                        DoubleVector vector2, double weight2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        int length = vector2.length();
        for (int i = 0; i < length; ++i) {
            double value = vector1.get(i) * weight1 +
                           vector2.get(i) * weight2;
            vector1.set(i, value);
        }
        return vector1;
    }

    /**
     * Adds two {@code IntegerVector}s with some scalar weight for each {@code
     * Vector}.
     *
     * @param vector1 The vector values should be added to.
     * @param weight1 The weight of values in {@code vector1}
     * @param vector2 The vector values that should be added to {@code vector1}
     * @param weight2 The weight of values in {@code vector2}
     *
     * @param {@code vector1}
     */
    public static Vector addWithScalars(IntegerVector vector1, int weight1,
                                        IntegerVector vector2, int weight2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        int length = vector2.length();
        for (int i = 0; i < length; ++i) {
            double value = vector1.get(i) * weight1 +
                           vector2.get(i) * weight2;
            vector1.set(i, value);
        }
        return vector1;
    }

    /**
     * Computes the dot product, {@code x}<sup>T</sup>{@code y} of the two
     * vectors.
     *
     * @param x the left vector that will be transposed
     * @param y the right vector
     *
     * @return the dot product of the two vectors.
     *
     * @throws IllegalArgumentException if the two vectors are not of equal
     *         length
     */
    public static double dotProduct(Vector x, Vector y) {
        // If both of the vectors are integer vectors, cast and special case
        if (x instanceof IntegerVector && y instanceof IntegerVector)
            return dotProduct((IntegerVector)x, (IntegerVector)y);
        // Otherwise, just make both vectors double vectors and compute the dot
        // product regardless of their internal data type.
        else 
            return dotProduct(Vectors.asDouble(x), Vectors.asDouble(y));
    }
    
    /**
     * Computes the dot product, {@code x}<sup>T</sup>{@code y} of the two
     * vectors.
     *
     * @param x the left vector that will be transposed
     * @param y the right vector
     *
     * @return the dot product of the two vectors.
     *
     * @throws IllegalArgumentException if the two vectors are not of equal
     *         length
     */
    public static double dotProduct(DoubleVector x, DoubleVector y) {
        if (x.length() != y.length())
            throw new IllegalArgumentException(
                "cannot compute dot product of vectors with different lengths");

        double dotProduct = 0;
        // Special case if either of the vectors is a sparse vectors to save on
        // computation costs.
        if (x instanceof SparseDoubleVector) {
            SparseDoubleVector sdv = (SparseDoubleVector)x;
            for (int i  : sdv.getNonZeroIndices()) 
                dotProduct += x.get(i) * y.get(i);
        }
        else if (y instanceof SparseDoubleVector) {
            SparseDoubleVector sdv = (SparseDoubleVector)y;
            for (int i  : sdv.getNonZeroIndices()) 
                dotProduct += x.get(i) * y.get(i);
        }
        else {
            int length = x.length();
            for (int i = 0; i < length; ++i)
                dotProduct += x.get(i) * y.get(i);
        }
        return dotProduct;
    }

    /**
     * Computes the dot product, {@code x}<sup>T</sup>{@code y} of the two
     * vectors.
     *
     * @param x the left vector that will be transposed
     * @param y the right vector
     *
     * @return the dot product of the two vectors.
     *
     * @throws IllegalArgumentException if the two vectors are not of equal
     *         length
     */
    public static int dotProduct(IntegerVector x, IntegerVector y) {
        if (x.length() != y.length())
            throw new IllegalArgumentException(
                "cannot compute dot product of vectors with different lengths");
        
        int dotProduct = 0;
        // Special case if either of the vectors is a sparse vectors to save on
        // computation costs.
        if (x instanceof SparseIntegerVector) {
            SparseIntegerVector sdv = (SparseIntegerVector)x;
            for (int i  : sdv.getNonZeroIndices()) 
                dotProduct += x.get(i) * y.get(i);
        }
        else if (y instanceof SparseIntegerVector) {
            SparseIntegerVector sdv = (SparseIntegerVector)y;
            for (int i  : sdv.getNonZeroIndices()) 
                dotProduct += x.get(i) * y.get(i);
        }
        else {
            int length = x.length();
            for (int i = 0; i < length; ++i)
                dotProduct += x.get(i) * y.get(i);
        }
        return dotProduct;
    }

    /**
     * Mulitplies the values in {@code left} and {@code right} and store the
     * product in {@code left}.  This is an element by element multiplication.
     *
     * @param left The left {@code Vector} to multiply, and contain the result
     *             values.
     * @param right The right {@code Vector} to multiply.
     *
     * @return The product of {@code left} and {@code right}
     */
    public static Vector multiply(Vector left, Vector right) {
        if (left.length() != right.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be multiplied");
        int length = left.length();
        for (int i = 0; i < length; ++i)
            left.set(i, left.getValue(i).doubleValue() *
                        right.getValue(i).doubleValue());
        return left;
    }

    /**
     * Multiply the values in {@code left} and {@code right} and store the
     * product in {@code left}.  This is an element by element multiplication.
     *
     * @param left The left {@code Vector} to multiply, and contain the result
     *             values.
     * @param right The right {@code Vector} to multiply.
     *
     * @return The product of {@code left} and {@code right}
     */
    public static DoubleVector multiply(DoubleVector left, DoubleVector right) {
        if (left.length() != right.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be multiplied");
        int length = left.length();
        for (int i = 0; i < length; ++i)
            left.set(i, left.get(i) * right.get(i));
        return left;
    }
    /**
     * Multiply the values in {@code a} and {@code b} and store the
     * product in a new {@link CompactSparseVector} This is an element by
     * element multiplication.
     *
     * @param a The left {@code Vector} to multiply
     * @param b The right {@code Vector} to multiply.
     *
     * @return The product of {@code left} and {@code right}
     */
    public static SparseDoubleVector multiplyUnmodified(SparseDoubleVector a,
                                                        SparseDoubleVector b) {
        SparseDoubleVector result = new CompactSparseVector();
        int[] nonZerosA = a.getNonZeroIndices();
        int[] nonZerosB = b.getNonZeroIndices();
        if (nonZerosA.length == 0 || nonZerosB.length == 0)
            return result;

        if (nonZerosA[nonZerosA.length-1] > nonZerosB[nonZerosB.length-1]) {
            SparseDoubleVector t = b;
            b = a;
            a = t;
        }
        nonZerosA = a.getNonZeroIndices();

        for (int index : nonZerosA) {
            double v = a.get(index);
            double w = b.get(index);
            if (w != 0d)
                result.set(index, v*w);
        }
        return result;
    }

    /**
     * Multiply the values in {@code left} and {@code right} and store the
     * product in {@code left}.  This is an element by element multiplication.
     *
     * @param left The left {@code Vector} to multiply, and contain the result
     *             values.
     * @param right The right {@code Vector} to multiply.
     *
     * @return The product of {@code left} and {@code right}
     */
    public static IntegerVector multiply(IntegerVector left,
                                         IntegerVector right) {
        if (left.length() != right.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be multiplied");
        int length = left.length();
        for (int i = 0; i < length; ++i)
            left.set(i, left.get(i) * right.get(i));
        return left;
    }

    /**
     * Multiply the values in {@code left} and {@code right} and store the
     * product in a new {@code Vector}.  This is an element by element
     * multiplication.
     *
     * @param left The left {@code Vector} to multiply.
     * @param right The right {@code Vector} to multiply.
     *
     * @return The product of {@code left} and {@code right}
     */
    public static DoubleVector multiplyUnmodified(DoubleVector left,
                                                  DoubleVector right) {
        if (left.length() != right.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be multiplied");
        DoubleVector result;
        if (left instanceof SparseVector ||
            right instanceof SparseVector)
            result = new CompactSparseVector(left.length());
        else
            result = new DenseVector(left.length());

        int length = left.length();
        for (int i = 0; i < length; ++i)
            result.set(i, left.get(i) * right.get(i));
        return result;
    }

    /**
     * Subtracts the second {@code Vector} fromt the first {@code Vector} and
     * returns the result.
     *
     * @param vector1 The destination vector to be subtracted from.
     * @param vector2 The source vector to subtract.
     * @return The subtraction of {code vector2} from {@code vector1}.
     */
    public static Vector subtract(Vector vector1, Vector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        if (vector2 instanceof IntegerVector &&
            vector1 instanceof DoubleVector)
            return subtract(vector1, Vectors.asDouble(vector2));
        if (vector2 instanceof SparseVector)
            subtractSparseValues(vector1, vector2);
        else {
            for (int i = 0; i < vector2.length(); ++i) {
                double value = vector1.getValue(i).doubleValue() -
                               vector2.getValue(i).doubleValue();
                vector1.set(i, value);
            }
        }

        return vector1;
    }

    /**
     * Subtracts the second {@code DoubleVector} from the first {@code
     * DoubleVector} and returns the result.
     *
     * @param vector1 The destination vector to be subtracted from.
     * @param vector2 The source vector to subtract.
     * @return The subtraction of {code vector2} from {@code vector1}.
     */
    public static DoubleVector subtract(DoubleVector vector1,
                                        DoubleVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector)
            subtractSparseValues(vector1, vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < vector2.length(); ++i) {
                double value = vector2.get(i);
                // In the case that vector1 is sparse, only add non zero values.
                if (value != 0d)
                    vector1.add(i, -1 * value);
            }
        }
        return vector1;
    }

    /**
     * Subtracts the second {@code IntegerVector} from the first {@code
     * IntegerVector} and returns the result.
     *
     * @param vector1 The destination vector to be subtracted from.
     * @param vector2 The source vector to subtract.
     * @return The subtraction of {code vector2} from {@code vector1}.
     */
    public static IntegerVector subtract(IntegerVector vector1,
                                         IntegerVector vector2) {
        if (vector2.length() != vector1.length())
            throw new IllegalArgumentException(
                    "Vectors of different sizes cannot be added");
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof SparseVector) 
            subtractSparseValues(vector1, vector2);
        else if (vector2 instanceof TernaryVector)
            subtractTernaryValues(vector1, (TernaryVector)vector2);
        else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < vector2.length(); ++i) {
                int value = vector2.get(i);
                // In the case that vector1 is sparse, only add non zero values.
                if (value != 0d)
                    vector1.add(i, -1 * value);
            }
        }
        return vector1;
    }

    /**
     * Adds the values from a {@code CompactSparseVector} to a {@code Vector}.
     * Only the non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addSparseValues(DoubleVector destination,
                                        DoubleVector source) {
        int[] otherIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : otherIndices)
            destination.add(index, source.get(index));
    }

    /**
     * Adds the values from a {@code CompactSparseVector} to a {@code Vector}.
     * Only the non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addSparseValues(IntegerVector destination,
                                        IntegerVector source) {
        int[] otherIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : otherIndices) {
            destination.add(index, source.get(index));
        }
    }

    /**
     * Adds the values from a {@code CompactSparseVector} to a {@code Vector}.
     * Only the non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addSparseValues(Vector destination,
                                        Vector source) {
        int[] otherIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : otherIndices) {
            double value = destination.getValue(index).doubleValue() +
                           source.getValue(index).doubleValue();
            destination.set(index, value);
        }
    }

    /**
     * Adds the values from a {@code TernaryVector} to an {@code IntegerVector}.
     * Only the positive and negative indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addTernaryValues(IntegerVector destination,
                                         TernaryVector source) {
        for (int p : source.positiveDimensions())
            destination.add(p, 1);
        for (int n : source.negativeDimensions())
            destination.add(n, -1);
    }

    /**
     * Adds the values from a {@code TernaryVector} to a {@code DoubleVector}.
     * Only the positive and negative indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addTernaryValues(DoubleVector destination,
                                         TernaryVector source) {
        for (int p : source.positiveDimensions())
            destination.add(p, 1);
        for (int n : source.negativeDimensions())
            destination.add(n, -1);
    }

    /**
     * Adds the values from a {@code TernaryVector} to a {@code Vector}.
     * Only the positive and negative indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addTernaryValues(Vector destination,
                                         TernaryVector source) {
        for (int p : source.positiveDimensions())
            destination.set(p, 1 + destination.getValue(p).doubleValue());
        for (int n : source.negativeDimensions())
            destination.set(n, -1 + destination.getValue(n).doubleValue());
    }

    /**
     * Adds the values from a {@code CompactSparseVector} to a {@code Vector}.
     * Only the non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void subtractSparseValues(DoubleVector destination,
                                             DoubleVector source) {
        int[] otherIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : otherIndices)
            destination.add(index, -1 * source.get(index));
    }

    /**
     * Adds the values from a {@code CompactSparseVector} to a {@code Vector}.
     * Only the non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void subtractSparseValues(IntegerVector destination,
                                             IntegerVector source) {
        int[] otherIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : otherIndices) {
            destination.add(index, -1 * source.get(index));
        }
    }

    /**
     * Adds the values from a {@code CompactSparseVector} to a {@code Vector}.
     * Only the non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void subtractSparseValues(Vector destination,
                                        Vector source) {
        int[] otherIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : otherIndices) {
            double value = destination.getValue(index).doubleValue() -
                           source.getValue(index).doubleValue();
            destination.set(index, value);
        }
    }

    /**
     * Adds the values from a {@code TernaryVector} to an {@code IntegerVector}.
     * Only the positive and negative indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void subtractTernaryValues(IntegerVector destination,
                                              TernaryVector source) {
        for (int p : source.positiveDimensions())
            destination.add(p, -1);
        for (int n : source.negativeDimensions())
            destination.add(n, 1);
    }

    /**
     * Adds the values from a {@code TernaryVector} to a {@code DoubleVector}.
     * Only the positive and negative indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void subtractTernaryValues(DoubleVector destination,
                                              TernaryVector source) {
        for (int p : source.positiveDimensions())
            destination.add(p, -1);
        for (int n : source.negativeDimensions())
            destination.add(n, 1);
    }

    /**
     * Adds the values from a {@code TernaryVector} to a {@code Vector}.
     * Only the positive and negative indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void subtractTernaryValues(Vector destination,
                                              TernaryVector source) {
        for (int p : source.positiveDimensions())
            destination.set(p, destination.getValue(p).doubleValue() - 1);
        for (int n : source.negativeDimensions())
            destination.set(n, destination.getValue(n).doubleValue() + 1);
    }
}
