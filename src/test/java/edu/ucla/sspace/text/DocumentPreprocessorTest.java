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

package edu.ucla.sspace.text;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class DocumentPreprocessorTest {

  @Test public void punktSpaceTest() {
    String[] wordList = {"how", "much", "wood", "would", "a", "woodchuck", "chuck", "if", ",", "?", ".", "as", "much"};
    String testDocument = "How much wood would a woodchuck chuck, if a woodchuck could chuck wood? As much wood as a woodchuck would, if a woodchuck could chuck wood.";
    String resultDocument = "how much wood would a woodchuck chuck , if a woodchuck could chuck wood ? as much wood as a woodchuck would , if a woodchuck could chuck wood .";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    String result = testProcessor.process(testDocument);
    assertEquals(resultDocument, result);
  }

  @Test public void convertEmailNumTest() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word :] be myusername@ucla.edu a b c d e f g or a 12345";
    String expectedResult = "can a word <emote> be <url> a b c d e f g or a <num>";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument));
  }

  @Test public void convertUrlTest() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word :] be http://cs.ucla.edu a b c d e f g or a 12345";
    String expectedResult = "can a word <emote> be <url> a b c d e f g or a <num>";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument));
  }

  @Test public void removeNonWords() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word <b>be</b> <script> / fqhegas a b c d e f g or a </html>";
    String expectedResult = "can a word be <slash> a b c d e f g or a ";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument, true));
  }

  @Test public void stripHtmlTest() {
    String[] wordList = {"word", "can", "a", "be", "a", "b", "c", "d", "e", "f", "g", "or"};
    String testDocument = "can a word <b>be</b> <script> / a b c d e f g or a </html>";
    String expectedResult = "can a word be <slash> a b c d e f g or a";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor(wordList);
    assertEquals(expectedResult, testProcessor.process(testDocument));
  }

  @Test public void stripLongWords() {
    String testDocument = "can a word be a b c d e f g or a superfragalisticexpaliadouciousmagicake";
    String expectedResult = "can a word be a b c d e f g or a";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor();
    assertEquals(expectedResult, testProcessor.process(testDocument));
  }

  @Test public void convertDollarTest() {
    String testDocument = "can a word be a b c d e f g or a $5";
    String expectedResult = "can a word be a b c d e f g or a <num> dollars";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor();
    assertEquals(expectedResult, testProcessor.process(testDocument));
    testDocument = "can a word be  $5notanumber";
    expectedResult = "can a word be";
    assertEquals(expectedResult, testProcessor.process(testDocument));
  }

  /*
  @Test public void duplicateDocTest() {
    String testDocument = "can a word be a b c d e f g or a superfragalisticexpaliadouciousmagicake";
    String expectedResult = "can a word be a b c d e f g or a";
    DocumentPreprocessor testProcessor = new DocumentPreprocessor();
    assertEquals(expectedResult, testProcessor.process(testDocument));
    assertEquals("", testProcessor.process(testDocument));
  }
  */

  @Test public void noArgChange() {
    String testDocument = "can a word be a b c d e f g or a superfragalisticexpaliadouciousmagicake";
    String resultDocument = new String(testDocument);
    DocumentPreprocessor testProcessor = new DocumentPreprocessor();
    testProcessor.process(testDocument);
    assertEquals(resultDocument, testDocument);
  }

  public static junit.framework.Test suite() {
    return new junit.framework.JUnit4TestAdapter(DocumentPreprocessorTest.class);
  }
}
