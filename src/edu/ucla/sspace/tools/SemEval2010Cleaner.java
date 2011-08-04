package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.text.EnglishStemmer;
import edu.ucla.sspace.text.Stemmer;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SemEval2010Cleaner {

    public static void main(String[] args) throws Exception {
        // Add and parser command line options.
        ArgOptions options = new ArgOptions();
        options.addOption('i', "includeSeparator",
                           "a separator between the previous context and " +
                           "the token of interest",
                           true, "STIRNG", "Optional");
        options.addOption('d', "prepareForDependencyParse",
                          "Set to true if special tokens signifying what " +
                          "instance is being generated should be added",
                          false, null, "Optional");
        options.parseOptions(args);

        // Validate that the expected number of arguments are given.
        if (options.numPositionalArgs() < 2) {
            System.out.println("usage: SemEval2010Cleaner [options]" +
                               "<out-file> <training-file.xml>+\n" +
                               options.prettyPrint());
            System.exit(1);
        }

        String separator = options.getStringOption("includeSeparator", "");
        boolean parsePrepare = options.hasOption("prepareForDependencyParse");

        // Set up the output writer.
        PrintWriter writer = new PrintWriter(options.getPositionalArg(0));

        // Set up a stemmer.
        Stemmer stemmer = new EnglishStemmer();

        // Process each training file, where the output is written to the
        // specified output file.
        for (int i = 1; i < options.numPositionalArgs(); ++i)
            processTrainingFile(writer, stemmer, parsePrepare,
                                separator, options.getPositionalArg(i));

        // Finish up the writer.
        writer.flush();
        writer.close();
    }

    public static void processTrainingFile(
            PrintWriter writer,
            Stemmer stemmer,
            boolean parsePrepare,
            String separator,
            String trainingFile) throws Exception {
        // Parse the input file.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(trainingFile));
        NodeList root = doc.getChildNodes();

        // Iterate over all of the word instances.
        NodeList instances = root.item(0).getChildNodes();
        for (int i = 0; i < instances.getLength(); ++i) {
            // Get the instance id and the base text of the instance.
            Element instanceNode = (Element) instances.item(i);
            String instanceId = instanceNode.getNodeName();
            String[] wordPosNum = instanceId.split("\\.");
            String word = stemmer.stem(wordPosNum[0].toLowerCase());
            String text = instanceNode.getTextContent();

            // Split the instance text and find the instance of the actual word
            // that needs to be represented.  This is done by stemming each word
            // and checking to see which one has the same stem as the instance
            // word.  Each time we encounter a word with the instance stem,
            // output a version of the context that has a |||| before the
            // instance word.  For some instance text's the word occurs twice,
            // so this should create a context for both instances with a
            // separator at different positions.
            String[] tokens = text.split("\\s+");
            StringBuilder prevContext = new StringBuilder();
            for (int k = 0; k < tokens.length; ++k)
                tokens[k] = tokens[k].toLowerCase();

            for (int k = 0; k < tokens.length; ++k) {
                // Find where the instance word occurs.
                String stem = stemmer.stem(tokens[k]);
                if (stem.equals(word)) {
                    // Generate the context that comes after the instance word.
                    StringBuilder nextContext = new StringBuilder();
                    for (int j = k + 1; j < tokens.length; ++j)
                        nextContext.append(tokens[j]).append(" ");

                    // Output the previous context, a separator, the instance
                    // stem, and the next context.
                    writer.printf("%s %s %s %s\n", 
                                  prevContext.toString(), separator, stem,
                                  nextContext.toString());

                    // If this output is being prepared for dependency parsing,
                    // add a special set of tokens signifying that this context
                    // coresponds to a given instance.
                    if (parsePrepare)
                        writer.printf("BREAK_INSTANCE %s %s %s.\n",
                                      stem, wordPosNum[1], wordPosNum[2]);
                }

                // Add the token to the previous context in case the instance 
                // word occurs multiple times.
                prevContext.append(tokens[k]).append(" ");
            }
        }
    }
}
