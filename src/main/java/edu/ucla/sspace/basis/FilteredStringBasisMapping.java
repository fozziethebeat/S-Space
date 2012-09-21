/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

package edu.ucla.sspace.basis;

import java.util.Set;


/**
 * This {@link FilteredStringBasisMapping} allows a user to specify a set of
 * tokens that should be excluded automatically from the basis mapping.  Any
 * calles to {@code getDimension} for words in this set will automatically
 * return {@code -1}.
 *
 * @author Keith Stevens
 */
public class FilteredStringBasisMapping
        extends AbstractBasisMapping<String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * The set of excluded words.
     */
    private final Set<String> excludedWords;

    /**
     * Creates a new {@link FilteredStringBasisMapping} where the words in
     * {@code excludedWords} will never receive a dimension in this mapping.
     */
    public FilteredStringBasisMapping(Set<String> excludedWords) {
        this.excludedWords = excludedWords;
    }

    /**
     * {@inheritDoc}
     */
    public int getDimension(String key) {
        String[] parts = key.split("-");
        String base = (parts.length == 0) ? key : parts[0];
        return excludedWords.contains(base) ? -1 : getDimensionInternal(key);
    }
}
