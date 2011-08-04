/*
 * Copyright 2009 Keith Stevens 
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

import edu.ucla.sspace.text.DocumentPreprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class OneLineDocumentCleaner {

    public static void main(String[] args) {
	try {
	    if (args.length != 2) {
		usage();
		return;
	    }
	    DocumentPreprocessor processor = new DocumentPreprocessor();
	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
	    BufferedWriter bw = new BufferedWriter(new FileWriter(args[1]));
	    for (String line = null; (line = br.readLine()) != null;) {
		String cleaned = processor.process(line);
		if (!cleaned.equals("")){
		    bw.write(cleaned);
		    bw.newLine();
		}
	    }
	    bw.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    private static void usage() {
	System.out.println(
	    "java OneLineDocumentCleaner word-file input-file output-file\n" +
	    "  input-file: a file with one document per line\n" +
	    "  output-file: a file where the cleaned documents will be put");
    }
}
