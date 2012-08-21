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

package edu.ucla.sspace.graph.isomorphism;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ucla.sspace.graph.DirectedEdge;
import edu.ucla.sspace.graph.DirectedGraph;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.TypedEdge;

import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.ObjectCounter;
import edu.ucla.sspace.util.Pair;


/**
 * A {@link State} implementation for testing isomorphism using the VF2
 * algorithm's logic with type constraints on the edges.  Note that this
 * implementation requires that the graphs have contiguous vertex indices
 * (beginning at 0 to {@code g.order()}-1.
 *
 * <p>This implementation is an extension of the vf2_state implemenation in
 * VFLib.
 */
public class TypedVF2State implements State { 

    /**
     * The first graph being compared
     */
    private Graph<? extends Edge> g1;

    /**
     * The second graph being compared
     */
    private Graph<? extends Edge> g2;

    /**
     * The number of nodes currently being matched betwen g1 and g3
     */
    private int coreLen;

    /**
     * The number of nodes that were matched prior to this current pair being
     * added, which is used in backtracking.
     */
    private int origCoreLen;
    
    /**
     * The node in g1 that was most recently added.
     */
    private int addedNode1;
    
    // State information
    int t1bothLen, t2bothLen, t1inLen, t1outLen, 
        t2inLen, t2outLen; // Core nodes are also counted by these...
    
    int[] core1;
    int[] core2;
    int[] in1;
    int[] in2;
    int[] out1;
    int[] out2;
      
    int[] order;

    /**
     * The number of nodes in {@code g1}
     */
    private final int n1;

    /**
     * The number of nodes in {@code g2}
     */
    private final int n2;

    /**
     * Whether the algorithm needs to check for type constraints on the graph's
     * edges.  This is stored as a global to avoid recomputing it each time
     * {@code areEdgesCompatible} is called (a hot spot), when it is already
     * known at state construction time.
     */
    private final boolean checkMultiplexEdges;

    /**
     * Creates a new {@code TypedVF2State} with an empty mapping between the two
     * graphs.
     */
    public TypedVF2State(Graph<? extends Edge> g1, Graph<? extends Edge> g2) {
        this.g1 = g1;
        this.g2 = g2;
        this.checkMultiplexEdges = 
            g1 instanceof Multigraph || g2 instanceof Multigraph;
        
        n1 = g1.order();
        n2 = g2.order();

        order = null;

        coreLen = 0;
        origCoreLen = 0;
        t1bothLen = 0;
        t1inLen = 0;
        t1outLen = 0;
        t2bothLen = 0; 
        t2inLen = 0;
        t2outLen = 0;
        
	addedNode1 = NULL_NODE;

        core1 = new int[n1];
        core2 = new int[n2];
        in1 = new int[n1];
        in2 = new int[n2];
        out1 = new int[n1];
        out2 = new int[n2];
        
        Arrays.fill(core1, NULL_NODE);
        Arrays.fill(core2, NULL_NODE);
        
    }
    
    /**
     * Creates a new {@code TypedVF2State} with a copy of the provided state's
     * data.
     */
    protected TypedVF2State(TypedVF2State copy) {
        checkMultiplexEdges = copy.checkMultiplexEdges;
        g1 = copy.g1;
        g2 = copy.g2;
        coreLen = copy.coreLen;
        origCoreLen = copy.origCoreLen;
        t1bothLen = copy.t1bothLen;
        t2bothLen = copy.t2bothLen;
        t1inLen = copy.t1inLen;
        t2inLen = copy.t2inLen;
        t1outLen = copy.t1outLen;
        t2outLen = copy.t2outLen;
        n1 = copy.n1;
        n2 = copy.n2;

        addedNode1 = NULL_NODE;

        // NOTE: we don't need to copy these arrays because their state restored
        // via the backTrack() function after processing on the cloned state
        // finishes
        core1 = copy.core1;
        core2 = copy.core2;
        in1 = copy.in1;
        in2 = copy.in2;
        out1 = copy.out1;
        out2 = copy.out2;
        order = copy.order;        
    }
    

