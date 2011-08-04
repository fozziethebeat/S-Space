/*
 * Copyright 2010 David Jurgens
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


import java.io.*;
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class TokenFilterTests {

    @Test public void testIncludeFromConfig() throws IOException {
        File toInclude = createFileWithText("include\nthree\nwords");
        String filterSpec = "include=" + toInclude.getAbsolutePath();
        TokenFilter filter = TokenFilter.loadFromSpecification(filterSpec);
        assertTrue(filter.accept("include"));
        assertTrue(filter.accept("three"));
        assertTrue(filter.accept("words"));
        assertFalse(filter.accept("foo"));
        assertFalse(filter.accept(""));
    }

    @Test public void testExcludeFromConfig() throws IOException {
        File toExclude = createFileWithText("include\nthree\nwords");
        String filterSpec = "exclude=" + toExclude.getAbsolutePath();
        TokenFilter filter = TokenFilter.loadFromSpecification(filterSpec);
        assertFalse(filter.accept("include"));
        assertFalse(filter.accept("three"));
        assertFalse(filter.accept("words"));
        assertTrue(filter.accept("foo"));
        assertTrue(filter.accept(""));
    }

    @Test public void testMultipleInclude() throws IOException {
        File toInclude = createFileWithText("include\nthree\nwords");
        File toInclude2 = createFileWithText("foo\nbar\nbaz");
        String filterSpec = "include=" + toInclude.getAbsolutePath() + ":"
            + toInclude2.getAbsolutePath();
        TokenFilter filter = TokenFilter.loadFromSpecification(filterSpec);
        assertTrue(filter.accept("include"));
        assertTrue(filter.accept("three"));
        assertTrue(filter.accept("words"));
        assertTrue(filter.accept("foo"));
        assertTrue(filter.accept("bar"));
        assertTrue(filter.accept("baz"));        
        assertFalse(filter.accept("fooo"));
        assertFalse(filter.accept("qux"));
        assertFalse(filter.accept("quux"));
    }

    @Test public void testLayeredExcludeFirst() throws IOException {
        File toExclude = createFileWithText("include");
        File toInclude = createFileWithText("include\nthree\nwords");
        String filterSpec = "exclude=" + toExclude.getAbsolutePath() + ",include="
            + toInclude.getAbsolutePath();
        TokenFilter filter = TokenFilter.loadFromSpecification(filterSpec);
        assertFalse(filter.accept("include"));
        assertTrue(filter.accept("three"));
        assertTrue(filter.accept("words"));
        assertFalse(filter.accept("foo"));
    }

    @Test public void testLayeredIncludeFirst() throws IOException {
        File toExclude = createFileWithText("include");
        File toInclude = createFileWithText("include\nthree\nwords");
        String filterSpec = "include=" + toInclude.getAbsolutePath() + ",exclude="
            + toExclude.getAbsolutePath();
        TokenFilter filter = TokenFilter.loadFromSpecification(filterSpec);
        assertFalse(filter.accept("include"));
        assertTrue(filter.accept("three"));
        assertTrue(filter.accept("words"));
        assertFalse(filter.accept("foo"));
    }

    
    public static BufferedReader getReader() {
	return new BufferedReader(new StringReader("this is my example sentence"));
    }

    public static File createFileWithText(String text) throws IOException {
	File tmp = File.createTempFile("test", ".txt");
        tmp.deleteOnExit();
	PrintWriter pw = new PrintWriter(tmp);
	pw.println(text);
	pw.close();
	return tmp;
    }
}
