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

package edu.ucla.sspace.util;

import java.io.*;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for {@link LineReader} 
 */
public class LineReaderTests {
    
    @Test public void testLine() throws Exception {
        File f = File.createTempFile("tmp",".txt");
        PrintWriter pw = new PrintWriter(f);
        pw.println("this");
        pw.println("is");
        pw.println("a");
        pw.println("sentence");
        pw.close();
        
        Iterator<String> lines = new LineReader(f).iterator();
        assertEquals("this", lines.next());
        assertEquals("is", lines.next());
        assertEquals("a", lines.next());
        assertEquals("sentence", lines.next());
        assertFalse(lines.hasNext());
        f.delete();
    }

    @Test public void testEmptyFile() throws Exception {
        File f = File.createTempFile("tmp",".txt");
        PrintWriter pw = new PrintWriter(f);
        pw.close();
        
        Iterator<String> lines = new LineReader(f).iterator();
        assertFalse(lines.hasNext());
        f.delete();
    }
}