    /**
     * {@inheritDoc}
     */
    public Graph<? extends Edge> getGraph1() {
        return g1;
    }

    /**
     * {@inheritDoc}
     */
    public Graph<? extends Edge> getGraph2() {
        return g2;
    }

    /**
     * {@inheritDoc}
     */
    public Pair<Integer> nextPair(int prevN1, int prevN2) {
        if (prevN1 == NULL_NODE)
            prevN1 = 0;

        if (prevN2 == NULL_NODE)
            prevN2 = 0;
        else
            prevN2++;

	if (t1bothLen>coreLen && t2bothLen > coreLen) {
            while (prevN1 < n1 && (core1[prevN1] != NULL_NODE 
                                   || out1[prevN1]==0
                                   || in1[prevN1]==0)) {
                prevN1++;    
                prevN2 = 0;
            }
        }
        else if (t1outLen>coreLen && t2outLen>coreLen) {
            while (prevN1<n1 &&
                   (core1[prevN1]!=NULL_NODE || out1[prevN1]==0)) {
                prevN1++;    
                prevN2=0;
            }
        }
        else if (t1inLen>coreLen && t2inLen>coreLen) {
            while (prevN1<n1 &&
                   (core1[prevN1]!=NULL_NODE || in1[prevN1]==0)) {
                 prevN1++;    
                 prevN2=0;
            }
        }
	else if (prevN1 == 0 && order != null) {
            int i=0;
	    while (i < n1 && core1[prevN1=order[i]] != NULL_NODE)
                i++;
	    if (i == n1)
                prevN1=n1;
        }
	else {
            while (prevN1 < n1 && core1[prevN1] != NULL_NODE ) {
                prevN1++;    
                prevN2=0;
            }
        }

	if (t1bothLen>coreLen && t2bothLen>coreLen) {
            while (prevN2<n2 && (core2[prevN2]!=NULL_NODE 
                                 || out2[prevN2]==0
                                 || in2[prevN2]==0)) {
                prevN2++;    
            }
        }
	else if (t1outLen>coreLen && t2outLen>coreLen) {
            while (prevN2 < n2 && (core2[prevN2] != NULL_NODE
                                   || out2[prevN2] == 0)) {
                prevN2++;    
            }
        }
        else if (t1inLen>coreLen && t2inLen>coreLen) {
             while (prevN2 < n2 && (core2[prevN2] != NULL_NODE
                                    || in2[prevN2] == 0)) {
                  prevN2++;    
             }
        }
	else {
            while (prevN2 < n2 && core2[prevN2] != NULL_NODE) {
                prevN2++; 
            }
        }

//         System.out.printf("prevN1: %d, prevN2: %d%n", prevN1, prevN2);
        if (prevN1 < n1 && prevN2 < n2) 
            return new Pair<Integer>(prevN1, prevN2);
        else
            return null;

    }
    
    protected boolean areCompatibleEdges(int v1, int v2, int v3, int v4) {
        // If either g1 or g2 is multigraph, then we need to check the number
        // of edges
        if (checkMultiplexEdges) {
            Set<? extends Edge> e1 = g1.getEdges(v1, v2);
            Set<? extends Edge> e2 = g2.getEdges(v3, v4);
            if (e1.size() != e2.size())
                return false;

            // We know that because both graphs are multigraphs, the edges must
            // be TypedEdge instances, so this cast is safe.
            @SuppressWarnings("unchecked")
            Set<? extends TypedEdge<?>> te1 = (Set<? extends TypedEdge<?>>)e1;
            @SuppressWarnings("unchecked")
            Set<? extends TypedEdge<?>> te2 = (Set<? extends TypedEdge<?>>)e2;

            // Fill a set with the edge types of the first list of edges.
            Set<Object> types1 = new HashSet<Object>();
            for (TypedEdge<?> e : te1)
                types1.add(e.edgeType());

            // Then check if all the other set's types are contained within
            for (TypedEdge<?> e : te2) {
                if (!types1.contains(e.edgeType()))
                    return false;
            }
            // Because the sets are the same size, if we made it through the
            // other edge set without finding a difference, then the set are
            // equivalent.
            return true;
        }
        return true;
    }

