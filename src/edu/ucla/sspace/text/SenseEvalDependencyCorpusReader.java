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

import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * A corpus reader for the SenseEvalDependency corpus.  Individual files will be traversed
 * using an xml parser.  Each of the sentences composing the context of a word
 * instance will be returned as a single {@link Document}.
 */
public class SenseEvalDependencyCorpusReader extends DirectoryCorpusReader {

    /**
     * The list of instances in a file.
     */
    private NodeList instances;

    /**
     * The index of the next instance to parse.
     */
    private int currentNodeIndex;

    /**
     * Creates a new {@link SenseEvalDependencyCorpusReader} with the given
     * filename as the senseEval07 dependency xml file.
     */
    public SenseEvalDependencyCorpusReader(String corpusFileName) {
        super(corpusFileName);
        init();
    }

    /**
     * {@inheritDoc}
     */
    protected void setupCurrentDoc(String currentDocName) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            System.out.println(currentDocName);
            org.w3c.dom.Document currentXmlDoc = db.parse(currentDocName);
            instances = currentXmlDoc.getElementsByTagName("instance");
            currentNodeIndex = 0;
        } catch (javax.xml.parsers.ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (org.xml.sax.SAXException saxe) {
            saxe.printStackTrace();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Iterates over the instances in a file and appends the words to create a
     * new document.
     */
    protected String advanceInDoc() {
        if (currentNodeIndex >= instances.getLength())
            return null;
        Element instance = (Element) instances.item(currentNodeIndex++);
        String instanceId = instance.getAttribute("id");
        String word = instance.getAttribute("name");
        String text = instance.getFirstChild().getNodeValue();

        return String.format("%s\n%s\n%s", instanceId, word, text);
    }
}
