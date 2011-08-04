package edu.ucla.sspace.dependency;

import edu.ucla.sspace.vector.Vector;

import java.util.LinkedList;


/**
 * An interface for permuting a {@link Vector} based on a dependecny path,
 * represented as a list of {@link DependencyRelation}s.  Implemenations are
 * recomended to extend existing {@link
 * edu.ucla.sspace.index.PermutationFunction PermutationFunction}s but simply
 * using an existing {@link edu.ucla.sspace.index.PermutationFunction
 * PermutationFunction}.  Implementations are also suggested to be thread-safe.
 *
 * @see edu.ucla.sspace.index.PermutationFunction
 *
 * @author Keith Stevens
 */
public interface DependencyPermutationFunction <T extends Vector> {

    /**
     * Returns a permuted form of {code vector} based on the dependency path
     * provided.
     *
     * @param path A linked list of word,relation pairs that compose a
     *        dependency path
     * @param vector The {@link Vector} to permute
     *
     * @return A new permuted {@link Vector} of the same type as {@code vector}
     *         that is
     */
    T permute(T vector, DependencyPath path);
}
