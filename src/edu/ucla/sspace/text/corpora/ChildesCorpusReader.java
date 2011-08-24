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

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * A corpus reader for the Childes corpus.  Individual files will be traversed
 * using an xml parser.  Documents can be returned either as all of the
 * utterances in a file or a single utterance.
 */
public class ChildesCorpusReader extends DirectoryCorpusReader<Document> {

    /**
     * Constructs a new {@link ChildesCorpusReader} that uses no preprocessing
     * before documents are returned.
     */
    public ChildesCorpusReader() {
        super();
    }

    /**
     * Constructs a new {@link ChildesCorpusReader} that uses {@link
     * preprocessor} to clean documents before they  are returned.
     */
    public ChildesCorpusReader(DocumentPreprocessor preprocessor) {
        super(preprocessor);
    }

    /**
     * {@inheritDoc}
     */
    protected Iterator<Document> corpusIterator(Iterator<File> files) {
        return new ChildesFileIterator(files);
    }

    public class ChildesFileIterator extends BaseFileIterator {

        /**
         * The list of utterances in a file.
         */
        private NodeList utterances;

        /**
         * The index of the current node being traversed, if one utterance is
         * being considered a document.
         */
        private int currentNodeIndex;

        /**
         * True if each utterance in a file is considered a document.
         */
        private boolean oneUtterancePerDoc;

        /**
         * The {@link DocumentBuilder} used to parse xml files.
         */
        private final DocumentBuilder db;

        public ChildesFileIterator(Iterator<File> files) {
            super(files);
            oneUtterancePerDoc = true;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder d = null;
            try {
                d = dbf.newDocumentBuilder();
            } catch (javax.xml.parsers.ParserConfigurationException pce) {
                pce.printStackTrace();
            }
            db = d;
        }

        /**
         * Parses the content of the given file and extracts the set of
         * utterances.
         */
        protected void setupCurrentDoc(File currentDocName) {
            try {
                org.w3c.dom.Document currentXmlDoc = db.parse(currentDocName);
                utterances = currentXmlDoc.getElementsByTagName("u");
                currentNodeIndex = 0;
            } catch (org.xml.sax.SAXException saxe) {
                saxe.printStackTrace();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        /**
         * Iterates over the utterances in a file and appends the words to
         * create a new document.
         */
        protected Document advanceInDoc() {
            if (currentNodeIndex >= utterances.getLength())
                return null;

            StringBuilder utteranceBuilder = new StringBuilder();
            if (oneUtterancePerDoc)
                addTextFromUtterance(
                        (Element) utterances.item(currentNodeIndex++),
                        utteranceBuilder);
            else {
                for (int i = 0; i < utterances.getLength(); ++i) {
                    addTextFromUtterance((Element) utterances.item(i),
                                         utteranceBuilder);
                    utteranceBuilder.append(". ");
                }
            }
            return new StringDocument(utteranceBuilder.toString());
        }

        /**
         * Adds words from a single utterance to a string builder.
         */
        private void addTextFromUtterance(Element utterance,
                                          StringBuilder utteranceBuilder) {
            NodeList words = utterance.getElementsByTagName("w");
            // Iterate over the words and get just the word text.
            for (int j = 0; j < words.getLength(); ++j) {
                Element wordNode = (Element) words.item(j);
                String word = wordNode.getFirstChild().getNodeValue();
                utteranceBuilder.append(word).append(" ");
            }
        }
    }
}
