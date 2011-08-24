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

package edu.ucla.sspace.text.corpora;

import edu.ucla.sspace.text.DirectoryCorpusReader;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.DocumentPreprocessor;
import edu.ucla.sspace.text.StringDocument;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.sql.Timestamp;

import java.util.Iterator;


/**
 * A {@code DirectoryCorpusReader} for the bloglines corpus.
 *
 * @author Keith Stevens
 */
public class BloglinesCorpusReader extends DirectoryCorpusReader<Document> {

    /**
     * Constructs a new {@link BloglinesCorpusReader} that uses no preprocessing
     * before documents are returned.
     */
    public BloglinesCorpusReader() {
        super();
    }

    /**
     * Constructs a new {@link BloglinesCorpusReader} that uses {@link
     * preprocessor} to clean documents before they are returned.
     */
    public BloglinesCorpusReader(DocumentPreprocessor preprocessor) {
        super(preprocessor);
    }

    /**
     * {@inheritDoc}
     */
    protected Iterator<Document> corpusIterator(Iterator<File> files) {
        return new BloglinesIterator(files);
    }

    public class BloglinesIterator extends BaseFileIterator {

        public BloglinesIterator(Iterator<File> files) {
            super(files);
        }

        /**
         * A reader for extracting content from the bloglines corpus.
         */
        private BufferedReader bloglinesReader;

        /**
         * Sets up a {@code BufferedReader} to read through a single file with
         * multiple blog entries.
         */
        protected void setupCurrentDoc(File currentDocName) {
            try {
                bloglinesReader =
                    new BufferedReader(new FileReader(currentDocName));
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        /**
         * Iterates over the utterances in a file and appends the words to
         * create a new document.
         */
        protected Document advanceInDoc() {
            String line = null;
            StringBuilder content = null;
            boolean inContent = false;
            try {
                // Read through a single content block, and possibly a the
                // timestamp, to extract a single document.
                while ((line = bloglinesReader.readLine()) != null) {
                    // If the line contains the starting tag of content extract
                    // as much text as possible.
                    if (line.contains("<content>")) {
                        // Extract the start of a content node.  If the previous
                        // content, updated pair was incomplete, i.e. updated
                        // had no value, this will overwrite the previous
                        // content value.
                        int startIndex = line.indexOf(">")+1;
                        int endIndex = line.lastIndexOf("<");
                        // If the line contains all of the text then just return
                        // that substring.
                        if (endIndex > startIndex) {
                            String extractedContent = 
                                line.substring(startIndex, endIndex);
                            extractedContent = cleanDoc(extractedContent);
                            return new StringDocument(extractedContent);
                        }
                        // Otherwise create a new builder and everything
                        // appearing after the content tag.
                        else  {
                            content = new StringBuilder(line.substring(
                                        startIndex));
                            inContent = true;
                        }
                    } else if (line.contains("</content>")) {
                        inContent = false;
                        // If this is the end of the content, extract everything
                        // before it and return the total amount of text
                        // extracted.
                        int endIndex = line.lastIndexOf("<");
                        content.append(line.substring(0, endIndex));

                        return new StringDocument(cleanDoc(content.toString()));
                    } else if (line.contains("<updated>") && content != null) {
                        // When the line has an updated tag and content is not
                        // null, we need to extract the date time and prepend it
                        // to the content.
                        int startIndex = line.indexOf(">")+1;
                        int endIndex = line.lastIndexOf("<");
                        String date = line.substring(startIndex, endIndex);
                        long dateTime = date.equals("")
                            ? 0 :
                            Timestamp.valueOf(date).getTime();
                        String doc = String.format(
                                "%d %s", dateTime,
                                cleanDoc(content.toString()));
                        return new StringDocument(doc);
                    } else if (inContent && content != null) {
                        // If the content builder has been created, we know this
                        // line contains content.  Add it to the builder.
                        content.append(line);
                    }
                }
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
            // There was no content left in this document.
            return null;
        }
    }
}
