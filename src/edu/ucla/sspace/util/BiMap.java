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

package edu.ucla.sspace.util;

import java.util.Map;


/**
 * This interface allows for a bi-directional mapping, where keys can map to
 * values and values can map to keys.  This is expected to be used with
 * one-to-oen mappings.
 *
 * @author Keith Stevens
 */
public interface BiMap<K, V> extends Map<K, V> {

    /**
     * Returns a reversed form of this {@link BiMap}, where values in this
     * {@link BiMap} will map to keys in this {@link BiMap}.  Calling {@code
     * inverse} on the returned {@link BiMap} should return a pointer to the
     * original {@link BiMap}.
     */
    BiMap<V, K> inverse();
}
