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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.graph.isomorphism.IsomorphicGraphCounter;
import edu.ucla.sspace.graph.isomorphism.IsomorphicSet;
import edu.ucla.sspace.graph.isomorphism.IsomorphismTester;
import edu.ucla.sspace.graph.isomorphism.TypedIsomorphicGraphCounter;
import edu.ucla.sspace.graph.isomorphism.TypedVF2IsomorphismTester;

import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.ObjectCounter;
import edu.ucla.sspace.util.WorkQueue;

import java.util.logging.Logger;
import java.util.logging.Level;

// Logger helper methods
import static edu.ucla.sspace.util.LoggerUtil.info;
import static edu.ucla.sspace.util.LoggerUtil.verbose;


/**
 * A complete re-implementation of the <a
 * href="http://theinf1.informatik.uni-jena.de/~wernicke/motifs/">FANMOD</a>
 * tool for finding motifs in graphs.  This implementation does not use the
 * motif subgraph counting described in the original paper due to supporting
 * multigraph-based motifs.  Furthermore, this implementation uses an equivalent
 * but <i>non-recursive</i> implementation of the ESU method described in the
 * paper for enumerate all the possible subgraphs of a fixed size in a graph.
 *
 * <p> The FANMOD method works by counting the number of subgraphs of the
 * specified size in the input graph.  Then it generates a number of random
 * graphs that preserve the properties of the original graph (its degree and
 * type sequence).  These random graphs create null model of the expected graph
 * distribution, which can then be compared with the actual distribution to
 * identify which subgraphs count as motifs.  Typically, a subgraph is a motif
 * if its frequency its greater than one standard deviation away from the
 * expected value (i.e., a <a
 * href=">http://en.wikipedia.org/wiki/Standard_score">Z-score</a> greater than
 * 1).
 *
 * <p> This implementation provides some flexibility in letting the user
 * identify which subgraphs should count as motifs based on their distribution
 * in the graph and null model.  A {@link MotifFilter} exposes the three main
 * parameters, a subgraph's actual frequency, its expected frequency, and its
 * standard deviation in the null model, which are then used to decide wither a
 * subgraph is a motif.  Typically a {@link ZScoreFilter} is used with a value
 * of {@code 1}.  However, callers may also filter with minimum frequency or
 * based on other attributes.
 * 
 * @see SubgraphIterator
 */
public class Fanmod {

    private static final Logger LOGGER = 
        Logger.getLogger(Fanmod.class.getName());

    /**
     * The internal queue for multithreading the null-model counting
     */
    private static final WorkQueue q = WorkQueue.getWorkQueue();

    /**
     * Creates a new Fanmod instance
     */
    public Fanmod() { }

