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

package edu.ucla.sspace.graph.generator;

import java.util.Set;

import edu.ucla.sspace.graph.DirectedEdge;
import edu.ucla.sspace.graph.DirectedGraph;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.TypedEdge;
import edu.ucla.sspace.graph.WeightedEdge;
import edu.ucla.sspace.graph.WeightedGraph;


/**
 * An interface for algorithms that can programatically generate {@link Graph}
 * instances using a predefined model.  
 *
 * <p> If a graph generator can be configured using global parameters, the class
 * should provide a no-arg constructor that sets all parameters to some
 * reasonable values, and one or more additional constructors to expose those
 * parameters.
 *
 * <p> Implementations are encouraged to specify additional {@code generate}
 * methods that may return different graph types or expose additional
 * parameters.
 *
 * <p> Note that all methods aside from {@link #generate(int,int)} are marked
 * optional in order support algorithms that are not designed to generate all
 * possible graph types.  Implementations are encourage to implement as many
 * methods as possible.
 */
public interface GraphGenerator {
    
    /**
     * Creates an undirected graph with the specified number of nodes and edges.
     */
    Graph<? extends Edge> generate(int numNodes, int numEdges);

    /**
     * Creates a directed graph with the specified number of nodes and edges.
     */
    DirectedGraph<? extends DirectedEdge> generateDirected(int numNodes, 
                                                           int numEdges);

    /***
     * Creates a multigraph with the specified number of node sand edges, using
     * the set of types to create the edges.
     */
    <T> Multigraph<T,? extends TypedEdge<T>> generateMultigraph(
                               int numNodes, int numEdges, Set<T> types);

    /**
     * Creates a weighted graph with the specified number of nodes and edges.
     */    
    WeightedGraph<? extends WeightedEdge> generateWeighted(int numNodes, 
                                                           int numEdges);
}