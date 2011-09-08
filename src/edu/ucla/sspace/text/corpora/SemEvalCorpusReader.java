/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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
import edu.ucla.sspace.text.Stemmer;
import edu.ucla.sspace.text.StringDocument;
import edu.ucla.sspace.text.EnglishStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.Reader;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.helpers.DefaultHandler;


/**
 * Reads the xml corpus files for the SemEval 2010 Word Sense Induction task,
 * available <a
 * href=http://semeval2.fbk.eu/semeval2.php?location=tasks#T2">here</a>.  Each
 * file contains all of the contexts for a single word.  The xml files should be
 * unchanged from their original format.  
 *
 * </p>
 *
 * This {@link CorpusReader} returns documents in the following format:
 *
 * </br>
 *
 * word_instance_id text ... ||| *focus_word* text ... 
 *
 * </br>
 *
 * This is particularly neccesary for the evaluating against the SemEval testing
 * framework which requires the focus word information and the instance id
 * infomration.
 *
 * </p>
 *
 * Note that this is implemented as a {@link DefaultHandler} for a {@link
 * SAXParser} due to difficult nature of the SemEval WSI xml format.  Line based
 * methods do not work as the entire xml document is contained on a single line.
 * Furthermore, the test set has an addition nested tag that specifies the
 * target sentence.  This information is discarded as it is does not specify the
 * focus word in each context.  Instead, this lemmatizes each word until it
 * finds a context word that matches the lemmatized version of the instance id.
 *
 * @author Keith Stevens
 */
public class SemEvalCorpusReader extends DefaultHandler 
                                 implements CorpusReader<Document> {

    /**
     * {@inheritDoc}
     */
    public Iterator<Document> read(Reader reader) {
        SAXParserFactory saxfac = SAXParserFactory.newInstance();
        saxfac.setValidating(false);

        // Ignore a number of xml features, like dtd grammars and validation.
        try {
            saxfac.setFeature("http://xml.org/sax/features/validation", false);
            saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            saxfac.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            saxfac.setFeature("http://xml.org/sax/features/external-general-entities", false);
            saxfac.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            SAXParser saxParser = saxfac.newSAXParser();
            SemEvalHandler handler = new SemEvalHandler();
            saxParser.parse(new InputSource(reader), handler);
            return handler.contexts.iterator();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (SAXNotRecognizedException e1) {
            throw new RuntimeException(e1);
        }catch (SAXNotSupportedException e1) {
            throw new RuntimeException(e1);
        } catch (ParserConfigurationException e1) {
            throw new RuntimeException(e1);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

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

    public class SemEvalHandler extends DefaultHandler {

        /**
         * The list of processed contexts.
         */
        List<Document> contexts;

        /**
         * A boolean marker signifying that the parser is in the context tag.
         */
        private boolean inContext;

        /**
         * The current instance id.
         */
        private String instanceId;

        /**
         * The current instance word's lemma.
         */
        private String lemma;

        /**
         * A {@link StringBuilder} for forming the current context.
         */
        private StringBuilder context;

        /**
         * A stemmer for discovering the focus word of each context.
         */
        private final Stemmer stemmer;

        public SemEvalHandler() {
            contexts = new ArrayList<Document>();
            inContext = false;
            context = new StringBuilder();
            stemmer = new EnglishStemmer();
        }

        /**
         * Extracts the instance id for the current context.
         */
        @Override
        public void startElement(String uri, String localName, 
                                 String name, Attributes atts)
                throws SAXException {
            if (!name.endsWith(".train") &&
                !name.endsWith(".test") &&
                !inContext) {
                inContext = true;
                instanceId = name;
                String[] wordPosId = instanceId.split("\\.");
                lemma = stemmer.stem(wordPosId[0]);
            }
        }

        /**
         * Ends the current context and stores all of the information in {@code
         * contexts}.
         */
        @Override
        public void endElement(String uri, String localName, String name)
                throws SAXException {
            if (name.equals(instanceId)) {
                inContext = false;


                String[] tokens = context.toString().split("\\s+");
                context.setLength(0);
                context.append(instanceId).append(" ");
                for (int i = 0; i < tokens.length; ++i) {
                    String stem = stemmer.stem(tokens[i]);
                    if (stem.equals(lemma)) {
                        context.append("|||| ");
                        tokens[i] = lemma;
                    }
                    context.append(tokens[i]).append(" ");
                }
                contexts.add(new StringDocument(context.toString()));
                context.setLength(0);
            }
        }

        /**
         * Appends the text to the current context.
         */
        @Override
        public void characters(char[] ch, int start, int length)
                throws SAXException {
            if (inContext)
                context.append(new String(ch, start, length));
        }
    }
}
