/*
 * Copyright 2010 David Jurgens
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

// javadoc includes
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;
import edu.ucla.sspace.common.Similarity.SimType;

/**
 * A status object that represents the result of agglomeratively merging two
 * clusters.  This class provides the information on which clusters were merged,
 * what the id of the remaining cluster is, and the similarity of the two
 * clusters at the point at which they were merged.
 *
 * @see HierarchicalAgglomerativeClustering#buildDendogram(Matrix,edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage,edu.ucla.sspace.common.Similarity.SimType)
 */
public class Merge implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final int remainingCluster;
    
    private final int mergedCluster;
    
    private final double similarity;

    public Merge(int remainingCluster, int mergedCluster, double similarity) {
        this.remainingCluster = remainingCluster;
        this.mergedCluster = mergedCluster;
        this.similarity = similarity;
    }

    public boolean equals(Object o) {
        if (o instanceof Merge) {
            Merge m = (Merge)o;
            return m.remainingCluster == remainingCluster
                && m.mergedCluster == mergedCluster
                && m.similarity == similarity;
        }
        return false;
    }

    public int hashCode() {
        return remainingCluster ^ mergedCluster;
    }

    /**
     * Returns the ID of the cluster that was merged into another cluster.  
     */
    public int mergedCluster() {
        return mergedCluster;
    }

    /**
     * Returns the ID of the clusters into which another cluster was merged,
     * i.e. all the data points in the merged cluster would now have this ID.
     */
    public int remainingCluster() {
        return remainingCluster;
    }

    /**
     * Returns the similarity of the two clusters at the time of their merging.
     */
    public double similarity() {
        return similarity;
    }

    public String toString() {
        return "(" + mergedCluster + " -> " + remainingCluster + ": "
            + similarity + ")";
    }
}
