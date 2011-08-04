/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.dependency.CoNLLDependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractorManager;
import edu.ucla.sspace.dependency.WaCKyDependencyExtractor;

import edu.ucla.sspace.text.DependencyFileDocumentIterator;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.TokenFilter;
import edu.ucla.sspace.text.Stemmer;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.ReflectionUtil;

import java.io.IOException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;


/**
 * This abstract class extends {@link GenericMain} by overing the {@code
 * getDocumentIterator} function such that it generates document iterators for
 * dependency parse trees.
 *
 * @author Keith Stevens 
 */
public abstract class DependencyGenericMain extends GenericMain {

    /**
     * A description of the currently supported {@link DependencyExtractor}
     * configuration options.
     */
    static final String DEPENDENCY_EXTRACTOR_DESCRIPTION =        
        "This semantic space algorithm operates only on dependency parsed " +
        "corpora.  The\n" +
        "corpora must be formated in way recognized by one of extractors.  " +
        "The currently\n" +
        "supported dependency extractors are CoNLL and WaCKy.  One of these " +
        "may be\n" +
        "specifed with the -D, --dependencyParseFormat option.  The CoNLL " + 
        "extractor\n" +
        "supports optional configuration with the -G, --configFile option to " +
        "indicate the\n" +
        "order of the fields.";

    /**
     * {@inheritDoc}
     */
    public void addExtraOptions(ArgOptions options) {
        options.addOption('G', "configFile",
                          "XML configuration file for the format of a " +
                          "dependency parse",
                          true, "FILE", 
                          "Advanced Dependency Parsing Properties");
        options.addOption('D', "dependencyParseFormat",
                          "the name of the dependency parsed format for " +
                          "the corpus (defalt: CoNLL)",
                          true, "STR", 
                          "Advanced Dependency Parsing Properties");
    }

    /**
     * Links the desired {@link DependencyExtractor} with the {@link
     * DependencyExtractorManager}, creating the {@code DependencyExtractor}
     * with optional configuration file, if it is not {@code null}, and any
     * {@link TokenFilter}s or {@link Stemmer}s that have been specified by the
     * command line.
     */
    protected void setupDependencyExtractor() {
        TokenFilter filter = (argOptions.hasOption("tokenFilter"))
            ? TokenFilter.loadFromSpecification(argOptions.getStringOption('F'))
            : null;
        Stemmer stemmer = (argOptions.hasOption("stemmer"))
            ? ReflectionUtil.<Stemmer>
                 getObjectInstance(argOptions.getStringOption('Z'))
            : null;

        
        String format = (argOptions.hasOption("dependencyParseFormat"))
            ? argOptions.getStringOption("dependencyParseFormat")
            : "CoNLL";
            
        if (format.equals("CoNLL")) {
            DependencyExtractor e = (argOptions.hasOption('G'))
                ? new CoNLLDependencyExtractor(argOptions.getStringOption('G'), 
                                               filter, stemmer)
                : new CoNLLDependencyExtractor(filter, stemmer);
            DependencyExtractorManager.addExtractor("CoNLL", e);
        }
        else if (format.equals("WaCKy")) {
            if (argOptions.hasOption('G'))
                throw new IllegalArgumentException(
                    "WaCKy does not support configuration with -G");
            DependencyExtractor e = 
                new WaCKyDependencyExtractor(filter, stemmer);
            DependencyExtractorManager.addExtractor("WaCKy", e);
        }
        else 
            throw new IllegalArgumentException(
                "Unrecognized dependency parsed format: " + format);
    }

    /**
     * Returns a {@link Document} iterator for all dependency parsed documents
     * specified through the command line.
     *
     * @throws IllegalArgumentException if the {@code --docFile} argument isn't
     *         set.
     */
    protected Iterator<Document> getDocumentIterator() throws IOException {
        Iterator<Document> docIter = null;

        String docFile = (argOptions.hasOption("docFile"))
            ? argOptions.getStringOption("docFile")
            : null;

        if (docFile == null) {
            throw new Error("must specify document sources");
        }

        // Second, determine where the document input sources will be coming
        // from.
        Collection<Iterator<Document>> docIters = 
            new LinkedList<Iterator<Document>>();

        if (docFile != null) {
            String[] fileNames = docFile.split(",");
            // all the documents are listed in one file, with one document per
            // line
            for (String s : fileNames) {
                docIters.add(new DependencyFileDocumentIterator(s));
            }
        }

        // combine all of the document iterators into one iterator.
        docIter = new CombinedIterator<Document>(docIters);
        return docIter;
    }

    /**
     * Prints out information on how to run the program to {@code stdout} using
     * the option descriptions for compound words, tokenization, .sspace formats
     * and help.
     */
    @Override protected void usage() {
        String specifics = getAlgorithmSpecifics();
        System.out.println(
            "usage: java " 
            + this.getClass().getName()
            + " [options] <output-dir>\n"
            + argOptions.prettyPrint() 
            + ((specifics.length() == 0) ? "" : "\n" + specifics)
            + "\n\n" + OptionDescriptions.TOKEN_FILTER_DESCRIPTION
            + "\n\n" + OptionDescriptions.TOKEN_STEMMING_DESCRIPTION
            + "\n\n" + OptionDescriptions.FILE_FORMAT_DESCRIPTION
            + "\n\n" + DEPENDENCY_EXTRACTOR_DESCRIPTION
            + "\n\n" + OptionDescriptions.HELP_DESCRIPTION);
    }

}
