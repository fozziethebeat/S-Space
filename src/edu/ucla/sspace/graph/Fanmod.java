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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.graph.isomorphism.IsomorphicSet;
import edu.ucla.sspace.graph.isomorphism.IsomorphismTester;
import edu.ucla.sspace.graph.isomorphism.ThreeVertexIsomorphismTester;
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
 *
 */
public class Fanmod {

    private static final Logger LOGGER = 
        Logger.getLogger(Fanmod.class.getName());

    private static final WorkQueue q = new WorkQueue(4);

    public Fanmod() { }

    public <T,E extends TypedEdge<T>> Map<Multigraph<T,E>,Result> 
            findMotifs(final Multigraph<T,E> g, final boolean findSimpleMotifs,
                       final int motifSize, int numRandomGraphs, 
                       MotifFilter filter) {

        Counter<Multigraph<T,E>> inGraph = 
            new TypedMotifCounter<T,Multigraph<T,E>>();

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
        final List<TypedMotifCounter<T,Multigraph<T,E>>> nullModelCounts = 
            new ArrayList<TypedMotifCounter<T,Multigraph<T,E>>>();
        
        // Initialize all the null models' counters ahead of time so they can
        // access the list in a thread-safe manner
        for (int j = 0; j < numRandomGraphs; ++j) {
            // Create a graph counter that will record how many times the motifs
            // in the original network appear in the randomized networks
            nullModelCounts.add(TypedMotifCounter.asMotifs(inGraph.items()));
            //new TypedMotifCounter<T,Multigraph<T,E>>(inGraph.items()));
        }
         
        Object taskKey = q.registerTaskGroup(numRandomGraphs);
        
        for (int j = 0; j < numRandomGraphs; ++j) {
            final int j_ = j;
            q.add(taskKey, new Runnable() { 
                    public void run() {                       
                        verbose(LOGGER, "Computing random model %d", j_);
                        // Create a counter for this graph's motif counts
                        TypedMotifCounter<T,Multigraph<T,E>> nullModelCounter =
                            nullModelCounts.get(j_);
                
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
        // can just use an ObjectCounter to sum their values
        Map<Multigraph<T,E>,List<Integer>> motifCounts = 
            new HashMap<Multigraph<T,E>,List<Integer>>();
        for (TypedMotifCounter<T,Multigraph<T,E>> mc : nullModelCounts) {
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
            Multigraph<T,E> motif = motifAndCount.getKey();
            //verbose(LOGGER, "Computing statistics for motif %s", motif);
            int count = motifAndCount.getValue();
            
            List<Integer> counts = motifCounts.get(motif);

            // Calcuate the statistics for the counts
            double mean = Statistics.mean(counts);
            double stddev = Statistics.stddev(counts);
            System.out.printf("count: %d, mean: %f, stddev: %f%n", count, mean, stddev);

            if (filter.accepts(count, mean, stddev)) {
                motifToResult.put(motif, new Result(count, mean, stddev,
                                  filter.getStatistic(count, mean, stddev)));
            }
        }
        info(LOGGER, "accepted %d motifs, rejected %d", motifToResult.size(),
             inGraph.size() - motifToResult.size());
        
        return motifToResult;
    }

    public <E extends Edge> Map<Graph<E>,Result> 
            findMotifs(final Graph<E> g, final int motifSize, 
                       int numRandomGraphs, MotifFilter filter) {

        Counter<Graph<E>> inGraph = new MotifCounter<Graph<E>>();
        
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
        final List<MotifCounter<Graph<E>>> nullModelCounts = 
            new ArrayList<MotifCounter<Graph<E>>>();
        
        // Initialize all the null models' counters ahead of time so they can
        // access the list in a thread-safe manner
        for (int j = 0; j < numRandomGraphs; ++j) {
            // Create a graph counter that will record how many times the motifs
            // in the original network appear in the randomized networks
            nullModelCounts.add(
                new MotifCounter<Graph<E>>(inGraph.items()));
        }
         
        Object taskKey = q.registerTaskGroup(numRandomGraphs);
        
        for (int j = 0; j < numRandomGraphs; ++j) {
            final int j_ = j;
            q.add(taskKey, new Runnable() { 
                    public void run() {                       
                        verbose(LOGGER, "Computing random model %d", j_);
                        // Create a counter for this graph's motif counts
                        MotifCounter<Graph<E>> nullModelCounter =
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
            for (MotifCounter<Graph<E>> mc : nullModelCounts)
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


    public static class Result {

        public final int count;
        public final double meanCountInNullModel;
        public final double stddevInNullModel;
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

    public static interface MotifFilter {
        
        boolean accepts(int actualFrequency, double expectedValue,
                        double standardDeviation);
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
}