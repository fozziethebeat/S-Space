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
 * Rubenstein and Goodneough.  See the following paper for full details.
 *
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> 
 *      Rubenstein, H. and Goodenough, J. B. Contextual Correlates of Synonymy
 *      Communications of the ACM, 1965, 8, 627-633
 *   </li>
 *
 * </ul>
 */
public class RubensteinGoodenoughWordSimilarityEvaluation 
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
    public RubensteinGoodenoughWordSimilarityEvaluation(String rbSimFileName) {
        this(new File(rbSimFileName));
    }

    /**
     * Constructs this word similarity evaluation test using the provide WS353
     * data file.
     */
    public RubensteinGoodenoughWordSimilarityEvaluation(File rbSimFile) {
        pairs = parse(rbSimFile);
        dataFileName = rbSimFile.getName();
    }

    /**
     * Parses the WordSimilarity353 file and returns the set of judgements.
     */
    private Collection<WordSimilarity> parse(File word353file) {

        Collection<WordSimilarity> pairs = new LinkedList<WordSimilarity>();
                
        try {
            BufferedReader br = new BufferedReader(new FileReader(word353file));
            // skip the first line
            br.readLine();
            for (String line = null; (line = br.readLine()) != null; ) {
                
                // skip comments and blank lines
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }

                String[] wordsAndNum = line.split("\\s+");
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
        return "Rubenstein & Goodenough Word Similarity Test [" 
            + dataFileName + "]";
    }
}