    /**
     * Finds motifs in the input graph according to the specified parameters.
     *
     * @param g the graph that contains motifs to be found
     * @param findSimpleMotifs {@code true} if the subgraphs must be <a
     *        href="http://en.wikipedia.org/wiki/Simple_graph#Simple_graph">simple
     *        graphs</a>, i.e., graphs that contain no parallel edges.  These
     *        motifs are created by sampling all possible simple graphs from the
     *        multigraph.  For example, a three node subgraph with four edges would
     *        generate four potential simple graph motifs.
     * @param motifSize the number of vertices in the motif
     * @param numRandomGraphs the number of random graphs to generate from {@g},
     *        which have the same graph properties, and which will be used to
     *        create the null model.
     * @param filter a {@link MotifFilter} used to specify which subgraphs are
     *        significant and constitute motifs
     *
     * @return a mapping from each motif to a {@link Result} which describes
     *         that motifs distribution and stastics in the null mode.
     */
    public <T,E extends TypedEdge<T>> Map<Multigraph<T,E>,Result> 
            findMotifs(final Multigraph<T,E> g, final boolean findSimpleMotifs,
                       final int motifSize, int numRandomGraphs, 
                       MotifFilter filter) {

        Counter<Multigraph<T,E>> inGraph = 
            new TypedIsomorphicGraphCounter<T,Multigraph<T,E>>();

        verbose(LOGGER, "Counting all the %smotifs of size %d in the input",
                (findSimpleMotifs) ? "simple " : "", motifSize);
        
        // Select the iterator based on whether the used has asked us to find
        // motifs that are simple graphs, or motifs that are possibly
        // multigraphs
        Iterator<Multigraph<T,E>> subgraphIter = (findSimpleMotifs)
            ? new SimpleGraphIterator<T,E>(g, motifSize)
            : new SubgraphIterator<E,Multigraph<T,E>>(g, motifSize);        

        // Count all the isomorphic subgraphs
        int cnt = 0;
        long start = System.currentTimeMillis();
        while (subgraphIter.hasNext()) {
            inGraph.count(subgraphIter.next());
            if (++cnt % 10000 == 0) {
                long cur = System.currentTimeMillis();
                verbose(LOGGER, "Counted %d subgraphs in original graph " +
                        "(%d unique), %f subgraphs/sec", cnt, 
                        inGraph.size(), cnt / ((cur - start) / 1000d));
            }
        }

        info(LOGGER, "Finished counting in original graph, " +
             "computing null model");

        // Create a data structure to hold the motif counts for each of the
        // random models.  We need the counts separately in order to calcuate
        // the mean and stanard deviation for the Z score.
        final List<TypedIsomorphicGraphCounter<T,Multigraph<T,E>>> nullModelCounts = 
            new ArrayList<TypedIsomorphicGraphCounter<T,Multigraph<T,E>>>();
        
        // As we search through the null models for these motifs, the
        // isomorphism tester will likely need the graphs in a packed format
        // with a contiguous vertex ordering starting at 0.  Therefore, we remap
        // all of the potential motifs here so that the counters are initialized
        // with the most efficient set of motifs for fast testing.
        Set<Multigraph<T,E>> canonical = new HashSet<Multigraph<T,E>>();
        for (Multigraph<T,E> m : inGraph.items()) {
            @SuppressWarnings("unchecked")
            Multigraph<T,E> packed = (Multigraph<T,E>)(Graphs.pack(m));
            canonical.add(packed);
        }

        // Initialize all the null models' counters ahead of time so they can
        // access the list in a thread-safe manner
        for (int j = 0; j < numRandomGraphs; ++j) {
            // Create a graph counter that will record how many times the motifs
            // in the original network appear in the randomized networks
            nullModelCounts.add(
                TypedIsomorphicGraphCounter.asMotifs(canonical));
        }
         
        Object taskKey = q.registerTaskGroup(numRandomGraphs);
        
        for (int j = 0; j < numRandomGraphs; ++j) {
            final int j_ = j;
            q.add(taskKey, new Runnable() { 
                    public void run() {                       
                        verbose(LOGGER, "Computing random model %d", j_);
                        // Create a counter for this graph's motif counts
                        TypedIsomorphicGraphCounter<T,Multigraph<T,E>> 
                            nullModelCounter = nullModelCounts.get(j_);
                
                        // Make a thread-local copy of the graph, which will be
                        // randomized and used to find the motif counts in the
                        // null model
                        verbose(LOGGER, "Copying graph for null model %d", j_);
                        Multigraph<T,E> nullModel = g.copy(g.vertices());

                        // Randomize the connections 
                        verbose(LOGGER, "Shuffling edges of null model %d", j_);
                        Graphs.shufflePreserveType(nullModel, 3);

                        verbose(LOGGER, "Counting motifs of null model %d", j_);

                        // Select the appropriate method for iterating over the
                        // graph
                        Iterator<Multigraph<T,E>> subgraphIter =
                            (findSimpleMotifs)
                            ? new SimpleGraphIterator<T,E>(nullModel, motifSize)
                            : new SubgraphIterator<E,Multigraph<T,E>>(
                                      nullModel, motifSize);

                        // Then count the subraphs in the null model.  REMINDER:
                        // it might be useful to put a check in here to avoid
                        // counting subgraphs that are not in the base graph
                        // originally.
                        int count = 0;
                        long start = System.currentTimeMillis();
                        while (subgraphIter.hasNext()) {
                            if (++count % 10000 == 0) {
                                long cur = System.currentTimeMillis();
                                verbose(LOGGER,
                                        "Counted %d subgraphs in null model %d "
                                        + "(%d unique), %f subgraphs/sec", 
                                        count, j_, nullModelCounter.size(),
                                        count / ((cur - start) / 1000d));
                            }
                            
                            Multigraph<T,E> sub = subgraphIter.next();
                            nullModelCounter.count(sub);
                        }
                    }                
                });
        }
    
        q.await(taskKey);
        
        // Create a map to hold all the motifs that occured a Z-Score above the
        // threshold
        Map<Multigraph<T,E>,Result> motifToResult = 
            new HashMap<Multigraph<T,E>,Result>();
        
        info(LOGGER, "Building motif model counts");

        // Sum the counts.  Because the specific motif instances used by each of
        // the null models are identical to each other (and non-isomorphic), we
        // can just use a HashMap to hold all the count values.  We use a
        // HashMap rather than a Counter in order to get the standard deviation
        Map<Multigraph<T,E>,List<Integer>> motifCounts = 
            new HashMap<Multigraph<T,E>,List<Integer>>();
        int counterIndex = 0;
        for (TypedIsomorphicGraphCounter<T,Multigraph<T,E>> mc : nullModelCounts) {
            info(LOGGER, "Updating results for counter %d%n", counterIndex++);
            for (Map.Entry<Multigraph<T,E>,Integer> motifAndCount : mc) {
                Multigraph<T,E> motif = motifAndCount.getKey();
                int count = motifAndCount.getValue();
                List<Integer> counts = motifCounts.get(motif);
                if (counts == null) {
                    counts = new ArrayList<Integer>(numRandomGraphs);
                    motifCounts.put(motif, counts);
                }
                counts.add(count);
            }
            assert motifCounts.size() == inGraph.size() 
                : "Null model has extra motif";
        }

        info(LOGGER, "Computing motif statistics");
        
        // For each of the motifs, calcuate its Z-Score using the random models.
        for (Map.Entry<Multigraph<T,E>,Integer> motifAndCount : inGraph) {
            
            // NOTE: the null models use a packed representation (for
            // efficiency) so in order to access the counts, we have to do a bit
            // of extra work to cover the in-graph motifs to the packed format
            Multigraph<T,E> motif = motifAndCount.getKey();
            @SuppressWarnings("unchecked")
            Multigraph<T,E> packed = (Multigraph<T,E>)(Graphs.pack(motif));

            int count = motifAndCount.getValue();
            // NOTE: use packed instead of motif because of the difference from
            // the null model
            List<Integer> counts = motifCounts.get(packed);

            // Calcuate the statistics for the counts
            double mean = Statistics.mean(counts);
            double stddev = Statistics.stddev(counts);

            if (filter.accepts(count, mean, stddev)) {
                motifToResult.put(packed, new Result(count, mean, stddev,
                                  filter.getStatistic(count, mean, stddev)));
            }
        }
        info(LOGGER, "accepted %d motifs, rejected %d", motifToResult.size(),
             inGraph.size() - motifToResult.size());
        
        return motifToResult;
    }

