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

public class IteratorFactoryTests {

    @Test public void testNoConfig() {
	Iterator<String> it = IteratorFactory.tokenize(getReader());
	assertEquals("this", it.next());
	assertEquals("is", it.next());
	assertEquals("my", it.next());
	assertEquals("example", it.next());
	assertEquals("sentence", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testWithFilter() throws IOException {
	File validTokens = createFileWithText("this\nmy\nexample\nsentence");
	String filterProp = "include=" + validTokens.getAbsolutePath();
	Properties props = new Properties();
	props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY, filterProp);
	IteratorFactory.setProperties(props);
	Iterator<String> it = IteratorFactory.tokenize(getReader());
	assertEquals("this", it.next());
	assertEquals("my", it.next());
	assertEquals("example", it.next());
	assertEquals("sentence", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testWithCompounds() throws IOException {
	File compounds = createFileWithText("example sentence");
	Properties props = new Properties();
	props.setProperty(IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
			  compounds.getAbsolutePath());
	IteratorFactory.setProperties(props);
	Iterator<String> it = IteratorFactory.tokenize(getReader());
	assertEquals("this", it.next());
	assertEquals("is", it.next());
	assertEquals("my", it.next());
	assertEquals("example sentence", it.next());
	assertFalse(it.hasNext());
    }

    @Test public void testWithCompoundsAndFilter() throws IOException {
	File validTokens = createFileWithText("this\nmy\nexample sentence");
	String filterProp = "include=" + validTokens.getAbsolutePath();
	Properties props = new Properties();
	props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY, filterProp);

	File compounds = createFileWithText("example sentence");
	props.setProperty(IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
			  compounds.getAbsolutePath());

	IteratorFactory.setProperties(props);
	Iterator<String> it = IteratorFactory.tokenize(getReader());
	assertEquals("this", it.next());
	assertEquals("my", it.next());
	assertEquals("example sentence", it.next());
	assertFalse(it.hasNext());
    }

    public static File createFileWithText(String text) throws IOException {
	File tmp = File.createTempFile("test", ".txt");
	PrintWriter pw = new PrintWriter(tmp);
	pw.println(text);
	pw.close();
	return tmp;
    }

    public static BufferedReader getReader() {
	return new BufferedReader(new StringReader("this is my example sentence"));
    }
}
