/*
 * Copyright 2012 David Jurgens
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

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.WaCKyDependencyExtractor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An iterator implementation that returns {@link Document} containg a single
 * dependency parsed sentence given a file in the <a
 * href="http://nextens.uvt.nl/depparse-wiki/DataFormat">CoNLL Format</a> which
 * is contained in the XML format provided in the WaCkypedia corpus.  See the <a
 * href="http://wacky.sslmit.unibo.it/doku.php?id=corpora">WaCky</a> group's
 * website for more information on the PukWaC.
 */
public class PukWaCDocumentIterator implements Iterator<LabeledParsedDocument> {

    /**
     * The extractor used to build trees from the PukWaC documents
     */
    private static final DependencyExtractor extractor = 
        new WaCKyDependencyExtractor();

    /**
     * The reader for accessing the file containing the documents
     */
    private final BufferedReader documentsReader;
    
    /**
     * The URL from which the current set of sentences has been drawn
     */
    private String currentSource;

    /**
     * The next document (sentence) in the corpus
     */
    private LabeledParsedDocument nextDoc;

    /**
     * Creates an {@code Iterator} over the file where each document returned
     * contains the sequence of dependency parsed words composing a sentence..
     *
     * @param documentsFile the name of the PukWaC file containing dependency
     *        parsed sentences in the <a
     *        href="http://nextens.uvt.nl/depparse-wiki/DataFormat">CoNLL
     *        Format</a> separated by XML tags for the sentences and articles
     *        from which they came
     *
     * @throws IOError if any error occurs when reading {@code documentsFile}
     */
    public PukWaCDocumentIterator(String documentsFile) {       
        try {
            documentsReader = new BufferedReader(new FileReader(documentsFile));
            advance();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    private void advance() throws IOException {
        nextDoc = null;
        StringBuilder sb = new StringBuilder();
        String line = null;
        next_doc:
        while ((line = documentsReader.readLine()) != null) {
            // The <text> tag contains the document id, so update where the
            // source of all the following sentences
            if (line.contains("<text")) {
                int start = line.indexOf("\"");
                if (start < 0)
                    continue;
                int end = line.indexOf("\"", start + 1);
                if (end < 0)
                    continue;
                currentSource = line.substring(start+1, end);                 
            }            
            // If the line indicates the start of a new sentence, then pull out
            // all parsed lines
            else if (line.equals("<s>")) {
                while ((line = documentsReader.readLine()) != null
                       && !line.equals("</s>")) {

                    // Unfortunately, the XML of the PukWaC is broken and some
                    // <text> elements are inside the <s> elements, so this code
                    // is needed to avoid putting those inside the CONLL data
                    if (line.contains("<text")) {
                        int start = line.indexOf("\"");
                        int end = line.indexOf("\"", start + 1);
                        if (end < 0) {
                            // Reset the contents of the current document so
                            // that if we managed to find another document, its
                            // contents aren't corrupted.
                            sb.setLength(0);
                            continue next_doc;
                        }
                        currentSource = line.substring(start+1, end);
                    }
                    else if (!line.contains("</text>"))
                        sb.append(line).append("\n");
                }
                
                nextDoc = new LabeledParsedStringDocument(
                    currentSource, extractor, sb.toString());
                break;
            }
        }        
    }

    /**
     * Returns {@code true} if there are more documents to return.
     */
    public boolean hasNext() {
        return nextDoc != null;
    }
    

    /**
     * Returns the next document from the file.
     */
    public LabeledParsedDocument next() {
        if (nextDoc == null)
            throw new NoSuchElementException("No further documents");
        LabeledParsedDocument next = nextDoc;
        while (true) {
            try {
                advance();
                break;
            } 
            // Case for if we didn't property read the line
            catch (IOException ioe) {
                throw new IOError(ioe);
            } 
            // Some lines in the PukWaC are corrupted due to missing characters.
            // We silently catch these errors here and try to advance further to
            // the next parse tree.  Note that if none further exist, advance
            // will still return and we will break from the loop.
            catch (Exception e) {

            }
        }
        return next;
    }        
    
    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
        throw new UnsupportedOperationException(
            "removing documents is not supported");
    }
}
