package edu.ucla.sspace.tools;

import edu.ucla.sspace.clustering.Assignments;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.util.ReflectionUtil;

import java.io.File;


/**
 * Clusters a data matrix using the specified algorithm with with requested
 * number of clusters.
 *
 * Arguments are:
 * <ol>
 *   <li>matrixFile</li>
 *   <li>file Format</li>
 *   <li>clustering algorithm</li>
 *   <li>number of clusters</li>
 * </ol>
 *
 * Cluster assignments will be reported to standard out.
 *
 * @author Keith Stevens
 */
public class ClusterMatrix {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("usage: Java ClusterMatrix <matrixFile> <format> <clusterAlg> <numClusters>");
            System.exit(1);
        }

        File matrixFile = new File(args[0]);
        Format format = Format.valueOf(args[1]);
        Matrix matrix = MatrixIO.readMatrix(matrixFile, format);
        Clustering alg = ReflectionUtil.getObjectInstance(args[2]);
        int numClusters = Integer.parseInt(args[3]);
        Assignments as = alg.cluster(matrix, numClusters, System.getProperties());
        for (int[] clusterId : as.assignments())
            System.out.println(clusterId[0]);
    }
}