    protected boolean areCompatableVertices(int v1, int v2) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isFeasiblePair(int node1, int node2) {
        assert node1 < n1;
        assert node2 < n2;
        assert core1[node1] == NULL_NODE;
        assert core2[node2] == NULL_NODE;

        // TODO: add checks for compatible nodes here

        // int i = 0;// , other1 = 0, other2 = 0;
        int termout1=0, termout2=0, termin1=0, termin2=0, new1=0, new2=0;

        // Check the 'out' edges of node1
        for (int other1 : getSuccessors(g1, node1)) {
            if (core1[other1] != NULL_NODE) {
                int other2 = core1[other1];
                // If there's node edge to the other node, or if there is some
                // edge incompatability, then the mapping is not feasible
                if (!g2.contains(node2, other2) ||
                        !areCompatibleEdges(node1, other1, node2, other2))
                    return false;
            }
            else {
                if (in1[other1] != 0)
                    termin1++;
                if (out1[other1] != 0)
                    termout1++;
                if (in1[other1] == 0 && out1[other1] == 0)
                    new1++;
            }
        }
            
        // Check the 'in' edges of node1
        for (int other1 : getPredecessors(g1, node1)) {           
            if (core1[other1] != NULL_NODE) {
                int other2 = core1[other1];
                // If there's node edge to the other node, or if there is some
                // edge incompatability, then the mapping is not feasible
                if (!g2.contains(other2, node2) ||
                        !areCompatibleEdges(node1, other1, node2, other2))
                    return false;
            }
            else {
                if (in1[other1] != 0)
                    termin1++;
                if (out1[other1] != 0)
                    termout1++;
                if (in1[other1] == 0 && out1[other1] == 0)
                    new1++;
            }
        }


        // Check the 'out' edges of node2
        for (int other2 : getSuccessors(g2, node2)) {
            if (core2[other2] != NULL_NODE) {
                int other1 = core2[other2];
                if (!g1.contains(node1, other1))
                    return false;
            }
            else {
                if (in2[other2] != 0)
                    termin2++;
                if (out2[other2] != 0)
                    termout2++;
                if (in2[other2] == 0 && out2[other2] == 0)
                    new2++;
            }
        }

        // Check the 'in' edges of node2
        for (int other2 : getPredecessors(g2, node2)) {
            if (core2[other2] != NULL_NODE) {
                int other1 = core2[other2];               
                if (!g1.contains(other1, node1))
                    return false;
            }

            else {
                if (in2[other2] != 0)
                    termin2++;
                if (out2[other2] != 0)
                    termout2++;
                if (in2[other2] == 0 && out2[other2] == 0)
                    new2++;
            }
        }

        return termin1 == termin2 && termout1 == termout2 && new1 == new2;
    }

    /**
     * {@inheritDoc}
     */
    public void addPair(int node1, int node2) {
        assert node1 < n1;
        assert node2 < n2;
        assert coreLen < n1;
        assert coreLen < n2;

        coreLen++;
	addedNode1 = node1;

	if (in1[node1] == 0) {
            in1[node1] = coreLen;
	    t1inLen++;
            if (out1[node1] != 0)
                t1bothLen++;
        }
	if (out1[node1] == 0) {
            out1[node1]=coreLen;
	    t1outLen++;
            if (in1[node1] != 0)
                t1bothLen++;
        }

	if (in2[node2] == 0) {
            in2[node2]=coreLen;
	    t2inLen++;
            if (out2[node2] != 0)
                t2bothLen++;
        }
	if (out2[node2] == 0) {
            out2[node2]=coreLen;
	    t2outLen++;
            if (in2[node2] != 0)
                t2bothLen++;
        }

        core1[node1] = node2;
        core2[node2] = node1;

        for (int other : getPredecessors(g1, node1)) {
            if (in1[other] == 0) {
                in1[other] = coreLen;
                t1inLen++;
                if (out1[other] != 0)
                    t1bothLen++;
            }
        }

        for (int other : getSuccessors(g1, node1)) {
            if (out1[other] == 0) {
                 out1[other]=coreLen;
                 t1outLen++;
                 if (in1[other] != 0)
                     t1bothLen++;
            }
        }
    
        for (int other : getPredecessors(g2, node2)) {            
            if (in2[other] == 0) {
                in2[other]=coreLen;
                t2inLen++;
                if (out2[other] != 0)
                    t2bothLen++;
            }
        }

        for (int other : getSuccessors(g2, node2)) {            
            if (out2[other] == 0) {
                out2[other]=coreLen;
                t2outLen++;
                if (in2[other] != 0)
                    t2bothLen++;
            }
        }        
    }

