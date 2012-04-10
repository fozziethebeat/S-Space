/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * An implementation of a strict partitioning over a dataset.  A {@link
 * Partition} may include multiple groupings of data points, but each data point
 * must occur in a single group.  Furthermore, a {@link Partition} may be
 * incomplete, in that not all data points are assigned to a group.  
 *
 * @author Keith Stevens
 */
public class Partition {

    /**
     * An array recording the cluster id for each element in a particular
     * partition.
     */
    private final int[] assignments;

    /**
     * Sets of elements within each partition.
     */
    private final List<Set<Integer>> clusters;

    /**
     * Creates a new {@link Partition} over data points.  
     *
     * @param clusters Sets of elements that are co-clustered together
     * @param assignments An index from element identifiers to their cluster
     *        identifiers.
     */
    private Partition(List<Set<Integer>> clusters, int[] assignments) {
        this.clusters = clusters;
        this.assignments = assignments;
    }

    /**
     * A private helper method to quickly compute n choose two.
     */
    private static int chooseTwo(int n) {
        return n * (n-1) / 2;
    }

    /**
     * Returns the number of co-clustered data points in this {@link Partition}.
     * This runs in O(num_clusters).
     */
    public int numPairs() {
        int numPairs = 0;
        for (Set<Integer> cluster : clusters)
            numPairs += chooseTwo(cluster.size());
        return numPairs;
    }

    /**
     * Returns true if elements {@code i} and {@code j} are co-clustered.
     */
    public boolean coClustered(int i, int j) {
        return assignments[i] == assignments[j];
    }

    /**
     * Returns true if {@code element} was moved to {@code newCluster}.  This
     * only returns false if {@code newCluster} equals {@code element}s current
     * cluster identifier or the new cluster identifier is out of range.
     */
    public boolean move(int element, int newCluster) {
        int oldCluster = assignments[element];
        if (oldCluster == newCluster || 
            newCluster < 0 || newCluster >= clusters.size())
            return false;
        assignments[element] = newCluster;
        clusters.get(oldCluster).remove(element);
        clusters.get(newCluster).add(element);
        return true;
    }

    /**
     * Returns access to the set of co-clustered pairs.
     */
    public List<Set<Integer>> clusters() {
        return clusters;
    }

    /**
     * Returns the number of partitions or clusters stored.
     */
    public int numClusters() {
        return clusters.size();
    }

    /**
     * Returns the indexing from element identifiers to their cluster
     * identifiers.
     */
    public int[] assignments() {
        return assignments;
    }

    /**
     * Returns the number of data points partitioned.
     */
    public int numPoints() {
        return assignments.length;
    }

    /**
     * Creates a new {@link Partition} using sets of co-clustered data points.
     */
    public Partition(List<Set<Integer>> clusters) {
        this.clusters = clusters;
        int numPoints = 0;
        for (Set<Integer> cluster : clusters)
            for (int point : cluster)
                numPoints = Math.max(numPoints, point);
        numPoints++;
        this.assignments = new int[numPoints];
        Arrays.fill(assignments, -1);
        int clusterId = 0;
        for (Set<Integer> cluster : clusters) {
            for (int point : cluster)
                assignments[point] = clusterId;
            clusterId++;
        }
    }

    /**
     * Returns a new {@link Partition} read from a formatted partition file.
     */
    public static Partition read(String partitionFile) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                        partitionFile));

            String line = br.readLine();
            String[] sizes = line.split("\\s+");
            int numElements = Integer.parseInt(sizes[0]);
            int numClusters = Integer.parseInt(sizes[1]);

            List<Set<Integer>> clusters = new ArrayList<Set<Integer>>();
            int[] assignments = new int[numElements];
            Arrays.fill(assignments, -1);

            int clusterId = 0;
            for (line = null; (line = br.readLine()) != null; ) {
                if (line.trim().equals(""))
                    continue;
                Set<Integer> cluster = new HashSet<Integer>();
                for (String point : line.split("\\s+")) {
                    int id = Integer.parseInt(point);
                    cluster.add(id);
                    assignments[id] = clusterId;
                }
                clusters.add(cluster);
                clusterId++;
            }

            return new Partition(clusters, assignments);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns a copy of the given {@link Partition}.
     */
    public static Partition copyOf(Partition partition) {
        List<Set<Integer>> newClusters = new ArrayList<Set<Integer>>();
        for (Set<Integer> cluster : partition.clusters())
            newClusters.add(new HashSet<Integer>(cluster));
        int[] newAssignments = Arrays.copyOf(partition.assignments(), partition.assignments().length);
        return new Partition(newClusters, newAssignments);
    }

    /**
     * Returns a new {@link Partition} using the same assignments as in the
     * given {@link Assignments} object.
     */
    public static Partition fromAssignments(Assignments assignments) {
        int[] newAssignments = new int[assignments.size()]; 
        List<Set<Integer>> newClusters = new ArrayList<Set<Integer>>();
        for (int c = 0; c < assignments.numClusters(); ++c)
            newClusters.add(new HashSet<Integer>());

        for (int i = 0; i < newAssignments.length; ++i) {
            int cid = assignments.get(i);
            newAssignments[i] = cid;
            newClusters.get(cid).add(i);
        }

        return new Partition(newClusters, newAssignments);
    }
}
