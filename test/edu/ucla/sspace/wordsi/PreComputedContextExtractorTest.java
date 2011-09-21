package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class PreComputedContextExtractorTest {

    @Test public void testLineExtractionAcceptAll() {
        String line = "cat 1.0,fluffy|3.2,juggle-1|3,z|";
        MockWordsi wordsi = new MockWordsi(null);
        ContextExtractor extractor = new PreComputedContextExtractor();
        extractor.processDocument(new BufferedReader(new StringReader(line)),
                                  wordsi);
        assertTrue(wordsi.called);
    }

    @Test public void testLineExtractionAcceptCat() {
        String line = "cat 1.0,fluffy|3.2,juggle-1|3,z|";
        MockWordsi wordsi = new MockWordsi("cat");
        ContextExtractor extractor = new PreComputedContextExtractor();
        extractor.processDocument(new BufferedReader(new StringReader(line)),
                                  wordsi);
        assertTrue(wordsi.called);
    }

    @Test public void testLineExtractionAcceptDog() {
        String line = "cat 1.0,fluffy|3.2,juggle-1|3,z|";
        MockWordsi wordsi = new MockWordsi("dog");
        ContextExtractor extractor = new PreComputedContextExtractor();
        extractor.processDocument(new BufferedReader(new StringReader(line)),
                                  wordsi);
        assertFalse(wordsi.called);
    }

    public class MockWordsi implements Wordsi {

        public String expectedWord;

        public boolean called;

        public MockWordsi(String expectedWord) {
            this.expectedWord = expectedWord;
            this.called = false;
        }

        public boolean acceptWord(String word) {
            return expectedWord == null || expectedWord.equals(word);
        }

        public void handleContextVector(String key1, String key2,
                                        SparseDoubleVector vector) {
            assertEquals("cat", key1);
            assertEquals("cat", key2);
            int[] nonZeros = vector.getNonZeroIndices();
            assertEquals(3, nonZeros.length);
            assertEquals(1.0, vector.get(0), .0001);
            assertEquals(3.2, vector.get(1), .0001);
            assertEquals(3.0, vector.get(2), .0001);
            called = true;
        }
    }
}
