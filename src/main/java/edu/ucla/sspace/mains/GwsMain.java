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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.gws.GenericWordSpace;

import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running the {@link GenericWordSpace} from the command
 * line.  See the <a href="http://code.google.com/p/airhead-research/">S-Space
 * documentation</a> for full details on the command line parameters.
 *
 * <p>This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see GenericWordSpace
 *
 * @author David Jurgens
 */
public class GwsMain extends GenericMain {

    private GwsMain() { }

    /**
     * {@inheritDoc}
     */
    protected void addExtraOptions(ArgOptions options) {
        options.addOption('s', "windowSize",
                          "The number of words to inspect to the left and " +
                          "right of a focus word (default: 5)",
                          true, "INT", "Algorithm Options");
        options.addOption('W', "useWordOrder", "Distinguish between relative "
                          + "positions of co-occurrences of the same word "
                          + "(default: false)", false,
                          null, "Algorithm Options");
    }

    public static void main(String[] args) {
        GwsMain gws = new GwsMain();
        try {
            gws.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    /**
     * {@inheritDoc}
     */
    protected SemanticSpace getSpace() {
        return new GenericWordSpace();
    }

    /**
     * {@inheritDoc}
     */
    protected Properties setupProperties() {
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = System.getProperties();

         if (argOptions.hasOption("windowSize")) {
             props.setProperty(
                     GenericWordSpace.WINDOW_SIZE_PROPERTY,
                     argOptions.getStringOption("windowSize"));
         }

         if (argOptions.hasOption("useWordOrder")) {
             props.setProperty(
                     GenericWordSpace.USE_WORD_ORDER_PROPERTY, "true");
         }        

        return props;
    }
}