    /**
     * {@inheritDoc}
     */
    public boolean isGoal() {
        return coreLen == n1 && coreLen == n2;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDead() {
        return n1 != n2 
            || t1bothLen != t2bothLen
            || t1outLen != t2outLen
            || t1inLen != t2inLen;        
    }

    /**
     * {@inheritDoc}
     */
    public Map<Integer,Integer> getVertexMapping() {
        Map<Integer,Integer> vertexMapping = new HashMap<Integer,Integer>();
        for (int i = 0; i < core1.length; ++i) {
            if (core1[i] != NULL_NODE) {
                vertexMapping.put(i, core1[i]);
            }
        }
        return vertexMapping;
    }
    
    /**
     * {@inheritDoc}
     */
    public TypedVF2State copy() {
        return new TypedVF2State(this);
    }

    /**
     * {@inheritDoc}
     */
    public void backTrack() {
//         assert coreLen - origCoreLen <= 1 : "coreLen - origCoreLen ("
//             + (coreLen - origCoreLen) + ") > 1";
        assert addedNode1 != NULL_NODE;
        
        if (origCoreLen < coreLen) {
            int i, node2;

            if (in1[addedNode1] == coreLen)
                in1[addedNode1] = 0;

            for (int other : getPredecessors(g1, addedNode1)) {
                if (in1[other]==coreLen)
                    in1[other]=0;
            }
        
            if (out1[addedNode1] == coreLen)
                out1[addedNode1] = 0;

            for (int other : getSuccessors(g1, addedNode1)) {
                if (out1[other]==coreLen)
                    out1[other]=0;
            }
	    
            node2 = core1[addedNode1];

            if (in2[node2] == coreLen)
                in2[node2] = 0;
	    
            for (int other : getPredecessors(g2, node2)) {
                if (in2[other]==coreLen)
                    in2[other]=0;
            }
            
            if (out2[node2] == coreLen)
                out2[node2] = 0;
	    
            for (int other : getSuccessors(g2, node2)) {
                if (out2[other]==coreLen)
                    out2[other]=0;
            }
	    
	    core1[addedNode1] = NULL_NODE;
            core2[node2] = NULL_NODE;
	    
	    coreLen = origCoreLen;
            addedNode1 = NULL_NODE;
        }
    }

    /**
     * Returns those vertices that can be reached from {@code vertex} or the empty
     * set if {@code g} is not a directed graph.
     */
    private Set<Integer> getSuccessors(Graph g, Integer vertex) {
        if (g instanceof DirectedGraph) {
            DirectedGraph<?> dg = (DirectedGraph)g;
            return dg.successors(vertex);
        }
        else
            return Collections.<Integer>emptySet();
    }

    /**
     * Returns those vertices that point to from {@code vertex} or all the
     * adjacent vertices if {@code g} is not a directed graph.
     */
    @SuppressWarnings("unchecked")
    private Set<Integer> getPredecessors(Graph g, Integer vertex) {
        // The DirectedGraph cast seems to change the return type of the set
        // from Set<Integer> to just Set
        if (g instanceof DirectedGraph) {
            DirectedGraph<?> dg = (DirectedGraph)g;
            return dg.predecessors(vertex);
        }
        else {
            return g.getNeighbors(vertex);
        }
    }
}
