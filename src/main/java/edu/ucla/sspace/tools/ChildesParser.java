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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * A simple xml parser for the Childes corpus.  Words in each utterance will be
 * extracted from the XML and saved into a specified file.  The resulting
 * document may consist of all uterances in an XML file or a single utterance,
 * where a single xml file generates multiple documents.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
public class ChildesParser {

    /**
     * A writer for writing utterances.
     */
    private PrintWriter writer;

    /**
     * A writer for writing part of speech tags for words in Childes.
     */
    private PrintWriter posWriter;

    /**
     * A map from strings to their parts of speech tags.
     */
    private MultiMap<String, String> posTags;

    /**
     * {@code true} if the parser should generate augmented utterances from the
     * comments when parsing
     */
    private final boolean generateAugmented;

    /**
     * {@code true} if the parser should separate sentences with periods.
     */
    private final boolean separateByPeriod;

    /**
     * {@code true} if the parser should append pos tags to each token in the
     * corpus.  This is useful when the corpus needs the tags to be aligned with
     * the text.  The format of each token will be TOKEN-POS.
     */
    private final boolean appendPosTags;

    /**
     * {@code true} if the parser shold generate one document for all the text
     * processed.
     */
    private final boolean generateOneDoc;

    /**
     * Creates the {@code ChildesParser}.   The given file name will be used to
     * write the extracted words.
     */
    public ChildesParser(String outFile,
                         String posOutFile,
                         boolean generateAugmented,
                         boolean separateByPeriod,
                         boolean appendPosTags,
                         boolean generateOneDoc) {
        this.generateAugmented = generateAugmented;
        this.separateByPeriod = separateByPeriod;
        this.appendPosTags = appendPosTags;
        this.generateOneDoc = generateOneDoc;
        posTags = new HashMultiMap<String, String>();
        try {
            writer = new PrintWriter(outFile);
            posWriter = new PrintWriter(posOutFile);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Writes strings to the resulting file.
     */
    private synchronized void print(String output) {
        if (generateOneDoc) 
            writer.print(output);
        else 
            writer.println(output);
    }

    /**
     * Parses a single xml file.  If {@code utterancePerDoc} is true, each
     * utterance will be on a separate line, otherwise they will all be
     * concantanated, and separated by periods, and stored on a single line.
     */
    public void parseFile(File file, boolean utterancePerDoc) {
        try {
            // Build an xml document.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);

            // Extract all utterances.
            NodeList utterances = doc.getElementsByTagName("u");
            StringBuilder fileBuilder = new StringBuilder();
            for (int i = 0; i < utterances.getLength(); ++i) {
                // Extract all words from the utterance
                Element item = (Element) utterances.item(i);
                NodeList words = item.getElementsByTagName("w");
                StringBuilder utteranceBuilder = new StringBuilder();

                // Iterate over the words and get just the word text.
                List<String> wordsInUtterance =
                    new ArrayList<String>(words.getLength());
                for (int j = 0; j < words.getLength(); ++j) {
                    // Get the part of speech tag.
                    Element wordNode = (Element) words.item(j);
                    NodeList posNodeList = wordNode.getElementsByTagName("pos");
                    String word = wordNode.getFirstChild().getNodeValue();
                    if (posNodeList.getLength() > 0) {
                        Node posNode = 
                            posNodeList.item(0).getFirstChild().getFirstChild();
                        String pos = posNode.getNodeValue();
                        posTags.put(word, pos);
                        if (appendPosTags)
                            word += "-" + pos;
                    }
                    wordsInUtterance.add(word);
                }

                // Each of the <a> nodes contains additional information about
                // the currnet utterances.  This may be syntactic information,
                // comments on the scene, descriptions of the action, or
                // clarification by the observer.  For all comments but the
                // syntactic, use the comment text to create new pseudo
                // utterances by combining tokens from the utterance with
                // pseudo-tokens in the comment.  The pseudo-tokens have a
                // "-GROUNDING" suffix which distiguishes them from tokens
                // actually present in the uttered speech.
                NodeList auxNodes = item.getElementsByTagName("a");
                List<String> augmentedUtterances = new LinkedList<String>();
                if (generateAugmented) {
                    for (int j = 0; j < auxNodes.getLength(); ++j) {
                        // Get any comment for the utterance
                        Node n = auxNodes.item(j);
                        String auxNodeType = n.getAttributes().
                            getNamedItem("type").getNodeValue();

                        // Get only those nodes that contain comments or
                        // descriptions on the utterance that may be used to
                        // ground the words being referred to.
                        if (auxNodeType.equals("action")
                            || auxNodeType.equals("actions")
                            || auxNodeType.equals("addressee")
                            || auxNodeType.equals("comments")
                            || auxNodeType.equals("explanation")
                            || auxNodeType.equals("gesture")
                            || auxNodeType.equals("happening")
                            || auxNodeType.equals("situation")) {
                            
                            String commentOnUtterance = 
                                n.getFirstChild().getNodeValue();
                            // Use the iterator factory to tokenize in the event
                            // that the user has specified some form of token
                            // filtering
                            Iterator<String> tokenIter = 
                                IteratorFactory.tokenize(
                                        new BufferedReader(
                                            new StringReader(
                                                commentOnUtterance)));
                            // For each of the tokens in the additional
                            // information, create a pseudo-utterance using a
                            // word from the actual utterance and an
                            // grounding-token
                            while (tokenIter.hasNext()) {
                                String token = tokenIter.next();
                                for (String word : wordsInUtterance) {
                                    augmentedUtterances.add(word + " "
                                        + token + "-GROUNDING");
                                }
                            }
                        }
                    }
                }
                    
                // Write the utterance if an utterance is a document.
                for (Iterator<String> it = wordsInUtterance.iterator(); 
                         it.hasNext(); ) {
                    utteranceBuilder.append(it.next());
                    if (it.hasNext())
                        utteranceBuilder.append(" ");
                }
                String utterance = utteranceBuilder.toString();
                if (utterancePerDoc) {
                    print(utterance);
                    // Print all the psuedo utterances constructed from the
                    // comments
                    for (String aug : augmentedUtterances)
                        print(aug);
                }
                else {  // otherwise save the utterance.
                    fileBuilder.append(utterance);
                    if (separateByPeriod)
                        fileBuilder.append(".");
                    fileBuilder.append(" ");

                    // Print all the psuedo utterances constructed from the
                    // comments.  Unlike the utterances, print these as separate
                    // documents to avoid having them register as co-occurrences
                    // with other utterances.
                    for (String aug : augmentedUtterances)
                        print(aug);
                }
            }

            // Write all the utterances if the whole xml file is a document.
            if (!utterancePerDoc)
                print(fileBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Finalizes the writing of documents.
     */
    public void finish() {
        for (Map.Entry<String, String> entry : posTags.entrySet()) {
            posWriter.println(entry.getKey() + " " + entry.getValue());
        }
        posWriter.flush();
        posWriter.close();

        writer.flush();
        writer.close();
    } 

    public static void main(String[] args) {

        // Add the options.
        ArgOptions options = new ArgOptions();
        options.addOption('p', "partOfSpeechTag",
                          "If set, each token will be appended with it's " +
                          "part of speech tag, such as cat-noun",
                          false, null, "Optional");
        options.addOption('S', "separateByPeriod",
                          "If set, seperates sentences by periods",
                          false, null, "Optional");
        options.addOption('U', "utterancePerDoc",
                          "If set, one utterance is considered a document, " +
                          "otherwise all uterances in a file will be " +
                          "considered a document",
                          false, null, "Optional");
        options.addOption('g', "generateOneDoc",
                          "If set, only one document will be generated for " +
                          "all the text processed",
                          false, null, "Optional");

        options.addOption('A', "augmentedUtterances",
                          "Generates augmented utterances from comments " +
                          "about the utterances", false, null, "Augmented");
        options.addOption('F', "augmentedUtterancesFilter",
                          "Specifes a token filter for which tokens in " +
                          "comments are used to generate augmented utterances",
                          true, "SPEC", "Augmented");

        options.addOption('d', "baseChildesDirectory",
                          "The base childes directory.  XML files will be " +
                          "searched for recursively from this base.  Use of " +
                          "this overrides the fileList option.",
                          true, "DIRECTORY", "Required (At least one of)");
        options.addOption('f', "fileList",
                          "The list of files to process",
                          true, "FILE[,FILE]*", "Required (At least one of)");

        // Process the options and emit errors if any required options are
        // missing.
        options.parseOptions(args);
        if ((!options.hasOption("fileList") &&
             !options.hasOption("baseChildesDirectory")) ||
             options.numPositionalArgs() != 2) {
            System.out.println(
                    "usage: java ChildesParser [options] " +
                    "<outfile> <pos-file>\n" +
                    options.prettyPrint());
            return;
        }

        // The default is to have all utterances from a conversation be in a
        // single document
        boolean utterancePerDoc = false;
        utterancePerDoc = options.hasOption("utterancePerDoc");

        boolean genAugmented = options.hasOption("augmentedUtterances");
        if (genAugmented && options.hasOption("augmentedUtterancesFilter")) {
            String filterConf = 
                options.getStringOption("augmentedUtterancesFilter");
            Properties p = System.getProperties();
            p.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY, filterConf);
            IteratorFactory.setProperties(p);
        }

        ChildesParser parser = new ChildesParser(options.getPositionalArg(0),
                                                 options.getPositionalArg(1),
                                                 genAugmented,
                                                 options.hasOption('S'),
                                                 options.hasOption('p'),
                                                 options.hasOption('g'));

        // Process the given file list, if provided.
        if (options.hasOption("fileList")) {
            String[] files = options.getStringOption("fileList").split(",");
            for (String file : files)
                parser.parseFile(new File(file), utterancePerDoc);
        } else {
            // Otherwise search for xml files to process.
            File baseDir =
                new File(options.getStringOption("baseChildesDirectory"));
            findXmlFiles(parser, utterancePerDoc, baseDir);
        }

        parser.finish();
    }

    /**
     * Recursively finds any xml documents to parse.
     */
    public static void findXmlFiles(ChildesParser parser,
                                    boolean utterancePerDoc,
                                    File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                findXmlFiles(parser, utterancePerDoc, file);
            else if (file.isFile() && file.getPath().endsWith(".xml"))
                parser.parseFile(file, utterancePerDoc);
        }
    }
}
