package edu.ucla.sspace.matrix;


/**
 * An interface for any method that aggregates the values in a matrix into a
 * single value.
 *
 * @author Keith Stevens
 */
public interface MatrixAggregate {

    double aggregate(Matrix m);
}
