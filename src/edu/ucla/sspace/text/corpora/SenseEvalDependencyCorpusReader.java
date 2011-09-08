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

import edu.ucla.sspace.text.CorpusReader;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.StringDocument;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.Reader;

import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;


/**
 * A corpus reader for the SenseEvalDependency corpus.  Individual files will be
 * traversed using an xml parser.  Each of the sentences composing the context
 * of a word instance will be returned as a single {@link Document}.
 *
 * This {@link CorpusReader} returns documents in the following format:
 *
 * </br>
 *
 * word_instance_id focus_word text ...
 *
 * </br>
 *
 * This is particularly neccesary for the evaluating against the SemEval testing
 * framework which requires the focus word information and the instance id
 *
 * @author Keith Stevens
 */
public class SenseEvalDependencyCorpusReader implements CorpusReader<Document> {

    /**
     * {@inheritDoc}
     */
    public Iterator<Document> read(File file) {
        try {
            return read(new FileReader(file));
        } catch (FileNotFoundException fnfe) {
            throw new IOError(fnfe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Document> read(Reader docReader) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document currentXmlDoc = db.parse(
                    new InputSource(docReader));
            return new SenseEvalIterator(
                    currentXmlDoc.getElementsByTagName("instance"));
        } catch (javax.xml.parsers.ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (org.xml.sax.SAXException saxe) {
            saxe.printStackTrace();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return null;
    }

    public class SenseEvalIterator implements Iterator<Document> {    

        /**
         * The list of instances in a file.
         */
        private NodeList instances;

        /**
         * The index of the next instance to parse.
         */
        private int currentNodeIndex;

        /**
         * The contents of the current document.
         */
        private String currentDoc;

        public SenseEvalIterator(NodeList instances) {
            this.instances = instances;
            currentDoc = null;
            currentNodeIndex = 0;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized boolean hasNext() {
            return currentDoc != null;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized Document next() {
            Document doc = new StringDocument(currentDoc);
            currentDoc = advance();
            return doc;
        }

        /**
         * Throws {@link UnsupportedOperationException} if called.
         */
        public synchronized void remove() {
            throw new UnsupportedOperationException("Remove not permitted.");
        }

        /**
         * Iterates over the instances in a file and appends the words to create
         * a new document.
         */
        protected String advance() {
            if (currentNodeIndex >= instances.getLength())
                return null;
            Element instance = (Element) instances.item(currentNodeIndex++);
            String instanceId = instance.getAttribute("id");
            String word = instance.getAttribute("name");
            String text = instance.getFirstChild().getNodeValue();

            return String.format("%s\n%s\n%s", instanceId, word, text);
        }
    }
}
