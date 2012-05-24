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

import java.util.*;

import edu.ucla.sspace.util.OpenIntSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests for the {@link SimpleGraphIterator}.
 */
public class SimpleGraphIteratorTests { 

    @Test public void testConstructor() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        
        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1", i, j));
        }    

        SimpleGraphIterator<String,DirectedTypedEdge<String>> iter = 
            new SimpleGraphIterator<String,DirectedTypedEdge<String>>(g, 3);
    }

    @Test(expected=IllegalArgumentException.class) public void testConstructorNonpositive() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1", i, j));
        }    

        SimpleGraphIterator<String,DirectedTypedEdge<String>> iter = 
            new SimpleGraphIterator<String,DirectedTypedEdge<String>>(g, -1);
    }

    @Test(expected=NullPointerException.class) public void testConstructorNull() {
        SimpleGraphIterator<String,DirectedTypedEdge<String>> iter = 
            new SimpleGraphIterator<String,DirectedTypedEdge<String>>(null, 10);
    }

    @Test(expected=IllegalArgumentException.class) public void testConstructorSizeTooLarge() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1", i, j));
        }    

        SimpleGraphIterator<String,DirectedTypedEdge<String>> iter = 
            new SimpleGraphIterator<String,DirectedTypedEdge<String>>(g, 20);
    }

    @Test public void testSimpleExample() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        g.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 2));
        g.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 3));
        g.add(new SimpleDirectedTypedEdge<String>("type-1", 2, 3));
        g.add(new SimpleDirectedTypedEdge<String>("type-2", 1, 2));

        SimpleGraphIterator<String,DirectedTypedEdge<String>> iter = 
            new SimpleGraphIterator<String,DirectedTypedEdge<String>>(g, 3);
        int numSimpleGraphs = 0;
        boolean sawType1 = false; 
        boolean sawType2 = false; 
        while (iter.hasNext()) {
            Multigraph<String,DirectedTypedEdge<String>> simple = iter.next();
            assertTrue(simple.contains(1));
            assertTrue(simple.contains(2));
            assertTrue(simple.contains(3));
            Set<String> types = simple.edgeTypes();
            if (types.size() == 1) {
                assertTrue(types.contains("type-1"));
                assertFalse(sawType1);
                sawType1 = true;
            }
            else if (types.size() == 2) {
                assertEquals(g.edgeTypes(), types);
                assertFalse(sawType2);
                sawType2 = true;
            }
            else 
                assertFalse(true);
            numSimpleGraphs++;
        }
        assertEquals(2, numSimpleGraphs);

        g.add(new SimpleDirectedTypedEdge<String>("type-2", 1, 3));
        iter = new SimpleGraphIterator<String,DirectedTypedEdge<String>>(g, 3);
        numSimpleGraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSimpleGraphs++;
        }
        assertEquals(4, numSimpleGraphs);

        g.add(new SimpleDirectedTypedEdge<String>("type-2", 2, 3));
        iter = new SimpleGraphIterator<String,DirectedTypedEdge<String>>(g, 3);
        numSimpleGraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSimpleGraphs++;
        }
        assertEquals(8, numSimpleGraphs);
    }

}