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

package edu.ucla.sspace.util;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A utility class for setting the logging level output of the S-Space package.
 * Logging information for each level can be categorized using the following
 * table.
 * <table>
 *
 *   <tr><td>{@link Level.WARNING}</td><td>Warnings about how the current
 *   systems's configuration could lead to errors.  These messages do not
 *   necessary indicate that an error occurred, however.</td></tr>
 *
 *   <tr><td>{@link Level.INFO}</td><td>High-level information about the current
 *   state of processing for a semantic space algorithm.  This is the standard
 *   level for normal output.</td></tr>
 *
 *   <tr><td>{@link Level.FINE}</td><td>Information about the current state of
 *   document processin, or detailed information about the state of the semantic
 *   space.  This level should be used for verbose output.</td></tr>

 *   <tr><td>{@link Level.FINER}</td><td>Detailed information about the current
 *   state of exectution.  The output may be used to trace high-level execution
 *   flow.  This level should only be used for very verbose output.</td></tr>
 *
 * </table>
 * All other level remain unused at this time.
 */
public final class LoggerUtil {
    
    /**
     * Uninstantiable
     */
    private LoggerUtil() { }

    /**
     * Sets the output level of the S-Space package according to the desired
     * level.
     */
    public static void setLevel(Level outputLevel) {
        Logger appRooLogger = Logger.getLogger("edu.ucla.sspace");
        Handler verboseHandler = new ConsoleHandler();
        verboseHandler.setLevel(outputLevel);
        appRooLogger.addHandler(verboseHandler);
        appRooLogger.setLevel(outputLevel);
        appRooLogger.setUseParentHandlers(false);
    }

    /**
     * Prints {@link Level#FINE} messages to the provided {@link Logger}.
     */
    public static void verbose(Logger log, String format, Object... args) {
        if (log.isLoggable(Level.FINE))
            log.fine(String.format(format, args));
    }

    /**
     * Prints {@link Level#INFO} messages to the provided {@link Logger}.
     */
    public static void info(Logger log, String format, Object... args) {
        if (log.isLoggable(Level.INFO))
            log.info(String.format(format, args));
    }

    /** 
     * Prints {@link Level#WARNING} messages to the provided {@link Logger}.
     */
    public static void warning(Logger log, String format, Object... args) {
        if (log.isLoggable(Level.WARNING))
            log.warning(String.format(format, args));
    }

    /** 
     * Prints {@link Level#SEVERE} messages to the provided {@link Logger}.
     */
    public static void severe(Logger log, String format, Object... args) {
        if (log.isLoggable(Level.SEVERE))
            log.severe(String.format(format, args));
    }
}
