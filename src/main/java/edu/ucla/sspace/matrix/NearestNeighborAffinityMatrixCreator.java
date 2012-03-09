package edu.ucla.sspace.matrix;

import edu.ucla.sspace.similarity.SimilarityFunction;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.Duple;
import edu.ucla.sspace.util.MultiMap;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Logger;


/**
 * This {@link AffinityMatrixCreator} adds an edge added between two data
 * points, i and j, if j is in the <i>k</i> nearest neighbors of i.  This
 * relationship is not symmetric.
 *
 * @author David Jurgens
 * @author Keith Stevens
 */
public class NearestNeighborAffinityMatrixCreator
        implements AffinityMatrixCreator {

    private static final Logger LOG =
        Logger.getLogger(MinSimilarityAffinityMatrixCreator.class.getName());

    private SimilarityFunction edgeSim;

    private SimilarityFunction kernelSim;

    private int numNearestNeighbors;

    public void setParams(double... params) {
        this.numNearestNeighbors = (int) params[0];
    }

    /** 
     * {@inheritDoc}
     */
    public void setFunctions(SimilarityFunction edgeSim,
                             SimilarityFunction kernelSim) {
        this.edgeSim = edgeSim;
        this.kernelSim = kernelSim;
    }

    /** 
     * {@inheritDoc}
     */
    public MatrixFile calculate(Matrix input) {
        try {
            File affMatrixFile = 
                File.createTempFile("affinty-matrix",".dat");
            PrintWriter affMatrixWriter = new PrintWriter(affMatrixFile);
            
            int rows = input.rows();

            LOG.fine("Calculating the affinity matrix");
            RowComparator rc = new RowComparator();
            for (int i = 0; i < rows; ++i) {
                LOG.fine("computing affinity for row " + i);
                MultiMap<Double,Integer> neighborMap = rc.getMostSimilar(
                        input, i, numNearestNeighbors, edgeSim);

                DoubleVector row = input.getRowVector(i);
                for (int n : neighborMap.values()) {
                    double edgeWeight = kernelSim.sim(
                            row, input.getRowVector(n));
                    affMatrixWriter.printf("%d %d %f\n", i+1 ,n+1, edgeWeight);
                }
            }

            // Finish writing the affinity matrix so it can be sent to matlab
            affMatrixWriter.close();    
            return new MatrixFile(affMatrixFile, MatrixIO.Format.MATLAB_SPARSE);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public MatrixFile calculate(MatrixFile input) {
        return calculate(input, false);
    }

    /**
     * {@inheritDoc}
     */
    public MatrixFile calculate(MatrixFile input, boolean useColumns) {
        File matrixFile = input.getFile();
        MatrixIO.Format format = input.getFormat();

        // IMPLEMENTATION NOTE: since the user has requested the matrix be dealt
        // with as a file, we need to keep the matrix on disk.  However, the
        // input matrix format may not be conducive to efficiently comparing
        // rows with each other (e.g. MATLAB_SPARSE is inefficient), so convert
        // the matrix to a better format.
        try {
            LOG.fine("Converting input matrix to new format for faster " +
                        "calculation of the affinity matrix");

            // Keep the matrix on disk, but convert it to a transposed SVDLIBC
            // sparse binary, which allows for easier efficient row-by-row
            // comparisons (which are really columns).  Note that if the data is
            // already in this format, the conversion is a no-op.
            //
            // NOTE: the !useColumns is used for the transpose because if we
            // want to use the rows, we need the data transposed to begin with
            // since the SVDLIBC sparse binary will give us column information
            // to start with
            File converted = MatrixIO.convertFormat(
                    matrixFile, format, 
                    MatrixIO.Format.SVDLIBC_SPARSE_BINARY, !useColumns);

            LOG.fine("Calculating the affinity matrix");
            // Read off the matrix dimensions
            DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(converted)));
            // CRITICAL NOTE: because we are interpreting the columns as rows,
            // the dimensions are read in *reverse order* from how they are
            // stored in the file.
            int cols = dis.readInt();
            int rows = dis.readInt();
            dis.close();
            
            // Once we know the matrix dimensions, create an iterator over the
            // data, and repeatedly loop through the columns (which are really
            // rows in the original matrix) to create the affinity matrix.
            File affMatrixFile = File.createTempFile("affinity-matrix",".dat");
            PrintWriter affMatrixWriter = new PrintWriter(affMatrixFile);

            // Keep track of the first row and have a reference to the next row.
            // The nextRow reference avoid us having to advance into data
            // unnecessarily to retrieval the vector for processing to start
            SparseDoubleVector curRow = null;
            SparseDoubleVector nextRow = null;

            SvdlibcSparseBinaryFileRowIterator matrixIter = 
                new SvdlibcSparseBinaryFileRowIterator(converted);
            
            for (int row = 0; row < rows; ++row) {
                LOG.fine("computing affinity for row " + row);

                // The map is to the row and its weighted affinity value.  We
                // need to store the potential value at the time of the
                // similarity calculation because that is the only time the two
                // row vectors are in memory
                MultiMap<Double,Duple<Integer,Double>> neighbors = 
                    new BoundedSortedMultiMap<Double,Duple<Integer,Double>>(
                        numNearestNeighbors, false);

                // Loop through each of the rows, gathering the statistics
                // necessary to compute the affinity matrix.
                for (int other = 0; other < rows; ++other) {
                    //System.out.printf("cur: %d, other %d%n", row, other);
                    // Special case for the very first row
                    if (row == 0 && curRow == null) {
                        curRow = matrixIter.next();
                        continue;
                    }
                    
                    SparseDoubleVector otherRow = matrixIter.next();

                    // Save the row that will be used next so we have it to do
                    // comparisons with for earlier rows in the file
                    if (other == row + 1)
                        nextRow = otherRow;

                    // Determine if the current row and the other row should be
                    // linked in the affinity matrix.  For code simplicity, both
                    // the k-nearest neighbors and the similarity threshold code
                    // are supported within the I/O, with the caller specifying
                    // which to use.
                    double dataSimilarity = edgeSim.sim(curRow, otherRow);
                    double edgeWeight = kernelSim.sim(curRow, otherRow);

                    neighbors.put(dataSimilarity,
                                  new Duple<Integer,Double>(other, edgeWeight));
                }
                curRow = nextRow;
                matrixIter.reset();

                // If using k-nearest neighbors, once the row has been
                // processed, report all the k-nearest as being adjacent
                // Note that the two rows may not have a symmetric
                // connection so only one value needs to be written
                for (Duple<Integer,Double> t : neighbors.values())
                    affMatrixWriter.printf("%d %d %f\n", row+1, t.x+1, t.y);
            }

            // Finish writing the matrix
            affMatrixWriter.close();
            return new MatrixFile(affMatrixFile, MatrixIO.Format.MATLAB_SPARSE);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}

