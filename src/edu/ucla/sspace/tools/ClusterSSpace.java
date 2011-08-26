package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StaticSemanticSpace;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Assignments;
import edu.ucla.sspace.clustering.Clustering;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.Vectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class ClusterSSpace {
    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('s', "sspace",
                          "The semantic space to be clustered",
                          true, "FILE", "Required");
        options.addOption('a', "clusteringAlgorithm",
                          "The clustering algorithm to use",
                          true, "CLASSNAME", "Required");
        options.addOption('c', "numClusters",
                          "The number of clusters to use",
                          true, "INT", "Optional");
        options.parseOptions(args);

        if (!options.hasOption('a') ||
            !options.hasOption('s')) {
            System.out.println("Usage: ClusterSSpace\n" +
                               options.prettyPrint());
            System.exit(1);
        }

        Clustering clustering = ReflectionUtil.getObjectInstance(
                options.getStringOption('a'));
        SemanticSpace sspace = new StaticSemanticSpace(
                options.getStringOption('s'));
        int numClusters = options.getIntOption('c', 0);

        Set<String> words = sspace.getWords();
        List<DoubleVector> vectors = new ArrayList<DoubleVector>();
        for (String word : words) {
            if (sspace.getVector(word) instanceof ScaledDoubleVector)
                System.out.println("whaaat the fuuuu");
            vectors.add(Vectors.asDouble(sspace.getVector(word)));
        }
        Matrix matrix = Matrices.asMatrix(vectors);
        Properties props = System.getProperties();
        Assignments assignments = (numClusters > 0) 
            ? clustering.cluster(matrix, numClusters, props)
            : clustering.cluster(matrix, props);

        int a = 0;
        for (String word : words) {
            Assignment assignment = assignments.get(a++);
            System.out.printf("%s ", word);
            for (int i : assignment.assignments())
                System.out.printf("%d ", i);
            System.out.println();
        }
    }
}
