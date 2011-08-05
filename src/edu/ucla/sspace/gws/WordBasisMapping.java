/*
 * Copyright 2011 David Jurgens
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

package edu.ucla.sspace.gws;

import edu.ucla.sspace.basis.AbstractBasisMapping;

import edu.ucla.sspace.util.Duple;


/**
 * A {@link BasisMapping} implementation where each word corresponds to a unique
 * dimension regardless of its word position.  
 *
 * @author David Jurgens
 */
public class WordBasisMapping
        extends AbstractBasisMapping<Duple<String,Integer>, String> {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    public int getDimension(Duple<String, Integer> key) {
        return getDimensionInternal(key.x);
    }
}
