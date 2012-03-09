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
 * @see MediumPennTemplateAcceptor
 * @see MaximumPennTemplateAcceptor
 */
public class MinimumPennTemplateAcceptor implements DependencyPathAcceptor {

    static final Set<String> MINIMUM_TEMPLATES = new HashSet<String>();
    
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

    // Static block for initializing the minimum patterns.  Note that this block
    // uses the shorted class labels for parts of speech, e.g. NNS, NP, NN -> N,
    // in order to handle the combinatorial explosion of patterns that would
    // need to be expressed when moving from the Minipar to Penn tag sets.
    static {
        MINIMUM_TEMPLATES.add(toPattern("R", "VMOD", "V"));
        MINIMUM_TEMPLATES.add(toPattern("R", "AMOD", "J"));
        MINIMUM_TEMPLATES.add(toPattern("R", "PROD", "V"));
        MINIMUM_TEMPLATES.add(toPattern("R", "PMOD", "V"));
        MINIMUM_TEMPLATES.add(toPattern("R", "ADV", "V"));
        MINIMUM_TEMPLATES.add(toPattern("R", "ADV", "N"));
        MINIMUM_TEMPLATES.add(toPattern("R", "ADV", "J"));

        MINIMUM_TEMPLATES.add(toPattern("J", "NMOD", "N"));
        MINIMUM_TEMPLATES.add(toPattern("J", "NMOD", "TO"));
        MINIMUM_TEMPLATES.add(toPattern("J", "PMOD", "TO"));
        MINIMUM_TEMPLATES.add(toPattern("J", "SBJ", "N"));

        MINIMUM_TEMPLATES.add(toPattern("N", "COORD", "N"));
        MINIMUM_TEMPLATES.add(toPattern("N", "PROD", "N"));
        MINIMUM_TEMPLATES.add(toPattern("N", "NMOD", "J"));
        MINIMUM_TEMPLATES.add(toPattern("N", "NMOD", "R"));
        MINIMUM_TEMPLATES.add(toPattern("N", "NMOD", "TO"));
        MINIMUM_TEMPLATES.add(toPattern("N", "NMOD", "N"));
        MINIMUM_TEMPLATES.add(toPattern("N", "OBJ", "V"));
        MINIMUM_TEMPLATES.add(toPattern("N", "SBJ", "J"));
        MINIMUM_TEMPLATES.add(toPattern("N", "SBJ", "R"));
        MINIMUM_TEMPLATES.add(toPattern("N", "SBJ", "N"));
        MINIMUM_TEMPLATES.add(toPattern("N", "SBJ", "V"));
        MINIMUM_TEMPLATES.add(toPattern("N", "ADV", "N"));


        // NOTE: not sure how to convert this pattern
        // MINIMUM_TEMPLATES.add(toPattern(null, "lex-mod", "V"));

        MINIMUM_TEMPLATES.add(toPattern("TO", "AMOD", "J"));
        MINIMUM_TEMPLATES.add(toPattern("TO", "NMOD", "N"));
        MINIMUM_TEMPLATES.add(toPattern("TO", "VMOD", "V"));   
        // NOTE: I think this pattern satisfies the commented out one below -dj
        MINIMUM_TEMPLATES.add(toPattern("TO", "PMOD", "N"));
        MINIMUM_TEMPLATES.add(toPattern("TO", "ADV", "N"));
        MINIMUM_TEMPLATES.add(toPattern("TO", "ADV", "V"));
        MINIMUM_TEMPLATES.add(toPattern("TO", "ADV", "R"));
        MINIMUM_TEMPLATES.add(toPattern("TO", "ADV", "J"));
        // MINIMUM_TEMPLATES.add(toPattern("TO", "pcomp-n", "N"));

        MINIMUM_TEMPLATES.add(toPattern("IN", "AMOD", "J"));
        MINIMUM_TEMPLATES.add(toPattern("IN", "NMOD", "N"));
        MINIMUM_TEMPLATES.add(toPattern("IN", "VMOD", "V"));   
        // NOTE: I think this pattern satisfies the commented out one below -dj
        MINIMUM_TEMPLATES.add(toPattern("IN", "PMOD", "N"));
        MINIMUM_TEMPLATES.add(toPattern("IN", "ADV", "N"));
        MINIMUM_TEMPLATES.add(toPattern("IN", "ADV", "V"));
        MINIMUM_TEMPLATES.add(toPattern("IN", "ADV", "R"));
        MINIMUM_TEMPLATES.add(toPattern("IN", "ADV", "J"));


        MINIMUM_TEMPLATES.add(toPattern("V", "AMOD", "R"));
        MINIMUM_TEMPLATES.add(toPattern("V", "VMOD", "R"));
        // MINIMUM_TEMPLATES.add(toPattern("V", "lex-mod", null));
        MINIMUM_TEMPLATES.add(toPattern("V", "VMOD", "J"));
        MINIMUM_TEMPLATES.add(toPattern("V", "AMOD", "J"));
        MINIMUM_TEMPLATES.add(toPattern("V", "PMOD", "TO"));
        MINIMUM_TEMPLATES.add(toPattern("V", "OBJ", "N"));
        MINIMUM_TEMPLATES.add(toPattern("V", "SBJ", "N"));
        MINIMUM_TEMPLATES.add(toPattern("V", "ADV", "N"));
        MINIMUM_TEMPLATES.add(toPattern("V", "ADV", "R"));
        MINIMUM_TEMPLATES.add(toPattern("V", "ADV", "V"));
        MINIMUM_TEMPLATES.add(toPattern("V", "ADV", "J"));
    };
    
    /**
     * Creates the acceptor with its standard templates
     */
    public MinimumPennTemplateAcceptor() { }
   
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
        // Filter out paths that can't match the template due to length
        if (path.length() != 2)
            return false;
        
        // Check that the nodes weren't filtered out.  If so reject the path
        // even if the part of speech and relation text may have matched a
        // template.
        if (path.getNode(0).word().equals(IteratorFactory.EMPTY_TOKEN)
                || path.getNode(0).word().equals(IteratorFactory.EMPTY_TOKEN))
            return false;

        String pos1 = path.getNode(0).pos();
        String rel = path.getRelation(0);
        String pos2 = path.getNode(1).pos();

        // Check whether each pos has a class category to which it should be
        // mapped.  These classes are necessary to handle the singificant number
        // of variations for a general category of POS's, e.g. verb -> VBZ, VBJ,
        // etc., which were not present when the MINIPAR tags were designed by
        // Padó and Lapata.
        String class1 = POS_TAG_TO_CLASS.get(pos1);
        String class2 = POS_TAG_TO_CLASS.get(pos2);
        String pattern = toPattern((class1 == null) ? pos1 : class1, rel,
                                   (class2 == null) ? pos2 : class2);
        return MINIMUM_TEMPLATES.contains(pattern);
    }
    
    /**
     * {@inheritDoc}
     */
    public int maxPathLength() {
        return 2;
    }
    
    /**
     * Returns the pattern string for the provided parts of speech and relation.
     */
    static String toPattern(String pos1, String rel, String pos2) {
        return pos1 + ":" + rel + ":" + pos2;
    }

}