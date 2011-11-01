package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;


/**
 * @author Keith Stevens
 */
public class ScalarMatrix implements Matrix {

    private double scalar;

    private int rows;

    private int columns;

    public ScalarMatrix(int rows, int columns, double scalar) {
        this.rows = rows;
        this.columns = columns;
        this.scalar = scalar;
    }

    public double get(int row, int col) {
        return scalar;
    }

    public double[] getColumn(int column) {
        double[] values = new double[rows];
        for (int r = 0; r < rows; ++r)
            values[r] = scalar;
        return values;
    }

    public DoubleVector getColumnVector(int column) {
        return new DenseVector(getColumn(column));
    }

    public double[] getRow(int row) {
        double[] values = new double[columns];
        for (int c = 0; c < columns; ++c)
            values[c] = scalar;
        return values;
    }

    public DoubleVector getRowVector(int row) {
        return new DenseVector(getRow(row));
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    public double[][] toDenseArray() {
        double[][] values = new double[rows][];
        for (int r = 0; r < rows; ++r) {
            values[r] = new double[columns];
            for (int c = 0; c < columns; ++c)
                values[r][c] = scalar;
        }
        return values;
    }

    public void set(int row, int col, double val) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }

    public void setColumn(int col, double[] values) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }

    public void setColumn(int col, DoubleVector values) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }

    public void setRow(int row, double[] values) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }

    public void setRow(int row, DoubleVector values) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }
}
