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

import java.util.HashSet;
import java.util.Set;

/**
 * A class containing information on the Penn Treebank part of speech (POS) tag
 * set.  This class is a utility for other classes that need to interact with
 * objects on the basis of their part of speech category.
 */
public final class PennTags {

    public static final Set<String> NOUN_POS_TAGS = new HashSet<String>();
    public static final Set<String> VERB_POS_TAGS = new HashSet<String>();
    public static final Set<String> ADJ_POS_TAGS = new HashSet<String>();
    public static final Set<String> ADV_POS_TAGS = new HashSet<String>();

    public static final Set<String> MODIFIERS = new HashSet<String>();
    
    static {
        ADJ_POS_TAGS.add("JJ");
        ADJ_POS_TAGS.add("JJR");
        ADJ_POS_TAGS.add("JJS");

        ADV_POS_TAGS.add("RB");
        ADV_POS_TAGS.add("RBR");
        ADV_POS_TAGS.add("RBS");

        NOUN_POS_TAGS.add("NN");
        NOUN_POS_TAGS.add("NNS");
        NOUN_POS_TAGS.add("NP");
        NOUN_POS_TAGS.add("NPS");

        VERB_POS_TAGS.add("VB");
        VERB_POS_TAGS.add("VBD");
        VERB_POS_TAGS.add("VBG");
        VERB_POS_TAGS.add("VBN");
        VERB_POS_TAGS.add("VBP");
        VERB_POS_TAGS.add("VBZ");
        VERB_POS_TAGS.add("VH");
        VERB_POS_TAGS.add("VHD");
        VERB_POS_TAGS.add("VHG");
        VERB_POS_TAGS.add("VHN");
        VERB_POS_TAGS.add("VHP");
        VERB_POS_TAGS.add("VHZ");
        VERB_POS_TAGS.add("VV");
        VERB_POS_TAGS.add("VVD");
        VERB_POS_TAGS.add("VVG");
        VERB_POS_TAGS.add("VVN");
        VERB_POS_TAGS.add("VVP");
        VERB_POS_TAGS.add("VVZ");        
    }

}