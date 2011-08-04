/*
 * Copyright 2010 David Jurgens 
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

package edu.ucla.sspace.dv;

import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.text.IteratorFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**      
 * A {@code DependencyPathAcceptor} that accepts the minimum set of path
 * templates specified by <a
 * href="http://www.nlpado.de/~sebastian/pub/papers/cl07_pado.pdf">Padó and
 * Lapata (2007)</a>.  This acceptor is designed to be used with the Penn
 * Treebank part of speech <a
 * href="http://www.ling.upenn.edu/courses/Fall_2003/ling001/penn_treebank_pos.html">tag
 * set</a> and dependency relations from the pre-1.4 Malt parser (not the
 * Stanford typed dependencies).  Note that this template's patters is an
 * <i>adaptation</i> of the the original patterns, which were specified using
 * the Minipar relations and part of speech tags.
 *
 * @see MinimumTemplateAcceptor
 * @see MaximumTemplateAcceptor
 */
public class MediumPennTemplateAcceptor implements DependencyPathAcceptor {

    static final Set<String> MEDIUM_TEMPLATES = new HashSet<String>();

    /**
     * A mapping from a specific POS tag, e.g. NN, JJS, to the general
     * <i>class</i> of part of speech tags, e.g. noun (N), to which it belongs.
     */
    static final Map<String,String> POS_TAG_TO_CLASS = 
        new HashMap<String,String>();

    // Static block for initializing the POS_TAGS_TO_CLASS mapping using the
    // PennTags class
    static {
        // NOTE: the class tags are intentionally short to facilitate faster
        // matching
        for (String noun : PennTags.NOUN_POS_TAGS)
            POS_TAG_TO_CLASS.put(noun, "N"); // Noun
        for (String adj : PennTags.ADJ_POS_TAGS)
            POS_TAG_TO_CLASS.put(adj, "J"); // adJective
        for (String adv : PennTags.ADV_POS_TAGS)
            POS_TAG_TO_CLASS.put(adv, "R"); // adveRb
        for (String verb : PennTags.VERB_POS_TAGS)
            POS_TAG_TO_CLASS.put(verb, "V"); // Verb
    }
    
    /**
     * A mapping from a relation to the more general class of relations to which
     * it belongs, e.g. AMOD and PMOD would be mapped to a "lexical modifier"
     * relation class.
     */
    static final Map<String,String> REL_TO_CLASS =
        new HashMap<String,String>();
    
    // Static block for intializing REL_TO_CLASS
    static {
        for (String mod : PennTags.MODIFIERS)
            REL_TO_CLASS.put(mod, "mod");
    }
    
