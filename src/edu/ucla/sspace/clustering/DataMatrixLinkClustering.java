package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.AffinityMatrixCreator;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.NearestNeighborAffinityMatrixCreator;

import edu.ucla.sspace.similarity.CosineSimilarity;
import edu.ucla.sspace.similarity.SimilarityFunction;

import java.util.Properties;


/**
 * @author Keith Stevens
 */
public class DataMatrixLinkClustering implements Clustering {

    private final AffinityMatrixCreator creator;

    private final LinkClustering linkCluster;

    public DataMatrixLinkClustering() {
        this(createDefaultAffinityMatrixCreator());
    }

    public DataMatrixLinkClustering(AffinityMatrixCreator creator) {
        this.creator = creator;
        this.linkCluster = new LinkClustering(); 
    }

    public static AffinityMatrixCreator createDefaultAffinityMatrixCreator() {
        SimilarityFunction simFunc = new CosineSimilarity();
        AffinityMatrixCreator creator =
            new NearestNeighborAffinityMatrixCreator();
        creator.setParams(10);
        creator.setFunctions(simFunc, simFunc);
        return creator;
    }

    public Assignments cluster(Matrix matrix, 
                               int numClusters,
                               Properties props) {
        MatrixFile affinityMatrix = creator.calculate(matrix);
        return linkCluster.cluster(affinityMatrix.load(), numClusters, props);
    }

    public Assignments cluster(Matrix matrix, Properties props) { 
        MatrixFile affinityMatrix = creator.calculate(matrix);
        return linkCluster.cluster(affinityMatrix.load(), props);
    }
}
