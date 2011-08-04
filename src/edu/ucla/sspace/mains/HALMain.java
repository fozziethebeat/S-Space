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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.hal.HyperspaceAnalogueToLanguage;

import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link HyperspaceAnalogueToLanguage} (HAL)
 * from the command line.  This class takes in several command line arguments.
 *
 * <ul>
 *
 * <li><u>Required (at least one of)</u>:
 *   <ul>
 *
 *   <li> {@code -d}, {@code --docFile=FILE[,FILE...]} a file where each line is
 *        a document.  This is the preferred input format for large corpora
 *
 *   <li> {@code -f}, {@code --fileList=FILE[,FILE...]} a list of document files
 *        where each file is specified on its own line.
 *
 *   </ul>
 * 
 * <li><u>Algorithm Options</u>:
 *   <ul>
 *
 *   <li> {@code -s}, {@code --windowSize=INT} how many words to consider in each
 *        direction
 *
 *   <li> {@code -r}, {@code --retain=INT} how many column dimensions to retain
 *        in the final word co-occurrence matrix.  The retained columns will be
 *        those that provide the most information for distinguishing the
 *        semantics of the words.  Unlike the {@code --threshold} option, this
 *        specifies a hard limit for how many to retain.  This option may not
 *        speciefied at the same time as {@code --threshold}
 * 
 *  <li> {@code -h}, {@code --threshold=DOUBLE} the minimum information
 *        theoretic entropy a word must have to be retained in the final word
 *        co-occurrence matrix.  This option may not be used at the same time as
 *        {@code --retain}.
 *
 *  <li> {@code -W}, {@code --weighting=CLASS} the fully qualified name of a
 *        {@link edu.ucla.sspace.hal.WeightingFunction} class to be used for
 *        weighting co-occurrences.  HAL traditionally uses a linear weighting
 *        where the closest neighboring words receive the highest weight.
 *
 *   <li> {@code -F}, {@code --tokenFilter=FILE[include|exclude][,FILE...]}
 *        specifies a list of one or more files to use for {@link
 *        edu.ucla.sspace.text.TokenFilter filtering} the documents.  An option
 *        flag may be added to each file to specify how the words in the filter
 *        filter should be used: {@code include} if only the words in the filter
 *        file should be retained in the document; {@code exclude} if only the
 *        words <i>not</i> in the filter file should be retained in the
 *        document.
 *
 *   </ul>
 *
 * <li><u>Program Options</u>:
 *   <ul>
 *
 *   <li> {@code -o}, {@code --outputFormat=}<tt>text|binary}</tt> Specifies the
 *        output formatting to use when generating the semantic space ({@code
 *        .sspace}) file.  See {@link edu.ucla.sspace.common.SemanticSpaceUtils
 *        SemanticSpaceUtils} for format details.
 *
 *   <li> {@code -t}, {@code --threads=INT} how many threads to use when
 *        processing the documents.  The default is one per core.
 * 
 *   <li> {@code -w}, {@code --overwrite=BOOL} specifies whether to overwrite
 *        the existing output files.  The default is {@code true}.  If set to
 *        {@code false}, a unique integer is inserted into the file name.
 *
 *   <li> {@code -v}, {@code --verbose}  specifies whether to print runtime
 *        information to standard out
 *
 *   </ul>
 *
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output {@code
 * hal-semantic-space.sspace}.  If {@code overwrite} was set to {@code true},
 * this file will be replaced for each new semantic space.  Otherwise, a new
 * output file of the format {@code hal-semantic-space<number>.sspace} will be
 * created, where {@code <number>} is a unique identifier for that program's
 * invocation.  The output file will be placed in the directory specified on the
 * command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see HyperspaceAnalogueToLanguage
 *
 * @author David Jurgens
 */
public class HALMain extends GenericMain {

    private HALMain() { }

    /**
     * {@inheritDoc}
     */
    protected void addExtraOptions(ArgOptions options) {
        options.addOption('h', "threshold", "minimum entropy for semantic " +
                          "dimensions (default: disabled)", true,
                          "DOUBLE", "Algorithm Options");
        options.addOption('r', "retain",
                          "maximum number of dimensions to retain" +
                          "(default: disabled)", true,
                          "INT", "Algorithm Options");
        options.addOption('s', "windowSize",
                          "The number of words to inspect to the left and " +
                          "right of a focus word (default: 5)",
                          true, "INT", "Algorithm Options");
        options.addOption('W', "weighting", "WeightingFunction class name"
                          + "(default: LinearWeighting)", true,
                          "CLASSNAME", "Algorithm Options");
    }

    public static void main(String[] args) {
        HALMain hal = new HALMain();
        try {
            hal.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    /**
     * {@inheritDoc}
     */
    protected SemanticSpace getSpace() {
        return new HyperspaceAnalogueToLanguage();
    }

    /**
     * {@inheritDoc}
     */
    protected Properties setupProperties() {
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = System.getProperties();

         if (argOptions.hasOption("windowSize")) {
             props.setProperty(
                     HyperspaceAnalogueToLanguage.WINDOW_SIZE_PROPERTY,
                     argOptions.getStringOption("windowSize"));
         }

         if (argOptions.hasOption("threshold")) {
             props.setProperty(
                     HyperspaceAnalogueToLanguage.ENTROPY_THRESHOLD_PROPERTY,
                     argOptions.getStringOption("threshold"));
         }

         if (argOptions.hasOption("retain")) {
             props.setProperty(
                     HyperspaceAnalogueToLanguage.RETAIN_PROPERTY,
                     argOptions.getStringOption("retain"));
         }

         if (argOptions.hasOption("weighting")) {
             props.setProperty(
                     HyperspaceAnalogueToLanguage.WEIGHTING_FUNCTION_PROPERTY,
                     argOptions.getStringOption("weighting"));
         }        

        return props;
    }

    /**
     * {@inheritDoc}
     */
    protected String getAlgorithmSpecifics() {
        return
            "Note that the --retain and --threshold properties are mutually " + 
            "exclusive;\nusing both will cause an exception";
    }
}
