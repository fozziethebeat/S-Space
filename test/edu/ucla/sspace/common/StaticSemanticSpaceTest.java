/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.common;

import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.text.*; 
import edu.ucla.sspace.util.*;
import edu.ucla.sspace.vector.*;

import java.io.*;

import org.junit.*;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link StaticSemanticSpace} class.
 */
public class StaticSemanticSpaceTest {

    private final DummySemanticSpace control;

    public StaticSemanticSpaceTest() {
	control = new DummySemanticSpace();
	control.setVector("cow", new DenseVector(new double[] {1, 0, 0, 0}));
	control.setVector("dog", new DenseVector(new double[] {0, 1, 0, 0}));
	control.setVector("ear", new DenseVector(new double[] {0, 0, 1, 0}));
	control.setVector("fig", new DenseVector(new double[] {0, 0, 0, 1}));
	control.setVector("git", new DenseVector(new double[] {1, 1, 0, 0}));
	control.setVector("hat", new DenseVector(new double[] {1, 0, 1, 0}));
	control.setVector("its", new DenseVector(new double[] {1, 0, 0, 1}));
    }

    @Test public void testText() throws Exception { 
	File textFile = File.createTempFile("test-text",".sspace");
	textFile.deleteOnExit();
	SemanticSpaceIO.save(control, textFile, SSpaceFormat.TEXT);
	SemanticSpace onDisk = new StaticSemanticSpace(textFile);
	
	assertEquals(control.getWords().size(), onDisk.getWords().size());
	assertTrue(control.getWords().containsAll(onDisk.getWords()));
	for (String word : control.getWords()) {
	    assertEquals(VectorIO.toString(control.getVector(word)),
			 VectorIO.toString(onDisk.getVector(word)));
	}	
    }

    @Test public void testSparseText() throws Exception { 
	File sparseTextFile = File.createTempFile("test-sparse-text",".sspace");
	sparseTextFile.deleteOnExit();
	SemanticSpaceIO.save(control, sparseTextFile, SSpaceFormat.SPARSE_TEXT);
	SemanticSpace onDisk = new StaticSemanticSpace(sparseTextFile);
	
	assertEquals(control.getWords().size(), onDisk.getWords().size());
	assertTrue(control.getWords().containsAll(onDisk.getWords()));
	for (String word : control.getWords()) {
	    assertEquals(VectorIO.toString(control.getVector(word)),
			 VectorIO.toString(onDisk.getVector(word)));
	}	
    }

    @Test public void testBinary() throws Exception { 
	File binaryFile = File.createTempFile("test-binary",".sspace");
 	binaryFile.deleteOnExit();
	SemanticSpaceIO.save(control, binaryFile, SSpaceFormat.BINARY);
	SemanticSpace onDisk = new StaticSemanticSpace(binaryFile);
	
	assertEquals(control.getWords().size(), onDisk.getWords().size());
	assertTrue(control.getWords().containsAll(onDisk.getWords()));
	for (String word : control.getWords()) {
	    assertEquals(VectorIO.toString(control.getVector(word)),
			 VectorIO.toString(onDisk.getVector(word)));
	}	
    }

    @Test public void testSparseBinary() throws Exception { 
	File sparseBinaryFile = File.createTempFile("test-sparse-binary",".sspace");
	sparseBinaryFile.deleteOnExit();
	SemanticSpaceIO.save(control, sparseBinaryFile, SSpaceFormat.SPARSE_BINARY);
	SemanticSpace onDisk = new StaticSemanticSpace(sparseBinaryFile);
	
	assertEquals(control.getWords().size(), onDisk.getWords().size());
	assertTrue(control.getWords().containsAll(onDisk.getWords()));
	for (String word : control.getWords()) {
	    assertEquals(VectorIO.toString(control.getVector(word)),
			 VectorIO.toString(onDisk.getVector(word)));
	}	
    }

    public static junit.framework.Test suite() {
	return new junit.framework.JUnit4TestAdapter(StaticSemanticSpaceTest.class);
    }
}
