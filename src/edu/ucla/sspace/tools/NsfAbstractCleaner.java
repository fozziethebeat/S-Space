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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.text.DocumentPreprocessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;


/**
 * An informal tool which cleans the <a
 * href="http://archive.ics.uci.edu/ml/databases/nsfabs/nsfawards.html>NSF
 * Research Awards Abstracts 1990-2003</a> corpus.  This cleaner removes all of
 * the meta data for each abstract posting.  This cleaner is expected to be run
 * with the all the awards directories to be in a single directory.  Output will
 * be written to a specified file where each line will contain all the contents
 * of a single abstract.
 *
 * @author Keith Stevens
 */
public class NsfAbstractCleaner {

    public static void main(String[] args) throws Exception{
        if (args.length != 2) {
            System.out.println(
                    "usage: java NsfAbstractCleaner <abstract_dir> <out_file>");
            System.exit(1);
        }

        DocumentPreprocessor processor = new DocumentPreprocessor();
        PrintWriter pw = new PrintWriter(args[1]);

        File baseAbstractDir = new File(args[0]);
        // Iterate over the year directories in the main directory.
        for (File abstractYearDir : baseAbstractDir.listFiles()) {

            // Skip files that are not directories and files that do not start
            // with "awards".
            if (!abstractYearDir.isDirectory() ||
                !abstractYearDir.getName().startsWith("awards"))
                continue;

            // Each NSF award year directory is split into several
            // subdirectories, iterate over each one.
            for (File abstractPartDir : abstractYearDir .listFiles()) {

                // Skip any non directory entries, such as links.html.
                if (!abstractPartDir.isDirectory())
                    continue;

                // Iterate over each award.
                for (File awardFile : abstractPartDir.listFiles()) {
                    BufferedReader br = 
                        new BufferedReader(new FileReader(awardFile));
                    StringBuilder sb = new StringBuilder();
                    boolean startedContent = false;

                    // Scan through the posting to find the "Abstract" line.
                    // This line marks the beginning of the real abstract.
                    for (String line = null; (line = br.readLine()) != null; ) {
                        if (startedContent)
                            sb.append(line).append(" ");
                        if (line.startsWith("Abstract"))
                            startedContent = true;
                    }

                    // Clean and write the posting's content to the output file.
                    sb.append("\n");
                    String cleanedContent = processor.process(sb.toString());
                    System.out.println(awardFile.getAbsolutePath());
                    pw.printf("%s\n", cleanedContent);
                    br.close();
                }
            }
        }
        pw.close();
    }
}
