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
public class PukWacDependencyCorpusReaderTest {

    public static final String FIRST_SENTENCE = 
        "1   Mr. _   NNP NNP _   2   NMOD    _   _\n" +
        "2   Holt    _   NNP NNP _   3   SBJ _   _\n" +
        "3   is  _   VBZ VBZ _   0   ROOT    _   _\n";

    public static final String SECOND_SENTENCE = 
        "4   a   _   DT  DT  _   5   NMOD    _   _\n" +
        "5   columnist   _   NN  NN  _   3   PRD _   _\n" +
        "6   for _   IN  IN  _   5   NMOD    _   _\n" +
        "7   the _   DT  DT  _   9   NMOD    _   _\n" +
        "8   Literary    _   NNP NNP _   9   NMOD    _   _\n";

    public static final String THIRD_SENTENCE = 
        "9   Review  _   NNP NNP _   6   PMOD    _   _\n" +
        "10  in  _   IN  IN  _   9   ADV _   _\n" +
        "11  London  _   NNP NNP _   10  PMOD    _   _\n" +
        "12  .   _   .   .   _   3   P   _   _\n";

    public static final String TEST_TEXT =
        "<text>\n" +
        "<s>\n" +
        FIRST_SENTENCE +
        "</s>\n" +
        "<s>\n" +
        SECOND_SENTENCE +
        "</s>\n" +
        "</text>\n" +
        "<text>\n" +
        "<s>\n" +
        THIRD_SENTENCE +
        "</s>\n" +
        "</text>\n";

    @Test public void testIterator() throws Exception {
        CorpusReader reader = new PukWacDependencyCorpusReader();
        reader.initialize(new StringReader(TEST_TEXT));
        Iterator<Document> docIter = reader;
        assertTrue(docIter.hasNext());
        assertEquals(FIRST_SENTENCE, readAll(docIter.next()));
        assertTrue(docIter.hasNext());
        assertEquals(SECOND_SENTENCE, readAll(docIter.next()));
        assertTrue(docIter.hasNext());
        assertEquals(THIRD_SENTENCE, readAll(docIter.next()));
        assertFalse(docIter.hasNext());
    }

    private static String readAll(Document doc) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String line = null; (line = doc.reader().readLine()) != null; )
            sb.append(line).append("\n");
        return sb.toString();
    }
}
