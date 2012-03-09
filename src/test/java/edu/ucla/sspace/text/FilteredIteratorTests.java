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


import java.io.*;
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class FilteredIteratorTests {

    @Test public void testIncludeFromConfig() throws IOException {
        File toInclude = createFileWithText("include\nthree\nwords");
        String filterSpec = "include=" + toInclude.getAbsolutePath();
        TokenFilter filter = TokenFilter.loadFromSpecification(filterSpec);
        Iterator<String> it =
            new FilteredIterator("include foo bar three include baz words", filter);
        assertEquals("include", it.next());
        assertEquals("three", it.next());
        assertEquals("include", it.next());
        assertEquals("words", it.next());
        assertFalse(it.hasNext());
    }

    @Test public void testExcludeFromConfig() throws IOException {
        File toInclude = createFileWithText("include\nthree\nwords");
        String filterSpec = "exclude=" + toInclude.getAbsolutePath();
        TokenFilter filter = TokenFilter.loadFromSpecification(filterSpec);
        Iterator<String> it =
            new FilteredIterator("include foo bar three include baz words", filter);
        assertEquals("foo", it.next());
        assertEquals("bar", it.next());
        assertEquals("baz", it.next());
        assertFalse(it.hasNext());
    }
    
    public static BufferedReader getReader() {
	return new BufferedReader(new StringReader("this is my example sentence"));
    }

    public static File createFileWithText(String text) throws IOException {
	File tmp = File.createTempFile("test", ".txt");
	PrintWriter pw = new PrintWriter(tmp);
	pw.println(text);
	pw.close();
	return tmp;
    }
}
