/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.text;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TestCompoundWordIterator {

    private Set<String> myCat;

    public TestCompoundWordIterator() {
	myCat = new HashSet<String>();
	myCat.add("my cat");
    }

    @Test public void testNext() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my cat is big"));
	CompoundWordIterator it = new CompoundWordIterator(br, myCat);
	assertEquals("my cat", it.next());
	assertEquals("is", it.next());
	assertEquals("big", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testNextWithNewlines() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my\ncat\nis\nbig"));
	CompoundWordIterator it = new CompoundWordIterator(br, myCat);
	assertEquals("my cat", it.next());
	assertEquals("is", it.next());
	assertEquals("big", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testNextWithEmptyLeadingNewlines() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("\n\n\n\nmy\ncat\nis\nbig"));
	CompoundWordIterator it = new CompoundWordIterator(br, myCat);
	assertEquals("my cat", it.next());
	assertEquals("is", it.next());
	assertEquals("big", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testNextWithEmptyTrailingNewlines() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my\ncat\nis\nbig\n\n\n\n"));
	CompoundWordIterator it = new CompoundWordIterator(br, myCat);
	assertEquals("my cat", it.next());
	assertEquals("is", it.next());
	assertEquals("big", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testNextWithEmptyMiddleNewlines() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my\ncat\n\n\n\n\nis\nbig"));
	CompoundWordIterator it = new CompoundWordIterator(br, myCat);
	assertEquals("my cat", it.next());
	assertEquals("is", it.next());
	assertEquals("big", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testMultiple() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my cat my cat my cat my cat"));
	CompoundWordIterator it = new CompoundWordIterator(br, myCat);
	for (int i = 0; i < 4; ++i)
	    assertEquals("my cat", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testDanglingStart() {
	BufferedReader br = 
	    new BufferedReader(new StringReader("my cat my cat my cat my"));
	CompoundWordIterator it = new CompoundWordIterator(br, myCat);
	for (int i = 0; i < 3; ++i)
	    assertEquals("my cat", it.next());
	assertEquals("my", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testCompoundsWithSameStart() {

	BufferedReader br = 
	    new BufferedReader(new StringReader("my cat my dog my chicken"));
	Set<String> compounds = new HashSet<String>();
	compounds.add("my cat");
	compounds.add("my dog");
	CompoundWordIterator it = new CompoundWordIterator(br, compounds);
 	assertEquals("my cat", it.next());
 	assertEquals("my dog", it.next());
	assertEquals("my", it.next());
	assertEquals("chicken", it.next());
	assertFalse(it.hasNext());
    }

}