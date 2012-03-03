/*
 * Copyright 2011 David Jurgens 
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

package edu.ucla.sspace.graph;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.util.MultiMap;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.CompactSparseVector;

import java.util.Properties;
import java.util.Set;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 *
 */
public class WeightedLinkClustering extends LinkClustering 
        implements java.io.Serializable {
    
    private static final long serialVersionUID = 1L;

    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.graph.WeightedLinkClustering";
    
    public static final String KEEP_WEIGHT_VECTORS_PROPERTY
        = PROPERTY_PREFIX + ".keepWeightVectors";

    private final TIntObjectMap<SparseDoubleVector> vertexToWeightVector;
    
    private boolean keepWeightVectors;

    public WeightedLinkClustering() {
        vertexToWeightVector = new TIntObjectHashMap<SparseDoubleVector>();
        keepWeightVectors = true;
    }

    /**
     * Computes the similarity of the graph's edges and merges them until the
     * specified number of clusters has been reached.
     *
     * @param numClusters the number of clusters to return
     */
    public <E extends WeightedEdge> MultiMap<Integer,Integer> cluster(
              final WeightedGraph<E> graph, int numClusters, Properties props) {
        if (props.getProperty(KEEP_WEIGHT_VECTORS_PROPERTY) != null)
            keepWeightVectors = Boolean.parseBoolean(
                props.getProperty(KEEP_WEIGHT_VECTORS_PROPERTY));
        vertexToWeightVector.clear();
        return super.cluster(graph, numClusters, props);
    }

    /**
     * Computes the similarity of the graph's edges using their weights and
     * merges them to select the final partitioning that maximizes the overall
     * cluster density.
     */
    public <E extends WeightedEdge> MultiMap<Integer,Integer> cluster(
                      final WeightedGraph<E> graph, Properties props) {
        if (props.getProperty(KEEP_WEIGHT_VECTORS_PROPERTY) != null)
            keepWeightVectors = Boolean.parseBoolean(
                props.getProperty(KEEP_WEIGHT_VECTORS_PROPERTY));
        vertexToWeightVector.clear();
        return super.cluster(graph, props);
    }

    @Override protected <E extends Edge> double getConnectionSimilarity(
                         Graph<E> graph, int keystone, int impost1, int impost2) {
        @SuppressWarnings("unchecked")
        WeightedGraph<WeightedEdge> wg = (WeightedGraph<WeightedEdge>)graph;
        // Use the extended Tanimoto coefficient to compute the similarity of
        // two edges on the basis of the impost nodes' weight vectors.
        return Similarity.tanimotoCoefficient(
            getVertexWeightVector(wg, impost1),
            getVertexWeightVector(wg, impost2));
    }


    /**
     * Returns the normalized weight vector for the specified row, to be used in
     * edge comparisons.  The weight vector is normalized by the number of edges
     * from the row with positive weights and includes a weight for the row to
     * itself, which reflects the similarity of the keystone nod.
     */ 
    private <E extends WeightedEdge> SparseDoubleVector getVertexWeightVector(
            WeightedGraph<E> g, int vertex) {
        if (keepWeightVectors) {
            SparseDoubleVector weightVec = vertexToWeightVector.get(vertex);
            if (weightVec == null) {
                synchronized(this) {
                    weightVec = vertexToWeightVector.get(vertex);
                    if (weightVec == null) {
                        weightVec = computeWeightVector(g, vertex);
                        vertexToWeightVector.put(vertex, weightVec);
                    }
                }
            }
            return weightVec;
        }
        else
            return computeWeightVector(g, vertex);
    }

    private <E extends WeightedEdge> SparseDoubleVector computeWeightVector(
            WeightedGraph<E> g, int vertex) {

        SparseDoubleVector weightVec = new CompactSparseVector();//  g.order());
        Set<E> adjacent = g.getAdjacencyList(vertex);
        
        // Count how many neighbors have positive edge weights
        // (assume for now that all edges are weighted positive)
        double normalizer = 1d / adjacent.size();
        
        // For each of the neighbors, normalize the positive edge
        // weights by the number of neighbors (with pos. weights)
        for (E e : adjacent) {
            int v = (e.from() == vertex) ? e.to() : e.from();
            weightVec.set(v, normalizer * e.weight());
        }                    
        
        // Last, although the graph is assumed to not have self-loops, the
        // weight for an node to itself is the normalizing constant (1/num
        // positive weights).  This is analogous to the similarity contribution
        // from the keystone node in the unweighted version
        weightVec.set(vertex, normalizer);
        return weightVec;
    }
}