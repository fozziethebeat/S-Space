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
public class BloglinesCorpusReader extends DirectoryCorpusReader {

    /**
     * A reader for extracting content from the bloglines corpus.
     */
    private BufferedReader bloglinesReader;

    /**
     * Set to true if timestamps should be included in the docuemnts extracted.
     */
    private final boolean useTimestamps;

    /**
     * Creates a new {@code BloglinesCorpusReader} from a given file name,
     * without time stamps.
     */
    public BloglinesCorpusReader(String corpusFileName) {
        this(corpusFileName, false);
    }

    /**
     * Creates a new {@code BloglinesCorpusReader} from a given file name that
     * will include time stamps if {@code includeTimeStamps} is true.
     */
    public BloglinesCorpusReader(String corpusFileName,
                                 boolean includeTimeStamps) {
        super(corpusFileName);
        useTimestamps = includeTimeStamps;
        init();
    }

    /**
     * Sets up a {@code BufferedReader} to read through a single file with
     * multiple blog entries.
     */
    protected void setupCurrentDoc(String currentDocName) {
        try {
            bloglinesReader =
                new BufferedReader(new FileReader(currentDocName));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Iterates over the utterances in a file and appends the words to create a
     * new document.
     */
    protected String advanceInDoc() {
        String line = null;
        StringBuilder content = null;
        boolean inContent = false;
        try {
            // Read through a single content block, and possibly a the
            // timestamp, to extract a single document.
            while ((line = bloglinesReader.readLine()) != null) {
                // If the line contains the starting tag of content extract as
                // much text as possible.
                if (line.contains("<content>")) {
                    // Extract the start of a content node.  If the previous
                    // content, updated pair was incomplete, i.e. updated had no
                    // value, this will overwrite the previous content value.
                    int startIndex = line.indexOf(">")+1;
                    int endIndex = line.lastIndexOf("<");
                    // If the line contains all of the text then just return
                    // that substring.
                    if (endIndex > startIndex) {
                        String extractedContent = 
                            line.substring(startIndex, endIndex);
                        if (!useTimestamps) 
                            return cleanDoc(extractedContent);

                        content = new StringBuilder(extractedContent);
                    }
                    // Otherwise create a new builder and everything appearing
                    // after the content tag.
                    else  {
                        content = new StringBuilder(line.substring(startIndex));
                        inContent = true;
                    }
                } else if (line.contains("</content>")) {
                    inContent = false;
                    // If this is the end of the content, extract everything
                    // before it and return the total amount of text extracted.
                    int endIndex = line.lastIndexOf("<");
                    content.append(line.substring(0, endIndex));

                    // If timestamps are desired, continue reading the document.
                    // The updated line should be immediately after contents.
                    if (useTimestamps) {
                        System.out.println("TIMESTAMPS");
                        continue;
                    }
                    return cleanDoc(content.toString());
                } else if (line.contains("<updated>") && content != null) {
                    // When the line has an updated tag and content is not null,
                    // we need to extract the date time and prepend it to the
                    // content.
                    int startIndex = line.indexOf(">")+1;
                    int endIndex = line.lastIndexOf("<");
                    String date = line.substring(startIndex, endIndex);
                    long dateTime = date.equals("")
                        ? 0 :
                        Timestamp.valueOf(date).getTime();
                    return String.format("%d %s",
                                         dateTime,
                                         cleanDoc(content.toString()));
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
