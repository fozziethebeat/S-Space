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
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractorManager;
import edu.ucla.sspace.dependency.UniversalPathAcceptor;

import edu.ucla.sspace.svs.StructuredVectorSpace;
import edu.ucla.sspace.svs.PointWiseCombinor;
import edu.ucla.sspace.svs.VectorCombinor;

import edu.ucla.sspace.util.ReflectionUtil;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * NOTE: NOT MEANT TO BE PART OF WORDSI-UPDATE.
 *
 * An executable class for running {@link StructuredVectorSpace}
 * (StructuredVectorSpace) from the command line.  This class takes in several
 * command line arguments.
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
        options.addOption('G', "configFile",
                          "XML configuration file for the format of a " +
                          "dependency parse",
 *
 *   <li> {@code -a}, {@code --pathAcceptor=CLASSNAME}
 *        Specifies the {@link edu.ucla.sspace.dependency.DependencyPathAcceptor
 *        DependencyPathAcceptor} to use while accepting or rejecting {@link
 *        DependencyPath}s.
 *
 *   <li> {@code -W}, {@code --pathWeighter=CLASSNAME} 
 *         Specifies the {@link
 *         edu.ucla.sspace.dependency.DependencyPathWeighter
 *         DependencyPathWeighter} to use while scoring {@link DependencyPath}s.
 *
 *   </ul>
 *
 * <li><u>Program Options</u>:
 *   <ul>
 *
 *   <li> {@code -G}, {@code --configFile=config.xml}
 *        Specifies a configuration file for specifying the ordering of the
 *        malth styled dependency parse trees.
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
 * structued-vector-space.sspace}.  If {@code overwrite} was set to {@code
 * true}, this file will be replaced for each new semantic space.  Otherwise, a
 * new output file of the format {@code structued-vector-space<number>.sspace}
 * will be created, where {@code <number>} is a unique identifier for that
 * program's invocation.  The output file will be placed in the directory
 * specified on the command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see StructuredVectorSpace 
 *
 * @author Keith Stevens
 */
public class StructuredVectorSpaceMain extends DependencyGenericMain {

    private StructuredVectorSpaceMain() { }

    /**
     * {@inheritDoc}
     */
    public void addExtraOptions(ArgOptions options) {
        super.addExtraOptions(options);

        options.addOption('a', "pathAcceptor",
                          "The DependencyPathAcceptor to use",
                          true, "CLASSNAME", "Algorithm Options");
        options.addOption('c', "vectorCombinor",
                          "The VectorCombinor to use",
                          true, "CLASSNAME", "Algorithm Options");
    }

    public static void main(String[] args) throws Exception {
        StructuredVectorSpaceMain svs =  new StructuredVectorSpaceMain();
        svs.run(args);
    }
    
    /**
     * {@inheritDoc}
     */
    protected SemanticSpace getSpace() {
        // Ensure that the configured DependencyExtactor is in place prior to
        // constructing the SVS
        setupDependencyExtractor();        

        DependencyPathAcceptor acceptor;
        if (argOptions.hasOption("pathAcceptor"))
            acceptor = ReflectionUtil.getObjectInstance(
                    argOptions.getStringOption("pathAcceptor"));
        else
            acceptor = new UniversalPathAcceptor();

        VectorCombinor combinor;
        if (argOptions.hasOption("pathAcceptor"))
            combinor = ReflectionUtil.getObjectInstance(
                    argOptions.getStringOption("vectorCombinor"));
        else
            combinor = new PointWiseCombinor();

        DependencyExtractor extractor = 
            DependencyExtractorManager.getDefaultExtractor();
        return new StructuredVectorSpace(extractor, acceptor, combinor);
    }

    /**
     * {@inheritDoc}
     */
    protected Properties setupProperties() {
        return System.getProperties();
    }

    /**
     * {@inheritDoc}
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SERIALIZE;
    }
}
