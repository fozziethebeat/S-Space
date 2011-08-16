package edu.ucla.sspace.text.corpora;

import edu.ucla.sspace.text.CorpusReader;
import edu.ucla.sspace.text.Document;

import java.io.StringReader;
import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class PukWacCorpusReaderTest {

    public static final String TEST_TEXT =
        "<text>\n" +
        "<s>\n" +
        "the blah blah blah\n" +
        "chicken blah blah blah\n" +
        "ate blah blah blah\n" +
        "a blah blah blah\n" +
        "dog blah blah blah\n" +
        ". blah blah blah\n" +
        "</s>\n" +
        "<s>\n" +
        "bob blah blah blah\n" +
        "rocks blah blah blah\n" +
        "! blah blah blah\n" +
        "</s>\n" +
        "</text>\n" +
        "<text>\n" +
        "<s>\n" +
        "blah blah blah\n" +
        "bloh blah blah\n" +
        "</s>\n" +
        "</text>\n";

    @Test public void testIterator() throws Exception {
        CorpusReader reader = new PukWacCorpusReader();
        reader.initialize(new StringReader(TEST_TEXT));
        Iterator<Document> docIter = reader;
        assertTrue(docIter.hasNext());
        assertEquals("the chicken ate a dog . bob rocks ! ",
                     docIter.next().reader().readLine());
        assertTrue(docIter.hasNext());
        assertEquals("blah bloh ",
                     docIter.next().reader().readLine());
        assertFalse(docIter.hasNext());
    }
}

