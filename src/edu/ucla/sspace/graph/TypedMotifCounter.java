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

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.CombinedSet;
import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.ObjectCounter;
import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.graph.isomorphism.IsomorphismTester;
import edu.ucla.sspace.graph.isomorphism.TypedVF2IsomorphismTester;

import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A special-purpose {@link Counter} that counts <b>multigraphs</b> based on
 * typed <a
 * href="http://en.wikipedia.org/wiki/Graph_isomorphism">isomorphism</a>, rather
 * than object equivalence (which may take into account vertex labeling, etc.).
 * Most commonly, isomorphism is needed when counting the number of motifs in a
 * graph and their relative occurrences.
 */
public class TypedMotifCounter<T,G extends Multigraph<T,? extends TypedEdge<T>>>
        implements Counter<G>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The isomorphism tester used to find multigraph equality
     */
    private final IsomorphismTester isoTest;

    private final Map<Set<T>,LinkedList<Map.Entry<G,Integer>>> typesToGraphs;

    private int sum;

    private final boolean allowNewMotifs;
    
    /**
     * Creates a new {@code MotifCounter} with the default isomorphism tester.
     */ 
    public TypedMotifCounter() {
        this(new TypedVF2IsomorphismTester());
    }

    /**
     * Creates a new {@code MotifCounter} with the specified isomorphism tester.
     * Most users will not need this constructor, which is intended for special
     * cases where an {@link IsomorphismTester} is tailored to quickly match the
     * type of motifs being counted.
     */ 
    public TypedMotifCounter(IsomorphismTester isoTest) {
        this.isoTest = isoTest;
        typesToGraphs = new HashMap<Set<T>,LinkedList<Map.Entry<G,Integer>>>();
        sum = 0;
        allowNewMotifs = true;
    }

    /**
     * Creates a new {@code TypedMotifCounter} that counts only the specified
     * motifs.  All other non-isomorphic graphs will not be counted.
     */
    private TypedMotifCounter(Collection<? extends G> motifs) {
        this.isoTest = new TypedVF2IsomorphismTester();
        typesToGraphs = new HashMap<Set<T>,LinkedList<Map.Entry<G,Integer>>>();
        sum = 0;       
        allowNewMotifs = false;
        // Initialize the motif mapping with all the isomorphic graphs in the
        // provided set
        for (G g : motifs)
            addInitial(g);
    }

    /**
     * Counts the number of isomorphic graphs in {@code c} and includes their
     * sum in this counter.
     */
    public void add(Counter<? extends G> c) {
        for (Map.Entry<? extends G,Integer> e : c) {
            count(e.getKey(), e.getValue());
        }
    }

    /**
     * Adds an initial set of valid motifs to this counter with no counts.  This
     * method enables the fixed-motif constructor to initialize the set of valid
     * motifs prior to counting.
     */
    private void addInitial(G g) {
        Set<T> typeCounts = g.edgeTypes();
        LinkedList<Map.Entry<G,Integer>> graphs = typesToGraphs.get(typeCounts);
        if (graphs == null) {
            graphs = new LinkedList<Map.Entry<G,Integer>>();
            typesToGraphs.put(new HashSet<T>(typeCounts), graphs);
        }

        graphs.add(new SimpleEntry<G,Integer>(g, 0));
    }    

    /**
     * Creates a new {@code TypedMotifCounter} that counts only the specified
     * motifs.  All other non-isomorphic graphs will not be counted.  
     *
     * @param motifs the set of graph instance to be treats as valid motifs.
     *        Note that this input graph is not itself checked for isomorphism.
     *        The presence of isomorphic graphs in {@code motifs} will cause all
     *        of the counts to be accumulated on one of the variants, while the
     *        other will have a count of 0.
     */
    public static <T,G extends Multigraph<T,? extends TypedEdge<T>>>
            TypedMotifCounter<T,G> asMotifs(Set<? extends G> motifs) {
        return new TypedMotifCounter<T,G>(motifs);
    }

    /**
     * Counts the isomorphic version of this graph, increasing the total by 1
     */
    public int count(G g) {
        return count(g, 1);
    }

    /**
     * Counts the isomorphic version of this graph, increasing its total count
     * by the specified positive amount.
     *
     * @param count a positive value for the number of times the object occurred
     *
     * @throws IllegalArgumentException if {@code count} is not a positive value.
     */
    public int count(G g, int count) {
        if (count < 1)
            throw new IllegalArgumentException("Count must be positive");
        sum += count;
        Set<T> typeCounts = g.edgeTypes();
        LinkedList<Map.Entry<G,Integer>> graphs = typesToGraphs.get(typeCounts);
        if (graphs == null) {
            // If there wasn't a mapping for this graph's configuration and
            // we're not allowing new motif instances, return 0.
            if (!allowNewMotifs)
                return 0;
            graphs = new LinkedList<Map.Entry<G,Integer>>();
            typesToGraphs.put(new HashSet<T>(typeCounts), graphs);
            graphs.add(new SimpleEntry<G,Integer>(g, count));
            return count;
        }
        else {
            Iterator<Map.Entry<G,Integer>> iter = graphs.iterator();
            while (iter.hasNext()) {
                Map.Entry<G,Integer> e = iter.next();
                if (isoTest.areIsomorphic(g, e.getKey())) {
                    int newCount = e.getValue() + count;
                    e.setValue(newCount);                   
                    // Move this graph from its current position to the front of
                    // the list by in hopes it will be accessed more frequently
                    iter.remove();
                    graphs.addFirst(e);
                    return newCount;
                }
            }
            // If the graph was not found and we can add new motifs, then do so.
            if (allowNewMotifs) {
                graphs.addFirst(new SimpleEntry<G,Integer>(g, count));
                return count;
            }
            else 
                return 0;
        }
    }    

    /**
     * Returns the count for graphs that are isomorphic to the provided graph.
     */
    public int getCount(G g) {
        Set<T> typeCounts = g.edgeTypes();
        LinkedList<Map.Entry<G,Integer>> graphs = typesToGraphs.get(typeCounts);
        if (graphs == null) 
            return 0;
        
        for (Map.Entry<G,Integer> e : graphs) {
            if (isoTest.areIsomorphic(g, e.getKey())) 
                return e.getValue();
        }
        return 0;
    }
    
    /**
     * {@inheritDoc}
     */
    public double getFrequency(G obj) {
        double count = getCount(obj);
        return (sum == 0) ? 0 : count / sum;
    }

    /**
     * {@inheritDoc}
     */
    public Set<G> items() {
//         LinkedList<Set<G>> sets = new ArrayLinkedList<Set<G>>(typesToGraphs.size());
//         for (LinkedList<Map.Entry<G,Integer>> m : typesToGraphs.values())
//             sets.add(m.keySet());
//         return new CombinedSet<G>(sets);

//         Set<G> set = new HashSet<G>();
//         for (LinkedList<Map.Entry<G,Integer>> m : typesToGraphs.values())
//             set.addAll(m.keySet());
//         return set;
        return new Items();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Map.Entry<G,Integer>> iterator() {
        List<Iterator<Map.Entry<G,Integer>>> iters = 
            new ArrayList<Iterator<Map.Entry<G,Integer>>>(typesToGraphs.size());
        for (LinkedList<Map.Entry<G,Integer>> list : typesToGraphs.values())
            iters.add(list.iterator());
        return new CombinedIterator<Map.Entry<G,Integer>>(iters);
    }
    
    /**
     * {@inheritDoc}
     */
    public G max() {
        int maxCount = -1;
        G max = null;
        for (LinkedList<Map.Entry<G,Integer>> graphs : typesToGraphs.values()) { 
            for (Map.Entry<G,Integer> e : graphs) {
                if (e.getValue() > maxCount) {
                    maxCount = e.getValue();
                    max = e.getKey();
                }
            }
        }
        return max;
    }

    /**
     * {@inheritDoc}
     */
    public G min() {
        int minCount = Integer.MAX_VALUE;
        G min = null;
        for (LinkedList<Map.Entry<G,Integer>> graphs : typesToGraphs.values()) {
            for (Map.Entry<G,Integer> e : graphs) {
                if (e.getValue() < minCount) {
                    minCount = e.getValue();
                    min = e.getKey();
                }
            }
        }
        return min;
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        typesToGraphs.clear();
        sum = 0;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        int sz = 0;
        for (LinkedList<Map.Entry<G,Integer>> m : typesToGraphs.values())
            sz += m.size();
        return sz;
    }

    /**
     * {@inheritDoc}
     */
    public int sum() {
        return sum;
    }

    class Items extends AbstractSet<G> {

        public boolean contains(G graph) {
            Set<T> typeCounts = graph.edgeTypes();
            LinkedList<Map.Entry<G,Integer>> graphs = typesToGraphs.get(typeCounts);
            if (graphs == null)
                return false;
            for (Map.Entry<G,Integer> e : graphs) {
                if (e.getKey().equals(graph))
                    return true;
            }
            return false;
        }

        public Iterator<G> iterator() {
            return new MotifIter();
        }

        public int size() {
            return TypedMotifCounter.this.size();
        }
        
        private class MotifIter implements Iterator<G> {

            Iterator<LinkedList<Map.Entry<G,Integer>>> graphs;

            Iterator<Map.Entry<G,Integer>> curIter;

            public MotifIter() {
                graphs = typesToGraphs.values().iterator();
                advance();
            }

            private void advance() {
                while ((curIter == null || !curIter.hasNext()) 
                           && graphs.hasNext())
                    curIter = graphs.next().iterator();
            }

            public boolean hasNext() {
                return curIter.hasNext();
            }

            public G next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                Map.Entry<G,Integer> e = curIter.next();
                advance();
                return e.getKey();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
        
    }
}