package edu.ucla.sspace.graph.generator;

import java.util.Set;

import edu.ucla.sspace.graph.DirectedEdge;
import edu.ucla.sspace.graph.DirectedGraph;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.SimpleEdge;
import edu.ucla.sspace.graph.SparseUndirectedGraph;
import edu.ucla.sspace.graph.TypedEdge;
import edu.ucla.sspace.graph.WeightedEdge;
import edu.ucla.sspace.graph.WeightedGraph;


public class ErdosRenyiGraphGenerator implements GraphGenerator {
    
    public Graph<? extends Edge> generate(int numNodes, int numEdges) {
        if (numNodes <= 1)
            throw new IllegalArgumentException("must have at least two nodes");
        if (numEdges < 0)
            throw new IllegalArgumentException("cannot have a negative nubmer of edges");
        if (numEdges >= (numNodes * (numNodes - 1) / 2))
            throw new IllegalArgumentException("too many edges for the number of nodes");
        Graph<Edge> g = new SparseUndirectedGraph();
        //System.out.printf("Creating a graph with %d vertices and %d edges%n");
        // Add all the vertices
        for (int i = 0; i < numNodes; ++i)
            g.add(i);
        int added = 0;
        while (added < numEdges) {
            // Pick two vertices at random
            int i = (int)(Math.random() * numNodes);
            int j = (int)(Math.random() * numNodes);
            if (g.add(new SimpleEdge(i, j)))
                added++;
        }
        return g;
    }

    public DirectedGraph<? extends DirectedEdge> generateDirected(int numNodes, int numEdges) {
        throw new Error();
    }

    public <T> Multigraph<T,? extends TypedEdge<T>> generateMultigraph(int numNodes, int numEdges, Set<T> types) {
        throw new Error();
    }
    
    public WeightedGraph<? extends WeightedEdge> generateWeighted(int numNodes, int numEdges) {
        throw new Error();
    }
}