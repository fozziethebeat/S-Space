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

import java.util.Arrays;

import java.lang.reflect.Constructor;


/**
 * A collection of static methods that operate on or return {@link Vector}
 * instances, following a format similar to that of {@link
 * java.util.Collections}.  <p>
 *
 * <p>Unless otherwise noted, all returned {@link Vector} instances implement
 * {@code Serializable}.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
public class Vectors {

    /**
     * A private constructor to make this class uninstantiable.
     */
    private Vectors() { }

    /**
     * Returns a view over the given {@code Vector} as a {@code DoubleVector}.
     * The returned vector is mutable but any changes will be converted into its
     * internal data-type, e.g. {@code int}, which may result in information
     * loss.
     *
     * @param v The {@code Vector} to return as a {@code DoubleVector}.
     *
     * @return a mutable {@code DoubleVector} view of {@code v}
     */
    public static DoubleVector asDouble(Vector v) {
        if (v == null)
            throw new NullPointerException("Cannot re-type a null vector");
        // NOTE: Special case the the sparse classes first to becase the
        // base interfaces of DoubleVectors and IntegerVectors matches as well.
        if (v instanceof SparseIntegerVector)
            return new IntAsSparseDoubleVector((SparseIntegerVector)v);
        if (v instanceof SparseDoubleVector)
            return (SparseDoubleVector) v;
        else if (v instanceof IntegerVector)
            return new IntAsDoubleVector((IntegerVector)v);
        else if (v instanceof DoubleVector)
            return (DoubleVector)v;
        // Base case: the vector is either of some unknown type or only
        // implements Vector.  Therefore, wrap its contents using Vector's API.
        else
            return new ViewVectorAsDoubleVector(v);
    }

    public static SparseDoubleVector asDouble(SparseIntegerVector v) {
        if (v == null)
            throw new NullPointerException("Cannot re-type a null vector");
        return new IntAsSparseDoubleVector(v);
    }

    /**
     * Returns a vector backed by the specified array.  Any changes to the
     * vector are written through to the array.  This method acts a bridge
     * between array-based and {@code Vector}-based computation.  
     *
     * @param array the array backing the vector
     *
     * @return a {@code Vector} view of the array
     */
    public static DoubleVector asVector(double[] array) {
        if (array == null)
            throw new NullPointerException("Cannot wrap a null array");
        return new DoubleArrayAsVector(array);
    }

    /**
     * Returns a vector backed by the specified array.  Any changes to the
     * vector are written through to the array.  This method acts a bridge
     * between array-based and {@code Vector}-based computation.  
     *
     * @param array the array backing the vector
     *
     * @return a {@code Vector} view of the array
     */
    public static IntegerVector asVector(int[] array) {
        if (array == null)
            throw new NullPointerException("Cannot wrap a null array");
        return new IntArrayAsVector(array);
    }

    /**
     * Returns {@code true} if the two vectors are equal to one another.  Two
     * {@code Vector} insances are considered equal if they contain the same
     * number of elements and all corresponding pairs of {@code double} values
     * are equal.
     */
    public static boolean equals(DoubleVector v1, DoubleVector v2) {
        if (v1.length() == v2.length()) {
            int length = v1.length();
            for (int i = 0; i < length; ++i) {
                if (v1.get(i) != v2.get(i))
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the two integer vectors are equal to one another.
     * Two {@code Vector} insances are considered equal if they contain the same
     * number of elements and all corresponding pairs of {@code int} values
     * are equal.
     */
    public static boolean equals(IntegerVector v1, IntegerVector v2) {
        if (v1.length() == v2.length()) {
            int length = v1.length();
            for (int i = 0; i < length; ++i) {
                if (v1.get(i) != v2.get(i))
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the two vectors are equal to one another.  Two
     * {@code Vector} insances are considered equal if they contain the same
     * number of elements and all corresponding pairs of {@code Number} values
     * are equal.  Two values {@code n1} and {@code n2} are considered equal if
     * {@code n1.equals(n2)}.
     */
    public static boolean equals(Vector v1, Vector v2) {
        if (v1.length() == v2.length()) {
            int length = v1.length();
            for (int i = 0; i < length; ++i) {
                Number n1 = v1.getValue(i);
                Number n2 = v2.getValue(i);
                if (!n1.equals(n2))
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns an immutable view of the given {@code DoubleVector}.
     *
     * @param vector The {@code DoubleVector} to decorate as immutable.
     * @return An immutable version of {@code vector}.
     */
    public static DoubleVector immutable(DoubleVector vector) {
        if (vector == null)
            throw new NullPointerException("Cannot create an immutable " +
                                           "null vector");
        return new DoubleVectorView(vector, true);
    }

    /**
     * Returns an immutable view of the given {@code SparseDoubleVector}.
     *
     * @param vector The {@code DoubleVector} to decorate as immutable.
     * @return An immutable version of {@code vector}.
     */
    public static SparseDoubleVector immutable(SparseDoubleVector vector) {
        if (vector == null)
            throw new NullPointerException("Cannot create an immutable " +
                                           "null vector");
         return new ViewDoubleAsDoubleSparseVector(vector, true);
    }

    /**
     * Returns an immutable view of the given {@code IntegerVector}.
     *
     * @param vector The {@code IntegerVector} to decorate as immutable.
     * @return An immutable version of {@code vector}.
     */
    public static IntegerVector immutable(IntegerVector vector) {
        if (vector == null)
            throw new NullPointerException("Cannot create an immutable " +
                                           "null vector");
        return new IntegerVectorView(vector, true);
    }

    /**
     * Returns an immutable view of the given {@code SparseIntegerVector}.
     *
     * @param vector The {@code IntegerVector} to decorate as immutable.
     * @return An immutable version of {@code vector}.
     */
    public static SparseIntegerVector immutable(SparseIntegerVector vector) {
        if (vector == null)
            throw new NullPointerException("Cannot create an immutable " +
                                           "null vector");
         return new ViewIntegerAsIntegerSparseVector(vector, true);
    }

    /**
     * Returns an immutable view of the given {@code Vector}.
     *
     * @param vector The {@code DoubleVector} to decorate as immutable.
     * @return An immutable version of {@code vector}.
     */
    public static Vector immutable(Vector vector) {
        if (vector == null)
            throw new NullPointerException("Cannot create an immutable " +
                                           "null vector");
        return new VectorView(vector, true);
    }

    /**
     * Returns a thread-safe version of a {@code DoubleVector} that guarantees
     * atomic access to all operations.  The returned vector allows concurrent
     * read access.
     *
     * @param vector The {@code DoubleVector} to decorate as atomic.
     * @return An atomic version of {@code vector}.
     */
    public static DoubleVector atomic(DoubleVector vector) {
        if (vector == null)
            throw new NullPointerException("Cannot create an atomic " +
                                           "null vector");
        return new AtomicVector(vector);
    }

    /**
     * Returns a syncrhonized view of a given {@code DoubleVector}.  This may
     * show slightly better performance than using an {@code AtomicVector} in
     * some use cases.
     *
     * @param vector The {@code DoubleVector} to decorate as synchronized.
     * @return An atomic version of {@code vector}.
     */
    public static DoubleVector synchronizedVector(DoubleVector vector) {
        if (vector == null)
            throw new NullPointerException("Cannot create an synchronized " +
                                           "null vector");
        return new SynchronizedVector(vector);
    }

    public static DoubleVector scaleByMagnitude(DoubleVector vector) {
        if (vector instanceof SparseDoubleVector)
            return scaleByMagnitude((SparseDoubleVector) vector);
        return new ScaledDoubleVector(vector, 1d/vector.magnitude());
    }

    public static DoubleVector scaleByMagnitude(SparseDoubleVector vector) {
        return new ScaledSparseDoubleVector(vector, 1d/vector.magnitude());
    }

    /**
     * Returns a subview for the given {@code DoubleVector} with a specified
     * offset and length.
     *
     * @param vector the {@code Vector} whose values will be shown in the view
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     *
     * @throws IllegalArgumentException if <ul><li>{@code offset} is
     *         negative<li>{@code length} is less than zero<li>the sum of {@code
     *         offset} plus {@code length} is greater than the length of {@code
     *         vector}</ul>
     */
    public static DoubleVector subview(DoubleVector vector, int offset,
                                       int length) {
        if (vector == null)
            throw new NullPointerException("Cannot create view of a " +
                                           "null vector");
        return new DoubleVectorView(vector, offset, length);
    }

    /**
     * Returns a subview for the given {@code SparseDoubleVector} with a
     * specified offset and length.
     *
     * @param vector the {@code Vector} whose values will be shown in the view
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     *
     * @throws IllegalArgumentException if <ul><li>{@code offset} is
     *         negative<li>{@code length} is less than zero<li>the sum of {@code
     *         offset} plus {@code length} is greater than the length of {@code
     *         vector}</ul>
     */
    public static SparseDoubleVector subview(SparseDoubleVector vector,
                                             int offset,
                                             int length) {
        if (vector == null)
            throw new NullPointerException("Cannot create view of a " +
                                           "null vector");
        return new ViewDoubleAsDoubleSparseVector(vector, offset, length);
    }

    /**
     * Returns a subview for the given {@code IntegerVector} with a specified
     * offset and length.
     *
     * @param vector the {@code Vector} whose values will be shown in the view
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     *
     * @throws IllegalArgumentException if <ul><li>{@code offset} is
     *         negative<li>{@code length} is less than zero<li>the sum of {@code
     *         offset} plus {@code length} is greater than the length of {@code
     *         vector}</ul>
     */
    public static IntegerVector subview(IntegerVector vector, int offset,
                                       int length) {
        if (vector == null)
            throw new NullPointerException("Cannot create view of a " +
                                           "null vector");
        return new IntegerVectorView(vector, offset, length);
    }

    /**
     * Returns a subview for the given {@code IntegerVector} with a specified
     * offset and length.
     *
     * @param vector the {@code Vector} whose values will be shown in the view
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     *
     * @throws IllegalArgumentException if <ul><li>{@code offset} is
     *         negative<li>{@code length} is less than zero<li>the sum of {@code
     *         offset} plus {@code length} is greater than the length of {@code
     *         vector}</ul>
     */
    public static SparseIntegerVector subview(SparseIntegerVector vector, 
                                              int offset, int length) {
        if (vector == null)
            throw new NullPointerException("Cannot create view of a " +
                                           "null vector");
        return new SparseIntegerVectorView(vector, offset, length);
    }

    /**
     * Copies all of the values from one {@code Vector} into another.  After the
     * operation, all of the values in {@code dest} will be the same as that of
     * {@code source}.  The legnth of {@code dest} must be as long as the length
     * of {@code source}.  Once completed {@code dest} is returned.
     *
     * @param dest The {@code Vector} to copy values into.
     * @param source The {@code Vector} to copy values from.
     * 
     * @return {@code dest} after being copied from {@code source}.
     *
     * @throws IllegalArgumentException if the length of {@code dest} is less
     *                                  than that of {@code source}.
     */
    public static Vector copy(Vector dest, Vector source) {
        for (int i = 0; i < source.length(); ++i)
            dest.set(i, source.getValue(i).doubleValue());
        return dest;
    }

    /**
     * Creates a copy of a given {@code DoubleVector} with the same type as the
     * original.
     *
     * @param source The {@code Vector} to copy.
     *
     * @return A copy of {@code source} with the same type.
     */
    public static DoubleVector copyOf(DoubleVector source) {
        if (source == null)
            return null;
        DoubleVector result = null;

        if (source instanceof SparseDoubleVector) {
            result = new CompactSparseVector(source.length());
            copyFromSparseVector(result, source);
        } else if (source instanceof DenseVector ||
                   source instanceof ScaledDoubleVector) {
            result = new DenseVector(source.length());
            for (int i = 0; i < source.length(); ++i)
                result.set(i, source.get(i));
        } else if (source instanceof AmortizedSparseVector) {
            result = new AmortizedSparseVector(source.length());
            copyFromSparseVector(result, source);
        } else if (source instanceof DoubleVectorView) {
            DoubleVectorView view = (DoubleVectorView) source;
            return copyOf(view.getOriginalVector());
        } else {
            // Create a copy of the given class using reflection.  This code
            // assumes that the given implemenation of Vector has a constructor
            // which accepts another Vector.
            try {
                Class<? extends DoubleVector> sourceClazz = source.getClass();
                Constructor<? extends DoubleVector> constructor =
                    sourceClazz.getConstructor(DoubleVector.class);
                result = (DoubleVector) constructor.newInstance(source);
            } catch (Exception e) {
                throw new Error(e);
            }
        }
        return result;
    }

    /**
     * Creates a copy of a given {@code Vector}.
     *
     * @param source The {@code Vector} to copy.
     *
     * @return A copy of {@code source} with the same type.
     */
    public static Vector copyOf(Vector source) {
        if (source instanceof DoubleVector)
            return copyOf((DoubleVector) source);
        if (source instanceof IntegerVector)
            return copyOf((IntegerVector) source);

        Vector result = new DenseVector(source.length());
        for (int i = 0; i < source.length(); ++i) 
            result.set(i, source.getValue(i));
        return result;
    }

    /**
     * Creates a copy of a given {@code IntegerVector} with the same type as the
     * original.
     *
     * @param source The {@code Vector} to copy.
     *
     * @return A copy of {@code source} with the same type.
     */
    public static IntegerVector copyOf(IntegerVector source) {
        IntegerVector result = null;

        if (source instanceof TernaryVector) {
            TernaryVector v = (TernaryVector) source;
            int[] pos = v.positiveDimensions();
            int[] neg = v.negativeDimensions();
            result = new TernaryVector(source.length(),
                                       Arrays.copyOf(pos, pos.length),
                                       Arrays.copyOf(neg, neg.length));
        } else if (source instanceof SparseVector) {
            result = new SparseHashIntegerVector(source.length());
            copyFromSparseVector(result, source);
        } else {
            result = new DenseIntVector(source.length());
            for (int i = 0; i < source.length(); ++i)
                result.set(i, source.get(i));
        }
        return result;
    }

    /**
     * Creates a {@code Vector} instance of the same type and length of the
     * provided vector.
     *
     * @param vector a vector whose type and length should be used when creating
     *        a new vector with the same properties
     *
     * @return a vector with the same type and length as the provided vector
     *
     * @throw IllegalArgumentException if <ul><li>the class of the provided
     *        vector does not have a constructor that takes in an {@code int} to
     *        specify the length <li>the class of the provided vector cannot be
     *        instantiated</ul>
     */
    @SuppressWarnings("unchecked")
    public static <T extends Vector> T instanceOf(T vector) {
        // Check for known vector types to avoid reflection overhead
        if (vector instanceof CompactSparseIntegerVector) {
            return (T)(new CompactSparseIntegerVector(vector.length()));
        }
        // Remaining cases of vector types is being left unfinished until the
        // vector name refactoring is finished.  -jurgens 12/7/09
        else {
            try {
                Class<T> clazz = (Class<T>)vector.getClass();
                Constructor<T> c = clazz.getConstructor(Integer.TYPE);
                T copy = c.newInstance(new Object[] { vector.length() });
                return copy;
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot instantiate a vector"
                    + " of type " + vector.getClass(), e);
            }
        }
    }

    /**
     * Copies values from a {@code SparseVector} into another vector
     *
     * @param destination The {@code Vector to copy values into.
     * @param source The {@code @SparseVector} to copy values from.
     */
    private static void copyFromSparseVector(DoubleVector destination,
                                             DoubleVector source) {
        int[] nonZeroIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : nonZeroIndices) 
            destination.set(index, source.get(index));
    }

    /**
     * Copies values from a {@code SparseVector} into another vector
     *
     * @param destination The {@code Vector to copy values into.
     * @param source The {@code @SparseVector} to copy values from.
     */
    private static void copyFromSparseVector(IntegerVector destination,
                                             IntegerVector source) {
        int[] nonZeroIndices = ((SparseVector) source).getNonZeroIndices();
        for (int index : nonZeroIndices) 
            destination.set(index, source.get(index));
    }
}
