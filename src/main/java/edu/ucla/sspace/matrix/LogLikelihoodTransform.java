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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.common.statistics.LogLikelihoodTest;


/**
 * Transforms a matrix using the log-likelihood weight.  The input matrix is
 * assumed to have non-negative values and be formatted as rows representing
 * terms and columns representing terms.  Each matrix cell indicates the number
 * of times the row's word occurs within the some range of the column's word.
 * Although the log likelihood typically requires much more than this, an
 * estimation is used that utilizes only the occurrence frequency counts based.
 * See the following papers for details and analysis:
 *
 * </li style="font-family:Garamond, Georgia, serif"> Pado, S. and Lapata, M.
 * (2007) Dependnecy-Based Construction of Semantic Space Models.
 * <i>Association of Computational Linguistics</i>, <b>33</b>.
 
 * @author Keith Stevens
 */
public class LogLikelihoodTransform extends SignificanceMatrixTransform {

    public LogLikelihoodTransform() {
        super(new LogLikelihoodTest());
    }
}
