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
 * Reads the xml corpus files for the SemEval 2010 Lexical Substition task,
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
 * </p>
 *
 * Note that this is implemented as a {@link DefaultHandler} for a {@link
 * SAXParser}.
 *
 * @author Keith Stevens
 */
public class SemEvalLexSubReader extends DefaultHandler 
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
         * A boolean marker signifying that the parser is in the head tag.
         */
        private boolean inHead;

        /**
         * A boolena marker signifying that the parser is in the lexelt tag.
         */
        private boolean inLexElement;

        /**
         * The current instance id.
         */
        private String instanceId;

        /**
         * The current instance lexical id
         */
        private String lexicalId;

        /**
         * A {@link StringBuilder} for forming the current context.
         */
        private StringBuilder context;

        public SemEvalHandler() {
            contexts = new ArrayList<Document>();
            inContext = false;
            inHead = false;
            inLexElement = false;
            context = new StringBuilder();
        }

        /**
         * Extracts the instance id for the current context.
         */
        @Override
        public void startElement(String uri, String localName, 
                                 String name, Attributes atts)
                throws SAXException {
            if (name.equals("lexelt")) {
                inLexElement = true;
                lexicalId = atts.getValue("item");
            } else if (name.equals("instance")) {
                instanceId = atts.getValue("id") + " ";
                context.append(lexicalId).append("_").append(instanceId);
            } else if (name.equals("context"))
                inContext = true;
            else if (name.equals("head"))
                inHead = true;
        }

        /**
         * Ends the current context and stores all of the information in {@code
         * contexts}.
         */
        @Override
        public void endElement(String uri, String localName, String name)
                throws SAXException {
            if (name.equals("head"))
                inHead = false;
            else if (name.equals("lexelt"))
                inLexElement = false;
            else if (name.equals("context")) {
                inContext = false;
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
            if (inHead)
                context.append(" |||| ").append(new String(ch,start,length));
        }
    }
}