    /**
     * Finds motifs in the input graph according to the specified parameters.
     *
     * @param g the graph that contains motifs to be found
     * @param motifSize the number of vertices in the motif
     * @param numRandomGraphs the number of random graphs to generate from {@g},
     *        which have the same graph properties, and which will be used to
     *        create the null model.
     * @param filter a {@link MotifFilter} used to specify which subgraphs are
     *        significant and constitute motifs
     *
     * @return a mapping from each motif to a {@link Result} which describes
     *         that motifs distribution and stastics in the null mode.
     */
    public <E extends Edge> Map<Graph<E>,Result> 
            findMotifs(final Graph<E> g, final int motifSize, 
                       int numRandomGraphs, MotifFilter filter) {

        Counter<Graph<E>> inGraph = new IsomorphicGraphCounter<Graph<E>>();
        
        // Select the iterator based on whether the used has asked us to find
        // motifs that are simple graphs, or motifs that are possibly
        // multigraphs
        Iterator<Graph<E>> subgraphIter = 
            new SubgraphIterator<E,Graph<E>>(g, motifSize);

        // Count all the isomorphic subgraphs
        while (subgraphIter.hasNext()) {
            inGraph.count(subgraphIter.next());
        }

        // Create a data structure to hold the motif counts for each of the
        // random models.  We need the counts separately in order to calcuate
        // the mean and stanard deviation for the Z score.
        final List<IsomorphicGraphCounter<Graph<E>>> nullModelCounts = 
            new ArrayList<IsomorphicGraphCounter<Graph<E>>>();
        
        // Initialize all the null models' counters ahead of time so they can
        // access the list in a thread-safe manner
        for (int j = 0; j < numRandomGraphs; ++j) {
            // Create a graph counter that will record how many times the motifs
            // in the original network appear in the randomized networks
            nullModelCounts.add(
                new IsomorphicGraphCounter<Graph<E>>(inGraph.items()));
        }
         
        Object taskKey = q.registerTaskGroup(numRandomGraphs);
        
        for (int j = 0; j < numRandomGraphs; ++j) {
            final int j_ = j;
            q.add(taskKey, new Runnable() { 
                    public void run() {                       
                        verbose(LOGGER, "Computing random model %d", j_);
                        // Create a counter for this graph's motif counts
                        IsomorphicGraphCounter<Graph<E>> nullModelCounter =
                            nullModelCounts.get(j_);
                
                        // Make a thread-local copy of the graph, which will be
                        // randomized and used to find the motif counts in the
                        // null model
                        Graph<E> nullModel = g.copy(g.vertices());

                        // Randomize the connections 
                        Graphs.shufflePreserve(nullModel, 3);

                        // Select the appropriate method for iterating over the
                        // graph
                        Iterator<Graph<E>> subgraphIter =
                            new SubgraphIterator<E,Graph<E>>(nullModel, motifSize);

                        // Then count the subraphs in the null model.  REMINDER:
                        // it might be useful to put a check in here to avoid
                        // counting subgraphs that are not in the base graph
                        // originally.
                        int count = 0;
                        long start = System.currentTimeMillis();
                        while (subgraphIter.hasNext()) {
                            if (++count % 100000 == 0) {
                                long cur = System.currentTimeMillis();
                                verbose(LOGGER,
                                        "Counted %d subgraphs in null model " +
                                        "(%d unique), %f subgraphs/sec", 
                                        count, nullModelCounter.size(),
                                        (double)count / (cur - start / 1000));
                            }
                            
                            Graph<E> sub = subgraphIter.next();
                            nullModelCounter.count(sub);
                        }
                    }                
                });
        }
    
        q.await(taskKey);
        
        // Create a map to hold all the motifs that occured a Z-Score above the
        // threshold
        Map<Graph<E>,Result> motifToResult = new HashMap<Graph<E>,Result>();
        
        // For each of the motifs, calcuate its Z-Score using the random models.
        for (Map.Entry<Graph<E>,Integer> motifAndCount : inGraph) {
            Graph<E> motif = motifAndCount.getKey();
            int count = motifAndCount.getValue();
            int[] counts = new int[numRandomGraphs];
            int i = 0;
            for (IsomorphicGraphCounter<Graph<E>> mc : nullModelCounts)
                counts[i++] = mc.getCount(motif);
            // Calcuate the statistics for the counts
            double mean = Statistics.mean(counts);
            double stddev = Statistics.stddev(counts);
            
            if (filter.accepts(count, mean, stddev)) {
                motifToResult.put(motif, new Result(count, mean, stddev,
                                  filter.getStatistic(count, mean, stddev)));
            }
        }
        
        return motifToResult;
    }

