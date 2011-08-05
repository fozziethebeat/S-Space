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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.svs.StructuredVectorSpace;

import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.wordsi.DependencyContextGenerator;
import edu.ucla.sspace.wordsi.SelPrefDependencyContextGenerator;

import java.io.File;


/**
 * A dependency based executable class for running {@link Wordsi}.  {@link
 * GenericWordsiMain} provides the core command line arguments and
 * functionality.  This class provides the following additional arguments:
 *
 * <ul>
 *   <li><u>Optional</u>
 *     <ul>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class SelPrefWordsiMain extends DVWordsiMain {

    public static void main(String[] args) throws Exception {
        SelPrefWordsiMain main = new SelPrefWordsiMain();
        main.run(args);
    }

    /**
     * {@inheritDoc}
     */
    protected void addExtraOptions(ArgOptions options) {
        super.addExtraOptions(options);

        options.addOption('l', "selecitonalPreferenceSpace",
                          "A serialized SelecitonalPreference SemanticSpace",
                          true, "FILE", "Required");
    }

    /**
     * {@inheritDoc}
     */
    protected DependencyContextGenerator getContextGenerator() {
        StructuredVectorSpace svs = SerializableUtil.load(new File(
                    argOptions.getStringOption('l')));
        return new SelPrefDependencyContextGenerator(svs);
    }
}
