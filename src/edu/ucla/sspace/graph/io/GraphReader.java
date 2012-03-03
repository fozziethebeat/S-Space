/*
 * Copyright 2012 David Jurgens
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

package edu.ucla.sspace.graph.io;


import edu.ucla.sspace.graph.DirectedEdge;
import edu.ucla.sspace.graph.DirectedGraph;
import edu.ucla.sspace.graph.DirectedMultigraph;
import edu.ucla.sspace.graph.DirectedTypedEdge;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.SimpleDirectedEdge;
import edu.ucla.sspace.graph.SimpleDirectedTypedEdge;
import edu.ucla.sspace.graph.SparseDirectedGraph;
import edu.ucla.sspace.graph.TypedEdge;
import edu.ucla.sspace.graph.UndirectedMultigraph;
import edu.ucla.sspace.graph.WeightedEdge;
import edu.ucla.sspace.graph.WeightedGraph;

import edu.ucla.sspace.util.Indexer;

import java.io.File;
import java.io.IOException;


/**
 * A general interface for reading {@link Graph} instances for a variety of
 * formats.
 */
public interface GraphReader {

    /**
     * Reads in the graph from the provided file
     *
     * @throws IOException if any error occurs while reading the file
     */
    Graph<Edge> readUndirected(File f) throws IOException;

    /**
     * Reads in the graph from the provided file, using the specified {@link
     * Indexer} to decide how vertex labels in the file are mapped to numeric
     * vertices.  The state of the indexer may be updated as a result of this
     * call.  Providing an initially-empty {@code Indexer} allows the caller to
     * identify which labels were present in the file initially.
     *
     * @param vertexLabels the mapping to use when converting form label to
     *        vertex ordinal
     *
     * @throws IOException if any error occurs while reading the file
     */
    Graph<Edge> readUndirected(File f, Indexer<String> vertexLabels)
        throws IOException;

    /**
     * Reads in the weighted graph from the provided file.
     *
     * @throws IOException if any error occurs while reading the file
     */
    WeightedGraph<WeightedEdge> readWeighted(File f) throws IOException;

    /**
     * Reads in the weighted graph from the provided file, using the specified
     * {@link Indexer} to decide how vertex labels in the file are mapped to
     * numeric vertices.  The state of the indexer may be updated as a result of
     * this call.  Providing an initially-empty {@code Indexer} allows the
     * caller to identify which labels were present in the file initially.
     *
     * @param vertexLabels the mapping to use when converting form label to
     *        vertex ordinal
     *
     * @throws IOException if any error occurs while reading the file
     */
    WeightedGraph<WeightedEdge> readWeighted(
            File f, Indexer<String> vertexLabels) throws IOException;

    /**
     * Reads in the graph from the provided file, using the specified {@link
     * Indexer} to decide how vertex labels in the file are mapped to numeric
     * vertices, and keeping only those weights above the specified value.  The
     * state of the indexer may be updated as a result of this call.  Providing
     * an initially-empty {@code Indexer} allows the caller to identify which
     * labels were present in the file initially.
     *
     * @param vertexLabels the mapping to use when converting form label to
     *        vertex ordinal
     * @param minWeight the minimum weight for any edge in {@code f} to have if
     *        it is to be added to the returned weighted graph.
     *
     * @throws IOException if any error occurs while reading the file
     */
    WeightedGraph<WeightedEdge> readWeighted(
            File f, Indexer<String> vertexLabels, 
            double minWeight) throws IOException;

    /**
     * Reads in an undirected network from a file containing weighted edges,
     * only keeping those undirected edges whose weight was above the specified
     * threshold
     *
     * @param vertexLabels the mapping to use when converting form label to
     *        vertex ordinal
     * @param minWeight the minimum weight for any edge in {@code f} to have if
     *        it is to be added to the returned graph.
     *
     * @throws IOException if any error occurs while reading the file

     */
    Graph<Edge> readUndirectedFromWeighted(File f, Indexer<String> vertexLabels,
                                           double minWeight) throws IOException;

    /**
     * Reads in the directed graph from the provided file
     *
     * @throws IOException if any error occurs while reading the file
     */
    DirectedGraph<DirectedEdge> readDirected(File f) throws IOException;

    /**
     * Reads in the directed graph from the provided file, using the specified
     * {@link Indexer} to decide how vertex labels in the file are mapped to
     * numeric vertices.  The state of the indexer may be updated as a result of
     * this call.  Providing an initially-empty {@code Indexer} allows the
     * caller to identify which labels were present in the file initially.
     *
     * @param vertexLabels the mapping to use when converting form label to
     *        vertex ordinal
     *
     * @throws IOException if any error occurs while reading the file
     */
    DirectedGraph<DirectedEdge> readDirected(File f, Indexer<String> vertexLabels)
        throws IOException;

    /**
     * Reads in the directed multigraph from the provided file.
     *
     * @throws IOException if any error occurs while reading the file
     */
    DirectedMultigraph<String> readDirectedMultigraph(File f) 
        throws IOException;

    /**
     * Reads in the graph from the provided file, using the specified {@link
     * Indexer} to decide how vertex labels in the file are mapped to numeric
     * vertices.  The state of the indexer may be updated as a result of this
     * call.  Providing an initially-empty {@code Indexer} allows the caller to
     * identify which labels were present in the file initially.
     *
     * @param vertexLabels the mapping to use when converting form label to
     *        vertex ordinal
     *
     * @throws IOException if any error occurs while reading the file
     */
    DirectedMultigraph<String> readDirectedMultigraph(
            File f, Indexer<String> vertexLabels) throws IOException;

    /**
     * Reads in the multigraph from the provided file
     *
     * @throws IOException if any error occurs while reading the file
     */
    UndirectedMultigraph<String> readUndirectedMultigraph(File f) 
        throws IOException;

    /**
     * Reads in the multigraph from the provided file, using the specified
     * {@link Indexer} to decide how vertex labels in the file are mapped to
     * numeric vertices.  The state of the indexer may be updated as a result of
     * this call.  Providing an initially-empty {@code Indexer} allows the
     * caller to identify which labels were present in the file initially.
     *
     * @param vertexLabels the mapping to use when converting form label to
     *        vertex ordinal
     *
     * @throws IOException if any error occurs while reading the file
     */
    UndirectedMultigraph<String> readUndirectedMultigraph(
            File f, Indexer<String> vertexLabels) throws IOException;

}