    // Static block for initializing the medium patterns.  Note that this block
    // uses the shorted class labels for parts of speech, e.g. NNS, NP, NN -> N,
    // in order to handle the combinatorial explosion of patterns that would
    // need to be expressed when moving from the Minipar to Penn tag sets.
    static {
        MEDIUM_TEMPLATES.add("J:nmod:N,N:amod:(null)");
        MEDIUM_TEMPLATES.add("J:nmod:N,N:vmod:(null)");
        MEDIUM_TEMPLATES.add("J:nmod:N,N:nmod:(null)");
        MEDIUM_TEMPLATES.add("J:nmod:N,N:nmod:N");
        MEDIUM_TEMPLATES.add("J:sbj:N,N:amod:(null)");
        MEDIUM_TEMPLATES.add("J:sbj:N,N:nmod:(null)");
        MEDIUM_TEMPLATES.add("J:sbj:N,N:vmod:(null)");
        MEDIUM_TEMPLATES.add("J:sbj:N,N:nmod:N");

        MEDIUM_TEMPLATES.add("R:nmod:N,N:amod:(null)");
        MEDIUM_TEMPLATES.add("R:nmod:N,N:vmod:(null)");
        MEDIUM_TEMPLATES.add("R:nmod:N,N:nmod:(null)");
        MEDIUM_TEMPLATES.add("R:nmod:N,N:nmod:N");
        MEDIUM_TEMPLATES.add("R:sbj:N,N:amod:(null)");
        MEDIUM_TEMPLATES.add("R:sbj:N,N:vmod:(null)");
        MEDIUM_TEMPLATES.add("R:sbj:N,N:nmod:(null)");
        MEDIUM_TEMPLATES.add("R:sbj:N,N:nmod:N");


        MEDIUM_TEMPLATES.add("N:coord:N,N:mod:(null)");
        MEDIUM_TEMPLATES.add("N:coord:N,N:nmod:N");
        MEDIUM_TEMPLATES.add("N:gen:N,N:nmod:(null)");
        MEDIUM_TEMPLATES.add("N:gen:N,N:nmod:N");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:coord:N");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:coord:N,N:nmod:N");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:gen:N");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:gen:N,N:nmod:N");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:mod:A");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:mod:TO");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:obj:V");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:prd:V");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:sbj:A");
        MEDIUM_TEMPLATES.add("N:nmod:N,N:sbj:V");

        MEDIUM_TEMPLATES.add("(null):mod:N,N:coord:N");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:coord:N,N:mod:(null)");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:gen:N");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:gen:N,N:mod:(null)");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:amod:J");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:adv:R");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:pmod:TO");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:obj:V");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:prd:V");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:sbj:J");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:sbj:R");
        MEDIUM_TEMPLATES.add("(null):mod:N,N:sbj:V");

        MEDIUM_TEMPLATES.add("TO:mod:N,N:mod:(null)");
        MEDIUM_TEMPLATES.add("TO:mod:N,N:nmod:N");

        MEDIUM_TEMPLATES.add("V:obj:N,N:mod:(null)");
        MEDIUM_TEMPLATES.add("V:obj:N,N:nmod:N");
        MEDIUM_TEMPLATES.add("V:sbj:N,N:mod:(null)");
        MEDIUM_TEMPLATES.add("V:sbj:N,N:nmod:N");
        MEDIUM_TEMPLATES.add("V:prd:N,N:mod:(null)");
        MEDIUM_TEMPLATES.add("V:prd:N,N:nmod:N");
    }
    
    /**
     * Creates the acceptor with its standard templates
     */
    public MediumPennTemplateAcceptor() { }
   
    /**
     * Returns {@code true} if the path matches one of the predefined templates
     *
     * @param path a dependency path
     *
     * @return {@code true} if the path matches a template
     */
    public boolean accepts(DependencyPath path) {
        return acceptsInternal(path);
    }

    /**
     * A package-private method that checks whether the path matches any of the
     * predefined templates.  This method is provided so other template classes
     * have access to the accept logic used by this class.
     *
     * @param path a dependency path
     *
     * @return {@code true} if the path matches a template
     */
    static boolean acceptsInternal(DependencyPath path) {
        // First check whether the minimum template acceptor would allow this
        // path
        if (MinimumPennTemplateAcceptor.acceptsInternal(path))
            return true;

        // Filter out paths that can't match the template due to length
        if (path.length() > 3)
            return false;

        int pathLength = path.length();

        // The medium set of templates contains "null" matches which are wild
        // cards against any part of speech.  We handle these by generating
        // three possible pattern instances that represent the provided path,
        // two of which include the wildcard "null", one each end.  If any of
        // these patterns are found in the medium set, the path is valid.
        StringBuilder nullStart = new StringBuilder(pathLength * 16);
        StringBuilder nullEnd = new StringBuilder(pathLength * 16);
        StringBuilder noNulls = new StringBuilder(pathLength * 16);

        // Iterate over each pair in the path and create the pattern string that
        // represents this path.  The pattern string is pos:rel:pos[,...] .
        DependencyTreeNode first = path.first();
        for (int i = 1; i < pathLength; ++i) {
            DependencyTreeNode second = path.getNode(i);
            // Check that the nodes weren't filtered out.  If so reject the path
            // even if the part of speech and relation text may have matched a
            // template.
            if (first.word().equals(IteratorFactory.EMPTY_TOKEN))
                return false;

            // Get the relation between the two nodes
            String rel = path.getRelation(i - 1);
            String firstPos = first.pos();
            String secPos = second.pos();

            // Check whether each POS has a class category to which it should be
            // mapped.  These classes are necessary to handle the singificant
            // number of variations for a general category of POS's, e.g. verb
            // -> VBZ, VBJ, etc., which were not present when the MINIPAR tags
            // were designed by Padó and Lapata.
            String class1 = POS_TAG_TO_CLASS.get(firstPos);
            String class2 = POS_TAG_TO_CLASS.get(secPos);
            
            if (class1 != null)
                firstPos = class1;
            if (class2 != null)
                secPos = class2;

            // Similarly, in order to handle the lex-mod relation, we check
            // whether the relation, e.g. PMOD, can be mapped to the general
            // lexical modifier class.
            String relClass = REL_TO_CLASS.get(rel);
            if (relClass != null)
                rel = relClass;
            
            // Create the three relation patterns by checking the current index
            // compared to the path length.
            nullStart.append((i == 1) ? "(null)" : firstPos);
            nullStart.append(":").append(rel).append(":").append(secPos);

            nullEnd.append(firstPos).append(":").append(rel).append(":");
            nullEnd.append((i + 1 == pathLength) ? "(null)" : secPos);

            noNulls.append(firstPos).append(":").append(rel)
                .append(":").append(secPos);

            // Check whether more elements existing, and if so, add the ','
            if (i + 1 < pathLength) {
                nullStart.append(",");
                nullEnd.append(",");
                noNulls.append(",");
            }

            // Last, shift over the node
            first = second;
        }

        // Extra case for the last token in the path
        if (first.word().equals(IteratorFactory.EMPTY_TOKEN))
            return false;

        boolean match = MEDIUM_TEMPLATES.contains(noNulls.toString())
            || MEDIUM_TEMPLATES.contains(nullStart.toString())
            || MEDIUM_TEMPLATES.contains(nullEnd.toString());
        
        return match;
    }

    /**
     * {@inheritDoc}
     */
    public int maxPathLength() {
        return 4;
    }
}