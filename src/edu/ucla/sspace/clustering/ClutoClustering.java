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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.SparseMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A class for interacting with the <a
 * href="http://glaros.dtc.umn.edu/gkhome/cluto/cluto/overview">CLUTO</a>
 * clustering library.
 *
 * @author David Jurgens
 */
public class ClutoClustering implements Clustering {

    /**
     * A property prefix for specifiying options when using Cluto.
     */
    public static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.clustering.ClutoClustering";

    /**
     * The property to set the name of a {@link ClutoClustering.Method} that
     * Cluto should use in clustering the data.
     */
    public static final String CLUSTER_METHOD = 
        PROPERTY_PREFIX + ".clusterSimilarity";

    /**
     * The property to set the name of a {@link ClutoClustering.Criterion} that
     * Cluto should use in clustering the data.
     */
    public static final String CLUSTER_CRITERION = 
        PROPERTY_PREFIX + ".clusterCriterion";

    /**
     * The method by which CLUTO should cluster the data points
     */
    public enum Method {
        
        REPEATED_BISECTIONS_REPEATED("rbr"),
        KMEANS("direct"),
        AGGLOMERATIVE("agglo"),
        NEAREST_NEIGHBOOR("graph"),
        BAGGLO("bagglo");       

        /**
         * The string abbreviation for each clustering method
         */
        private final String name;

        Method(String name) {
            this.name = name;
        }

        /**
         * Returns the name for this method that CLUTO uses on the command line.
         */
        String getClutoName() {
            return name;
        }
    }

    /**
     * The crition function by which CLUTO should evaluate the clustering
     * assignment.
     */
    public enum Criterion  {
        
        I1("i1"),
        I2("i2"),
        E1("e1"),
        G1("g1"),
        G1P("g1p"),
        H1("h1"),
        H2("h2"),
        SLINK("slink"),
        WSLINK("wslink"),
        CLINK("clink"),
        WCLINK("wclink"),
        UPGMA("upgma"),
        WUPGMA("wupgma");

        /**
         * The string abbreviation for each clustering method
         */
        private final String name;

        Criterion(String name) {
            this.name = name;
        }

        /**
         * Returns the name for this method that CLUTO uses on the command line.
         */
        String getClutoName() {
            return name;
        }
    }

    /**
     * The default clustering method to be used by Cluto.
     */
    private static Method DEFAULT_CLUSTER_METHOD = Method.AGGLOMERATIVE;

    private static Criterion DEFAULT_CRITERION = Criterion.UPGMA; 

    /**
     * A logger to track the status of Cluto.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(ClutoClustering.class.getName());

    /**
     * Creates a new {@code ClutoClustering} instance.
     */
    public ClutoClustering() { }

    /**
     * Throws an {@link UnsupportedOperationException} if called, as CLUTO
     * requires the number of clusters to be specified.
     */
    public Assignment[] cluster(Matrix matrix, Properties properties) {
        throw new UnsupportedOperationException(
            "CLUTO requires the number of clusters to be specified and " +
            "therefore cannot be invoked using this method.");
    }

    /**
     * {@inheritDoc}
     *
     * @param properties the properties to use for clustering with CLUTO.  See
     *        {@link ClutoClustering} for the list of supported properties.
     */
    public Assignment[] cluster(Matrix matrix, int numClusters, 
                                Properties properties) {
        Method clmethod = DEFAULT_CLUSTER_METHOD;
        String methodProp = properties.getProperty(CLUSTER_METHOD);
        if (methodProp != null) 
            clmethod = Method.valueOf(methodProp);
        Criterion criterion = DEFAULT_CRITERION;
        String criterionProp = properties.getProperty(CLUSTER_CRITERION);
        if (criterionProp != null)
            criterion = Criterion.valueOf(criterionProp);
        return cluster(matrix, numClusters, clmethod, criterion);
    }

    /**
     * Clusters the set of rows in the given {@code Matrix} into a specified
     * number of clusters using the specified CLUTO clustering method.
     *
     * @param matrix the {@link Matrix} containing data points to cluster
     * @param numClusters the number of clusters to generate
     * @param clusterMethod the method by which cluto should cluster the rows
     *
     * @return an array of {@link Assignment} instances that indicate zero or
     *         more clusters to which each row belongs.
     */
    public Assignment[] cluster(Matrix matrix, int numClusters, 
                                Method clusterMethod,
                                Criterion criterionMethod) {
        try {
            String clmethod = clusterMethod.getClutoName();
            String crtmethod = criterionMethod.getClutoName();
            return ClutoWrapper.cluster(matrix, clmethod,
                                        crtmethod, numClusters);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
