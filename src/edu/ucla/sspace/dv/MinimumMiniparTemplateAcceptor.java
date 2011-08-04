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

import java.util.HashSet;
import java.util.Set;


/**      
 * A {@code DependencyPathAcceptor} that accepts the minimum set of path
 * templates specified by <a
 * href="http://www.nlpado.de/~sebastian/pub/papers/cl07_pado.pdf">Padó and
 * Lapata (2007)</a>.  This acceptor is designed to be used with the <a
 * link="http://webdocs.cs.ualberta.ca/~lindek/minipar.htm">Minipar</a> parser
 * and its associated part of speech tag set
 *
 * @see MediumMiniparTemplateAcceptor
 * @see MaximumMiniparTemplateAcceptor
 */
public class MinimumMiniparTemplateAcceptor implements DependencyPathAcceptor {

    static final Set<String> MINIMUM_TEMPLATES = new HashSet<String>();

    static {
        MINIMUM_TEMPLATES.add(toPattern("A", "amod", "V"));
        MINIMUM_TEMPLATES.add(toPattern("A", "amod", "A"));
        MINIMUM_TEMPLATES.add(toPattern("A", "mod", "A"));
        MINIMUM_TEMPLATES.add(toPattern("A", "mod", "N"));
        MINIMUM_TEMPLATES.add(toPattern("A", "mod", "Prep"));
        MINIMUM_TEMPLATES.add(toPattern("A", "mod", "V"));
        MINIMUM_TEMPLATES.add(toPattern("A", "subj", "N"));
        MINIMUM_TEMPLATES.add(toPattern("N", "conj", "N"));
        MINIMUM_TEMPLATES.add(toPattern("N", "gen", "N"));
        MINIMUM_TEMPLATES.add(toPattern("N", "mod", "A"));
        MINIMUM_TEMPLATES.add(toPattern("N", "mod", "Prep"));
        MINIMUM_TEMPLATES.add(toPattern("N", "nn", "N"));
        MINIMUM_TEMPLATES.add(toPattern("N", "obj", "V"));
        MINIMUM_TEMPLATES.add(toPattern("N", "pcomp-n", "Prep"));
        MINIMUM_TEMPLATES.add(toPattern("N", "subj", "A"));
        MINIMUM_TEMPLATES.add(toPattern("N", "subj", "N"));
        MINIMUM_TEMPLATES.add(toPattern("N", "subj", "V"));
        MINIMUM_TEMPLATES.add(toPattern(null, "lex-mod", "V"));
        MINIMUM_TEMPLATES.add(toPattern("Prep", "mod", "A"));
        MINIMUM_TEMPLATES.add(toPattern("Prep", "mod", "N"));
        MINIMUM_TEMPLATES.add(toPattern("Prep", "mod", "V"));   
        MINIMUM_TEMPLATES.add(toPattern("Prep", "pcomp-n", "N"));
        MINIMUM_TEMPLATES.add(toPattern("V", "amod", "A"));
        MINIMUM_TEMPLATES.add(toPattern("V", "lex-mod", null));
        MINIMUM_TEMPLATES.add(toPattern("V", "mod", "A"));
        MINIMUM_TEMPLATES.add(toPattern("V", "mod", "Prep"));
        MINIMUM_TEMPLATES.add(toPattern("V", "obj", "N"));
        MINIMUM_TEMPLATES.add(toPattern("V", "subj", "N"));
    };
    
    /**
     * Creates the acceptor with its standard templates
     */
    public MinimumMiniparTemplateAcceptor() { }
   
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

        return MINIMUM_TEMPLATES.contains(toPattern(pos1, rel, pos2));
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