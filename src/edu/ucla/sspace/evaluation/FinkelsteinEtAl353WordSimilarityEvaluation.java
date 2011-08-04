/*
 * Copyright 2009 David Jurgens 
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

package edu.ucla.sspace.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Collection;
import java.util.LinkedList;

import edu.ucla.sspace.common.SemanticSpace;

/**
 * A collection of human similarity judgements of word pairs gathered by
 * Finkelstein et al.  See <a
 * href="http://www.cs.technion.ac.il/~gabr/resources/data/wordsim353/">their
 * website</a> for access to the test data.  See the following reference for
 * full details on how the data was gathered.
 *
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Lev Finkelstein, Evgeniy
 *   Gabrilovich, Yossi Matias, Ehud Rivlin, Zach Solan, Gadi Wolfman, and Eytan
 *   Ruppin, "Placing Search in Context: The Concept Revisited", ACM
 *   Transactions on Information Systems, 20(1):116-131, January 2002.
 *   Available <a
 *   href="http://www.cs.technion.ac.il/~gabr/papers/tois_context.pdf">here</a>.
 *
 * </ul>
 */
public class FinkelsteinEtAl353WordSimilarityEvaluation 
    implements WordSimilarityEvaluation {

    /**
     * A collection of human judgements on word relatedness
     */
    private final Collection<WordSimilarity> pairs;

    /**
     * The name of the data file for this test
     */
    private final String dataFileName;

    /**
     * Constructs this word similarity evaluation test using the WS353 data file
     * refered to by the provided name.
     */
    public FinkelsteinEtAl353WordSimilarityEvaluation(String word353fileName) {
        this(new File(word353fileName));
    }

    /**
     * Constructs this word similarity evaluation test using the provide WS353
     * data file.
     */
    public FinkelsteinEtAl353WordSimilarityEvaluation(File word353file) {
        pairs = parse(word353file);
        dataFileName = word353file.getName();
    }

    /**
     * Parses the WordSimilarity353 file and returns the set of judgements.
     */
    private Collection<WordSimilarity> parse(File word353file) {
        // the ws353 data set comes in two formats, a comma-separated format and
        // a tab-separated format.  Support both by checking the file name
        // suffix.
        String delimeter = (word353file.getName().endsWith(".csv"))
            ? "," : "\\s";

        Collection<WordSimilarity> pairs = new LinkedList<WordSimilarity>();
                
        try {
            BufferedReader br = new BufferedReader(new FileReader(word353file));
            // skip the first line
            br.readLine();
            for (String line = null; (line = br.readLine()) != null; ) {

                String[] wordsAndNum = line.split(delimeter);
                if (wordsAndNum.length != 3) {
                    throw new Error("Unexpected line formatting: " + line);
                }
                pairs.add(new SimpleWordSimilarity(
                          wordsAndNum[0], wordsAndNum[1], 
                          Double.parseDouble(wordsAndNum[2])));
            }
        } catch (IOException ioe) {
            // rethrow as an IOE is fatal evaluation
            throw new IOError(ioe);
        }
            
        return pairs;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<WordSimilarity> getPairs() {
        return pairs;
    }

    /**
     * {@inheritDoc}
     */
    public double getMostSimilarValue() {
        return 10d;
    }
    
    /**
     * {@inheritDoc}
     */
    public double  getLeastSimilarValue() {
        return 0d;
    }

    public String toString() {
        return "Finkelstein et al. Word Similarity Test [" + dataFileName + "]";
    }

}


