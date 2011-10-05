package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.TernaryVector;

import java.util.Map;
import java.util.Queue;


/**
 * @author Keith Stevens
 */
public class RandomIndexingOccurrenceDependencyContextGenerator
    extends AbstractOccurrenceDependencyContextGenerator {

    /**
     * A mapping from strings to {@code IntegerVector}s which represent an index
     * vector.
     */
    private final Map<String, TernaryVector> indexMap;

    /**
     * The {@code PermutationFunction} to use for co-occurrances.
     */
    private final PermutationFunction<TernaryVector> permFunc;

    /**
     * The size of each index vector, as set when the sspace is created.
     */
    private final int indexVectorLength;

    /**
     * Creates a new {@link RandomIndexingContextGenerator}.
     *
     * @param indexMap The map responsible for mapping co-occurring terms to
     *        their index vectors.  This map should generate index vectors for
     *        words that are not currently mapped when in normal mode.
     * @param perm A {@link PermutationFunction} for {@link TernaryVector}s.
     * @param indexVectorLength The number of dimensions in each index vector.
     */
    public RandomIndexingOccurrenceDependencyContextGenerator(
            Map<String, TernaryVector> indexMap,
            PermutationFunction<TernaryVector> perm,
            int indexVectorLength) {
        this.indexMap = indexMap;
        this.permFunc = perm;
        this.indexVectorLength = indexVectorLength;
    }

    /**
     * Returns the word stored by {@code node}.
     */
    public String getFeature(DependencyTreeNode node, int index) {
        return node.word();
    }

    /**
     * Adds an index vector corresponding to each word in {@code words} to
     * {@code meaning}.
     */
    protected void addContextTerms(SparseDoubleVector meaning,
                                   Queue<String> words,
                                   int distance) {
        --distance;
        // Iterate through the words in the context.
        for (String term : words) {
            ++distance;
            if (term.equals(IteratorFactory.EMPTY_TOKEN))
                continue;

            // Get the index vector for the word.
            TernaryVector termVector = indexMap.get(term);
            if (termVector == null)
                continue;
            
            // Permute the index vector if a permutation function is provided.
            if (permFunc != null)
                termVector = permFunc.permute(termVector, distance);

            // Add the index vector and update the distance.
            add(meaning, termVector);
        }
    }

    /**
     * Adds a {@link TernaryVector} to a {@link IntegerVector}
     */
    private void add(SparseDoubleVector dest, TernaryVector src) {
        for (int p : src.positiveDimensions())
            dest.add(p, 1);
        for (int n : src.negativeDimensions())
            dest.add(n, -1);
    }
    
    public void setReadOnly(boolean readOnly) {
    }
}
