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

package edu.ucla.sspace.evaluation;

import java.util.List;


/**
 * @author Keith Stevens
 */
public class SimpleNormedPrimingQuestion implements NormedPrimingQuestion {

    /**
     * The cue word for this priming question.
     */
    private String cue;

    /**
     * The set of targets associated with the priming question.
     */
    private String[] targets;

    /**
     * The corresponding set of strengths for each target.
     */
    private double[] strengths;

    /**
     * Creates a new {@link SimpleMultipleChoiceQuestion} from the given pieces
     * of data.
     */
    public SimpleNormedPrimingQuestion(String cue,
                                       String[] targets,
                                       double[] strengths) {
        if (targets.length != strengths.length)
            throw new IllegalArgumentException(
                    "The target and strength length must be equal");
        this.cue = cue;
        this.targets = targets;
        this.strengths = strengths;
    }
    /**
     * {@inheritDoc}
     */
    public String getCue() {
        return cue;
    }

    /**
     * {@inheritDoc}
     */
    public int numberOfTargets() {
        return targets.length;
    }

    /**
     * {@inheritDoc}
     */
    public String getTarget(int i) {
        return targets[i];
    }

    /**
     * {@inheritDoc}
     */
    public double getStrength(int i) {
        return strengths[i];
    }
}
