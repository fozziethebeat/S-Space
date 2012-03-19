package edu.ucla.sspace.matrix;


/**
 * An interface for any ranking algorithm that applies a score to each node in a
 * graph based on it's connectivity strength.  Input the every {@link
 * MatrixRank} implementation should be an adjacency matrix where the columns
 * specify the out edges for every node and the rows specify the in edges such
 * that
 *
 * <pre>
 *   adjacencyMatrix.get(x, y);
 * </pre>
 *
 * returns the edge weight from node {@code y} to node {@code x}.
 *
 * </p>
 *
 * Node ranks can be any real value.  Implementations should be state free.  Any
 * additional parameters for the algorithm should be set through the constructor
 * and treated as read only values.
 *
 * @author Keith Stevens
 */
public interface MatrixRank {

    /**
     * Returns the node rank of every vertex in a graph stored represented as an
     * {@code adjacencyMatrix}.  Each entry in {@code adjacencyMatrix} should
     * correspond to the link between two vertices where 
     *
     * <pre>
     *   adjacencyMatrix.get(to, from);
     * </pre>
     *
     * returns the weight of the edge from {@code from} into {@code to}.  
     *
     * </p>
     *
     * If the method requires an initial ranking for every node, it will use the
     * ranks returned from {@link #initialRanks}.
     */
    double[] rank(Matrix adjacencyMatrix);

    /**
     * Returns the node rank of every vertex in a graph stored represented as an
     * {@code adjacencyMatrix}.  Each entry in {@code adjacencyMatrix} should
     * correspond to the link between two vertices where 
     *
     * <pre>
     *   adjacencyMatrix.get(to, from);
     * </pre>
     *
     * returns the weight of the edge from {@code from} into {@code to}. 
     *
     * </p>
     *
     * If method requires an initial ranking for every node, it will use the
     * ranks returned from {@link #initialRanks}.
     */
    double[] rank(SparseMatrix adjacencyMatrix);

    /**
     * Returns the node rank of every vertex in a graph stored represented as an
     * {@code adjacencyMatrix}.  Each entry in {@code adjacencyMatrix} should
     * correspond to the link between two vertices where 
     *
     * <pre>
     *   adjacencyMatrix.get(to, from);
     * </pre>
     *
     * returns the weight of the edge from {@code from} into {@code to}.
     * 
     * </p>
     *
     * The values in {@code initialRanks} will be used as the initial node ranks
     * if they are needed.
     */
    double[] rank(Matrix adjacencyMatrix, double[] initialRanks);

    /**
     * Returns the node rank of every vertex in a graph stored represented as an
     * {@code adjacencyMatrix}.  Each entry in {@code adjacencyMatrix} should
     * correspond to the link between two vertices where 
     *
     * <pre>
     *   adjacencyMatrix.get(to, from);
     * </pre>
     *
     * returns the weight of the edge from {@code from} into {@code to}.
     * 
     * </p>
     *
     * The values in {@code initialRanks} will be used as the initial node ranks
     * if they are needed.
     */
    double[] rank(SparseMatrix adjacencyMatrix, double[] initialRanks);

    /**
     * Returns an initial set of rankings for each node.
     */
    double[] initialRanks(int numRows);
}
