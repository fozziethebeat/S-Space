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

import edu.ucla.sspace.purandare.PurandareFirstOrder;

import java.io.IOError;
import java.io.IOException;

import java.util.Properties;


/**
 * An executable class for running {@link PurandareFirstOrder} from the command
 * line.  See the Purandare and Pedersen <a
 * href="http://code.google.com/p/airhead-research/wiki/PurandareAndPedersen">
 * wiki page</a> for details on running this class from the command line. <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see PurandareFirstOrder
 *
 * @author David Jurgens
 */
public class PurandareMain extends GenericMain {

    /**
     * Uninstantiable.
     */
    private PurandareMain() { }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    protected void addExtraOptions(ArgOptions options) {
        options.addOption('m', "maxContexts", "The maximum number of contexts "
                          +"to use per word", true, "INT", "Algorithm Options");
    }

    public static void main(String[] args) {
        PurandareMain lsa = new PurandareMain();
        try {
            lsa.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected SemanticSpace getSpace() {
        return new PurandareFirstOrder();
    }

    /**
     * Returns the {@link SSpaceFormat.SPARSE_BINARY sparse_binary} format as
     * the default format of a {@code PurandareFirstOrder} space.
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    /**
     * {@inheritDoc}
     */
    protected Properties setupProperties() {
        Properties props = System.getProperties();
        // Use the command line options to set the desired properites in the
        // constructor.  Use the system properties in case these properties were
        // set using -Dprop=<value>
        if (argOptions.hasOption("maxContexts")) {
            props.setProperty(PurandareFirstOrder.MAX_CONTEXTS_PER_WORD,
                              argOptions.getStringOption("maxContexts"));
        }
        return props;
    }
}
