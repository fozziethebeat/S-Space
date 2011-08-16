/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
 * conditions therein.
 *
 * The C-Cat package is free software: you can redistribute it and/or modify
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
 * @author Keith Stevens
 */
public class SemEvalCorpusReader extends DefaultHandler 
                                 implements CorpusReader {

    List<String> contexts;
    Iterator<String> contextIter;
    private boolean inContext;

    private String instanceId;
    private StringBuilder context;

    private final Stemmer stemmer;

    public SemEvalCorpusReader() {
        contexts = new ArrayList<String>();
        inContext = false;
        stemmer = new EnglishStemmer();
        context = new StringBuilder();
    }

    public void initialize(Reader reader) {
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
            saxParser.parse(new InputSource(reader), this);
            contextIter = contexts.iterator();
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

    public void initialize(String fileName) {
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
            saxParser.parse(new File(fileName), this);
            contextIter = contexts.iterator();
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

    @Override
    public void startElement(String uri, String localName, 
                             String name, Attributes atts)
            throws SAXException {
        if (!name.endsWith(".train") && !name.endsWith(".test") && !inContext) {
            inContext = true;
            instanceId = name;
        }
    }

    @Override
    public void endElement(String uri, String localName, String name)
            throws SAXException {
        if (name.equals(instanceId)) {
            inContext = false;

            String[] wordPosId = instanceId.split("\\.");
            String lemma = stemmer.stem(wordPosId[0]);

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
            contexts.add(context.toString());
            context.setLength(0);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        if (inContext)
            context.append(new String(ch, start, length));
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return contextIter.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    public Document next() {
        return new StringDocument(contextIter.next());
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove documents");
    }
}

