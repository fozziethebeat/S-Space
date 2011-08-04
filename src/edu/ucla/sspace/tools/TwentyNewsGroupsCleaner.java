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
 * href="http://kdd.ics.uci.edu/databases/20newsgroups/20newsgroups.html">20
 * NewsGroups</a> corpus.  This cleaner removes all of the meta data for each
 * newsgroup posting.  Email characters, such as "<" are separated from normal
 * text.  This cleaner is expected to be run with the "20_newsgroups" directory
 * that is provided in the standard tarball distributed by UCI.  Output will be
 * written to a specified file where each line will contain all the contents of
 * a single posting.
 *
 * @author Keith Stevens
 */
public class TwentyNewsGroupsCleaner {

    public static void main(String[] args) throws Exception{
        if (args.length != 2) {
            System.out.println(
                    "usage: java TwentyNewsGroupCleaner <ng_dir> <out_file>");
            System.exit(1);
        }

        DocumentPreprocessor processor = new DocumentPreprocessor();
        PrintWriter pw = new PrintWriter(args[1]);

        File baseNGDir = new File(args[0]);
        // Iterate over the newsgroup directories in the main directory.
        for (File newsGroupDir : baseNGDir.listFiles()) {

            // Skip any non-directories.
            if (!newsGroupDir.isDirectory())
                continue;

            // Iterate over the individual postings in each newsgroup.
            for (File newsGroupEntry : newsGroupDir.listFiles()) {
                BufferedReader br = 
                    new BufferedReader(new FileReader(newsGroupEntry));
                StringBuilder sb = new StringBuilder();
                boolean startedContent = false;

                // Scan through the posting to find the "Lines" line.  This line
                // marks the beginning of the real newsgroup data.
                for (String line = null; (line = br.readLine()) != null; ) {
                    if (startedContent)
                        sb.append(line).append(" ");
                    if (line.startsWith("Lines:"))
                        startedContent = true;
                }

                // Clean and write the posting's content to the output file.
                sb.append("\n");
                String cleanedContent = processor.process(sb.toString());
                System.out.println(newsGroupEntry.getAbsolutePath());
                pw.printf("%s\n", cleanedContent);
                br.close();
            }
        }
        pw.close();
    }
}
