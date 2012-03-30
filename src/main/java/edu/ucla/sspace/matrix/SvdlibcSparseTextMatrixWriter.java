package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class SvdlibcSparseTextMatrixWriter implements MatrixWriter {

    public void writeMatrix(Matrix m, OutputStream s) {
        if (m instanceof SparseMatrix) {
            writeMatrix((SparseMatrix) m, s);
            return;
        }

        PrintStream pw = new PrintStream(s);

        // count the number of non-zero values for each column as well as the
        // total
        int nonZero = 0;
        int[] nonZeroPerCol = new int[m.columns()];
        for (int i = 0; i < m.rows(); ++i) {
            for (int j = 0; j < m.columns(); ++j) {
                if (m.get(i, j) != 0d) {
                    nonZero++;
                    nonZeroPerCol[j]++;
                }
            }
        }

        // loop through the matrix a second time, printing out the number of
        // non-zero values for each column, followed by those values and their
        // associated row
        pw.println(m.rows() + " " + m.columns() + " " + nonZero);
        for (int col = 0; col < m.columns(); ++col) {
            pw.println(nonZeroPerCol[col]);
            if (nonZeroPerCol[col] > 0) {
                for (int row = 0; row < m.rows(); ++row) {
                    double val = m.get(row, col);
                    if (val != 0d) {
                        // SVDLIBC requires floats.
                        pw.printf("%d %f\n", 
                                  row, Double.valueOf(val).floatValue());
                    }
                }
            }
        }

        pw.flush();
        pw.close();
    }

    public void writeMatrix(SparseMatrix m, OutputStream s) {
        // Create a list that stores the rows and values within each column.
        List<List<String>> colOrder = new ArrayList<List<String>>();
        for (int col = 0; col < m.columns(); ++col)
            colOrder.add(new ArrayList<String>());

        int nonZeroCount = 0;
        // File the column ordered lists for each column by adding a line for
        // every non zero value in the matrix.
        for (int r = 0; r < m.rows(); ++r) {
            SparseDoubleVector sv = m.getRowVector(r);
            int[] nonZeros = sv.getNonZeroIndices();
            nonZeroCount += nonZeros.length;
            for (int c : nonZeros)
                colOrder.get(c).add(String.format(
                            "%d %f",r,Double.valueOf(sv.get(c)).floatValue()));
        }

        PrintStream pw = new PrintStream(s);
        // Print out the header for the SVDLIBC format.
        pw.printf("%d %d %d\n", m.rows(), m.columns(), nonZeroCount);
        // Print out each column.  This requires printing out the number of rows
        // that are non-zero in each column and then printing out each row index
        // and matching cell value.
        for (List<String> column : colOrder) {
            pw.println(column.size());
            // Sort the row entries.
            Collections.sort(column);
            // Print each row entry.
            for (String rowValue : column)
                pw.println(rowValue);
            pw.flush();
        }
        pw.close();
    }
}