    // NOTE: probably need something more reasonable here as output?

    /**
     * The result of computing a motif's distribution in the graph and null
     * model
     */
    public static class Result {

        /**
         * The motif's count in the graph
         */
        public final int count;

        /**
         * The expected value of motif's count in the null model
         */
        public final double meanCountInNullModel;

        /**
         * The standard deviation of motif's counts in the null model
         */
        public final double stddevInNullModel;

        /**
         * The statistic value generated by the {@link MotifFilter} for deciding
         * if the associated subgraph is a motif.
         */
        public final double statistic;
        
        public Result(int count, 
                      double meanCountInNullModel,
                      double stddevInNullModel,
                      double statistic) {
            this.count = count;
            this.meanCountInNullModel = meanCountInNullModel;
            this.stddevInNullModel = stddevInNullModel;
            this.statistic = statistic;
        }

    }

    /**
     * An interface for performing some computation on a subgraph's frequency
     * information to deicide whether its occurrences constitute it being a
     * motif.
     */
    public static interface MotifFilter {
        
        /**
         * Returns {@code true} if the information on the subgraph's frequencies
         * indicate that it is a motif
         */
        boolean accepts(int actualFrequency, double expectedValue,
                        double standardDeviation);

        /**
         * Returns the value used by this filter to identify whether a subgraph
         * is a motif, which could be the Z-score, for example.
         */
        double getStatistic(int actualFrequency, double expectedValue,
                            double standardDeviation);

    }

    public static class ZScoreFilter implements MotifFilter {

        private final double minZscore;

        public ZScoreFilter(double minZscore) {
            this.minZscore = minZscore;
        }
        
        public boolean accepts(int actualFrequency, double expectedValue,
                               double standardDeviation) {
            double zScore = (actualFrequency - expectedValue) 
                / standardDeviation;
            return zScore >= minZscore;
        }

        public double getStatistic(int actualFrequency, double expectedValue,
                                   double standardDeviation) {
            return (actualFrequency - expectedValue) / standardDeviation;
        }
    }

    public static class FrequencyFilter implements MotifFilter {

        private final double minFrequency;

        public FrequencyFilter(double minFrequency) {
            this.minFrequency = minFrequency;
        }
        
        public boolean accepts(int actualFrequency, double expectedValue,
                               double standardDeviation) {
            return actualFrequency >= minFrequency;
        }

        public double getStatistic(int actualFrequency, double expectedValue,
                                   double standardDeviation) {
            return actualFrequency;
        }
    }

    public static class FrequencyAndZScoreFilter implements MotifFilter {

        private final int minFrequency;

        private final double minZscore;

        public FrequencyAndZScoreFilter(int minFrequency, double minZscore) {
            this.minFrequency = minFrequency;
            this.minZscore = minZscore;
        }
        
        public boolean accepts(int actualFrequency, double expectedValue,
                               double standardDeviation) {
            double zScore = (actualFrequency - expectedValue) 
                / standardDeviation;
            return actualFrequency >= minFrequency && zScore >= minZscore;
        }

        public double getStatistic(int actualFrequency, double expectedValue,
                                   double standardDeviation) {
            return (actualFrequency - expectedValue) / standardDeviation;
        }
    }

}