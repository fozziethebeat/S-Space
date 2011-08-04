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

import edu.ucla.sspace.matrix.SVD;


/**
 * A utility class that contains string descriptions of differnt command line
 * options.  Classes that are designed to execute a {@link SemanticSpace}
 * algorithm are encouraged to use this class to generate option descriptions
 * for their help messages.
 *
 * <p> All return strings are formatted to fit in under 80 columns of text and
 * are <i>not</i> terminated with a newline.  The descriptions do not contain
 * any option specification (e.g. <tt>-g</tt>, <tt>--compoundWords</tt>)
 * themselves; the calling code will need to specify how these behaviors are
 * invoked.
 *
 * @see GenericMain
 */
public final class OptionDescriptions {

    /**
     * Uninstantiable
     */
    private OptionDescriptions() { }

    /**
     * A description of the file used to specify compound words.
     */
    public static final String COMPOUND_WORDS_DESCRIPTION =
        "The compound word option specifies a file whose contents are " +
        "compount tokens,\n" +
        "e.g. white house.  Each compound token should be specified on " + 
        "its own line.\n" +
        "Compount tokenization is greedy and will select the longest " + 
        "compound token\n" +
        "present.  For example if \"bar exam\" and \"California bar " + 
        "exam\" are both\n" +
        "compound tokens, the latter will always be returned as a single " +
        "token, rather\n" +
        "than returning the two tokens \"California\" and \"bar exam\".";

    /**
     * A description of the use of a {@link edu.ucla.sspace.text.TokenFilter
     * TokenFilter}.
     */
    public static final String TOKEN_FILTER_DESCRIPTION =
        "token configuration lists sets of files that contain tokens to be " +
        "included or\n" +
        "excluded.  The behavior, \"include\" or \"exclude\" is specified\n" +
        "first, followed by one or more file names, each separated by " +
        "colons.\n" +
        "Multiple behaviors may be specified one after the other using a ','\n"+
        "character to separate them.  For example, a typicaly configuration " +
        "may\n" +
        "look like: " +
        "include=top-tokens.txt:test-words.txt,exclude=stop-words.txt\n" +
        "Note behaviors are applied in the order they are presented on the " +
        "command-line.";
 
    /**
     * A description of the use of a {@link edu.ucla.sspace.text.Stemmer
     * Stemmer}.
     */
    public static final String TOKEN_STEMMING_DESCRIPTION =
        "Tokens can be stemmed for various languages using wrappers for " +
        "the snoball\n" +
        "stemming algorithms.  Each language has it's own stemmer, following " +
        "a simple naming\n " +
        "convention: LanguagenameStemmer.";
 
    /**
     * A description of the output file format produced by {@link
     * edu.ucla.sspace.common.SemanticSpaceIO}
     */
    public static final String FILE_FORMAT_DESCRIPTION = 
        "Semantic space files stored in one of four formats: text, " +
        "sparse_text, binary\n" +
        "sparse_binary.  The sparse versions should be used if the " +
        "algorithm produces\n" +
        "semantic vectors in which more than half of the values are 0.  " +
        "The sparse\n" +
        "versions are much more compact for these types of semantic spaces " +
        "and will be\n" +
        "both faster to read and write as well as be much smaller on disk.  " +
        "Text formats\n" +
        "are human readable but may take up more space.  Binary formats " +
        "offer\n" +
        "significantly better I/O performance.";

    /**
     * A description of who to contact for bugs
     */
    public static final String HELP_DESCRIPTION =
        "Send bug reports or comments to " +
        "<s-space-research-dev@googlegroups.com>.";
    
    /**
     * A description of the configuration SVD algorithm
     */
    public static final String SVD_DESCRIPTION =
        "The SVD implmentation may select from several options:\n  " +
        SVD.Algorithm.values() +
        "\nSVDLIBJ is included with the S-Space Package and provides a fully "+
        "Java\n" +
        "implementation of a sparse SVD.  The other options provide support " +
        "from\n" +
        "external libraries and programs that implement the SVD.  " + 
        "If external\n" +
        "programs (svd, octave, matlab) are used, the program must " +
        "be invokable\n" +
        "using the current PATH environment variable.  If the library is " +
        "Java-based\n" +
        "(JAMA, COLT), the library must be present in the classpath.  " +
        "However, for\n" +
        "Java-based libraries, if the semantic space algorithm iteself is " +
        "being invoked\n" +
        "through a .jar, the classpath to the SVD library must be specified " +
        "using an\n" + 
        "additional system property.  For JAMA, the path to the .jar file " +
        "must be\n" +
        "specified using the system property \"jama.path\".  To set this " +
        "on the\n" +
        "command-line, use -Djama.path=<.jar location>.  Similarly, if COLT " +
        "is to be used\n" +
        "when invoked from a .jar, then the \"colt.path\" property\n" +
        "must be set.";
}
